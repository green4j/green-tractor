package org.green.cproc.example;

import org.green.cproc.Entry;

public class MyEntry extends Entry {
    public long value;

    @Override
    public String toString() {
        return "MyEntry{" + "value=" + value + '}';
    }
}