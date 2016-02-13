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

package jp.ac.tohoku.ecei.cl.www.liblinear;

import java.util.ArrayList;
import java.util.List;

import de.bwaldvogel.liblinear.Train;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.Model;

import jp.ac.tohoku.ecei.cl.www.base.*;

public class LibLinearUtil {
    /*
    public static double predictProbability (Model model, Feature[] x, double[] prob_estimates) throws IllegalArgumentException {
        int[] labels = model.getLabels();

        if (!model.isProbabilityModel()) {
            StringBuilder sb = new StringBuilder("probability output is only supported for logistic regression");
            sb.append(". This is currently only supported by the following solvers: ");
            int i = 0;
            for (SolverType solverType : SolverType.values()) {
                if (solverType.isLogisticRegressionSolver()) {
                    if (i++ > 0) {
                        sb.append(", ");
                    }
                    sb.append(solverType.name());
                }
            }
            throw new IllegalArgumentException(sb.toString());
        }
        int nr_class = model.getNrClass();
        int nr_w;
        if (nr_class == 2)
            nr_w = 1;
        else
            nr_w = nr_class;

        double label = predictValues(model, x, prob_estimates);
        for (int i = 0; i < nr_w; i++)
            prob_estimates[i] = 1 / (1 + Math.exp(-prob_estimates[i]));

        if (nr_class == 2) // for binary classification
            prob_estimates[1] = 1. - prob_estimates[0];
        else {
            double sum = 0;
            for (int i = 0; i < nr_class; i++)
                sum += prob_estimates[i];

            for (int i = 0; i < nr_class; i++)
                prob_estimates[i] = prob_estimates[i] / sum;
        }

        return label;
    }
    */

    public static Problem convert (ArrayList<Instance> instances) {
        List<Double> vy = new ArrayList<Double>();
        List<de.bwaldvogel.liblinear.Feature[]> vx = new ArrayList<de.bwaldvogel.liblinear.Feature[]>();
        int maxIndex = Integer.MIN_VALUE;
        double bias = -1.0;
        for (int i = 0; i < instances.size(); i++) {
            Instance inst = instances.get(i);
            Label l = inst.getTarget();
            
            FeatureVector fv = inst.getFv();
            fv.sortAndMerge();
            Feature[] feats = fv.getFeatures();
            de.bwaldvogel.liblinear.Feature[] featNodes = new FeatureNode[feats.length];
            for (int j = 0; j < feats.length; j++) {
                featNodes[j] = new FeatureNode(feats[j].index, (double) feats[j].value);
                if (maxIndex < feats[j].index) {
                    maxIndex = feats[j].index;
                }
            }
            vy.add(new Double(l.getLabel()));
            //System.err.println("l.label="+l.getLabel());
            vx.add(featNodes);
        }
        Problem p = constructProblem(vy, vx, maxIndex, bias);
        return p;
    }

    public static FeatureNode[] convert (FeatureVector fv) {
        fv.sortAndMerge();
        Feature[] feats = fv.getFeatures();
        FeatureNode[] featNodes = new FeatureNode[feats.length];
        for (int i = 0; i < feats.length; i++) {
            featNodes[i] = new FeatureNode(feats[i].index, (double) feats[i].value);
        }
        return featNodes;
    }

    private static Problem constructProblem(List<Double> vy, List<de.bwaldvogel.liblinear.Feature[]> vx, int max_index, double bias) {
        Problem prob = new Problem();
        prob.bias = bias;
        prob.l = vy.size();
        prob.n = max_index;
        if (bias >= 0) {
            prob.n++;
        }
        prob.x = new de.bwaldvogel.liblinear.Feature[prob.l][];
        for (int i = 0; i < prob.l; i++) {
            prob.x[i] = vx.get(i);

            if (bias >= 0) {
                assert prob.x[i][prob.x[i].length - 1] == null;
                prob.x[i][prob.x[i].length - 1] = new FeatureNode(max_index + 1, bias);
            }
        }

        prob.y = new double[prob.l];
        for (int i = 0; i < prob.l; i++)
            prob.y[i] = vy.get(i).doubleValue();

        return prob;
    }

    public static double[][] getWeights (Model model, FeatureNode[] fn) {
        double[] w = model.getFeatureWeights();
        int numClasses = model.getNrClass();
        int numFeats = model.getNrFeature();
        double[][] featWeights = new double[numClasses][];
        for (int i = 0; i < numClasses; i++) {
            featWeights[i] = new double[fn.length];
        }
        if (numClasses == 2) {
            for (int j = 0; j < fn.length; j++) {
                //if (w.length <= fn[j].index) {
                //System.err.println("w.length="+w.length+" fn[j].index="+fn[j].index);
                //}
                if (numFeats <= fn[j].index) { continue; } 
                featWeights[0][j] = w[fn[j].index-1];
            }
            for (int j = 0; j < fn.length; j++) {
                if (numFeats <= fn[j].index) { continue; } 
                featWeights[1][j] = -w[fn[j].index-1];
            }
        } else {
            featWeights = new double[numClasses][];
            for (int i = 0; i < numClasses; i++) {
                featWeights[i] = new double[fn.length];
                for (int j = 0; j < fn.length; j++) {
                    if (numFeats <= fn[j].index) { continue; } 
                    featWeights[i][j] = w[(fn[j].index-1)*numClasses+i];
                }
            }
        }
        return featWeights;
    }
}
