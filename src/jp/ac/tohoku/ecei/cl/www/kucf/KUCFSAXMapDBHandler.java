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

import jp.ac.tohoku.ecei.cl.www.db.*;
import org.mapdb.*;

public class KUCFSAXMapDBHandler extends DefaultHandler {

    HashMap<String, TObjectIntHashMap<String>> pcf; // case - <arg, freq>

    // entry attrs
    String headword = null;
    String predType = null;
    // caseframe attrs
    String id = null;
    // argument attrs
    String caseStr = null;
    String closest = null;
    // component attrs
    int frequency;

    // 
    StringBuilder text = null;
    ArrayList<String> textDivision = null;

    // flags
    boolean currentCaseFrameData = false;
    boolean currentEntry = false;
    boolean currentCaseFrame = false;
    boolean currentArgument  = false;
    boolean currentComponent = false;

    int count;
    Map<String, String> map;
    HashMap<String, String> caseMapping;
    KeyValueDBManager dbMan;
    public static enum PredMode {LEMMA, CLASS};
    public static enum ArgMode {LEMMA, CLASS};
    
    PredMode predMode = PredMode.LEMMA;
    ArgMode argMode = ArgMode.LEMMA;

    public KUCFSAXMapDBHandler (Map<String, String> map, HashMap<String, String> caseMapping,
                              KeyValueDBManager dbMan) {
        this (map, caseMapping, dbMan, PredMode.LEMMA, ArgMode.LEMMA);
    }
    
    public KUCFSAXMapDBHandler (Map<String, String> map, HashMap<String, String> caseMapping,
                              KeyValueDBManager dbMan, PredMode predMode, ArgMode argMode) {
        this.map = map;
        this.caseMapping = caseMapping;
        this.count = 0;
        this.dbMan = dbMan;
        this.predMode = predMode;
        this.argMode = argMode;
    }

    public void startDocument () {}

    private HashMap<String, String> parseAttributes (Attributes attrs) {
        HashMap<String, String> attrsHash = new HashMap<String, String>();
        for (int i = 0; i < attrs.getLength(); i++) {
            attrsHash.put(attrs.getQName(i), attrs.getValue(i));
        }
        return attrsHash;
    }

    public void startElement (String namespaceURI,
                              String localName,
                              String qName,
                              Attributes attrs) {
        // get attributes
        HashMap<String, String> attrsHash = parseAttributes(attrs);
        //System.err.println("qName="+qName);
        if (qName.equals("caseframedata")) {
            currentCaseFrameData = true;
        } else if (qName.equals("entry")) {
            currentEntry = true;
            headword = attrsHash.get("headword");
            predType = attrsHash.get("predtype");
            pcf = new HashMap<String, TObjectIntHashMap<String>>();
            //ArrayList<getKeys(headword);
        } else if (qName.equals("caseframe")) {
            currentCaseFrame = true;
            String id = attrsHash.get("id");
        } else if (qName.equals("argument")) {
            currentArgument = true;
            caseStr = attrsHash.get("case");
            if (caseMapping.containsKey(caseStr)) {
                caseStr = caseMapping.get(caseStr);
            } else {
                caseStr = null;
            }
            closest = attrsHash.get("closest");
        } else if (qName.equals("component")) {
            currentComponent = true;
            frequency = Integer.parseInt(attrsHash.get("frequency"));
        } 
    }

    public void characters (char[] ch, int start, int length) {
        if (currentComponent) {
            if (text == null) {
                text = new StringBuilder();
            }
            text.append(new String(ch, start, length));
            //System.err.println("text="+text.toString());
        }
    }

