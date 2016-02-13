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

package jp.ac.tohoku.ecei.cl.www.io;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;

import jp.ac.tohoku.ecei.cl.www.base.*;
import jp.ac.tohoku.ecei.cl.www.util.*;

public class JapaneseDependencyTree2CoNLL2009 {

    public String pipePerArticle (DependencyTree[] trees) {
	
        StringBuilder str = new StringBuilder();
        PredicateArgumentStructure[][] pASList = new PredicateArgumentStructure[trees.length][];
        int numOfPASInArticle = 0;
        for (int i = 0; i < trees.length; i++) {
            pASList[i] = trees[i].getPASList();
            numOfPASInArticle += pASList.length;
        }

        Alphabet depId2SemId = new Alphabet();
        depId2SemId.lookupIndex("", true);

        for (int i = 0; i < trees.length; i++) {
            int treeSize = trees[i].size();
            pASList[i] = trees[i].getPASList();
            Bunsetsu[] bunsetsuList = trees[i].getBunsetsuList();
            for (int b = 0; b < bunsetsuList.length; b++) {
                DependencyNode[] bNodes = bunsetsuList[b].getNodes();
                for (int j = 0; j < bNodes.length; j++) {
                    DependencyNode node = bNodes[j];
                    str.append(node.getId()+"\t");
                    str.append(node.getForm()+"\t");
                    str.append(node.getLemma()+"\t");
                    str.append("_\t"); // plemma
                    str.append(node.getPOS()+"\t"); // pos
                    str.append(node.ppos==null?"_\t":node.ppos+"\t"); // ppos

                    // feats
                    //str.append(node.yomi+"|");
                    if (node.cType != null) {
                        str.append(node.cType+"|");
                        str.append(node.cForm+"\t");
                    } else {
                        str.append("_");
                    }
                    str.append("\t");
		    
                    // pfeats
                    str.append("_\t");
		    
                    // dep and dep rel
                    str.append(node.getHead()+"\t"); // head
                    str.append("_\t"); // phead
                    str.append(node.getDepRel()+"\t"); //deprel
                    str.append(node.pDepRel==null?"_\t":node.pDepRel+"\t"); //pdeprel

                    StringBuffer semStr = new StringBuffer();
                    String fillPred = "_";
                    String predSense = "_";

                    if (pASList[i] != null) {
                        for (int p = 0; p < pASList[i].length; p++) {
                            if (node.id == pASList[i][p].getPredicateId()) {
                                fillPred = "Y";
                                predSense = pASList[i][p].getPredicateSense();
                            }
                        }
                    }
                    semStr.append(fillPred+"\t"+predSense);

                    for (int k = 0; k < pASList.length; k++) {
                        //if (pASList[i] != null) {
                        if (pASList[k] != null) {
                            for (int p = 0; p < pASList[k].length; p++) {
                                int[] argumentIds = pASList[k][p].argumentIds;
                                String[] argumentLabels = pASList[k][p].argumentLabels;
                                String arg = "_";
                                if (argumentIds == null) {
                                    semStr.append("\t"+arg);
                                } else {
                                    for (int ai = 0; ai < argumentIds.length; ai++) {
                                        if (argumentIds[ai] == node.id) {
                                            arg = argumentLabels[ai];
                                        }
                                    }
                                    semStr.append("\t"+arg);
                                }
                            }
                        }
                    }
                    str.append(semStr.toString());
                    str.append("\n");
                }
            }
            str.append("\n");
        }
        return str.toString();
    }

