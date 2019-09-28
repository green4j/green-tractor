package org.green.cproc.example;

import org.green.cproc.Command;
import org.green.cproc.DefaultExecutor;

import java.util.List;

public class MyExecutor extends DefaultExecutor<MyEntry, MyProcessListener> {
    public MyExecutor(final String name) {
        super(name);
    }

    @Override
    public void processEntry(final MyEntry entry) {
        System.out.println("Processing " + entry);
    }

    @Override
    protected void doStart() {
        System.out.println("Start");
    }

    @Override
    protected void doStop() {
        System.out.println("Stop");
    }

    @Override
    protected void doCustom(final long executionId, final Command command, final List<MyProcessListener> listeners) {
        System.out.println(command);

        if (command instanceof MySumCommand) {
            final MySumCommand myCommand = (MySumCommand) command;
            final int result = myCommand.a() + myCommand.b();

            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).onSum(executionId, this, result, null);
            }
            return;
        }

        if (command instanceof MyMultiplyCommand) {
            final MyMultiplyCommand myCommand = (MyMultiplyCommand) command;
            final int result = myCommand.a() * myCommand.b();

            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).onMultiply(executionId, this, result, null);
            }
            return;
        }

        throw new UnsupportedOperationException("Unknown command: " + command);
    }
}
