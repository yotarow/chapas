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

package jp.ac.tohoku.ecei.cl.www.eval;

import jp.ac.tohoku.ecei.cl.www.util.*;
import jp.ac.tohoku.ecei.cl.www.base.*;

import gnu.trove.iterator.*;
import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.hash.*;

public class Evaluator {

    private static final int N = 0;
    private static final int CORRECT = 1;
    private static final int RETURNED = 2;

    private TIntObjectHashMap<TIntIntHashMap> label2ncorret; // used for calculating precision recall and f.
    private IntInt2IntHashMap confusionMatrix;

    public Evaluator() {
        this.label2ncorret = new TIntObjectHashMap<TIntIntHashMap>();
        this.confusionMatrix = new IntInt2IntHashMap();
    }
    
    private void initIfNotPresent(int label) {
        if (!this.label2ncorret.containsKey(label)) {
            label2ncorret.put(label, new TIntIntHashMap());
            TIntIntHashMap ncorret = label2ncorret.get(label);
            ncorret.put(N,0);
            ncorret.put(CORRECT,0);
            ncorret.put(RETURNED,0);
        }
    }	

    public void addTarget(int label) {
        initIfNotPresent(label);
        TIntIntHashMap ncorret = label2ncorret.get(label);
        ncorret.increment(N);
    }
    
    public void addReturned(int label) {
        initIfNotPresent(label);
        TIntIntHashMap ncorret = label2ncorret.get(label);
        ncorret.increment(RETURNED);
    }
    
    public void addCorrect(int label) {
        initIfNotPresent(label);
        TIntIntHashMap ncorret = label2ncorret.get(label);
        ncorret.increment(CORRECT);
    }
    
    public void add(int target, int returned) {
        initIfNotPresent(target);
        initIfNotPresent(returned);
        TIntIntHashMap ncorret = label2ncorret.get(target);
        ncorret.increment(N);
        if (target == returned) {
            ncorret.increment(CORRECT);
        }
        ncorret = label2ncorret.get(returned);
        ncorret.increment(RETURNED);
        confusionMatrix.increment(target, returned);
    }
    
    public int getNum (int target, int returned) {
        return confusionMatrix.get(target, returned);
    }

    public double getTotalF() {
        int[] labels = this.getLabels();
        int n = 0;
        int cor = 0;
        int ret = 0;
        for (int i = 0; i < labels.length; i++) {
            n += this.getNumberOfTarget(labels[i]);
            cor += this.getNumberOfCorrect(labels[i]);
            ret += this.getNumberOfReturned(labels[i]);
        }
        double pre = ret == 0 ? 0.0 : 100*(double)cor/(double)ret;
        double rec = n == 0 ? 0.0 : 100*(double)cor/(double)n;
        return pre+rec == 0.0 ? 0.0 : 2*pre*rec/(pre+rec);
    }

    public int getNumberOfTarget(int label) {
        if (!this.label2ncorret.containsKey(label)) {
            return 0;
        }
        TIntIntHashMap ncorret = label2ncorret.get(label);
        return ncorret.get(N);
    }

    /*
    public int getNumberOfTarget() {
        int N = 0;
        int[] labels = getLabels();
        for (int i = 0; i < labels.length; i++) {
            N += getNumberOfTarget(labels[i]);
        }
        return N;
    }
    */

    public int getNumberOfCorrect(int label) {
        if (!this.label2ncorret.containsKey(label)) {
            return 0;
        }
        TIntIntHashMap ncorret = label2ncorret.get(label);
        return ncorret.get(CORRECT);
    }
    /*
    public int getNumberOfCorrect() {
        int cor = 0;
        int[] labels = getLabels();
        for (int i = 0; i < labels.length; i++) {
            cor += getNumberOfCorrect(labels[i]);
        }
        return cor;
    }
    */
    public int getNumberOfReturned(int label) {
        if (!this.label2ncorret.containsKey(label)) {
            return 0;
        }
        TIntIntHashMap ncorret = label2ncorret.get(label);
        return ncorret.get(RETURNED);
    }
    /*
    public int getNumberOfReturned() {
        int ret = 0;
        int[] labels = getLabels();
        for (int i = 0; i < labels.length; i++) {
            ret += getNumberOfCorrect(labels[i]);
        }
        return ret;
    }
    */
    public int[] getLabels() {
        return this.label2ncorret.keys();
    }

