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

package jp.ac.tohoku.ecei.cl.www.chapas;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import gnu.trove.iterator.*;
import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.hash.*;

import jp.ac.tohoku.ecei.cl.www.eval.*;
import jp.ac.tohoku.ecei.cl.www.base.*;
import jp.ac.tohoku.ecei.cl.www.util.*;
import jp.ac.tohoku.ecei.cl.www.io.*;

public class Eval {

    public static File goldFile = null;
    public static File sysFile = null;
    public static File goldDir = null;
    public static File sysDir = null;

    // evaluators 
    public Evaluator predE;
    public Evaluator labE;
    public Evaluator unlabE;
    public Evaluator pLabE;
    public Evaluator nLabE;

    public Evaluator depE;
    public Evaluator sameBunE;
    public Evaluator intraSentE;
    public Evaluator interSentE;

    public Evaluator pDepE;
    public Evaluator pSameBunE;
    public Evaluator pIntraSentE;
    public Evaluator pInterSentE;

    public Evaluator nDepE;
    public Evaluator nSameBunE;
    public Evaluator nIntraSentE;
    public Evaluator nInterSentE;
    
    public Alphabet labelAlphabet;
    public IntInt2IntHashMap lEr;

    public enum DepType {DEP, ZERO_INTRA, ZERO_INTER, SAME_PHRASE}

    public Eval () {
        // evaluators 
        this.predE = new Evaluator();

        this.labE = new Evaluator();
        this.unlabE = new Evaluator();
        this.pLabE = new Evaluator();
        this.nLabE = new Evaluator();

        this.depE = new Evaluator();
        this.sameBunE = new Evaluator();
        this.intraSentE = new Evaluator();
        this.interSentE = new Evaluator();

        this.pDepE = new Evaluator();
        this.pSameBunE = new Evaluator();
        this.pIntraSentE = new Evaluator();
        this.pInterSentE = new Evaluator();

        this.nDepE = new Evaluator();
        this.nSameBunE = new Evaluator();
        this.nIntraSentE = new Evaluator();
        this.nInterSentE = new Evaluator();
        this.labelAlphabet = new Alphabet();
        this.lEr = new IntInt2IntHashMap();
    }

    public Evaluator getPredE() {
        return this.predE;
    }

    public Evaluator getLabE() {
        return this.labE;
    }

    public Evaluator getPLabE() {
        return this.pLabE;
    }
    
    public Evaluator getPDepE() {
        return this.pDepE;
    }

    public Evaluator getPSameBunE() {
        return this.pSameBunE;
    }

    public Evaluator getPIntraSentE() {
        return this.pIntraSentE;
    }

    public Evaluator getNLabE() {
        return this.nLabE;
    }

    public Evaluator getNDepE() {
        return this.nDepE;
    }

    public Evaluator getNSameBunE() {
        return this.nSameBunE;
    }

    public Evaluator getNIntraSentE() {
        return this.nIntraSentE;
    }

    public Alphabet getLabelAlphabet() {
        return this.labelAlphabet;
    }

    public DepType getDepType (DependencyTree tree, int pIdx, int aIdx) {
        Bunsetsu pBun = tree.getBunsetsuFromNodeId(pIdx);
        Bunsetsu aBun = tree.getBunsetsuFromNodeId(aIdx);
        Bunsetsu pBunHeadBun = tree.getBunsetsuFromId(pBun.getHead());
        Bunsetsu aBunHeadBun = tree.getBunsetsuFromId(aBun.getHead());
        if (pBun.getId() == aBun.getId()) { 
            return DepType.SAME_PHRASE;
        } else if (pBun.getId() == aBunHeadBun.getId()) {
            return DepType.DEP;
        } else if (pBunHeadBun.getId() == aBunHeadBun.getId()) {
            return DepType.DEP;
        } else {
            return DepType.ZERO_INTRA;
        }
    }

