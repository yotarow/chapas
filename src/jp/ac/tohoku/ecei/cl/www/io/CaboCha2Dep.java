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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import gnu.trove.iterator.*;
import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.hash.*;

import jp.ac.tohoku.ecei.cl.www.base.*;
import jp.ac.tohoku.ecei.cl.www.util.*;

public class CaboCha2Dep {

    private InputStream is;
    private BufferedReader reader = null;
    private boolean eof = false;
    private String curSID = null;
    private Pattern p = Pattern.compile("(.*?)=\"(.*?)\"");
    
    private CaboChaFormat format = CaboChaFormat.UNK;

    public void setFormat (CaboChaFormat format) {
        this.format = format;
    }

    public CaboChaFormat getFormat () {
        return this.format;
    }

    public CaboCha2Dep () {
        this (null);
    }

    public CaboCha2Dep (InputStream is) {
        this.is = is;
    }

    public void guess (ArrayList<String> lines) {
        int formatNew = 0;
        int formatOld = 0;
        for (String line : lines) {
            if (line.equals("") ||
                line.charAt(0) == '*' ||
                line.charAt(0) == '#' ||
                line.equals("EOS")) { 
                continue;
            }
            String[] e = line.split(",");
            if (e.length >= 6) {
                formatNew++;
            } else {
                formatOld++;
            }
        }
        this.format = formatNew > formatOld ? CaboChaFormat.NEW : CaboChaFormat.OLD;
    }