    public double getF(int label) {
        if (!this.label2ncorret.containsKey(label)) { return 0; }
        int n = this.getNumberOfTarget(label);
        int cor = this.getNumberOfCorrect(label);
        int ret = this.getNumberOfReturned(label);
        double pre = ret == 0 ? 0.0 : 100*(double)cor/(double)ret;
        double rec = n == 0 ? 0.0 : 100*(double)cor/(double)n;
        return pre+rec == 0.0 ? 0.0 : 2*pre*rec/(pre+rec);
    }

    public double getPrecision(int label) {
        if (!this.label2ncorret.containsKey(label)) { return 0; }
        int cor = this.getNumberOfCorrect(label);
        int ret = this.getNumberOfReturned(label);
        return ret == 0 ? 0.0 : 100*(double)cor/(double)ret;
    }

    public double getRecall(int label) {
        if (!this.label2ncorret.containsKey(label)) { return 0; }
        int n = this.getNumberOfTarget(label);
        int cor = this.getNumberOfCorrect(label);
        return n == 0 ? 0.0 : 100*(double)cor/(double)n;
    }

    public double getTotalMicroF() {
        int[] labels = this.getLabels();
        int n = 0;
        int cor = 0;
        int ret = 0;
        for (int i = 0; i < labels.length; i++) {
            n += this.getNumberOfTarget(labels[i]);
            cor += this.getNumberOfCorrect(labels[i]);
            ret += this.getNumberOfReturned(labels[i]);
        }
        double pre = ret == 0 ? 0.0 : 100*(double)cor/(double)ret;
        double rec = n == 0 ? 0.0 : 100*(double)cor/(double)n;
        return pre+rec == 0.0 ? 0.0 : 2*pre*rec/(pre+rec);
    }

    public double getTotalMacroF() {
        int[] labels = this.getLabels();
        double totalF = 0.0;
        for (int i = 0; i < labels.length; i++) {
            int n = this.getNumberOfTarget(labels[i]);
            int cor = this.getNumberOfCorrect(labels[i]);
            int ret = this.getNumberOfReturned(labels[i]);
            double pre = ret == 0 ? 0.0 : 100*(double)cor/(double)ret;
            double rec = n == 0 ? 0.0 : 100*(double)cor/(double)n;
            totalF += pre+rec == 0.0 ? 0.0 : 2*pre*rec/(pre+rec);
        }
        return totalF / (double) labels.length;
    }

