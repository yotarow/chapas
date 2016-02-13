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

import java.io.*;
import java.util.*;

import org.mapdb.*;
import gnu.trove.map.hash.*;
import org.apache.commons.io.*;

public class KUCFManagerMapDB implements KUCFManager {

    private DB db;
    private Map<String, String> map;

    TObjectIntHashMap<String> dbHash;
    private boolean storeOnMemory;

    public KUCFManagerMapDB (String dbName) throws Exception {
        this (dbName, false);
    }

    public KUCFManagerMapDB (String dbName, boolean storeOnMemory) throws Exception {
        this.db = DBMaker.newFileDB(new File(dbName))
            .closeOnJvmShutdown()
            .make();

        // open existing an collection (or create new)
        this.map = db.getTreeMap("");
        //this.db.open(dbName, DB.OREADER);
        //if (storeOnMemory) {
        //this.dbHash = new TObjectIntHashMap<String>();
        //Map<String, Object> all = db.getAll();
        //for (Iterator<String> it = all.keySet().iterator(); it.hasNext();) {
        //String key = it.next();
        //int val = Integer.parseInt((String) all.get(key));
        //this.dbHash.put(key, val);
        //}
        //db.close();
        //}
    }

    public int getFrequency(String pred, String caseStr, String arg) {
        int freq = 0;
        /*
        if (storeOnMemory) {
            freq = this.dbHash.get(pred+"/"+caseStr+"/"+arg);
            if (freq == -1) {
                freq = 0;
            }
        } else {
        */
        try {
            String value = this.map.get(pred+"/"+caseStr+"/"+arg);
            //IOUtils.write("[KUCFManagerMapDB] "+pred+"/"+caseStr+"/"+arg+":"+value+"\n", System.err, "utf-8");
            if (value == null) {
                return freq;
            } else {
                freq = Integer.parseInt(value);
                if (freq == -1) {
                    freq = 0;
                }
            }
            return freq;
        } catch (Exception e) {
            e.printStackTrace();
        }
        //}
        return freq;
    }

    public int getFrequency(String pred, String caseStr) {
        int freq = 0;
        /*
        if (storeOnMemory) {
            freq = this.dbHash.get(pred+"/"+caseStr);
            if (freq == -1) {
                freq = 0;
            }
        } else {
        */
        try {
            String value = this.map.get(pred+"/"+caseStr);
            if (value == null) {
                return freq;
            } else {
                freq = Integer.parseInt(value);
                if (freq == -1) {
                    freq = 0;
                }
            }
            return freq;
        } catch (Exception e) {
            e.printStackTrace();
        }
        //}
        return freq;
    }
}
