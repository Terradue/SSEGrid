package org.ogf.saga.adaptors.javaGAT.stream;

import org.ogf.saga.error.IncorrectStateException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.impl.monitoring.MetricImpl;
import org.ogf.saga.stream.StreamState;

public class StreamStateUtils {

    static boolean equalsStreamState(MetricImpl streamState, StreamState state)
            throws NoSuccessException {
        String val;
        try {
            val = streamState.getAttribute(MetricImpl.VALUE);
        } catch (Throwable e) {
            throw new NoSuccessException("Internal error", e);
        }
        return state.toString().equals(val);
    }

    static void checkStreamState(MetricImpl streamState, StreamState state)
            throws NoSuccessException, IncorrectStateException {
        String val;
        try {
            val = streamState.getAttribute(MetricImpl.VALUE);
        } catch (Throwable e) {
            throw new NoSuccessException("Internal error", e);
        }
        if (!state.toString().equals(val)) {
            throw new IncorrectStateException("Should have been in " + state
                    + " state, not in " + val);
        }
    }

    static void setStreamState(MetricImpl streamState, StreamState state)
            throws NoSuccessException {
        try {
            streamState.setValue(state.toString());
        } catch (Throwable e) {
            throw new NoSuccessException("Internal error", e);
        }
    }

    static boolean isFinalState(MetricImpl streamState)
            throws NoSuccessException {
        try {
            String val = streamState.getAttribute(MetricImpl.VALUE);
            return (val.equals(StreamState.DROPPED.toString())
                    || val.equals(StreamState.CLOSED.toString()) || val
                    .equals(StreamState.ERROR.toString()));
        } catch (Throwable e) {
            throw new NoSuccessException("Internal error", e);
        }
    }
}
