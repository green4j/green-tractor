package org.green.samples.cproc.myproc;

import org.green.cproc.Command;

public class MySumCommand extends Command {
    private int a;
    private int b;

    public int a() {
        return a;
    }

    public int b() {
        return b;
    }

    public void setA(final int a) {
        this.a = a;
    }

    public void setB(final int b) {
        this.b = b;
    }

    @Override
    public String toString() {
        return "Sum{" + "a=" + a + ", b=" + b + '}';
    }
}