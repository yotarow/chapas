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

package jp.ac.tohoku.ecei.cl.www.chapas;

import java.util.*;

import jp.ac.tohoku.ecei.cl.www.base.*;
import jp.ac.tohoku.ecei.cl.www.util.*;

import gnu.trove.iterator.*;
import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.hash.*;

public class SRLArgumentPruner {

    public static int[] prune(DependencyTree tree, int predId) {
        return prune(tree, predId, null);
    }

    public static int[] prune(DependencyTree tree, int predId, HashSet argPOSSet) {
        //System.out.print("prune argPOSSet: "+(argPOSSet!=null));
        TIntHashSet nodeIds = new TIntHashSet();
        TIntHashSet footprints = new TIntHashSet();
        pruneRecur(tree,nodeIds,predId, footprints, argPOSSet);
        //System.out.print(" "+new TIntArrayList(nodeIds.toArray())+"\n");
        int[] nodes;
        if (nodeIds == null) {
            nodes = tree.getIds();
        } else {
            nodes = nodeIds.toArray();
        }
        Arrays.sort(nodes);
        return nodes;
    }
    
    public static void pruneRecur(DependencyTree tree, TIntHashSet nodeIds, int currentId,
                                  TIntHashSet footprints, HashSet argPOSSet) {
        DependencyNode currentNode = tree.getNodeFromId(currentId);
        if (footprints.contains(currentId)) {
            nodeIds = null;
            return;
        }
        footprints.add(currentId);
        int[] children = currentNode.getChildren();
        for (int i = 0; i < children.length; i++) {
            if (argPOSSet != null && argPOSSet.contains(tree.getNodeFromId(children[i]).getPOS()) ) {
                nodeIds.add(children[i]);
            } else {
                nodeIds.add(children[i]);
            }
        }
        if (currentNode.getHead() != 0) {
            pruneRecur(tree, nodeIds, currentNode.getHead(), footprints, argPOSSet);
        } else {
            //return;
        }
    }
        
    public static int[] prune2(DependencyTree tree, int predId) {
        return prune2(tree, predId, null);
    }

    public static int[] prune2(DependencyTree tree, int predId, HashSet argPOSSet) {
        //System.out.print("prune2 argPOSSet: "+(argPOSSet!=null));
        TIntHashSet nodeIds = new TIntHashSet();
        TIntHashSet footprints = new TIntHashSet();
        DependencyNode predNode = tree.getNodeFromId(predId);
        pruneRecur2(tree, nodeIds, predId, footprints, argPOSSet);
        int[] children = predNode.getChildren();
        for (int i = 0; i < children.length; i++) {
            DependencyNode cNode = tree.getNodeFromId(children[i]);
            int[] cChildren = cNode.getChildren();
            nodeIds.addAll(cChildren);
        }
        //System.out.print(" "+new TIntArrayList(nodeIds.toArray())+"\n");
        int[] nodes;
        if (nodeIds == null) {
            nodes = tree.getIds();
        } else {
            nodes = nodeIds.toArray();
        }
        Arrays.sort(nodes);
        return nodes;
    }

    public static void pruneRecur2(DependencyTree tree, TIntHashSet nodeIds, int currentId,
                                   TIntHashSet footprints, HashSet argPOSSet) {
        DependencyNode currentNode = tree.getNodeFromId(currentId);

        if (footprints.contains(currentId)) {
            //nodeIds = null;
            return;
        }

        footprints.add(currentId);
        int[] children = currentNode.getChildren();
        for (int i = 0; i < children.length; i++) {
            if (argPOSSet != null) {
                if (argPOSSet.contains(tree.getNodeFromId(children[i]).getPOS()) ) {
                    nodeIds.add(children[i]);
                    //System.out.print(" HOGE");
                } else {
                    //System.out.print(" SAGE");
                }
            } else {
                //System.out.println("SAGE");
                nodeIds.add(children[i]);
            }
        }
        if (currentNode.getHead() != 0) {
            pruneRecur2(tree, nodeIds, currentNode.getHead(), footprints, argPOSSet);
        } else {
            //return;
        }
    }

