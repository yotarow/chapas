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
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import jp.ac.tohoku.ecei.cl.www.base.*;
import jp.ac.tohoku.ecei.cl.www.util.*;
import jp.ac.tohoku.ecei.cl.www.kucf.*;
import jp.ac.tohoku.ecei.cl.www.db.*;

import gnu.trove.iterator.*;
import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.hash.*;

public class SRLFeatureVectorPipe0317 implements SRLFeatureVectorPipe, FeatureVectorPipe {
    
    public static enum DepType {DEP, ZERO_INTRA, ZERO_INTER, SAME_PHRASE}

    public static final String FORM_SURU_STR = "\u3059\u308B"; // utf-16

    public static final boolean PMI_NORMALIZED = true;
    public static final boolean COORD_FEAT = true;

    public static final String POS_NOUN_SAHEN_STR = "\u540D\u8A5E-\u30B5\u5909\u63A5\u7D9A"; // utf-16
    public static final String[] FORM_CASES_STR = {"\u304c", "\u3092", "\u306B"}; // ga or wo or ni

    public static enum Voice { Active, Passive, Causative, Nominal };
    private static float COOC_THRESHOLD = 1.0e-5F;
    private static int KUCF_FREQ_THRESHOLD = 1;
    private static double CASE_EXISTS_THRESHOLD = 0.01;

    private static Pattern gchild = Pattern.compile("^\\d+,<,\\d+?,<,\\d+?$");
    private static Pattern gparent = Pattern.compile("^\\d+?,>,\\d+?,>,\\d+?$");
    private static Pattern sibling = Pattern.compile("^\\d+?,>,\\d+?,<,\\d+?$");
    private static Pattern child = Pattern.compile("^\\d+?,<,\\d+?$");
    private static Pattern parent = Pattern.compile("^\\d+?,>,\\d+?$");

    private AlphabetTrie alphabet;
    private int numThreads;
    private boolean useThread;
    private boolean useFS;
    private FeatureSet fs;
    private HashSet<String> particlePat;
    private HashMap<String, String> passivePat;
    private HashMap<String, String> causativePat;
    private HashMap<String, String> sahenPat;
    private PLSICooccurenceInfoManager coocMan;
    private KUCFManager kucfMan;
    private KUCFManager[] kucf2KMan;
    private KUCFManager[] kucf5HMan;
    private ArrayList<String> cases;

    private KeyValueDBManager sw2000Man;
    private KeyValueDBManager sw500Man;
    private PredicatePattern kucfPredPat;
    //private ChaPASOptions options;
    public SRLFeatureVectorPipe0317 (ArrayList<String> cases, FeatureSet fs, HashMap<String, String> passivePat, 
                                     HashMap<String, String> causativePat, HashMap<String, String> sahenPat, 
                                     HashSet<String> particlePat, PLSICooccurenceInfoManager coocMan, 
                                     KUCFManager kucfMan, KUCFManager[] kucf2KMan, KUCFManager[] kucf5HMan,
                                     PredicatePattern kucfPredPat, KeyValueDBManager sw2000Man, 
                                     KeyValueDBManager sw500Man) {
        this.cases = cases;
        this.useFS = fs == null ? false : true;
        this.fs = fs;
        this.passivePat = passivePat;
        this.causativePat = causativePat;
        this.sahenPat = sahenPat;
        this.particlePat = particlePat;
        this.coocMan = coocMan;
        this.kucfMan = kucfMan;
        this.kucf2KMan = kucf2KMan;
        this.kucf5HMan = kucf5HMan;
        this.kucfPredPat = kucfPredPat;
        this.sw2000Man = sw2000Man;
        this.sw500Man = sw500Man;
    }


