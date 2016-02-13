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

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import jp.ac.tohoku.ecei.cl.www.base.*;
import jp.ac.tohoku.ecei.cl.www.util.*;
import jp.ac.tohoku.ecei.cl.www.io.*;

import gnu.trove.iterator.*;
import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.hash.*;

public class DepTypeAnnotator {

    public static boolean isNewFormat = false;
    public enum DepType {DEP, ZERO_INTRA, ZERO_INTER, SAME_PHRASE}
    public enum ArgPositionType {HEAD, HEAD_LEFT, HEAD_RIGHT, OTHER};

    public static DepType getDepType (DependencyTree tree, int pIdx, int aIdx) {
        Bunsetsu pBun = tree.getBunsetsuFromNodeId(pIdx);
        Bunsetsu aBun = tree.getBunsetsuFromNodeId(aIdx);
        Bunsetsu pBunHeadBun = tree.getBunsetsuFromId(pBun.getHead());
        Bunsetsu aBunHeadBun = tree.getBunsetsuFromId(aBun.getHead());
        if (pBun.getId() == aBun.getId()) { 
            return DepType.SAME_PHRASE;
        } else if (pBun.getId() == aBunHeadBun.getId()) {
            return DepType.DEP;
        } else if (pBunHeadBun.getId() == aBunHeadBun.getId()) {
            return DepType.DEP;
        } else {
            return DepType.ZERO_INTRA;
        }
    }

    public static ArgPositionType getArgPositionType (DependencyTree tree, int aIdx) {
        Bunsetsu aBun = tree.getBunsetsuFromNodeId(aIdx);
        DependencyNode aBunHeadNode = aBun.getHeadNode();
        if (aIdx == aBunHeadNode.getId()) {
            return ArgPositionType.HEAD;
        } else if (aIdx == aBunHeadNode.getId()-1) {
            return ArgPositionType.HEAD_LEFT;
        } else if (aIdx == aBunHeadNode.getId()+1) {
            return ArgPositionType.HEAD_RIGHT;
        } else {
            return ArgPositionType.OTHER;
        }
    }

    public static void main(String[] args) {
        //String file = args[0];

        IntInt2IntHashMap lEr = new IntInt2IntHashMap();
        
        HashMap<String, TObjectIntHashMap<String>> stat = new HashMap<String, TObjectIntHashMap<String>>();
        
        try {
            HashSet<String> headPat = PatternFileParser.parse(new File("ipa_head_pat.txt"));
            HashSet<String> funcPat = PatternFileParser.parse(new File("ipa_func_pat.txt"));

            CaboCha2Dep pipe = new CaboCha2Dep(System.in);
            JapaneseDependencyTree2CaboCha caboChaOutPipe = new JapaneseDependencyTree2CaboCha();
            caboChaOutPipe.setFormat(CaboChaFormat.OLD);

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out, "utf-8"));
            while (!pipe.eof()) {
                DependencyTree tree = pipe.pipePerSentence();
                if (tree == null) { continue; }
                JapaneseDependencyTreeLib.setBunsetsuHead(tree, funcPat, headPat);
                PredicateArgumentStructure[] pasList = tree.getPASList();

                for (int j = 0; j < pasList.length; j++) {
                    int predId = pasList[j].getPredicateId();
                    String predType = pasList[j].predicateType;
                    int[] aIds = pasList[j].getIds();
                    String[] aLabels = pasList[j].getLabels();
                    for (int k = 0; k < aIds.length; k++) {
                        DepType dt = getDepType(tree, predId, aIds[k]);
                        ArgPositionType apt = getArgPositionType(tree, aIds[k]);
                        if (!stat.containsKey(aLabels[k])) {
                            stat.put(aLabels[k], new TObjectIntHashMap<String>());
                        }
                        TObjectIntHashMap<String> inner = stat.get(aLabels[k]);
                        if (!inner.containsKey(dt.toString()+":"+apt.toString())) {
                            inner.put(dt.toString()+":"+apt.toString(), 0);
                        }
                        inner.increment(dt.toString()+":"+apt.toString());
                        aLabels[k] += ":"+dt+":"+apt;
                    }
                }
                StringBuilder resultStr = new StringBuilder();
                resultStr.append(caboChaOutPipe.pipePerSentence(tree));
                writer.write(resultStr.toString());
            }
            // print statistics
            for (Iterator it = stat.keySet().iterator(); it.hasNext();) {
                String key = (String) it.next();
                TObjectIntHashMap inner = stat.get(key);
                for (TObjectIntIterator iit = inner.iterator(); iit.hasNext();) {
                    iit.advance();
                    System.err.print(key+"\t"+iit.key()+"\t"+iit.value()+"\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