    public static int[] prune3(DependencyTree tree, int predId) {
        return prune3(tree, predId, null);
    }

    public static int[] prune3(DependencyTree tree, int predId, HashSet argPOSSet) {
        //System.out.print("prune3 argPOSSet: "+(argPOSSet!=null));
        TIntHashSet nodeIds = new TIntHashSet();
        TIntHashSet footprints = new TIntHashSet();
        DependencyNode predNode = tree.getNodeFromId(predId);
        pruneRecur3(tree, nodeIds, predId, footprints, argPOSSet);
        int[] children = predNode.getChildren();
        for (int i = 0; i < children.length; i++) {
            DependencyNode cNode = tree.getNodeFromId(children[i]);
            int[] cChildren = cNode.getChildren();
            nodeIds.addAll(cChildren);
        }
        for (int i = 0; i < children.length; i++) {
            pruneRecur3(tree, nodeIds, children[i], footprints, argPOSSet);
        }
        //System.out.print(" "+new TIntArrayList(nodeIds.toArray())+"\n");
        int[] nodes;
        if (nodeIds == null) {
            nodes = tree.getIds();
        } else {
            nodes = nodeIds.toArray();
        }
        Arrays.sort(nodes);
        return nodes;
    }

    public static void pruneRecur3(DependencyTree tree, TIntHashSet nodeIds, int currentId,
                                   TIntHashSet footprints, HashSet argPOSSet) {
        DependencyNode currentNode = tree.getNodeFromId(currentId);

        if (footprints.contains(currentId)) {
            //nodeIds = null;
            return;
        }

        footprints.add(currentId);
        int[] children = currentNode.getChildren();
        for (int i = 0; i < children.length; i++) {
            DependencyNode cNode = tree.getNodeFromId(children[i]);
            int[] cChildren = cNode.getChildren();
            for (int j = 0; j < cChildren.length; j++) {
                if (argPOSSet != null) {
                    if (argPOSSet.contains(tree.getNodeFromId(cChildren[j]).getPOS()) ) {
                        nodeIds.add(cChildren[j]);
                        //System.out.print(" HOGE");
                    } else {
                        //System.out.print(" SAGE");
                    }
                } else {
                    //System.out.println("SAGE");
                    nodeIds.add(cChildren[j]);
                }
            }
            if (argPOSSet != null) {
                if (argPOSSet.contains(tree.getNodeFromId(children[i]).getPOS()) ) {
                    nodeIds.add(children[i]);
                    //System.out.print(" HOGE");
                } else {
                    //System.out.print(" SAGE");
                }
            } else {
                //System.out.println("SAGE");
                nodeIds.add(children[i]);
            }
        }
        if (currentNode.getHead() != 0) {
            pruneRecur3(tree, nodeIds, currentNode.getHead(), footprints, argPOSSet);
        } else {
            //return;
        }
    }

    // for catalan and spanish
    public static int[] prune4(DependencyTree tree, int predId) {
        return prune4(tree, predId, null);
    }

    public static int[] prune4(DependencyTree tree, int predId, HashSet argPOSSet) {
        TIntHashSet nodeIds = new TIntHashSet();
        TIntHashSet footprints = new TIntHashSet();
        DependencyNode predNode = tree.getNodeFromId(predId);
        //pruneRecur4(tree, nodeIds, predId, footprints, argPOSSet);
        nodeIds.addAll(predNode.getChildren());
        int[] nodes;
        if (nodeIds == null) {
            nodes = tree.getIds();
        } else {
            nodes = nodeIds.toArray();
        }
        Arrays.sort(nodes);
        return nodes;
    }

    // for czech
    public static int[] prune5(DependencyTree tree, int predId) {
        return prune5(tree, predId, null);
    }

