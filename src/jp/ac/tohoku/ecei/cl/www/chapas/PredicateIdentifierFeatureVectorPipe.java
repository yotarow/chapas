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
import java.io.Serializable;

import jp.ac.tohoku.ecei.cl.www.base.*;
import jp.ac.tohoku.ecei.cl.www.util.*;

public class PredicateIdentifierFeatureVectorPipe implements FeatureVectorPipe {
    
    public PredicateIdentifierFeatureVectorPipe(){}

    public FeatureVector[] pipe(AlphabetTrie alphabet, DependencyTree tree, boolean alphabetGrowth, FrameDictionary frames) {
        return this.pipe(alphabet, tree, alphabetGrowth, frames, null);
    }

    public FeatureVector[] pipe(AlphabetTrie alphabet, DependencyTree tree, boolean alphabetGrowth, FrameDictionary frames, FeatureSet fs) {

        FeatureVector[] fvs = new FeatureVector[tree.size()];
        for (int i = 0; i < fvs.length; i++) {
            fvs[i] = getFeatureVector(alphabet, tree, i+1, frames, alphabetGrowth, fs);
        }
        return fvs;
    }

    public FeatureVector[] pipe(AlphabetTrie alphabet, DependencyTree tree, int[] indices, 
                                boolean alphabetGrowth, FrameDictionary frames, FeatureSet fs) {

        FeatureVector[] fvs = new FeatureVector[indices.length];
        for (int i = 0; i < indices.length; i++) {
            fvs[i] = getFeatureVector(alphabet, tree, indices[i], frames, alphabetGrowth, fs);
        }
        return fvs;
    }

    public FeatureVector getFeatureVector(AlphabetTrie alphabet, DependencyTree tree, int nodeId,
                                          FrameDictionary frames, boolean alphabetGrowth ){
        return getFeatureVector(alphabet, tree, nodeId, frames, alphabetGrowth, false, null);
    }

    public FeatureVector getFeatureVector(AlphabetTrie alphabet, DependencyTree tree, int nodeId,
                                          FrameDictionary frames, boolean alphabetGrowth, FeatureSet fs){
        return getFeatureVector(alphabet, tree, nodeId, frames, alphabetGrowth, fs==null?false:true, fs);
    }

