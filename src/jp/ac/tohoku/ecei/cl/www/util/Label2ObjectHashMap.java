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

package jp.ac.tohoku.ecei.cl.www.util;

import java.util.Iterator;
import java.util.ArrayList;
import java.io.Serializable;

import gnu.trove.iterator.*;
import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.hash.*;

import jp.ac.tohoku.ecei.cl.www.base.*;

public class Label2ObjectHashMap<E> implements Serializable {

    private int size = 0;
    private Node root;
    
    private class Node implements Serializable {
	
	private int val;
	private E obj;
	private TIntObjectHashMap<Node> children;
	private TIntHashSet cIndices;

	// for debugging
	public int nodeType = 0; // 1: factor node,  2: label node

	Node (int val) {
	    this (val, null);
	}

	Node (int val, E obj) {
	    this.children = new TIntObjectHashMap<Node>();
	    this.cIndices = new TIntHashSet();
	    this.val = val;
	    this.obj = obj;
	}

	void addChild(int val) {
	    this.children.put(val, new Node(val, null));
	    this.cIndices.add(val);
	}

	Node getChild(int val) {
	    return this.cIndices.contains(val) ? this.children.get(val) : null;
	}
	
	TIntIterator getChildrenValsIterator() {
	    return cIndices.iterator();
	}

	boolean contains(int val) {
	    return this.cIndices.contains(val) ? true : false;
	}

	E getObject() {
	    return obj;
	}

	int getValue() {
	    return this.val;
	}

	void setObject(E obj) {
	    this.obj = obj;
	}
	
	public String toString() {
	    StringBuilder s = new StringBuilder();
	    s.append("val="+val+" children=");
	    for (TIntIterator it = getChildrenValsIterator(); it.hasNext(); ) {
		s.append(it.next()+",");
	    }
	    if (this.obj != null) {
		s.append(" obj="+this.obj.toString());
	    }
	    // for debugging
	    s.append(" nodetype="+this.nodeType);
	    return s.toString();
	}
    }

    public Label2ObjectHashMap() {
	this.root = new Node(-1, null);
    }

    private Node lookupNode (Label l, boolean add) {
	Node curNode = this.root;
	int groupNumber = l.getGroup();
	if (!curNode.contains(groupNumber)) {
	    if (!add) {
		return null;
	    }
	    curNode.addChild(groupNumber);
	    this.size++;
	}
	curNode = curNode.getChild(groupNumber);
	curNode.nodeType = 1;
	int lInt = l.getLabel();
	if (!curNode.contains(lInt)) {
	    if (!add) {
		return null;
	    }
	    curNode.addChild(lInt);
	    this.size++;
	}
	curNode = curNode.getChild(lInt);
	curNode.nodeType = 2;
	return curNode;
    }
    
    public void put (Label l, E obj) {
	Node node = lookupNode(l, true);
	node.setObject(obj);
    }

    public boolean containsKey (Label l) {
	Node node = lookupNode(l, false);
	return node == null ? false : true;
    }

    public E get (Label l) {
	return lookupNode(l, false).getObject();
    }

    public int size() {
	return this.size;
    }

    public void clear () {
	this.root = new Node(-1, null);
    }

    public Object[] getObjects() {
	ArrayList<E> objects = new ArrayList<E>();
	Node curNode = this.root;
	traverse(curNode, objects);
	return objects.toArray();
    }

    private void traverse(Node curNode, ArrayList<E> objects) {
	E obj = curNode.getObject();
	if (obj != null) {
	    objects.add(obj);
	}
	for (TIntIterator it = curNode.getChildrenValsIterator(); it.hasNext();) {
	    traverse(curNode.getChild(it.next()), objects);
	}
    }

    public String toString() {
	StringBuilder s = new StringBuilder();
	s.append("size="+this.size+"\n");
	toStringRecur(this.root, s);
	return s.toString();
    }

    private void toStringRecur(Node curNode, StringBuilder s) {
	E obj = curNode.getObject();
	s.append(curNode.toString()+"\n");
	for (TIntIterator it = curNode.getChildrenValsIterator(); it.hasNext();) {
	    toStringRecur(curNode.getChild(it.next()), s);
	}
    }

    public static void main(String[] args) {

	
	/*
	Label2ObjectHashMap hm = new Label2ObjectHashMap();
	Alphabet la = new Alphabet();
	String label1Str = "NONE";
	String label2Str = "A1";
	String label3Str = "A2";

	int group = 1;

	Label label1 = new Label(group, la, la.lookupIndex(label1Str));
	Label label2 = new Label(group, la, la.lookupIndex(label2Str));
	Label label3 = new Label(group, la, la.lookupIndex(label3Str));

	Labels label1 = new Labels(new Label(group, la, la.lookupIndex(label1Str)));
	Labels label2 = new Labels(new Label(group, la, la.lookupIndex(label2Str)));
	Labels label3 = new Labels(new Label(group, la, la.lookupIndex(label3Str)));

	//Labels labels1 = new Labels(label1, label2);
	String labels1Str = "NONE-A1";

	hm.put(label1, label1Str);
	hm.put(label2, label2Str);
	hm.put(label3, label3Str);
	//hm.put(labels1, labels1Str);
	System.out.println(hm.toString());

	//String[] labelStrRet = new String[]{(String) hm.get(label1), (String) hm.get(label2), (String) hm.get(label3)};
	String[] labelStrRet = new String[]{(String) hm.get(label1), (String) hm.get(label2), (String) hm.get(label3), (String) hm.get(labels1)};
	for (int i = 0; i < labelStrRet.length; i++) {
	    System.out.println("label"+(i+1)+"="+labelStrRet[i]);
	}

	*/
    }
}
