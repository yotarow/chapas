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

import java.util.List;
import java.util.ArrayList;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import jp.ac.tohoku.ecei.cl.www.io.CaboChaFormat;

public class CoordinationAnalyzerOptions {

    @Option(name="-c", usage="config file path")
    public String configFileName = "coord.conf";
    
    @Option(name="-m", usage="model file path")
    public String modelFile = "models/coord.model";

    @Option(name="-if", usage="input format")
    public CaboChaFormat inputFormat = CaboChaFormat.NEW;

    @Option(name="-of", usage="output format")
    public CaboChaFormat outputFormat = CaboChaFormat.NEW;

    @Option(name="-t", usage="training data")
    public String trainFileStr = null;

    //@Option(name="-te", usage="test data")
    //public String testFileStr = null;

    @Option(name="-td", usage="training data directory")
    public String trainDirStr = null;

    @Option(name="-ted", usage="test data directory")
    public String testDirStr = null;

    @Option(name="-od", usage="output data directory")
    public String outputDir = null;

    @Option(name="-v", usage="verbose")
    public boolean verbose = false;

    @Argument
    private List<String> arguments = new ArrayList<String>();
}
