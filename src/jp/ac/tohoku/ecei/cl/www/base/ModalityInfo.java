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

public class ModalityInfo {

    private String name;
    private int eventId;

    private String source;
    private String tense;
    private String assumptional;
    private String modType;
    private String authenticity;
    private String sentiment;
    private String focus;

    public ModalityInfo (String name,
                         int eventId,
                         String source, 
                         String tense,
                         String assumptional, 
                         String modType, // type
                         String authenticity,
                         String sentiment, 
                         String focus) {   
        this.name = name;
        this.eventId = eventId;
        this.source = source;
        this.tense = tense;
        this.assumptional = assumptional;
        this.modType = modType;
        this.authenticity = authenticity;
        this.sentiment = sentiment;
        this.focus = focus;
    }

    public String getName() {
        return this.name;
    }

    public int getId () {
        return this.eventId;
    }

    public void setId (int id) {
        this.eventId = id;
    }

    public String getSource() {
        return this.source;
    }

    public String getTime() {
        return this.tense;
    }

    public String getTense() {
        return this.tense;
    }

    public String getConditional() {
        return this.assumptional;
    }

    public String getAssumptional() {
        return this.assumptional;
    }

    public String getModType() {
        return this.modType;
    }

    public String getActuality() {
        return this.authenticity;
    }

    public String getAuthenticity() {
        return this.authenticity;
    }

    public String getValuation() {
        return this.sentiment;
    }

    public String getSentiment() {
        return this.sentiment;
    }

    public String getFocus() {
        return this.focus;
    }

    public String print () {
        StringBuilder s = new StringBuilder();
        s.append(this.getName());
        s.append("\t"+(this.getId()-1)+" ");
        s.append("\t"+this.getSource());
        s.append("\t"+this.getTime());
        s.append("\t"+this.getConditional());
        s.append("\t"+this.getModType());
        s.append("\t"+this.getActuality());
        s.append("\t"+this.getValuation());
        s.append("\t"+this.getFocus()+"\n");
        return s.toString();
    }
}