    public double getTotalFWithout(int label) {
        int[] labels = this.getLabels();
        int n = 0;
        int cor = 0;
        int ret = 0;
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] == label) { continue; }
            n += this.getNumberOfTarget(labels[i]);
            cor += this.getNumberOfCorrect(labels[i]);
            ret += this.getNumberOfReturned(labels[i]);
        }
        double pre = ret == 0 ? 0.0 : 100*(double)cor/(double)ret;
        double rec = n == 0 ? 0.0 : 100*(double)cor/(double)n;
        return pre+rec == 0.0 ? 0.0 : 2*pre*rec/(pre+rec);
    }

    public int getNumberOfTarget() {
        int[] labels = this.getLabels();
        int n = 0;
        for (int i = 0; i < labels.length; i++) {
            n += this.getNumberOfTarget(labels[i]);
        }
        return n;
    }

    public int getNumberOfTargetWithout(int label) {
        int[] labels = this.getLabels();
        int n = 0;
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] == label) { continue; }
            n += this.getNumberOfTarget(labels[i]);
        }
        return n;
    }

    public int getNumberOfReturned() {
        int[] labels = this.getLabels();
        int ret = 0;
        for (int i = 0; i < labels.length; i++) {
            ret += this.getNumberOfReturned(labels[i]);
        }
        return ret;
    }

    public int getNumberOfReturnedWithout(int label) {
        int[] labels = this.getLabels();
        int ret = 0;
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] == label) { continue; } 
            ret += this.getNumberOfReturned(labels[i]);
        }
        return ret;
    }

    public int getNumberOfCorrect() {
        int[] labels = this.getLabels();
        int cor = 0;
        for (int i = 0; i < labels.length; i++) {
            cor += this.getNumberOfCorrect(labels[i]);
        }
        return cor;
    }

    public int getNumberOfCorrectWithout(int label) {
        int[] labels = this.getLabels();
        int cor = 0;
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] == label) { continue; }
            cor += this.getNumberOfCorrect(labels[i]);
        }
        return cor;
    }

    public double getTotalPrecision() {
        int[] labels = this.getLabels();
        int cor = 0;
        int ret = 0;
        for (int i = 0; i < labels.length; i++) {
            cor += this.getNumberOfCorrect(labels[i]);
            ret += this.getNumberOfReturned(labels[i]);
        }
        return ret == 0 ? 0.0 : 100*(double)cor/(double)ret;
    }

    public double getTotalPrecisionWithout (int label) {
        int[] labels = this.getLabels();
        int cor = 0;
        int ret = 0;
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] == label) { continue; }
            cor += this.getNumberOfCorrect(labels[i]);
            ret += this.getNumberOfReturned(labels[i]);
        }
        return ret == 0 ? 0.0 : 100*(double)cor/(double)ret;
    }

    public double getTotalRecall() {
        int[] labels = this.getLabels();
        int n = 0;
        int cor = 0;
        for (int i = 0; i < labels.length; i++) {
            n += this.getNumberOfTarget(labels[i]);
            cor += this.getNumberOfCorrect(labels[i]);
        }
        return n == 0 ? 0.0 : 100*(double)cor/(double)n;
    }

    public double getTotalRecallWithout (int label) {
        int[] labels = this.getLabels();
        int n = 0;
        int cor = 0;
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] == label) { continue; }
            n += this.getNumberOfTarget(labels[i]);
            cor += this.getNumberOfCorrect(labels[i]);
        }
        return n == 0 ? 0.0 : 100*(double)cor/(double)n;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        int[] labels = this.getLabels();
        for (int i = 0; i < labels.length; i++) {
            sb.append("LABEL="+labels[i]+"\n");
            int n = this.getNumberOfTarget(labels[i]);
            int cor = this.getNumberOfCorrect(labels[i]);
            int ret = this.getNumberOfReturned(labels[i]);
            double pre = ret == 0 ? 0.0 : 100*(double)cor/(double)ret;
            double rec = n == 0 ? 0.0 : 100*(double)cor/(double)n;
            double f = pre+rec == 0.0 ? 0.0 : 2*pre*rec/(pre+rec);
            sb.append("PRECISION="+pre+" ("+cor+"/"+ret+")  RECALL="+rec+" ("+cor+"/"+n+")  F="+f+"\n");
        }
        return sb.toString();
    }

    public String toString(Alphabet labelAlphabet) {
        StringBuilder sb = new StringBuilder();
        int[] labels = this.getLabels();
        for (int i = 0; i < labels.length; i++) {
            sb.append("LABEL="+labelAlphabet.lookupObject(labels[i])+"\n");
            int n = this.getNumberOfTarget(labels[i]);
            int cor = this.getNumberOfCorrect(labels[i]);
            int ret = this.getNumberOfReturned(labels[i]);
            double pre = 100*(double)cor/(double)ret;
            double rec = 100*(double)cor/(double)n;
            double f = pre+rec == 0.0 ? 0.0 : 2*pre*rec/(pre+rec);
            sb.append("PRECISION="+pre+" ("+cor+"/"+ret+")  RECALL="+rec+" ("+cor+"/"+n+")  F="+f+"\n");
        }
        return sb.toString();
    }

    public static void main (String[] args) {

        Evaluator eval = new Evaluator();
        eval.add(1,1);
        eval.add(0,0);
        eval.add(2,1);
        System.out.print(eval.toString());
    }
}
