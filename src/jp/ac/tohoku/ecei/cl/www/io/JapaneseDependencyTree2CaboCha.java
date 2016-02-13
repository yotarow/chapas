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
import java.util.HashMap;
import java.util.Iterator;

import jp.ac.tohoku.ecei.cl.www.base.*;
import jp.ac.tohoku.ecei.cl.www.util.*;

public class JapaneseDependencyTree2CaboCha {

    private int outputMode = 2; // 0: morph only, 1: morph and dep, 2: morph, dep and pas

    private CaboChaFormat format = CaboChaFormat.UNK;
    private boolean printSentenceID = false;
    private boolean outputPASNBest = false;

    public JapaneseDependencyTree2CaboCha () {}
    
    public void setPrintSentenceID (boolean val) {
        this.printSentenceID = val;
    }

    public void setOutputPASNBest (boolean val) {
        this.outputPASNBest = val;
    }

    public void setFormat (CaboChaFormat format) {
        this.format = format;
    }

    public CaboChaFormat getFormat () {
        return this.format;
    }
    
    public void setOutputMode (int outputMode) {
        this.outputMode = outputMode;
    }

    public String pipePerArticle (ArrayList<DependencyTree> treesAry) {
        DependencyTree[] trees = new DependencyTree[treesAry.size()];
        for (int i = 0; i < treesAry.size(); i++) {
            trees[i] = treesAry.get(i);
        }
        return pipePerArticle(trees);
    }

    public String pipePerArticle (DependencyTree[] trees) {
	
        StringBuilder str = new StringBuilder();
        PredicateArgumentStructure[][] pASList = new PredicateArgumentStructure[trees.length][];
        int numOfPASInArticle = 0;
        int numWords = 0;
        for (int i = 0; i < trees.length; i++) {
            pASList[i] = trees[i].getPASList();
            numWords += trees[i].size();
            numOfPASInArticle += pASList.length;
        }

        Alphabet depId2SemId = new Alphabet();
        depId2SemId.lookupIndex("", true);
	
        boolean[] argList = new boolean[numWords];
        // create depId2SemId hash
        int curTreeIdx = 0;
        for (int i = 0; i < trees.length; i++) {
            if (pASList[i] == null)
                continue;
            boolean predFlag = false;
            for (int p = 0; p < pASList[i].length; p++) {
                int[] argumentIds = pASList[i][p].argumentIds;
                String[] argumentLabels = pASList[i][p].argumentLabels;
                for (int aIdx = 0; aIdx < argumentIds.length; aIdx++) {
                    argList[argumentIds[aIdx]-1] = true;
                }
            }
        }

        for (int i = 0; i < numWords; i++) {
            if (argList[i]) { 
                depId2SemId.lookupIndex((i+1)+"", true);
            }
        }

        for (int i = 0; i < trees.length; i++) {
            if (printSentenceID && trees[i].getId() != null) {
                str.append(trees[i].getId()+"\n");
            }
            int treeSize = trees[i].size();
            Bunsetsu[] bunsetsuList = trees[i].getBunsetsuList();
            for (int b = 0; b < bunsetsuList.length; b++) {
                DependencyNode[] bNodes = bunsetsuList[b].getNodes();
                if (outputMode >= 1) {
                    if (!bunsetsuList[b].getInfo().equals("")) {
                        str.append(bunsetsuList[b].getInfo()+"\n");
                    } else {
                        str.append("* "+bunsetsuList[b].getId()+" "+bunsetsuList[b].getHead()+bunsetsuList[b].getDepLabel()+"\n"); // head/func is not included
                    }
                }
                for (int j = 0; j < bNodes.length; j++) {
                    DependencyNode node = bNodes[j];
                    str.append(this.getNodeLine(node)+"\t");

                    StringBuffer semStr = new StringBuffer();
                    
                    if (outputMode >= 2) {
                        if (pASList[i] != null) {
                            boolean predFlag = false;
                            for (int p = 0; p < pASList[i].length; p++) {
                                if (node.getId() == pASList[i][p].getPredicateId() && !predFlag) {
                                    semStr.append("type=\""+pASList[i][p].predicateType+"\" ");
                                    int[] argumentIds = pASList[i][p].argumentIds;
                                    String[] argumentLabels = pASList[i][p].argumentLabels;
                                    for (int aIdx = 0; aIdx < argumentIds.length; aIdx++) {
                                        semStr.append(argumentLabels[aIdx]+"=\""+depId2SemId.lookupIndex(argumentIds[aIdx]+"", false)+"\" ");
                                    }
                                    predFlag = true;
                                }
                            }
                        }
                    }
                    int semId = -1;
                    for (int k = 0; k < trees.length; k++) {
                        if (pASList[k] != null) {
                            for (int p = 0; p < pASList[k].length; p++) {
                                int[] argumentIds = pASList[k][p].argumentIds;
                                String[] argumentLabels = pASList[k][p].argumentLabels;
                                if (argumentIds != null) {
                                    for (int ai = 0; ai < argumentIds.length; ai++) {
                                        if (argumentIds[ai] == node.id) {
                                            semId = depId2SemId.lookupIndex(node.getId()+"", false);
                                        }
                                    }
                                }
                            }
                        }
                        if (semId != -1)
                            break;
                    }
                    if (semId != -1) {
                        semStr.append("ID=\""+semId+"\" ");
                    }
                    str.append(semStr.toString());
                    str.append("\n");
                }
            }
            str.append("EOS\n");
        }
        return str.toString();
    }

