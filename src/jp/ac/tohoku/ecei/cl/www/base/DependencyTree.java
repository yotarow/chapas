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

import java.lang.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.Serializable;

import gnu.trove.iterator.*;
import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.hash.*;

public class DependencyTree implements Serializable, Cloneable {

    private static DependencyNode ROOT_NODE = new DependencyNode(0, "ROOT", "ROOT", "ROOT", -1, null);
    private static Bunsetsu ROOT_BUNSETSU = new Bunsetsu(0, -1, "BUNSETSU_ROOT", new DependencyNode[] {ROOT_NODE});

    private DependencyNode[] nodes;
    private Bunsetsu[] bunsetsuList;
    private PredicateArgumentStructure[] pASList;

    private PredicateArgumentStructure[] bunsetsuPASList;
    private ArrayList<PredicateArgumentStructure>[] pASListNBest;

    private ModalityInfo[] modList;

    private TIntIntHashMap id2idx;
    private TIntIntHashMap id2BunsetsuIdx;

    private String id;
    private double score;
    public String sentenceId;

    // for srr
    public String wholeSent;
    public String sentRole;
    
    // for debugging
    public HashMap<String, String> debugInfoHash;

    public DependencyTree(DependencyNode[] nodes) {
        this(null, nodes, null);
    }

    public DependencyTree(Bunsetsu[] bunsetsuList) {
        this (null, null, bunsetsuList, null);
    }

    public DependencyTree(String id, Bunsetsu[] bunsetsuList) {
        this (id, null, bunsetsuList, null);
    }

    public DependencyTree(DependencyNode[] nodes, PredicateArgumentStructure[] pASList) {
        this ("", nodes, null, pASList);
    }

    public DependencyTree(Bunsetsu[] bunsetsuList, PredicateArgumentStructure[] pASList) {
        this ("", null, bunsetsuList, pASList);
    }

    public DependencyTree(String id, DependencyNode[] nodes, PredicateArgumentStructure[] pASList) {
        this (id, nodes, null, pASList);
    }

    public DependencyTree(String id, Bunsetsu[] bunsetsuList, PredicateArgumentStructure[] pASList) {
        this (id, null, bunsetsuList, pASList);
    }

    public DependencyTree(String id, DependencyNode[] nodes, Bunsetsu[] bunsetsuList, PredicateArgumentStructure[] pASList) {
        if (nodes == null && bunsetsuList != null) {
            int size = 0;
            for (int i = 0; i < bunsetsuList.length; i++) {
                size += bunsetsuList[i].size();
            }
            DependencyNode[] bNodesAll = new DependencyNode[size];
            int idx = 0;
            for (int i = 0; i < bunsetsuList.length; i++) {
                DependencyNode[] bNodes = bunsetsuList[i].getNodes();
                for (int j = 0; j < bNodes.length; j++) {
                    bNodesAll[idx++] = bNodes[j];
                }
            }
            this.nodes = bNodesAll;
        } else {
            this.nodes = nodes;
        }

        this.id = id;
        this.bunsetsuList = bunsetsuList;
        this.pASList = pASList;
        //this.pASList = new ArrayList<PredicateArgumentStructure>();
        //for (int i = 0; i < pASList.length; i++) {
        //this.pASList.add(pASList[i]);
        //}
        this.init();
        this.score = 0.0;

        this.debugInfoHash = new HashMap<String, String>();
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void init() {
        this.id2idx = new TIntIntHashMap();

        for (int i = 0; i < nodes.length; i++) {
            this.id2idx.put(nodes[i].getId(), i);
        }
        setChildren();

        if (this.bunsetsuList != null) {
            this.id2BunsetsuIdx = new TIntIntHashMap();
            for (int i = 0; i < bunsetsuList.length; i++) {
                this.id2BunsetsuIdx.put(bunsetsuList[i].getId(), i);
            }
            setBunsetsuChildren();
        }
    }

    public String getSentence () {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < nodes.length; i++) {
            s.append(nodes[i].getForm());
        }
        return s.toString();
    }

