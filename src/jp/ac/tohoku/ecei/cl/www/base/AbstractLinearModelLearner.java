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

import jp.ac.tohoku.ecei.cl.www.util.*;

public abstract class AbstractLinearModelLearner {

    protected boolean averaged = false;
    protected LinearModel model;

    public AbstractLinearModelLearner(LinearModel model) {
        this (model, false);
    }

    public AbstractLinearModelLearner(LinearModel model, boolean averaged) {
        this.model = model;
        this.averaged = averaged;
        if (averaged) {
            model.setAveraged();
        }
    }

    public abstract int learn(InstanceList2[] instances);
    public abstract int learn(Instance2[] instances);

    public void update(FeatureVector[] fvs, Labels[] labels, double[] coefficients) {
        for (int i = 0; i < labels.length; i++) {
            if (!this.model.w.containsKey(labels[i])) {
                this.model.w.put(labels[i], new SparseVector2());
                if (this.averaged) {
                    this.model.wa.put(labels[i], new SparseVector2());
                }
            }
        }
        for (int i = 0; i < fvs.length; i++) {
            ((SparseVector2)this.model.w.get(labels[i])).add(fvs[i], coefficients[i]);
        }
        if (this.averaged) {
            for (int i = 0; i < fvs.length; i++) {
                ((SparseVector2)this.model.wa.get(labels[i])).add(fvs[i], this.model.ca * coefficients[i]);
            }
        }
    }

    public void update(FeatureVector fv, Label label, double coefficient) {
        update(fv, new Labels(label), coefficient);
    }

    public void update(FeatureVector fv, Labels label, double coefficient) {
        if (!this.model.w.containsKey(label)) {
            this.model.w.put(label, new SparseVector2());
            if (this.averaged) {
                this.model.wa.put(label, new SparseVector2());
            }
        }
        SparseVector2 sv = (SparseVector2) this.model.w.get(label);
        sv.add(fv, coefficient);
        if (this.averaged) {
            SparseVector2 sva = (SparseVector2) this.model.wa.get(label);
            sva.add(fv, this.model.ca * coefficient);
        }
    }

    public void update(int idx, Label label, double coefficient) {
        if (!this.model.w.containsKey(label)) {
            this.model.w.put(label, new SparseVector2());
            if (this.averaged) {
                this.model.wa.put(label, new SparseVector2());
            }
        }
        SparseVector2 sv = (SparseVector2) this.model.w.get(label);
        sv.set(idx, sv.get(idx) + coefficient);
    }

    public void update(int idx, Labels label, double diff) {
        if (!this.model.w.containsKey(label)) {
            this.model.w.put(label, new SparseVector2());
            if (this.averaged) {
                this.model.wa.put(label, new SparseVector2());
            }
        }
        SparseVector2 sv = (SparseVector2) this.model.w.get(label);
        sv.set(idx, sv.get(idx) + diff);
    }

    public double dotProduct(FeatureVector fv, Label label) {
        return this.dotProduct(fv, new Labels(label), false);
    }

    public double dotProduct(FeatureVector fv, Label label, boolean train) {
        return this.dotProduct(fv, new Labels(label), train);
    }

    public double dotProduct(FeatureVector fv, Labels label) {
        return this.dotProduct(fv, label, false);
    }

    public double dotProduct(FeatureVector fv, Labels label, boolean train) {
        if (!this.model.w.containsKey(label)) {
            return 0.0;
        } 
        if (!train && this.averaged) {
            return ((SparseVector2)this.model.w.get(label)).dotProduct(fv) - ((SparseVector2)this.model.wa.get(label)).dotProduct(fv)/this.model.ca;
        } else {
            return ((SparseVector2)this.model.w.get(label)).dotProduct(fv);
        }
    } 
    
    public LinearModel getModel() {
        return this.model;
    }
}
