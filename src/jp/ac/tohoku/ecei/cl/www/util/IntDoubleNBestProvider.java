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
import java.util.PriorityQueue;
import java.util.Comparator;

import jp.ac.tohoku.ecei.cl.www.base.*;

public class IntDoubleNBestProvider {

    PriorityQueue nBPQ = null;
    PriorityQueue aPQ = null;

    int[] indices;

    public class State implements Comparator, Comparable {

        public State parent;
        public State[] children;

        public int index;
        public int intVal;
        public double score;
        public double value;

        public State(int index, int intVal) {
            this(index,intVal,0.0);
        }

        public State(int index, int intVal, double score) {
            this.index = index;
            this.intVal = intVal;
            this.score = score;
        }
  
        public void setScore(double value) {
            this.score = value;
        }

        public void setChildren(State[] children) {
            this.children = children;
        }
  
        public State getParent() {
            return this.parent;
        }

        public State[] getChildren() {
            return this.children;
        }

        public int compare(Object o1, Object o2) {
            State n1 = (State) o1;
            State n2 = (State) o2;
            if (n1.score < n2.score) {
                return -1;
            } else if (n1.score == n2.score) {
                return 0;
            } else {
                return 1;
            }
        }

        public int compareTo(Object o1) {
            State n1 = (State) o1;
            if (this.score < n1.score) {
                return -1;
            } else if (this.score == n1.score) {
                return 0;
            } else {
                return 1;
            }
        }

        public boolean equals(State node) {
            return false;
        }

        public String toString() {
            StringBuilder s = new StringBuilder();
            s.append("[State]");
            s.append(" score=" + this.score);
            s.append(" index=" + this.index);
            s.append(" intval=" + this.intVal);
            if (this.parent != null) {
                s.append(" parent=" + this.parent.index);
            } else {
                s.append(" parent=null");
            }
            return s.toString();
        }
    }

    public class StateComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            State n1 = (State) o1;
            State n2 = (State) o2;
            if (n1.score < n2.score) {
                return 1;
            } else if (n1.score == n2.score) {
                return 0;
            } else {
                return -1;
            }
        }
    }


    public IntDoubleNBestProvider (int[] indices, IntDoublePairs[] results, int[][] constraints, int n) {
        this.indices = indices;

        int latticeIndex = 0;

        // init
        this.nBPQ = new PriorityQueue(200, new StateComparator());

        while (latticeIndex < indices.length) {

            PriorityQueue pQ = new PriorityQueue(100, new StateComparator());

            int[] ids = results[latticeIndex].getIds();
            double[] values = results[latticeIndex].getValues();

            if (1 <= latticeIndex) {
                int pollCnt = 0;
                State[] nBestStates = new State[n];
                while(pollCnt < n) {
                    State s = (State) nBPQ.poll();
                    if (s == null) { break; }
                    nBestStates[pollCnt++] = s;
                }
                nBPQ.clear();

                for (int l = 0; l < nBestStates.length; l++) {
                    if (nBestStates[l] == null) { continue; }
                }
		
                for (int l = 0; l < nBestStates.length; l++) {
                    for (int j = 0; j < ids.length; j++) {
                        if (nBestStates[l] == null) { break; }
                        if (constraints != null && constraints[latticeIndex] != null && constraints[latticeIndex][j] == 1) { continue; }
                        State node = new State(latticeIndex, ids[j], nBestStates[l].score + values[j]);
                        node.value = values[j];
                        node.parent = nBestStates[l];
                        pQ.add(node);
                    }
                }
            } else {
                for (int j = 0; j < ids.length; j++) {
                    if (constraints != null && constraints[latticeIndex] != null && constraints[latticeIndex][j] == 1) { continue; }
                    State node = new State(latticeIndex, ids[j], values[j]);
                    node.value = values[j];
                    pQ.add(node);
                }
            }
            int pollCnt = 0;
            while (pollCnt < n) {
                State node;
                if ((node = (State) pQ.poll()) != null) {
                    nBPQ.add(node);
                } else {
                    break;
                }
                pollCnt++;
            }
            latticeIndex++;
        }
    }

    public IntDoubleNBestProvider (int[] indices, IntDoublePairs[] results, int n) {
        this (indices, results, null, n);
    }

    public int size() {
        if (this.nBPQ != null) {
            return this.nBPQ.size();
        } else if (this.aPQ != null) {
            return this.aPQ.size();
        } else {
            return -1;
        }
    }

    public IntAssignment next () { 
        if (nBPQ != null) {
            if (nBPQ.size() > 0) {
                State node = (State) nBPQ.poll();
                IntAssignment asn = new IntAssignment();
                asn.score = node.score;
                while (node != null) {
                    asn.add(this.indices[node.index], node.intVal, node.value);
                    node = node.parent;
                }
                return asn;
            } else {
                return null;
            }
        } else if (aPQ != null) {
            if (aPQ.size() > 0) {
                return (IntAssignment) aPQ.poll();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
    /*
    public static void main(String[] args) {

        int[] indices = new int[]{1,2,3};
        ClassifyResults2[] crs = new ClassifyResults2[indices.length];
        Alphabet la = new Alphabet();
        int g = 1;
        crs[0] = new ClassifyResults2(new Label[]{new Label(g,la,la.lookupIndex("NONE")),
                                                  new Label(g,la,la.lookupIndex("A0")),
                                                  new Label(g,la,la.lookupIndex("A1")),
                                                  new Label(g,la,la.lookupIndex("A2"))}, new double[]{0.5,0.45,0.05,0.35}, new int[]{0,0,0,0});
        crs[1] = new ClassifyResults2(new Label[]{new Label(g,la,la.lookupIndex("NONE")),
                                                  new Label(g,la,la.lookupIndex("A0")),
                                                  new Label(g,la,la.lookupIndex("A1")),
                                                  new Label(g,la,la.lookupIndex("A2"))}, new double[]{0.7,0.25,0.50,0.75}, new int[]{0,0,0,0});
        crs[2] = new ClassifyResults2(new Label[]{new Label(g,la,la.lookupIndex("NONE")),
                                                  new Label(g,la,la.lookupIndex("A0")),
                                                  new Label(g,la,la.lookupIndex("A1")),
                                                  new Label(g,la,la.lookupIndex("A2"))}, new double[]{0.7,0.25,0.50,0.75}, new int[]{0,0,0,0});
        Timer timer = new Timer();
        timer.start();
        NBestLabelAssignmentProvider nbp = new NBestLabelAssignmentProvider(indices, crs, 10);
        timer.stop();

        LabelAssignment asn;
        ArrayList<LabelAssignment> aP = new ArrayList<LabelAssignment>();
        System.out.println("n best list created from classify results");
        int cnt = 1;
        while( (asn = nbp.next()) != null) {
            aP.add(asn);
            System.out.println((cnt++)+" th asn:"+asn);
        }
    }
    */
}
