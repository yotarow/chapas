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

package jp.ac.tohoku.ecei.cl.www.coord;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;

//import liblinear.*;
import de.bwaldvogel.liblinear.*;
import org.kohsuke.args4j.*;

import jp.ac.tohoku.ecei.cl.www.base.*;
import jp.ac.tohoku.ecei.cl.www.io.*;
import jp.ac.tohoku.ecei.cl.www.util.*;
import jp.ac.tohoku.ecei.cl.www.kucf.*;
import jp.ac.tohoku.ecei.cl.www.db.*;
import jp.ac.tohoku.ecei.cl.www.mapdb.*;
import jp.ac.tohoku.ecei.cl.www.liblinear.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CoordinationAnalyzer {

    private static Log log = LogFactory.getLog(CoordinationAnalyzer.class);

    private static final int FEATURE_FREQ_THRESHOLD = 1;

    private Model model;

    private AlphabetTrie alp; // feature info
    private Alphabet la; // label alphabet for global features
    private Label trueLabel; // label for global features
    private Label falseLabel; // label for global features

    private HashSet<String> funcPat;
    private HashSet<String> headPat;
    private HashSet<String> particlePat;
    private HashMap<String, String> config;

    private CoordinationAnalyzerOptions options = null;

    public CoordinationAnalyzer (CoordinationAnalyzerOptions options) throws FileNotFoundException, IOException {
        this.options = options;
        this.init();
    }

    private void init () throws FileNotFoundException, IOException {
        ClassLoader cl = this.getClass().getClassLoader();
        this.config = PairParser.parse(cl.getResourceAsStream(options.configFileName));
        log.debug("reading pattern files...");
        this.funcPat = PatternFileParser.parse(cl.getResourceAsStream(this.config.get("FUNCPAT")));
        this.headPat = PatternFileParser.parse(cl.getResourceAsStream(this.config.get("HEADPAT")));
        this.particlePat = PatternFileParser.parse(cl.getResourceAsStream(this.config.get("PARTICLEPAT")));
    }

    public void learn (String trainFileStr, String trainDirStr) throws IOException {
        this.learn (trainFileStr, trainDirStr, new AlphabetTrie());
    }

    public void learn (String trainFileStr, String trainDirStr, AlphabetTrie alphabet) throws IOException {

        this.alp = alphabet;
        
        this.la = new Alphabet();
        //this.trueLabel = new Label(0, la, la.lookupIndex("TRUE", true)); 
        //this.falseLabel = new Label(0, la, la.lookupIndex("FALSE", true)); 

        ArrayList<DependencyTree> treesAry = new ArrayList<DependencyTree>();
        
        if (trainFileStr != null) {
            log.info("reading "+trainFileStr);
            File trainFile = new File(trainFileStr);
            CaboCha2Dep pipe = new CaboCha2Dep(new FileInputStream(trainFile));
            //pipe.setFormat(options.inputFormat);
            while (!pipe.eof()) {
                int articleIdx = 0;
                DependencyTree tree = pipe.pipePerSentence();
                if (tree != null) {
                    treesAry.add(tree);
                }
            }
        } else if (trainDirStr != null) {
            File trainDir = new File(trainDirStr);
            File[] trainFiles = trainDir.listFiles();
            for (int i = 0; i < trainFiles.length; i++) {
                if (trainFiles[i].toString().endsWith("~")) { continue; }
                log.info("reading "+trainFiles[i]);
                CaboCha2Dep pipe = new CaboCha2Dep(new FileInputStream(trainFiles[i]));
                while (!pipe.eof()) {
                    DependencyTree tree = pipe.pipePerSentence();
                    if (tree != null) {
                        treesAry.add(tree);
                    }
                }
            }
        }
        
        for (DependencyTree tree : treesAry) {
            JapaneseDependencyTreeLib.annotateWordLevelDependencies(tree, funcPat, headPat);
            JapaneseDependencyTreeLib.setBunsetsuHead(tree, funcPat, headPat);
            //JapaneseDependencyTreeLib.setParticleToBunsetsuDepRel(tree, particlePat); // 
            tree.setChildren();
            tree.setBunsetsuChildren();
        }

        ArrayList<Instance> instsAry = new ArrayList<Instance>();
        
        log.info("counting feature vectors...");
        int cnt = 0;
        for (DependencyTree tree : treesAry) {
            Instance[] coordInsts = this.createInstances(tree, true);
        }

        log.info("removing pi features which appear only one time in the training data");
        Object[] alpKeys = this.alp.toArray();
        for (int i = 0; i < alpKeys.length; i++) {
            String key = (String) alpKeys[i];
            int freq = this.alp.getFreq(key);
            //System.err.println("key="+key+" freq="+freq);
            if (freq <= FEATURE_FREQ_THRESHOLD) {
                //System.err.println("FEATURE "+key+" REMOVED");
                this.alp.remove(key);
            }
        }
        this.alp.reIndex();

        log.info("creating feature vectors...");
        Timer timer = new Timer();
        timer.start();
        cnt = 0;
        for (DependencyTree tree : treesAry) {
            Instance[] coordInsts = this.createInstances(tree, false);
            for (int k = 0; k < coordInsts.length; k++) {
                instsAry.add(coordInsts[k]);
            }
            cnt++;
            if ((cnt+1)%2000 == 0) {
                log.info("read "+(cnt+1)+" instances");
            }
        }
        timer.stop();


        log.info("creating time of feature vectors: "+timer.get());

        log.info("# of instances for coordination analyzer: "+instsAry.size());

        log.info("=== learning coordination analyzer model ===");

        timer.start();
        Parameter llParam = new Parameter (SolverType.L2R_LR, 1, 0.01);
        Problem prob = LibLinearUtil.convert(instsAry);
        this.model = Linear.train(prob, llParam);
        timer.stop();

        log.info("done!");
        log.info("learning time: "+timer.get());
    }

    private Instance[] createInstances(DependencyTree tree, boolean alphabetGrowth) {
        Bunsetsu[] bs = tree.getBunsetsuList();
        Instance[] insts = new Instance[bs.length];

        CoordinationAnalyzerFeatureVectorPipe fvPipe =
            new CoordinationAnalyzerFeatureVectorPipe(this.alp, this.funcPat, this.particlePat);
        FeatureVector[] fvs = fvPipe.pipe(tree, alphabetGrowth);

        for (int i = 0; i < bs.length; i++) {
            String depLabel = bs[i].getDepLabel();
            Label label = new Label(0, la, la.lookupIndex(depLabel, true)); 
            insts[i] = new Instance("", label, fvs[i]);
        }
        return insts;
    }

    public void saveModel (String file) throws IOException {
        ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        os.writeObject(this.model);
        os.writeObject(this.la);
        os.close();

        Timer t = new Timer();
        t.start();
        /*
        File alpKCFile = new File(this.config.get("KCALPDB"));
        if (alpKCFile.exists()) {
            alpKCFile.delete();
        }
        log.info("writing "+this.config.get("KCALPDB")+" ...");
        KCAlphabetRecordWriter kcAlpWriter = new KCAlphabetRecordWriter(this.config.get("KCALPDB"));
        kcAlpWriter.write(this.alp);
        */
        File alpMapDBFile = new File(this.config.get("MAPDBALPDB"));
        if (alpMapDBFile.exists()) {
            alpMapDBFile.delete();
        }
        alpMapDBFile = new File(this.config.get("MAPDBALPDB")+".t");
        if (alpMapDBFile.exists()) {
            alpMapDBFile.delete();
        }
        alpMapDBFile = new File(this.config.get("MAPDBALPDB")+".p");
        if (alpMapDBFile.exists()) {
            alpMapDBFile.delete();
        }

        log.info("writing alphabet.db...");
        MapDBAlphabetWriter mapDBAlpWriter = new MapDBAlphabetWriter(this.config.get("MAPDBALPDB"));
        mapDBAlpWriter.write(this.alp);

        t.stop();
        log.info("writing time of alphabet: "+t.get());
    }

    public void analyze (DependencyTree tree) throws IOException {
        
        CoordinationAnalyzerFeatureVectorPipe fvPipe = 
            new CoordinationAnalyzerFeatureVectorPipe(this.alp, this.funcPat, this.particlePat);

        JapaneseDependencyTreeLib.annotateWordLevelDependencies(tree, funcPat, headPat);
        JapaneseDependencyTreeLib.setBunsetsuHead(tree, funcPat, headPat);
        //JapaneseDependencyTreeLib.setParticleToBunsetsuDepRel(tree, particlePat);
        tree.setChildren();
        tree.setBunsetsuChildren();

        FeatureVector[] fvs = fvPipe.pipe(tree, false);
        Bunsetsu[] bs = tree.getBunsetsuList();
        for (int j = 0; j < bs.length; j++) {
            Bunsetsu b = bs[j];
            ClassifyResults2 cr = new ClassifyResults2();
            FeatureNode[] fNodes = LibLinearUtil.convert(fvs[j]);
            double[] values = new double[this.model.getNrClass()];
            int[] labels = this.model.getLabels();
            Linear.predictValues(this.model, fNodes, values);
            for (int l = 0; l < labels.length; l++) {
                cr.add(new Label(0, la, labels[l]), values[l]);
            }
            b.setDepLabel(cr.getArgmax().toString());
        }
    }
        
    public void loadModel (InputStream is) throws IOException {
        Timer t = new Timer();
        t.start();
        ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(is));
        try {
            this.model = (Model) ois.readObject();
            this.la = (Alphabet) ois.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }
        /*
        this.alp = new AlphabetTrieKC();
        ClassLoader cl = this.getClass().getClassLoader();
        log.debug("opening alphabet db...");
        String alphabetPath = cl.getResource(this.config.get("KCALPDB")).getPath();
        ((AlphabetTrieKC)this.alp).loadDB(alphabetPath);
        */

        this.alp = new AlphabetTrieMapDB();
        ClassLoader cl = this.getClass().getClassLoader();
        String alphabetPath = cl.getResource(this.config.get("MAPDBALPDB")).getPath();
        //log.info("opening "+this.config.get("MAPDBALPDB"));
        ((AlphabetTrieMapDB)this.alp).loadDB(alphabetPath);

        ois.close();
        t.stop();
        log.debug("reading time of coordination analyzer: "+t.get()+" msec.");
    }

    public void run () throws IOException {
        ClassLoader cl = this.getClass().getClassLoader();
        boolean modelPrepared = false;
        boolean train = false;
        if (options.trainFileStr != null || options.trainDirStr != null) {
            train = true;
            Timer timer = new Timer();
            learn(options.trainFileStr, options.trainDirStr);
            saveModel(options.modelFile);
            modelPrepared = true;
        } else if (options.modelFile != null) {
            log.debug("reading "+this.config.get("COORD_MODEL"));
            loadModel(cl.getResourceAsStream(this.config.get("COORD_MODEL")));
            modelPrepared = true;
        }

        if (train || !modelPrepared) { return; }
      
        log.debug("analyzing...");

        if (options.testDirStr == null) {
            Timer timer = new Timer();
            timer.start();
            CaboCha2Dep pipe = new CaboCha2Dep(System.in);
            //pipe.setFormat(options.inputFormat);

            JapaneseDependencyTree2CaboCha caboChaOutPipe = new JapaneseDependencyTree2CaboCha();
            //caboChaOutPipe.setFormat(options.outputFormat);

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out, "utf-8"));
            int cnt = 0;
            while (!pipe.eof()) {
                DependencyTree tree = pipe.pipePerSentence();
                if (tree == null) { continue; }
                this.analyze(tree);
                writer.write(caboChaOutPipe.pipePerSentence(tree));
                cnt++;
            }
            timer.stop();
            writer.close();
            log.debug("testing time: " + timer.get());
            if (cnt > 0) {
                log.debug("testing time per sentence: "+((double) timer.get()/(double)cnt)+" msec.");
            }
        } else {
            File testDir = new File(options.testDirStr);
            File[] testFiles = testDir.listFiles();

            int cnt = 0;
            Timer timer = new Timer();
            timer.start();

            for (int i = 0; i < testFiles.length; i++) {
                File testFile = testFiles[i];
                String[] e = testFile.toString().split("/");
                String outputFile = e[e.length-1];

                CaboCha2Dep pipe = new CaboCha2Dep(new FileInputStream(testFile));
                JapaneseDependencyTree2CaboCha caboChaOutPipe = new JapaneseDependencyTree2CaboCha();
                //caboChaOutPipe.setFormat(options.outputFormat);

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(options.outputDir+"/"+outputFile), "utf-8"));
                        
                while (!pipe.eof()) {
                    DependencyTree tree = pipe.pipePerSentence();
                    if (tree == null) { continue; }
                    this.analyze(tree);
                    cnt++;
                }
                writer.close();
            }
            timer.stop();
            log.debug("testing time: " + timer.get());
            if (cnt > 0) {
                log.debug("testing time per sentence: "+(timer.get()/(long)cnt));
            }
           
        }
    }

    public static void main(String[] args) {
        CoordinationAnalyzerOptions options = new CoordinationAnalyzerOptions();
        CmdLineParser parser = new CmdLineParser(options);
        parser.setUsageWidth(80);
        try {
            parser.parseArgument(args);
            CoordinationAnalyzer analyzer = new CoordinationAnalyzer(options);
            analyzer.run();
        } catch (CmdLineException e ) {
            System.err.println(e.getMessage());
            System.err.println("java ...");
            parser.printUsage(System.err);
            System.err.println();
            System.err.println("  Example: java ... "+parser.printExample(org.kohsuke.args4j.ExampleMode.ALL));
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }
}
