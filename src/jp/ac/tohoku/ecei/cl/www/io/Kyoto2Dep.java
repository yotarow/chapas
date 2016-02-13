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

public class Kyoto2Dep {

    private String file;
    private BufferedReader reader = null;
    private boolean eof = false;
    private String curSID = null;
    private Pattern p = Pattern.compile("(.*?)=\"(.*?)\"");

    public Kyoto2Dep (String file) {
        this.file = file;
    }

    /*
      public DependencyTree[] pipe() {
      int treeCnt = 0;
      ArrayList treeList = new ArrayList();
      try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(this.file), "UTF-8"));
      //BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(this.file), "EUC-JP"));
      Pattern p = Pattern.compile(" ");
      ArrayList<String> lines = new ArrayList<String>();
      if (this.curSID == null) {
      this.curSID = reader.readLine();
      }
      lines.add(this.curSID);
      String line;
      while( (line = reader.readLine() ) != null) {
      if (line.charAt(0) != '#' && lines.size()==0) {
      lines.add(this.curSID);
      }
      if (line.charAt(0) == '#') {
      this.curSID = line;
      treeList.add(parseLines(lines, 0));
      lines.clear();
      } else {
      lines.add(line);
      }
      }
      } catch (Exception e) {
      e.printStackTrace();
      }
      return (DependencyTree[]) treeList.toArray();
      }
    */

