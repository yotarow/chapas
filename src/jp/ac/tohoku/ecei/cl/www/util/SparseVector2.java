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

package jp.ac.tohoku.ecei.cl.www.util;

import java.io.Serializable;

import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.hash.*;

import jp.ac.tohoku.ecei.cl.www.base.*; 

public class SparseVector2 implements Serializable {

    private TIntDoubleHashMap vec;

    public SparseVector2() {
        this (1000);
    }
  
    public SparseVector2(int capacity) {
        this.vec = new TIntDoubleHashMap(capacity);
    }

    public SparseVector2(int[] indices,  double[] values) {
        this (1000);
        for (int i = 0; i < indices.length; i++) {
            this.vec.put(indices[i], values[i]);
        }
    }

    public void add(FeatureVector fv, double coefficient) {
        int fvSize = fv.size();
        for (int i = 0; i < fvSize; i++) {
            Feature f = fv.get(i);
            this.vec.put(f.index, this.vec.get(f.index) + f.value * coefficient);
        }
    }

    public void add(SparseVector2 sv, double coefficient) {
        int[] svIndices = sv.getIndices();
        double[] svValues = sv.getValues();
        for (int i = 0; i < svIndices.length; i++) {
            this.vec.put(svIndices[i], this.vec.get(svIndices[i]) + svValues[i] * coefficient);
        }
    }

    public void set(int idx, double value) {
        this.vec.put(idx, value);
    }

    public double get(int idx) {
        return this.vec.get(idx);
    }

    public double dotProduct (FeatureVector fv) {
        double val = 0.0;
        int fvSize = fv.size();
        for (int i = 0; i < fvSize; i++) {
            Feature f = fv.get(i);
            val += this.vec.get(f.index) * f.value;
        }
        return val;
    }

    public void setZero (FeatureVector fv) {
        double val = 0.0;
        int fvSize = fv.size();
        for (int i = 0; i < fvSize; i++) {
            Feature f = fv.get(i);
            this.vec.put(f.index, 0.0);
        }
    }

    public double dotProduct (SparseVector2 sv) {
        double val = 0.0;

        int[] svIndices = sv.getIndices();
        double[] svValues = sv.getValues();
    
        for (int i = 0; i < svIndices.length; i++) {
            val += this.vec.get(svIndices[i]) * svValues[i];
        }
        return val;
    }

    public double norm(int p) {
        double norm = 0.0;
        double[] values = this.vec.values();
        if (p == 1) {
            for (int i = 0; i < values.length; i++) {
                norm += Math.abs(values[i]);
            }
        } else if (p == 2) {
            for (int i = 0; i < values.length; i++) {
                norm += Math.pow(values[i], 2);
            }
        }
        if (p == 2) {
            norm = Math.sqrt(norm);
        }
        return norm;
    }

    public boolean containsKey(int idx) {
        if (vec.containsKey(idx)) {
            return true;
        } else {
            return false;
        }
    }

    public double getValue(int idx) {
        if (vec.containsKey(idx)) {
            return vec.get(idx);
        } else {
            //return Double.NEGATIVE_INFINITY;
            return 0.0;
        }
    }

    public void remove(int idx) {
        vec.remove(idx);
    }

    public int[] getIndices() {
        return this.vec.keys();
    }

    public double[] getValues() {
        return this.vec.values();
    }

    private String indicesAsStr() {
        String str = new String();
        int[] indices = getIndices();
        for (int i = 0; i < indices.length; i++) {
            str += indices[i];
            if (i != indices.length - 1)
                str += " ";
        }
        return str;
    }

    public void multiply(double value) {
        double[] values = getValues();
        for (int i = 0; i < values.length; i++) {
            values[i] *= value;
        }
    }

    public int size() {
        return this.vec.size();
    }

    public String toString() {
        int[] indices = getIndices();
        double[] values = getValues();
        String str = new String();
        str += "vec: ";
        for (int i = 0; i < indices.length; i++) {
            str += indices[i] + ":" + values[i];
            if (i != indices.length - 1)
                str += " ";
        }
        return str;
    }

    public String toString(AlphabetTrie alp) {
        int[] indices = getIndices();
        double[] values = getValues();
        String str = new String();
        str += "vec: ";
        for (int i = 0; i < indices.length; i++) {
            str += alp.lookupObject(indices[i]) + ":" + values[i];
            if (i != indices.length - 1)
                str += " ";
        }
        return str;
    }

    public static void main (String[] args) {
        SparseVector2 v = new SparseVector2();
    
        int[] indices1 = {0, 1, 3};
        double[] values1 = {1.0, 1.0, 1.0};
        SparseVector2 sv1 = new SparseVector2(indices1, values1);
        System.out.println("sv1:" + sv1.toString());
        v.add(sv1, 0.5);
        int[] indices2 = {1, 2, 3};
        double[] values2 = {1.0, 1.0, 1.0};
        SparseVector2 sv2 = new SparseVector2(indices2, values2);
        System.out.println("sv2:" + sv2.toString());
        v.add(sv2, 0.5);
        System.out.println("v:" + v.toString());
    }
}
