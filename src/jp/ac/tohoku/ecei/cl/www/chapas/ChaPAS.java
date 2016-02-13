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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.net.URL;

import de.bwaldvogel.liblinear.*;
import org.kohsuke.args4j.*;

import jp.ac.tohoku.ecei.cl.www.base.*;
import jp.ac.tohoku.ecei.cl.www.io.*;
import jp.ac.tohoku.ecei.cl.www.util.*;
import jp.ac.tohoku.ecei.cl.www.kucf.*;
import jp.ac.tohoku.ecei.cl.www.mapdb.*;
import jp.ac.tohoku.ecei.cl.www.db.*;
import jp.ac.tohoku.ecei.cl.www.liblinear.*;
import jp.ac.tohoku.ecei.cl.www.coord.*;

import gnu.trove.iterator.*;
import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.hash.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ChaPAS {

    private static Log log = LogFactory.getLog(ChaPAS.class);

    public enum ArLibType { PA, LIBLINEAR }; // argument classifier libtype
    
    // experimental settings
    private static final boolean DEBUG_MODE = false;
    private static final boolean INTRA_SENTENCE_ONLY = true;
    private static final ArLibType AR_LIBTYPE = ArLibType.LIBLINEAR;

    private static final int ARGUMENT_POS_THRESHOLD = 0;
    private static final int FEATURE_FREQ_THRESHOLD = 1;
    private static final double SRL_LOCAL_MARGIN = 1.0;
    private static final double NONE_PENALTY = 2.0;

    private CoordinationAnalyzer coordAnalyzer = null;

    private Model llPiModel;
    private Model llArModel;

    private static int FALSE_VAL = -1;
    private static int TRUE_VAL = 1;

    //private static double gaThreshold = 0.99999;
    //private static double woThreshold = 0.99999;
    //private static double niThreshold = 0.99999;

    private LinearModel arModel; // re-ranking

    private AlphabetTrie piAlphabet; // feature info
    private AlphabetTrie arAlphabet; // feature info
    private Alphabet predLa; // predicate label alphabet for predicate identifier
    private Alphabet srlLa; // argument label info
    private Alphabet globalLa; // label alphabet for global features
    private Label globalLabel; // label for global features

    private HashMap<String, String> config;
    private HashSet<String> argPOSSet;
    private ArrayList<String> cases;
    private HashSet<String> funcPat;
    private HashSet<String> headPat;
    private HashSet<String> particlePat;
    private HashMap<String, String> passivePat;
    private HashMap<String, String> causativePat;
    private HashMap<String, String> sahenPat;
    private HashMap<String, String> caseMap;
    private PredicatePattern kucfPredPat;

    private ChaPASOptions options = null;

    private PLSICooccurenceInfoManager coocMan = null;
    private KUCFManager kucfMan = null;
    private KUCFManager[] kucf2KMan = null;
    private KUCFManager[] kucf5HMan = null;

    private KeyValueDBManager sw2000Man = null;
    private KeyValueDBManager sw500Man = null;


    // for analyze
    private ChaPAS (ChaPASOptions options) throws FileNotFoundException, IOException, Exception {
        this.options = options;
        this.init();
    }

    private void init () throws FileNotFoundException, IOException, Exception {
        ClassLoader cl = this.getClass().getClassLoader();
        this.config = PairParser.parse(cl.getResourceAsStream(options.configFileName));

        log.debug("reading pattern files...");
        this.funcPat = PatternFileParser.parse(cl.getResourceAsStream(this.config.get("FUNCPAT")));
        this.headPat = PatternFileParser.parse(cl.getResourceAsStream(this.config.get("HEADPAT")));
        this.particlePat = PatternFileParser.parse(cl.getResourceAsStream(this.config.get("PARTICLEPAT")));
        this.passivePat = PairParser.parse(cl.getResourceAsStream(this.config.get("PASSIVEPAT")));
        this.causativePat = PairParser.parse(cl.getResourceAsStream(this.config.get("CAUSATIVEPAT")));
        this.sahenPat = PairParser.parse(cl.getResourceAsStream(this.config.get("SAHENPAT")));
        this.caseMap = PairParser.parse(cl.getResourceAsStream(this.config.get("CASE_MAPPING")));
        this.kucfPredPat = new PredicatePattern(PatternFileParser.parse(cl.getResourceAsStream(this.config.get("KUCFPREDPAT"))));

        log.debug("opening cooccurence dbs...");
        String pzDBFile = cl.getResource(this.config.get("COOCPZDB_MAPDB")).getPath();
        String pnzDBFile = cl.getResource(this.config.get("COOCPNZDB_MAPDB")).getPath();
        String pcvzDBFile = cl.getResource(this.config.get("COOCPCVZDB_MAPDB")).getPath();
        //this.coocMan = PLSICooccurenceInfoManagerMapDB.getInstance(pzDBFile, pnzDBFile, pcvzDBFile, options.storeOnMemory);
        this.coocMan = PLSICooccurenceInfoManagerMapDB.getInstance(pzDBFile, pnzDBFile, pcvzDBFile);

        URL kucfURL = cl.getResource(this.config.get("KUCF_MAPDB"));

        if (kucfURL != null) {
            log.debug("opening case frame dbs...");
            String kucfDBFileStr = kucfURL.getPath();
            File kucfDBFile = new File(kucfDBFileStr);
            if (kucfDBFile.exists()) {
                this.kucfMan = new KUCFManagerMapDB(kucfDBFileStr);
            }
        } else {
            log.debug("kucfURL == null");
        }

        URL[] kucf2KURL = new URL[3];
        this.kucf2KMan = new KUCFManagerMapDB[3];
        for (int i = 0; i < kucf2KURL.length; i++) {
            kucf2KURL[i] = null;
            kucf2KMan[i] = null;
        }
        kucf2KURL[0] = cl.getResource(this.config.get("KUCF_2KPLAC_MAPDB"));
        kucf2KURL[1] = cl.getResource(this.config.get("KUCF_2KPCAC_MAPDB"));
        kucf2KURL[2] = cl.getResource(this.config.get("KUCF_2KPCAL_MAPDB"));
        for (int i = 0; i < kucf2KURL.length; i++) {
            if (kucf2KURL[i] == null) { continue; }
            String kucfDBFileStr = kucf2KURL[i].getPath();
            File kucfDBFile = new File(kucfDBFileStr);
            if (kucfDBFile.exists()) {
                this.kucf2KMan[i] = new KUCFManagerMapDB(kucfDBFileStr);
            }
        }

        URL[] kucf5HURL = new URL[3];
        this.kucf5HMan = new KUCFManagerMapDB[3];
        for (int i = 0; i < kucf5HURL.length; i++) {
            kucf5HURL[i] = null;
            kucf5HMan[i] = null;
        }
        kucf5HURL[0] = cl.getResource(this.config.get("KUCF_5HPLAC_MAPDB"));
        kucf5HURL[1] = cl.getResource(this.config.get("KUCF_5HPCAC_MAPDB"));
        kucf5HURL[2] = cl.getResource(this.config.get("KUCF_5HPCAL_MAPDB"));
        for (int i = 0; i < kucf5HURL.length; i++) {
            if (kucf5HURL[i] == null) { continue; }
            String kucfDBFileStr = kucf5HURL[i].getPath();
            File kucfDBFile = new File(kucfDBFileStr);
            if (kucfDBFile.exists()) {
                this.kucf5HMan[i] = new KUCFManagerMapDB(kucfDBFileStr);
            }
        }
        
        log.debug("opening word class dbs...");
        URL sw2000DBURL = cl.getResource(this.config.get("SW2000_MAPDB"));
        if (sw2000DBURL != null) {
            String sw2000DBFileStr = sw2000DBURL.getPath();
            File sw2000DBFile = new File(sw2000DBFileStr);
            if (sw2000DBFile.exists()) {
                //log.info(sw2000DBFileStr);
                this.sw2000Man = new KeyValueDBManagerMapDB(sw2000DBFileStr);
            }
        } else {
            log.debug("sw2000DBURL == null");
        }

        URL sw500DBURL = cl.getResource(this.config.get("SW500_MAPDB"));
        if (sw500DBURL != null) {
            String sw500DBFileStr = sw500DBURL.getPath();
            File sw500DBFile = new File(sw500DBFileStr);
            if (sw500DBFile.exists()) {
                this.sw500Man = new KeyValueDBManagerMapDB(sw500DBFileStr);
            }
        } else {
            log.debug("sw500DBURL == null");
        }

        // read coordination model
        if (options.useCoordinationAnalyzer) {
            log.debug("reading coordination analyzer...");
            CoordinationAnalyzerOptions coordOptions = new CoordinationAnalyzerOptions();
            this.coordAnalyzer = new CoordinationAnalyzer(coordOptions);
            this.coordAnalyzer.loadModel(cl.getResourceAsStream(this.config.get("COORD_MODEL")));
        }
    }

    private void learn (String trainFileStr, String trainDirStr) throws IOException {

        this.piAlphabet = new AlphabetTrie();
        this.arAlphabet = new AlphabetTrie();
        
        this.cases = new ArrayList<String>();
        this.predLa = new Alphabet();
        this.srlLa = new Alphabet();
        this.globalLa = new Alphabet();
        this.globalLabel = new Label(0, globalLa, globalLa.lookupIndex("GLOBAL", true)); 

        TObjectIntHashMap<String> argPOSFreqHash = new TObjectIntHashMap<String>();

        double max = Double.NEGATIVE_INFINITY;
        int epochCnt = 0;

        ArrayList<ArrayList<DependencyTree>> trees2DAry = new ArrayList<ArrayList<DependencyTree>>();
        
        if (trainFileStr != null) {
            log.info("reading "+trainFileStr);
            try {
                File trainFile = new File(trainFileStr);
                CaboCha2Dep pipe = new CaboCha2Dep(new FileInputStream(trainFile));
                pipe.setFormat(options.inputFormat);
                while (!pipe.eof()) {
                    ArrayList<DependencyTree> treesAry = new ArrayList<DependencyTree>();
                    int articleIdx = 0;
                    DependencyTree[] trees;
                    if (INTRA_SENTENCE_ONLY) {
                        DependencyTree tree = pipe.pipePerSentence();
                        if (tree != null) {
                            treesAry.add(tree);
                        }
                    }
                    if (treesAry.size() > 0) {
                        trees2DAry.add(treesAry);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (trainDirStr != null) {
            File trainDir = new File(trainDirStr);
            File[] trainFiles = trainDir.listFiles();
            for (int i = 0; i < trainFiles.length; i++) {
                if (trainFiles[i].toString().endsWith("~")) { continue; }
                log.info("reading "+trainFiles[i]);
                try {
                    CaboCha2Dep pipe = new CaboCha2Dep(new FileInputStream(trainFiles[i]));
                    while (!pipe.eof()) {
                        ArrayList<DependencyTree> treesAry = new ArrayList<DependencyTree>();
                        int articleIdx = 0;
                        DependencyTree[] trees;
                        if (INTRA_SENTENCE_ONLY) {
                            DependencyTree tree = pipe.pipePerSentence();
                            if (tree != null) {
                                treesAry.add(tree);
                            }
                        } else {
                            trees = pipe.pipePerArticle(); // does not checked
                            for (int j = 0; j < trees.length; j++) {
                                treesAry.add(trees[j]);
                            }
                        }
                        if (treesAry.size() > 0) {
                            trees2DAry.add(treesAry);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        for (int i = 0; i < trees2DAry.size(); i++) {
            ArrayList<DependencyTree> trees = trees2DAry.get(i);
            for (DependencyTree tree : trees) {
                JapaneseDependencyTreeLib.annotateWordLevelDependencies(tree, funcPat, headPat);
                JapaneseDependencyTreeLib.setBunsetsuHead(tree, funcPat, headPat);
                this.coordAnalyzer.analyze(tree); // provisional
                tree.setChildren();
                tree.setBunsetsuChildren();

                // get POS frequencies of arguments
                PredicateArgumentStructure[] pASList = tree.getPASList();
                for (int j = 0; j < pASList.length; j++) {
                    int[] argumentIds = pASList[j].getIds();
                    String[] argumentLabels = pASList[j].getLabels();
                    for (int k = 0; k < argumentIds.length; k++) {
                        DependencyNode node = null;
                        for (int l = 0; l < tree.size(); l++) {
                            node = tree.getNodeFromId(argumentIds[k]);
                            if (node != null) { break; }
                        }
                        if (!argPOSFreqHash.containsKey(node.getPOS())) {
                            argPOSFreqHash.put(node.getPOS(), 0);
                        }
                        argPOSFreqHash.increment(node.getPOS());			
                    }
                    for (int k = 0; k < argumentLabels.length; k++) {
                        if (!cases.contains(argumentLabels[k])) {
                            cases.add(argumentLabels[k]);
                            log.info("case added: "+argumentLabels[k]);
                        }
                    }
                }
            }
        }

        // create argPOSSet
        Object[] argPOSKeys = argPOSFreqHash.keys();
        this.argPOSSet = new HashSet<String>();
        for (int i = 0; i < argPOSKeys.length; i++) {
            int freq = argPOSFreqHash.get((String) argPOSKeys[i]);
            if (freq > ARGUMENT_POS_THRESHOLD) {
                this.argPOSSet.add((String) argPOSKeys[i]);
            }
        }

        int numOfArticles = trees2DAry.size();
        DependencyTree[][] trees = new DependencyTree[numOfArticles][];
        PASInstance[][][] pasInsts = new PASInstance[numOfArticles][][]; // instances for SRL
        ArrayList<Instance> piInstsAry = new ArrayList<Instance>();
        ArrayList<Instance> arInstsAry = new ArrayList<Instance>();

        // instances for training ga, wo, ni models
        HashMap<String, ArrayList<Instance>> instsHash = new HashMap<String, ArrayList<Instance>>();
        
        Timer fvTimer = new Timer();

        boolean filterFeatures = FEATURE_FREQ_THRESHOLD > 0;

        if (filterFeatures) {
            log.info("counting features...");
            fvTimer.start();

            // count features
            for (int i = 0; i < trees2DAry.size(); i++) {
                ArrayList<DependencyTree> treesAry = trees2DAry.get(i);
                trees[i] = (DependencyTree[]) treesAry.toArray(new DependencyTree[treesAry.size()]);
                // create instances for predicate identification
                for (int j = 0; j < trees[i].length; j++) {
                    Instance[] pii = this.createPIInstances(trees[i][j], true);
                }

                PASInstance[][] ari = this.createARInstances(trees[i], true);
                if ((i+1) % 200 == 0) {
                    log.info("read "+(i+1)+" instances");
                }
            }

            log.info("removing infrequent features for pi model");
            Object[] piAlpKeys = this.piAlphabet.toArray();
            for (int i = 0; i < piAlpKeys.length; i++) {
                String key = (String) piAlpKeys[i];
                int freq = this.piAlphabet.getFreq(key);
                if (freq <= FEATURE_FREQ_THRESHOLD) {
                    this.piAlphabet.remove(key);
                }
            }
            this.piAlphabet.reIndex();

            log.info("removing infrequent features for ar model");
            Object[] arAlpKeys = this.arAlphabet.toArray();
            for (int i = 0; i < arAlpKeys.length; i++) {
                String key = (String) arAlpKeys[i];
                int freq = this.arAlphabet.getFreq(key);
                if (freq <= FEATURE_FREQ_THRESHOLD) {
                    this.arAlphabet.remove(key);
                }
            }
            this.arAlphabet.reIndex();
        }

        log.info("creating feature vectors...");
        TObjectIntHashMap<String> exCount = new TObjectIntHashMap();
        for (int i = 0; i < trees2DAry.size(); i++) {
            ArrayList<DependencyTree> treesAry = trees2DAry.get(i);
            trees[i] = (DependencyTree[]) treesAry.toArray(new DependencyTree[treesAry.size()]);

            // create instances for predicate identification
            for (int j = 0; j < trees[i].length; j++) {
                Instance[] piInsts = this.createPIInstances(trees[i][j], filterFeatures ? false : true);
                for (int k = 0; k < piInsts.length; k++) {
                    piInstsAry.add(piInsts[k]);
                }
            }
            // create instances for argument classification
            pasInsts[i] = this.createARInstances(trees[i], filterFeatures ? false : true);
            for (int j = 0; j < pasInsts[i].length; j++) {
                for (int k = 0; k < pasInsts[i][j].length; k++) {
                    if (pasInsts[i][j][k] == null) { continue; }
                    FeatureVector[] srlFvs = pasInsts[i][j][k].getSRLFvs();
                    Label[] srlLabels = pasInsts[i][j][k].getArgLabels();
                    for (int l = 0; l < srlFvs.length; l++) {
                        arInstsAry.add(new Instance("", srlLabels[l], srlFvs[l]));

                        String label = srlLabels[l].toString();
                        if (!instsHash.containsKey(label)) {
                            instsHash.put(label, new ArrayList<Instance>());
                        }
                        //ArrayList<Instance> insts = instsHash.get(label);
                        instsHash.get(label).add(new Instance("", new Label(0, null, TRUE_VAL), srlFvs[l]));
                        if (!exCount.containsKey(label)) {
                            exCount.put(label, 0);
                        }
                        exCount.increment(label);
                    }
                }
            }
            if ((i+1) % 200 == 0) {
                log.info("read "+(i+1)+" instances");
            }
        }
        for (TObjectIntIterator<String> it = exCount.iterator(); it.hasNext();) {
            it.advance();
            log.info("# of "+it.key()+" instances:"+it.value());
        }

        log.info("\n");
        fvTimer.stop();
        log.info("creating time of feature vectors: "+fvTimer.get());

        log.info("# of instances for learning predicate identifier: "+piInstsAry.size());
        log.info("# of instances for learning argument classifier : "+arInstsAry.size());

        // learn predicate identifier
        log.info("=== learning predicate identification model ===");

        Parameter llPiParam = new Parameter (options.solverType, 1, 0.01);
        Problem piProb = LibLinearUtil.convert(piInstsAry);
        this.llPiModel = Linear.train(piProb, llPiParam);

        Timer trainTimer = new Timer();
        long totalTime = 0;

        this.arModel = new LinearModel();
        AbstractOnlineLinearModelLearner learner = new PA2 (this.arModel);

        trainTimer.start();
        if (AR_LIBTYPE == ArLibType.LIBLINEAR) {
            log.info("=== learning llArModel model ===");
            Parameter llArParam = new Parameter (options.solverType, 1, 0.01);
            Problem arProb = LibLinearUtil.convert(arInstsAry);
            this.llArModel = Linear.train(arProb, llArParam);
        }

        trainTimer.stop();
        totalTime += trainTimer.get();
        trainTimer.start();
        
        if (options.useReranking) {
            while (epochCnt < options.numOfIter) {
                log.info("[ epoch "+(epochCnt+1)+" ]");
                trainTimer.start();
                int totalLoss = 0;
                for (int i = 0; i < pasInsts.length; i++) {
                    totalLoss += this.learnGlobalModel (learner, trees[i], pasInsts[i]);
                    if ((i+1) % 200 == 0) {
                        log.info((i+1)+"\n");
                    }
                }
                log.info("\n");
                epochCnt++;
                log.info("cumulative loss: " + totalLoss);
                trainTimer.stop();
                log.info("epoch time: " + trainTimer.get());	    
                totalTime += trainTimer.get();
                if (totalLoss == 0) {
                    break;
                }
                //if (options.writeModelForEachIter) {
                //try {
                //this.saveModel(options.modelFile+"."+epochCnt);
                //} catch (IOException e) {
                //e.printStackTrace();
                //bin/}
                //}
            }
            this.arModel.compress();
            this.arModel.deleteZeroWeights();
        }
        log.info("done!");
        log.info("learning time: "+totalTime);
    }

    private Instance[] createPIInstances(DependencyTree tree, boolean alphabetGrowth) {
        Instance[] insts = new Instance[tree.size()];
        DependencyNode[] nodes = tree.getNodes();

        PredicateIdentifierFeatureVectorPipe pp = new PredicateIdentifierFeatureVectorPipe();
        FeatureVector[] fvs = pp.pipe(this.piAlphabet, tree, alphabetGrowth, null);
        PredicateArgumentStructure[] pASList = tree.getPASList();
        for (int i = 0; i < nodes.length; i++) {
            int pASIdx = -1;
            for (int j = 0; j < pASList.length; j++) {
                if (nodes[i].getId() == pASList[j].getPredicateId()) {
                    pASIdx = j;
                }
            }
            String label = "FALSE";
            if (pASIdx != -1) {
                label = pASList[pASIdx].predicateType;
            }
            Label target = new Label(0, predLa, predLa.lookupIndex(label, true));
            insts[i] = new Instance("", target, fvs[i]);
        }
        return insts;
    }

    private PASInstance[][] createARInstances(DependencyTree[] trees, boolean alphabetGrowth) throws IOException {
       	Timer timer = new Timer();
        PASInstance[][] pasInsts = new PASInstance[trees.length][];
        SRLFeatureVectorPipe srlfvp;
        srlfvp = new SRLFeatureVectorPipe0317(this.cases, null, this.passivePat, this.causativePat, 
                                              this.sahenPat, this.particlePat, this.coocMan, 
                                              this.kucfMan, this.kucf2KMan, this.kucf5HMan,
                                              this.kucfPredPat, this.sw2000Man, this.sw500Man);
        for (int i = 0; i < trees.length; i++) {
            PredicateArgumentStructure[] pASList = trees[i].getPASList();
            int pasCnt = 0;
            pasInsts[i] = new PASInstance[pASList.length];
        }
	
        // create groups and their label alphabets

        for (int i = 0; i < trees.length; i++) {
            DependencyTree tree = trees[i];
            PredicateArgumentStructure[] pASList = tree.getPASList();
            int pasCnt = 0;
            pasInsts[i] = new PASInstance[pASList.length];
            for (int j = 0; j < pASList.length; j++) {
                int predId = pASList[j].getPredicateId();
                String predSense = pASList[j].getPredicateSense();
                String predType = pASList[j].predicateType;

                DependencyNode node = tree.getNodeFromId(predId);
		
                // SRL -----------------------------
                TIntObjectHashMap<String> id2labelStr = new TIntObjectHashMap<String>();

                int[] args = pASList[j].getIds();
                for (int k = 0; k < args.length; k++) {
                    id2labelStr.put(args[k], pASList[j].getLabel(k));
                }

                if (args.length == 0) { continue; }

                int[] argIndices = SRLArgumentPruner.leaveBunsetsuHeadsAndInnerCands(tree, predId, 
                                                                                     this.argPOSSet, 
                                                                                     this.headPat);

                String[] argIds = new String[argIndices.length];
                Label[] argLabels = new Label[argIndices.length];
                LabelAssignment corLabelAsn = new LabelAssignment();
                for (int k = 0; k < argIndices.length; k++) {

                    String argLabelStr = id2labelStr.containsKey(argIndices[k]) ? 
                        (String) id2labelStr.get(argIndices[k]) : "NONE";

                    argLabels[k] = new Label(0, srlLa, srlLa.lookupIndex(argLabelStr));
                    corLabelAsn.add(argIndices[k], argLabels[k]);
                }

                // get feature vectors
                FeatureVector[] srlFvs = srlfvp.pipe(this.arAlphabet, tree, predId, 
                                                     argIndices, alphabetGrowth, predType);
                FeatureVector corGlobalFv = srlfvp.getGlobalFeatureVector(this.arAlphabet, tree, predId, predType, 
                                                                          corLabelAsn, this.caseMap, alphabetGrowth);

                pasInsts[i][pasCnt] = new PASInstance(predId, argIndices, argLabels, null, srlFvs, corGlobalFv);
                pasInsts[i][pasCnt].setSentenceId(tree.getId());
                pasInsts[i][pasCnt].predType = predType;
                pasCnt++;
            }
        }
        return pasInsts;
    }

    private int learnGlobalModel (AbstractOnlineLinearModelLearner learner, DependencyTree[] trees, PASInstance[][] pasInsts) {

        Timer timer = new Timer();
        
        SRLFeatureVectorPipe srlfvp;
        srlfvp = new SRLFeatureVectorPipe0317(this.cases, null, this.passivePat, this.causativePat, 
                                              this.sahenPat, this.particlePat, this.coocMan, 
                                              this.kucfMan, this.kucf2KMan, this.kucf5HMan,
                                              this.kucfPredPat, this.sw2000Man, this.sw500Man); 
        timer.start();

        Label[] srlLabels = new Label[srlLa.size()];
        int slCnt = 0;
        for (Iterator it = srlLa.iterator(); it.hasNext();) {
            srlLabels[slCnt] = new Label(0, srlLa, srlLa.lookupIndex(it.next(), true));
            slCnt++;
        }

        //  learn global SRL Model ----------------------------------------
        int totalLoss = 0;


        for (int i = 0; i < pasInsts.length; i++) {

            if (pasInsts[i] == null || pasInsts[i].length == 0) {
                continue;
            }

            DependencyTree tree = trees[i];
            for (int j = 0; j < pasInsts[i].length; j++) {
                if (pasInsts[i][j] == null) { continue; }

                PASInstance inst = pasInsts[i][j];
                
                String predType = inst.predType; 
                
                int predId = inst.predId;
                
                String[] corIds = inst.argIds;
                int[] argIndices = inst.argIndices;
                Label[] corSRLLabels = inst.argLabels;

                // get feature vectors
                FeatureVector[] srlFvs = inst.srlFvs;
                FeatureVector corGlobalFv = inst.getGlobalFv();

                // -----------------------------------------------
                //                calc target score
                // -----------------------------------------------

                double corScore = 0.0;
                for (int k = 0; k < srlFvs.length; k++) {
                    corScore += learner.dotProduct(srlFvs[k], corSRLLabels[k], true);
                }
                corScore += learner.dotProduct(corGlobalFv, globalLabel, true); // add global feat scores
                
                // for debugging
                int argmax = -1;
                double max = Double.NEGATIVE_INFINITY;
                double argmaxLoss = 0.0;

                FeatureVector argmaxGlobalFv = null;
                Label[] argmaxSRLLabels = null;

                // first best fields
                Label[] fstBestSRLLabels = null;
                FeatureVector fstBestGlobalFv = null;
                LabelAssignment[] nBestAsns = new LabelAssignment[options.numOfNBest];

                ClassifyResults2[] ccrs = new ClassifyResults2[argIndices.length];
                for (int k = 0; k < argIndices.length; k++) {
                    ccrs[k] = new ClassifyResults2();
                    if (AR_LIBTYPE == ArLibType.LIBLINEAR) {
                        FeatureNode[] fNodes = LibLinearUtil.convert(srlFvs[k]);
                        double[] values = new double[this.llArModel.getNrClass()];
                        int[] labels = this.llArModel.getLabels();
                        Linear.predictProbability(this.llArModel, fNodes, values);
                        for (int l = 0; l < labels.length; l++) {
                            ccrs[k].add(new Label(0, srlLa, labels[l]), values[l]);
                        }
                    } else if (AR_LIBTYPE == ArLibType.PA) {
                        for (int l = 0; l < srlLabels.length; l++) {
                            // local factor
                            double score = learner.dotProduct(srlFvs[k], srlLabels[l], true);
                            ccrs[k].add(srlLabels[l], score);
                        }
                    }
                }

                // create consistency constraint matrix
                int[][] constraints = new int[ccrs.length][];
                for (int l = 0; l < ccrs.length; l++) {
                    constraints[l] = new int[ccrs[l].getLabels().length];
                    for (int m = 0; m < ccrs[l].getLabels().length; m++) {
                        constraints[l][m] = 0;
                    }
                }

                NBestLabelAssignmentProvider asnP = new NBestLabelAssignmentProvider(argIndices, ccrs, constraints, 3*options.numOfNBest/2+1);

                int nBCnt = 0;
                // generate top N candidates
                while(nBCnt < options.numOfNBest) { 		    
                    LabelAssignment asn = asnP.next();
                    if (asn == null) { break; }
                    nBestAsns[nBCnt++] = asn;
                }

                // create n-best instances for global model
                int nBestN = 0;
                while (nBestN < nBestAsns.length && nBestAsns[nBestN] != null) {
                    nBestN++;
                }

                // calc argmax score -----------------------------------------------
                    
                for (int k = 0; k < nBestN; k++) {
                    int[] nBestSRLIndices = nBestAsns[k].getParticularGroupIndices(0);
                    Label[] nBestSRLLabels = nBestAsns[k].getParticularGroupLabels(0);
                    FeatureVector nBestGlobalFv = srlfvp.getGlobalFeatureVector(this.arAlphabet, tree, predId, predType, 
                                                                                nBestAsns[k], this.caseMap, true);

                    if (k == 0) {
                        fstBestSRLLabels = nBestSRLLabels;
                        fstBestGlobalFv = nBestGlobalFv;
                    }

                    boolean[] correct = new boolean[nBestSRLLabels.length];

                    // calc loss
                    double loss = 0.0;
                    for (int lIdx = 0; lIdx < nBestSRLLabels.length; lIdx++) {
                        correct[lIdx] = corSRLLabels[lIdx].equals(nBestSRLLabels[lIdx]) ? true : false;
                        if (!correct[lIdx]) {
                            loss += SRL_LOCAL_MARGIN;
                        }
                    }

                    double score = 0.0;
                    if (AR_LIBTYPE == ArLibType.PA) {
                        // calc score of assignment in n-best
                        score += nBestAsns[k].getScore();
                        for (int l = 0; l < inst.srlFvs.length; l++) {
                            score += correct[l] ? 0.0 : SRL_LOCAL_MARGIN;
                            
                        }
                    } 
                    score += learner.dotProduct(nBestGlobalFv, globalLabel, true);

                    if (max < score) {
                        max = score;
                        argmax = k;
                        argmaxLoss = loss;
                        argmaxSRLLabels = nBestSRLLabels;
                        argmaxGlobalFv = nBestGlobalFv;
                    }
                }

                ArrayList<Labels> corLabels = new ArrayList<Labels>();
                ArrayList<FeatureVector> corFvs = new ArrayList<FeatureVector>();
                ArrayList<Labels> argmaxLabels = new ArrayList<Labels>();
                ArrayList<FeatureVector> argmaxFvs = new ArrayList<FeatureVector>();

                if (AR_LIBTYPE == ArLibType.PA) {
                    for (int l = 0; l < srlFvs.length; l++) {
                        corLabels.add(new Labels(corSRLLabels[l]));
                        corFvs.add(srlFvs[l]);
                        argmaxLabels.add(new Labels(argmaxSRLLabels[l]));
                        argmaxFvs.add(srlFvs[l]);
                    }
                }

                // add global feats
                corLabels.add(new Labels(globalLabel));
                corFvs.add(corGlobalFv);
                argmaxLabels.add(new Labels(globalLabel));
                argmaxFvs.add(argmaxGlobalFv);

                learner.incrementalLearn((FeatureVector[]) corFvs.toArray(new FeatureVector[corFvs.size()]),
                                         (Labels[]) corLabels.toArray(new Labels[corLabels.size()]),
                                         (FeatureVector[]) argmaxFvs.toArray(new FeatureVector[argmaxFvs.size()]),
                                         (Labels[]) argmaxLabels.toArray(new Labels[argmaxLabels.size()]), argmaxLoss);
                totalLoss += (int) argmaxLoss;

                if (AR_LIBTYPE == ArLibType.PA) {
                    double fstBestLoss = 0.0;
                    for (int lIdx = 0; lIdx < fstBestSRLLabels.length; lIdx++) {
                        fstBestLoss += corSRLLabels[lIdx].equals(fstBestSRLLabels[lIdx]) ? 0.0 : SRL_LOCAL_MARGIN;
                    }
                    if (fstBestLoss > 0.0) {
                        corLabels = new ArrayList<Labels>();
                        ArrayList<Labels> fstBestLabels = new ArrayList<Labels>();
                        ArrayList<FeatureVector> fvs = new ArrayList<FeatureVector>();
			
                        for (int l = 0; l < srlFvs.length; l++) {
                            corLabels.add(new Labels(corSRLLabels[l]));
                            fstBestLabels.add(new Labels(fstBestSRLLabels[l]));
                            fvs.add(srlFvs[l]);
                        }
                        FeatureVector[] localUpdFvs = (FeatureVector[]) fvs.toArray(new FeatureVector[fvs.size()]);
                        learner.incrementalLearn(localUpdFvs, (Labels[]) corLabels.toArray(new Labels[corLabels.size()]), 
                                                 localUpdFvs, (Labels[]) fstBestLabels.toArray(new Labels[fstBestLabels.size()]), fstBestLoss);
                        totalLoss += (int) fstBestLoss;
                    } else {
                        learner.incrementC();
                    }
                }
            }
        }
        return totalLoss;
    }

    public void saveModel (String file) throws IOException {
        ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        os.writeObject(this.llPiModel);
        os.writeObject(this.llArModel);
        os.writeObject(this.arModel);
        os.writeObject(this.argPOSSet);
        os.writeObject(this.cases);

        // write label alphabets
        os.writeObject(this.globalLa);
        os.writeObject(this.globalLabel);
        os.writeObject(this.predLa);
        os.writeObject(this.srlLa);

        os.close();

        Timer t = new Timer();
        t.start();

        // write piAlphabet as MapDB db
        String mapDBPiAlpDBStr = this.config.get("MAPDBPIALPDB");
        File piAlpMapDBFile = new File(mapDBPiAlpDBStr);
        if (piAlpMapDBFile.exists()) {
            piAlpMapDBFile.delete();
        }
        piAlpMapDBFile = new File(mapDBPiAlpDBStr+".t");
        if (piAlpMapDBFile.exists()) {
            piAlpMapDBFile.delete();
        }
        piAlpMapDBFile = new File(mapDBPiAlpDBStr+".p");
        if (piAlpMapDBFile.exists()) {
            piAlpMapDBFile.delete();
        }
        log.info("writing "+mapDBPiAlpDBStr);
        MapDBAlphabetWriter mapDBPIAlpWriter = new MapDBAlphabetWriter(mapDBPiAlpDBStr);
        mapDBPIAlpWriter.write(this.piAlphabet);

        // write arAlphabet as MapDB db
        String mapDBArAlpDBStr = this.config.get("MAPDBARALPDB");
        File arAlpMapDBFile = new File(mapDBArAlpDBStr);
        if (arAlpMapDBFile.exists()) {
            arAlpMapDBFile.delete();
        }
        arAlpMapDBFile = new File(mapDBArAlpDBStr+".t");
        if (arAlpMapDBFile.exists()) {
            arAlpMapDBFile.delete();
        }
        arAlpMapDBFile = new File(mapDBArAlpDBStr+".p");
        if (arAlpMapDBFile.exists()) {
            arAlpMapDBFile.delete();
        }
        log.info("writing "+mapDBArAlpDBStr);
        MapDBAlphabetWriter mapDBARAlpWriter = new MapDBAlphabetWriter(mapDBArAlpDBStr);
        mapDBARAlpWriter.write(this.arAlphabet);

        /*
        // write piAlphabet by serializing
        log.info("writing pi_alphabet.ser.gz...");
        ObjectOutputStream piAlpos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("models/pi_alphabet.ser.gz")));
        piAlpos.writeObject(this.piAlphabet);
        piAlpos.close();

        // write arAlphabet by serializing
        log.info("writing ar_alphabet.ser.gz...");
        ObjectOutputStream arAlpos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("models/ar_alphabet.ser.gz")));
        arAlpos.writeObject(this.arAlphabet);
        arAlpos.close();
        */

        t.stop();
        log.info("writing time of alphabet: "+t.get());
    }

    public void analyze (DependencyTree[] trees) throws IOException {
        analyze(trees, false, null);
    }
	
    public void analyze (DependencyTree[] trees, boolean setLocalNBest, LabelAssignment[][] localNBest) throws IOException {
        Timer timerB = new Timer();

        SRLFeatureVectorPipe srlfvp;
        srlfvp = new SRLFeatureVectorPipe0317(this.cases, null, this.passivePat, this.causativePat, 
                                              this.sahenPat, this.particlePat, this.coocMan, 
                                              this.kucfMan, this.kucf2KMan, this.kucf5HMan,
                                              this.kucfPredPat, this.sw2000Man, this.sw500Man);
        // create label instances for SRL
        Label[] srlLabels = new Label[srlLa.size()];
        int slCnt = 0;
        for (Iterator it = srlLa.iterator(); it.hasNext();) {
            srlLabels[slCnt++] = new Label(0, srlLa, srlLa.lookupIndex(it.next()));
        }

        if (setLocalNBest) {
            localNBest = new LabelAssignment[options.numOfNBest][];
        }
        int noneIdx = srlLa.lookupIndex("NONE");

        for (int i = 0; i < trees.length; i++) {
            DependencyTree tree = trees[i];
            JapaneseDependencyTreeLib.annotateWordLevelDependencies(trees[i], funcPat, headPat);
            JapaneseDependencyTreeLib.setBunsetsuHead(tree, funcPat, headPat);
            this.coordAnalyzer.analyze(tree); // provisional

            tree.setChildren();
            tree.setBunsetsuChildren();

            PredicateArgumentStructure[] pASList; // predicate identification
            HashSet<String>[] restrictedLabelsSetAry = null;

            ArrayList<PredicateArgumentStructure>[] pASListNBest = null;

            // predicate identification
            int numPAS;
            if (options.useGoldPredicateId || options.caseRestrictedMode) {
                pASList = tree.getPASList();
                if (options.caseRestrictedMode) {
                    restrictedLabelsSetAry = new HashSet[pASList.length];
                    for (int j = 0; j < restrictedLabelsSetAry.length; j++) {
                        String[] labels = pASList[j].getLabels();
                        restrictedLabelsSetAry[j] = new HashSet<String>();
                        for (int k = 0; k < labels.length; k++) {
                            if (labels[k].equals("NONE")) { continue; }
                            restrictedLabelsSetAry[j].add(labels[k]);
                        }
                    }
                }

                for (int j = 0; j < pASList.length; j++) {
                    pASList[j].setIds(new int[0]);
                    pASList[j].setLabels(new String[0]);
                }

                numPAS = pASList.length;
                if (options.numOfPASNBest > 1) {
                    pASListNBest = new ArrayList[numPAS];
                    for (int j = 0; j < pASListNBest.length; j++) {
                        pASListNBest[j] = new ArrayList<PredicateArgumentStructure>();
                    }
                } 
            } else {
                pASList = new PredicateArgumentStructure[0]; 

                PredicateIdentifierFeatureVectorPipe pp = new PredicateIdentifierFeatureVectorPipe();
                ArrayList<PredicateArgumentStructure> pASListAry = new ArrayList<PredicateArgumentStructure>();

                DependencyNode[] nodes = tree.getNodes();
                FeatureVector[] fvs = pp.pipe(this.piAlphabet, tree, false, null);
                
                for (int j = 0; j < nodes.length; j++) {
                    ClassifyResults2 cr = new ClassifyResults2();
                    FeatureNode[] fNodes = LibLinearUtil.convert(fvs[j]);
                    double[] values = new double[this.llPiModel.getNrClass()];
                    int[] labels = this.llPiModel.getLabels();
                    Linear.predictValues(this.llPiModel, fNodes, values);
                    for (int l = 0; l < labels.length; l++) {
                        cr.add(new Label(0, predLa, labels[l]), values[l]); 
                    }
                    String argmaxStr = cr.getArgmax().toString();
                    if (!argmaxStr.equals("FALSE")) {
                        PredicateArgumentStructure pas = new PredicateArgumentStructure(nodes[j].getId(),
                                                                                        nodes[j].getLemma(),
                                                                                        (int[]) null, 
                                                                                        (String[]) null);
                        pas.predicateType = argmaxStr;
                        pASListAry.add(pas);
                    }
                }
                numPAS = pASListAry.size();

                if (options.numOfPASNBest > 1) {
                    pASListNBest = new ArrayList[numPAS];
                    for (int k = 0; k < numPAS; k++) {
                        pASListNBest[k] = new ArrayList<PredicateArgumentStructure>();
                    }
		    
                } 
                pASList = (PredicateArgumentStructure[]) pASListAry.toArray(new PredicateArgumentStructure[pASListAry.size()]);
            }

            for (int j = 0; j < numPAS; j++) {
                int predId = pASList[j].getPredicateId();
                String predicateType = pASList[j].predicateType;

                DependencyNode node = tree.getNodeFromId(predId);

                int[] argIndices = SRLArgumentPruner.leaveBunsetsuHeadsAndInnerCands(tree, predId, this.argPOSSet, this.headPat);

                DependencyNode predNode = tree.getNodeFromId(predId);

                if (argIndices.length == 0) { continue; }

                FeatureVector[] srlFvs = srlfvp.pipe(this.arAlphabet, tree, predId, argIndices, false, predicateType);

                double max = Double.NEGATIVE_INFINITY;
                double argmaxLoss = 0.0;
                LabelAssignment argmaxAsn = null;
                FeatureVector argmaxGlobalFv = null;


                PriorityQueue pQ = new PriorityQueue(3*options.numOfPASNBest/2, new LabelAssignmentComparator());

                ClassifyResults2[] ccrs = new ClassifyResults2[srlFvs.length]; // == argIndices.length
                LabelAssignment[] localNBestAsns = new LabelAssignment[options.numOfNBest];

                for (int k = 0; k < srlFvs.length; k++) {
                    ccrs[k] = new ClassifyResults2();
                    if (AR_LIBTYPE == ArLibType.LIBLINEAR) {
                        FeatureNode[] fNodes = LibLinearUtil.convert(srlFvs[k]);
                        double[] values = new double[this.llArModel.getNrClass()];
                        int[] labels = this.llArModel.getLabels();
                        Linear.predictProbability(this.llArModel, fNodes, values);
                        for (int l = 0; l < labels.length; l++) {
                            if (labels[l] == noneIdx) {
                                values[l] = - Math.log( (1.0 - values[l]) / values[l]);
                                values[l] -= NONE_PENALTY; // apply penalty
                                values[l] = 1.0 / (1 + Math.exp (-values[l]));
                            }
                            ccrs[k].add(new Label(0, srlLa, labels[l]), values[l]);
                        }
                    } else if (AR_LIBTYPE == ArLibType.PA) {
                        for (int l = 0; l < srlLabels.length; l++) {
                            double score = this.arModel.dotProduct(srlFvs[k], srlLabels[l]);
                            ccrs[k].add(srlLabels[l], score);
                            DependencyNode argNode = tree.getNodeFromId(argIndices[k]);
                            log.debug("p="+predNode.getLemma()+" a="+argNode.getLemma()+" l="+srlLabels[l].toString());
                            log.debug(this.arModel.printWeights(this.arAlphabet, srlFvs[k], srlLabels[l]));
                        }
                    }
                } 
                // create consistency constraint matrix
                int[][] constraints = new int[ccrs.length][];
                for (int l = 0; l < ccrs.length; l++) {
                    constraints[l] = new int[ccrs[l].getLabels().length];
                    for (int m = 0; m < ccrs[l].getLabels().length; m++) {
                        constraints[l][m] = 0;
                    }
                }

                NBestLabelAssignmentProvider asnP = new NBestLabelAssignmentProvider(argIndices, ccrs, constraints, 
                                                                                     3*options.numOfNBest/2+1);
                
                // re-ranking
                int nBCnt = 0;
                // generate top N candidates
                LabelAssignment argmaxAssignment = null;
                while(nBCnt < options.numOfNBest) {
                    LabelAssignment asn = asnP.next();
                    if (asn == null)
                        break;
                    if (nBCnt == 0) {
                        argmaxAssignment = asn;
                    }
                    boolean satisfied = SRLAssignmentFilter.satisfied(asn);
                    if (!satisfied) {
                        continue;
                    }
                    if (options.caseRestrictedMode) {
                        asn.setScore(asn.getScore() - SRLAssignmentFilter.scoreMinus(asn, restrictedLabelsSetAry[j]));
                    }
                    localNBestAsns[nBCnt++] = asn;
                }
                if (nBCnt == 0) {
                    localNBestAsns[nBCnt++] = argmaxAssignment;
                }
                    
                if (setLocalNBest)
                    localNBest[i] = localNBestAsns;
                
                // create n-best instances for global model
                int nBestN = 0;
                while (nBestN < localNBestAsns.length && localNBestAsns[nBestN] != null) {
                    nBestN++;
                }
		    
                if (nBestN == 0)
                    continue; 

                // calc argmax score -----------------------------------------------
                for (int k = 0; k < nBestN; k++) {
                    FeatureVector nBestGlobalFv = srlfvp.getGlobalFeatureVector(this.arAlphabet, tree, predId,  
                                                                                predicateType, localNBestAsns[k], 
                                                                                this.caseMap, false);

                    // calc score of assignment in n-best
                    double score = localNBestAsns[k].getScore();
                    score += this.arModel.dotProduct(nBestGlobalFv, globalLabel);

                    if (max < score) {
                        max = score;
                        argmaxAsn = localNBestAsns[k];
                        argmaxGlobalFv = nBestGlobalFv;
                    }
                    if (options.numOfPASNBest > 1) {
                        localNBestAsns[k].score = score;
                        pQ.add(localNBestAsns[k]);
                    }
                }
                
                if (options.numOfPASNBest > 1) {
                    int nBestCnt = 0;
                    while (nBestCnt < options.numOfPASNBest) {
                        LabelAssignment asn;
                        if ((asn = (LabelAssignment) pQ.poll()) == null)
                            break;
                        PredicateArgumentStructure pas = PredicateArgumentStructure.convert(predId, asn);
                        pas.predicateType = predicateType;
                        pASListNBest[j].add(pas);
                        nBestCnt++;
                    }
                } else if (argmaxAsn != null) {
                    int absPredId = pASList[j].absPredId;
                    String voice = pASList[j].voice;
                    String attrAll = pASList[j].attrAll;
                    pASList[j] = PredicateArgumentStructure.convert(predId, argmaxAsn);
                    pASList[j].predicateType = predicateType;
                    pASList[j].absPredId = absPredId;
                    pASList[j].voice = voice;
                    pASList[j].attrAll = attrAll;
                }
            }

            if (options.numOfPASNBest > 1) {
                trees[i].setPASListNBest(pASListNBest);
            } 
            trees[i].setPASList(pASList);
            trees[i].sortPASList();
        }
    }
        
    public void loadModel (InputStream is) throws IOException {
        Timer t = new Timer();
        t.start();
        //ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(is));
        ObjectInputStream ois = new ObjectInputStream(is);
        try {
            log.debug("reading predicate identifier parameters ...");
            this.llPiModel = (Model) ois.readObject();
            log.debug("reading argument classifier parameters ...");
            this.llArModel = (Model) ois.readObject();
            this.arModel = (LinearModel) ois.readObject();
            log.debug("reading labels...");
            this.argPOSSet = (HashSet<String>) ois.readObject();
            this.cases = (ArrayList<String>) ois.readObject();
            this.globalLa = (Alphabet) ois.readObject();
            this.globalLabel = (Label) ois.readObject();
            this.predLa = (Alphabet) ois.readObject();
            this.srlLa = (Alphabet) ois.readObject();

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }

        ClassLoader cl = this.getClass().getClassLoader();
        try {
            if (options.dbType == ChaPASDBType.MAPDB) {
                this.piAlphabet = new AlphabetTrieMapDB();
                String piAlphabetPath = cl.getResource(this.config.get("MAPDBPIALPDB")).getPath();
                log.debug("opening "+this.config.get("MAPDBPIALPDB"));
                ((AlphabetTrieMapDB)this.piAlphabet).loadDB(piAlphabetPath);

                this.arAlphabet = new AlphabetTrieMapDB();
                String arAlphabetPath = cl.getResource(this.config.get("MAPDBARALPDB")).getPath();
                log.debug("opening "+this.config.get("MAPDBARALPDB"));
                ((AlphabetTrieMapDB)this.arAlphabet).loadDB(arAlphabetPath);

            } else if (options.dbType == ChaPASDBType.MEM) {
                ObjectInputStream alpis = new ObjectInputStream(new BufferedInputStream(new FileInputStream("models/pi_alphabet.ser.gz")));
                log.debug("opening models/pi_alphabet.ser.gz");
                this.piAlphabet = (AlphabetTrie) alpis.readObject();
                alpis.close();

                alpis = new ObjectInputStream(new BufferedInputStream(new FileInputStream("models/ar_alphabet.ser.gz")));
                log.debug("opening models/ar_alphabet.ser.gz");
                this.arAlphabet = (AlphabetTrie) alpis.readObject();
                alpis.close();
            } 
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        ois.close();

        t.stop();
        log.debug("reading time of chapas model: "+t.get());
    }

    public void run () throws IOException {

        boolean modelPrepared = false;
        boolean train = false;
        if (options.trainFileStr != null || options.trainDirStr != null) {
            train = true;
            Timer timer = new Timer();
            learn(options.trainFileStr, options.trainDirStr);
            saveModel(options.modelFile);
            modelPrepared = true;
        } else if (options.modelFile != null) {
            ClassLoader cl = this.getClass().getClassLoader();
            //loadModel(cl.getResourceAsStream(this.config.get("MODELFILE")));
            loadModel(cl.getResourceAsStream(options.modelFile));
            modelPrepared = true;
        }

        if (train || !modelPrepared) { return; }
      
        log.debug("analyzing...");

        if (options.testDirStr == null) {
            Timer timer = new Timer();
            timer.start();

            InputStream is = null;

            if (options.chaPASInputMode == ChaPASInputMode.RAW) {
                // run cabocha
                String[] cmd = new String[]{"cabocha", "-f1", "-n1", "-I0"};
                Process pr = Runtime.getRuntime().exec(cmd);

                // set input text to standard input
                BufferedWriter caboChaIn = new BufferedWriter(new OutputStreamWriter(pr.getOutputStream(), "utf-8")) ; 
                
                BufferedReader systemInReader = new BufferedReader(new InputStreamReader(System.in, "utf-8")); 
                StringBuilder s = new StringBuilder();
                String line = null;
                while ( ( line = systemInReader.readLine()) != null) {
                    s.append(line+"\n");
                }
                systemInReader.close();
                caboChaIn.write(s.toString());
                caboChaIn.close();
                is = pr.getInputStream();
            } else { // options.chaPASInputMode == ChaPASInputMode.SYN
                is = System.in;
            }

            CaboCha2Dep pipe = new CaboCha2Dep(is);
            
            JapaneseDependencyTree2CaboCha caboChaOutPipe = new JapaneseDependencyTree2CaboCha();
            caboChaOutPipe.setPrintSentenceID(options.printSentenceID);
            caboChaOutPipe.setOutputPASNBest(options.numOfPASNBest > 1);
                
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out, "utf-8"));
            int cnt = 0;
            while (!pipe.eof()) {
                DependencyTree tree = pipe.pipePerSentence();
                if (tree == null) { continue; }
                DependencyTree[] trees = new DependencyTree[]{tree};
                this.analyze(trees);
                if (options.outputFormat == CaboChaFormat.UNK) {
                    options.outputFormat = pipe.getFormat();
                    caboChaOutPipe.setFormat(options.outputFormat);
                }
        
                StringBuilder resultStr = new StringBuilder();
                for (int i = 0; i < trees.length; i++) {
                    resultStr.append(caboChaOutPipe.pipePerSentence(trees[i]));
                }
                writer.write(resultStr.toString());
                cnt++;
                if ((cnt) % 10 == 0) {
                    System.err.print(".");
                }
                if ((cnt) % 200 == 0) {
                    System.err.print(" "+cnt+"\n");
                }
            }
            timer.stop();
            writer.close();
            log.debug("testing time: " + timer.get());
            if (cnt > 0) {
                log.debug("testing time per sentence: "+(timer.get()/(long)cnt));
            }
        } else {
            if (options.outputDir == null) {
                System.out.println("please set output directory by option '-od'");
                System.exit(1);
            }

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
                caboChaOutPipe.setPrintSentenceID(options.printSentenceID);
                caboChaOutPipe.setOutputPASNBest(options.numOfPASNBest > 1);

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(options.outputDir+"/"+outputFile), "utf-8"));
                        
                while (!pipe.eof()) {
                    DependencyTree tree = pipe.pipePerSentence();
                    if (tree == null) { continue; }
                    DependencyTree[] trees = new DependencyTree[]{tree};

                    if (options.outputFormat == CaboChaFormat.UNK) {
                        options.outputFormat = pipe.getFormat();
                        caboChaOutPipe.setFormat(pipe.getFormat());
                    }

                    this.analyze(trees);
                    StringBuilder resultStr = new StringBuilder();
                    for (int j = 0; j < trees.length; j++) {
                        resultStr.append(caboChaOutPipe.pipePerSentence(trees[j]));
                    }
                    writer.write(resultStr.toString());
                    cnt++;
                    if ((cnt) % 200 == 0) {
                        log.debug(cnt+"\n");
                    }
                }
                writer.close();
            }
            timer.stop();
            log.info("testing time: " + timer.get());
            if (cnt > 0) {
                log.info("testing time per sentence: "+(timer.get()/(long)cnt));
            }
        }
    }

    public static void main(String[] args) {
        ChaPASOptions options = new ChaPASOptions();
        CmdLineParser parser = new CmdLineParser(options);
        parser.setUsageWidth(80);
        try {
            parser.parseArgument(args);
            ChaPAS chapas = new ChaPAS(options);
            chapas.run();
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
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
}
