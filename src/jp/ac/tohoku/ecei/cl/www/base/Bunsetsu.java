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

public class Bunsetsu implements DependencyElement, Serializable {
    private String info;
    private String role;
    private String name;

    private int id;
    private int head;
    private String depLabel;
    private DependencyNode[] nodes;
    private DependencyNode headNode;

    private int[] children;

    public String polarity;
    public String polarity2;
    public String remainingInfo;

    // for Natural Logic
    public String projectivity;
    public int downCnt = 0;

    public Bunsetsu (int id, int head, String depLabel, DependencyNode[] nodes) {
        this ("", id, head, depLabel, nodes);
    }
    
    public Bunsetsu (String info, int id, int head, String depLabel, DependencyNode[] nodes) {
        this.info = info;
        this.id = id;
        this.head = head;
        this.depLabel = depLabel;
        this.nodes = nodes;
    }

    public int size() {
        return this.nodes.length;
    }

    public void setChildren(int[] children) {
        this.children = children;
    }

    public int[] getChildren() {
        return this.children;
    }

    public void setName (String name) {
        this.name = name;
    }

    public DependencyNode getNode(int idx) {
        return this.nodes[idx];
    }

    public String getLemmaSeq () {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < nodes.length; i++) {
            s.append(nodes[i].getLemma());
        }
        return s.toString();
    }

    public String getContentWordsLemmaSeq () {
        StringBuilder s = new StringBuilder();
        ArrayList<DependencyNode> contentWordNodes = this.getContentWordNodes();
        for (int i = 0; i < contentWordNodes.size(); i++) {
            s.append(contentWordNodes.get(i).getLemma());
        }
        return s.toString();
    }

    public String getPOSSeq () {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < nodes.length; i++) {
            s.append(nodes[i].getPOS());
        }
        return s.toString();
    }

    public DependencyNode getNodeFromNodeId (int id) {
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i].getId() == id) {
                return nodes[i];
            }
        }
        return null;
    }
    
    public DependencyNode[] getNodes() {
        return this.nodes;
    }

    public void setHeadNode(DependencyNode node) {
        this.headNode = node;
    }

    public DependencyNode getHeadNode() {
        if (this.headNode != null) {
            return this.headNode;
        } else {
            int curNodeId = nodes[0].getId();
            while (curNodeId != -1) {
                DependencyNode node = getNodeFromNodeId(curNodeId);
                int head = node.getHead();
                if (this.contains(head)) {
                    curNodeId = head;
                } else {
                    return node;
                }
            }
            return nodes[0];
        }
    }

    public ArrayList<DependencyNode> getContentWordNodes() {
        DependencyNode headNode = this.getHeadNode();
        int cnt = this.nodes.length-1;
        while (cnt >= 0) {
            if (headNode.getId() == this.nodes[cnt].getId()) {
                break;
            }
            cnt--;
        }
        ArrayList<DependencyNode> contentWords = new ArrayList<DependencyNode>();
        for (int i = 0; i <= cnt; i++) {
            contentWords.add(this.nodes[i]);
        }
        return contentWords;
    }

    public String getInfo() {
        return this.info;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getHead() {
        return this.head;
    }

    public void setHead(int head) {
        this.head = head;
    }

    public String getDepLabel() {
        return this.depLabel;
    }

    public void setDepLabel(String depLabel) {
        this.depLabel = depLabel;
    }

    public String getName () {
        return this.name;
    }

    public boolean contains(int nodeId) {
        for (int i = 0; i < this.nodes.length; i++) {
            if (this.nodes[i].getId() == nodeId) {
                return true;
            }
        }
        return false;
    }

    public Object clone() {
        return new Bunsetsu(this.id, this.head, new String(this.depLabel), this.nodes);
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("[Bunsetsu id="+this.id+" head="+this.head+" depRel="+this.depLabel+"]\n");
        for (int i = 0; i < this.nodes.length; i++) {
            s.append(this.nodes[i].toString()+"\n");
        }

        if (this.children != null) {
            s.append("children:");
            for (int i = 0; i < this.children.length; i++) {
                s.append(" "+this.children[i]);
            }
        }
        return s.toString();
    }
}
