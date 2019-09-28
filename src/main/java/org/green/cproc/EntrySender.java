package org.green.cproc;

public interface EntrySender<E extends Entry> {
    EntryEnvelope<E> nextEnvelope();
}
