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

import java.util.ArrayList;

import jp.ac.tohoku.ecei.cl.www.base.*;
import jp.ac.tohoku.ecei.cl.www.util.*;

public class PredicateFeatureVectorPipe {
    
    public FeatureVector[] pipe(AlphabetTrie alphabet, DependencyTree tree, boolean alphabetGrowth) {
        return this.pipe(alphabet, tree, alphabetGrowth, null);
    }

    public FeatureVector[] pipe(AlphabetTrie alphabet, DependencyTree tree, boolean alphabetGrowth, FeatureSet fs) {

        FeatureVector[] fvs = new FeatureVector[tree.size()];
        for (int i = 0; i < fvs.length; i++) {
            fvs[i] = getFeatureVector(alphabet, tree, i+1, alphabetGrowth, fs);
        }
        return fvs;
    }

    public FeatureVector[] pipe(AlphabetTrie alphabet, DependencyTree tree, int[] indices, 
                                boolean alphabetGrowth, FeatureSet fs) {

        FeatureVector[] fvs = new FeatureVector[indices.length];
        for (int i = 0; i < indices.length; i++) {
            fvs[i] = getFeatureVector(alphabet, tree, indices[i], alphabetGrowth, fs);
        }
        return fvs;
    }

    public FeatureVector getFeatureVector(AlphabetTrie alphabet, DependencyTree tree, int nodeId,
                                          boolean alphabetGrowth ){
        return getFeatureVector(alphabet, tree, nodeId, alphabetGrowth, false, null);
    }

    public FeatureVector getFeatureVector(AlphabetTrie alphabet, DependencyTree tree, int nodeId,
                                          boolean alphabetGrowth, FeatureSet fs){
        return getFeatureVector(alphabet, tree, nodeId, alphabetGrowth, fs==null?false:true, fs);
    }

    private FeatureVector getFeatureVector(AlphabetTrie alphabet, DependencyTree tree, int nodeId, 
                                           boolean alphabetGrowth, boolean useFS, FeatureSet fs) {
        ArrayList<String> featsAry = new ArrayList<String>();
        int treeSize = tree.size();
        int totalLength = 0;
	
        DependencyNode node = tree.getNodeFromId(nodeId);

        featsAry.add("PRD:P:" + node.getPOS());

        DependencyNode headNode = tree.getNodeFromId(node.getHead());

        if (headNode!=null) {
            featsAry.add("PRD:H:P:" + headNode.getPOS());
            featsAry.add("PRD:DH:PP:" + node.getPOS()+headNode.getPOS());
        }

        int[] children = node.getChildren();

        StringBuilder depSubCat = new StringBuilder();
        depSubCat.append("PRD:DSC:");

        for (int i = 0; i < children.length; i++) {
            DependencyNode child = tree.getNodeFromId(children[i]);
            featsAry.add("PRD:C:P:" + child.getPOS());
            featsAry.add("PRD:DC:PP:" + node.getPOS()+child.getPOS());
        }

        String[] feats = featsAry.toArray(new String[featsAry.size()]);
        return new FeatureVector(alphabet, feats, alphabetGrowth);
    }

}
