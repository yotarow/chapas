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

package jp.ac.tohoku.ecei.cl.www.chapas;

import jp.ac.tohoku.ecei.cl.www.base.*;
import jp.ac.tohoku.ecei.cl.www.util.*;

public class PASInstance2 {
    
    public int predId;
    public Label predLabel;
    public String predType;

    public String[] argIds;
    public int[] argIndices;
    public Label[] argLabels;
    public int[] argLabelsInt;

    // feature vectors
    public FeatureVector[] srlFvs;
    public FeatureVector[] pajointFvs; // pred - arg bigram features
    public FeatureVector[][] srlPairwiseFvs;
    public FeatureVector globalPASFv;
    public FeatureVector predGlobalFv;
    public FeatureVector predFv;

    public boolean oracle = false;
    public double score;

    public PASInstance2 (int predId, Label predLabel, int[] argIndices, Label[] argLabels, 
                         FeatureVector predFv, FeatureVector[] srlFvs, FeatureVector[] pajointFvs, FeatureVector globalPASFv) {
        this.predId = predId;
        this.predLabel = predLabel;
        this.argIds = argIds;
        this.argIndices = argIndices;
        this.argLabels = argLabels;
        this.predFv = predFv;
        this.srlFvs = srlFvs;
        this.pajointFvs = pajointFvs;
        this.globalPASFv = globalPASFv;
    }

    public int getPredId () {
        return this.predId;
    }

    public Label getPredLabel() {
        return this.predLabel;
    }

    public String[] getArgIds() {
        return this.argIds;
    }

    public int[] getArgIndices () {
        return this.argIndices;
    }

    public Label[] getArgLabels () {
        return this.argLabels;
    }

    public int[] getArgLabelsInt () {
        return this.argLabelsInt;
    }
    
    public FeatureVector getPredFv () {
        return this.predFv;
    }

    public FeatureVector[] getSRLFvs () {
        return this.srlFvs;
    }

    public FeatureVector getGlobalFv () {
        return this.globalPASFv;
    }

    public FeatureVector getPredGlobalFv () {
        return this.predGlobalFv;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getScore() {
        return this.score;
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("[PASInstance]\n");
        s.append("arguments: ");
        for (int i = 0; i < this.argIndices.length; i++) {
            s.append(this.argIndices[i]+",");
        }
        s.append("\n");
        s.append("labels: ");
        for (int i = 0; i < this.argLabels.length; i++) {
            s.append(this.argLabels[i]+",");
        }
        s.append("\n");
        return s.toString();
    }
}