    public void resetNodeIds(int beginFrom) {
        TIntIntHashMap oldId2newIdMap = new TIntIntHashMap();
        int curIdx = beginFrom;
        for (int i = 0; i < nodes.length; i++) {
            int oldId = nodes[i].getId();
            int newId = curIdx++;
            nodes[i].setId(newId);
            oldId2newIdMap.put(oldId, newId);
        }
	
        for (int i = 0; i < nodes.length; i++) {
            nodes[i].setHead(oldId2newIdMap.get(nodes[i].getHead()));
        }
	
        for (int i = 0; i < pASList.length; i++) {
            int newPredId = oldId2newIdMap.get(pASList[i].getPredicateId());
            pASList[i].setPredicateId(newPredId);
            int[] ids = pASList[i].getIds();
            int[] newIds = new int[ids.length];
            for (int j = 0; j < ids.length; j++) {
                newIds[j] = oldId2newIdMap.get(ids[j]);
            }
            pASList[i].setIds(newIds);
        }	    
    }

    public void setChildren() {
        for (int i = 0; i < nodes.length; i++) {
            TIntArrayList list = new TIntArrayList();
            for (int j = 0; j < nodes.length; j++) {
                if (nodes[j].getHead() == nodes[i].getId())
                    list.add(nodes[j].getId());
            }
            list.sort();
            nodes[i].setChildren(list.toArray());
        }
    }

    public void setBunsetsuPASList() {
        this.bunsetsuPASList = new PredicateArgumentStructure[this.pASList.length];
        for (int i = 0; i < this.pASList.length; i++) {
            this.bunsetsuPASList[i] = (PredicateArgumentStructure) this.pASList[i].clone();
            int predId = this.bunsetsuPASList[i].getPredicateId();
	    
            for (int k = 0; k < this.bunsetsuList.length; k++) {
                if (this.bunsetsuList[k].contains(predId)) {
                    this.bunsetsuPASList[i].setPredicateId(this.bunsetsuList[k].getId());
                }
            }
	    
            int[] ids = pASList[i].getIds();
            TIntArrayList newIds = new TIntArrayList();
            for (int j = 0; j < ids.length; j++) {
                boolean flag = false;
                for (int k = 0; k < this.bunsetsuList.length; k++) {
                    if (this.bunsetsuList[k].contains(ids[j])) {
                        newIds.add(this.bunsetsuList[k].getId());
                        flag = true;
                    }
                }
                if (!flag) {
                    System.err.println("[DependencyTree] ERROR!");
                }
            }
            this.bunsetsuPASList[i].setIds(newIds.toArray());
        }
    }

    public void setBunsetsuPASList(PredicateArgumentStructure[] bunsetsuPASList) {
        this.bunsetsuPASList = bunsetsuPASList;
    }

    public void setBunsetsuChildren() {
        for (int i = 0; i < bunsetsuList.length; i++) {
            TIntArrayList list = new TIntArrayList();
            for (int j = 0; j < bunsetsuList.length; j++) {
                if (bunsetsuList[j].getHead() == bunsetsuList[i].getId())
                    list.add(bunsetsuList[j].getId());
            }
            list.sort();
            bunsetsuList[i].setChildren(list.toArray());
        }
    }

    public Bunsetsu getBunsetsuFromNodeId (int nodeId) {
        Bunsetsu[] bunsetsuList = this.getBunsetsuList();
        for (int i = 0; i < bunsetsuList.length; i++) {
            if (bunsetsuList[i].contains(nodeId)) {
                return bunsetsuList[i];
            }
        }
        return null;
    }

    public boolean hasRole (Bunsetsu b, String role) {
        DependencyNode[] bNodes = b.getNodes();
        for (int i = 0; i < pASList.length; i++) {
            int[] indices = pASList[i].getIds();
            String[] labels = pASList[i].getLabels();
            for (int j = 0; j < indices.length; j++) {
                if (b.contains(indices[j]) && role.equalsIgnoreCase(labels[j])) {
                    return true;
                }
            }
        }
        return false;
    }

    public Bunsetsu getBunsetsu (int bunsetsuId) {
        return this.bunsetsuList[bunsetsuId];
    }

    public int size() {
        return this.nodes.length;
    }

