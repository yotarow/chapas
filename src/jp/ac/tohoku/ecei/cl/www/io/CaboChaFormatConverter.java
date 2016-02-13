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

package jp.ac.tohoku.ecei.cl.www.io;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.net.URL;

import jp.ac.tohoku.ecei.cl.www.base.*;
import jp.ac.tohoku.ecei.cl.www.io.*;
import jp.ac.tohoku.ecei.cl.www.util.*;

import gnu.trove.iterator.*;
import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.hash.*;

public class CaboChaFormatConverter {

    public static CaboChaFormat inputFormat  = CaboChaFormat.OLD;
    public static CaboChaFormat outputFormat = CaboChaFormat.NEW;

    public static void main (String[] args) {
        processArguments(args);

        try {
            Timer timer = new Timer();
            timer.start();
            CaboCha2Dep pipe = new CaboCha2Dep(System.in);
            pipe.setFormat(inputFormat);
            
            JapaneseDependencyTree2CaboCha caboChaOutPipe = new JapaneseDependencyTree2CaboCha();
            caboChaOutPipe.setFormat(outputFormat);

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out, "utf-8"));
            int cnt = 0;
            while (!pipe.eof()) {
                DependencyTree tree = pipe.pipePerSentence();
                if (tree == null) { continue; }
                writer.write(caboChaOutPipe.pipePerSentence(tree));
            }
            timer.stop();
            writer.close();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processArguments(String[] args) {
        int idx = 0;
        while (idx < args.length) {
            if(args[idx].equals("-I")) {
                idx++;
                String val = args[idx];
                if (val.equalsIgnoreCase("OLD")) {
                    inputFormat = CaboChaFormat.OLD;
                } else if (val.equalsIgnoreCase("NEW")) {
                    inputFormat = CaboChaFormat.NEW;
                }
            } else if(args[idx].equals("-O")) {
                idx++;
                String val = args[idx];
                if (val.equalsIgnoreCase("OLD")) {
                    outputFormat = CaboChaFormat.OLD;
                } else if (val.equalsIgnoreCase("NEW")) {
                    outputFormat = CaboChaFormat.NEW;
                }
            } else {
                idx++;
            }
        }
    }

}
