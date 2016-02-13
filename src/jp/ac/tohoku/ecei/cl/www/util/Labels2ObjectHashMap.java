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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;
import java.io.Serializable;

import gnu.trove.iterator.*;
import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.hash.*;

import jp.ac.tohoku.ecei.cl.www.base.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Labels2ObjectHashMap<E> implements Serializable {

    private static Log log = LogFactory.getLog(Labels2ObjectHashMap.class);

    private int size = 0;
    private Node root;

    private TreeSet<Labels> labels;
    //private ArrayList<Labels> labels;
    //private SortedList<Labels> labels;

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
            s.append("val="+this.val+" children=");
            for (TIntIterator it = getChildrenValsIterator(); it.hasNext(); ) {
                s.append(it.next()+",");
            }
            if (this.obj != null) {
                //s.append(" obj="+this.obj.toString());
                s.append(" obj!=null");
            } else {
                s.append(" obj=null");
            }
            // for debugging
            s.append(" nodetype="+this.nodeType);
            return s.toString();
        }
    }

    private class Itr implements Iterator {
        Iterator<Labels> labelsIt;
        Labels currentKey;

        public Itr () {
            labelsIt = labels.iterator();
        }
        
        public boolean hasNext() {
            return labelsIt.hasNext();
        }

        public E next() {
            currentKey = labelsIt.next();
            return get(currentKey);
        }
            
        public Labels key() {
            currentKey = labelsIt.next();
            return currentKey;
        }
        
        public E value () {
            return get(currentKey);
        }
        
        public void remove () {}
    }

    public Labels2ObjectHashMap() {
        this.root = new Node(-1, null);
        this.labels = new TreeSet<Labels>();
        //this.labels = new HashSet<Labels>();
        //this.labels = new ArrayList<Labels>();
        //this.labels = new SortedList<Labels>();
    }

    private Node lookupNode (Labels ls, boolean add) {
        //System.err.println("[Labels2ObjectHashMap] lookupNode: "+ls);

        Node curNode = this.root;
        //log.info("ls="+ls.toString2());
        int cnt = 1;
        for (Iterator it = ls.iterator(); it.hasNext();) {
            Label l = (Label) it.next();
            int groupNumber = l.getGroup();
            //System.err.println(curNode.toString());
            //System.err.println("[lookupNode] label="+l.toString()+" groupNumber="+groupNumber);
            //System.err.println("[lookupNode] !curNode.contains(groupNumber)="+(!curNode.contains(groupNumber)));
            if (!curNode.contains(groupNumber)) {
                if (!add) {
                    return null;
                }
                curNode.addChild(groupNumber);
                this.labels.add(ls);
                //System.err.println("[Labels2ObjectHashMap] added: "+ls);
            }
            curNode = curNode.getChild(groupNumber);
            curNode.nodeType = 1;
            //System.err.println(curNode.toString());
            int lInt = l.getLabel();
            if (!curNode.contains(lInt)) {
                if (!add) {
                    return null;
                }
                curNode.addChild(lInt);
                this.labels.add(ls);
                //System.err.println("[Labels2ObjectHashMap] added: "+ls);
            } else {
                if (ls.size() == cnt) {
                    this.labels.add(ls);
                    if (ls.size() == 1 && !this.labels.contains(ls)) {
                        System.err.println("[Labels2ObjectHashMap] added: "+ls+" contains="+this.labels.contains(ls));
                    }
                }
            }
            curNode = curNode.getChild(lInt);
            curNode.nodeType = 2;
            //System.err.println(curNode.toString());
            cnt++;
        }
        return curNode;
    }
    
    private Node lookupNode (Label l, boolean add) {
        Node curNode = this.root;
        int groupNumber = l.getGroup();
        if (!curNode.contains(groupNumber)) {
            if (!add) {
                return null;
            }
            curNode.addChild(groupNumber);
            this.labels.add(new Labels(l));
        }
        curNode = curNode.getChild(groupNumber);
        curNode.nodeType = 1;
        int lInt = l.getLabel();
        if (!curNode.contains(lInt)) {
            if (!add) {
                return null;
            }
            curNode.addChild(lInt);
            this.labels.add(new Labels(l));
            //System.err.println("[Labels2ObjectHashMap] added: "+l);
        } else {
            Labels ls = new Labels(l);
            if (!this.labels.contains(ls)) {
                this.labels.add(ls);
                //System.err.println("[Labels2ObjectHashMap] added: "+ls);
            }
        }
        curNode = curNode.getChild(lInt);
        curNode.nodeType = 2;
        return curNode;
    }
    
    public void put (Labels ls, E obj) {
        //System.err.println("[Labels2ObjectHashMap] put("+ls.toString()+", "+obj);
        Node node = lookupNode(ls, true);
        node.setObject(obj);
        this.labels.add(ls);
    }

    public void put (Label l, E obj) {
        Node node = lookupNode(l, true);
        node.setObject(obj);
        this.labels.add(new Labels(l));
    }

    public boolean containsKey (Labels ls) {
        //System.err.println("[Labels2ObjectHashMap] containsKey("+ls.toString()+")");
        Node node = lookupNode(ls, false);
        if (node == null) {
            return false;
        } else if (node.getObject() == null) {
            return false;
        } else {
            return true;
        }
        //return node.getObject() == null ? false : true;
    }

    public boolean containsKey (Label l) {
        Node node = lookupNode(l, false);
        if (node == null) {
            return false;
        } else if (node.getObject() == null) {
            return false;
        } else {
            return true;
        }
    }

    public E get (Labels ls) {
        Node node = lookupNode(ls, false);
        return node != null ? node.getObject() : null;
        //return lookupNode(ls, false).getObject();
    }

    public E get (Label l) {
        Node node = lookupNode(l, false);
        return node != null ? node.getObject() : null;
        //return lookupNode(l, false).getObject();
    }

    public void clear () {
        this.root = new Node(-1, null);
    }
    
    public Iterator<Labels> getLabelsIterator() {
        return this.labels.iterator();
    }
    
    public Iterator<E> iterator() {
        return new Itr();
    }

    public Labels[] getLabels() {
        ArrayList<Labels> labelsAry = new ArrayList<Labels>();
        for (Iterator<Labels> it = getLabelsIterator(); it.hasNext();) {
            labelsAry.add(it.next());
        }
        return (Labels[]) labelsAry.toArray(new Labels[labelsAry.size()]);
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

        Labels2ObjectHashMap hm = new Labels2ObjectHashMap();
        Alphabet la = new Alphabet();
        String label1Str = "NONE";
        String label2Str = "A1";
        String label3Str = "A2";

        Label label1 = new Label(0, la, la.lookupIndex(label1Str));
        Label label2 = new Label(0, la, la.lookupIndex(label2Str));
        Label label3 = new Label(1, la, la.lookupIndex(label3Str));

        Labels labels1 = new Labels(label1, label3);
        String labels1Str = "NONE-A1";

        hm.put(label1, label1Str);
        hm.put(label2, label2Str);
        //hm.put(label3, label3Str);
        hm.put(labels1, labels1Str);

        System.out.println(hm.toString());        

        System.err.println(hm.containsKey(label1));
        System.err.println(hm.containsKey(label2));
        System.err.println(hm.containsKey(label3));
        System.err.println(hm.containsKey(labels1));

        String label1Get = (String) hm.get(label1);
        String label2Get = (String) hm.get(label2);
        String label3Get = (String) hm.get(label3);
        String labels1Get = (String) hm.get(labels1);

        System.err.println("label1Str="+label1Get);
        System.err.println("label2Str="+label2Get);
        System.err.println("label3Str="+label3Get);
        System.err.println("labels1Str="+labels1Get);

        /*
        //String[] labelStrRet = new String[]{(String) hm.get(label1), (String) hm.get(label2), (String) hm.get(label3)};
        String[] labelStrRet = new String[]{(String) hm.get(label1), (String) hm.get(label2), (String) hm.get(label3), (String) hm.get(labels1)};
        for (int i = 0; i < labelStrRet.length; i++) {
	    System.out.println("label"+(i+1)+"="+labelStrRet[i]);
        }
        */
    }
}
