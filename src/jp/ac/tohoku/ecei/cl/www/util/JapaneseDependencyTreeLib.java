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
import java.util.HashSet;
import java.util.Iterator;

import jp.ac.tohoku.ecei.cl.www.base.*;
import jp.ac.tohoku.ecei.cl.www.util.*;

import gnu.trove.iterator.*;
import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.hash.*;

public class JapaneseDependencyTreeLib {

    public static void annotateWordLevelDependencies (DependencyTree tree, HashSet<String> funcPat, HashSet<String> headPat) {
        setWordHead(tree, funcPat, headPat);
    }

    public static void setBunsetsuHead (DependencyTree tree, HashSet<String> funcPat, HashSet<String> headPat) {
        Bunsetsu[] bList = tree.getBunsetsuList();
        for (int i = 0; i < bList.length; i++) {
            bList[i].setHeadNode(getHeadNode(bList[i], funcPat, headPat));
        }
    }

    public static DependencyNode getHeadNode (Bunsetsu b, HashSet<String> funcPat, HashSet<String> headPat) {
	
        DependencyNode[] bNodes = b.getNodes();
        int idx = bNodes.length-1;
        while (idx > 0) {
            if (headPat.contains(bNodes[idx].getPOS())) {
                break;
            } else {
                idx--;
            }
        }
        return bNodes[idx];
    }

    public static ArrayList<DependencyNode> getParticles (Bunsetsu b, HashSet<String> particlePat) {
        DependencyNode[] bNodes = b.getNodes();
        TIntArrayList indices = getParticleIndices (b, particlePat);
        ArrayList<DependencyNode> particleNodes = new ArrayList<DependencyNode>();
        for (int i = 0; i < indices.size(); i++) {
            particleNodes.add(bNodes[indices.get(i)]);
        }
        return particleNodes;
        //return idx == -1 ? null : bNodes[idx];
    }

    private static TIntArrayList getParticleIndices (Bunsetsu b, HashSet<String> particlePat) {
        TIntArrayList indices = new TIntArrayList();
        DependencyNode[] bNodes = b.getNodes();
        int idx = 0;
        while (idx < bNodes.length) {
            if (particlePat.contains(bNodes[idx].getPOS())) {
                indices.add(idx);
            } 
            idx++;
        }
        return indices;
    }
    
    public static void setParticleToBunsetsuDepRel (DependencyTree tree, HashSet<String> particlePat) {
        Bunsetsu[] bList = tree.getBunsetsuList();
        for (int i = 0; i < bList.length; i++) {
            bList[i].setDepLabel(getParticleSequence(bList[i], particlePat));
        }
    }

    public static String getParticleSequence (Bunsetsu b, HashSet<String> particlePat) {
        ArrayList<DependencyNode> particleNodes = getParticles(b, particlePat);
        StringBuilder particlesStr = new StringBuilder();
        if (particleNodes.size() == 0) {
            //DependencyNode[] bNodes = bList[i].getNodes();
            //particlesStr.append(bNodes[bNodes.length-1].getLemma());
            particlesStr.append("NONE");
        } else {
            for (int j = 0; j < particleNodes.size(); j++) {
                particlesStr.append(particleNodes.get(j).getForm());
                if (j != particleNodes.size()-1) {
                    particlesStr.append("-");
                }
            }
        }
        return particlesStr.toString();
    }

    public static void setWordHead (DependencyTree tree, HashSet<String> funcPat, HashSet<String> headPat) {
        Bunsetsu[] bunsetsuList = tree.getBunsetsuList();

        for (int i = 0; i < bunsetsuList.length; i++) {
            int bHead = bunsetsuList[i].getHead();
            Bunsetsu hBunsetsu = tree.getBunsetsuFromId(bHead);
            DependencyNode[] hBNodes = hBunsetsu.getNodes();
            int hBIdx = hBNodes.length-1;

            while (hBIdx > 0) {
                if (headPat.contains(hBNodes[hBIdx].getPOS())) {
                    break;
                } else {
                    hBIdx--;
                }
            }

            // set word head
            DependencyNode[] dBNodes = bunsetsuList[i].getNodes();
            for (int j = 0; j < dBNodes.length-1; j++) {
                dBNodes[j].setHead(dBNodes[j+1].getId());
                dBNodes[j].setDepRel("D");
            }
            dBNodes[dBNodes.length-1].setHead(hBNodes[hBIdx].getId());
            dBNodes[dBNodes.length-1].setDepRel(bunsetsuList[i].getDepLabel());
        }
        tree.setChildren();
    }


    public static void main (String[] args) {
	
	
    }

}
