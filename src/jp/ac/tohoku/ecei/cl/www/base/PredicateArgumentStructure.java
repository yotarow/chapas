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
import java.io.Serializable;

import gnu.trove.iterator.*;
import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.hash.*;

public class PredicateArgumentStructure implements Serializable, Cloneable {

    public int absPredId = -1;
    public int predicateId;
    public String predicateSense;
    public int[] argumentIds;
    public String[] argumentLabels;
    public double[] argumentScores;
    public double score;
    public String predicateType = null;
    public String voice = null;

    public String attrAll = null;

    public PredicateArgumentStructure(int predicateId, int[] argumentIds, String[] argumentLabels) {
        this (predicateId, null, argumentIds, argumentLabels);
    } 

    public PredicateArgumentStructure(int predicateId, String predicateSense, int[] argumentIds, String[] argumentLabels) {
        this (predicateId, predicateSense, argumentIds, argumentLabels, null);
    } 

    public PredicateArgumentStructure(int predicateId, int[] argumentIds, String[] argumentLabels, double[] argumentScores) {
        this (predicateId, null, argumentIds, argumentLabels, argumentScores);
    }

    public PredicateArgumentStructure(int predicateId, String predicateSense, int[] argumentIds, String[] argumentLabels, double[] argumentScores) {
        this.predicateId = predicateId;
        this.predicateSense = predicateSense;
        this.argumentIds = argumentIds;
        this.argumentLabels = argumentLabels;
        this.argumentScores = argumentScores;
    }

    public void setPredicateId(int id) {
        this.predicateId = id;
    }

    public int getPredicateId() {
        return this.predicateId;
    }

    public String getPredicateSense() {
        return this.predicateSense;
    }

    public int[] getIds() {
        return this.argumentIds;
    }

    public void setIds(int[] ids) {
        this.argumentIds = ids;
    }

    public int getId(int k) {
        return this.argumentIds[k];
    }

    public String[] getLabels() {
        return this.argumentLabels;
    }

    public void setLabels(String[] labels) {
        this.argumentLabels = labels;
    }

    public String getLabel(int k) {
        return this.argumentLabels[k];
    }

    public double getScore(int k) {
        return this.argumentScores[k];
    }

    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("[PredicateArgumentStructure]\n");
        str.append("TYPE="+this.predicateType+" PREDID="+this.predicateId+" PREDSENSE="+this.predicateSense+"\n");
        if (this.argumentIds != null) {
            str.append("ARGUMENTS:");
            for (int i = 0; i < this.argumentIds.length; i++) {
                str.append(" "+this.argumentIds[i]+":"+this.argumentLabels[i]);
            }
        }
        return str.toString();
    }

    public Object clone() {
        int[] cloneArgumentIds = new int[this.argumentIds.length];
        for (int i = 0; i < argumentIds.length; i++) {
            cloneArgumentIds[i] = this.argumentIds[i];
        }

        String[] cloneArgumentLabels = new String[this.argumentLabels.length];
        for (int i = 0; i < argumentLabels.length; i++) {
            cloneArgumentLabels[i] = new String(this.argumentLabels[i]);
        }

        PredicateArgumentStructure newPAS = new PredicateArgumentStructure(predicateId, new String(predicateSense),
                                                                           cloneArgumentIds, cloneArgumentLabels);
        newPAS.score = this.score;
        return newPAS;
    }

    public static PredicateArgumentStructure convert (int predId, String predLabel, LabelAssignment asn) {
        int[] srlIndices = asn.getIndices();
        Label[] srlLabels = asn.getLabels();
        double[] srlScores = asn.getValues();

        TIntArrayList srlIndicesAry = new TIntArrayList();
        ArrayList<String> srlLabelsAry = new ArrayList<String>();
        TDoubleArrayList srlScoresAry = new TDoubleArrayList();

        for (int k = 0; k < srlLabels.length; k++) {
            if (srlLabels[k].toString().equalsIgnoreCase("NONE"))
                continue;
            srlIndicesAry.add(srlIndices[k]);
            srlLabelsAry.add(srlLabels[k].toString());
            srlScoresAry.add(srlScores[k]);
        }
        return new PredicateArgumentStructure(predId, predLabel, 
                                              srlIndicesAry.toArray(),
                                              (String[]) srlLabelsAry.toArray(new String[srlLabelsAry.size()]),
                                              srlScoresAry.toArray());

    }

    public static PredicateArgumentStructure convert (int predId, LabelAssignment asn) {
        int[] srlIndices = asn.getIndices();
        Label[] srlLabels = asn.getLabels();
        double[] srlScores = asn.getValues();

        TIntArrayList srlIndicesAry = new TIntArrayList();
        ArrayList<String> srlLabelsAry = new ArrayList<String>();
        TDoubleArrayList srlScoresAry = new TDoubleArrayList();

        for (int k = 0; k < srlLabels.length; k++) {
            if (srlLabels[k].toString().equalsIgnoreCase("NONE"))
                continue;
            srlIndicesAry.add(srlIndices[k]);
            srlLabelsAry.add(srlLabels[k].toString());
            srlScoresAry.add(srlScores[k]);
        }
        return new PredicateArgumentStructure(predId, 
                                              srlIndicesAry.toArray(),
                                              (String[]) srlLabelsAry.toArray(new String[srlLabelsAry.size()]),
                                              srlScoresAry.toArray());

    }

    public boolean contains(int id) {
        for (int i = 0; i < this.argumentIds.length; i++) {
            if (this.argumentIds[i] == id) {
                return true;
            }
        }
        return false;
    }
}