    public DependencyTree getSubTree(int id, int depth) {

        TIntArrayList list = new TIntArrayList();
        this.subTreeSearch(id, 0, depth, list);
        list.sort();
        int[] idList = list.toArray();
        ArrayList nodeList = new ArrayList();
        int nodeCnt = 0;
        for (int i = 0; i < idList.length; i++) {
            if (nodeList.contains(this.nodes[this.id2idx.get(idList[i])]) == false) {
                nodeList.add( this.nodes[this.id2idx.get(idList[i])] );
                nodeCnt++;
            }
        }
    
        ArrayList subTreePASList = new ArrayList();
        int pASCnt = 0;
        for (int i = 0; i < this.pASList.length; i++) {
            if (this.pASList[i].getPredicateId() == this.id2idx.get(id)) {
                subTreePASList.add(this.pASList[i]);
                pASCnt++;
            }
        }

        return new DependencyTree((DependencyNode[]) nodeList.toArray(new DependencyNode[nodeCnt]),
                                  (PredicateArgumentStructure[]) subTreePASList.toArray(new PredicateArgumentStructure[pASCnt]));
    }

    private void subTreeSearch (int currentId, int currentDepth, int depth, TIntArrayList idList) {
        idList.add(currentId);
        //System.out.println("currentId=" + currentId + ", currentDepth=" + currentDepth + ", depth=" + depth);
        int parent = nodes[this.id2idx.get(currentId)].getHead();
        if ( currentDepth < depth && parent != -1) {
            int nextDepth = currentDepth + 1;
            subTreeSearch(parent, nextDepth, depth, idList);
        }
        int[] children = nodes[this.id2idx.get(currentId)].getChildren();
        for (int i = 0; i < children.length; i++) {
            if ( currentDepth < depth ) {
                int nextDepth = currentDepth + 1;
                subTreeSearch(children[i], nextDepth, depth, idList);
            }
        }
    }

    public DependencyNode[] getPath (int startId, int targetId) {
        TIntArrayList path = new TIntArrayList();
        TIntHashSet passed = new TIntHashSet();
        boolean arrived = false;
        this.pathSearch(startId, targetId, path, passed);
        int[] pathElements = path.toArray();
        ArrayList nodeList = new ArrayList();
        for (int i = 0; i < pathElements.length; i++) {
            DependencyNode node = this.nodes[this.id2idx.get(pathElements[i])];
            nodeList.add(this.nodes[this.id2idx.get(pathElements[i])]);
        }
        return (DependencyNode[]) nodeList.toArray(new DependencyNode[pathElements.length]);
    }

    public String getPathStr (int startId, int targetId) {
        TIntHashSet passed = new TIntHashSet();
        ArrayList<String> pathArray = new ArrayList<String>();
        StringBuilder pathStr = new StringBuilder();
        this.pathSearchStr(startId, targetId, pathArray, passed, pathStr);
        return pathStr.toString();
    }

    private void pathSearchStr (int currentId, int targetId, ArrayList<String> pathArray,
                                TIntHashSet passed, StringBuilder pathStr) {
        passed.add(currentId);
        pathArray.add(String.valueOf(currentId));

        /*
        // for debugging 
        System.out.println("[pathSearchStr] currentId:" + currentId + ", targetId:" + targetId);
        System.out.print("[pathSearchStr] StrList:");
        String[] pathStrList = (String[]) pathArray.toArray(new String[pathArray.size()]);
        for (int i = 0; i < pathStrList.length; i++) {
	    System.out.print(pathStrList[i] + ",");
        }
        System.out.print("\n");
        */

        if (currentId == targetId) {
            String[] pathElements = pathArray.toArray(new String[pathArray.size()]);
            for (int i = 0; i < pathElements.length; i++) {
                pathStr.append(pathElements[i]);
                if (i != pathElements.length - 1) {
                    pathStr.append(",");
                }
            }
            return;
        } else {
            int parent = nodes[this.id2idx.get(currentId)].getHead();
            if (parent != 0 && !passed.contains(parent) && !passed.contains(targetId)) {
                pathArray.add(">");
                this.pathSearchStr(parent, targetId, pathArray, passed, pathStr);
                pathArray.remove(pathArray.size() - 1);
            }
            int[] children = nodes[this.id2idx.get(currentId)].getChildren();
            for (int i = 0; i < children.length; i++) {
                if (!passed.contains(children[i]) && !passed.contains(targetId)) {
                    pathArray.add("<");
                    this.pathSearchStr(children[i], targetId, pathArray, passed, pathStr);
                    pathArray.remove(pathArray.size() - 1);
                }
            }
        }
        pathArray.remove(pathArray.size() - 1);
    }

