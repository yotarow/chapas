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

import java.util.HashMap;
import java.io.IOException;

import jp.ac.tohoku.ecei.cl.www.base.*;
import jp.ac.tohoku.ecei.cl.www.util.*;
import jp.ac.tohoku.ecei.cl.www.kucf.*;
import jp.ac.tohoku.ecei.cl.www.db.*;

public interface SRLFeatureVectorPipe extends FeatureVectorPipe {

    public FeatureVector[] pipe(AlphabetTrie alphabet, DependencyTree tree, int predId, int[] argIds, boolean alphabetGrowth,
                                String predType) throws IOException;

    public FeatureVector getFeatureVector(AlphabetTrie alphabet, DependencyTree tree, int predId, int argId, boolean alphabetGrowth, String predType) throws IOException;
    
    
    public int numOfFeatures();

    public FeatureVector getGlobalFeatureVector(AlphabetTrie alphabet, DependencyTree tree, int predId, String predType, 
                                                LabelAssignment asn, HashMap<String, String> caseMap, boolean alphabetGrowth);

}