    private String getNodeLine(DependencyNode node) {
        StringBuilder str = new StringBuilder();

        // OLD Format (cabocha ver. <= 0.53)
        if (format == CaboChaFormat.OLD) {
            str.append(node.getForm()+"\t");
            str.append((node.yomi == null ? "" : node.yomi)+"\t");
            str.append((node.getLemma() == null ? "" : node.getLemma())+"\t");
            str.append(node.getPOS()+"\t");
            str.append(node.cType+"\t");
            str.append(node.cForm+"\t");
            str.append(node.ne);
            // NEW Format (cabocha ver. >= 0.6)
        } else {
            str.append(node.getForm()+"\t");
            String[] poss = node.getPOS().split("-");
            int cnt = 0;
            while (cnt < poss.length) {
                str.append(poss[cnt]+",");
                cnt++;
            }
            while (cnt < 4) {
                str.append("*,");
                cnt++;
            }
            str.append((node.cType.equals("") ? "*" : node.cType));
            str.append(","+(node.cForm.equals("") ? "*" : node.cForm));
            str.append(","+(node.getLemma().equals("") ? "*" : node.getLemma()));
            if (node.yomi != null) {
                str.append(","+node.yomi);
            }
            if (node.pron != null) {
                str.append(","+node.pron);
            }
            str.append("\t");
            str.append(node.ne);
        }
        return str.toString();
    }

