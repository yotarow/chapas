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

public class PLSICooccurenceInfoDBCreatorMapDB {

    private static String pzDBName = "pz.db";
    private static String pnzDBName = "pnz.db";
    private static String pnDBName = "pn.db";
    private static String pcvzDBName = "pcvz.db";
    private static String pcvDBName = "pcv.db";

    public static void main( String[] args ) {

        String pzFile = args[0];
        String pnzFile = args[1];
        String pcvzFile = args[2];

        String dir = args[3];
        
        double[] pZ = new double[1000];
        try {
            
            {
                System.err.println("creating "+dir+"/"+pzDBName);
                DB pzDB = DBMaker.newFileDB(new File(dir+"/"+pzDBName))
                    .closeOnJvmShutdown()
                    .make();
                Map<String, String> pzDBMap = pzDB.getTreeMap("");

                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(pzFile), "UTF-8"));
                String line;
                StringBuilder pzStr = new StringBuilder();
                int cnt = 0;
                while ((line = reader.readLine()) != null) {
                    pzStr.append(line+" ");
                    pZ[cnt] = Double.parseDouble(line);
                    cnt++;
                }
                pzDBMap.put("pz", pzStr.toString());
                pzDB.commit();
                pzDB.close();
                reader.close();
            }
	    
            //-------------------------------------------------------------------------------
            {
                System.err.println("creating "+dir+"/"+pnzDBName);
                DB pnzDB = DBMaker.newFileDB(new File(dir+"/"+pnzDBName))
                    .closeOnJvmShutdown()
                    .make();
                Map<String, String> pnzDBMap = pnzDB.getTreeMap("");
                DB pnDB = DBMaker.newFileDB(new File(dir+"/"+pnDBName))
                    .closeOnJvmShutdown()
                    .make();
                Map<String, String> pnDBMap = pnDB.getTreeMap("");

                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(pnzFile), "UTF-8"));
                String line;
                int cnt = 0;
                while ((line = reader.readLine()) != null) {
                    String[] e1 = line.split("\t");
                    String nStr = e1[0];
                    String[] e = e1[1].split(" ");
                    StringBuilder joinedValueStr = new StringBuilder();
                    //ArrayList<String> zProbsAry = new ArrayList<String>();
                    double pn = 0.0;
                    for (int i = 0; i < e.length; i++) {
                        String[] e2 = e[i].split(":");
                        int d = Integer.parseInt(e2[0]);
                        //zProbsAry.add(e2[1]);
                        double pnGivenZi = Double.parseDouble(e2[1]);
                        pn += pnGivenZi*pZ[i];
                        joinedValueStr.append(e2[1]);
                        if (i != e.length-1) {
                            joinedValueStr.append(" ");
                        }
                    }

                    pnzDBMap.put(nStr, joinedValueStr.toString());
                    pnDBMap.put(nStr, pn+"");
                    cnt++;
                    if (cnt % 1000 == 0) {
                        System.err.print(".");
                    }
                    if (cnt % 20000 == 0) {
                        pnzDB.commit();
                        pnDB.commit();
                    }
                    if (cnt % 20000 == 0) {
                        System.err.print(" "+cnt+"\n");

                    }
                }
                pnzDB.commit();
                pnzDB.close();
                pnDB.commit();
                pnDB.close();
            }
            System.err.print("\n");

            //-------------------------------------------------------------------------------
            {
                System.err.println("creating "+dir+"/"+pcvDBName);
                DB pcvzDB = DBMaker.newFileDB(new File(dir+"/"+pcvzDBName))
                    .closeOnJvmShutdown()
                    .make();
                Map<String, String> pcvzDBMap = pcvzDB.getTreeMap("");
                DB pcvDB = DBMaker.newFileDB(new File(dir+"/"+pcvDBName))
                    .closeOnJvmShutdown()
                    .make();
                Map<String, String> pcvDBMap = pcvDB.getTreeMap("");

                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(pcvzFile), "UTF-8"));
                String line;
                int cnt = 0;
                while ((line = reader.readLine()) != null) {
                    String[] e1 = line.split("\t");
                    String nStr = e1[0];
                    String[] e = e1[1].split(" ");
                    StringBuilder joinedValueStr = new StringBuilder();
                    //ArrayList<String> zProbsAry = new ArrayList<String>();
                    double pCv = 0.0;
                    for (int i = 0; i < e.length; i++) {
                        String[] e2 = e[i].split(":");
                        int d = Integer.parseInt(e2[0]);
                        double pcvGivenZi = Double.parseDouble(e2[1]);
                        pCv += pcvGivenZi*pZ[i];
                        joinedValueStr.append(e2[1]);
                        if (i != e.length-1) {
                            joinedValueStr.append(" ");
                        }
                    }
                    pcvzDBMap.put(nStr, joinedValueStr.toString());
                    pcvDBMap.put(nStr, pCv+"");
                    cnt++;
                    if (cnt % 1000 == 0) {
                        System.err.print(".");
                    }
                    if (cnt % 20000 == 0) {
                        pcvzDB.commit();
                        pcvDB.commit();

                    }
                    if (cnt % 20000 == 0) {
                        System.err.print(" "+cnt+"\n");

                    }
                }
                pcvzDB.commit();
                pcvzDB.close();
                pcvDB.commit();
                pcvDB.close();
                System.err.print("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