    /*
    public String pipePerSentence (DependencyTree tree) {
	
        StringBuilder str = new StringBuilder();
        Alphabet depId2SemId = new Alphabet();
        depId2SemId.lookupIndex("", true);

        int treeSize = tree.size();
        PredicateArgumentStructure[] pASList = tree.getPASList();
        Bunsetsu[] bunsetsuList = tree.getBunsetsuList();
        for (int b = 0; b < bunsetsuList.length; b++) {
            DependencyNode[] bNodes = bunsetsuList[b].getNodes();
            //str.append("* "+bunsetsuList[b].getId()+" "+bunsetsuList[b].getHead()+bunsetsuList[b].getDepLabel()+"\n"); // head/func is not included
            for (int j = 0; j < bNodes.length; j++) {
                DependencyNode node = bNodes[j];
                str.append(node.getId()+"\t");
                str.append(node.getForm()+"\t");
                str.append(node.getLemma()+"\t");
                str.append("_\t"); // plemma
                str.append(node.getPOS()+"\t"); // pos
                str.append(node.ppos==null?"_\t":node.ppos+"\t"); // ppos

                // feats
                if (node.cType != null) {
                    str.append("_\t");
                    //str.append(node.cType+"|");
                    //str.append(node.cForm+"\t");
                } else {
                    str.append("_\t");
                }
		    
                // pfeats
                str.append("_\t");
		    
                // dep and dep rel
                str.append(node.getHead()+"\t"); // head
                str.append("_\t"); // phead
                str.append(node.getDepRel()+"\t"); //deprel
                str.append(node.pDepRel==null?"_\t":node.pDepRel+"\t"); //pdeprel

                StringBuffer semStr = new StringBuffer();

                String fillPred = "_";
                String predSense = "_";

                if (pASList != null) {
                    for (int p = 0; p < pASList.length; p++) {
                        if (node.id == pASList[p].getPredicateId()) {
                            fillPred = "Y";
                            predSense = pASList[p].getPredicateSense();
                        }
                    }
                }
                semStr.append(fillPred+"\t"+predSense);

                if (pASList != null) {
                    for (int p = 0; p < pASList.length; p++) {
                        int[] argumentIds = pASList[p].argumentIds;
                        String[] argumentLabels = pASList[p].argumentLabels;
                        String arg = "_";
                        if (argumentIds == null) {
                            semStr.append("\t"+arg);
                        } else {
                            for (int ai = 0; ai < argumentIds.length; ai++) {
                                if (argumentIds[ai] == node.id) {
                                    arg = argumentLabels[ai];
                                }
                            }
                            semStr.append("\t"+arg);
                        }
                    }
                }
                str.append(semStr.toString());
                str.append("\n");
            }
        }
        str.append("\n");
        return str.toString();
    }
    */

