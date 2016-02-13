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

import java.io.*;
import java.util.*;
import org.mapdb.*;

import jp.ac.tohoku.ecei.cl.www.util.*;

import gnu.trove.iterator.*;
import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.hash.*;

public class PLSICooccurenceInfoManagerMapDB implements PLSICooccurenceInfoManager {

    //private static final boolean USE_PN_PCV = false;

    private static double[] pz;
    private static HashMap<String, double[]> pnzHash;
    private static HashMap<String, double[]> pcvzHash;
    //private static TObjectDoubleHashMap<String> pnHash;
    //private static TObjectDoubleHashMap<String> pcvHash;
	
    private static PLSICooccurenceInfoManagerMapDB instance = null;
    private static boolean storeCoocOnMemory;

    private static DB pnzDB;
    private static DB pnDB;
    private static DB pcvzDB;
    private static DB pcvDB;
    private static DB pzDB;

    private static Map<String, String> pnzDBMap;
    private static Map<String, String> pnDBMap;
    private static Map<String, String> pzDBMap;
    private static Map<String, String> pcvzDBMap;
    private static Map<String, String> pcvDBMap;

    private PLSICooccurenceInfoManagerMapDB (String pzDBName, String pnzDBName, 
                                             String pcvzDBName) {
        this (pzDBName, pnzDBName, pcvzDBName, false);
    }

