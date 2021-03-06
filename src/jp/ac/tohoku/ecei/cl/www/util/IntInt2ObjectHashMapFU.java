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

package jp.ac.tohoku.ecei.cl.www.util;

import gnu.trove.iterator.*;
import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.hash.*;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import java.io.Serializable;

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.*;

public class IntInt2ObjectHashMapFU implements Serializable {

    private Int2ObjectOpenHashMap outer = new Int2ObjectOpenHashMap ();

    public Object[] getValues() {
        ArrayList objects = new ArrayList();
        int[] outerKeys = outer.keySet().toIntArray();
        for (int i = 0; i < outerKeys.length; i++) {
            Int2ObjectOpenHashMap inner = (Int2ObjectOpenHashMap) outer.get(outerKeys[i]);
            int[] innerKeys = inner.keySet().toIntArray();
            for (int j = 0; j < innerKeys.length; j++) {
                objects.add(inner.get(innerKeys[j]));
            }
        }
        return objects.toArray();
    }

    public void put (int key1, int key2, Object obj) {
        Int2ObjectOpenHashMap inner = (Int2ObjectOpenHashMap) outer.get (key1);
        if (inner == null) {
            inner = new Int2ObjectOpenHashMap ();
            outer.put (key1, inner);
        }

        inner.put(key2, obj);
    }

    public boolean containsKey (int key1, int key2) {
        Int2ObjectOpenHashMap inner = (Int2ObjectOpenHashMap) outer.get (key1);
        if (inner == null) {
            return false;
        } else {
            if (inner.containsKey(key2)) {
                return true;
            } else {
                return false;
            }
        }
    }

    public Object get (int key1, int key2) {
        Int2ObjectOpenHashMap inner = (Int2ObjectOpenHashMap) outer.get (key1);
        if (inner == null) {
            return null;
        } else {
            return inner.get (key2);
        }
    }

    // not yet implemented
    public int size() {
        return 0;
    }

    public void clear () { outer.clear (); }
}