    public void endElement (String namespaceURI,
                            String localName,
                            String qName) {
        if (qName.equals("entry")) {
            ArrayList<String> headwordKeys = getKeys(headword);
            // write freqinfo to db
            for (int i = 0; i < headwordKeys.size(); i++) {
                String pred = headwordKeys.get(i);
                if (this.predMode == PredMode.CLASS && this.dbMan != null) {
                    String value = this.dbMan.getValue(pred);
                    if (value == null) { continue; }
                    pred = value;
                }
                for (Iterator it = pcf.keySet().iterator(); it.hasNext();) {
                    String cs = (String) it.next();
                    TObjectIntHashMap<String> fh = pcf.get(cs);
                    int freqTotal = 0;
                    for (TObjectIntIterator oii = fh.iterator(); oii.hasNext();) {
                        oii.advance();
                        String arg = (String) oii.key();
                        if (this.argMode == ArgMode.CLASS && this.dbMan != null) {
                            String value = this.dbMan.getValue(arg);
                            if (value == null) { continue; }
                            arg = value;
                        }
                        int freq = oii.value();
                        freqTotal += freq;
                        String key = pred+"/"+cs+"/"+arg;
                        String val = map.get(key);
                        //System.err.println("key="+key+" val="+val);
                        if (val != null) {
                            int newVal = Integer.parseInt(val)+freq;
                            map.put(key, newVal+"");
                            System.err.println("key="+key+" val="+val+" freq="+freq+" newVal="+newVal);
                        } else {
                            map.put(key, freq+"");
                            System.err.println("key="+key+" pred="+pred+" arg="+arg+" freq="+freq);
                        }
                        //System.err.println(pred+"/"+cs+"/"+arg+"\t"+freq);
                    }
                    String key = pred+"/"+cs;
                    String val = map.get(key);
                    if (val != null) {
                        map.put(key, Integer.parseInt(val)+freqTotal+"");
                    } else {
                        map.put(key, freqTotal+"");
                    }
                    //kucfDB.set(pred+"/"+cs, freqTotal+"");
                }
            }
            pcf = null;
            this.count++;
            if (count % 50 == 0) {
                System.err.print(".");
            }
            if (count % 1000 == 0) {
                System.err.print(" "+count+"\n");
            }
            currentEntry = false;

            // init global fields	    
            headword = null;
            predType = null;
            // caseframe attrs
            id = null;
            // argument attrs
            caseStr = null;
            closest = null;
            // component attrs
            frequency = 0;
            text = null;
        } else if (qName.equals("caseframe")) {
            /*
              String idAll = id;
              String[] e = idAll.split(":");
              // e[0]
              ArrayList<String> keys = getKeys(e[0]);
              String[] e2 = e[0].split("/");
              id = e2[0];
            */
            currentCaseFrame = false;
        } else if (qName.equals("argument")) {
            //if (!pcf.containsKey(caseStr)) {
            //pcf.put(new HashMap<String, TObjectIntHashMap<String>>());
            //}
            //HashMap<String, TObjectIntHashMap<String>> freqHash = pcf.get(caseStr);
            //pcf.put(caseStr, freqHash);
            currentArgument = false;
        } else if (qName.equals("component")) {
            if (caseStr != null) {
                ArrayList<String> textKeys = getKeys(text.toString());
                if (!pcf.containsKey(caseStr)) {
                    pcf.put(caseStr, new TObjectIntHashMap<String>());
                }
                TObjectIntHashMap<String> freqHash = pcf.get(caseStr);
                for (int i = 0; i < textKeys.size(); i++) {
                    if (freqHash.containsKey(textKeys.get(i))) {
                        freqHash.put(textKeys.get(i), freqHash.get(textKeys.get(i))+frequency);
                    } else {
                        freqHash.put(textKeys.get(i), frequency);
                    }
                }
            }
            currentComponent = false;
        } 
        text = new StringBuilder();
    }
	
    private ArrayList<String> getKeys (String line) {
        ArrayList<String> currentKeys = new ArrayList<String>();
        String[] e1 = line.split("\\+");
        for (int j = 0; j < e1.length; j++) {
            String[] e2 = e1[j].split("\\?");
            ArrayList<String> morphs = new ArrayList<String>();
            for (int k = 0; k < e2.length; k++) {
                String[] e3 = e1[j].split("/");
                morphs.add(e3[0]);
            }
            if (currentKeys.size() == 0) {
                currentKeys.addAll(morphs);
            } else {
                ArrayList<String> newCurrentKeys = new ArrayList<String>();
                for (int k = 0; k < currentKeys.size(); k++) {
                    for (int l = 0; l < morphs.size(); l++) {
                        //newCurrentKeys.add(currentKeys.get(k)+"+"+morphs.get(l));
                        newCurrentKeys.add(currentKeys.get(k)+morphs.get(l));
                    }
                }
                currentKeys = newCurrentKeys;
            }
        }
        return currentKeys;
    }
}