    public String pipePerSentence (DependencyTree tree) {
	
        StringBuilder str = new StringBuilder();

        Alphabet depId2SemId = new Alphabet();
        depId2SemId.lookupIndex("", true);

        int treeSize = tree.size();
        PredicateArgumentStructure[] pASList = tree.getPASList();
        PredicateArgumentStructure[] bunsetsuPASList = tree.getBunsetsuPASList();

        Bunsetsu[] bunsetsuList = tree.getBunsetsuList();
        for (int b = 0; b < bunsetsuList.length; b++) {
            DependencyNode[] bNodes = bunsetsuList[b].getNodes();
            //str.append("* "+bunsetsuList[b].getId()+" "+bunsetsuList[b].getHead()+bunsetsuList[b].getDepLabel()+"\n"); // head/func is not included
            //for (int j = 0; j < bNodes.length; j++) {
            //DependencyNode node = bNodes[j];

            str.append(bunsetsuList[b].getId()+1); // id
            str.append("\t");

            // form
            for (int j = 0; j < bNodes.length; j++) {
                DependencyNode node = bNodes[j];
                str.append(node.getForm());
                if (j != bNodes.length-1) {
                    str.append("/");
                }
            }
            str.append("\t");

            // lemma
            for (int j = 0; j < bNodes.length; j++) {
                DependencyNode node = bNodes[j];
                str.append(node.getLemma());
                if (j != bNodes.length-1) {
                    str.append("/");
                }
            }
            str.append("\t");

            // plemma
            for (int j = 0; j < bNodes.length; j++) {
                DependencyNode node = bNodes[j];
                str.append(node.getLemma());
                if (j != bNodes.length-1) {
                    str.append("/");
                }
            }
            str.append("\t");

            // pos
            for (int j = 0; j < bNodes.length; j++) {
                DependencyNode node = bNodes[j];
                str.append(node.getPOS()); // pos
                if (j != bNodes.length-1) {
                    str.append("/");
                }
            }
            str.append("\t");

            // ppos
            for (int j = 0; j < bNodes.length; j++) {
                DependencyNode node = bNodes[j];
                str.append(node.getPOS()); // pos
                if (j != bNodes.length-1) {
                    str.append("/");
                }
            }
            str.append("\t");

            // feats
            str.append("_\t"); // feats
            str.append("_\t"); // pfeats
		    
            // dep and dep rel
            str.append((bunsetsuList[b].getHead()+1)+"\t"); // head
            str.append((bunsetsuList[b].getHead()+1)+"\t"); // phead

            str.append(bunsetsuList[b].getDepLabel()+"\t"); //deprel
            str.append(bunsetsuList[b].getDepLabel()+"\t"); //pdeprel

            StringBuffer semStr = new StringBuffer();
            
            String fillPred = "_";
            String predSense = "_";
            
            if (bunsetsuPASList != null) {
                for (int p = 0; p < bunsetsuPASList.length; p++) {
                    if (bunsetsuPASList[p].getPredicateId() == bunsetsuList[b].getId()) {
                        fillPred = "Y";
                        predSense = pASList[p].getPredicateSense();
                    }
                }
            }

            semStr.append(fillPred+"\t"+predSense);
            
            if (bunsetsuPASList != null) {
                for (int p = 0; p < bunsetsuPASList.length; p++) {
                    int[] argumentIds = bunsetsuPASList[p].argumentIds;
                    String[] argumentLabels = bunsetsuPASList[p].argumentLabels;
                    String arg = "_";
                    if (argumentIds == null) {
                        semStr.append("\t"+arg);
                    } else {
                        for (int ai = 0; ai < argumentIds.length; ai++) {
                            if (argumentIds[ai] == bunsetsuList[b].getId()) {
                                arg = argumentLabels[ai];
                            }
                        }
                        semStr.append("\t"+arg);
                    }
                }
            }
            str.append(semStr.toString());
            str.append("\n");
        }
        str.append("\n");
        return str.toString();
    }

    public static boolean isNewFormat = false;

    public static void main (String[] args) {
        try {
            File particlePatFile = new File("resources/patterns/ipa_particle_pat.txt");
            HashSet<String> particlePat = PatternFileParser.parse(particlePatFile);
                    
            CaboCha2Dep pipe = new CaboCha2Dep(System.in);
            pipe.setFormat(CaboChaFormat.OLD);
            JapaneseDependencyTree2CoNLL2009 outPipe = new JapaneseDependencyTree2CoNLL2009();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out, "utf-8"));
            while (!pipe.eof()) {
                DependencyTree tree = pipe.pipePerSentence();
                if (tree == null) { break; }
                tree.setBunsetsuPASList();
                PredicateArgumentStructure[] pasList = tree.getBunsetsuPASList();
                ArrayList<PredicateArgumentStructure> newPASListAry = new ArrayList<PredicateArgumentStructure>();
                int idx = 0;
                while (idx < pasList.length) {
                    while (idx+1 < pasList.length &&
                           pasList[idx].getPredicateId() == pasList[idx+1].getPredicateId()) {
                        idx++;
                    }
                    newPASListAry.add(pasList[idx]);
                    idx++;
                }
                tree.setBunsetsuPASList((PredicateArgumentStructure[]) newPASListAry.toArray(new PredicateArgumentStructure[newPASListAry.size()]));
                //JapaneseDependencyTreeLib.setParticleToBunsetsuDepRel(tree, particlePat);
                //System.err.println(tree.toString());
                //System.out.println(outPipe.pipePerSentence(tree));
                writer.write(outPipe.pipePerSentence(tree));
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processArguments(String[] args) {
        int idx = 0;
        while (idx < args.length) {
            idx++;
        }
    }
}
