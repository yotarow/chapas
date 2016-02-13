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

public class Perceptron2 extends AbstractOnlineLinearModelLearner {
  
    private boolean margined;
    private int margin;

    public Perceptron2 (LinearModel model) {
	this (model, true);
    }
	
    public Perceptron2 (LinearModel model, boolean averaged) {
	this (model, false, 0, averaged);
    }

    public Perceptron2 (LinearModel model, boolean margined, int margin) {
	this (model, margined, margin, true);
    }

    public Perceptron2 (LinearModel model, boolean margined, int margin, boolean averaged) {
	super(model, averaged);
	this.margined = margined;
	this.margin = margin;
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
	    if (this.margined && !l.equals(target)) {
		value += this.margin;
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
	this.update(fv, target, 1.0);
	this.update(fv, argmax, -1.0);
    }

    // ---------------------------------------------------------------------------------------
    // for structured-prediction
    // ---------------------------------------------------------------------------------------

    public void incrementalLearn(FeatureVector[] corFvs, Labels[] corLabels, FeatureVector[] argmaxFvs, Labels[] argmaxLabels) {
	this.incrementalLearn(corFvs, corLabels, argmaxFvs, argmaxLabels, this.margined?this.margin:0.0);
    }

    public void incrementalLearn(FeatureVector[] corFvs, Labels[] corLabels, FeatureVector[] argmaxFvs, Labels[] argmaxLabels, double margin) {
	double[] corCoef = new double[corFvs.length];
	double[] argmaxCoef = new double[argmaxFvs.length];

	for (int i = 0; i < corFvs.length; i++) {
	    corCoef[i] = 1.0;
	}

	for (int i = 0; i < argmaxFvs.length; i++) {
	    argmaxCoef[i] = -1.0;
	}

	this.update(corFvs, corLabels, corCoef);
	this.update(argmaxFvs, argmaxLabels, argmaxCoef);
	if (this.averaged) {
	    this.model.ca++;
	}
    }
}
