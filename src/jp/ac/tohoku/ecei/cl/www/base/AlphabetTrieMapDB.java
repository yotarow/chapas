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

package jp.ac.tohoku.ecei.cl.www.base;

import java.io.*;
import java.util.*;

import org.mapdb.*;

import jp.ac.tohoku.ecei.cl.www.util.Timer;

public class AlphabetTrieMapDB extends AlphabetTrie implements Serializable {

    int numEntries;
    boolean growthStopped = false;
    String dbName;
    DB db;
    Map<String, String> map; 

    public AlphabetTrieMapDB () {
        super(0);
        numEntries = 0;
    }
    
    public void createDB (String path) throws IOException {
        this.db = DBMaker.newFileDB(new File(path))
            .closeOnJvmShutdown()
            .make();
        //this.map = db.getHashMap("");
        this.map = db.getTreeMap("");
    }

    public void createDB (String path, String name) throws IOException {
        this.db = DBMaker.newFileDB(new File(path))
            .closeOnJvmShutdown()
            .make();
        this.map = db.getTreeMap(name);
    }

    public void loadDB (String path) throws IOException {
        this.db = DBMaker.newFileDB(new File(path)).make();
        //this.map = db.getHashMap("");
        this.map = db.getTreeMap("");
    }

    public void loadDB (String path, String name) throws IOException {
        this.db = DBMaker.newFileDB(new File(path)).make();
        this.map = db.getTreeMap(name);
    }

    /** Return -1 if entry isn't present. */
    public int lookupIndex (String entry, boolean addIfNotPresent) {
        if (entry == null) {
            throw new IllegalArgumentException ("Can't lookup \"null\" in an AlphabetTrie.");
        }
        int ret = -1;
        String retStr = this.map.get(entry);
        if (retStr == null) {
            if (addIfNotPresent && !growthStopped) {
                ret = ++numEntries;
                this.map.put (entry, ret+"");
            } else {
                // do nothing
            }
        } else {
            ret = Integer.parseInt(retStr);
        }
        return ret;
    }

    public Object[] toArray () {
        if (this.db == null) { return null; }
        Object[] array = new Object[numEntries];
        int cnt = 0;
        for (Iterator<String> it = this.map.keySet().iterator(); it.hasNext();) {
            array[cnt] = it.next();
            cnt++;
        }
        return array;
    }

    public boolean contains (Object entry) {
        return this.map.containsKey((String) entry);
    }

    public void stopGrowth () {
        growthStopped = true;
    }

    public void allowGrowth () {
        growthStopped = false;
    }

    public boolean growthStopped () {
        return growthStopped;
    }
    
    public void close() throws IOException {
        this.db.commit();
        this.db.close();
    }
}
