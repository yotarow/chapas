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

import java.io.Serializable;

import gnu.trove.iterator.*;
import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.hash.*;

public class FeatureSet implements Serializable {

    private TIntHashSet featsSet;
    private double value;

    public FeatureSet () {
        this (new TIntHashSet());
    }

    public FeatureSet (int[] feats) {
        this (new TIntHashSet(feats));
    }

    public FeatureSet (TIntHashSet feats) {
        this.featsSet = feats;
    }

    public void add (int feat) {
        this.featsSet.add(feat);
    }

    public void remove (int feat) {
        this.featsSet.remove(feat);
    }

    public boolean contains (int fIdx) {
        return this.featsSet.contains(fIdx);
    }

    /*
    public TIntHashSet getFeatureSet() {
        return (TIntHashSet) this.featsSet;
    }
    */

    public void setValue(double value) {
        this.value = value;
    }
    
    public double getValue() {
        return this.value;
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("[FeatureSet]");
        s.append(new TIntArrayList(featsSet.toArray()));
        //s.append(" value="+value);
        return s.toString();
    }
}
