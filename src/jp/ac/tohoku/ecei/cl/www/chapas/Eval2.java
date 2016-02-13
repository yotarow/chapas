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

import jp.ac.tohoku.ecei.cl.www.base.*;
import jp.ac.tohoku.ecei.cl.www.util.*;
import jp.ac.tohoku.ecei.cl.www.io.*;

public class Eval2 {

    public enum DepType {DEP, ZERO_INTRA, ZERO_INTER, SAME_PHRASE}

    public static DepType getDepType (DependencyTree tree, int pIdx, int aIdx) {
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

    public static void main(String[] args) {
        String goldFile = args[0];
        String outputFile = args[1];

        // label(String) 2 label(Int)
        Alphabet labelAlphabet = new Alphabet();

        try {
            CaboCha2Dep goldPipe = new CaboCha2Dep(new FileInputStream(goldFile));
            JapaneseDependencyTree2CaboCha outPipe = new JapaneseDependencyTree2CaboCha();

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "utf-8"));

            while (!goldPipe.eof()) {
                DependencyTree goldTree = goldPipe.pipePerSentence();
                if (goldTree == null) { continue; }

                PredicateArgumentStructure[] goldPASList = goldTree.getPASList();

                // calc target (N)
                for (int j = 0; j < goldPASList.length; j++) {
                    int predId = goldPASList[j].getPredicateId();
                    String predType = goldPASList[j].predicateType;
                    int[] aIds = goldPASList[j].getIds();
                    String[] aLabels = goldPASList[j].getLabels();
                    for (int k = 0; k < aIds.length; k++) {
                        int aLabelAsInt = labelAlphabet.lookupIndex(aLabels[k], true);
                        DepType dt = getDepType(goldTree, predId, aIds[k]);
                        if (predType.equals("pred")) {

                        } else if (predType.equals("event")) {

                        }
                        if (dt == DepType.DEP) {
                            if (predType.equals("pred")) {
                            } else if (predType.equals("event")) {
                            }
                        } else if (dt == DepType.SAME_PHRASE) {
                            if (predType.equals("pred")) {
                                writer.write(outPipe.pipePerSentence(goldTree));
                            } else if (predType.equals("event")) {
                            }
                        } else if (dt == DepType.ZERO_INTRA) {
                            if (predType.equals("pred")) {
                            } else if (predType.equals("event")) {
                            }
                        } else {
                            if (predType.equals("pred")) {
                            } else if (predType.equals("event")) {
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
          int[] labels = labE.getLabels();

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

          System.out.print("=== Verb Predicates =====================================\n");

          System.out.print("--- Total Performance -----------------------------------\n");
          System.out.print("|Label\t|#\t|Precision\t|Recall\t\t|F1\t|\n");
          for (int i = 0; i < labels.length; i++) {
          System.out.print("|");
          System.out.print(labelAlphabet.lookupObject(labels[i])+"\t|");
          System.out.print(pLabE.getNumberOfTarget(labels[i])+"\t|");
          System.out.printf("%3.2f\t\t|", pLabE.getPrecision(labels[i]));
          System.out.printf("%3.2f\t\t|", pLabE.getRecall(labels[i]));
          System.out.printf("%3.2f\t|", pLabE.getF(labels[i]));
          System.out.print("\n");
          }

          System.out.print("--- Dependency Relations --------------------------------\n");
          System.out.print("|Label\t|#\t|Precision\t|Recall\t\t|F1\t|\n");
          for (int i = 0; i < labels.length; i++) {
          System.out.print("|");
          System.out.print(labelAlphabet.lookupObject(labels[i])+"\t|");
          System.out.print(pDepE.getNumberOfTarget(labels[i])+"\t|");
          System.out.printf("%3.2f\t\t|", pDepE.getPrecision(labels[i]));
          System.out.printf("%3.2f\t\t|", pDepE.getRecall(labels[i]));
          System.out.printf("%3.2f\t|", pDepE.getF(labels[i]));
          System.out.print("\n");
          }

          System.out.print("--- In Same Phrase --------------------------------------\n");
          System.out.print("|Label\t|#\t|Precision\t|Recall\t\t|F1\t|\n");
          for (int i = 0; i < labels.length; i++) {
          System.out.print("|");
          System.out.print(labelAlphabet.lookupObject(labels[i])+"\t|");
          System.out.print(pSameBunE.getNumberOfTarget(labels[i])+"\t|");
          System.out.printf("%3.2f\t\t|", pSameBunE.getPrecision(labels[i]));
          System.out.printf("%3.2f\t\t|", pSameBunE.getRecall(labels[i]));
          System.out.printf("%3.2f\t|", pSameBunE.getF(labels[i]));
          System.out.print("\n");
          }

          System.out.print("--- Zero-Anaphoric (Intra) ------------------------------\n");
          System.out.print("|Label\t|#\t|Precision\t|Recall\t\t|F1\t|\n");
          for (int i = 0; i < labels.length; i++) {
          System.out.print("|");
          System.out.print(labelAlphabet.lookupObject(labels[i])+"\t|");
          System.out.print(pIntraSentE.getNumberOfTarget(labels[i])+"\t|");
          System.out.printf("%3.2f\t\t|", pIntraSentE.getPrecision(labels[i]));
          System.out.printf("%3.2f\t\t|", pIntraSentE.getRecall(labels[i]));
          System.out.printf("%3.2f\t|", pIntraSentE.getF(labels[i]));
          System.out.print("\n");
          }

          System.out.print("=== Noun Predicates =====================================\n");

          System.out.print("--- Total Performance -----------------------------------\n");
          System.out.print("|Label\t|#\t|Precision\t|Recall\t\t|F1\t|\n");
          for (int i = 0; i < labels.length; i++) {
          System.out.print("|");
          System.out.print(labelAlphabet.lookupObject(labels[i])+"\t|");
          System.out.print(nLabE.getNumberOfTarget(labels[i])+"\t|");
          System.out.printf("%3.2f\t\t|", nLabE.getPrecision(labels[i]));
          System.out.printf("%3.2f\t\t|", nLabE.getRecall(labels[i]));
          System.out.printf("%3.2f\t|", nLabE.getF(labels[i]));
          System.out.print("\n");
          }

          System.out.print("--- Dependency Relations --------------------------------\n");
          System.out.print("|Label\t|#\t|Precision\t|Recall\t\t|F1\t|\n");
          for (int i = 0; i < labels.length; i++) {
          System.out.print("|");
          System.out.print(labelAlphabet.lookupObject(labels[i])+"\t|");
          System.out.print(nDepE.getNumberOfTarget(labels[i])+"\t|");
          System.out.printf("%3.2f\t\t|", nDepE.getPrecision(labels[i]));
          System.out.printf("%3.2f\t\t|", nDepE.getRecall(labels[i]));
          System.out.printf("%3.2f\t|", nDepE.getF(labels[i]));
          System.out.print("\n");
          }

          System.out.print("--- In Same Phrase --------------------------------------\n");
          System.out.print("|Label\t|#\t|Precision\t|Recall\t\t|F1\t|\n");
          for (int i = 0; i < labels.length; i++) {
          System.out.print("|");
          System.out.print(labelAlphabet.lookupObject(labels[i])+"\t|");
          System.out.print(nSameBunE.getNumberOfTarget(labels[i])+"\t|");
          System.out.printf("%3.2f\t\t|", nSameBunE.getPrecision(labels[i]));
          System.out.printf("%3.2f\t\t|", nSameBunE.getRecall(labels[i]));
          System.out.printf("%3.2f\t|", nSameBunE.getF(labels[i]));
          System.out.print("\n");
          }

          System.out.print("--- Zero-Anaphoric (Intra) ------------------------------\n");
          System.out.print("|Label\t|#\t|Precision\t|Recall\t\t|F1\t|\n");
          for (int i = 0; i < labels.length; i++) {
          System.out.print("|");
          System.out.print(labelAlphabet.lookupObject(labels[i])+"\t|");
          System.out.print(nIntraSentE.getNumberOfTarget(labels[i])+"\t|");
          System.out.printf("%3.2f\t\t|", nIntraSentE.getPrecision(labels[i]));
          System.out.printf("%3.2f\t\t|", nIntraSentE.getRecall(labels[i]));
          System.out.printf("%3.2f\t|", nIntraSentE.getF(labels[i]));
          System.out.print("\n");
          }


          System.out.print("=========================================================\n");
        */
    }
}