    private FeatureVector getFeatureVector(AlphabetTrie alphabet, DependencyTree tree, int nodeId, 
                                           FrameDictionary frames, boolean alphabetGrowth, boolean useFS, FeatureSet fs) {

        ArrayList<String> featsAry = new ArrayList<String>();
        int treeSize = tree.size();
        int totalLength = 0;
	
        DependencyNode node = tree.getNodeFromId(nodeId);

        String pos = node.getPOS();
	
        if (!useFS || fs.contains(0)) {
            featsAry.add("PRD:L:" + node.getLemma());
        }
	
        if (!useFS || fs.contains(1)) {
            featsAry.add("PRD:P:" + node.getPOS());
        }
	
        if (!useFS || fs.contains(2)) {
            featsAry.add("PRD:LP:" + node.getLemma()+node.getPOS()); // this degraded performance
        }

        DependencyNode headNode = tree.getNodeFromId(node.getHead());

        if (headNode!=null) {

            if (!useFS || fs.contains(3)) {
                featsAry.add("PRD:H:L:" + headNode.getLemma());
            }
            if (!useFS || fs.contains(4)) {
                featsAry.add("PRD:H:P:" + headNode.getPOS());
            }
	    
            if (!useFS || fs.contains(5)) {
                // head + pred cand conj
                featsAry.add("PRD:DH:PP:" + node.getPOS()+headNode.getPOS());
            }
            if (!useFS || fs.contains(6)) {
                featsAry.add("PRD:DH:PL:" + node.getPOS()+headNode.getLemma());
            }
            if (!useFS || fs.contains(7)) {
                featsAry.add("PRD:DH:LP:" + node.getLemma()+headNode.getPOS());
            }
            if (!useFS || fs.contains(8)) {
                featsAry.add("PRD:DH:PDRP:" + node.getPOS()+node.getDepRel()+headNode.getPOS());
            }
        }

        // pred rel to parent
        if (!useFS || fs.contains(9)) {
            featsAry.add("PRD:DR:" + node.getDepRel());
        }

        if (frames != null && frames.contains(node.getLemma())) {
            if (!useFS || fs.contains(10)) {
                featsAry.add("LEMMAINDIC");
            }
            if (!useFS || fs.contains(11)) {
                featsAry.add("LEMMAINDIC:"+node.getLemma());
            }
        } else {
            if (!useFS || fs.contains(12)) {
                featsAry.add("LEMMANOTINDIC");
            }
            if (!useFS || fs.contains(13)) {
                featsAry.add("LEMMANOTINDIC"+node.getPOS());
            }
            if (!useFS || fs.contains(14)) {
                featsAry.add("LEMMANOTINDIC"+node.getLemma());
            }
            if (!useFS || fs.contains(1)) {
                featsAry.add("LEMMANOTINDIC"+node.getPOS()+node.getLemma());
            }
        }

        int[] children = node.getChildren();

        StringBuilder depSubCat = new StringBuilder();
        depSubCat.append("PRD:DSC:");

        for (int i = 0; i < children.length; i++) {
            DependencyNode child = tree.getNodeFromId(children[i]);
            if (!useFS || fs.contains(10)) {
                featsAry.add("PRD:C:DR:" + child.getDepRel());
            }
            if (!useFS || fs.contains(11)) {
                featsAry.add("PRD:C:L:" + child.getLemma());
            }
            if (!useFS || fs.contains(12)) {
                featsAry.add("PRD:C:P:" + child.getPOS());
            }
            if (!useFS || fs.contains(13)) {
                featsAry.add("PRD:C:LDR:" +  child.getLemma()+child.getDepRel());
            }
            if (!useFS || fs.contains(14)) {
                featsAry.add("PRD:C:PDR:" +  child.getPOS()+child.getDepRel());
            }
            if (!useFS || fs.contains(15)) {
                featsAry.add("PRD:DC:PP:" + node.getPOS()+child.getDepRel()+child.getPOS());
            }
            if (!useFS || fs.contains(16)) {
                featsAry.add("PRD:DC:PP:" + node.getPOS()+child.getPOS());
            }
            if (!useFS || fs.contains(17)) {
                featsAry.add("PRD:DC:PL:" + node.getPOS()+child.getLemma());
            }
            if (!useFS || fs.contains(18)) {
                featsAry.add("PRD:DC:LP:" + node.getLemma()+child.getPOS());
            }
            depSubCat.append("-" + child.getDepRel());
        }
        if (!useFS || fs.contains(19)) {
            featsAry.add(depSubCat.toString());
        }

        String[] nodeFeats = node.getFeats();

        // node.feats
        if (!useFS || fs.contains(20)) {
            if (nodeFeats != null) {
                for (int i = 0; i < nodeFeats.length; i++) {
                    featsAry.add("FEATS:"+nodeFeats[i]);
                }
            }
        }

        String[] feats = featsAry.toArray(new String[featsAry.size()]);

        return new FeatureVector(alphabet, feats, alphabetGrowth);
    }

    public int numOfFeatures() {
        return 21;
    }

