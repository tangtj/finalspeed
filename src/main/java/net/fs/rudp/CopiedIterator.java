// Copyright (c) 2015 D1SM.net

package net.fs.rudp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * @author TANG
 */
public class CopiedIterator<E> implements Iterator<E> {
    private Iterator<E> iterator;

    public CopiedIterator(Iterator<E> itr) {
        ArrayList<E> list = new ArrayList<>();
        while (itr.hasNext()) {
            list.add(itr.next());
        }
        this.iterator = list.iterator();
    }

    @Override
    public boolean hasNext() {
        return this.iterator.hasNext();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("This is a read-only iterator.");
    }

    @Override
    public E next() {
        return this.iterator.next();
    }
}
