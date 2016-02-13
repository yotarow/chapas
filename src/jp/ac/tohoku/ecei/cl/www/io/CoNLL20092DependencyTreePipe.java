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

import gnu.trove.iterator.*;
import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.hash.*;

import jp.ac.tohoku.ecei.cl.www.base.*;
import jp.ac.tohoku.ecei.cl.www.util.*;

public class CoNLL20092DependencyTreePipe implements DependencyTreePipe {
  
    private static final boolean USE_PREDICTED = true;
    private InputStream is;
    private BufferedReader reader = null;
    private boolean eof = false;
    private boolean useLemma;
    private boolean usePredicted;
    private boolean usePredictedDep;

    public CoNLL20092DependencyTreePipe (InputStream is) {
	this(is, false);
    }

    public CoNLL20092DependencyTreePipe (InputStream is, boolean useLemma) {
	this(is, useLemma, false);
    }

    public CoNLL20092DependencyTreePipe (InputStream is, boolean useLemma, boolean usePredicted) {
	this(is, useLemma, usePredicted, false);
    }

    public CoNLL20092DependencyTreePipe (InputStream is, boolean useLemma, boolean usePredicted, boolean usePredictedDep) {
	this.is = is;
	this.useLemma = useLemma;
	this.usePredicted = usePredicted;
	this.usePredictedDep = usePredictedDep;
    }