    public FeatureVector getFeatureVector(AlphabetTrie alphabet, DependencyTree tree, int nodeId, 
                                          FrameDictionary frames, boolean alphabetGrowth, boolean useFS, FeatureSet fs, String factor) {

        ArrayList<String> featsAry = new ArrayList<String>();
        int treeSize = tree.size();
        int totalLength = 0;
	
        DependencyNode node = tree.getNodeFromId(nodeId);
	
        String pos = node.getPOS();
	
        if (!useFS || fs.contains(0)) {
            featsAry.add(factor+"PRD:L:" + node.getLemma());
        }
	
        if (!useFS || fs.contains(1)) {
            featsAry.add(factor+"PRD:P:" + node.getPOS());
        }
	
        if (!useFS || fs.contains(2)) {
            featsAry.add(factor+"PRD:LP:" + node.getLemma()+node.getPOS()); // this degraded performance
        }

        DependencyNode headNode = tree.getNodeFromId(node.getHead());

        if (!useFS || fs.contains(3)) {
            featsAry.add(factor+"PRD:H:L:" + headNode.getLemma());
        }
        if (!useFS || fs.contains(4)) {
            featsAry.add(factor+"PRD:H:P:" + headNode.getPOS());
        }

        if (!useFS || fs.contains(5)) {
            // head + pred cand conj
            featsAry.add(factor+"PRD:DH:PP:" + node.getPOS()+headNode.getPOS());
        }
        if (!useFS || fs.contains(6)) {
            featsAry.add(factor+"PRD:DH:PL:" + node.getPOS()+headNode.getLemma());
        }
        if (!useFS || fs.contains(7)) {
            featsAry.add(factor+"PRD:DH:LP:" + node.getLemma()+headNode.getPOS());
        }
        if (!useFS || fs.contains(8)) {
            featsAry.add(factor+"PRD:DH:PDRP:" + node.getPOS()+node.getDepRel()+headNode.getPOS());
        }

        // pred rel to parent
        if (!useFS || fs.contains(9)) {
            featsAry.add(factor+"PRD:DR:" + node.getDepRel());
        }

        if (frames != null && frames.contains(node.getLemma())) {
            if (!useFS || fs.contains(10)) {
                featsAry.add("LEMMAINDIC");
            }
            if (!useFS || fs.contains(11)) {
                featsAry.add("LEMMAINDIC:"+node.getLemma());
            }
        } else {
            if (!useFS || fs.contains(12)) {
                featsAry.add("LEMMANOTINDIC");
            }
            if (!useFS || fs.contains(13)) {
                featsAry.add("LEMMANOTINDIC"+node.getPOS());
            }
            if (!useFS || fs.contains(14)) {
                featsAry.add("LEMMANOTINDIC"+node.getLemma());
            }
            if (!useFS || fs.contains(1)) {
                featsAry.add("LEMMANOTINDIC"+node.getPOS()+node.getLemma());
            }
        }


        int[] children = node.getChildren();

        StringBuilder depSubCat = new StringBuilder();
        depSubCat.append("PRD:DSC:");

        for (int i = 0; i < children.length; i++) {
            DependencyNode child = tree.getNodeFromId(children[i]);
            if (!useFS || fs.contains(10)) {
                featsAry.add(factor+"PRD:C:DR:" + child.getDepRel());
            }
            if (!useFS || fs.contains(11)) {
                featsAry.add(factor+"PRD:C:L:" + child.getLemma());
            }
            if (!useFS || fs.contains(12)) {
                featsAry.add(factor+"PRD:C:P:" + child.getPOS());
            }
            if (!useFS || fs.contains(13)) {
                featsAry.add(factor+"PRD:C:LDR:" +  child.getLemma()+child.getDepRel());
            }
            if (!useFS || fs.contains(14)) {
                featsAry.add(factor+"PRD:C:PDR:" +  child.getPOS()+child.getDepRel());
            }
            if (!useFS || fs.contains(15)) {
                featsAry.add(factor+"PRD:DC:PP:" + node.getPOS()+child.getDepRel()+child.getPOS());
            }
            if (!useFS || fs.contains(16)) {
                featsAry.add(factor+"PRD:DC:PP:" + node.getPOS()+child.getPOS());
            }
            if (!useFS || fs.contains(17)) {
                featsAry.add(factor+"PRD:DC:PL:" + node.getPOS()+child.getLemma());
            }
            if (!useFS || fs.contains(18)) {
                featsAry.add(factor+"PRD:DC:LP:" + node.getLemma()+child.getPOS());
            }
            depSubCat.append("-" + child.getDepRel());
        }
        if (!useFS || fs.contains(19)) {
            featsAry.add(factor+depSubCat.toString());
        }

        String[] nodeFeats = node.getFeats();

        // node.feats
        if (!useFS || fs.contains(20)) {
            if (nodeFeats != null) {
                for (int i = 0; i < nodeFeats.length; i++) {
                    featsAry.add(factor+"FEATS:"+nodeFeats[i]);
                }
            }
        }

        String[] feats = featsAry.toArray(new String[featsAry.size()]);

        return new FeatureVector(alphabet, feats, alphabetGrowth);
    }
}