    public String pipePerSentence (DependencyTree tree) {
        StringBuilder str = new StringBuilder();
        Alphabet depId2SemId = new Alphabet();
        depId2SemId.lookupIndex("", true);

        int treeSize = tree.size();
	
        if (printSentenceID && tree.getId() != null) {
            str.append(tree.getId()+"\n");
        }

        PredicateArgumentStructure[] pASList = tree.getPASList();
        ArrayList<PredicateArgumentStructure>[] pASListNBest = tree.getPASListNBest();

        Bunsetsu[] bunsetsuList = tree.getBunsetsuList();

        boolean[] argList = new boolean[treeSize];

        // create depId2SemId hash
        DependencyNode[] nodes = tree.getNodes();
        int bIdx = nodes[0].getId();
        int eIdx = nodes[nodes.length-1].getId();

        if (outputPASNBest && pASListNBest != null) {
            for (int p = 0; p < pASListNBest.length; p++) {
                int size = pASListNBest[p].size();
                for (int q = 0; q < size; q++) {
                    PredicateArgumentStructure pas = pASListNBest[p].get(q);
                    int[] argumentIds = pas.argumentIds;
                    String[] argumentLabels = pas.argumentLabels;
                    for (int aIdx = 0; aIdx < argumentLabels.length; aIdx++) {
                        if (argumentIds[aIdx]-bIdx >= 0 && argumentIds[aIdx]-bIdx < treeSize) {
                            argList[argumentIds[aIdx]-bIdx] = true;
                        }
                    }
                }
            }
        } else if (pASList != null) {
            for (int p = 0; p < pASList.length; p++) {
                int[] argumentIds = pASList[p].argumentIds;
                String[] argumentLabels = pASList[p].argumentLabels;
                if (argumentIds == null) { continue; }
                for (int aIdx = 0; aIdx < argumentIds.length; aIdx++) {
                    if (argumentIds[aIdx]-bIdx >= 0 && argumentIds[aIdx]-bIdx < treeSize) {
                        argList[argumentIds[aIdx]-bIdx] = true;
                    }
                }
            }
        }

        for (int aIdx = 0; aIdx < treeSize; aIdx++) {
            if (argList[aIdx]) { 
                depId2SemId.lookupIndex(nodes[aIdx].getId()+"", true);
            }
        }

        for (int b = 0; b < bunsetsuList.length; b++) {
            DependencyNode[] bNodes = bunsetsuList[b].getNodes();
            str.append("* "+bunsetsuList[b].getId()+" "+bunsetsuList[b].getHead()+bunsetsuList[b].getDepLabel());
            if (bunsetsuList[b].getInfo() != null && !bunsetsuList[b].getInfo().equals("")) {
                str.append(" "+bunsetsuList[b].getInfo());
            }
            str.append("\n");
            for (int j = 0; j < bNodes.length; j++) {
                DependencyNode node = bNodes[j];
                str.append(this.getNodeLine(node)+"\t");

                StringBuffer semStr = new StringBuffer();

                // output N-best
                if (outputPASNBest && pASListNBest != null) {
                    boolean predFlag = false;
                    for (int p = 0; p < pASListNBest.length; p++) {
                        if (pASListNBest[p].size() == 0) { continue; }
                        PredicateArgumentStructure pas = pASListNBest[p].get(0);
                        if (node.getId() == pas.getPredicateId() && !predFlag) {
                            int numN = pASListNBest[p].size();
                            for (int n = 0; n < numN; n++) {
                                pas = pASListNBest[p].get(n);
                                String predType = pas.predicateType == null ? "" : pas.predicateType;
                                semStr.append("type=\""+predType+"\" ");
                                int[] argumentIds = pas.argumentIds;
                                String[] argumentLabels = pas.argumentLabels;
                                for (int aIdx = 0; aIdx < argumentIds.length; aIdx++) {
                                    int semId = depId2SemId.lookupIndex(argumentIds[aIdx]+"", false);
                                    if (semId > 0) {
                                        semStr.append(argumentLabels[aIdx]+"=\""+semId+"\" ");
                                    }
                                }
                                semStr.append("|");
                            }
                            predFlag = true;
                        }
                    }

                    // output only 1-best
                } else if (pASList != null) {
                    //boolean predFlag = false;
                    for (int p = 0; p < pASList.length; p++) {
                        //if (node.getId() == pASList[p].getPredicateId() && !predFlag) {
                        if (node.getId() == pASList[p].getPredicateId()) {
                            //System.err.println("absPredId="+pASList[p].absPredId);

                            if (pASList[p].absPredId != -1) {
                                semStr.append("pred_id=\""+pASList[p].absPredId+"\" ");
                            }
                            if (pASList[p].voice != null) {
                                semStr.append("alt=\""+pASList[p].voice+"\" ");
                            }
                            if (pASList[p].predicateType != null) { 
                                semStr.append("type=\""+pASList[p].predicateType+"\" ");
                            }
                            int[] argumentIds = pASList[p].argumentIds;
                            String[] argumentLabels = pASList[p].argumentLabels;
                            if (argumentIds == null) { continue; }
                            for (int aIdx = 0; aIdx < argumentIds.length; aIdx++) {
                                int semId = depId2SemId.lookupIndex(argumentIds[aIdx]+"", false);
                                //if (semId > 0) {
                                semStr.append(argumentLabels[aIdx]+"=\""+semId+"\" ");
                                //}
                            }
                            break;
                            //predFlag = true;
                        }
                    }
                }

                // output ids to arguments
                int semId = depId2SemId.lookupIndex(node.id+"", false);
                if (semId != -1) {
                    semStr.append("ID=\""+semId+"\" ");
                }
                str.append(semStr.toString());
                str.append("\n");
            }
        }
        str.append("EOS\n");
        // add debuginfo
        HashMap<String, String> debugInfoHash = tree.getDebugInfo();
        for (Iterator it = debugInfoHash.keySet().iterator(); it.hasNext();) {
            String key = (String) it.next();
            String value = debugInfoHash.get(key);
            str.append(key+"\t"+value);
        }

        return str.toString();
    }

