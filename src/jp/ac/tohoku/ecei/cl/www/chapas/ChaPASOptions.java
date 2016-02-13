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

import java.util.List;
import java.util.ArrayList;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import jp.ac.tohoku.ecei.cl.www.io.CaboChaFormat;

import de.bwaldvogel.liblinear.*;

public class ChaPASOptions {

    @Option(name="-c", usage="chapas config file path")
    public String configFileName = "chapas.conf";
    
    @Option(name="-m", usage="model file path")
    public String modelFile = "models/chapas.model";

    @Option(name="--use-gold-predicates", usage="use gold annotations of predicates")
    public boolean useGoldPredicateId = false;
    
    @Option(name="--print-sentence-id", usage="print sentence id")
    public boolean printSentenceID = true;

    //@Option(name="--write-model-each-iter", usage="write chapas model for each iteration")
    //public boolean writeModelForEachIter = false;

    //@Option(name="-mem", aliases="--store-on-memory", usage="store additional data on memory")
    //public boolean storeOnMemory = false; // not yet implemented

    @Option(name="-N", usage="number of n-best")
    public int numOfNBest = 64;

    @Option(name="-if", usage="input format")
    public CaboChaFormat inputFormat = CaboChaFormat.UNK;

    @Option(name="-of", usage="output format")
    public CaboChaFormat outputFormat = CaboChaFormat.UNK;

    @Option(name="-pasN", usage="number of PAS n-best")
    public int numOfPASNBest = 1;

    @Option(name="-iter", usage="number of iterations in training")
    public int numOfIter = 10;

    @Option(name="-t", usage="training data")
    public String trainFileStr = null;

    @Option(name="-td", usage="training data directory")
    public String trainDirStr = null;

    @Option(name="-ted", usage="test data directory")
    public String testDirStr = null;

    @Option(name="-od", usage="output data directory")
    public String outputDir = null;

    @Option(name="--case-restricted-mode", usage="case restriction mode")
    public boolean caseRestrictedMode = false;

    // @Option(name="--use-reranking", usage="use re-ranking")
    public boolean useReranking = false; // currently not supported
    
    //@Option(name="-v", usage="verbose")
    //public boolean verbose = false;

    // options for coordination analyzer
    @Option(name="-use-coord", usage="use coordination analyzer")
    public boolean useCoordinationAnalyzer = true;

    @Option(name="-cm", usage="model coordination analyzer file path")
    public String coordModelFile = "models/coord.model";
    
    @Option(name="--solver-type", usage="liblinear solvertype")
    public SolverType solverType = SolverType.L2R_LR;

    @Option(name="-I", usage="input mode")
    public ChaPASInputMode chaPASInputMode = ChaPASInputMode.SYN;

    @Option(name="-d", usage="database type")
    public ChaPASDBType dbType = ChaPASDBType.MAPDB;

    @Argument
    private List<String> arguments = new ArrayList<String>();
}
