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

import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

import gnu.trove.iterator.*;
import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.hash.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import jp.ac.tohoku.ecei.cl.www.io.*;
import jp.ac.tohoku.ecei.cl.www.db.*;
import jp.ac.tohoku.ecei.cl.www.mapdb.*;

import org.mapdb.*;

public class KUCFDBCreatorSAXMapDB {
    public static boolean predClass = true;
    public static boolean argClass = true;

    public static void main (String[] args) {
        File kucfFile = new File(args[0]);
        String dbName = args[1];
        File kuCaseMappingFile = new File(args[2]);

        String dbFile = null;

        try {
            KeyValueDBManager dbMan = null;
            if (args.length >= 4) {
                dbFile = args[3];
                dbMan = new KeyValueDBManagerMapDB(dbFile);
            }

            BufferedInputStream cmis = new BufferedInputStream(new FileInputStream(kuCaseMappingFile));
            HashMap<String, String> caseMapping = PairParser.parse(cmis);

            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            SAXParser saxParser = saxParserFactory.newSAXParser();


            BufferedReader bis = new BufferedReader(new InputStreamReader(new FileInputStream(kucfFile), "utf-8"));
            InputSource inputSource = new InputSource(bis);
            System.err.println("Encoding: "+inputSource.getEncoding());
            inputSource.setEncoding("ja.JP.UTF-8");
            System.err.println("Encoding: "+inputSource.getEncoding());


            DB db = DBMaker.newFileDB(new File(dbName+".pcac.db"))
                .closeOnJvmShutdown()
                .make();
            Map<String, String> map = db.getTreeMap("");

            KUCFSAXMapDBHandler kucfSAXMapDBHandler = 
                new KUCFSAXMapDBHandler (map, caseMapping, dbMan,
                                         KUCFSAXMapDBHandler.PredMode.CLASS,
                                         KUCFSAXMapDBHandler.ArgMode.CLASS);
            // read xml file
            System.err.println("reading case frame file...");
            saxParser.parse(inputSource, kucfSAXMapDBHandler);
            db.commit();
            db.close();


            bis = new BufferedReader(new InputStreamReader(new FileInputStream(kucfFile), "utf-8"));
            inputSource = new InputSource(bis);
            inputSource.setEncoding("ja.JP.UTF-8");
            System.err.println("Encoding: "+inputSource.getEncoding());

            db = DBMaker.newFileDB(new File(dbName+".pcal.db"))
                .closeOnJvmShutdown()
                .make();
            map = db.getTreeMap("");

            kucfSAXMapDBHandler = new KUCFSAXMapDBHandler (map, caseMapping, dbMan,
                                                           KUCFSAXMapDBHandler.PredMode.CLASS,
                                                           KUCFSAXMapDBHandler.ArgMode.LEMMA);

            // read xml file
            System.err.println("reading case frame file...");
            saxParser.parse(inputSource, kucfSAXMapDBHandler);
            db.commit();
            db.close();


            bis = new BufferedReader(new InputStreamReader(new FileInputStream(kucfFile), "utf-8"));
            inputSource = new InputSource(bis);
            inputSource.setEncoding("ja.JP.UTF-8");
            System.err.println("Encoding: "+inputSource.getEncoding());

            db = DBMaker.newFileDB(new File(dbName+".plac.db"))
                .closeOnJvmShutdown()
                .make();
            map = db.getTreeMap("");

            kucfSAXMapDBHandler = new KUCFSAXMapDBHandler (map, caseMapping, dbMan,
                                                           KUCFSAXMapDBHandler.PredMode.LEMMA,
                                                           KUCFSAXMapDBHandler.ArgMode.CLASS);

            // read xml file
            System.err.println("reading case frame file...");
            saxParser.parse(inputSource, kucfSAXMapDBHandler);
            db.commit();
            db.close();

            /*
            bis = new BufferedReader(new InputStreamReader(new FileInputStream(kucfFile), "utf-8"));
            inputSource = new InputSource(bis);
            inputSource.setEncoding("ja.JP.UTF-8");
            System.err.println("Encoding: "+inputSource.getEncoding());

            db = DBMaker.newFileDB(new File(dbName+".db"))
                .closeOnJvmShutdown()
                .make();
            map = db.getTreeMap("");

            kucfSAXMapDBHandler = new KUCFSAXMapDBHandler (map, caseMapping, dbMan,
                                                           KUCFSAXMapDBHandler.PredMode.LEMMA,
                                                           KUCFSAXMapDBHandler.ArgMode.LEMMA);

            // read xml file
            System.err.println("reading case frame file...");
            saxParser.parse(inputSource, kucfSAXMapDBHandler);
            db.commit();
            db.close();
            */
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