    // edit
    public String pipePerSentenceWithInfoOnly (DependencyTree tree) {

        StringBuilder str = new StringBuilder();
        Alphabet depId2SemId = new Alphabet();
        int treeSize = tree.size();
	
        Bunsetsu[] bunsetsuList = tree.getBunsetsuList();
        str.append(tree.getId()+"\n");
        for (int b = 0; b < bunsetsuList.length; b++) {
            DependencyNode[] bNodes = bunsetsuList[b].getNodes();
            if (!bunsetsuList[b].getInfo().equals("")) {
                str.append(bunsetsuList[b].getInfo()+"\n");
            } else {
                str.append("* "+bunsetsuList[b].getId()+" "+bunsetsuList[b].getHead()+bunsetsuList[b].getDepLabel()+"\n"); // head/func is not included
            }
            for (int j = 0; j < bNodes.length; j++) {
                DependencyNode node = bNodes[j];
                str.append(this.getNodeLine(node)+"\t");
                if (node.addInfo != null) {
                    StringBuilder addInfoStr = new StringBuilder();
                    for (int k = 0; k < node.addInfo.size(); k++) {
                        addInfoStr.append(node.addInfo.get(k));
                        if (k != node.addInfo.size() -1) {
                            addInfoStr.append(" ");
                        }
                    }
                    str.append(addInfoStr.toString());
                }
                str.append("\n");
            }
        }
        str.append("EOS\n");
        // add debuginfo
        HashMap<String, String> debugInfoHash = tree.getDebugInfo();
        for (Iterator it = debugInfoHash.keySet().iterator(); it.hasNext();) {
            String key = (String) it.next();
            String value = debugInfoHash.get(key);
            str.append(key+"\t"+value);
        }

        return str.toString();
    }

    public String pipePerSentenceWithOrigAttr (DependencyTree tree) {

        StringBuilder str = new StringBuilder();
        Alphabet depId2SemId = new Alphabet();
        depId2SemId.lookupIndex("", true);

        int treeSize = tree.size();
	
        PredicateArgumentStructure[] pASList = tree.getPASList();

        Bunsetsu[] bunsetsuList = tree.getBunsetsuList();

        boolean[] argList = new boolean[treeSize];

        // create depId2SemId hash
        DependencyNode[] nodes = tree.getNodes();
        int bIdx = nodes[0].getId();
        int eIdx = nodes[nodes.length-1].getId();

        for (int p = 0; p < pASList.length; p++) {
            int[] argumentIds = pASList[p].argumentIds;
            String[] argumentLabels = pASList[p].argumentLabels;
            if (argumentIds == null) { continue; }
            for (int aIdx = 0; aIdx < argumentIds.length; aIdx++) {
                if (argumentIds[aIdx]-bIdx >= 0 && argumentIds[aIdx]-bIdx < treeSize) {
                    argList[argumentIds[aIdx]-bIdx] = true;
                }
            }
        }

        for (int aIdx = 0; aIdx < treeSize; aIdx++) {
            if (argList[aIdx]) { 
                depId2SemId.lookupIndex(nodes[aIdx].getId()+"", true);
            }
        }

        for (int b = 0; b < bunsetsuList.length; b++) {
            DependencyNode[] bNodes = bunsetsuList[b].getNodes();
            if (!bunsetsuList[b].getInfo().equals("")) {
                str.append(bunsetsuList[b].getInfo()+"\n");
            } else {
                str.append("* "+bunsetsuList[b].getId()+" "+bunsetsuList[b].getHead()+bunsetsuList[b].getDepLabel()+"\n"); // head/func is not included
            }
            for (int j = 0; j < bNodes.length; j++) {
                DependencyNode node = bNodes[j];
                str.append(this.getNodeLine(node)+"\t");
                StringBuilder semStr = new StringBuilder();
                boolean predFlag = false;
                for (int p = 0; p < pASList.length; p++) {
                    if (node.getId() == pASList[p].getPredicateId() && !predFlag) {
                        semStr.append(pASList[p].attrAll);
                        predFlag = true;
                    }
                }
                str.append(semStr.toString());
                str.append("\n");
            }
        }
        str.append("EOS\n");
        return str.toString();
    }
}
