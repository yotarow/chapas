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

package jp.ac.tohoku.ecei.cl.www.mapdb;

import java.io.*;
import java.util.*;
import org.mapdb.*;
import jp.ac.tohoku.ecei.cl.www.db.*;

public class KeyValueDBPrinter {

    private DB db;
    private Map<String, Object> map;

    public KeyValueDBPrinter (String dbName) throws Exception {
        this (dbName, false);
    }

    public KeyValueDBPrinter (String dbName, boolean storeOnMemory) throws Exception {
        this.db = DBMaker.newFileDB(new File(dbName))
            .closeOnJvmShutdown()
            .make();
        this.map = db.getTreeMap("");
    }

    public void print () {
        for (Iterator it = this.map.keySet().iterator(); it.hasNext();) {
            String key = (String) it.next();
            System.err.println(key+":"+this.map.get(key));
        }
    }
    
    public static void main (String[] args) throws Exception {
        String dbFile = args[0];
        KeyValueDBPrinter printer = new KeyValueDBPrinter(dbFile);
        printer.print();
    }
}
