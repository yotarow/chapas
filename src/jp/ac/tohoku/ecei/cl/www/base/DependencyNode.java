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

import java.io.Serializable;

import java.util.ArrayList;

public class DependencyNode implements Serializable {

    // base fields

    public int id; 
    public String form = null; // 2008:splitForm, 2009:form
    public String lemma = null;// 2008:splitLemma, 2009:lemma 
    public String pos = null;  // 2008:pposs, 2009:pos 
    public String word = null; // this field is used as a word feature
    public int head;    // 2008:head,  2009:head
    public String depRel = null;

    public int[] children;

    public int phead;
    public String pDepRel = null;

    public String ne = null;
    public String event = null;

    // fields for CoNLL-2008
    public String form2 = null;  // form
    public String lemma2 = null; // lemma 
    public String pos2 = null;   // gpos
    public String pos3 = null;   // ppos

    public String conll2003 = null;
    public String bbn = null;
    public String wnss = null;
    public String maltHead = null;
    public String maltDepRel = null;

    // fields for CoNLL-2009
    public String plemma = null;
    public String ppos = null;
    public String[] feats = null;
    public String[] pfeats = null;

    // for japanese 
    public String yomi = null;
    public String pron = null;
    public String cType = null;
    public String cForm = null;

    // for ntc conversion
    public ArrayList<String> addInfo = null;

    public DependencyNode(int id, String form, String lemma, String pos, int head, String depRel) {
        this.id = id;
        this.form = form;
        this.lemma = lemma;
        this.pos = pos;
        this.head = head;
        this.depRel = depRel;
    }
    
    // constructor for japanese
    public DependencyNode(int id, String form, String yomi, String lemma, String pos, String cType, String cForm, int head, String depRel) {
        this.id = id;
        this.form = form;
        this.yomi = yomi;
        this.lemma = lemma;
        this.pos = pos;
        this.cType = cType;
        this.cForm = cForm;
        this.head = head;
        this.depRel = depRel;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public String getPOS() {
        return this.pos;
    }

    public String getForm() {
        return this.form;
    }

    public String getLemma() {
        return this.lemma;
    }

    public String getWord() {
        return this.word;
    }

    public int getHead() {
        return this.head;
    }

    public String getDepRel() {
        return this.depRel;
    }

    // for CoNLL-2009
    public String[] getFeats() {
        return this.feats;
    }

    public int[] getChildren() {
        return this.children;
    }

    public void setHead(int head) {
        this.head = head;
    }

    public void setDepRel(String depRel) {
        this.depRel = depRel;
    }

    public void setChildren(int[] children) {
        this.children = children;
    }

    public Object clone() {
        DependencyNode node = new DependencyNode(id, new String(form), new String(lemma), new String(pos), head, new String(depRel));

        //CoNLL-2008 fields
        if (form2 != null) { node.form2 = new String(form2); }
        if (lemma2 != null) { node.lemma2 = new String(lemma2); }
        if (pos2 != null) { node.pos2 = new String(pos2); }
        if (pos3 != null) { node.pos3 = new String(pos3); }
	
        //CoNLL-2009 fields
        if (plemma != null) { node.plemma = new String(plemma); }
        if (ppos != null) { node.ppos = new String(ppos); }
        if (feats != null) {
            node.feats = new String[feats.length];
            for (int i = 0; i < feats.length; i++) {
                node.feats[i] = new String(feats[i]);
            }
        }
        if (pfeats != null) {
            node.pfeats = new String[pfeats.length];
            for (int i = 0; i < pfeats.length; i++) {
                node.pfeats[i] = new String(pfeats[i]);
            }
        }
        node.phead = phead;
        if (pDepRel != null) { node.pDepRel = new String(pDepRel); }
        return node;
    }

    public boolean equals(DependencyNode n) {
        if (this.id != n.getId()) { return false; }
        if (!this.getForm().equals(n.getForm())) { return false; }
        if (!this.getLemma().equals(n.getLemma())) { return false; }
        if (!this.getPOS().equals(n.getPOS())) { return false; }
        if (this.getHead() != n.getHead()) { return false; }
        if (!this.getDepRel().equals(n.getDepRel())) { return false; }
        return true;
    }

    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("[DependencyNode]");
        str.append(" id="+this.id);
        str.append(" form=" + this.getForm());
        str.append(" lemma=" + this.getLemma());
        str.append(" pos=" + this.getPOS());
        str.append(" head=" + this.getHead());
        str.append(" deprel=" + this.getDepRel());
        if (this.feats != null) {
            StringBuilder s = new StringBuilder();
            s.append(" feats=");
            for (int j = 0; j < this.feats.length; j++) {
                s.append(this.feats[j]);
                if (j != this.feats.length-1) {
                    s.append("|");
                }
            }
            str.append(s.toString() + "\t");
        }
        return str.toString();
    }
}
