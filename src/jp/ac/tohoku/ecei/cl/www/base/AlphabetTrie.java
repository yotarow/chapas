/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */




/** 
    @author Andrew McCallum <a href="mailto:mccallum@cs.umass.edu">mccallum@cs.umass.edu</a>
*/

package jp.ac.tohoku.ecei.cl.www.base;

import gnu.trove.iterator.*;
import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.hash.*;

import java.util.ArrayList;
import java.io.*;
import java.util.Iterator;

import jp.ac.tohoku.ecei.cl.www.util.Timer;

public class AlphabetTrie implements Serializable {
    TObjectIntHashMap key2val;
    TIntObjectHashMap val2key;
    TObjectIntHashMap key2count;

    int numEntries;
    boolean growthStopped = false;

    public AlphabetTrie () {
        this (10000);
    }

    public AlphabetTrie (int capacity) {
        this.key2val = new TObjectIntHashMap<String> (capacity);
        this.val2key = new TIntObjectHashMap (capacity);
        this.key2count = new TObjectIntHashMap<String> (capacity);
        numEntries = 0;
    }

    public AlphabetTrie (TObjectIntHashMap key2val, boolean growthStopped) {
        this.key2val = key2val;
        numEntries = this.key2val.size();
        // create val2key
        this.val2key = new TIntObjectHashMap ();
        this.key2count = new TObjectIntHashMap<String>();
        for (TObjectIntIterator it = this.key2val.iterator(); it.hasNext();) {
            it.advance();
            this.val2key.put(it.value(), it.key());
            this.key2count.put(it.key(), 1);
        }
        this.growthStopped = growthStopped;
    }

	
    /** Return -1 if entry isn't present. */
    public int lookupIndex (String entry, boolean addIfNotPresent) {
        if (entry == null) {
            throw new IllegalArgumentException ("Can't lookup \"null\" in an AlphabetTrie.");
        }

        int ret = key2val.get(entry);
        if (ret == 0 && addIfNotPresent && !growthStopped) {
            ret = ++numEntries;
            this.key2val.put (entry, ret);
            this.val2key.put (ret, entry);
            this.key2count.put(entry, 1);
        } else {
            if (ret != 0 && addIfNotPresent && !growthStopped && this.key2count != null) {
                if (!this.key2count.containsKey(entry)) {
                    this.key2count.put(entry, 1);
                } else {
                    this.key2count.increment(entry);
                }
            }
        }
        if (ret == 0) {
            ret = -1;
        }
        return ret;
    }

    public void remove (String entry) {
        int idx = this.key2val.get(entry);
        this.key2val.remove(entry);
        this.val2key.remove(idx);
        this.key2count.remove(entry);
    }

    public Object lookupObject (int id) {
        return this.val2key.containsKey(id) ? this.val2key.get(id) : null;
    }

    public int getFreq (String entry) {
        return this.key2count.containsKey(entry) ? key2count.get(entry) : 0;
    }

    public Object[] toArray () {
        return key2val.keys();
    }

    public void reIndex () {
        Object[] keys = this.key2val.keys();
        this.key2val = new TObjectIntHashMap();
        this.val2key = new TIntObjectHashMap();
        TObjectIntHashMap key2countNew = new TObjectIntHashMap();
        for (int i = 0; i < keys.length; i++) {
            int val = i+1;
            this.key2val.put(keys[i], val);
            this.val2key.put(val, keys[i]);
            int count = 0;
            if (this.key2count.containsKey(keys[i])) {
                count = this.key2count.get(keys[i]);
            }
            key2countNew.put(keys[i], count);
        }
        this.key2count = key2countNew;
    }

    public boolean contains (Object entry) {
        return key2val.contains (entry);
    }

    public int size () {
        return numEntries;
    }

    public void stopGrowth () {
        growthStopped = true;
        key2val.compact();
        val2key.compact();
    }

    public void allowGrowth () {
        growthStopped = false;
    }

    public boolean growthStopped () {
        return growthStopped;
    }

    public TObjectIntIterator<String> iterator() {
        return this.key2val.iterator();
    }

    // Serialization 
    private static final long serialVersionUID = 1;
    private static final int CURRENT_SERIAL_VERSION = 0;

    private void writeObject (ObjectOutputStream out) throws IOException {
        out.writeInt (CURRENT_SERIAL_VERSION);
        out.writeInt (this.numEntries);
        out.writeObject(this.key2val);
        out.writeBoolean (this.growthStopped);
    }

    private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt ();
        this.numEntries = in.readInt();
        this.key2val = (TObjectIntHashMap) in.readObject();
        this.growthStopped = in.readBoolean();
        // create val2key
        this.val2key = new TIntObjectHashMap ();
        //System.err.println("HOGE");
        for (TObjectIntIterator it = this.key2val.iterator(); it.hasNext();) {
            it.advance();
            this.val2key.put(it.value(), it.key());
        }
    }

    public static void main (String[] args) {

        AlphabetTrie alp = new AlphabetTrie();
        Timer t = new Timer();
        t.start();
        for (int i = 0; i < 1000000; i++) {
            int idx = alp.lookupIndex("str"+i, true);
        }
        t.stop();
        System.err.println("looking up new 100000 elements. time : "+t.get());
        t.start();
        for (int i = 0; i < 1000000; i++) {
            int idx = alp.lookupIndex("str"+i, true);
        }
        t.stop();
        System.err.println("looking up existing 100000 elements. time : "+t.get());

    }
}
