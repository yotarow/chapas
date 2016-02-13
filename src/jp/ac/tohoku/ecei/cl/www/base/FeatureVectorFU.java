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

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.doubles.*;

public class FeatureVectorFU implements Serializable, Cloneable {

    private ArrayList<Feature> features;
    private boolean sorted;
    private AlphabetTrie alp;
    private boolean alphabetGrowth = true;

    public FeatureVectorFU (FeatureVectorFU fv1, FeatureVectorFU fv2) {
        ArrayList<Feature> feats1 = fv1.getFeaturesAry();
        ArrayList<Feature> feats2 = fv2.getFeaturesAry();
        this.features = new ArrayList<Feature>();
        for (int i = 0; i < feats1.size(); i++) {
            this.features.add(feats1.get(i));
        }
        for (int i = 0; i < feats2.size(); i++) {
            this.features.add(feats2.get(i));
        }
        this.sorted = false;
    }
        
    public FeatureVectorFU(AlphabetTrie alphabet, boolean add) {
        this (alphabet, new String[0], add);
    }

    public FeatureVectorFU(AlphabetTrie alphabet, String[] fStr, boolean add) {
        this.sorted = false;
        this.features = new ArrayList<Feature>();
        this.alp = alphabet;
        for (int i = 0; i < fStr.length; i++) {
            int idx = alphabet.lookupIndex(fStr[i], add);
            if (idx >= 0) {
                this.features.add(new Feature(idx, 1.0F));
            }
        }
        this.alphabetGrowth = add;
    }

    public FeatureVectorFU(AlphabetTrie alphabet, ArrayList<String> fAry, boolean add) {
        this.sorted = false;
        this.features = new ArrayList<Feature>();
        this.alp = alphabet;
        int fArySize = fAry.size();
        //System.err.println("add = "+add);
        for (int i = 0; i < fArySize; i++) {
            int idx = alphabet.lookupIndex(fAry.get(i), add);
            //System.err.println(fAry.get(i)+":"+idx);
            if (idx >= 0) {
                this.features.add(new Feature(idx, 1.0F));
            } //else {
            //System.err.println("the feature "+fAry.get(i)+" did not be add");
            //}
        }
        this.alphabetGrowth = add;
    }

    public FeatureVectorFU(AlphabetTrie alphabet) {
        this (alphabet, true);
        //this.alp = alphabet;
        //this.sorted = false;
        //this.features = new ArrayList<Feature>();
    }

    public FeatureVectorFU(AlphabetTrie alphabet, ArrayList<String> fAry, DoubleArrayList values, boolean add) {
        this.sorted = false;
        this.features = new ArrayList<Feature>();
        this.alp = alphabet;
        int fArySize = fAry.size();
        for (int i = 0; i < fArySize; i++) {
            int idx = alphabet.lookupIndex(fAry.get(i), add);
            if (idx >= 0) {
                this.features.add(new Feature(idx, (float) values.getDouble(i)));
            } else {
                //System.err.println("the feature did not be add");
            }
        }
        this.alphabetGrowth = add;
    }

    public FeatureVectorFU(int[] indices) {
        this(indices, null);
    }

    public FeatureVectorFU(int[] indices,  float[] values) {
        this.features = new ArrayList<Feature>();
        if (values != null) {
            for (int i = 0; i < indices.length; i++)
                this.features.add(new Feature(indices[i], values[i]));
        } else {
            for (int i = 0; i < indices.length; i++)
                this.features.add(new Feature(indices[i]));
        }	    
        this.sorted = false;
    }

    public void add(int index) {
        this.add(index, 1.0F);
    }

    public void add(int index, float value) {
        this.features.add(new Feature(index, value));
    }

    public void add (Feature f) {
        this.features.add(f);
    }

    public void add (String feat) {
        int idx = this.alp.lookupIndex(feat, this.alphabetGrowth);
        if (idx >= 0) {
            this.features.add(new Feature(idx, 1.0F));
        }
    }

    public void add (String feat, float value) {
        int idx = this.alp.lookupIndex(feat, this.alphabetGrowth);
        if (idx >= 0) {
            this.features.add(new Feature(idx, value));
        }
    }

    public void add (String feat, double value) {
        int idx = this.alp.lookupIndex(feat, this.alphabetGrowth);
        if (idx >= 0) {
            this.features.add(new Feature(idx, (float) value));
        }
    }

    public void sort() {
        for (int i = 0; i < this.features.size(); i++) {
            for (int j = 1; j < this.features.size() - i; j++) {
                Feature pF = this.features.get(j-1);
                Feature nF = this.features.get(j);
                if (pF.index > nF.index) {
                    this.features.set(j-1, nF);
                    this.features.set(j, pF);
                }
            }
        }
        this.sorted = true;
    }