    public DependencyTree[] pipe () {
	int treeCnt = 0;
	long fAddTotal = 0;
	ArrayList treeList = new ArrayList();
	try {
	    BufferedReader reader = new BufferedReader(new InputStreamReader(this.is, "UTF-8") );
	    String line;
	    Pattern p = Pattern.compile(" ");
	    char sep = ' ';
	    ArrayList linesAry = new ArrayList();
	    int lineCnt = 0;
	    String treeId = "";
	    while( (line = reader.readLine() ) != null) {
		if (line.length() >= 1 && line.charAt(0) == '#') { 
		    treeId = line;
		    continue;
		}
		if (line.equals("")) {
		    String[] lines = (String[]) linesAry.toArray(new String[lineCnt]);

		    int[] id = new int[lines.length];
		    String[] form = new String[lines.length];
		    String[] word = new String[lines.length];
		    String[] lemma = new String[lines.length];
		    String[] plemma = new String[lines.length];
		    String[] gpos = new String[lines.length];
		    String[] ppos = new String[lines.length];
		    String[][] feats = new String[lines.length][];
		    String[][] pfeats = new String[lines.length][];
		    int[] head = new int[lines.length];
		    int[] phead = new int[lines.length];
		    String[] depRel = new String[lines.length];
		    String[] pDepRel = new String[lines.length];
		    String[] fillPred = new String[lines.length];
		    String[] pred = new String[lines.length];

		    String[][] args = new String[lines.length][];

		    int prednum = 0;
		    TIntArrayList predicatesIdAry = new TIntArrayList();
		    ArrayList predicatesSenseAry = new ArrayList();

		    for (int i = 0; i < lines.length; i++) {
			String[] e = lines[i].split("\t");
			if (e.length > 12 && e[12].equals("Y")) {
			    prednum++;
			}
		    }

		    for (int i = 0; i < lines.length; i++) {
			String[] e = lines[i].split("\t");
			id[i] = Integer.parseInt(e[0]);
			form[i] = e[1];
			lemma[i] = e[2];
			word[i] = useLemma ? normalize(lemma[i]) : normalize(form[i]);
			plemma[i] = e[3];
			gpos[i] = e[4];
			ppos[i] = e[5];
			feats[i] = e[6].equals("_") ? new String[]{"_"} : e[6].split("\\|");
			pfeats[i] = e[7].equals("_") ? new String[]{"_"} : e[7].split("\\|");
			head[i] = e[8].equals("_") ? -1 : Integer.parseInt(e[8]);
			phead[i] = e[9].equals("_") ? -1 : Integer.parseInt(e[9]);
			if (e.length >= 11) {
			    depRel[i] = e[10];
			}
			if (e.length >= 12) {
			    pDepRel[i] = e[11];
			}
			if (e.length >= 13) {
			    fillPred[i] = e[12];
			}
			if (e.length >= 14) {
			    pred[i] = e[13];
			}

			if (prednum >= 1) {
			    args[i] = new String[prednum];
			}
			for (int j = 0; j < prednum; j++) {
			    if (e.length > 14 + j) {
				args[i][j] = e[14 + j];
			    }
			}
			if (e.length >= 13 && e[12].equals("Y")) {
			    predicatesIdAry.add(id[i]);
			    if (e.length >= 14) {
				predicatesSenseAry.add(e[13]);
			    }
			}
		    }

		    DependencyNode[] nodes = new DependencyNode[lines.length];
		    for (int i = 0; i < lines.length; i++) {
			nodes[i] = new DependencyNode(id[i], form[i], 
						      usePredicted ? plemma[i] : lemma[i], 
						      usePredicted ? ppos[i] : gpos[i],
						      usePredictedDep ? phead[i] : head[i], 
						      usePredictedDep ? pDepRel[i] : depRel[i]);
			nodes[i].word = useLemma ? (usePredicted ? normalize(plemma[i]) : normalize(lemma[i])) : normalize(form[i]);
			nodes[i].plemma = usePredicted ? lemma[i] : plemma[i];
			nodes[i].ppos = usePredicted ? gpos[i] : ppos[i];
			nodes[i].feats = usePredicted ? pfeats[i] : feats[i];
			nodes[i].pfeats = usePredicted ? feats[i] : pfeats[i];
			nodes[i].phead = usePredictedDep ? head[i] : phead[i];
			nodes[i].pDepRel = usePredictedDep ? depRel[i] : pDepRel[i];
		    }
		    ArrayList pASList = new ArrayList();
		    int[] predicateIds = predicatesIdAry.toArray();
		    
		    String[] predicateSenses = (String[]) predicatesSenseAry.toArray(new String[predicateIds.length]);

		    for (int i = 0; i < predicateIds.length; i++) {
			TIntArrayList argIndices = new TIntArrayList();
			ArrayList argLabels = new ArrayList();
			int argcnt = 0;
			for (int j = 0; j < lines.length; j++) {
			    if (args != null && j < args.length && args[j] != null && i < args[j].length && args[j][i] !=null && !args[j][i].equals("_")) {
				argIndices.add(id[j]);
				argLabels.add(args[j][i]);
				argcnt++;
			    }
			}
			pASList.add(new PredicateArgumentStructure(predicateIds[i], predicateSenses[i], argIndices.toArray(),
								   (String[]) argLabels.toArray(new String[argcnt]) ) );
		    }
		    DependencyTree tree = new DependencyTree(treeId, nodes, (PredicateArgumentStructure[]) pASList.toArray(new PredicateArgumentStructure[predicateIds.length]) );
		    treeList.add(tree);
		    treeCnt++;
		    lineCnt = 0;
		    linesAry.clear();
		} else {
		    linesAry.add(line);
		    lineCnt++;
		}
	    }
	    reader.close();
	} catch (FileNotFoundException e) {
	    System.out.println(e);
	} catch (IOException e) {
	    System.out.println(e);
	}
	return (DependencyTree[]) treeList.toArray(new DependencyTree[treeCnt]);
    }

