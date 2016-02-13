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

import java.util.ArrayList;
import java.io.*;
import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.doubles.*;
import it.unimi.dsi.fastutil.objects.*;

import jp.ac.tohoku.ecei.cl.www.util.Timer;

public class AlphabetTrieFU implements Serializable {
    Object2IntOpenHashMap key2val;
    Int2ObjectOpenHashMap val2key;
    Object2IntOpenHashMap key2count;

    int numEntries;
    boolean growthStopped = false;

    public AlphabetTrieFU () {
        this.key2val = new Object2IntOpenHashMap<String> ();
        this.val2key = new Int2ObjectOpenHashMap ();
        this.key2count = new Object2IntOpenHashMap<String> ();
        numEntries = 0;
    }

    public AlphabetTrieFU (Object2IntOpenHashMap key2val, boolean growthStopped) {
        this.key2val = key2val;
        numEntries = this.key2val.size();
        // create val2key
        this.val2key = new Int2ObjectOpenHashMap ();
        this.key2count = new Object2IntOpenHashMap<String>();
        for (ObjectIterator<String> it = this.key2val.keySet().iterator(); it.hasNext();) {
            String key = it.next();
            int val = this.key2val.get(key);
            this.val2key.put(val, key);
            this.key2count.put(key, 1);
            /*
            it.advance();
            this.val2key.put(it.value(), it.key());
            this.key2count.put(it.key(), 1);
            */
        }
        this.growthStopped = growthStopped;
    }

	
    /** Return -1 if entry isn't present. */
    public int lookupIndex (String entry, boolean addIfNotPresent) {
        if (entry == null) {
            throw new IllegalArgumentException ("Can't lookup \"null\" in an AlphabetTrieFU.");
        }

        int ret = this.key2val.getInt(entry);
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
                    this.key2count.put(entry, this.key2count.getInt(entry) + 1);
                }
            }
        }
        if (ret == 0) {
            ret = -1;
        }
        return ret;
    }

    public void remove (String entry) {
        int idx = this.key2val.getInt(entry);
        this.key2val.remove(entry);
        this.val2key.remove(idx);
        this.key2count.remove(entry);
    }

    public Object lookupObject (int id) {
        return this.val2key.containsKey(id) ? this.val2key.get(id) : null;
    }

    public int getFreq (String entry) {
        return this.key2count.containsKey(entry) ? key2count.getInt(entry) : 0;
    }

    public Object[] toArray () {
        return this.key2val.keySet().toArray(new Object[this.key2val.keySet().size()]);
    }

    public void reIndex () {
        //Object[] keys = this.key2val.keys();
        Object[] keys = (Object[]) this.key2val.keySet().toArray(new Object[this.key2val.keySet().size()]);
        this.key2val = new Object2IntOpenHashMap();
        this.val2key = new Int2ObjectOpenHashMap();
        Object2IntOpenHashMap key2countNew = new Object2IntOpenHashMap();
        for (int i = 0; i < keys.length; i++) {
            int val = i+1;
            this.key2val.put(keys[i], val);
            this.val2key.put(val, keys[i]);
            int count = 0;
            if (this.key2count.containsKey(keys[i])) {
                count = this.key2count.getInt(keys[i]);
            }
            key2countNew.put(keys[i], count);
        }
        this.key2count = key2countNew;
    }

    public boolean contains (Object entry) {
        return key2val.containsKey (entry);
    }

    public int size () {
        return numEntries;
    }

    public void stopGrowth () {
        growthStopped = true;
        //key2val.compact();
        //val2key.compact();
    }

    public void allowGrowth () {
        growthStopped = false;
    }

    public boolean growthStopped () {
        return growthStopped;
    }

    public ObjectIterator<String> iterator() {
        return this.key2val.keySet().iterator();
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
        this.key2val = (Object2IntOpenHashMap) in.readObject();
        this.growthStopped = in.readBoolean();
        // create val2key
        this.val2key = new Int2ObjectOpenHashMap ();
        //System.err.println("HOGE");
        for (ObjectIterator<String> it = this.key2val.keySet().iterator(); it.hasNext();) {
            String key = it.next();
            int val = this.key2val.getInt(key);
            this.val2key.put(val, key);
        }
    }

    public static void main (String[] args) {

        AlphabetTrieFU alp = new AlphabetTrieFU();
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