    public void getResults(DependencyTree goldTree, DependencyTree systemTree) {

        PredicateArgumentStructure[] goldPASList = goldTree.getPASList();
        PredicateArgumentStructure[] systemPASList = systemTree.getPASList();

        TIntHashSet goldPredIds = new TIntHashSet();
        TIntHashSet sysPredIds = new TIntHashSet();

        for (int i = 0; i < goldPASList.length; i++) {
            PredicateArgumentStructure goldPAS = goldPASList[i];
            goldPredIds.add(goldPAS.getPredicateId());
            this.predE.addTarget(1);
        }

        for (int i = 0; i < systemPASList.length; i++) {
            PredicateArgumentStructure sysPAS = systemPASList[i];
            int sysPredId = sysPAS.getPredicateId();
            sysPredIds.add(sysPredId);
            this.predE.addReturned(1);
            if (goldPredIds.contains(sysPredId)) {
                this.predE.addCorrect(1);
            }
        }
             
        // calc target (N)
        for (int j = 0; j < goldPASList.length; j++) {
            int predId = goldPASList[j].getPredicateId();
            String predType = goldPASList[j].predicateType;
            int[] aIds = goldPASList[j].getIds();
            String[] aLabels = goldPASList[j].getLabels();
            for (int k = 0; k < aIds.length; k++) {
                int aLabelAsInt = labelAlphabet.lookupIndex(aLabels[k].toLowerCase(), true);
                this.labE.addTarget(aLabelAsInt);
                DepType dt = getDepType(goldTree, predId, aIds[k]);
                if (predType.equals("pred")) {
                    this.pLabE.addTarget(aLabelAsInt);
                } else if (predType.equals("event") || predType.equals("noun")) {
                    this.nLabE.addTarget(aLabelAsInt);
                }
                if (dt == DepType.DEP) {
                    if (predType.equals("pred")) {
                        this.pDepE.addTarget(aLabelAsInt);
                    } else if (predType.equals("event") || predType.equals("noun")) {
                        this.nDepE.addTarget(aLabelAsInt);
                    }
                    this.depE.addTarget(aLabelAsInt);
                } else if (dt == DepType.SAME_PHRASE) {
                    if (predType.equals("pred")) {
                        this.pSameBunE.addTarget(aLabelAsInt);
                    } else if (predType.equals("event") || predType.equals("noun")) {
                        this.nSameBunE.addTarget(aLabelAsInt);
                    }
                    this.sameBunE.addTarget(aLabelAsInt);
                } else if (dt == DepType.ZERO_INTRA) {
                    if (predType.equals("pred")) {
                        this.pIntraSentE.addTarget(aLabelAsInt);
                    } else if (predType.equals("event") || predType.equals("noun")) {
                        this.nIntraSentE.addTarget(aLabelAsInt);
                    }
                    this.intraSentE.addTarget(aLabelAsInt);
                } else {
                    if (predType.equals("pred")) {
                        this.pInterSentE.addTarget(aLabelAsInt);
                    } else if (predType.equals("event") || predType.equals("noun")) {
                        this.nInterSentE.addTarget(aLabelAsInt);
                    }
                    this.interSentE.addTarget(aLabelAsInt);
                }
            }
        }

        // calc returned
        for (int j = 0; j < systemPASList.length; j++) {
            int predId = systemPASList[j].getPredicateId();
            String predType = systemPASList[j].predicateType;
            int[] aIds = systemPASList[j].getIds();
            String[] aLabels = systemPASList[j].getLabels();
            for (int k = 0; k < aIds.length; k++) {
                int aLabelAsInt = labelAlphabet.lookupIndex(aLabels[k].toLowerCase(), true);
                this.labE.addReturned(aLabelAsInt);
                DepType dt = getDepType(goldTree, predId, aIds[k]);
                if (predType.equals("pred")) {
                    this.pLabE.addReturned(aLabelAsInt);
                } else if (predType.equals("event") || predType.equals("noun")) {
                    this.nLabE.addReturned(aLabelAsInt);
                }

                if (dt == DepType.DEP) {
                    if (predType.equals("pred")) {
                        this.pDepE.addReturned(aLabelAsInt);
                    } else if (predType.equals("event") || predType.equals("noun")) {
                        this.nDepE.addReturned(aLabelAsInt);
                    }
                    this.depE.addReturned(aLabelAsInt);
                } else if (dt == DepType.SAME_PHRASE) {
                    if (predType.equals("pred")) {
                        this.pSameBunE.addReturned(aLabelAsInt);
                    } else if (predType.equals("event") || predType.equals("noun")) {
                        this.nSameBunE.addReturned(aLabelAsInt);
                    }
                    this.sameBunE.addReturned(aLabelAsInt);
                } else if (dt == DepType.ZERO_INTRA) {
                    if (predType.equals("pred")) {
                        this.pIntraSentE.addReturned(aLabelAsInt);
                    } else if (predType.equals("event") || predType.equals("noun")) {
                        this.nIntraSentE.addReturned(aLabelAsInt);
                    }
                    this.intraSentE.addReturned(aLabelAsInt);
                } else {
                    if (predType.equals("pred")) {
                        this.pInterSentE.addReturned(aLabelAsInt);
                    } else if (predType.equals("event") || predType.equals("noun")) {
                        this.nInterSentE.addReturned(aLabelAsInt);
                    }
                    this.interSentE.addReturned(aLabelAsInt);
                }
            }
        }

        // calc correct
        for (int sIdx = 0; sIdx < systemPASList.length; sIdx++) {
            int sPredId = systemPASList[sIdx].getPredicateId();
            String predType = systemPASList[sIdx].predicateType;
            for (int gIdx = 0; gIdx < goldPASList.length; gIdx++) {
                if (sPredId == goldPASList[gIdx].getPredicateId()) {
                    TIntIntHashMap goldHash = new TIntIntHashMap();
                    int[] gArgIds = goldPASList[gIdx].getIds();
                    String[] gArgLabels = goldPASList[gIdx].getLabels();
                    for (int gArgIdx = 0; gArgIdx < gArgIds.length; gArgIdx++) {
                        int gArgBId = goldTree.getBunsetsuFromNodeId(gArgIds[gArgIdx]).getId();
                        //goldHash.put(gArgIds[gArgIdx], labelAlphabet.lookupIndex(gArgLabels[gArgIdx].toLowerCase()));
                        goldHash.put(gArgBId, labelAlphabet.lookupIndex(gArgLabels[gArgIdx].toLowerCase()));
                    }
			    
                    // for debugging
                    int[] keys = goldHash.keys();
                    int[] sArgIds = systemPASList[sIdx].getIds();
                    String[] sArgLabels = systemPASList[sIdx].getLabels();
                    int[] sArgLabelsAsInt = new int[sArgLabels.length];
                    for (int sArgIdx = 0; sArgIdx < sArgIds.length; sArgIdx++) {
                        int sArgBId = systemTree.getBunsetsuFromNodeId(sArgIds[sArgIdx]).getId();
                        sArgLabelsAsInt[sArgIdx] = labelAlphabet.lookupIndex(sArgLabels[sArgIdx].toLowerCase());
                        //if (goldHash.containsKey(sArgIds[sArgIdx])) { //node evaluation
                        if (goldHash.containsKey(sArgBId)) { // bunsetsu evaluation
                            // true
                            //int goldLabelAsInt = goldHash.get(sArgIds[sArgIdx]); // node evaluation
                            int goldLabelAsInt = goldHash.get(sArgBId); // bunsetsu evaluation
                            if (sArgLabelsAsInt[sArgIdx] == goldLabelAsInt) {
                                this.labE.addCorrect(sArgLabelsAsInt[sArgIdx]);
                                DepType dt = getDepType(goldTree, sPredId, sArgIds[sArgIdx]);
                                if (predType.equals("pred")) {
                                    this.pLabE.addCorrect(sArgLabelsAsInt[sArgIdx]);
                                } else if (predType.equals("event") || predType.equals("noun")) {
                                    this.nLabE.addCorrect(sArgLabelsAsInt[sArgIdx]);
                                }
                                if (dt == DepType.DEP) {
                                    if (predType.equals("pred")) {
                                        this.pDepE.addCorrect(sArgLabelsAsInt[sArgIdx]);
                                    } else if (predType.equals("event") || predType.equals("noun")) {
                                        this.nDepE.addCorrect(sArgLabelsAsInt[sArgIdx]);
                                    }
                                    this.depE.addCorrect(sArgLabelsAsInt[sArgIdx]);
                                } else if (dt == DepType.SAME_PHRASE) {
                                    if (predType.equals("pred")) {
                                        this.pSameBunE.addCorrect(sArgLabelsAsInt[sArgIdx]);
                                    } else if (predType.equals("event") || predType.equals("noun")) {
                                        this.nSameBunE.addCorrect(sArgLabelsAsInt[sArgIdx]);
                                    }
                                    this.sameBunE.addCorrect(sArgLabelsAsInt[sArgIdx]);
                                } else if (dt == DepType.ZERO_INTRA) {
                                    if (predType.equals("pred")) {
                                        this.pIntraSentE.addCorrect(sArgLabelsAsInt[sArgIdx]);
                                    } else if (predType.equals("event") || predType.equals("noun")) {
                                        this.nIntraSentE.addCorrect(sArgLabelsAsInt[sArgIdx]);
                                    }
                                    this.intraSentE.addCorrect(sArgLabelsAsInt[sArgIdx]);
                                } else {
                                    if (predType.equals("pred")) {
                                        this.pInterSentE.addCorrect(sArgLabelsAsInt[sArgIdx]);
                                    } else if (predType.equals("event") || predType.equals("noun")) {
                                        this.nInterSentE.addCorrect(sArgLabelsAsInt[sArgIdx]);
                                    }
                                    this.interSentE.addCorrect(sArgLabelsAsInt[sArgIdx]);
                                }
                            } else {
                                // false
                            }
                            if (!lEr.containsKey(goldLabelAsInt, sArgLabelsAsInt[sArgIdx])) {
                                lEr.put(goldLabelAsInt, sArgLabelsAsInt[sArgIdx], 0);
                            }
                            lEr.increment(goldLabelAsInt, sArgLabelsAsInt[sArgIdx]);
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        processArguments(args);

        // label(String) 2 label(Int)
        Eval eval = new Eval();

        try {
            if (goldFile != null && sysFile != null) {
                CaboCha2Dep goldPipe = new CaboCha2Dep(new FileInputStream(goldFile));
                CaboCha2Dep systemPipe = new CaboCha2Dep(new FileInputStream(sysFile));	

                while (!goldPipe.eof()) {
                    DependencyTree goldTree = goldPipe.pipePerSentence();
                    if (goldTree == null) { continue; }
                    //System.err.println(goldTree.toString());
                    DependencyTree systemTree = systemPipe.pipePerSentence();
                    //System.err.println(systemTree.toString());
                    eval.getResults(goldTree, systemTree);
                }
            } else if (goldDir != null && sysDir != null) {
                File[] goldFiles = goldDir.listFiles();
                File[] sysFiles = sysDir.listFiles();
                if (goldFiles.length != sysFiles.length) {
                    System.err.println("the number of examples different between gold and system results.");
                    System.err.println("gold: "+goldFiles.length+"  sys: "+sysFiles.length);
                } else {
                    System.err.println("the number of examples: "+goldFiles.length);
                }

                TObjectIntHashMap<String> sysFilesHash = new TObjectIntHashMap<String>();
                for (int i = 0; i < sysFiles.length; i++) {
                    String[] sysE = sysFiles[i].toString().split("/");
                    String sysBaseName = sysE[sysE.length-1];
                    //sysBaseName.
                    sysFilesHash.put(sysBaseName, i);
                }

                for (int i = 0; i < goldFiles.length; i++) {
                    String[] goldE = goldFiles[i].toString().split("/");
                    String goldBaseName = goldE[goldE.length-1];
                    if (!sysFilesHash.contains(goldBaseName)) {
                        System.err.println("error!");
                        //System.exit(1);
                    }
                    System.err.println("processsing "+goldBaseName);
                    int idx = sysFilesHash.get(goldBaseName);
                    CaboCha2Dep sysPipe = new CaboCha2Dep(new FileInputStream(sysFiles[idx]));	
                    CaboCha2Dep goldPipe = new CaboCha2Dep(new FileInputStream(goldFiles[i]));
                    
                    while (!goldPipe.eof()) {
                        DependencyTree goldTree = goldPipe.pipePerSentence();
                        if (goldTree == null) { continue; }
                        //System.err.println(goldTree.toString());
                        DependencyTree sysTree = sysPipe.pipePerSentence();
                        //System.err.println(systemTree.toString());
                        eval.getResults(goldTree, sysTree);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Alphabet labelAlphabet = eval.getLabelAlphabet();

        Evaluator predE = eval.getPredE();
        Evaluator labE = eval.getLabE();
        Evaluator pLabE = eval.getPLabE();
        Evaluator pDepE = eval.getPDepE();
        Evaluator pSameBunE = eval.getPSameBunE();
        Evaluator pIntraSentE = eval.getPIntraSentE();

        Evaluator nLabE = eval.getNLabE();
        Evaluator nDepE = eval.getNDepE();
        Evaluator nSameBunE = eval.getNSameBunE();
        Evaluator nIntraSentE = eval.getNIntraSentE();

        int[] labels = labE.getLabels();
        System.out.print("=== Predicate Identification =============================================\n");
        System.out.print("Precision:\t");
        System.out.print("("+predE.getNumberOfCorrect()+")");
        System.out.print(" / ("+predE.getNumberOfReturned()+")");
        System.out.printf(" * 100 = %3.2f %% \n", predE.getTotalPrecision());

        System.out.print("Recall:\t\t");
        System.out.print("("+predE.getNumberOfCorrect()+")");
        System.out.print(" / ("+predE.getNumberOfTarget()+")");
        System.out.printf(" * 100 = %3.2f %%\n", predE.getTotalRecall());

        System.out.print("F1:\t\t");
        System.out.printf("%3.2f \n", predE.getTotalF());

        System.out.print("=== Argument Classification ==============================================\n");
        System.out.print("Labeled precision:\t");
        System.out.print("("+labE.getNumberOfCorrect()+")");
        System.out.print(" / ("+labE.getNumberOfReturned()+")");
        System.out.printf(" * 100 = %3.2f %% \n", labE.getTotalPrecision());

        System.out.print("Labeled recall:\t\t");
        System.out.print("("+labE.getNumberOfCorrect()+")");
        System.out.print(" / ("+labE.getNumberOfTarget()+")");
        System.out.printf(" * 100 = %3.2f %%\n", labE.getTotalRecall());

        System.out.print("Labeled F1:\t\t");
        System.out.printf("%3.2f \n", labE.getTotalF());

        System.out.print("=== Verb Predicates =====================================================\n");

        System.out.print("--- Total Performance ---------------------------------------------------\n");
        System.out.print("|Label\t|#\t|Precision\t\t|Recall\t\t\t|F1\t|\n");
        for (int i = 0; i < labels.length; i++) {
            System.out.print("|");
            System.out.print(labelAlphabet.lookupObject(labels[i])+"\t|");
            System.out.print(pLabE.getNumberOfTarget(labels[i])+"\t|");
            System.out.printf("%3.2f\t(%4d/%4d)\t|", pLabE.getPrecision(labels[i]), pLabE.getNumberOfCorrect(labels[i]), pLabE.getNumberOfReturned(labels[i]));
            System.out.printf("%3.2f\t(%4d/%4d)\t|", pLabE.getRecall(labels[i]), pLabE.getNumberOfCorrect(labels[i]), pLabE.getNumberOfTarget(labels[i]));
            System.out.printf("%3.2f\t|", pLabE.getF(labels[i]));
            System.out.print("\n");
        }

        System.out.print("--- Dependency Relations ------------------------------------------------\n");
        System.out.print("|Label\t|#\t|Precision\t\t|Recall\t\t\t|F1\t|\n");
        for (int i = 0; i < labels.length; i++) {
            System.out.print("|");
            System.out.print(labelAlphabet.lookupObject(labels[i])+"\t|");
            System.out.print(pDepE.getNumberOfTarget(labels[i])+"\t|");
            System.out.printf("%3.2f\t(%4d/%4d)\t|", pDepE.getPrecision(labels[i]), pDepE.getNumberOfCorrect(labels[i]), pDepE.getNumberOfReturned(labels[i]));
            System.out.printf("%3.2f\t(%4d/%4d)\t|", pDepE.getRecall(labels[i]), pDepE.getNumberOfCorrect(labels[i]), pDepE.getNumberOfTarget(labels[i]));
            System.out.printf("%3.2f\t|", pDepE.getF(labels[i]));
            System.out.print("\n");
        }

        System.out.print("--- In Same Phrase ------------------------------------------------------\n");
        System.out.print("|Label\t|#\t|Precision\t\t|Recall\t\t\t|F1\t|\n");
        for (int i = 0; i < labels.length; i++) {
            System.out.print("|");
            System.out.print(labelAlphabet.lookupObject(labels[i])+"\t|");
            System.out.print(pSameBunE.getNumberOfTarget(labels[i])+"\t|");
            System.out.printf("%3.2f\t(%4d/%4d)\t|", pSameBunE.getPrecision(labels[i]), pSameBunE.getNumberOfCorrect(labels[i]), pSameBunE.getNumberOfReturned(labels[i]));
            System.out.printf("%3.2f\t(%4d/%4d)\t|", pSameBunE.getRecall(labels[i]), pSameBunE.getNumberOfCorrect(labels[i]), pSameBunE.getNumberOfTarget(labels[i]));
            System.out.printf("%3.2f\t|", pSameBunE.getF(labels[i]));
            System.out.print("\n");
        }

        System.out.print("--- Zero-Anaphoric (Intra) ----------------------------------------------\n");
        System.out.print("|Label\t|#\t|Precision\t\t|Recall\t\t\t|F1\t|\n");
        for (int i = 0; i < labels.length; i++) {
            System.out.print("|");
            System.out.print(labelAlphabet.lookupObject(labels[i])+"\t|");
            System.out.print(pIntraSentE.getNumberOfTarget(labels[i])+"\t|");
            System.out.printf("%3.2f\t(%4d/%4d)\t|", pIntraSentE.getPrecision(labels[i]), pIntraSentE.getNumberOfCorrect(labels[i]), pIntraSentE.getNumberOfReturned(labels[i]));
            System.out.printf("%3.2f\t(%4d/%4d)\t|", pIntraSentE.getRecall(labels[i]), pIntraSentE.getNumberOfCorrect(labels[i]), pIntraSentE.getNumberOfTarget(labels[i]));
            System.out.printf("%3.2f\t|", pIntraSentE.getF(labels[i]));
            System.out.print("\n");
        }

        System.out.print("=== Noun Predicates =====================================================\n");

        System.out.print("--- Total Performance ---------------------------------------------------\n");
        System.out.print("|Label\t|#\t|Precision\t\t|Recall\t\t\t|F1\t|\n");
        for (int i = 0; i < labels.length; i++) {
            System.out.print("|");
            System.out.print(labelAlphabet.lookupObject(labels[i])+"\t|");
            System.out.print(nLabE.getNumberOfTarget(labels[i])+"\t|");
            System.out.printf("%3.2f\t(%4d/%4d)\t|", nLabE.getPrecision(labels[i]), nLabE.getNumberOfCorrect(labels[i]), nLabE.getNumberOfReturned(labels[i]));
            System.out.printf("%3.2f\t(%4d/%4d)\t|", nLabE.getRecall(labels[i]), nLabE.getNumberOfCorrect(labels[i]), nLabE.getNumberOfTarget(labels[i]));
            System.out.printf("%3.2f\t|", nLabE.getF(labels[i]));
            System.out.print("\n");
        }

        System.out.print("--- Dependency Relations ------------------------------------------------\n");
        System.out.print("|Label\t|#\t|Precision\t\t|Recall\t\t\t|F1\t|\n");
        for (int i = 0; i < labels.length; i++) {
            System.out.print("|");
            System.out.print(labelAlphabet.lookupObject(labels[i])+"\t|");
            System.out.print(nDepE.getNumberOfTarget(labels[i])+"\t|");
            System.out.printf("%3.2f\t(%4d/%4d)\t|", nDepE.getPrecision(labels[i]), nDepE.getNumberOfCorrect(labels[i]), nDepE.getNumberOfReturned(labels[i]));
            System.out.printf("%3.2f\t(%4d/%4d)\t|", nDepE.getRecall(labels[i]), nDepE.getNumberOfCorrect(labels[i]), nDepE.getNumberOfTarget(labels[i]));
            System.out.printf("%3.2f\t|", nDepE.getF(labels[i]));
            System.out.print("\n");
        }

        System.out.print("--- In Same Phrase ------------------------------------------------------\n");
        System.out.print("|Label\t|#\t|Precision\t\t|Recall\t\t\t|F1\t|\n");
        for (int i = 0; i < labels.length; i++) {
            System.out.print("|");
            System.out.print(labelAlphabet.lookupObject(labels[i])+"\t|");
            System.out.print(nSameBunE.getNumberOfTarget(labels[i])+"\t|");
            System.out.printf("%3.2f\t(%4d/%4d)\t|", nSameBunE.getPrecision(labels[i]), nSameBunE.getNumberOfCorrect(labels[i]), nSameBunE.getNumberOfReturned(labels[i]));
            System.out.printf("%3.2f\t(%4d/%4d)\t|", nSameBunE.getRecall(labels[i]), nSameBunE.getNumberOfCorrect(labels[i]), nSameBunE.getNumberOfTarget(labels[i]));
            System.out.printf("%3.2f\t|", nSameBunE.getF(labels[i]));
            System.out.print("\n");
        }

        System.out.print("--- Zero-Anaphoric (Intra) ----------------------------------------------\n");
        System.out.print("|Label\t|#\t|Precision\t\t|Recall\t\t\t|F1\t|\n");
        for (int i = 0; i < labels.length; i++) {
            System.out.print("|");
            System.out.print(labelAlphabet.lookupObject(labels[i])+"\t|");
            System.out.print(nIntraSentE.getNumberOfTarget(labels[i])+"\t|");
            System.out.printf("%3.2f\t(%4d/%4d)\t|", nIntraSentE.getPrecision(labels[i]), nIntraSentE.getNumberOfCorrect(labels[i]), nIntraSentE.getNumberOfReturned(labels[i]));
            System.out.printf("%3.2f\t(%4d/%4d)\t|", nIntraSentE.getRecall(labels[i]), nIntraSentE.getNumberOfCorrect(labels[i]), nIntraSentE.getNumberOfTarget(labels[i]));
            System.out.printf("%3.2f\t|", nIntraSentE.getF(labels[i]));
            System.out.print("\n");
        }

        System.out.print("=========================================================================\n");
    }

    private static void processArguments(String[] args) {
        int idx = 0;
        while (idx < args.length) {
            if(args[idx].equals("--gold-file") || args[idx].equals("-g")) {
                idx++;
                goldFile = new File(args[idx++]);
            } else if(args[idx].equals("--goldn-dir") || args[idx].equals("-gd")) {
                idx++;
                goldDir = new File(args[idx++]);
            } else if(args[idx].equals("--sys-file") || args[idx].equals("-s")) {
                idx++;
                sysFile = new File(args[idx++]);
            } else if(args[idx].equals("--sys-dir") || args[idx].equals("-sd")) {
                idx++;
                sysDir = new File(args[idx++]);
            } else {
                idx++;
            }
        }
    }

}
