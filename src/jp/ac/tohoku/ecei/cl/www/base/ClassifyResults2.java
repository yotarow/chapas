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

public class ClassifyResults2 {

    public ArrayList<Label> labels;
    public TDoubleArrayList values;

    public TIntArrayList constraints; // for constrained n-best search

    public ClassifyResults2() {
        this.labels = new ArrayList<Label>();
        this.values = new TDoubleArrayList();
        this.constraints = new TIntArrayList();
    }

    public ClassifyResults2(Label[] labels, double[] values) {
        this(labels, values, null);
    }

    public ClassifyResults2(Label[] labels, double[] values, int[] constraints) {
        this.labels = new ArrayList();
        for (int i = 0; i < labels.length; i++) {
            this.labels.add(labels[i]);
        }
        this.values = new TDoubleArrayList(values);
        if (constraints != null) {
            this.constraints = new TIntArrayList(constraints);
        } else {
            this.constraints = new TIntArrayList();
            for (int i = 0; i < labels.length; i++) {
                this.constraints.add(0);
            }
        }
    }

    public void add(Label label, double value) {
        this.labels.add(label);
        this.values.add(value);
        this.constraints.add(0);
    }

    public void add(Label label, double value, int constraint) {
        this.labels.add(label);
        this.values.add(value);
        this.constraints.add(constraint);
    }

    public Label getLabel(int idx) {
        return this.labels.get(idx);
    }

    public double getValue(int idx) {
        return this.values.get(idx);
    }

    public Label[] getLabels() {
        return (Label[]) this.labels.toArray(new Label[labels.size()]);
    }

    public double[] getValues() {
        return this.values.toArray();
    }

    public int[] getConstraints() {
        return this.constraints.toArray();
    }

    public void setConstraints(int[] constraints) {
        this.constraints = new TIntArrayList(constraints);
    }

    public Label getArgmax() {
        Label[] labels = (Label[]) this.labels.toArray(new Label[this.labels.size()]);
        double[] values = this.values.toArray();
        int[] constraints = this.constraints.toArray();

        Label argmax = null;
        double max = Double.NEGATIVE_INFINITY;
        int cnt = 0;
        for (Iterator it = this.labels.iterator(); it.hasNext();) {
            Label l = (Label) it.next();
            if (constraints[cnt] == 0 && max < values[cnt]) {
                max = values[cnt];
                argmax = l;
            }
            cnt++;
        }
        return argmax;
    }

    public void normalize(boolean exp) {
        double[] values = this.values.toArray();
        double denom = 0.0;
        for (int l = 0; l < values.length; l++) {
            values[l] = exp ? Math.exp(values[l]) : values[l];
            denom += values[l];
        }
        for (int l = 0; l < values.length; l++) {
            values[l] /= denom;
        }
        this.values = new TDoubleArrayList(values);
    }

    public String toString() {
        double[] values = this.values.toArray();
        StringBuilder str = new StringBuilder();
        str.append("[ClassifyResults2]\n"); 
        int cnt = 0;
        for (Iterator it = this.labels.iterator(); it.hasNext();) {
            Label l = (Label)it.next();
            str.append("group="+l.getGroup()+" label="+l.toString()+" value="+values[cnt]+"\n");
            cnt++;
        }
        return str.toString();
    }

    public ClassifyResults2 getNBest(int n) {
        ArrayList<Label> nBestLabels = new ArrayList<Label>();
        TDoubleArrayList nBestValues = new TDoubleArrayList();

        Label[] labels = (Label[]) this.labels.toArray(new Label[this.labels.size()]);
        double[] values = this.values.toArray();
        int[] constraints = this.constraints.toArray();

        boolean[] added = new boolean[labels.length];

        for (int i = 0; i < added.length; i++) {
            added[i] = false;
        }

        int k = 0;
        while (k < n && k < labels.length) {
            Label argmax = null;
            int argmaxIdx = -1;
            double max = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < labels.length; i++) {
                if (constraints[i] == 0 && !added[i] && max < values[i]) {
                    max = values[i];
                    argmax = labels[i];
                    argmaxIdx = i;
                }
            }
            nBestLabels.add(argmax);
            nBestValues.add(max);
            added[argmaxIdx] = true;
            k++;
        }
        return new ClassifyResults2((Label[])nBestLabels.toArray(new Label[nBestLabels.size()]), nBestValues.toArray());
    }

    public static void main (String[] args) {
        ClassifyResults2 cr = new ClassifyResults2();
        Alphabet la = new Alphabet();
        int gIdx = 1;
        Label l1 = new Label(gIdx, la, la.lookupIndex("A0"));
        Label l2 = new Label(gIdx, la, la.lookupIndex("A1"));
        Label l3 = new Label(gIdx, la, la.lookupIndex("A2"));
        Label l4 = new Label(gIdx, la, la.lookupIndex("A3"));
        cr.add(l1, 15);
        cr.add(l2, 25, 1);
        cr.add(l3, 55);
        cr.add(l4, 5);
        System.out.println(cr.toString());
        System.out.println("getArgmax()=" + cr.getArgmax().toString());
        ClassifyResults2 nBest = cr.getNBest(2);
        System.out.println("2 best: " + nBest.toString());
        cr.normalize(true);
        System.out.println(cr.toString());
        System.out.println("getArgmax()=" + cr.getArgmax().toString());
        ClassifyResults2 normalizedNBest = cr.getNBest(2);
        System.out.println("2 best: " + normalizedNBest.toString());
    }
}
