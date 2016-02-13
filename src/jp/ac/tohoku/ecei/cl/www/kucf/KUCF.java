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

package jp.ac.tohoku.ecei.cl.www.kucf;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;

import gnu.trove.iterator.*;
import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.hash.*;

public class KUCF implements Serializable {

    // HashMap<String:pred, HashMap<String:case, String:arg-int:freq>>
    private HashMap<String, HashMap<String, TObjectIntHashMap<String>>> cf;

    public KUCF (HashMap<String, HashMap<String, TObjectIntHashMap<String>>> cf) {
        this.cf = cf;
    }

    public int getFreq(String pred, String caseStr, String arg) {
        if (!cf.containsKey(pred)) {
            return 0;
        }
        HashMap<String, TObjectIntHashMap<String>> pcf = cf.get(pred);
        if (!pcf.containsKey(caseStr)) { 
            return 0;
        }
        TObjectIntHashMap freqHash = pcf.get(caseStr);
        if (!freqHash.containsKey(arg)) { 
            return 0;
        } else {
            return freqHash.get(arg);
        }
    }
    
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (Iterator it = cf.keySet().iterator(); it.hasNext();) {
            String pred = (String) it.next();
            HashMap<String, TObjectIntHashMap<String>> pcf = (HashMap<String, TObjectIntHashMap<String>>) cf.get(pred);
            for (Iterator pcfIt = pcf.keySet().iterator(); pcfIt.hasNext();) {
                String caseStr = (String) pcfIt.next();
                TObjectIntHashMap<String> freqHash = (TObjectIntHashMap<String>) pcf.get(caseStr);
                for (TObjectIntIterator fIt = freqHash.iterator(); fIt.hasNext();) {
                    fIt.advance();
                    String arg = (String) fIt.key();
                    int freq = fIt.value();
                    s.append(pred+"\t"+caseStr+"\t"+arg+"\t"+freq+"\n");
                }
            }
        }
        return s.toString();
    }
}