    public DependencyTree pipePerSentence () {
        DependencyTree tree = null;
        try {
            if (this.reader == null) {
                this.reader = new BufferedReader(new InputStreamReader(new FileInputStream(this.file), "UTF-8") );
            }
            ArrayList<String> lines = new ArrayList<String>();
            if (this.curSID == null) {
                this.curSID = reader.readLine();
            }
            lines.add(this.curSID);
            String line;
            while(tree == null) {
                line = reader.readLine();
                //System.out.println(line);
                if (line == null) {
                    tree = parseLines(lines, 0, null);
                    this.eof = true;
                    break;
                }
                if (line.charAt(0) != '#' && lines.size()==0) {
                    lines.add(this.curSID);
                }
                if (line.charAt(0) == '#' || line.equals("EOS")) {
                    this.curSID = line;
                    if (line.equals("EOS")) {
                        lines.add(line);
                    }
                    tree = parseLines(lines, 1, null);
                    lines.clear();
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println(e);
        }
        return (DependencyTree) tree;
    }

    public DependencyTree[] pipePerArticle () {
        ArrayList<DependencyTree> treeList = new ArrayList<DependencyTree>();
        int curId = 1;
        TIntIntHashMap semId2wordId = new TIntIntHashMap();
        try {
            if (this.reader == null) {
                this.reader = new BufferedReader(new InputStreamReader(new FileInputStream(this.file), "UTF-8") );
            }
            ArrayList<String> lines = new ArrayList<String>();
            if (this.curSID == null) {
                this.curSID = reader.readLine();
            }
            lines.add(this.curSID);
            String curArticleId = this.getArticleId(this.curSID);
            String articleId = curArticleId;
            String line = null;
            while(articleId.equals(curArticleId) && !this.eof) {
                line = reader.readLine();
                if (line == null) {
                    DependencyTree tree = parseLines(lines, curId, semId2wordId);
                    curId += tree.size();
                    treeList.add(tree);
                    this.eof = true;
                } else if (line.equals("")) {
                    continue;
                } else if (line.charAt(0) == '#') {
                    this.curSID = line;
                    articleId = this.getArticleId(line);
                    DependencyTree tree = parseLines(lines, curId, semId2wordId);
                    treeList.add(tree);
                    curId += tree.size();
                    lines.clear();
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println(e);
        }
        return (DependencyTree[]) treeList.toArray(new DependencyTree[treeList.size()]);
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
	
    private DependencyTree parseLines(ArrayList<String> lines, int firstWordID) {
        return parseLines(lines, firstWordID, null);
    }

    private DependencyTree parseLines(ArrayList<String> lines, int firstWordID, TIntIntHashMap semId2wordId) {
        if (semId2wordId == null) {
            semId2wordId = new TIntIntHashMap();
        }
        int size = lines.size();
        String idAll = "";
        String sId = "";
        ArrayList<Bunsetsu> bunsetsuList = new ArrayList<Bunsetsu>();
        ArrayList<String> bunsetsuLines = new ArrayList<String>();
        DependencyTree tree = null;
        int curWordId = firstWordID;
        int curBunsetsuId = -1;
        int curHeadId = -1;
        String curDepLabel = "";

        // create a dependency tree
        for (int i = 0; i < size; i++) {
            String line = lines.get(i);
            if (line.charAt(0) == '#') {
                idAll = line;
                sId = this.getSentenceId(line);
            } else if (line.charAt(0) == '*' || line.equals("EOS")) {
                int bSize = bunsetsuLines.size();
                //System.out.println("i="+i+", line="+lines.get(i)+" bSize="+bSize);
                if (bSize > 0) {
                    int[] id = new int[bSize];
                    String[] form = new String[bSize];
                    String[] yomi = new String[bSize];
                    String[] lemma = new String[bSize];
                    String[] pos = new String[bSize]; 
                    String[] pos2 = new String[bSize]; // fine-grained POS
                    String[] cType = new String[bSize];
                    String[] cForm = new String[bSize];
                    String[] sem = new String[bSize];
                    for (int j = 0; j < bSize; j++) {
                        String[] nE = bunsetsuLines.get(j).split("\t");

                        id[j] = curWordId++;
                        form[j] = nE[0];
                        yomi[j] = nE[1];
                        lemma[j] = nE[2];
                        pos[j] = nE[3];
                        cType[j] = nE[4];
                        cForm[j] = nE[5];
                        boolean predFlag = false;
                        String predType = "";
                        if (nE.length >= 7) {
                            sem[j] = nE[6];
                            String[] semE = sem[j].split(" ");
                            for (int k = 0; k < semE.length; k++) {
                                Matcher m = p.matcher(semE[k]);
				
                                while (m.find()) {
                                    String type = m.group(1);
                                    String value = m.group(2);
                                    if (type.equals("type")) {
                                        predFlag = true;
                                        predType = value;
                                    } else if (type.equals("ID")) {
                                        if (value.equals("exog") || value.equals("exo1") || value.equals("exo2") ) { 
                                            continue;
                                        }
                                        semId2wordId.put(Integer.parseInt(value), id[j]);
                                    }
                                }
                            }
                        }
                    }

                    DependencyNode[] nodes = new DependencyNode[bSize];
                    for (int j = 0; j < bSize; j++) {
                        nodes[j] = new DependencyNode(id[j],form[j],yomi[j],lemma[j],pos[j],
                                                      cType[j], cForm[j], -1, "");
                    }
                    Bunsetsu b = new Bunsetsu(curBunsetsuId, curHeadId, curDepLabel, nodes);
                    bunsetsuList.add(b);
                    if (line.equals("EOS")) {
                        tree = new DependencyTree(idAll, (Bunsetsu[]) bunsetsuList.toArray(new Bunsetsu[bunsetsuList.size()]));
                        tree.sentenceId = sId;
                    } 
                    bunsetsuLines.clear();
                }
                if (line.charAt(0) == '*') {
                    String[] bIDE = line.split(" ");
                    curBunsetsuId = Integer.parseInt(bIDE[1]);
                    curHeadId = Integer.parseInt(bIDE[2].substring(0, bIDE[2].length() - 1));
                    curDepLabel = bIDE[2].substring(bIDE[2].length() - 1, bIDE[2].length());
                }
            } else {
                bunsetsuLines.add(line);
            }
        }

        // create instances of predicate argument structures
        ArrayList<PredicateArgumentStructure> pASListAry = new ArrayList<PredicateArgumentStructure>();
        TIntArrayList argumentIds = new TIntArrayList();
        ArrayList<String> argumentLabels = new ArrayList<String>();
        curWordId = firstWordID;

        for (int i = 0; i < size; i++) {
            String line = lines.get(i);
            if (line.charAt(0) == '#') {
                idAll = line;
                sId = this.getSentenceId(line);
            } else if (line.charAt(0) == '*' || line.equals("EOS")) {
                int bSize = bunsetsuLines.size();

                if (bSize > 0) {
                    String[] sem = new String[bSize];
                    for (int j = 0; j < bSize; j++) {
                        int id = curWordId++;
                        DependencyNode node = tree.getNodeFromId(id);
                        String lemma = node.getLemma();
                        String[] nE = bunsetsuLines.get(j).split("\t");
                        boolean predFlag = false;
                        String predType = "";
                        if (nE.length >= 7) {
                            String[] semE = nE[6].split(" ");
                            for (int k = 0; k < semE.length; k++) {
                                Matcher m = p.matcher(semE[k]);
				
                                while (m.find()) {
                                    String type = m.group(1);
                                    String value = m.group(2);
                                    if (type.equals("type")) {
                                        predFlag = true;
                                        predType = value;
                                    } else if (type.equals("ID")) {
                                        // do nothing
                                    } else {
                                        // ga, o or ni
                                        argumentIds.add(semId2wordId.get(Integer.parseInt(value)));
                                        argumentLabels.add(type);
                                    }
                                }
                            }
                            if (predFlag) {
                                PredicateArgumentStructure pAS = new PredicateArgumentStructure(id, lemma,
                                                                                                argumentIds.toArray(),
                                                                                                (String[]) argumentLabels.toArray(new String[argumentLabels.size()]));
                                pASListAry.add(pAS);
                                argumentIds.clear();
                                argumentLabels.clear();
                            }
                        }
                    }
                    if (tree != null) {
                        tree.setPASList((PredicateArgumentStructure[]) pASListAry.toArray(new PredicateArgumentStructure[pASListAry.size()]));
                        bunsetsuLines.clear();
                    }
                }
            } else {
                bunsetsuLines.add(line);
            }
        }

        return tree;
    }
    
    public boolean eof() {
        return this.eof;
    }
}
