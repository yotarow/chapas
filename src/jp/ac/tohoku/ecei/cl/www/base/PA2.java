/*
 *  Copyright (c) 2013, Yotaro Watanabe
 *  All rights reserved.
 *  
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *    * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    * Neither the names of the authors nor the names of its contributors
 *      may be used to endorse or promote products derived from this
 *      software without specific prior written permission.
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *  OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jp.ac.tohoku.ecei.cl.www.base;

import java.util.Iterator;

import jp.ac.tohoku.ecei.cl.www.util.*;

public class PA2 extends AbstractOnlineLinearModelLearner {
    
    public enum PAMode { PA, PAI, PAII }

    private PAMode mode;
    private double rho;
    private int p;
    private int c;

    public PA2 (LinearModel model) {
        this (model, PAMode.PAI, true);
    }

    public PA2 (LinearModel model, PAMode mode) {
        this (model, mode, true);
    }

    public PA2 (LinearModel model, PAMode mode, boolean averaged) {
        this (model, mode, 1.0, 2, 1, averaged);
    }

    public PA2 (LinearModel model, PAMode mode, double rho, int p, int c, boolean averaged) {
        super(model, averaged);
        // init model parameter
        this.mode = mode;
        this.rho = rho;
        this.p = p;
        this.c = c;
    }

    // ---------------------------------------------------------------------------------------
    // for multi-class classification
    // ---------------------------------------------------------------------------------------

    public int learn(InstanceList2[] instList) {
        int numOfUpdates = 0;
        for (int i = 0; i < instList.length; i++) {
            numOfUpdates += this.learn(instList[i].getInstances());
        }
        return numOfUpdates;
    }

    public int learn(Instance2[] instances) {
        int numOfUpdates = 0;
        for (int i = 0; i < instances.length; i++) {
            numOfUpdates += this.incrementalLearn(instances[i]);
        }
        return numOfUpdates;
    }

    public int incrementalLearn(Instance2 instance) {
        int numOfUpdates = 0;
        FeatureVector fv = instance.getFv();
        Label target = instance.getTarget();
        int g = target.getGroup();
        Alphabet la = target.getLabelAlphabet();
        Label argmax = null;
        double max = Double.NEGATIVE_INFINITY;
        double targetScore = 0.0;
        for (Iterator it = la.iterator(); it.hasNext();) {
            Label l = new Label(g, la, la.lookupIndex(it.next()));
            double value = dotProduct(fv, l, true);
            if (!l.equals(target)) {
                targetScore = value;
                value += this.rho;
            }
            if (value > max) {
                max = value;
                argmax = l;
            }
        }
        if (!target.equals(argmax)) {
            this.update(fv, target, argmax);
            numOfUpdates++;
        } 
        if (this.averaged) {
            this.model.ca++;
        }
        return numOfUpdates;
    }

    private void update(FeatureVector fv, Label target, Label argmax) {
        double loss = this.rho;
        loss -= dotProduct(fv, target, true);
        loss += dotProduct(fv, argmax, true);

        double fvNorm = 0.0;
        if (this.p == 2) {
            fvNorm = Math.sqrt(2 * Math.pow(fv.norm(this.p), 2));
        } else if (this.p == 1) {
            fvNorm = 2 * fv.norm(this.p);
        } else {
            System.out.println("p value is supported only for 1 and 2.");
            System.exit(1);
        }

        double tau;
        if (mode == PAMode.PAI) {
            // PA-I
            tau = loss / Math.pow(fvNorm, 2);
            if (tau > this.c) {
                tau = this.c;
            }
        } else if (mode == PAMode.PAII) {
            // PA-II
            tau = loss / ( Math.pow(fvNorm, 2) + 0.5 / this.c);
        } else {
            // PA
            tau = loss / Math.pow(fvNorm, 2);
        }
        this.update(fv, target, tau);
        this.update(fv, argmax, -tau);
    }

    // ---------------------------------------------------------------------------------------
    // for structured-prediction
    // ---------------------------------------------------------------------------------------

    public void incrementalLearn(FeatureVector[] corFvs, Labels[] corLabels, FeatureVector[] argmaxFvs, Labels[] argmaxLabels) {
        this.incrementalLearn(corFvs, corLabels, argmaxFvs, argmaxLabels, this.rho);
    }

    public void incrementalLearn(FeatureVector[] corFvs, Labels[] corLabels, FeatureVector[] argmaxFvs, Labels[] argmaxLabels, double rho) {
        double loss = rho;
        for (int i = 0; i < corFvs.length; i++) {
            loss -= dotProduct(corFvs[i], corLabels[i], true);
        }
        for (int i = 0; i < argmaxFvs.length; i++) {
            loss += dotProduct(argmaxFvs[i], argmaxLabels[i], true);
        }

        double fvNorm = this.getNorm(corFvs, corLabels, argmaxFvs, argmaxLabels);
        if (fvNorm == 0.0) {
            return; // do nothing
        }

        // calc tau
        double tau; 
        if (mode == PAMode.PAI) { // PA-I
            tau = loss / Math.pow(fvNorm, 2);
            tau = tau > this.c ? this.c : tau;
        } else if (mode == PAMode.PAII) { // PA-II
            tau = loss / ( Math.pow(fvNorm, 2) + 0.5 / this.c);
        } else { // PA
            tau = loss / Math.pow(fvNorm, 2); 
        }

        double[] corCoef = new double[corFvs.length];
        double[] argmaxCoef = new double[argmaxFvs.length];

        for (int i = 0; i < corFvs.length; i++) {
            corCoef[i] = tau;
        }

        for (int i = 0; i < argmaxFvs.length; i++) {
            argmaxCoef[i] = -tau;
        }

        this.update(corFvs, corLabels, corCoef);
        this.update(argmaxFvs, argmaxLabels, argmaxCoef);
        if (this.averaged) {
            this.model.ca++;
        }
    }

    private double getNorm(FeatureVector[] corFvs, Labels[] corLabels, FeatureVector[] argmaxFvs, Labels[] argmaxLabels) {
        double fvNorm = 0.0;
        Labels2ObjectHashMap vec = new Labels2ObjectHashMap();
        for (int i = 0; i < corFvs.length; i++) {
            if (!vec.containsKey(corLabels[i])) {
                vec.put(corLabels[i], new SparseVector2());
            }
            SparseVector2 sv = (SparseVector2) vec.get(corLabels[i]);
            sv.add(corFvs[i], 1.0);
        }

        for (int i = 0; i < argmaxFvs.length; i++) {
            if (!vec.containsKey(argmaxLabels[i])) {
                vec.put(argmaxLabels[i], new SparseVector2());
            }
            SparseVector2 sv = (SparseVector2) vec.get(argmaxLabels[i]);
            sv.add(argmaxFvs[i], -1.0);
        }

        Object[] vecs = vec.getObjects();
        SparseVector2[] svs = new SparseVector2[vecs.length];
        for (int i = 0; i < vecs.length; i++) {
            svs[i] = (SparseVector2) vecs[i];
        }

        for (int i = 0; i < svs.length; i++) {
            if (this.p == 1) {
                fvNorm += svs[i].norm(this.p);
            } else if (this.p == 2) {
                fvNorm += Math.pow(svs[i].norm(this.p), 2);
            }
        }
        if (this.p == 2) {
            fvNorm = Math.sqrt(fvNorm);
        }
        return fvNorm;
    }
}