    public static int[] prune5(DependencyTree tree, int predId, HashSet argPOSSet) {
        TIntHashSet nodeIds = new TIntHashSet();
        TIntHashSet footprints = new TIntHashSet();
        DependencyNode predNode = tree.getNodeFromId(predId);
        nodeIds.addAll(predNode.getChildren());
        int[] children = predNode.getChildren();
        for (int i = 0; i < children.length; i++) {
            DependencyNode child = tree.getNodeFromId(children[i]);
            nodeIds.addAll(child.getChildren()); // add grandchildren;
            int[] grandchildren = child.getChildren();
            for (int j = 0; j < grandchildren.length; j++) {
                DependencyNode grandchild = tree.getNodeFromId(grandchildren[j]);
                nodeIds.addAll(grandchild.getChildren()); // add great grandchildren;
            }
        }
        int pHead = predNode.getHead();
        if (pHead != 0) {
            nodeIds.add(pHead);
            DependencyNode predParentNode = tree.getNodeFromId(pHead);
            nodeIds.addAll(predParentNode.getChildren());
        }
        int[] nodes;
        if (nodeIds == null) {
            nodes = tree.getIds();
        } else {
            nodes = nodeIds.toArray();
        }
        Arrays.sort(nodes);
        return nodes;
    }

    public static int[] pruneByPOS(DependencyTree tree, int predId, HashSet argPOSSet) {
        DependencyNode[] nodes = tree.getNodes();
        TIntHashSet nodeIds = new TIntHashSet();
        for (int i = 0; i < nodes.length; i++) {
            if (argPOSSet == null || argPOSSet.contains(nodes[i].getPOS())) {
                nodeIds.add(nodes[i].getId());
            }
        }
        int[] nodesInt;
        if (nodeIds == null) {
            nodesInt = tree.getIds();
        } else {
            nodesInt = nodeIds.toArray();
        }
        Arrays.sort(nodesInt);
        return nodesInt;
    }

    public static int[] leaveBunsetsuHeads (DependencyTree tree, int predId, HashSet argPOSSet) {
        Bunsetsu[] bList = tree.getBunsetsuList();
        TIntHashSet nodeIds = new TIntHashSet();
        for (int i = 0; i < bList.length; i++) {
            DependencyNode bHeadNode = bList[i].getHeadNode();
            if (argPOSSet == null || argPOSSet.contains(bHeadNode.getPOS())) {
                nodeIds.add(bHeadNode.getId());
            }
        }
        int[] nodesInt;
        if (nodeIds == null) {
            nodesInt = tree.getIds();
        } else {
            nodesInt = nodeIds.toArray();
        }
        Arrays.sort(nodesInt);
        return nodesInt;
    }

    public static int[] leaveBunsetsuHeadsAndInnerCands (DependencyTree tree, int predId, 
                                                         HashSet argPOSSet, HashSet<String> headPat) {
        Bunsetsu[] bList = tree.getBunsetsuList();
        TIntHashSet nodeIds = new TIntHashSet();

        // add inner arg cands
        Bunsetsu pB = tree.getBunsetsuFromNodeId(predId);
        DependencyNode[] pBNodes = pB.getNodes();
        for (int i = 0; i < pBNodes.length; i++) {
            if (pBNodes[i].getId() == predId) { continue; }
            if (headPat.contains(pBNodes[i].getPOS()) &&
                (argPOSSet == null || argPOSSet.contains(pBNodes[i].getPOS()))) {
                nodeIds.add(pBNodes[i].getId());
            }
        }

        // add heads
        for (int i = 0; i < bList.length; i++) {
            if (pB.getId() == bList[i].getId()) { continue; }
            DependencyNode bHeadNode = bList[i].getHeadNode();
            if (argPOSSet == null || argPOSSet.contains(bHeadNode.getPOS())) {
                nodeIds.add(bHeadNode.getId());
            }
        }
        int[] nodesInt;
        if (nodeIds == null) {
            nodesInt = tree.getIds();
        } else {
            nodesInt = nodeIds.toArray();
        }
        Arrays.sort(nodesInt);
        return nodesInt;
    }
}