    private PLSICooccurenceInfoManagerMapDB (String pzDBName, String pnzDBName, 
                                             String pcvzDBName, boolean storeCoocOnMemory) {

        try {
            this.pzDB = DBMaker.newFileDB(new File(pzDBName))
                .closeOnJvmShutdown()
                .make();
            this.pzDBMap = pzDB.getTreeMap("");

            this.pnzDB = DBMaker.newFileDB(new File(pnzDBName))
                .closeOnJvmShutdown()
                .make();
            this.pnzDBMap = pnzDB.getTreeMap("");

            this.pcvzDB = DBMaker.newFileDB(new File(pcvzDBName))
                .closeOnJvmShutdown()
                .make();
            this.pcvzDBMap = pcvzDB.getTreeMap("");

            // load pz ary
            String pzStr = pzDBMap.get("pz");
            String[] pzAryStr = pzStr.split(" ");
            pz = new double[pzAryStr.length];
            for (int i = 0; i < pz.length; i++) {
                pz[i] = Double.parseDouble(pzAryStr[i]);
            }
            pzDB.close();
            /*
            if (storeCoocOnMemory) {
                // store all entries in memory
                pnzHash = new HashMap<String, double[]>();
                //pnHash = new TObjectDoubleHashMap<String>();
	
                Map<String, Object> pnzDBAll = pnzDB.getAll();
                for (Iterator<String> it = pnzDBAll.keySet().iterator(); it.hasNext();) {
                    String key = it.next();
                    String value = (String) pnzDBAll.get(key);
                    String[] valuesStr = value.split(" ");
                    double[] values = new double[valuesStr.length];
                    for (int i = 0; i < valuesStr.length; i++) {
                        values[i] = Double.parseDouble(valuesStr[i]);
                    }
                    pnzHash.put(key, values);
                }
                pnzDB.close();

                pcvzHash = new HashMap<String, double[]>();
		
                Map<String, Object> pcvzDBAll = pcvzDB.getAll();
                for (Iterator<String> it = pcvzDBAll.keySet().iterator(); it.hasNext();) {
                    String key = it.next();
                    String value = (String) pcvzDBAll.get(key);
                    String[] valuesStr = value.split(" ");
                    double[] values = new double[valuesStr.length];
                    for (int i = 0; i < values.length; i++) {
                        values[i] = Double.parseDouble(valuesStr[i]);
                    }
                    pcvzHash.put(key, values);
                }
                pcvzDB.close();
            } 
            */
            //timer.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static PLSICooccurenceInfoManagerMapDB getInstance(String pzDBName, String pnzDBName, 
                                                           String pcvzDBName) {
        return getInstance(pzDBName, pnzDBName, pcvzDBName, false);
    }

    public static PLSICooccurenceInfoManagerMapDB getInstance(String pzDBName, String pnzDBName, 
                                                           String pcvzDBName,  boolean storeCoocOnMemory) {
        if (instance == null) {
            instance = new PLSICooccurenceInfoManagerMapDB(pzDBName, pnzDBName, pcvzDBName, storeCoocOnMemory);
        }
        return instance;
    }

    public double getMI(String cv, String n) throws IOException {
        return getMI (cv, n, false);
    }

    public double getMI(String cv, String n, boolean normalized) throws IOException {

        double jointProb = 0.0;
        double cvPriorProb = 0.0;
        double nPriorProb = 0.0;
        double[] cvzProbs = null;
        double[] nzProbs = null;

        cvzProbs = getCVZ(cv);
        nzProbs = getNZ(n);
        //System.err.println("cvzProbs==null?"+(cvzProbs==null)+" nzProbs==null?"+(nzProbs==null));
        if (cvzProbs == null || nzProbs == null) { 
            return 0.0;
        }
        
        for (int i = 0; i < pz.length; i++) {
            jointProb += pz[i]*cvzProbs[i]*nzProbs[i];
            //if (!USE_PN_PCV) {
            nPriorProb += pz[i]*nzProbs[i];
            cvPriorProb += pz[i]*cvzProbs[i];
            //}
        }
        if (normalized) {
            double val = (Math.log(jointProb)-Math.log(nPriorProb)-Math.log(cvPriorProb)) / (-Math.log(jointProb));
            if (Double.isNaN(val)) {
                val = 0.0;
            } 
            return val;
        } else {
            return Math.log(jointProb)-Math.log(nPriorProb)-Math.log(cvPriorProb);
        } 
    }
    
    public double getCVPrior(String cv) throws IOException {
        double cvPriorProb = 0.0;
        //if (USE_PN_PCV) {
            //System.err.println("CV HOGEHOGE");
        //cvPriorProb = getCV(cv);
        //} else {
        double[] cvzProbs = null;
        cvzProbs = getCVZ(cv);
        
        if (cvzProbs == null) { 
            return 0.0;
        }
        for (int i = 0; i < pz.length; i++) {
            cvPriorProb += pz[i]*cvzProbs[i];
        }
        return cvPriorProb;
    }
    /*
    public double getNPrior(String n) throws IOException {
        double nPriorProb = 0.0;
        if (USE_PN_PCV) {
            //System.err.println("N HOGEHOGE");
            nPriorProb = getN(n);
        } else {
            double[] nzProbs = getNZ(n);
            if (nzProbs == null) { 
                return 0.0;
            }
            for (int i = 0; i < pz.length; i++) {
                nPriorProb += pz[i]*nzProbs[i];
            }
        }
        return nPriorProb;
    }
    */

    public synchronized double[] getNZ(String n) throws IOException {
        if (this.storeCoocOnMemory) {
            if (!pnzHash.containsKey(n)) {
                return null;
            } else {
                return pnzHash.get(n);
            }
        } else {
            return convertDouble(pnzDBMap.get(n));
        }
    }

    private double[] convertDouble (String valueStr) {
        if (valueStr == null) { return null; }
        String[] valuesStr = valueStr.split(" ");
        double[] values = new double[valuesStr.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = Double.parseDouble(valuesStr[i]);
        }
        return values;
    }

    public double[] getCVZ(String c, String v) throws IOException {
        return getCVZ(c+"|"+v);
    }

    public synchronized double[] getCVZ(String cv) throws IOException {
        if (this.storeCoocOnMemory) {
            if (!pcvzHash.containsKey(cv)) {
                return null;
            } else {
                return pcvzHash.get(cv);
            }
        } else {
            //System.err.println("pcvzDBMap == null ? "+(pcvzDBMap == null)+"cv == null ? "+(cv == null));
            return convertDouble(pcvzDBMap.get(cv));
        }
    }
    /*
    public synchronized double getCV (String cv) throws IOException {
        if (this.storeCoocOnMemory) {
            if (!pcvHash.containsKey(cv)) {
                return 0.0;
            } else {
                return pcvHash.get(cv);
            }
        } else {
            return Double.parseDouble(pcvDB.get(cv));
        }
    }
    */
    /*
    public synchronized double getN (String n) throws IOException {
        if (this.storeCoocOnMemory) {
            if (!pnHash.containsKey(n)) {
                return 0.0;
            } else {
                return pnHash.get(n);
            }
        } else {
            return Double.parseDouble(pnDB.get(n));
        }
    }
    */
}