    public void sortAndMerge() {
        Int2FloatOpenHashMap featHash = new Int2FloatOpenHashMap();
        for (int i = 0; i < this.features.size(); i++) {
            Feature f = this.features.get(i);
            if (featHash.containsKey(f.index)) {
                featHash.put(f.index, featHash.get(f.index)+f.value);
            } else {
                featHash.put(f.index, f.value);
            }
        }
        int[] indices = featHash.keySet().toIntArray();
        Arrays.sort(indices);
        ArrayList<Feature> newFeats = new ArrayList<Feature>();
        for (int i = 0; i < indices.length; i++) {
            newFeats.add(new Feature(indices[i], featHash.get(indices[i])));
        }
        this.features = newFeats;
        this.sorted = true;
    }

    public int size() {
        return this.features.size();
    }

    public Feature get(int index) {
        return this.features.get(index);
    }

    public int[] getIndices() {
        int[] indices = new int[this.features.size()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = this.features.get(i).index;
        }
        return indices;
    }

    public Iterator iterator() {
        return this.features.iterator();
    }

    public Feature[] getFeatures() {
        return (Feature[]) this.features.toArray(new Feature[this.features.size()]);
    }

    public ArrayList<Feature> getFeaturesAry() {
        return this.features;
    }

    public float norm(int p) {
        float value = 0.0F;

        for (int i = 0; i < this.features.size(); i++) {
            Feature f = this.features.get(i);
            if (p == 1)
                value += Math.abs(f.value);
            else if (p == 2) {
                value += Math.pow(f.value, p);
            }
        }
        if (p == 2)
            value = (float) Math.sqrt((double) value);
        return value;
    }
    
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("1 norm: " + this.norm(1) + ", 2 norm: " + this.norm(2));
        str.append(", fv: ");
        int size = this.features.size();
        for (int i = 0; i < size; i++) {
            str.append(((Feature)this.features.get(i)).toString());
            if (i != size - 1)
                str.append(" ");
        }
        return str.toString();
    }

    public String toString(AlphabetTrie alphabet) {
        StringBuilder str = new StringBuilder();
        str.append("1 norm: " + this.norm(1) + ", 2 norm: " + this.norm(2));
        str.append(", fv: ");
        int size = this.features.size();
        for (int i = 0; i < size; i++) {
            Feature f = this.features.get(i);
            str.append( (String) alphabet.lookupObject(f.index)+":"+f.value);
            if (i != size - 1)
                str.append(" ");
        }
        return str.toString();
    }

    /*
    public String toString(LinearModel model, AlphabetTrie alphabet, Labels ls) {
        StringBuilder str = new StringBuilder();
        str.append("1 norm: " + this.norm(1) + ", 2 norm: " + this.norm(2));
        str.append(", fv: ");
        int size = this.features.size();
        for (int i = 0; i < size; i++) {
            Feature f = this.features.get(i);
            str.append( (String) alphabet.lookupObject(f.index)+":"+f.value+"*"+model.getWeight(ls, f.index));
            if (i != size - 1)
                str.append(" ");
        }
        return str.toString();
    }
    */

    private static final long serialVersionUID = 1;
    private static final int CURRENT_SERIAL_VERSION = 1;

    private void writeObject (ObjectOutputStream out) throws IOException {
        int[] indices = new int[this.features.size()];
        for (int i = 0; i < this.features.size(); i++) {
            indices[i] = ((Feature)this.features.get(i)).index;
        }
        out.writeObject(indices);
    }

    private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
        int[] indices = (int[]) in.readObject();
        this.features = new ArrayList<Feature>();
        for (int i = 0; i < indices.length; i++) {
            this.features.add(new Feature(indices[i]));
        }
    }

    public static void main(String[] args) {
        int[] indices1 = {1, 3};
        float[] values1 = {1.0F, 1.0F};
        FeatureVectorFU fv1 = new FeatureVectorFU(indices1, values1);
        System.out.println("fv1:" + fv1.toString());

        int[] indices2 = {2, 3};
        float[] values2 = {1.0F, 1.0F};
        FeatureVectorFU fv2 = new FeatureVectorFU(indices2, values2);
        System.out.println("fv2:" + fv2.toString());

        int[] indices3 = {3, 4};
        float[] values3 = {1.0F, 1.0F};
        FeatureVectorFU fv3 = new FeatureVectorFU(indices3, values3);
        System.out.println("fv3:" + fv3.toString());
    }
}