    public String getBunsetsuPathStr (int startId, int targetId) {
        TIntHashSet passed = new TIntHashSet();
        ArrayList<String> pathArray = new ArrayList<String>();
        StringBuilder pathStr = new StringBuilder();
        this.bunsetsuPathSearchStr(startId, targetId, pathArray, passed, pathStr);
        return pathStr.toString();
    }

    private void bunsetsuPathSearchStr (int currentId, int targetId, ArrayList<String> pathArray,
                                        TIntHashSet passed, StringBuilder pathStr) {
        passed.add(currentId);
        pathArray.add(String.valueOf(currentId));

        if (currentId == targetId) {
            String[] pathElements = pathArray.toArray(new String[pathArray.size()]);
            for (int i = 0; i < pathElements.length; i++) {
                pathStr.append(pathElements[i]);
                if (i != pathElements.length - 1) {
                    pathStr.append(",");
                }
            }
            return;
        } else {
            int parent = bunsetsuList[this.id2BunsetsuIdx.get(currentId)].getHead();
            if (parent != -1 && !passed.contains(parent) && !passed.contains(targetId)) {
                pathArray.add(">");
                this.bunsetsuPathSearchStr(parent, targetId, pathArray, passed, pathStr);
                pathArray.remove(pathArray.size() - 1);
            }
            int[] children = bunsetsuList[this.id2BunsetsuIdx.get(currentId)].getChildren();
            for (int i = 0; i < children.length; i++) {
                if (!passed.contains(children[i]) && !passed.contains(targetId)) {
                    pathArray.add("<");
                    this.bunsetsuPathSearchStr(children[i], targetId, pathArray, passed, pathStr);
                    pathArray.remove(pathArray.size() - 1);
                }
            }
        }
        pathArray.remove(pathArray.size() - 1);
    }


    private void pathSearch (int currentId, int targetId, TIntArrayList path, TIntHashSet passed) {
        path.add(currentId);
        passed.add(currentId);
        ////System.out.println(path.toString());
        if (currentId == targetId) {
            return;
        }
        int parent = nodes[this.id2idx.get(currentId)].getHead();
        if (parent != 0 && !passed.contains(parent) && !passed.contains(targetId)) {
            this.pathSearch(parent, targetId, path, passed);
            if (path.contains(targetId)) return;
            path.remove(path.size() - 1);
        }
        int[] children = nodes[this.id2idx.get(currentId)].getChildren();
        for (int i = 0; i < children.length; i++) {
            if (!passed.contains(children[i]) && !passed.contains(targetId)) {
                this.pathSearch(children[i], targetId, path, passed);
                if (path.contains(targetId)) return;
                path.remove(path.size() - 1);
            }
        }
    }

    public String getRel (int from, int to) {
        TIntHashSet passed = new TIntHashSet();
        int depth = 0;
        boolean arrived = false;
        String rel = new String();
        if (from == to) {
            rel = "SAME";
        } else {
            rel = this.relSearch(from, to, depth, passed);
        }
        return rel;
    }

    private String relSearch (int currentId, int targetId, int depth, TIntHashSet passed) {
        passed.add(currentId);
        String str = new String();
        if (currentId == targetId) {
            if (depth == 0) {
                return new String("SIBLING");
            } else if (depth == -1) {
                return new String("PARENT");
            } else if (depth == 1) {
                return new String("CHILD");
            } else if (depth == 2) {
                return new String("GRANDCHILD");
            } else {
                return new String("ELSE");
            }
        }
        int parent = nodes[this.id2idx.get(currentId)].getHead();
        if (parent != 0 && !passed.contains(parent) && !passed.contains(targetId)) {
            int newDepth = depth - 1;
            str = this.relSearch(parent, targetId, newDepth, passed);
        }
        int[] children = nodes[this.id2idx.get(currentId)].getChildren();
        for (int i = 0; i < children.length; i++) {
            if (!passed.contains(children[i]) && !passed.contains(targetId)) {
                int newDepth = depth + 1;
                str = relSearch(children[i], targetId, newDepth, passed);
            }
        }
        return str;
    }

