package org.ogf.saga.adaptors.javaGAT.stream;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gridlab.gat.io.Pipe;
import org.ogf.saga.buffer.Buffer;
import org.ogf.saga.error.BadParameterException;
import org.ogf.saga.error.IncorrectStateException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.error.SagaIOException;
import org.ogf.saga.impl.monitoring.MetricImpl;
import org.ogf.saga.stream.StreamState;

public class StreamListener implements Runnable {

    private boolean closed = false;
    private CircularBuffer buf;
    private MetricImpl streamRead;
    private ErrorInterface err;

    private static Logger logger = LoggerFactory
            .getLogger(StreamListener.class);

    public StreamListener(Pipe pipe, MetricImpl streamRead, int bufferCapacity,
            ErrorInterface err) {
        this.streamRead = streamRead;
        this.buf = new CircularBuffer(pipe, bufferCapacity);
        this.err = err;
    }

    public void run() {
        logger.debug("Stream Listener: Start");
        StreamExceptionalSituation error = new StreamExceptionalSituation();
        try {
            while (!closed) {
                int oldSize = buf.getSize();
                int bytesRead = buf.readFromStream();

                // note: here we can also get dropped connection notification
                // but in this situation we cannot distinguish it so we
                // go into ERROR state

                if (bytesRead == -1) {
                    error
                            .setCause(new NoSuccessException(
                                    "Connection dropped"));
                    error.setTargetState(StreamState.DROPPED);
                    err.signalReaderException(error);
                    buf.onError(CircularBuffer.REASON_DROPPED);
                    logger.debug("Stream Listener: Connection dropped");
                    break;
                } else if (oldSize == 0) {
                    streamRead.internalFire();
                }
            }
        } catch (InterruptedException e) {
            logger.debug("Stream Listener: Interrupted exception");
            // silently finish job
            closed = true;
            buf.onError(CircularBuffer.REASON_CLOSED);
            // try {
            // StreamStateUtils.setStreamState(streamState,
            // StreamState.ERROR);
            // } catch (NoSuccess ee) {
            // }
        } catch (IOException e) {
            // There is a problem: we cannot tell if the read
            // was unsuccessful because of closing socket or other error

            logger.debug("IO exception", e);
            buf.onError(CircularBuffer.REASON_ERROR);
            error.setCause(e);
            error.setTargetState(StreamState.ERROR);
            err.signalReaderException(error);

            // try {
            // StreamStateUtils.setStreamState(streamState, StreamState.ERROR);
            // } catch (NoSuccess ee) {
            // }
        }
        logger.debug("Listener Exit");

    }

    // No arguments' validation in this method

    public int read(Buffer buffer, int len) throws IncorrectStateException,
            NoSuccessException, BadParameterException, NotImplementedException,
            SagaIOException {
        return buf.read(buffer, len);
    }

    public boolean isEmpty() {
        return buf.getSize() == 0;
    }

    InputStream getInputStream() {
        return buf;
    }
}
