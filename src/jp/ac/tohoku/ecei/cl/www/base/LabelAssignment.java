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

public class LabelAssignment {

    public TIntArrayList indices;
    public ArrayList<Label> labels;
    public TDoubleArrayList values;
    public double score;
    public boolean sorted;
    public boolean exp; // score is probability or not

    public LabelAssignment() {
        this.indices = new TIntArrayList();
        this.labels = new ArrayList<Label>();
        this.values = new TDoubleArrayList();
        this.sorted = false;
    }

    public LabelAssignment (int[] indices, Label[] labels) {
        this(indices, labels, (double[]) null);
    }

    public LabelAssignment (int[] indices, Label[] labels, double[] values) {
        this(indices, labels, values, false);
    }

    public LabelAssignment (int[] indices, Label[] labels, double[] values, boolean exp) {
        assert indices.length == labels.length && indices.length == values.length;
        this.indices = new TIntArrayList(indices);
        this.labels = new ArrayList<Label>();
        for (int i = 0; i < labels.length; i++) {
            this.labels.add(labels[i]);
        }
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

    public void add(int index, Label label) {
        this.add(index, label, 1.0);
    }

    public boolean contains (String labelStr) {
        for (int i = 0; i < this.labels.size(); i++) {
            Label l = this.labels.get(i);
            if (l.toString().equals(labelStr)) {
                return true;
            }
        }
        return false;
    }

    public void add(int index, Label label, double value) {
        this.indices.add(index);
        this.labels.add(label);
        this.values.add(value);
        this.sorted = false;
        this.calcScore();
    }

    public int[] getParticularGroupIndices(int groupIdx) {
        if (!this.sorted) { this.sort(); }
        TIntArrayList indices = new TIntArrayList();
        int cnt = 0;
        for (Iterator it = this.labels.iterator(); it.hasNext();) {
            Label l = (Label) it.next();
            if (l.getGroup() == groupIdx) {
                indices.add(this.indices.get(cnt));
            }
            cnt++;
        }
        return indices.toArray();
    }

    public Label[] getParticularGroupLabels(int groupIdx) {
        if (!this.sorted) { this.sort(); }
        ArrayList<Label> labels = new ArrayList<Label>();
        int cnt = 0;
        for (Iterator it = this.labels.iterator(); it.hasNext();) {
            Label l = (Label) it.next();
            if (l.getGroup() == groupIdx) {
                labels.add(l);
            }
            cnt++;
        }
        return (Label[]) labels.toArray(new Label[labels.size()]);
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

    public Label[] getLabels() {
        if (!this.sorted) { this.sort(); }
        return (Label[]) this.labels.toArray(new Label[this.labels.size()]);
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
        Label[] labels = (Label[]) this.labels.toArray(new Label[this.labels.size()]);
        double[] values = this.values.toArray();

        for (int i = 0; i < indices.length - 1; i++) {
            for (int j = 1; j < indices.length - i; j++) {
                if (indices[j-1] > indices[j]) {
                    int idx_tmp = indices[j];
                    indices[j] = indices[j-1];
                    indices[j-1] = idx_tmp;
                    Label label_tmp = labels[j];
                    labels[j] = labels[j-1];
                    labels[j-1] = label_tmp;
                    double val_tmp = values[j];
                    values[j] = values[j-1];
                    values[j-1] = val_tmp;
                }
            }
        }
        this.sorted = true;
        this.indices = new TIntArrayList(indices);
        this.labels = new ArrayList<Label>();
        for (int i = 0; i < labels.length; i++) {
            this.labels.add(labels[i]);
        }
        this.values = new TDoubleArrayList(values);
    }

    public boolean equals(LabelAssignment asn) {

        int[] asnIndices = asn.getIndices();
        Label[] asnLabels = asn.getLabels();

        TIntHashSet asnIndicesHashSet = new TIntHashSet(asnIndices);
        TIntObjectHashMap asnHash = new TIntObjectHashMap();

        for (int i = 0; i < asnIndices.length; i++) {
            asnHash.put(asnIndices[i], asnLabels[i]);
        }

        boolean equal = true;
        int[] indices = this.indices.toArray();
        Label[] labels = (Label[]) this.labels.toArray(new Label[this.labels.size()]);
        for (int i = 0; i < indices.length; i++) {
            if (asnHash.containsKey(indices[i])) {
                asnIndicesHashSet.remove(indices[i]);
                Label label = (Label) asnHash.get(indices[i]);
                if (!labels[i].equals(label)) {
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
        Label[] labels = this.getLabels();
        double[] values = this.getValues();
        for (int i = 0; i < indices.length; i++) {
            str.append("INDEX="+indices[i]+",LABEL="+labels[i].toString()+",VALUE="+values[i]+" ");
        }
        return str.toString();
    }

    public int size() {
        return this.indices.size();
    }

    public static void main (String[] args) {
	
        LabelAssignment asn = new LabelAssignment();
        Alphabet la = new Alphabet();
        Alphabet la2 = new Alphabet();
        int gIdx = 1;
        int gIdx2 = 2;
        asn.add(1,new Label(gIdx, la, la.lookupIndex("NONE")),4.0);
        asn.add(0,new Label(gIdx, la, la.lookupIndex("A1")),1.0);
        asn.add(3,new Label(gIdx, la, la.lookupIndex("A2")),3.0);
        asn.add(2,new Label(gIdx2, la2, la2.lookupIndex("have.03")),2.0);

        LabelAssignment asn2 = new LabelAssignment();
        asn2.add(1,new Label(gIdx, la, la.lookupIndex("NONE")),4.0);
        asn2.add(0,new Label(gIdx, la, la.lookupIndex("A1")),1.0);
        asn2.add(3,new Label(gIdx, la, la.lookupIndex("A2")),3.0);
        asn2.add(2,new Label(gIdx2, la2, la2.lookupIndex("have.03")),3.0);

        Label[] ls = asn.getParticularGroupLabels(1);
        System.out.println("ls.length="+ls.length);
        System.out.println("asn.toString()");
        System.out.println(asn.toString());
        System.out.println("asn2.toString()");
        System.out.println(asn2.toString());
        System.out.println("asn.equals(asn2)="+asn.equals(asn2));
    }
}