    public int getDistance (int from, int to) {
        TIntHashSet passed = new TIntHashSet();
        int depth = 0;
        boolean arrived = false;
        int distance = 0;
        ////System.out.println("[getDistance] from=" + from + ", to=" + to);
        if (from != to) {
            distance = this.distanceSearch(from, to, distance, passed);
        }
        ////System.out.println("returned depth: " + distance);
        return distance;
    }

    private int distanceSearch (int currentId, int targetId, int depth, TIntHashSet passed) {
        passed.add(currentId);
        ////System.out.println("[distanceSearch] currentId=" + currentId + ", targetId=" + targetId + ", depth=" + depth + ", passed:" + passed.toString());
        if (currentId == targetId) {
            return depth;
        }
        int rDepth = -1;
        int returnedDepth = -1;
        int parent = nodes[this.id2idx.get(currentId)].getHead();
        if (parent != 0 && !passed.contains(parent) && !passed.contains(targetId)) {
            int newDepth = depth + 1;
            rDepth = this.distanceSearch(parent, targetId, newDepth, passed);
        }
        int[] children = nodes[this.id2idx.get(currentId)].getChildren();
        for (int i = 0; i < children.length; i++) {
            if (!passed.contains(children[i]) && !passed.contains(targetId)) {
                int newDepth = depth + 1;
                rDepth = this.distanceSearch(children[i], targetId, newDepth, passed);
            }
        }
        return rDepth;
    }

    public int[] getIds() {
        TIntArrayList idsAry = new TIntArrayList();
        for (int i = 0; i < this.nodes.length; i++) {
            idsAry.add(this.nodes[i].getId());
        }
        return idsAry.toArray();
    }

    public int[] getHeads() {
        TIntArrayList headsAry = new TIntArrayList();
        for (int i = 0; i < this.nodes.length; i++) {
            headsAry.add(this.nodes[i].getHead());
        }
        return headsAry.toArray();
    }
    
    public void setHeads(int[] heads) {
        if (heads.length != this.nodes.length) {
            System.out.println("num of heads != num of nodes.");
            System.exit(1);
        }
        for (int i = 0; i < this.nodes.length; i++) {
            this.nodes[i].setHead(heads[i]);
        }
        this.setChildren();
    }
  
    public DependencyNode[] getNodes() {
        return this.nodes;
    }

    public DependencyNode getNodeFromId(int id) {
        if (id == 0) { return ROOT_NODE; }
        if (this.id2idx.containsKey(id)) {
            return this.nodes[this.id2idx.get(id)];
        } else {
            return null;
        }
    }

    public Bunsetsu[] getBunsetsuList() {
        return this.bunsetsuList;
    }

    public Bunsetsu getBunsetsuFromId(int id) {
        return id == -1 ? ROOT_BUNSETSU : this.bunsetsuList[id];
    }

    public void setPASList(PredicateArgumentStructure[] pASList) {
        this.pASList = pASList;
        this.sortPASList();
    }

    public void setPASListNBest(ArrayList<PredicateArgumentStructure>[] pASListNBest) {
        this.pASListNBest = pASListNBest;
        this.sortPASList();
    }
  
    public void sortPASList() {
        if (this.pASList == null) { return; }
        for (int i = 0; i < this.pASList.length - 1; i++) {
            for (int j = 1; j < this.pASList.length - i; j++) {
                if (this.pASList[j-1].predicateId > this.pASList[j].predicateId) {
                    PredicateArgumentStructure pASTmp = this.pASList[j];
                    this.pASList[j] = this.pASList[j-1];
                    this.pASList[j-1] = pASTmp;
                }
            }
        }
        if (this.pASListNBest != null) {
            for (int i = 0; i < this.pASListNBest.length - 1; i++) {
                for (int j = 1; j < this.pASListNBest.length - i; j++) {
                    PredicateArgumentStructure iPAS = this.pASListNBest[j-1].get(0);
                    PredicateArgumentStructure jPAS = this.pASListNBest[j].get(0);
                    if (iPAS.predicateId > jPAS.predicateId) {
                        ArrayList<PredicateArgumentStructure> tmpAry = this.pASListNBest[j];
                        this.pASListNBest[j] = this.pASListNBest[j-1];
                        this.pASListNBest[j-1] = tmpAry;
                    }
                }
            }
        }
    }

    public int pasSize() {
        return this.pASList.length;
    }

