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

package jp.ac.tohoku.ecei.cl.www.coord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.io.*;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import jp.ac.tohoku.ecei.cl.www.base.*;
import jp.ac.tohoku.ecei.cl.www.util.*;
import jp.ac.tohoku.ecei.cl.www.kucf.*;
import jp.ac.tohoku.ecei.cl.www.db.*;

public class CoordinationAnalyzerFeatureVectorPipe {

    public static final String TOUTEN1 = "\u3001"; // utf-16
    public static final String TOUTEN2 = "\uFF0C"; // utf-16

    private AlphabetTrie alphabet;

    private HashSet<String> funcPat;
    private HashSet<String> headPat;
    private HashSet<String> particlePat;

    public CoordinationAnalyzerFeatureVectorPipe (AlphabetTrie alphabet,
                                                  HashSet<String> funcPat, 
                                                  HashSet<String> particlePat) {
        this.alphabet = alphabet;
        this.funcPat = funcPat;
        this.particlePat = particlePat;
    }

    public FeatureVector[] pipe(DependencyTree tree, boolean alphabetGrowth) {
        Bunsetsu[] bs = tree.getBunsetsuList();
        FeatureVector[] fvs = new FeatureVector[bs.length];
        for (int i = 0; i < bs.length; i++) {
            fvs[i] = getFeatureVector(tree, bs[i], alphabetGrowth);
        }
        return fvs;
    }

    public FeatureVector getFeatureVector(DependencyTree tree, Bunsetsu b, boolean alphabetGrowth) {
        ArrayList<String> featsAry = getFeatures(tree, b);
        FeatureVector fv = new FeatureVector(this.alphabet, featsAry, alphabetGrowth);
        return fv;
    }
    
    public ArrayList<String> getFeatures(DependencyTree tree, Bunsetsu b) {
        ArrayList<String> featsAry = new ArrayList<String>();

        DependencyNode headNode = b.getHeadNode();
        String headNodeLemma = headNode.getLemma();
        String headNodePOS = headNode.getPOS();
        String depLabel = b.getDepLabel();


        Bunsetsu parentB = tree.getBunsetsuFromId(b.getHead());
        DependencyNode parentHeadNode = parentB.getHeadNode();
        String parentHeadNodeLemma = parentHeadNode.getLemma();
        String parentHeadNodePOS = parentHeadNode.getPOS();
        
        ArrayList<DependencyNode> particles = JapaneseDependencyTreeLib.getParticles(b, this.particlePat);
        StringBuilder particleSeq = new StringBuilder();
        for (DependencyNode node : particles) {
            particleSeq.append(node.getLemma());
        }

        featsAry.add("CP:P-PAR-P:"+headNodePOS+","+particleSeq.toString()+","+parentHeadNodePOS);

        DependencyNode[] bNodes = b.getNodes();
        DependencyNode lastNode = bNodes[bNodes.length-1];
        String touten = "0";
        if (lastNode.getLemma().equals(TOUTEN1) || lastNode.getLemma().equals(TOUTEN2)) {
            touten = "1";
        }
        //featsAry.add("CP:L-COM-L:"+headNodeLemma+","+touten+","+parentHeadNodeLemma);
        featsAry.add("CP:L-COM-P:"+headNodeLemma+","+touten+","+parentHeadNodePOS);
        featsAry.add("CP:P-COM-L:"+headNodePOS+","+touten+","+parentHeadNodeLemma);
        featsAry.add("CP:P-COM-P:"+headNodePOS+","+touten+","+parentHeadNodePOS);
        featsAry.add("CP:P-PAR-COM-P:"+headNodePOS+","+particleSeq.toString()+","+touten+","+parentHeadNodePOS);
        
        return featsAry;
    }
}
