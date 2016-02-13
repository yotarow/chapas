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
import java.util.Iterator;

import jp.ac.tohoku.ecei.cl.www.base.*;
import jp.ac.tohoku.ecei.cl.www.util.*;

public class LinearModel implements Serializable {
    public static double EPS = 10e-30;

    protected Labels2ObjectHashMap<SparseVector2> w; 
    protected Labels2ObjectHashMap<SparseVector2> wa; // for averaging

    // fields for parameter averaging
    protected boolean averaged = false;
    protected int ca = 1;

    public LinearModel () {
        this.w = new Labels2ObjectHashMap();
    }

    protected void setAveraged() {
        this.averaged = true;
        this.wa = new Labels2ObjectHashMap();
    }
    
    public double dotProduct(FeatureVector fv, Label label) {
        if (!this.w.containsKey(label)) {
            return 0.0;
        }
        if (this.averaged) {
            return ((SparseVector2)this.w.get(label)).dotProduct(fv) - ((SparseVector2)this.wa.get(label)).dotProduct(fv)/this.ca;
        } else {
            return ((SparseVector2)this.w.get(label)).dotProduct(fv);
        }
    } 

    public double dotProduct(FeatureVector fv, Labels labels) {
        if (!this.w.containsKey(labels)) {
            return 0.0;
        }
        if (this.averaged) {
            return ((SparseVector2)this.w.get(labels)).dotProduct(fv) - ((SparseVector2)this.wa.get(labels)).dotProduct(fv)/this.ca;
        } else {
            return ((SparseVector2)this.w.get(labels)).dotProduct(fv);
        }
    } 
    
    public void clear() {
        this.w = new Labels2ObjectHashMap();
        this.wa = new Labels2ObjectHashMap();
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("[LinearModel] w: ");
        s.append(w.toString());
        return s.toString();
    }

    public void deleteZeroWeights() {
        Object[] weights = this.w.getObjects();
        for (int i = 0; i < weights.length; i++) {
            SparseVector2 w = (SparseVector2) weights[i];
            int[] indices = w.getIndices();
            for (int j = 0; j < indices.length; j++) {
                if (w.get(indices[j]) == 0.0) {
                    w.remove(indices[j]);
                }
            }
        }
        if (this.averaged) {
            Object[] waAry = this.wa.getObjects();
            for (int i = 0; i < waAry.length; i++) {
                SparseVector2 wa = (SparseVector2) waAry[i];
                int[] indices = wa.getIndices();
                for (int j = 0; j < indices.length; j++) {
                    if (wa.get(indices[j]) == 0.0) {
                        wa.remove(indices[j]);
                    }
                }
            }
        }
    }

    public void compress() {
        Object[] wAry = this.w.getObjects();
        Object[] waAry = this.w.getObjects();
        for (int i = 0; i < wAry.length; i++) {
            SparseVector2 w = (SparseVector2) wAry[i];
            SparseVector2 wa = (SparseVector2) waAry[i];
            int[] indices = w.getIndices();
            for (int j = 0; j < indices.length; j++) {
                w.set(indices[j], w.get(indices[j]) - wa.get(indices[j])/this.ca);
            }
            this.wa.clear();
        }
        this.wa = null;
        this.averaged = false;
    }
    
    public int getTotalNonzeroWeights() {
        int total = 0;
        Object[] weights = this.w.getObjects();
        for (int i = 0; i < weights.length; i++) {
            total += ((SparseVector2)weights[i]).size();
        }
        return total;
    }

    public Iterator<Labels> getLabelsIterator() {
        return this.w.getLabelsIterator();
    }

    public Labels[] getLabels() {
        return this.w.getLabels();
    }

    public String printWeights (AlphabetTrie alp, FeatureVector fv, Label l) {
        StringBuilder s = new StringBuilder();
        SparseVector2 wVec = (SparseVector2) this.w.get(l);
        SparseVector2 waVec = null;
        if (this.averaged) {
            waVec = (SparseVector2) this.wa.get(l);
        }
        Feature[] features = fv.getFeatures();
        s.append(" "+"total:"+this.dotProduct(fv, l));
        for (int j = 0; j < features.length; j++) {
            if (features[j].index == -1) {
                continue; 
            }
            Object f = alp.lookupObject(features[j].index);
            //if (f == null) { continue; }
            if (this.averaged) {
                s.append(" "+f+":"+(wVec.get(features[j].index)-waVec.get(features[j].index)/this.ca)+"*"+features[j].value);
            } else {
                s.append(" "+f+":"+wVec.get(features[j].index)+"*"+features[j].value);
            }
        }
        s.append("\n");
        return s.toString();
    }

    public String printWeights (AlphabetTrie alp, FeatureVector fv, Labels l) {
        StringBuilder s = new StringBuilder();
        SparseVector2 wVec = (SparseVector2) this.w.get(l);
        SparseVector2 waVec = null;
        if (this.averaged) {
            waVec = (SparseVector2) this.wa.get(l);
        }
        Feature[] features = fv.getFeatures();
        s.append(" "+"total:"+this.dotProduct(fv, l));
        for (int j = 0; j < features.length; j++) {
            if (features[j].index == -1) {
                continue; 
            }
            Object f = alp.lookupObject(features[j].index);
            if (f == null) { continue; }
            if (this.averaged) {
                s.append(" "+f+":"+(wVec.get(features[j].index)-waVec.get(features[j].index)/this.ca)+"*"+features[j].value);
            } else {
                s.append(" "+f+":"+wVec.get(features[j].index)+"*"+features[j].value);
            }
        }
        s.append("\n");
        return s.toString();
    }
}
