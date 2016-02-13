/* Copyright (C) 2003 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package jp.ac.tohoku.ecei.cl.www.util;

import gnu.trove.iterator.*;
import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.hash.*;

import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class MObjectObject2IntMap<E,F> implements Serializable {

    private HashMap<E, TObjectIntHashMap<F>> backing = new HashMap<E, TObjectIntHashMap<F>> ();

    public MObjectObject2IntMap () { }

    public void put (E key1, F key2, int value) {
        if (!backing.containsKey (key1)) {
            TObjectIntHashMap<F> inner = new TObjectIntHashMap<F>();
            backing.put (key1, inner);
        }
        TObjectIntHashMap<F> inner = (TObjectIntHashMap<F>) backing.get (key1);
        inner.put(key2, value);
    }

    public int get (E key1, F key2) {
        TObjectIntHashMap<F> inner = (TObjectIntHashMap<F>) backing.get (key1);
        if (inner == null) {
            return -1;
        } else {
            return inner.get (key2);
        }
    }

    public boolean containsKey (E key1, F key2) {
        TObjectIntHashMap<F> inner = (TObjectIntHashMap<F>) backing.get (key1);
        if (inner == null) {
            return false;
        } else {
            return inner.containsKey (key2);
        }
    }

    /** Returns an array of first-level keys. */
    public Object[] keys1 () {
        ArrayList keys1Ary = new ArrayList();
        for (Iterator it = backing.keySet().iterator(); it.hasNext();) {
            keys1Ary.add((E) it.next());
        }
        return keys1Ary.toArray();
    }

    public Object[] keys2 (E key1)
    {
        TObjectIntHashMap<F> inner = (TObjectIntHashMap<F>) backing.get (key1);
        ArrayList keys2Ary = new ArrayList();
        for (TObjectIntIterator it = inner.iterator(); it.hasNext();) {
            it.advance();
            keys2Ary.add(it.key());
        }
        return keys2Ary.toArray();
    }

}