    public DepType getDepType (DependencyTree tree, int pIdx, int aIdx) {
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

    public FeatureVector[] pipe(AlphabetTrie alphabet, DependencyTree tree, int predId, int[] argIds, 
                                boolean alphabetGrowth, String predType) throws IOException {
        FeatureVector[] fvs = new FeatureVector[argIds.length];
        for (int j = 0; j < argIds.length; j++) {
            fvs[j] = getFeatureVector(alphabet, tree, predId, argIds[j], alphabetGrowth, predType);
        }
        return fvs;
    }

    public void getTokenFeatures(FeatureVector fv, DependencyNode node, String type, String add) {
        fv.add(type+":L:"+node.getLemma()+":"+add);
        fv.add(type+":P:"+node.getPOS()+":"+add);
        fv.add(type+":LP:"+node.getLemma()+node.getPOS()+":"+add);
        if (node.ne != null && !node.ne.equals("O")) {
            String neTag = node.ne.substring(2,node.ne.length());
            fv.add(type+":LNE:"+node.getLemma()+neTag+",voice:"+add);
        }
        // sw class
        if (sw2000Man != null) {
            String class2000Val = sw2000Man.getValue(node.getLemma());
            if (class2000Val != null) {
                fv.add(type+":C2K:"+class2000Val+":"+add);
                //System.err.println(type+":C2K:"+class2000Val+":"+add);
            } else {
                fv.add(type+":C2K:NULL:"+add);
            }
        }

        if (sw500Man != null) {
            String class500Val = sw500Man.getValue(node.getLemma());
            if (class500Val != null) {
                fv.add(type+":C5H:"+class500Val+":"+add);
            } else {
                fv.add(type+":C5H:NULL:"+add);
            }
        }

    }

    public void getTokenPairFeatures(FeatureVector fv, DependencyNode predNode, DependencyNode argNode, 
                                     String type, String add) {
        fv.add(type+":LL:"+predNode.getLemma()+":"+argNode.getLemma()+":"+add);
        fv.add(type+":PP:"+predNode.getPOS()+":"+argNode.getPOS()+":"+add);
        if (argNode.ne != null && !argNode.ne.equals("O")) {
            String neTag = argNode.ne.substring(2,argNode.ne.length());
            fv.add(type+":LNE:"+predNode.getLemma()+neTag+":"+add);
        }

        // sw class
        if (sw2000Man != null) {
            String class2000Val = sw2000Man.getValue(argNode.getLemma());
            if (class2000Val != null) {
                fv.add(type+":PC2K:"+predNode.getLemma()+":"+class2000Val+":"+add);
            } else {
                fv.add(type+":PC2K:"+predNode.getLemma()+":NULL:"+add);
            }
        }

        if (sw500Man != null) {
            String class500Val = sw500Man.getValue(argNode.getLemma());
            if (class500Val != null) {
                fv.add(type+":PC5H:"+predNode.getLemma()+":"+class500Val+":"+add);
            } else {
                fv.add(type+":PC5H:"+predNode.getLemma()+":NULL:"+add);
            }
        }

    }
    
    public FeatureVector getFeatureVector(AlphabetTrie alphabet, DependencyTree tree, int predId, int argId, boolean alphabetGrowth, String predType) throws IOException {
        FeatureVector fv = new FeatureVector(alphabet, alphabetGrowth);

        ArrayList<String> featsAry = new ArrayList<String>();
        int treeSize = tree.size();
        int totalLength = 0;

        DependencyNode predNode = tree.getNodeFromId(predId);
        DependencyNode prevPredNode = tree.getNodeFromId(predId-1);

        DependencyNode argNode = tree.getNodeFromId(argId);
        DependencyNode right1ArgNode = null;
        DependencyNode right2ArgNode = null;
        DependencyNode right3ArgNode = null;
        if (argId+1 < tree.size()) {
            right1ArgNode = tree.getNodeFromId(argId+1);
        }
        if (argId+2 < tree.size()) {
            right2ArgNode = tree.getNodeFromId(argId+2);
        }
        if (argId+3 < tree.size()) {
            right3ArgNode = tree.getNodeFromId(argId+3);
        }
        DependencyNode left1ArgNode = null;
        DependencyNode left2ArgNode = null;
        DependencyNode left3ArgNode = null;
        if (argId-1 >= 0) {
            left1ArgNode = tree.getNodeFromId(argId-1);
        }
        if (argId-2 >= 0) {
            left2ArgNode = tree.getNodeFromId(argId-2);
        }
        if (argId-2 >= 0) {
            left2ArgNode = tree.getNodeFromId(argId-3);
        }

        DependencyNode predsHeadNode = tree.getNodeFromId(predNode.getHead());
        Bunsetsu predBunsetsu = tree.getBunsetsuFromNodeId(predId);
        Bunsetsu argBunsetsu = tree.getBunsetsuFromNodeId(argId);
        String argParticleStr = JapaneseDependencyTreeLib.getParticleSequence(argBunsetsu, this.particlePat);
        
        Voice voice = getVoice (tree, predId);

        String voiceStr = "";
        if (voice == Voice.Passive) {
            voiceStr = "VoP";
        } else if (voice == Voice.Causative) {
            voiceStr = "VoC";
        } else if (voice == Voice.Active) {
            voiceStr = "VoA";
        } else if (voice == Voice.Nominal) {
            voiceStr = "VoN";
        }

        // add position feature
        String position;
        if (predNode.getId() < argNode.getId()) {
            position = "POS:<";
        } else if (predNode.getId() > argNode.getId()) {
            position = "POS:>";
        } else {
            position = "POS:=";
        }

        String ptcl = JapaneseDependencyTreeLib.getParticleSequence(argBunsetsu, this.particlePat);
        fv.add("PTCL:"+ptcl);
        fv.add("PTCL-VOICE:"+ptcl+voice);
        fv.add("PTCL-VOICE-POSITION:"+ptcl+voice+position);

        DepType depType = getDepType(tree, predNode.getId(), argNode.getId());

        String predLemma = null;
        if (this.sahenPat.containsKey(predNode.getLemma()) &&
            this.sahenPat.get(predNode.getLemma()).equals(prevPredNode.getPOS())) {
            predLemma = prevPredNode.getLemma();
        } else {
            predLemma = predNode.getLemma();
        }
        
        if (predNode != null) {
            getTokenFeatures(fv, predNode, "P", voiceStr);
            getTokenFeatures(fv, predNode, "P", position);
            //getTokenFeatures(fv, predNode, "P", depType+"");
            //getTokenFeatures(fv, predNode, "P", ptcl);
        } 
        if (argNode != null) {
            getTokenFeatures(fv, argNode, "A", voiceStr);
            getTokenFeatures(fv, argNode, "A", position);
            getTokenFeatures(fv, argNode, "A", depType+"");
            //getTokenFeatures(fv, argNode, "A", ptcl);
        }
        getTokenPairFeatures(fv, predNode, argNode, "PA", voiceStr);
        //getTokenPairFeatures(fv, predNode, argNode, "PA", depType+"");
        //getTokenPairFeatures(fv, predNode, argNode, "PA", position);
        getTokenPairFeatures(fv, prevPredNode, argNode, "PPA", voiceStr);
        getTokenPairFeatures(fv, prevPredNode, argNode, "PPA", depType+"");
        //getTokenPairFeatures(fv, prevPredNode, argNode, "PPA", position);


        if (left1ArgNode != null) {
            getTokenFeatures(fv, left1ArgNode, "LA", voiceStr);
            getTokenFeatures(fv, left1ArgNode, "LA", position);
            getTokenPairFeatures(fv, predNode, left1ArgNode, "PLA", voiceStr);
        } 
        if (left2ArgNode != null) {
            getTokenFeatures(fv, left2ArgNode, "LA", voiceStr);
            getTokenFeatures(fv, left2ArgNode, "LA", position);
            getTokenPairFeatures(fv, predNode, left2ArgNode, "PLA", voiceStr);
        } 
        //if (left3ArgNode != null) {
        //getTokenFeatures(fv, left3ArgNode, "LA", voiceStr);
        //getTokenFeatures(fv, left3ArgNode, "LA", position);
        //getTokenPairFeatures(fv, predNode, left3ArgNode, "PLA", voiceStr);
        //} 
        if (right1ArgNode != null) {
            getTokenFeatures(fv, right1ArgNode, "RA", voiceStr);
            getTokenFeatures(fv, right1ArgNode, "RA", position);
            getTokenPairFeatures(fv, predNode, right1ArgNode, "PRA", voiceStr);
        }
        if (right2ArgNode != null) {
            getTokenFeatures(fv, right2ArgNode, "RA", voiceStr);
            getTokenFeatures(fv, right2ArgNode, "RA", position);
            getTokenPairFeatures(fv, predNode, right2ArgNode, "PRA", voiceStr);
        }
        //if (right3ArgNode != null) {
        //getTokenFeatures(fv, right3ArgNode, "RA", voiceStr);
        //getTokenFeatures(fv, right3ArgNode, "RA", position);
        //getTokenPairFeatures(fv, predNode, right3ArgNode, "PRA", voiceStr);
        //}


        fv.add("PG:L:"+predLemma+":"+voiceStr);
        fv.add("PG:L:"+predLemma+":"+position);
        fv.add("PGA:LL:"+predLemma+argNode.getLemma()+":"+voiceStr);
        fv.add("PGA:LL:"+predLemma+argNode.getLemma()+":"+position);
        if (!argNode.ne.equals("O")) {
            String neTag = argNode.ne.substring(2,argNode.ne.length());
            fv.add("PGA:LNE:"+predLemma+neTag+",voice:"+voiceStr);
            fv.add("PGA:LNE:"+predLemma+neTag+",voice:"+position);
        }
        

        // add path features
        StringBuilder depParticlePOSPath = new StringBuilder();
        depParticlePOSPath.append("DEP_PTCL_POS_PATH:");

        StringBuilder depParticlePath = new StringBuilder();
        depParticlePath.append("DEP_PTCL_PATH:");

        //StringBuilder depCoarseRelPOSPath = new StringBuilder();
        //depCoarseRelPOSPath.append("DEP_COARSE_REL_POS_PATH:");

        //StringBuilder depCoarseRelPath = new StringBuilder();
        //depCoarseRelPath.append("DEP_COARSE_REL_PATH:");

        StringBuilder depPath = new StringBuilder();
        depPath.append("DEP_PATH:");

        StringBuilder posPath = new StringBuilder();
        posPath.append("POS_PATH:");

        String familyStr = "FAMILY_OTHER";

        // in same phrase
        if (predBunsetsu.getId() == argBunsetsu.getId()) {
            depParticlePath.append("SAME_B:");
            depParticlePOSPath.append("SAME_B:");
            familyStr = "FAMILY_SAME_B";
            String pathStr = tree.getPathStr(predNode.getId(), argNode.getId());
            //System.err.println("pathStr="+pathStr);

            if (pathStr.length() > 0) {
                String[] e = pathStr.split(",");

                for (int i = 0; i < e.length; i++) {
                    if (e[i].charAt(0) == '<' || e[i].charAt(0) == '>') {
                        if (e[i].charAt(0) == '<') {
                            DependencyNode n = tree.getNodeFromId(Integer.parseInt(e[i+1]));
                            String subPath = "<" + n.getDepRel();
                            depParticlePath.append(subPath);
                            depParticlePOSPath.append(subPath);
                            //depCoarseRelPath.append(subPath);
                            //depCoarseRelPOSPath.append(subPath);
                        } else if (e[i].charAt(0) == '>') {
                            DependencyNode n = tree.getNodeFromId(Integer.parseInt(e[i-1]));
                            String subPath = n.getDepRel() + ">";
                            depParticlePath.append(subPath);
                            depParticlePOSPath.append(subPath);
                            //depCoarseRelPath.append(subPath);
                            //depCoarseRelPOSPath.append(subPath);
                        }
                        depPath.append(e[i]);
                        continue;
                    }
                    DependencyNode n = tree.getNodeFromId(Integer.parseInt(e[i]));
                    int nHead = n.getHead();
                    if (nHead != -1) {
                        DependencyNode node = tree.getNodeFromId(nHead);
                        //System.err.println("node==null?"+(node==null));
                    
                        posPath.append(tree.getNodeFromId(nHead).getPOS());
                        depParticlePOSPath.append(tree.getNodeFromId(nHead).getPOS());
                    }
                }
                //System.err.println("depParticlePath="+depParticlePath);
                //System.err.println("depParticlePOSPath="+depParticlePOSPath);
            }
        } else {
            String pathStr = tree.getBunsetsuPathStr(predBunsetsu.getId(), argBunsetsu.getId());


            if (pathStr.length() > 0) {

                Matcher gchildMat = gchild.matcher(pathStr);
                Matcher gparentMat = gparent.matcher(pathStr);
                Matcher siblingMat = sibling.matcher(pathStr);
                Matcher childMat = child.matcher(pathStr);
                Matcher parentMat = parent.matcher(pathStr);
	    
                if (gchildMat.matches()) {
                    familyStr = "FAMILY_GCHILD";
                } else if (gparentMat.matches()) {
                    familyStr = "FAMILY_GPARENT";
                } else if (siblingMat.matches()) {
                    familyStr = "FAMILY_SIBLING";
                } else if (childMat.matches()) {
                    familyStr = "FAMILY_CHILD";
                } else if (parentMat.matches()) {
                    familyStr = "FAMILY_PARENT";
                }

                String[] e = pathStr.split(",");

                boolean skip = false;
                for (int i = 0; i < e.length; i++) {
                    if (e[i].charAt(0) == '<' || e[i].charAt(0) == '>') {
                        if (skip) { continue; }
                        if (e[i].charAt(0) == '<') {
                            Bunsetsu b = tree.getBunsetsuFromId(Integer.parseInt(e[i+1]));
                            String particleSequence = JapaneseDependencyTreeLib.getParticleSequence(b, this.particlePat);
                            boolean coordSkip = false;
                            //if (COORD_FEAT && b.getDepLabel().equals("P")) {
                            //coordSkip = true;
                            //}
                            String subPath;
                            if (COORD_FEAT) {
                                subPath = "<" + b.getDepLabel()+":"+particleSequence;
                            } else {
                                subPath = "<" + particleSequence;
                            }
                            //String subPath = "<" + particleSequence;
                            ArrayList<DependencyNode> bParticles = JapaneseDependencyTreeLib.getParticles(b, this.particlePat);
                            String bLastParticle = bParticles.size() == 0 ? "NONE" : bParticles.get(bParticles.size()-1).getForm();

                            depParticlePath.append(subPath);
                            depParticlePOSPath.append(subPath);

                        } else if (e[i].charAt(0) == '>') {
                            Bunsetsu b = tree.getBunsetsuFromId(Integer.parseInt(e[i-1]));
                            String particleSequence = JapaneseDependencyTreeLib.getParticleSequence(b, this.particlePat);
                            String subPath = null;
                            if (COORD_FEAT) {
                                subPath = particleSequence + ":"+ b.getDepLabel() + ">";
                            } else {
                                subPath = particleSequence + ">";
                            }
                            //String subPath = particleSequence + ">";
                            ArrayList<DependencyNode> bParticles = JapaneseDependencyTreeLib.getParticles(b, this.particlePat);
                            String bLastParticle = bParticles.size() == 0 ? "NONE" : bParticles.get(bParticles.size()-1).getForm();
                            depParticlePath.append(subPath);
                            depParticlePOSPath.append(subPath);
                        }
                        depPath.append(e[i]);

                        continue;
                    }
                    Bunsetsu b = tree.getBunsetsuFromId(Integer.parseInt(e[i]));
                    depParticlePOSPath.append(b.getHeadNode().getPOS());
                    /*
                    if (COORD_FEAT) {
                        if ( b.getDepLabel().equals("P") && 
                             (i+1 < e.length && e[i+1].charAt(0) == '<') || 0 <= i-1 && e[i-1].charAt(0) == '>') {

                            //Bunsetsu childB = tree.getNodeFromId(Integer.parseInt(e[i+2]));
                            ///if (childB
                            //System.err.println("HOGE e[i]="+e[i]);
                            skip = true;
                        } else {
                            posPath.append(b.getHeadNode().getPOS());
                        }
                    } else {
                        posPath.append(b.getHeadNode().getPOS());
                    }
                    */
                }
            }
            //System.err.println("pathStr="+pathStr);
            //System.err.println("depParticlePath="+depParticlePath);
            //System.err.println("depParticlePOSPath="+depParticlePOSPath);
        }

        fv.add(familyStr); // for argument identification
        //featsAry.add(familyStr+"-"+position); // new
        //featsAry.add(familyStr+"-"+position+"-"+voiceStr); // new

        fv.add(argParticleStr+"-"+voiceStr);
        fv.add(argParticleStr+"-"+familyStr+"-"+voiceStr);

        fv.add(posPath+"");
        fv.add(posPath+"-"+voiceStr);
        fv.add(posPath+"-"+position);
        fv.add(posPath+"-"+position+voiceStr);
        //fv.add(familyStr+"-"+posPath);
        //fv.add(argParticleStr+"-"+posPath+"-"+voiceStr);

        //featsAry.add(depParticlePath+""); // new
        //featsAry.add(familyStr+"-"+depParticlePath);
        fv.add(depParticlePath+"-"+position);
        fv.add(depParticlePath+"-"+voiceStr);
        fv.add(depParticlePath+"-"+position+voiceStr);

        //featsAry.add(depParticlePOSPath+""); // new
        //featsAry.add(familyStr+"-"+depParticlePath);
        fv.add(depParticlePOSPath+"-"+position);
        //featsAry.add(depParticlePOSPath+"-"+voice); // new 
        fv.add(depParticlePOSPath+"-"+position+"-"+voiceStr);

        // add features of left/right dependent of the argument
        /*
        int[] aChildren = argBunsetsu.getChildren();
        
        if (aChildren.length > 0) {
            // LEFT WORD/POS, RIGHT WORD/POS
            Bunsetsu leftMostB = tree.getBunsetsuFromId(aChildren[0]);
            featsAry.add("LMDN:W:" + leftMostB.getHeadNode().getLemma());
            featsAry.add("LMDN:P:" + leftMostB.getHeadNode().getPOS());
            
            Bunsetsu rightMostB = tree.getBunsetsuFromId(aChildren[aChildren.length-1]);
            featsAry.add("RMDN:W:" + rightMostB.getHeadNode().getLemma());
            featsAry.add("RMDN:P:" + rightMostB.getHeadNode().getPOS());
        }

        if (argBunsetsu.getHead() != -1) {
            Bunsetsu hB = tree.getBunsetsuFromId(argBunsetsu.getHead());
            if (hB != null) {
                DependencyNode hBHead = hB.getHeadNode();
                int[] aPChildren = hB.getChildren();
                int argBIdx = -1;
                for (int j = 0; j < aPChildren.length; j++) {
                    if (aPChildren[j] == argBunsetsu.getId()) {
                        argBIdx = j;
                    }
                }
                if (argBIdx != -1 && 0 <= argBIdx-1) {
                    Bunsetsu leftMostSiblingBunsetsu = tree.getBunsetsuFromId(aPChildren[argBIdx-1]);
                    DependencyNode leftMostSiblingBunsetsuHead= leftMostSiblingBunsetsu.getHeadNode();
                    fv.add("LMSN:W:" + leftMostSiblingBunsetsuHead.getLemma()+",voice:"+voiceStr);
                }
                if (argBIdx != -1 && argBIdx+1 < aPChildren.length) {
                    Bunsetsu rightMostSiblingBunsetsu = tree.getBunsetsuFromId(aPChildren[argBIdx+1]);
                    DependencyNode rightMostSiblingBunsetsuHead = rightMostSiblingBunsetsu.getHeadNode();
                    fv.add("RMSN:W:" + rightMostSiblingBunsetsuHead.getLemma()+",voice:"+voiceStr);
                }
            }
        }
        */

        // get cooccurence features (PMI) 
        if (this.coocMan != null) {
            double pmi = 0.0;
            String cvKey = null;
            String cKey = null;

            Bunsetsu ab = tree.getBunsetsuFromNodeId(argId);
            ArrayList<DependencyNode> particles = JapaneseDependencyTreeLib.getParticles(ab, particlePat);
            String particleStr = JapaneseDependencyTreeLib.getParticleSequence(ab, particlePat);
            //System.err.println("particleStr="+particleStr);
            //DependencyNode argNode = tree.getNodeFromId(argId);
            DependencyNode[] abNodes = ab.getNodes();
            DependencyNode caseNode = particles.size() == 0 ? null : particles.get(particles.size()-1);

            String caseLemma = caseNode == null ? "" : caseNode.getLemma();
            // keys for cooc db access
            String nKey = argNode.getLemma()+"|"+argNode.getPOS();

            if (predType.charAt(0) == 'p') { // applyed for 'pred' predicates
                if (this.sahenPat.containsKey(predNode.getLemma()) && 
                    this.sahenPat.get(predNode.getLemma()).equals(prevPredNode.getPOS())) {
                    cvKey = caseLemma+":"+prevPredNode.getLemma()+predNode.getLemma();
                    cKey = caseLemma;
                } else {
                    cvKey = caseLemma+":"+predNode.getLemma();
                    cKey = caseLemma;
                }
                pmi = this.coocMan.getMI(cvKey, nKey, PMI_NORMALIZED);
            } else if (predType.charAt(0) == 'e') { // applied for "event" predicates
                double pmiMax = Double.NEGATIVE_INFINITY;
                for (int l = 0; l < FORM_CASES_STR.length; l++) {
                    String localCVKey = FORM_CASES_STR[l]+":"+predNode.getLemma()+FORM_SURU_STR;
                    double pmiLocal = this.coocMan.getMI(localCVKey, nKey, PMI_NORMALIZED);
                    if (pmiMax > pmiLocal) { continue; }
                    pmiMax = pmiLocal;
                    cvKey = localCVKey;
                    cKey = FORM_CASES_STR[l];
                }
                pmi = pmiMax; 
            } else {
                // provisional
                cvKey = "";
                cKey = "";
                pmi = 0.0;
            }
            fv.add("PMI:"+cKey, (float) pmi);
        }

        // get features taken from kyoto university case frames
        if (kucfMan != null && sw2000Man != null && sw500Man != null) {
            StringBuilder predSeq = new StringBuilder();
            StringBuilder predSeq1 = new StringBuilder();
            StringBuilder predSeq2 = new StringBuilder();
            int length = 0;
            
            if ((length = this.kucfPredPat.matches(tree, predId)) > 0) {
                for (int i = predId - length + 1; i <= predId; i++) {
                    predSeq1.append(tree.getNodeFromId(i).getLemma());
                }    
                for (int i = predId - length + 1; i < predId; i++) {
                    predSeq2.append(tree.getNodeFromId(i).getLemma());
                }
            }
            String predSeqStr1 = predSeq1.toString().equals("") ? predNode.getLemma() : predSeq1.toString();
            String predSeqStr2 = predSeq2.toString().equals("") ? null : predSeq2.toString();


            Bunsetsu ab = tree.getBunsetsuFromNodeId(argId);
            String particleStr = JapaneseDependencyTreeLib.getParticleSequence(ab, particlePat);
            ArrayList<DependencyNode> particles = JapaneseDependencyTreeLib.getParticles(ab, particlePat);
            DependencyNode caseNode = particles.size() == 0 ? null : particles.get(0);
            String caseLemma = caseNode == null ? "" : caseNode.getLemma();

            for (Iterator<String> it = this.cases.iterator(); it.hasNext();) {
                String caseStr = it.next();
                if (caseStr.equalsIgnoreCase("wo")) {
                    caseStr = "o";
                }
                int freq = kucfMan.getFrequency(predSeqStr1, caseStr.toUpperCase(), argNode.getLemma());
                if (freq == 0 && predSeqStr2 != null) {
                    freq = kucfMan.getFrequency(predSeqStr2, caseStr.toUpperCase(), argNode.getLemma());
                }
                if (freq > 0.0) {
                    fv.add("KUCF-"+caseStr.toUpperCase(), Math.log(freq+1.0));
                }                 
            }

            String predCLS2K = sw2000Man.getValue(predSeqStr1);
            if (predCLS2K == null && predSeqStr2 != null) {
                predCLS2K = sw2000Man.getValue(predSeqStr2);
            }
            String predCLS5H = sw500Man.getValue(predSeqStr1);
            if (predCLS5H == null && predSeqStr2 != null) {
                predCLS5H = sw500Man.getValue(predSeqStr2);
            }
            String argCLS2K = sw2000Man.getValue(argNode.getLemma());
            String argCLS5H = sw500Man.getValue(argNode.getLemma());

            if (predCLS2K != null && argCLS2K != null) {

                //HashMap<String, int[]> freq5H = new HashMap<String, int[]>();
                for (Iterator<String> it = this.cases.iterator(); it.hasNext();) {
                    int[] freq2K = new int[3];
                    String caseStr = it.next();
                    if (caseStr.equalsIgnoreCase("wo")) {
                        caseStr = "o";
                    }

                    freq2K[0] = kucf2KMan[0].getFrequency(predSeqStr1, caseStr.toUpperCase(), argCLS2K);
                    if (freq2K[0] == 0 && predSeqStr2 != null) {
                        freq2K[0] = kucf2KMan[0].getFrequency(predSeqStr2, caseStr.toUpperCase(), argCLS2K);
                    }
                    freq2K[1] = kucf2KMan[1].getFrequency(predCLS2K, caseStr.toUpperCase(), argCLS2K);
                    freq2K[2] = kucf2KMan[2].getFrequency(predCLS2K, caseStr.toUpperCase(), argNode.getLemma());
                    for (int i = 0; i < freq2K.length; i++) {
                        if (freq2K[i] > 0) {
                            fv.add("KUCF-"+caseStr+"-2K["+i+"]", Math.log(freq2K[i]+1.0));
                        }
                    }
                }

                for (Iterator<String> it = this.cases.iterator(); it.hasNext();) {
                    int[] freq5H = new int[3];
                    String caseStr = it.next();
                    if (caseStr.equalsIgnoreCase("wo")) {
                        caseStr = "o";
                    }

                    freq5H[0] = kucf5HMan[0].getFrequency(predSeqStr1, caseStr.toUpperCase(), argCLS5H);
                    if (freq5H[0] == 0 && predSeqStr2 != null) {
                        freq5H[0] = kucf5HMan[0].getFrequency(predSeqStr2, caseStr.toUpperCase(), argCLS5H);
                    }
                    freq5H[1] = kucf5HMan[1].getFrequency(predCLS5H, caseStr.toUpperCase(), argCLS5H);
                    freq5H[2] = kucf5HMan[2].getFrequency(predCLS5H, caseStr.toUpperCase(), argNode.getLemma());
                    for (int i = 0; i < freq5H.length; i++) {
                        if (freq5H[i] > 0) {
                            fv.add("KUCF-"+caseStr+"-5H["+i+"]", Math.log(freq5H[i]+1.0));
                        }
                    }
                }
            }
        }
        return fv;
    }
    
    public int numOfFeatures() {
        return 44;
    }
    

    public FeatureVector getGlobalFeatureVector(AlphabetTrie alphabet, DependencyTree tree, int predId, 
                                                String predType, LabelAssignment asn, 
                                                HashMap<String, String> caseMap, boolean alphabetGrowth) {
        FeatureVector fv = new FeatureVector(alphabet, alphabetGrowth);

        int[] srlIndices = asn.getIndices();
        Label[] srlLabels = asn.getLabels();

        DependencyNode predNode = tree.getNodeFromId(predId);
        DependencyNode prevPredNode = tree.getNodeFromId(predId-1);

        String predPOS = predNode.getPOS();

        // pred sequence
        int length;
        StringBuilder predSeq1 = new StringBuilder();
        StringBuilder predSeq2 = new StringBuilder();
        if ((length = this.kucfPredPat.matches(tree, predId)) > 0) {
            for (int j = predId - length + 1; j <= predId; j++) {
                predSeq1.append(tree.getNodeFromId(j).getLemma());
            }
            for (int j = predId - length + 1; j < predId; j++) {
                predSeq2.append(tree.getNodeFromId(j).getLemma());
            }
        } else {
            predSeq1.append(predNode.getLemma());
        }
        String predSeqStr1;
        String predSeqStr2;
        if (predSeq1.toString().equals("")) {
            predSeqStr1 = predNode.getLemma();
        } else {
            predSeqStr1 = predSeq1.toString();
        }
        if (predSeq2.toString().equals("")) {
            predSeqStr2 = null;
        } else {
            predSeqStr2 = predSeq2.toString();
        }
        String predSeqStr = predSeqStr2 == null ? predSeqStr1 : predSeqStr2;

        // TEMPLATES, WHOLE LABEL SEQUENCE -----------------------------------
        {
            //StringBuilder tempEl = new StringBuilder();
            //tempEl.append("TEMPL:");

            StringBuilder wls = new StringBuilder();
            wls.append("WLSEQ:");
            wls.append("PRED:"+predSeqStr+"ARG");
            //boolean predUsed = false;
            for (int i = 0; i < srlIndices.length; i++) {
                DependencyNode node = tree.getNodeFromId(srlIndices[i]);
                String nodePOS = node.getPOS();

                //if (!predUsed && predId < srlIndices[i]) {
                //wls.append(predSeqStr);    
                //predUsed = true;
                //}

                if (srlLabels[i].toString().equals("NONE")) {
                    continue;
                }
                wls.append(","+srlLabels[i].toString());

            }
            //feats.add(wls.toString());

            //System.err.println(wls+"");
        }

        // MISSING ARGUMENT LABELS ----------------------------------------
        {
            Label[] asnLabels = asn.getLabels();

            HashSet<String> existArgs = new HashSet<String>();
            for (int i = 0; i < asnLabels.length; i++) {
                if (asnLabels[i].toString().equalsIgnoreCase("NONE")) { continue; }
                if (existArgs.contains(asnLabels[i].toString())) { continue; }
                existArgs.add(asnLabels[i].toString());
            }

            String[] argLabels = (String[]) existArgs.toArray(new String[existArgs.size()]);

            for (Iterator<String> it = this.cases.iterator(); it.hasNext();) {
                String caseStr = it.next();
                if (caseStr.equalsIgnoreCase("wo")) {
                    caseStr = "o";
                }

                boolean flag = false;
                for (int j = 0; j < argLabels.length; j++) {
                    if (argLabels[j].equalsIgnoreCase(caseStr)) {
                        flag = true;
                    }
                }
                //caseExists[i] = flag ? true : false;
            }
            
            
            TObjectIntHashMap labelNum = new TObjectIntHashMap();
            for (int i = 0; i < asnLabels.length; i++) {
                String l = asnLabels[i].toString();
                if (!labelNum.containsKey(l)) {
                    labelNum.put(l, 0);
                }
                labelNum.increment(l);
            }

            Object[] keys = labelNum.keys();
            for (int i = 0; i < keys.length; i++) {
                String label = (String) keys[i];
                if (label.equalsIgnoreCase("NONE")) { continue; }
                int cnt = labelNum.get(keys[i]);
                if (cnt >= 5) {
                    //fv.add("numcase:"+keys[i]+":n>=5");
                } else if (cnt >= 3) {
                    //fv.add("numcase:"+keys[i]+":4>=n>=3");
                } else if (cnt >= 2) {
                    //fv.add("numcase:"+keys[i]+":n==2");
                } else if (cnt == 1) {
                    //fv.add("numcase:"+keys[i]+":n==1");
                }
            }
        }

        // add features
        double kucfScore = 0.0;
        String predStr = null;
        if (this.kucfMan != null) {
            TObjectIntHashMap<String> kucfCheckHash = new TObjectIntHashMap<String>();
            int[] asnIndices = asn.getIndices();
            Label[] asnLabels = asn.getLabels();
            if (this.sahenPat.containsKey(predNode.getLemma()) &&
                this.sahenPat.get(predNode.getLemma()).equals(prevPredNode.getPOS())) {
                predStr = prevPredNode.getLemma();
            } else {
                predStr = predNode.getLemma();
            }

            for (int i = 0; i < asnLabels.length; i++) {
                int id = asnIndices[i];
                String caseStr = asnLabels[i].toString();
                if (caseStr.equalsIgnoreCase("wo")) {
                    caseStr = "o";
                }

                DependencyNode argNode = tree.getNodeFromId(id);
                if (caseStr.equals("NONE")) { continue; }
                int freq = kucfMan.getFrequency(predStr, caseStr.toUpperCase(), argNode.getLemma());
                if (freq >= 1)
                    kucfCheckHash.put(caseStr.toUpperCase(), freq);
            }

            HashSet<String> existArgs = new HashSet<String>();
            for (int i = 0; i < asnLabels.length; i++) {
                if (asnLabels[i].toString().equalsIgnoreCase("NONE")) { continue; }
                if (existArgs.contains(asnLabels[i].toString())) { continue; }
                existArgs.add(asnLabels[i].toString());
            }

            String[] argLabels = (String[]) existArgs.toArray(new String[existArgs.size()]);

            int[] caseFreqs = new int[3];
            int caseFreqMax = 1;
            boolean[] caseExists = new boolean[3];
            int i = 0;
            for (Iterator<String> it = this.cases.iterator(); it.hasNext();) {
                String caseStr = it.next();
                if (caseStr.equalsIgnoreCase("wo")) {
                    caseStr = "o";
                }

                if (kucfCheckHash.containsKey(caseStr)) {
                    kucfScore += Math.log(kucfCheckHash.get(caseStr));
                } 
                caseFreqs[i] = kucfMan.getFrequency(predStr, caseStr);
                if (caseFreqMax < caseFreqs[i]) {
                    caseFreqMax = caseFreqs[i];
                }

                boolean flag = false;
                for (int j = 0; j < argLabels.length; j++) {
                    if (argLabels[j].equalsIgnoreCase(caseStr)) {
                        flag = true;
                    }
                }
                caseExists[i] = flag ? true : false;
                i++;
            }
            fv.add("KUCF_SCORE:", kucfScore);

            i = 0;
            for (Iterator<String> it = this.cases.iterator(); it.hasNext();) {
                String caseStr = it.next();
                if (caseStr.equalsIgnoreCase("wo")) {
                    caseStr = "o";
                }

                double ratio = (double) caseFreqs[i] / (double) caseFreqMax;
                //System.err.println("case:"+cases[i]+" ratio="+ratio);
                if (ratio > CASE_EXISTS_THRESHOLD) {
                    if (!caseExists[i]) {
                        fv.add("ARG_MISSING:"+caseStr);
                        //System.err.println("ARG_MISSING:"+cases[i]);
                    } else {
                        fv.add("ARG_EXISTS:"+caseStr);
                        //System.err.println("ARG_EXISTS:"+cases[i]);
                    } 
                }
            }
        }

        return fv;
    }
    
    private Voice getVoice (DependencyTree tree, int predId) {
        DependencyNode predNode = tree.getNodeFromId(predId);
        if (predId+1 >= tree.size()) {
            return Voice.Active; // provisional
        } 
        DependencyNode predsNextNode = tree.getNodeFromId(predId+1);
        if (this.passivePat.containsKey(predsNextNode.getLemma())) {
            String pos = this.passivePat.get(predsNextNode.getLemma());
            if (pos.equals(predsNextNode.getPOS())) {
                return Voice.Passive;
            }
        }
        if (this.causativePat.containsKey(predsNextNode.getLemma())) {
            String pos = this.causativePat.get(predsNextNode.getLemma());
            if (pos.equals(predsNextNode.getPOS())) {
                return Voice.Causative;
            }
        }
        if (predNode.getPOS().equals(POS_NOUN_SAHEN_STR)) {
            return Voice.Nominal;
        }
        return Voice.Active;
    }
}
