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

import java.util.ArrayList;
import java.util.Iterator;

import gnu.trove.iterator.*;
import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.hash.*;

public class IntDoublePairs {

    public TIntArrayList ids;
    public TDoubleArrayList values;

    public TIntArrayList constraints; // for constrained n-best search

    public IntDoublePairs () {
        this.ids = new TIntArrayList();
        this.values = new TDoubleArrayList();
        this.constraints = new TIntArrayList();
    }

    public IntDoublePairs (int[] ids, double[] values) {
        this(ids, values, null);
    }

    public IntDoublePairs (int[] ids, double[] values, int[] constraints) {
        this.ids = new TIntArrayList();
        for (int i = 0; i < ids.length; i++) {
            this.ids.add(ids[i]);
        }
        this.values = new TDoubleArrayList(values);
        if (constraints != null) {
            this.constraints = new TIntArrayList(constraints);
        } else {
            this.constraints = new TIntArrayList();
            for (int i = 0; i < ids.length; i++) {
                this.constraints.add(0);
            }
        }
    }

    public void add (int id, double value) {
        this.ids.add(id);
        this.values.add(value);
        this.constraints.add(0);
    }

    public void add (int id, double value, int constraint) {
        this.ids.add(id);
        this.values.add(value);
        this.constraints.add(constraint);
    }

    public int getId (int idx) {
        return this.ids.get(idx);
    }

    public double getValue(int idx) {
        return this.values.get(idx);
    }

    public int[] getIds() {
        return this.ids.toArray();
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

    public int getArgmax() {
        int argmax = -1;
        double max = Double.NEGATIVE_INFINITY;
        int cnt = 0;
        for (int i = 0; i < ids.size(); i++) {
            int id = ids.get(i);
            double value = values.get(i);
            if (constraints.get(i) == 0 && max < value) {
                max = value;
                argmax = id;
            }
        }
        return argmax;
    }

    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("[IntDoublePairs]\n"); 
        int cnt = 0;
        for (int i = 0; i < ids.size(); i++) {
            str.append("id="+ids.get(i)+" value="+values.get(i)+"\n");
        }
        return str.toString();
    }
}