    public DependencyTree partialPipe () {
	DependencyTree tree = null;
	try {
	    if (this.reader == null) {
		this.reader = new BufferedReader(new InputStreamReader(this.is, "UTF-8") );
	    }
	    String line;
	    Pattern p = Pattern.compile(" ");
	    char sep = ' ';
	    ArrayList linesAry = new ArrayList();
	    int lineCnt = 0;
	    while(tree == null) {
		line = reader.readLine();
		if (line == null) {
		    this.eof = true;
		    return null;
		}
		if (line.equals("")) {
		    String[] lines = (String[]) linesAry.toArray(new String[lineCnt]);

		    int[] id = new int[lines.length];
		    String[] form = new String[lines.length];
		    String[] lemma = new String[lines.length];
		    String[] plemma = new String[lines.length];
		    String[] gpos = new String[lines.length];
		    String[] ppos = new String[lines.length];
		    String[][] feats = new String[lines.length][];
		    String[][] pfeats = new String[lines.length][];
		    int[] head = new int[lines.length];
		    int[] phead = new int[lines.length];
		    String[] depRel = new String[lines.length];
		    String[] pDepRel = new String[lines.length];
		    String[] fillPred = new String[lines.length];
		    String[] pred = new String[lines.length];

		    String[][] args = new String[lines.length][];
		    int prednum = 0;

		    TIntArrayList predicatesIdAry = new TIntArrayList();
		    ArrayList predicatesSenseAry = new ArrayList();

		    for (int i = 0; i < lines.length; i++) {
			String[] e = lines[i].split("\t");
			if (e.length > 12 && e[12].equals("Y")) {
			    prednum++;
			}
		    }

		    for (int i = 0; i < lines.length; i++) {
			String[] e = lines[i].split("\t");
			id[i] = Integer.parseInt(e[0]);
			form[i] = e[1];
			lemma[i] = e[2];
			plemma[i] = e[3];
			gpos[i] = e[4];
			ppos[i] = e[5];
			feats[i] = e[6].split("\\|");
			pfeats[i] = e[7].split("\\|");
			head[i] = e[8].equals("_") ? -1 : Integer.parseInt(e[8]);
			phead[i] = e[9].equals("_") ? -1 : Integer.parseInt(e[9]);
			if (e.length >= 11) {
			    depRel[i] = e[10];
			}
			if (e.length >= 12) {
			    pDepRel[i] = e[11];
			}
			if (e.length >= 13) {
			    fillPred[i] = e[12];
			}
			if (e.length >= 14) {
			    pred[i] = e[13];
			}

			if (prednum >= 1) {
			    args[i] = new String[prednum];
			}

			for (int j = 0; j < prednum; j++) {
			    if (e.length > 14 + j) {
				args[i][j] = e[14 + j];
			    }
			}
			if (e.length >= 13 && e[12].equals("Y")) {
			    predicatesIdAry.add(id[i]);
			    if (e.length >= 14) {
				predicatesSenseAry.add(e[13]);
			    }
			}
		    }
		    
		    DependencyNode[] nodes = new DependencyNode[lines.length];
		    for (int i = 0; i < lines.length; i++) {
			nodes[i] = new DependencyNode(id[i], form[i], 
						      usePredicted ? plemma[i] : lemma[i], 
						      usePredicted ? ppos[i] : gpos[i], 
						      usePredictedDep ? phead[i] : head[i],
						      usePredictedDep ? pDepRel[i] : depRel[i]);
			nodes[i].word = useLemma ? (usePredicted ? normalize(plemma[i]) : normalize(lemma[i])) : normalize(form[i]);
			nodes[i].plemma = usePredicted ? lemma[i] : plemma[i];
			nodes[i].ppos = usePredicted ? gpos[i] : ppos[i];
			nodes[i].feats = usePredicted ? pfeats[i] : feats[i];
			nodes[i].pfeats = usePredicted ? feats[i] : pfeats[i];
			nodes[i].phead = usePredictedDep ? head[i] : phead[i];
			nodes[i].pDepRel = usePredictedDep ? depRel[i] : pDepRel[i];
		    }
		    ArrayList pASList = new ArrayList();
		    int[] predicateIds = predicatesIdAry.toArray();
		    String[] predicateSenses = (String[]) predicatesSenseAry.toArray(new String[predicateIds.length]);

		    for (int i = 0; i < predicateIds.length; i++) {
			TIntArrayList argIndices = new TIntArrayList();
			ArrayList argLabels = new ArrayList();
			int argcnt = 0;
			for (int j = 0; j < lines.length; j++) {
			    if (args != null && j < args.length && args[j] != null && i < args[j].length && args[j][i] !=null && !args[j][i].equals("_")) {
				argIndices.add(id[j]);
				argLabels.add(args[j][i]);
				argcnt++;
			    }
			}
			pASList.add(new PredicateArgumentStructure(predicateIds[i], predicateSenses[i], argIndices.toArray(),
								   (String[]) argLabels.toArray(new String[argcnt]) ) );
		    }
		    tree = new DependencyTree(nodes, (PredicateArgumentStructure[]) pASList.toArray(new PredicateArgumentStructure[predicateIds.length]) );
		} else {
		    linesAry.add(line);
		    lineCnt++;
		}
	    }
	} catch (IOException e) {
	    System.out.println(e);
	}
	return tree;
    }

    public boolean eof() {
	return this.eof;
    }

    public static String normalize (String s) {    
	if(s.matches("[0-9]+|[0-9]+\\.[0-9]+|[0-9]+[0-9,]+") ) {
	    return "<num>";
	}
	return s;
    }
}
