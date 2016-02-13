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

import java.util.ArrayList;
import java.util.Iterator;

import gnu.trove.iterator.*;
import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.hash.*;

public class IntAssignment {

    public TIntArrayList indices;
    public TIntArrayList ints;
    public TDoubleArrayList values;
    public double score;
    public boolean sorted;
    public boolean exp; // score is probability or not

    public IntAssignment() {
        this.indices = new TIntArrayList();
        this.ints = new TIntArrayList();
        this.values = new TDoubleArrayList();
        this.sorted = false;
    }

    public IntAssignment (int[] indices, int[] ints) {
        this(indices, ints, (double[]) null);
    }

    public IntAssignment (int[] indices, int[] ints, double[] values) {
        this(indices, ints, values, false);
    }

    public IntAssignment (int[] indices, int[] ints, double[] values, boolean exp) {
        assert indices.length == ints.length && indices.length == values.length;
        this.indices = new TIntArrayList(indices);
        this.ints = new TIntArrayList(ints);
        if (values != null) {
            this.values = new TDoubleArrayList(values);
        } else {
            this.values = new TDoubleArrayList();
            for (int i = 0; i < indices.length; i++) {
                this.values.add(0.0);
            }
        }
        this.sorted = false;
        this.exp = exp;
        this.calcScore();
    }

    public void add(int index, int intVal) {
        this.add(index, intVal, 1.0);
    }

    public boolean contains (int intVal) {
        for (int i = 0; i < this.ints.size(); i++) {
            if (this.ints.get(i) == intVal) {
                return true;
            }
        }
        return false;
    }

    public void add (int index, int intVal, double value) {
        this.indices.add(index);
        this.ints.add(intVal);
        this.values.add(value);
        this.sorted = false;
        this.calcScore();
    }

    public void calcScore() {
        if (this.exp) {
            this.score = 1;
            double[] values = this.values.toArray();
            for (int i = 0; i < values.length; i++) {
                this.score *= values[i];
            }
        } else {
            this.score = 0;
            double[] values = this.values.toArray();
            for (int i = 0; i < values.length; i++) {
                this.score += values[i];
            }
        }
    }

    public int[] getIndices() {
        if (!this.sorted) { this.sort(); }
        return this.indices.toArray();
    }

    public int[] getInts() {
        if (!this.sorted) { this.sort(); }
        return this.ints.toArray();
    }

    public double[] getValues() {
        if (!this.sorted) { this.sort(); }
        return this.values.toArray();
    }

    public double getScore() {
        return this.score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public void sort() {
        int[] indices = this.indices.toArray();
        int[] ints = this.ints.toArray();
        double[] values = this.values.toArray();

        for (int i = 0; i < indices.length - 1; i++) {
            for (int j = 1; j < indices.length - i; j++) {
                if (indices[j-1] > indices[j]) {
                    int idx_tmp = indices[j];
                    indices[j] = indices[j-1];
                    indices[j-1] = idx_tmp;
                    int label_tmp = ints[j];
                    ints[j] = ints[j-1];
                    ints[j-1] = label_tmp;
                    double val_tmp = values[j];
                    values[j] = values[j-1];
                    values[j-1] = val_tmp;
                }
            }
        }
        this.sorted = true;
        this.indices = new TIntArrayList(indices);
        this.ints = new TIntArrayList(ints);
        this.values = new TDoubleArrayList(values);
    }

    public boolean equals(IntAssignment asn) {

        int[] asnIndices = asn.getIndices();
        int[] asnInts = asn.getInts();

        TIntHashSet asnIndicesHashSet = new TIntHashSet(asnIndices);
        TIntIntHashMap asnHash = new TIntIntHashMap();

        for (int i = 0; i < asnIndices.length; i++) {
            asnHash.put(asnIndices[i], asnInts[i]);
        }

        boolean equal = true;
        int[] indices = this.indices.toArray();
        int[] ints = this.ints.toArray();
        for (int i = 0; i < indices.length; i++) {
            if (asnHash.containsKey(indices[i])) {
                asnIndicesHashSet.remove(indices[i]);
                int intVal = asnHash.get(indices[i]);
                if (ints[i] != intVal) {
                    return false;
                }
            } else {
                return false;
            }
        }
        if (equal && asnIndicesHashSet.size() == 0) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        if (!this.sorted) { this.sort(); }
        StringBuilder str = new StringBuilder();
        str.append("[Assignment] score="+this.score+" ");
        int[] indices = this.getIndices();
        int[] ints = this.getInts();
        double[] values = this.getValues();
        for (int i = 0; i < indices.length; i++) {
            str.append("INDEX="+indices[i]+",LABEL="+ints[i]+",VALUE="+values[i]+" ");
        }
        return str.toString();
    }

    public int size() {
        return this.indices.size();
    }
}
