package org.ogf.saga.adaptors.javaGAT.stream;

import org.ogf.saga.stream.StreamState;

public class StreamExceptionalSituation {

    private Exception cause;

    private StreamState targetState;

    public Exception getCause() {
        return cause;
    }

    public void setCause(Exception cause) {
        this.cause = cause;
    }

    public StreamState getTargetState() {
        return targetState;
    }

    public void setTargetState(StreamState targetState) {
        this.targetState = targetState;
    }

}
