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

// Dependency Tree object to CoNLL2009 format file
package jp.ac.tohoku.ecei.cl.www.io;

import java.io.*;

import jp.ac.tohoku.ecei.cl.www.base.*;
import jp.ac.tohoku.ecei.cl.www.util.*;

public class CoNLL2009DependencyTree2CoNLL2009 implements DependencyTreeOutputPipe {
    private boolean usePredicted;
    private boolean usePredictedDep;

    public CoNLL2009DependencyTree2CoNLL2009() {
	this(false);
    }

    public CoNLL2009DependencyTree2CoNLL2009(boolean usePredicted) {
	this(usePredicted, false);
    }

    public CoNLL2009DependencyTree2CoNLL2009(boolean usePredicted, boolean usePredictedDep) {
	this.usePredicted = usePredicted;
	this.usePredictedDep = usePredictedDep;
    }

    public String pipe (DependencyTree tree) {
	String treeId = tree.getId();
	StringBuilder str = new StringBuilder();
	PredicateArgumentStructure[] pASs = tree.getPASList();

	if (treeId != null && !treeId.equals("")) {
	    str.append(treeId+"\n");
	}

	int treeSize = tree.size();
	for (int i = 0; i < treeSize; i++) {
	    DependencyNode node = tree.getNodeFromId(i+1);
	    str.append(node.id + "\t");
	    str.append(node.getForm() + "\t");
	    str.append((usePredicted?node.plemma:node.getLemma()) + "\t");
	    str.append((usePredicted?node.getLemma():node.plemma) + "\t");
	    str.append((usePredicted?node.ppos:node.getPOS()) + "\t");
	    str.append((usePredicted?node.getPOS():node.ppos) + "\t");

	    String[] feats1 = usePredicted ? node.pfeats : node.feats;

	    if (feats1.length == 1 && feats1[0].equals("_")) {
		str.append("_" + "\t");
	    } else {
		StringBuilder s = new StringBuilder();
		for (int j = 0; j < feats1.length; j++) {
		    s.append(feats1[j]);
		    if (j != feats1.length-1) {
			s.append("|");
		    }
		}
		str.append(s.toString() + "\t");
	    }
	    
	    String[] feats2 = usePredicted ? node.feats : node.pfeats;

	    if (feats2.length == 1 && feats2[0].equals("_")) {
		str.append("_" + "\t");
	    } else {
		StringBuilder s = new StringBuilder();
		for (int j = 0; j < feats2.length; j++) {
		    s.append(feats2[j]);
		    if (j != feats2.length-1) {
			s.append("|");
		    }
		}
		str.append(s.toString() + "\t");
	    }

	    if (usePredictedDep) {
		//str.append("_\t");
		str.append(node.phead + "\t");
		str.append(node.getHead()+"\t");
		str.append(node.pDepRel + "\t");
		str.append(node.getDepRel()+"\t");
	    } else {
		str.append(node.getHead() + "\t");
		str.append(node.phead + "\t");
		str.append(node.getDepRel() + "\t");
		str.append(node.pDepRel + "\t");
	    }

	    String fillPred = "_";
	    String pred = "_";
	    if (pASs != null) {
		for (int p = 0; p < pASs.length; p++) {
		    if (node.id == pASs[p].predicateId) {
			fillPred = "Y";
			pred = pASs[p].getPredicateSense();
		    }
		}
	    }
	    str.append(fillPred+"\t"+pred);

	    if (pASs != null) {
		for (int p = 0; p < pASs.length; p++) {
		    int[] argumentIds = pASs[p].argumentIds;
		    String[] argumentLabels = pASs[p].argumentLabels;
		    String arg = "_";
		    if (argumentIds == null) {
			str.append("\t"+arg);
		    } else {
			for (int ai = 0; ai < argumentIds.length; ai++) {
			    if (argumentIds[ai] == node.id) {
				arg = argumentLabels[ai];
			    }
			}
			str.append("\t"+arg);
		    }
		}
	    }
	    str.append("\n");
	}
	return str.toString();
    }

}
