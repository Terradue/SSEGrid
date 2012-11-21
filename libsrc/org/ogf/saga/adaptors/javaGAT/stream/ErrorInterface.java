package org.ogf.saga.adaptors.javaGAT.stream;

public interface ErrorInterface {

    public void signalReaderException(StreamExceptionalSituation e);

    // public void signalReaderConnectionDropped();

}