    public boolean existsBunsetsuPAS(int predId) {
        for (int i = 0; i < this.bunsetsuPASList.length; i++) {
            if (this.bunsetsuPASList[i].getPredicateId() == predId) {
                return true;
            }
        }
        return false;
    }

    public PredicateArgumentStructure getPAS(int idx) {
        return this.pASList[idx];
    }

    public PredicateArgumentStructure getPASByPredicateId(int id) {
        for (int i = 0; i < this.pASList.length; i++) {
            if (this.pASList[i].getPredicateId() == id) {
                return this.pASList[i];
            }
        }
        return null;
    }
    /*
    public boolean doChildrenContainLemma (int idx, String lemma) { 
        DependencyNode curNode = getNodeFromId(idx);
        if (curNode.getLemma().equals(lemma)) {
            return true;
        }
        int[] children = curNode.getChildren();
        boolean val = false;
        for (int i = 0; i < children.length; i++) {
            boolean val = doChildrenContainLemma(children[i], lemma);
            if (val == true;) {
                val = true;
            }
        }
        return val;
    }
    */
    
    public ArrayList<Bunsetsu> getBunsetsuDescendants (int bunsetsuId) {
        ArrayList<Bunsetsu> descendants = new ArrayList<Bunsetsu>();
        Bunsetsu rootB = getBunsetsu(bunsetsuId);
        //System.err.println("rootB:"+rootB.toString());
        ArrayList<Bunsetsu> q = new ArrayList<Bunsetsu>();
        q.add(rootB);
        while (q.size() != 0) {
            Bunsetsu b = q.remove(0);
            //System.err.println("add:"+b.getId());
            descendants.add(b);
            int[] children = b.getChildren();
            //System.err.println("bunsetsu:"+b.getId());
            if (children == null) { continue; }
            for (int i = 0; i < children.length; i++) {
                Bunsetsu cb = getBunsetsu(children[i]);
                //System.err.println("bunsetsuchild:"+cb.getId());
                q.add(cb);
            }
        }
        return descendants;
    }

    public PredicateArgumentStructure getBunsetsuPASByPredicateId(int predId) {
        for (int i = 0; i < this.bunsetsuPASList.length; i++) {
            if (this.bunsetsuPASList[i].getPredicateId() == predId) {
                return this.bunsetsuPASList[i];
            }
        }
        return null;
    }

    public PredicateArgumentStructure[] getPASList() {
        return this.pASList;
    }

    public PredicateArgumentStructure[] getBunsetsuPASList() {
        return this.bunsetsuPASList;
    }

    public ArrayList<PredicateArgumentStructure>[] getPASListNBest() {
        return this.pASListNBest;
    }

    public PredicateArgumentStructure[] getPASListContainsNodeId (int idx) {
        ArrayList<PredicateArgumentStructure> list = new ArrayList<PredicateArgumentStructure>();
        for (int i = 0; i < this.pASList.length; i++) {
            if (this.pASList[i].getPredicateId() == idx) {
                list.add(this.pASList[i]);
            } else if (this.pASList[i].contains(idx)) {
                list.add(this.pASList[i]);
            }
        }
        return (PredicateArgumentStructure[]) list.toArray(new PredicateArgumentStructure[list.size()]);
    }

    public PredicateArgumentStructure[] getBunsetsuPASListContainsBunsetsuId (int idx) {
        ArrayList<PredicateArgumentStructure> list = new ArrayList<PredicateArgumentStructure>();
        for (int i = 0; i < this.bunsetsuPASList.length; i++) {
            if (this.bunsetsuPASList[i].contains(idx)) {
                list.add(this.bunsetsuPASList[i]);
            }
        }
        return (PredicateArgumentStructure[]) list.toArray(new PredicateArgumentStructure[list.size()]);
    }

    public PredicateArgumentStructure[] getPASListIncludesPredicateArgumentPair (int predId, int argId) {
        ArrayList<PredicateArgumentStructure> list = new ArrayList<PredicateArgumentStructure>();
        for (int i = 0; i < this.pASList.length; i++) {
            if (this.pASList[i].getPredicateId() == predId && this.pASList[i].contains(argId)) {
                list.add(this.pASList[i]);
            }
        }
        return (PredicateArgumentStructure[]) list.toArray(new PredicateArgumentStructure[list.size()]);
    }
    
