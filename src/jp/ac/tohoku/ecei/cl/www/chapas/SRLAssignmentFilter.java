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

import java.util.HashSet;
import java.util.Iterator;

import jp.ac.tohoku.ecei.cl.www.base.*;
import jp.ac.tohoku.ecei.cl.www.util.*;

public class SRLAssignmentFilter {
    
    public static boolean satisfied(LabelAssignment assignment) {
        Label[] cLabels = assignment.getLabels();
        boolean satisfied = true;
        HashSet<String> labelSet = new HashSet<String>();
        for (int i = 0; i < cLabels.length; i++) {
            if (cLabels[i].toString().equalsIgnoreCase("NONE")) {

            } else {
                if (labelSet.contains(cLabels[i].toString())) {
                    //System.err.println("duplicate: "+ cLabels[i]);
                    satisfied = false;
                }
                labelSet.add(cLabels[i].toString());
                //System.err.println("added: "+ cLabels[i]);
            }
        }
        //System.err.println("satisfied:"+satisfied);
        return satisfied;
    }

    public static boolean contains (LabelAssignment assignment, HashSet<String> restrictedLabelsSet) {
        boolean flag = true;
        for (Iterator<String> it = restrictedLabelsSet.iterator(); it.hasNext();) {
            String restrictedLabel = it.next();
            if (!assignment.contains(restrictedLabel)) {
                flag = false;
            }
        }
        return flag;
    }

    public static double scoreMinus (LabelAssignment assignment, HashSet<String> restrictedLabelsSet) {
        double score = 0.0;
        for (Iterator<String> it = restrictedLabelsSet.iterator(); it.hasNext();) {
            String restrictedLabel = it.next();
            if (!assignment.contains(restrictedLabel)) {
                score += 1.0;
            }
        }
        Label[] labels = assignment.getLabels();
        for (int i = 0; i < labels.length; i++) {
            if (!restrictedLabelsSet.contains(labels[i].toString())) {
                score += 1.0;
            }
        }
        return score;
    }

    /*
      public static boolean satisfied(Alphabet alphabet, Assignment assignment) {

      int[] cIndices = assignment.getIndices();
      int[] cLabels = assignment.getLabels();

      boolean satisfied = true;

      gnu.trove.TIntIntHashMap hash = new gnu.trove.TIntIntHashMap();
      for (int i = 0; i < cIndices.length; i++) {
      String cLabelStr = (String) alphabet.lookupObject(cLabels[i]);
      if (cLabelStr.length() == 0) { continue; }
      // CORE ARGUMENT CONSISTENCY
      if (cLabelStr.charAt(0) == 'A' && cLabelStr.charAt(1) != 'M' || 
      cLabelStr.length() > 4 && cLabelStr.charAt(2) == 'A' && cLabelStr.charAt(3) != 'M') {
      if (hash.containsKey(cLabels[i])) {
      satisfied = false;
      }
      hash.put(cLabels[i], 1);
      }
	    
      // DISCONTINUITY CONSISTENCY
      if (cLabelStr.charAt(0) == 'C') {
      String roleStr = cLabelStr.substring(2);
      if (roleStr.charAt(0) == 'A' && roleStr.charAt(1) != 'M') {
      boolean flag = false;
      for (int j = i-1; j >= 0; j--) {
      String prevLabelStr = (String) alphabet.lookupObject(cLabels[j]);
      if (prevLabelStr.equals(roleStr)) {
      flag = true;
      }
      }
      satisfied = flag;
      }
      }

      // REFERENCE CONSISTENCY
      if (cLabelStr.charAt(0) == 'R') {
      String roleStr = cLabelStr.substring(2);
      if (roleStr.charAt(0) == 'A' && roleStr.charAt(1) != 'M') {
      boolean flag = false;
      for (int j = i-1; j >= 0; j--) {
      String prevLabelStr = (String) alphabet.lookupObject(cLabels[j]);
      if (prevLabelStr.equals(roleStr)) {
      flag = true;
      }
      }
      satisfied = flag;
      }
      }
      }
      return satisfied;
      }
    */

    /*
      public static double getScore(Assignment cAssignment) {

      int[] cIndices = cAssignment.getIndices();
      int[] cLabels = cAssignment.getLabels();
      double[] cValues = cAssignment.getValues();

      double score = 1.0;

      for (int i = 0; i < cIndices.length; i++) {
      score *= cValues[i];
      }
      return score;
      }

      public static double getScore(boolean divideCore, Alphabet iAlphabet, Assignment iAssignment,
      Alphabet cAlphabet, Assignment cAssignment) {

      int[] iIndices = iAssignment.getIndices();
      int[] iLabels = iAssignment.getLabels();
      double[] iValues = iAssignment.getValues();

      int[] cIndices = cAssignment.getIndices();
      int[] cLabels = cAssignment.getLabels();
      double[] cValues = cAssignment.getValues();

      assert iIndices.length == cIndices.length;

      double score = 1.0;

      // for debugging
      for (int i = 0; i < iIndices.length; i++) {
      assert iIndices[i] == cIndices[i];
      }

      for (int i = 0; i < iIndices.length; i++) {
      String iLabelStr = (String) iAlphabet.lookupObject(iLabels[i]);
      String cLabelStr = (String) cAlphabet.lookupObject(cLabels[i]);
      if (divideCore) {
      if (iLabelStr.equals("NONE")) {
      if (cLabelStr.equals("NONE")) {
      score *= iValues[i] * cValues[i];
      } else {
      score *= 0.5;
      }
      } else if (iLabelStr.equals("CORE")) {
      if (cLabelStr.charAt(1) == 'M' || 
      cLabelStr.length() > 4 && cLabelStr.charAt(3) == 'M') {
      score *= 0.0;
      } else if (cLabelStr.charAt(0) == 'A' ||
      cLabelStr.length() > 4 && cLabelStr.charAt(2) == 'A') {
      score *= iValues[i] * cValues[i];
      } else {
      score *= 0.0;
      }
      } else if (iLabelStr.equals("MODIFIER")) {
      if (cLabelStr.charAt(1) == 'M' ||
      cLabelStr.length() > 4 && cLabelStr.charAt(3) == 'M') {
      score *= iValues[i] * cValues[i];
      } else {
      score *= 0.0;
      }
      } 
      } else {
      if (iLabelStr.equals("NONE")) {
      if (cLabelStr.equals("NONE")) {
      score *= iValues[i] * cValues[i];
      } else {
      score *= 0.0;
      }
      } else if (iLabelStr.equals("ARG")) {
      if (!cLabelStr.equals("NONE")) {
      score *= iValues[i] * cValues[i];
      } else {
      score *= 0.0;
      }
      }
      }
      }
      return score;
      }
    */
}