    public DependencyTree pipePerSentence () {
        ArrayList<String> lines = new ArrayList<String>();
        try {
            if (this.reader == null) {
                this.reader = new BufferedReader(new InputStreamReader(this.is, "UTF-8"));
            }
            String line;
            boolean eos = false;
            while(!eos && !this.eof) {
                line = reader.readLine();
                if (line == null) {
                    this.eof = true;
                    eos = true;
                } else if (line.equals("")) {
                    continue;
                } else if (line.equals("EOS")) {
                    lines.add(line);
                    eos = true;
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println(e);
        }
        ArrayList<ArrayList<String>> lines2DAry = new ArrayList<ArrayList<String>>();
        lines2DAry.add(lines);
        DependencyTree[] trees = parseLines(lines2DAry);
        return trees[0];
    }
    
    public DependencyTree pipePerSentence (ArrayList<String> lines) {
        ArrayList<ArrayList<String>> lines2DAry = new ArrayList<ArrayList<String>>();
        lines2DAry.add(lines);
        DependencyTree[] trees = parseLines(lines2DAry);
        return trees[0];
    }

    public DependencyTree[] pipePerArticle () {
        ArrayList<DependencyTree> treeList = new ArrayList<DependencyTree>();
        ArrayList<ArrayList<String>> linesAry = new ArrayList<ArrayList<String>>();
        try {
            if (this.reader == null) {
                this.reader = new BufferedReader(new InputStreamReader(this.is, "UTF-8") );
            }
            ArrayList<String> lines = new ArrayList<String>();
            if (this.curSID == null) {
                String line = reader.readLine();
                this.curSID = line;
            }
            lines.add (this.curSID);
            String curArticleId = this.getArticleId (this.curSID);
            String articleId = curArticleId;
            String line = null;
            while (articleId.equals(curArticleId) && !this.eof) {
                line = reader.readLine();
                if (line == null) {
                    this.eof = true;
                    linesAry.add(lines);
                } else if (line.equals("")) {
                    continue;
                } else if (line.charAt(0) == '#') {
                    this.curSID = line;
                    articleId = this.getArticleId(line);
                    linesAry.add(lines);
                    lines = new ArrayList<String>();
                    lines.add(line);
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println(e);
        }
        DependencyTree[] trees = parseLines(linesAry);
        return trees;
    }
    
    private String getArticleId(String line) {
        String[] e1 = line.split(" ");
        String[] e2 = e1[1].split(":");
        String[] e3 = e2[1].split("-");
        return e3[0];
    }
	
    private String getSentenceId(String line) {
        String[] e1 = line.split(" ");
        String[] e2 = e1[1].split(":");
        String[] e3 = e2[1].split("-");
        return e3[1];
    }

    private boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private DependencyTree[] parseLines(ArrayList<ArrayList<String>> lines2DAry) {
        if (this.format == CaboChaFormat.UNK) {
            this.guess(lines2DAry.get(0));
        }

        TIntIntHashMap semId2wordId = new TIntIntHashMap();

        // create dependency trees 
        int numTrees = lines2DAry.size();
        DependencyTree[] trees = new DependencyTree[numTrees];

        int curWordId = 1;
        for (int tIdx = 0; tIdx < numTrees; tIdx++) {
            ArrayList<Bunsetsu> bunsetsuList = new ArrayList<Bunsetsu>();
            ArrayList<String> bunsetsuLines = new ArrayList<String>();
            String sId = null;
            String curDepLabel = "";
            String curBInfo = "";
            int curBunsetsuId = -1;
            int curHeadId = -1;

            ArrayList<String> linesAry = lines2DAry.get(tIdx);
            int size = linesAry.size();
            for (int i = 0; i < size; i++) {
                String line = linesAry.get(i);
                if (line.charAt(0) == '#') {
                    //System.err.println("line = "+line);
                    sId = line;
                } else if ( ( line.charAt(0) == '*' && line.charAt(1) != '\t') || line.equals("EOS")) {
                    int bSize = bunsetsuLines.size();
                    if (bSize > 0) {
                        int[] id = new int[bSize];
                        String[] form = new String[bSize];
                        String[] pos = new String[bSize];
                        String[] cType = new String[bSize];
                        String[] cForm = new String[bSize];
                        String[] lemma = new String[bSize];
                        String[] yomi = new String[bSize];
                        String[] pron = new String[bSize];
                        String[] ne = new String[bSize];
			
                        for (int j = 0; j < bSize; j++) {
                            id[j] = curWordId++;
                            String semStr = null;
                            boolean containsPAS = false;
                            String[] nE = bunsetsuLines.get(j).split("\t");

                            // CaboCha ver. <= 0.53

                            if (format == CaboChaFormat.OLD) {
                                form[j] = nE[0];
                                yomi[j] = nE[1];
                                lemma[j] = nE[2];
                                pos[j] = nE[3];
                                if (nE.length >= 5) {
                                    cType[j] = nE[4];
                                } else {
                                    cType[j] = "";
                                }
                                if (nE.length >= 6) {
                                    cForm[j] = nE[5];
                                } else {
                                    cForm[j] = "";
                                }
                                if (nE.length >= 7) {
                                    ne[j] = nE[6];
                                } else {
                                    ne[j] = "O";
                                }
                                containsPAS = nE.length >= 8 ? true : false;
                                if (containsPAS)
                                    semStr = nE[7];
          
                                //System.err.println("old form="+form[j]+" lemma="+lemma[j]+" pos="+pos[j]+" cType="+cType[j]+" cForm="+cForm[j]);
                            } else { // CaboCha ver. >= 0.6
                                form[j] = nE[0];
                                ne[j] = nE[2];
                                String[] nE2 = nE[1].split(",");
                                StringBuilder posStr = new StringBuilder();
                                for (int pIdx = 0; pIdx <=3; pIdx++) {
                                    posStr.append(nE2[pIdx]);
                                    if (pIdx+1 == 4 || nE2[pIdx+1].equals("*")) {
                                        break;
                                    } 
                                    posStr.append("-");
                                }

                                pos[j] = posStr.toString();
                                if (nE2.length >= 5) {
                                    cType[j] = nE2[4].equals("*") ? "" : nE2[4];
                                }
                                if (nE2.length >= 6) {
                                    cForm[j] = nE2[5].equals("*") ? "" : nE2[5];
                                }
                                if (nE2.length >= 7) {
                                    lemma[j] = nE2[6].equals("*") ? "" : nE2[6];
                                }
                                if (nE2.length >= 8) {
                                    yomi[j]  = nE2[7].equals("*") ? "" : nE2[7];
                                }
                                if (nE2.length >= 9) {
                                    pron[j]  = nE2[8].equals("*") ? "" : nE2[8];
                                }
                                containsPAS = nE.length >= 4 ? true : false;
                                if (containsPAS) {
                                    semStr = nE[3];
                                    //System.err.println("semStr="+semStr);
                                }
                                

                                //System.err.println("new form="+form[j]+" lemma="+lemma[j]+" pos="+pos[j]+" cType="+cType[j]+" cForm="+cForm[j]);
                            }

                            boolean predFlag = false;
                            String predType = "";
                            if (containsPAS) {
                                // nE[6]:"O", nE[7]: ga="1" etc.
                                String[] semE = semStr.split(" ");
                                for (int k = 0; k < semE.length; k++) {
                                    Matcher m = p.matcher(semE[k]);
                                    while (m.find()) {
                                        String type = m.group(1);
                                        String value = m.group(2);
                                        if (type.equalsIgnoreCase("ID")) {
                                            if (value.equalsIgnoreCase("exog") || 
                                                value.equalsIgnoreCase("exo1") || value.equalsIgnoreCase("exo2") ) { 
                                                // do nothing
                                            } else {
                                                semId2wordId.put(Integer.parseInt(value), id[j]);
                                            }
                                        } 
                                    }
                                }
                            }
                        }
                        DependencyNode[] nodes = new DependencyNode[bSize];
                        for (int j = 0; j < bSize; j++) {
                            nodes[j] = new DependencyNode(id[j],form[j],yomi[j],lemma[j],pos[j],
                                                          cType[j], cForm[j], -1, "");
                            nodes[j].pron = pron[j];
                            nodes[j].ne = ne[j];
                        }
                        Bunsetsu b = new Bunsetsu(curBInfo, curBunsetsuId, curHeadId, curDepLabel, nodes);
                        bunsetsuList.add(b);
                        if (line.equals("EOS")) {
                            trees[tIdx] = new DependencyTree(sId, (Bunsetsu[]) bunsetsuList.toArray(new Bunsetsu[bunsetsuList.size()]));
                            trees[tIdx].wholeSent = trees[tIdx].getSentence();
                            if (sId != null) {
                                trees[tIdx].sentenceId = this.getSentenceId(sId);
                            }
                        }
                        bunsetsuLines.clear();
                    }
                    if (line.charAt(0) == '*' && line.charAt(1) != '\t') {
                        //curBInfo = line;
                        String[] bIDE = line.split(" ");
                        //System.err.println(curBInfo);
                        if (bIDE.length >= 2 && isInteger(bIDE[1])) {
                            curBunsetsuId = Integer.parseInt(bIDE[1]);
                            curHeadId = Integer.parseInt(bIDE[2].substring(0, bIDE[2].length() - 1));
                            curDepLabel = bIDE[2].substring(bIDE[2].length() - 1, bIDE[2].length());
                        }
                        StringBuilder info = new StringBuilder();
                        if (bIDE.length >= 4) {
                            for (int k = 3; k < bIDE.length; k++) {
                                info.append(bIDE[k]);
                                if (k != bIDE.length-1) {
                                    info.append(" ");
                                }
                            }
                            curBInfo = info.toString();
                        }

                    }
                } else {
                    bunsetsuLines.add(line);
                }
            }
        }

        curWordId = 1;
        for (int tIdx = 0; tIdx < numTrees; tIdx++) {
            if (trees[tIdx] == null) { 
                continue;
            }
            ArrayList<String> linesAry = lines2DAry.get(tIdx);
            int size = linesAry.size();

            // create predicate argument structures
            ArrayList<PredicateArgumentStructure> pASListAry = new ArrayList<PredicateArgumentStructure>();
            TIntArrayList argumentIds = new TIntArrayList();
            ArrayList<String> argumentLabels = new ArrayList<String>();

            ArrayList<String> bunsetsuLines = new ArrayList<String>();
	    
            String sId = "";
            for (int i = 0; i < size; i++) {
                String line = linesAry.get(i);
                if (line.charAt(0) == '#') {
                    sId = line;
                } else if ( (line.charAt(0) == '*' && line.charAt(1) != '\t') || line.equals("EOS")) {
                    int bSize = bunsetsuLines.size();
                    if (bSize > 0) {
                        String[] sem = new String[bSize];
                        for (int j = 0; j < bSize; j++) {
                            int id = curWordId++;
                            DependencyNode node = trees[tIdx].getNodeFromId(id);
                            String lemma = node.getLemma();
                            String[] nE = bunsetsuLines.get(j).split("\t");
                            boolean predFlag = false;
                            int absPredId = -1;
                            String predType = "";
                            String voice = null;
                            String attrAll = null;
                            if (format == CaboChaFormat.OLD &&
                                nE.length >= 8) {
                                attrAll = nE[7];
                            } else if (format == CaboChaFormat.NEW &&
                                       nE.length >= 4) {
                                attrAll = nE[3];
                            }
                            if (attrAll != null) {
                                // nE[6]:"O", nE[7]: ga="1" etc.
                                String[] semE = attrAll.split(" ");
                                for (int k = 0; k < semE.length; k++) {
                                    Matcher m = p.matcher(semE[k]);
                                    while (m.find()) {
                                        String type = m.group(1);
                                        String value = m.group(2);
                                        if (type.equalsIgnoreCase("type")) {
                                            predFlag = true;
                                            predType = value;
                                        } else if (type.equalsIgnoreCase("ID")) {
                                            // do nothing
                                        } else if (type.equalsIgnoreCase("alt")) {
                                            // active, passive, causative, causative/passive
                                            voice = value;
                                        } else if (type.equalsIgnoreCase("pred_id")) {
                                            absPredId = Integer.parseInt(value);
                                            /*
                                              } else if (type.equalsIgnoreCase("pred_bf")) {
                                              // do nothing
                                              } else if (type.equalsIgnoreCase("ga_type")) {
                                              // do nothing
                                              } else if (type.equalsIgnoreCase("o_type")) {
                                              // do nothing
                                              } else if (type.equalsIgnoreCase("ni_type")) {
                                              // do nothing
                                              } else if (type.equalsIgnoreCase("ga_prob_id")) {
                                              // ga problem id
                                              } else if (type.equalsIgnoreCase("o_prob_id")) {
                                              // o problem id
                                              } else if (type.equalsIgnoreCase("ni_prob_id")) {
                                              // ni problem id
                                              */
                                        } else if (type.equalsIgnoreCase("ga") || 
                                                   type.equalsIgnoreCase("o") ||
                                                   type.equalsIgnoreCase("wo") ||
                                                   type.equalsIgnoreCase("ni") ) {
                                            // ga, o or ni
                                            if (value.equals("exo1") || value.equals("exo2") || value.equals("exog")) {
                                                // do nothing
                                            } else {
                                                int wordId = semId2wordId.get(Integer.parseInt(value));
                                                if (wordId != 0 && !argumentIds.contains(wordId)) {
                                                    argumentIds.add(wordId);
                                                    argumentLabels.add(type);
                                                } else {
                                                    // the value is not included in semId2wordId hash
                                                    //System.out.println(sId);
                                                    //System.err.println("type="+type+" value="+value+" semId2wordId:"+new TIntArrayList(semId2wordId.keys()));
                                                }
                                            }
                                        }
                                    }
                                }
                                if (predFlag) {
                                    PredicateArgumentStructure pAS = new PredicateArgumentStructure(id, lemma,
                                                                                                    argumentIds.toArray(),
                                                                                                    (String[]) argumentLabels.toArray(new String[argumentLabels.size()]));
                                    pAS.absPredId = absPredId;
                                    pAS.attrAll = attrAll;
                                    pAS.predicateType = predType;
                                    pAS.voice = voice;
                                    pASListAry.add(pAS);
                                    argumentIds.clear();
                                    argumentLabels.clear();
                                }
                                //System.err.println("absPredId="+absPredId+" voice="+voice+" predFlag="+predFlag);
                            }
                        }
                        trees[tIdx].setPASList((PredicateArgumentStructure[]) pASListAry.toArray(new PredicateArgumentStructure[pASListAry.size()]));
                        trees[tIdx].sortPASList();
                        bunsetsuLines.clear();
                    }
                } else {
                    bunsetsuLines.add(line);
                }
            }
        }
        return trees;
    }
        
    public boolean eof() {
        return this.eof;
    }
}
