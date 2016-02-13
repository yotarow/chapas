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

import gnu.trove.iterator.*;
import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.hash.*;

import java.util.ArrayList;
import java.io.*;
import java.util.Iterator;
import java.util.HashMap;
import java.rmi.dgc.VMID;

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.*;

/**
 *  A mapping between integers and objects where the mapping in each
 * direction is efficient.  Integers are assigned consecutively, starting
 * at zero, as objects are added to the Alphabet.  Objects can not be
 * deleted from the Alphabet and thus the integers are never reused.
 * <p>
 * The most common use of an alphabet is as a dictionary of feature names
 * associated with a {@link edu.umass.cs.mallet.base.types.FeatureVector} in an
 * {@link edu.umass.cs.mallet.base.types.Instance}. In a simple document
 * classification usage,
 * each unique word in a document would be a unique entry in the Alphabet
 * with a unique integer associated with it.   FeatureVectors rely on
 * the integer part of the mapping to efficiently represent the subset of
 * the Alphabet present in the FeatureVector.
 * @see FeatureVector
 * @see Instance
 * @see edu.umass.cs.mallet.base.pipe.Pipe
 */
public class AlphabetFU implements Serializable
{
    Object2IntOpenHashMap map;
    ArrayList entries;
    boolean growthStopped = false;
    Class entryClass = null;
    VMID instanceId = new VMID();  //used in readResolve to identify persitent instances

    public AlphabetFU (Class entryClass)
    {
        this.map = new Object2IntOpenHashMap ();
        this.entries = new ArrayList ();
        this.entryClass = entryClass;
        // someone could try to deserialize us into this image (e.g., by RMI).  Handle this.
        deserializedEntries.put (instanceId, this);
    }

    public AlphabetFU ()
    {
        this (null);
    }

    /** Return -1 if entry isn't present. */
    public int lookupIndex (Object entry, boolean addIfNotPresent)
    {
        if (entry == null)
            throw new IllegalArgumentException ("Can't lookup \"null\" in an AlphabetFU.");
        if (entryClass == null)
            entryClass = entry.getClass();
        else {
            // Insist that all entries in the AlphabetFU are of the same
            // class.  This may not be strictly necessary, but will catch a
            // bunch of easily-made errors.
            if (entry.getClass() != entryClass) {
                //System.out.println("entry.getClass() = " + entry.getClass());
                //System.out.println("entryClass       = " + entryClass);
                throw new IllegalArgumentException ("Non-matching entry class, "+entry.getClass()+", was "+entryClass);
            }
        }
        int retIndex = -1;
        if (map.containsKey( entry )) {
            retIndex = map.get( entry );
        }
        else if (!growthStopped && addIfNotPresent) {
            synchronized (this) {
                retIndex = entries.size();
                map.put (entry, retIndex);
                entries.add (entry);
            }
        }
        return retIndex;
    }

    public int lookupIndex (Object entry)
    {
        return lookupIndex (entry, true);
    }

    public Object lookupObject (int index)
    {
        return entries.get(index);
    }

    public Object[] toArray () {
        return entries.toArray();
    }

    /**
     * Returns an array containing all the entries in the AlphabetFU.
     *  The runtime type of the returned array is the runtime type of in.
     *  If in is large enough to hold everything in the alphabet, then it
     *  it used.  The returned array is such that for all entries <tt>obj</tt>,
     *  <tt>ret[lookupIndex(obj)] = obj</tt> .
     */ 
    public Object[] toArray (Object[] in) {
        return entries.toArray (in);
    }

    // xxx This should disable the iterator's remove method...
    public Iterator iterator () {
        return entries.iterator();
    }

    public Object[] lookupObjects (int[] indices)
    {
        Object[] ret = new Object[indices.length];
        for (int i = 0; i < indices.length; i++)
            ret[i] = entries.get(indices[i]);
        return ret;
    }

    /**
     * Returns an array of the objects corresponding to
     * @param indices An array of indices to look up
     * @param buf An array to store the returned objects in.
     * @return An array of values from this AlphabetFU.  The runtime type of the array is the same as buf
     */
    public Object[] lookupObjects (int[] indices, Object[] buf)
    {
        for (int i = 0; i < indices.length; i++)
            buf[i] = entries.get(indices[i]);
        return buf;
    }

    public int[] lookupIndices (Object[] objects, boolean addIfNotPresent)
    {
        int[] ret = new int[objects.length];
        for (int i = 0; i < objects.length; i++)
            ret[i] = lookupIndex (objects[i], addIfNotPresent);
        return ret;
    }

    public boolean contains (Object entry)
    {
        return map.containsKey (entry);
    }

    public int size ()
    {
        return entries.size();
    }

    public void stopGrowth ()
    {
        growthStopped = true;
    }

    public void startGrowth ()
    {
        growthStopped = false;
    }

    public boolean growthStopped ()
    {
        return growthStopped;
    }

    public Class entryClass ()
    {
        return entryClass;
    }

    /** Return String representation of all AlphabetFU entries, each
        separated by a newline. */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < entries.size(); i++) {
            sb.append (entries.get(i).toString());
            sb.append ('\n');
        }
        return sb.toString();
    }

    public void dump () { dump (System.out); }

    public void dump (PrintStream out)
    {
        dump (new PrintWriter (new OutputStreamWriter (out), true));
    }

    public void dump (PrintWriter out)
    {
        for (int i = 0; i < entries.size(); i++) {
            out.println (i+" => "+entries.get (i));
        }
    }

    public VMID getInstanceId() { return instanceId;} // for debugging
    public void setInstanceId(VMID id) { this.instanceId = id; }
    // Serialization

    private static final long serialVersionUID = 1;
    private static final int CURRENT_SERIAL_VERSION = 1;

    private void writeObject (ObjectOutputStream out) throws IOException {
        out.writeInt (CURRENT_SERIAL_VERSION);
        out.writeInt (entries.size());
        for (int i = 0; i < entries.size(); i++)
            out.writeObject (entries.get(i));
        out.writeBoolean (growthStopped);
        out.writeObject (entryClass);
        out.writeObject(instanceId);
    }

    private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt ();
        int size = in.readInt();
        entries = new ArrayList ();
        map = new Object2IntOpenHashMap ();
        for (int i = 0; i < size; i++) {
            Object o = in.readObject();
            map.put (o, i);
            entries. add (o);
        }
        growthStopped = in.readBoolean();
        entryClass = (Class) in.readObject();
        if (version >0 ){ // instanced id added in version 1S
            instanceId = (VMID) in.readObject();
        }
    }

    private transient static HashMap deserializedEntries = new HashMap();
    /**
     * This gets called after readObject; it lets the object decide whether
     * to return itself or return a previously read in version.
     * We use a hashMap of instanceIds to determine if we have already read
     * in this object.
     * @return
     * @throws ObjectStreamException
     */

    public Object readResolve() throws ObjectStreamException {
        Object previous = deserializedEntries.get(instanceId);
        if (previous != null){
            //System.out.println(" ***AlphabetFU ReadResolve:Resolving to previous instance. instance id= " + instanceId);
            return previous;
        }
        if (instanceId != null){
            deserializedEntries.put(instanceId, this);
        }
        //System.out.println(" *** AlphabetFU ReadResolve: new instance. instance id= " + instanceId);
        return this;
    }
}
