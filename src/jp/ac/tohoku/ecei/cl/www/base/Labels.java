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

package jp.ac.tohoku.ecei.cl.www.base;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Labels implements Comparable, Serializable {

    private static Log log = LogFactory.getLog(Labels.class);

    //private FactorTuple ft;
    private Label[] labels;
    private int size;

    /*
    public Labels() {
        this.labels = new Label[3];
        size = 0;
    }
    */

    public Labels(Label l) {
        this.labels = new Label[1];
        this.labels[size++] = l;
    }

    public Labels(int size) {
        this.labels = new Label[size];
        this.size = size;
    }

    public Labels(Label l1, Label l2) {
        this.labels = new Label[2];
        this.labels[size++] = l1;
        this.labels[size++] = l2;
    }

    public Labels(Label l1, Label l2, Label l3) {
        this.labels = new Label[3];
        this.labels[size++] = l1;
        this.labels[size++] = l2;
        this.labels[size++] = l3;
    }

    public Labels (ArrayList<Label> labelAry) {
        this.labels = new Label[labelAry.size()];
        for (Label l : labelAry) {
            this.labels[size++] = l;
        }
    }

    public Iterator iterator() {
        return new Itr();
    }
    
    public int compareTo (Object o) {
        Labels l = (Labels) o;
        int coef = 1;
        double thisVal = 0;
        for (int i = size-1; i >=0; i--) {
            thisVal += coef * (this.labels[i].getLabel()+1); // values must be larger than "1" (for treeSet)
            coef *= 100;
        }
        double lVal = 0;
        int lSize = l.size();
        coef = 1;
        for (int i = lSize-1; i >=0; i--) {
            lVal += coef * (l.getLabel(i).getLabel()+1);
            coef *= 100;
        }
        //System.err.println("l1="+this+" l1val="+thisVal+" l2="+l+" l2val="+lVal);
        if (thisVal > lVal) {
            return 1;
        } else if (thisVal == lVal) {
            return 0;
        } else {
            return -1;
        }
    }

    public int size() {
        return size;
    }
    
    public Label[] getLabels() {
        return this.labels;
    }

    public Label getLabel(int idx) {
        return idx < this.labels.length ? this.labels[idx] : null;
    }

    public void setLabel (int idx, Label l) {
        this.labels[idx-1] = l;
    }

    public boolean equals(Object o) {
        Labels l = (Labels) o;
        int size = this.size();
        if (size != l.size()) {
            return false;
        }
        Label[] labels = l.getLabels();
        for (int i = 0; i < size; i++) {
            if (!this.labels[i].equals(labels[i])) {
                return false;
            }
        }
        return true;
    }

    public boolean equals(Labels l) {
        int size = this.size();
        if (size != l.size()) {
            return false;
        }
        Label[] labels = l.getLabels();
        for (int i = 0; i < size; i++) {
            if (!this.labels[i].equals(labels[i])) {
                return false;
            } 
        }
        return true;
    }

    public boolean contains(int idx, Label label) {
        if (labels.length <= idx) {
            return false;
        }
        return labels[idx].equals(label);
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        int cnt = 0;
        for (Iterator it = this.iterator(); it.hasNext();) {
            Label l = (Label) it.next();
            if (cnt >= 1) {
                s.append("-");
            }
            s.append(l.toString());
            cnt++;
        }
        return s.toString();
    }

    public String toString2() {
        StringBuilder s = new StringBuilder();
        int cnt = 0;
        for (Iterator it = this.iterator(); it.hasNext();) {
            Label l = (Label) it.next();
            if (cnt >= 1) {
                s.append("-");
            }
            //log.info("l==null?"+(l==null));
            //log.info(l.toString2());
            s.append(l.getGroup()+":"+l.toString());
            cnt++;
        }
        return s.toString();
    }

    // provisional

    public int hashCode () {
        //int labelSize = this.labels[0].getLabelAlphabet().size();
        int val = 0;
        for (int i = 0; i < this.labels.length; i++) {
            if (this.labels[i] == null) { continue; } 
            val += Math.pow(2, 8*i) + this.labels[i].hashCode();
        }
        return val; 
    }

    /*
      public FactorTuple getFactorTuple() {
      FactorTuple ft = new FactorTuple();
      for (Iterator it = this.iterator(); it.hasNext();) {
      ft.add(((Label)it.next()).getFactor());
      }
      return ft;
      }
    */

    /*
      public Factor[] getFactors() {
      Factor[] fs = new Factor[this.size()];
      int cnt = 0;
      for (Iterator it = this.iterator(); it.hasNext();) {
      fs[cnt++] = ((Label)it.next()).getFactor();
      }
      return fs;
      }
    */

    private class Itr implements Iterator {
        int cursor = 0;
        int lastRet = -1;

        public boolean hasNext() {
            return cursor != size();
        }

        public Label next() {
            Label l = labels[cursor];
            this.cursor++;
            return l;
        }
        
        public void remove() {
            // unsupported
            // do nothing
        }
    }
}
