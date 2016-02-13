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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import javax.xml.parsers.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import gnu.trove.iterator.*;
import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.hash.*;

public class KUCFParserDOM {

    public KUCFParserDOM () {}
    
    public HashMap<String, String> parseAttrs (NamedNodeMap nm) {
        HashMap<String, String> attrHash = new HashMap<String, String>();
        for (int k = 0; k < nm.getLength(); k++) {
            Node attr = nm.item(k);
            attrHash.put(attr.getNodeName(), attr.getNodeValue());
        }
        return attrHash;
    }

    public String getText (Element e) {
        NodeList l = e.getChildNodes();
        String t = null;
        for (int i = 0; i < l.getLength(); i++) {
            Node n = l.item(i);
            if (n.getNodeType() != Node.TEXT_NODE) { continue; }
            t = n.getNodeValue();
        }
        return t;
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
                        newCurrentKeys.add(currentKeys.get(k)+"+"+morphs.get(l));
                    }
                }
                currentKeys = newCurrentKeys;
            }
        }
        return currentKeys;
    }

    public TObjectIntHashMap<String> parseArgumentElement (Element aEl) {
        NodeList cmpNodeList = aEl.getElementsByTagName("component");
        TObjectIntHashMap<String> freqHash = new TObjectIntHashMap<String>();
        for (int i = 0; i < cmpNodeList.getLength(); i++) {
            Node cmpNode = cmpNodeList.item(i);
            if (cmpNode.getNodeType() != Node.ELEMENT_NODE) { continue; }
            Element cmpEl = (Element) cmpNode;
            HashMap<String, String> attrsHash = parseAttrs(cmpEl.getAttributes());
            int freq = -1;
            if (attrsHash.containsKey("frequency")) {
                freq = Integer.parseInt(attrsHash.get("frequency"));
            }

            String text = getText(cmpEl);
            ArrayList<String> keys = getKeys(text);
            for (int j = 0; j < keys.size(); j++) {
                if (freqHash.containsKey(keys.get(j))) { continue; }
                freqHash.put(keys.get(j), freq);
            }
        }
        return freqHash;
    }

    // returns hash contains case - <arg, frequency> pairs
    public HashMap<String, TObjectIntHashMap<String>> parseEntry (Element entry) {

        HashMap<String, TObjectIntHashMap<String>> entriesAllHash = new HashMap<String, TObjectIntHashMap<String>>();

        NodeList cfNodeList = entry.getElementsByTagName("caseframe");

        for (int i = 0; i < cfNodeList.getLength(); i++) {
            Node cfNode = cfNodeList.item(i);
            if (cfNode.getNodeType() != Node.ELEMENT_NODE) { continue; }
            Element cfEl = (Element) cfNode;
            // caseframe attrs
            String id = null;
            HashMap<String, String> attrsHash = parseAttrs(cfEl.getAttributes());
	    
            if (attrsHash.containsKey("id")) {
                String idAll = attrsHash.get("id");
                String[] e = idAll.split(":");
                String[] e2 = e[0].split("/");
                id = e2[0];
            }

            NodeList argNodeList = cfEl.getElementsByTagName("argument");
            for (int j = 0; j < argNodeList.getLength(); j++) {
                Node argNode = argNodeList.item(j);
                if (argNode.getNodeType() != Node.ELEMENT_NODE) { continue; }
                Element argEl = (Element) argNode;
                HashMap<String, String> argAttrsHash = parseAttrs(argEl.getAttributes());
                String caseStr = null;
                int closest = -1;
                if (argAttrsHash.containsKey("case")) {
                    caseStr = argAttrsHash.get("case");
                } else if (argAttrsHash.containsKey("closest")) {
                    closest = Integer.parseInt(argAttrsHash.get("closest"));
                }
                TObjectIntHashMap<String> freqHash = parseArgumentElement(argEl);

                if (entriesAllHash.containsKey(caseStr)) {
                    TObjectIntHashMap<String> existingFreqHash = entriesAllHash.get(caseStr);
                    for (TObjectIntIterator it = freqHash.iterator(); it.hasNext();) {
                        it.advance();
                        String key = (String) it.key();
                        int value = it.value();
                        if (existingFreqHash.containsKey(key)) {
                            existingFreqHash.put(key, existingFreqHash.get(key)+value);
                        } else {
                            existingFreqHash.put(key, value);
                        }
                    }
                } else {
                    entriesAllHash.put(caseStr, freqHash);
                }
            }
        }
        return entriesAllHash;
    }

    public KUCF parseXMLAllAsKUCFObject (InputStream is) throws Exception {
        return new KUCF(this.parseXMLAll(is));
    }

    public HashMap<String, HashMap<String, TObjectIntHashMap<String>>> parseXMLAll (InputStream is) throws Exception {
	
        HashMap<String, HashMap<String, TObjectIntHashMap<String>>> cfHash = new HashMap<String, HashMap<String, TObjectIntHashMap<String>>>();

        System.err.println("reading case frame file...");
        Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
        Element root = d.getDocumentElement();
	
        NodeList entryList = root.getElementsByTagName("entry");

        System.err.println("creating case frame object...");
        for (int i = 0; i < entryList.getLength(); i++) {
            Node entryNode = entryList.item(i);
            if (entryNode.getNodeType() != Node.ELEMENT_NODE) { continue; }
            Element entryEl = (Element) entryNode;
            HashMap<String, String> attrsHash = parseAttrs(entryEl.getAttributes());
            String head = null;
            String predType = null;
            String voice = null;
            if (attrsHash.containsKey("headword")) {
                head = attrsHash.get("headword");
            } else if (attrsHash.containsKey("predtype")) {
                predType = attrsHash.get("predtype");
            } else if (attrsHash.containsKey("voice")) {
                voice = attrsHash.get("voice");
            }
	    
            // use only active voice predicates !!!!!!!!!!!!!!!
            if (voice != null) {
                continue;
            } 

            ArrayList<String> headKeys = getKeys(head);

            // key: currentKeys
            // this contains contains <case, <arg, frequency>> pairs
            HashMap<String, TObjectIntHashMap<String>> entriesAllHash = parseEntry(entryEl);

            // create key-value pairs
            for (int j = 0; j < headKeys.size(); j++) {
                String key = headKeys.get(j);
                if (cfHash.containsKey(key)) { continue; }
                cfHash.put(key, entriesAllHash);
            }
            if ((i+1) % 50 == 0) {
                System.err.print(".");
            } 
            if ((i+1) % 1000 == 0) {
                System.err.print(" "+(i+1)+"\n");
            } 
        }
        return cfHash;
        //KUCF kucf = new KUCF(cfHash);
        //System.err.println(kucf.toString());
        //return kucf;
    }
}