    public void setModalityInfo(ArrayList<ModalityInfo> modalityInfoArray) {
        this.modList = (ModalityInfo[]) modalityInfoArray.toArray(new ModalityInfo[modalityInfoArray.size()]);
    }

    public ModalityInfo getModalityInfo(int predicateId) {
        for (int i = 0; i < this.modList.length; i++) {
            if (this.modList[i].getId() == predicateId) {
                return this.modList[i];
            }
        }
        return null;
    }

    public ModalityInfo[] getModalityInfoList () {
        return this.modList;
    }

    /*
      public DependencyAssignment getAssignment (Alphabet labelAlphabet) {
      DependencyAssignment depAsn = new DependencyAssignment();
      DependencyNode[] nodes = this.getNodes();
      for (int i = 0; i < nodes.length; i++) {
      if (labelAlphabet.contains(nodes[i].getDepRel())) {
      depAsn.add(nodes[i].getId(), nodes[i].getHead(), labelAlphabet.lookupIndex(nodes[i].getDepRel()), 0.0);
      } else {
      depAsn.add(nodes[i].getId(), nodes[i].getHead(), labelAlphabet.lookupIndex("OTHER"), 0.0);
      }
      }
      return depAsn;
      } 

      public void setAssignment (DependencyAssignment assignment, Alphabet labelAlphabet) {
      int[] dependents = assignment.getDependents();
      int[] heads = assignment.getHeads();
      int[] labels = assignment.getLabels();

      for (int i = 0; i < dependents.length; i++) {
      DependencyNode node = this.getNodeFromId(dependents[i]);
      node.setHead(heads[i]);
      node.setDepRel((String) labelAlphabet.lookupObject(labels[i]));
      }
      this.score = assignment.getScore();
      } 
    */

    public void setScore(double score) {
        this.score = score;
    }

    public double getScore() {
        return this.score;
    }

    public DependencyTree clone() {
        // copy nodes
        DependencyNode[] nodes = this.getNodes();
        DependencyNode[] cloneNodes = new DependencyNode[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            cloneNodes[i] = (DependencyNode) nodes[i].clone();
        }

        PredicateArgumentStructure[] pASList = this.getPASList();
        PredicateArgumentStructure[] clonePASList = new PredicateArgumentStructure[pASList.length];
        for (int i = 0; i < pASList.length; i++) {
            clonePASList[i] = (PredicateArgumentStructure) pASList[i].clone();
        }
	
        DependencyTree cloneTree = new DependencyTree(cloneNodes, clonePASList);
        cloneTree.setId(new String(id));
        cloneTree.setScore(score);

        return cloneTree;
    }

    public boolean equals (DependencyTree tree) {
        DependencyNode[] thisNodes = this.getNodes();
        DependencyNode[] treeNodes = tree.getNodes();

        if (thisNodes.length != treeNodes.length) { return false; }
        for (int i = 0; i < thisNodes.length; i++) {
            if (!thisNodes[i].equals(treeNodes[i])) { return false; }
        }
        return true;
    }

    public boolean isPredicate(int id) {
        if (this.pASList == null) { return false; }
        for (int i = 0; i < this.pASList.length; i++) {
            if (this.pASList[i].getPredicateId() == id) { 
                return true;
            }
        }
        return false;
    }

    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("[DependencyTree]\n");
        if (this.id != null) {
            str.append("ID="+this.id+"\n");
        }
        str.append("SCORE="+this.score+"\n");
        //for (int i = 0; i < this.nodes.length; i++) {
        //str.append(this.nodes[i].toString()+"\n");
        //}
        for (int i = 0; i < this.bunsetsuList.length; i++) {
            str.append(this.bunsetsuList[i].toString()+"\n");
        }
        if (this.pASList != null) {
            for (int i = 0; i < this.pASList.length; i++) {
                str.append(this.pASList[i].toString()+"\n");
            }
        }
        return str.toString();
    }

    public HashMap<String, String> getDebugInfo() {
        return this.debugInfoHash;
    }
    
    public void addDebugInfo (String label, String debugInfo) {
        this.debugInfoHash.put(label, debugInfo);
    }

    public void setDebugInfo (HashMap<String, String> debugInfoHash) {
        this.debugInfoHash = debugInfoHash;
    }
}
