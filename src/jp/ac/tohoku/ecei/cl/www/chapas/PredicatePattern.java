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
import java.util.Collection;
import java.util.HashMap;

//import org.speedblue.util.*;
import jp.ac.tohoku.ecei.cl.www.base.*;

public class PredicatePattern {

    // these nested hashes should be replaced by Tries!!!
    HashMap<String, HashMap<String, String>> pat2;
    HashMap<String, HashMap<String, HashMap<String, String>>> pat3;

    public PredicatePattern (Collection<String> patterns) {
        pat2 = new HashMap<String, HashMap<String, String>>();
        pat3 = new HashMap<String, HashMap<String, HashMap<String, String>>>();
        for ( String patStr : patterns) {
            String[] e = patStr.split("\t");
            //System.err.println("e.length="+e.length+" patStr="+patStr);
            if (e.length == 2) {
                int idx = 0;
                HashMap<String, String> inner1 = null;
                String inner2 = null;
                while (idx < e.length) {
                    if (idx == 0) {
                        if (!pat2.containsKey(e[0])) {
                            pat2.put(e[0], new HashMap<String, String>());
                        }
                        inner1 = pat2.get(e[0]);
                    } else if (idx == 1) {
                        if (!inner1.containsKey(e[1])) { 
                            inner1.put(e[1], "E");
                        }
                    }
                    idx++;
                }
            } else if (e.length == 3) {
                int idx = 0;
                HashMap<String, HashMap<String, String>> inner1 = null;
                HashMap<String, String> inner2 = null;
                String inner3 = null;
                while (idx < e.length) {
                    if (idx == 0) {
                        if (!pat3.containsKey(e[0])) {
                            pat3.put(e[0], new HashMap<String, HashMap<String, String>>());
                        }
                        inner1 = pat3.get(e[0]);
                    } else if (idx == 1) {
                        if (!inner1.containsKey(e[1])) { 
                            inner1.put(e[1], new HashMap<String, String>());
                        }
                        inner2 = inner1.get(e[1]);
                    } else if (idx == 2) {
                        if (!inner2.containsKey(e[2])) {
                            inner2.put(e[2], "E");
                        }
                    }
                    idx++;
                }
            }
        }
    }
    
    public int matches (DependencyTree tree, int id) {
        //return matches (tree, id, null);
        //}
        //public int matches (DependencyTree tree, int id, StringBuilder matchedString) {
        // case: 2 words 
        {
            int idx = 0;
            HashMap<String, String> inner1 = null;
            String inner2 = null;
            while (idx < 2) {
                //System.err.println("idx=="+idx);
                if (id - idx < 1) { break; }
                DependencyNode node = tree.getNodeFromId(id - idx);
                //System.err.println("[PredicatePattern] idx="+idx+" lemma="+node.getLemma());
                if (idx == 0) {
                    String str = node.getLemma();
                    if (!pat2.containsKey(str)) {
                        //System.err.println("!pat2.containsKey(str): str="+str);
                        break;
                    }
                    //matchedString.insert(0, node.getLemma());
                    inner1 = pat2.get(str);
                } else if (idx == 1) {
                    String str = node.getPOS();
                    //matchedString.insert(0, node.getLemma());
                    if (!inner1.containsKey(str)) { 
                        //System.err.println("!inner1.containsKey(str): str="+str);
                        break;
                    } 
                    return 2;
                } 
                idx++;
            }
        }

        // case: 3 words
        {
            int idx = 0;
            HashMap<String, HashMap<String, String>> inner1 = null;
            HashMap<String, String> inner2 = null;
            while (idx < 3) {
                DependencyNode node = tree.getNodeFromId(id - idx);
                if (idx == 0) {
                    String str = node.getLemma();
                    if (!pat3.containsKey(str)) {
                        break;
                    }
                    //matchedString.insert(0, node.getLemma());
                    inner1 = pat3.get(str);
                } else if (idx == 1) {
                    String str = node.getPOS();
                    if (!inner1.containsKey(str)) { 
                        break;
                    }
                    //matchedString.insert(0, node.getLemma());
                    inner2 = inner1.get(str);
                } else if (idx == 2) {
                    String str = node.getPOS();
                    if (!inner2.containsKey(str)) {
                        break;
                    }
                    return 3;
                }
                idx++;
            }
        }
        return 0;
    }
}
