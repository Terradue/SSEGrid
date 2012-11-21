package org.ogf.saga.adaptors.javaGAT.stream;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gridlab.gat.GAT;
import org.gridlab.gat.GATContext;
import org.gridlab.gat.GATInvocationException;
import org.gridlab.gat.GATObjectCreationException;
import org.gridlab.gat.URI;
import org.gridlab.gat.advert.AdvertService;
import org.gridlab.gat.io.Endpoint;
import org.gridlab.gat.io.Pipe;
import org.ogf.saga.adaptors.javaGAT.util.GatURIConverter;
import org.ogf.saga.adaptors.javaGAT.util.Initialize;
import org.ogf.saga.buffer.Buffer;
import org.ogf.saga.context.Context;
import org.ogf.saga.context.ContextFactory;
import org.ogf.saga.engine.SAGAEngine;
import org.ogf.saga.error.AuthenticationFailedException;
import org.ogf.saga.error.AuthorizationFailedException;
import org.ogf.saga.error.BadParameterException;
import org.ogf.saga.error.DoesNotExistException;
import org.ogf.saga.error.IncorrectStateException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.error.PermissionDeniedException;
import org.ogf.saga.error.SagaException;
import org.ogf.saga.error.SagaIOException;
import org.ogf.saga.error.TimeoutException;
import org.ogf.saga.impl.monitoring.MetricImpl;
import org.ogf.saga.impl.session.SessionImpl;
import org.ogf.saga.proxies.stream.StreamWrapper;
import org.ogf.saga.spi.stream.StreamAdaptorBase;
import org.ogf.saga.stream.Activity;
import org.ogf.saga.stream.StreamInputStream;
import org.ogf.saga.stream.StreamOutputStream;
import org.ogf.saga.stream.StreamState;
import org.ogf.saga.url.URL;
import org.ogf.saga.url.URLFactory;

public class StreamAdaptor extends StreamAdaptorBase implements ErrorInterface {

    static {
        Initialize.initialize();
    }

    private GATContext gatContext;
    private Pipe pipe;
    private boolean wasOpen = false;
    private Thread listeningReaderThread;
    private StreamListener listeningReader;
    private StreamExceptionalSituation streamListenerException = null;

    private static float MINIMAL_TIMEOUT = 0.001f;
    private static int NUM_WAIT_TRIES = 10;

    private static Logger logger = LoggerFactory.getLogger(StreamAdaptor.class);

    public StreamAdaptor(StreamWrapper wrapper, SessionImpl sessionImpl, URL url)
            throws NotImplementedException, BadParameterException {
        super(wrapper, sessionImpl, url);
        gatContext = initializeGatContext(sessionImpl);
    }

    public Object clone() throws CloneNotSupportedException {
        StreamAdaptor clone = (StreamAdaptor) super.clone();
        synchronized (clone) {
            clone.streamListenerException = null;
        }
        clone.gatContext = (GATContext) this.gatContext.clone();
        clone.pipe = this.pipe;

        // only if the stream is in Open state we create listening thread

        try {
            if (streamState.getAttribute(MetricImpl.VALUE).equals(
                    StreamState.OPEN.toString())) {
                clone.listeningReader = new StreamListener(clone.pipe,
                        clone.streamRead, 1024, this);
                clone.listeningReaderThread = new Thread(clone.listeningReader,
                        "clientListener");
                clone.listeningReaderThread.start();
            } else {
                clone.listeningReader = null;
                clone.listeningReaderThread = null;
            }
        } catch (SagaException e) {
            // fatal error but we cannot do anything about it
            clone.listeningReader = null;
            clone.listeningReaderThread = null;
        }

        return clone;

    }

    public void close(float timeoutInSeconds) throws NotImplementedException,
            NoSuccessException {

        try {
            if (listeningReader != null) {
                listeningReaderThread.interrupt();
                pipe.close();
            }
            if (!StreamStateUtils.isFinalState(streamState)) {
                StreamStateUtils
                        .setStreamState(streamState, StreamState.CLOSED);
                onStateChange(StreamState.CLOSED);
            }
        } catch (GATInvocationException e) {
            throw new NoSuccessException(e);
        }
    }

    public void connect(float timeoutInSeconds) throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, IncorrectStateException,
            TimeoutException, NoSuccessException {

        // on any failure we change state to Error

        try {
            StreamStateUtils.checkStreamState(streamState, StreamState.NEW);
            logger.debug("Successful check for OPEN state [CONNECT]");
        } catch (IncorrectStateException e) {
            logger.debug("Unsuccessful check for OPEN state [CONNECT]");
            if (!StreamStateUtils.isFinalState(streamState)) {
                StreamStateUtils.setStreamState(streamState, StreamState.ERROR);
                onStateChange(StreamState.ERROR);
            }
            throw e;
        } catch (NoSuccessException e) {
            logger.debug("Other error while verifying OPEN state [CONNECT]");
            StreamStateUtils.setStreamState(streamState, StreamState.ERROR);
            onStateChange(StreamState.ERROR);
            throw e;
        }
        
        /*
         * long deadline = 0;
        
        if (timeoutInSeconds < 0.0)
            deadline = -1;
        else if (timeoutInSeconds > 0.0) {
            deadline = Math.round(timeoutInSeconds * 1000) + System.currentTimeMillis();
        }
        */

        // Obtain advert service.
        AdvertService advService;
        try {
            advService = GAT.createAdvertService(gatContext);
        } catch (GATObjectCreationException e) {
            StreamStateUtils.setStreamState(streamState, StreamState.ERROR);
            onStateChange(StreamState.ERROR);
            throw new NoSuccessException(e);
        }
        
        // Convert URL to advertName.
        URI db;
        try {
            db = GatURIConverter.cvtToGatURI(URLFactory
                    .createURL(MY_FACTORY, getAdvertName(gatContext)));
        } catch (BadParameterException e) {
            StreamStateUtils.setStreamState(streamState, StreamState.ERROR);
            onStateChange(StreamState.ERROR);
            throw new NoSuccessException(
                    "Incorrect URL for javagat advert service?", e);
        } catch (URISyntaxException e) {
            StreamStateUtils.setStreamState(streamState, StreamState.ERROR);
            onStateChange(StreamState.ERROR);
            throw new NoSuccessException(
                    "Incorrect URL for javagat advert service?", e);
        }

        // long startTime = System.currentTimeMillis();
        
        String path = url.getString();
        logger.debug("URL = " + url + ", path = " + path);
               
        for (;;) {
            try {
                logger.debug("Importing database ...");
                advService.importDataBase(db);
                logger.debug("Obtaining remote endpoint ...");
                Endpoint remoteEndPoint =
                    (Endpoint) advService.getAdvertisable(path);
                pipe = remoteEndPoint.connect();
                StreamStateUtils.setStreamState(streamState, StreamState.OPEN);
                onStateChange(StreamState.OPEN);
                wasOpen = true;
                this.listeningReader = new StreamListener(pipe, streamRead, 1024,
                        this);
                this.listeningReaderThread = new Thread(this.listeningReader);
                this.listeningReaderThread.start();
                return;
            } catch (GATInvocationException e) {
                StreamStateUtils.setStreamState(streamState, StreamState.ERROR);
                onStateChange(StreamState.ERROR);
                throw new NoSuccessException("GAT error", e);
            } catch (NoSuchElementException e) {
                // No, not correct, the server socket should at least exist and
                // be accepting. The timeout is only for setting up the connection.
                // Unfortunately, JavaGAT does not support that.
                /*
                long time = System.currentTimeMillis();
                if (deadline < 0 || deadline > time) {
                    try {
                        Thread.sleep(1000L);
                    } catch(Throwable e1) {
                        // ignored
                    }
                    continue;
                }
                */
                StreamStateUtils.setStreamState(streamState, StreamState.ERROR);
                onStateChange(StreamState.ERROR);
                throw new NoSuccessException("Incorrect entry information", e);
            }
        }
    }

    public Context getContext() throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, IncorrectStateException,
            TimeoutException, NoSuccessException {
        if (!wasOpen)
            throw new IncorrectStateException("This stream was never opened");

        return ContextFactory.createContext(MY_FACTORY, "Unknown");
    }

    public URL getUrl() throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, IncorrectStateException,
            TimeoutException, NoSuccessException {
        return url;
    }

    public int read(Buffer buf, int len) throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, BadParameterException,
            IncorrectStateException, TimeoutException, NoSuccessException,
            SagaIOException {

        StreamStateUtils.checkStreamState(streamState, StreamState.OPEN);

        if (len < 0)
            throw new BadParameterException("Length should be non-negative");

        int bytesRead = 0;

        try {
            bytesRead = listeningReader.read(buf, len);
        } catch (NoSuccessException e) {
            StreamStateUtils.setStreamState(streamState, StreamState.ERROR);
            onStateChange(StreamState.ERROR);
            throw e;
        }

        // we have successfully read some data but there are is no data left
        // and there has been an exceptional situation at the buffer in the
        // meantime
        // so that it is unable to gather more data

        synchronized (this) {

            if (streamListenerException != null && listeningReader.isEmpty()) {
                StreamStateUtils.setStreamState(streamState,
                        streamListenerException.getTargetState());
                onStateChange(streamListenerException.getTargetState());
                // it should be dropped metric detection
                // we shouldn't throw this as our invocation should return
                // successfully
                // throw new NoSuccess("connection problem",
                // streamListenerException);
            }
        }

        return bytesRead;
    }

    public int waitFor(int what, float timeoutInSeconds)
            throws NotImplementedException, AuthenticationFailedException,
            AuthorizationFailedException, PermissionDeniedException,
            IncorrectStateException, NoSuccessException {

        int cause = 0;
        float actualTimeout = 0.0f;
        int waitTime = 0;
        boolean forever = false;
        boolean once = false;

        if (timeoutInSeconds == 0.0f)
            once = true;
        else if (timeoutInSeconds < 0.0f) {
            forever = true;
            actualTimeout = 1.0f;
        } else if (timeoutInSeconds < MINIMAL_TIMEOUT * NUM_WAIT_TRIES)
            actualTimeout = MINIMAL_TIMEOUT * NUM_WAIT_TRIES;
        else
            actualTimeout = timeoutInSeconds;

        waitTime = (int) (actualTimeout * 1000);
        waitTime /= NUM_WAIT_TRIES;

        // System.out.println("WAIT: actualTimeout = " + actualTimeout);
        // System.out.println("WAIT: time waiting = " + waitTime);

        StreamStateUtils.checkStreamState(streamState, StreamState.OPEN);

        try {
            do {
                for (int i = 0; i < NUM_WAIT_TRIES; i++) {

                    if ((what & Activity.EXCEPTION.getValue()) != 0) {
                        if (streamState.getAttribute(MetricImpl.VALUE).equals(
                                StreamState.ERROR.toString()))
                            return Activity.EXCEPTION.getValue();
                    }

                    // can we tell that the stream can be read from ?
                    // when there is > 0 bytes we can
                    // when there is = 0 bytes, it may be readable and may not

                    if ((what & Activity.READ.getValue()) != 0) {
                        if (!listeningReader.isEmpty()) {
                            cause |= Activity.READ.getValue();
                            forever = false; // we got some information and
                            // we exit the loops
                            break;
                        }

                    }

                    if (once)
                        break;

                    Thread.sleep(waitTime);
                }
            } while (forever);
        } catch (SagaException e) {
            throw new NoSuccessException("waitFor", e);
        } catch (InterruptedException e) {
            throw new NoSuccessException("waitFor -- thread interrupted", e);
        }

        if ((what & Activity.WRITE.getValue()) != 0) {
            throw new NotImplementedException("waitFor: writeable");
        }

        return cause;
    }

    public int write(Buffer buf, int len) throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, BadParameterException,
            IncorrectStateException, TimeoutException, NoSuccessException,
            SagaIOException {

        StreamStateUtils.checkStreamState(streamState, StreamState.OPEN);

        byte[] data;
        try {
            data = buf.getData();
        } catch (DoesNotExistException e) {
            throw new BadParameterException("The buffer contains no data");
        }
        if (len > data.length) {
            len = data.length;
        } else if (len < 0) {
            len = data.length;
        }

        try {
            // outputstream write method doesn't give any info
            // how many bytes it has written
            pipe.getOutputStream().write(data, 0, len);
        } catch (GATInvocationException e) {
            IOException t = (IOException) e.getCause();
            if (t != null)
                throw new SagaIOException(t);
            throw new NoSuccessException(e);
        } catch (IOException e) {
            StreamStateUtils.setStreamState(streamState, StreamState.ERROR);
            onStateChange(StreamState.ERROR);
            throw new SagaIOException(e);
        }
        return len;
    }

    public String getGroup() throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, TimeoutException, NoSuccessException {
        throw new NotImplementedException();
    }

    public String getOwner() throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, TimeoutException, NoSuccessException {
        throw new NotImplementedException();
    }

    public void permissionsAllow(String id, int permissions)
            throws NotImplementedException, AuthenticationFailedException,
            AuthorizationFailedException, PermissionDeniedException,
            BadParameterException, TimeoutException, NoSuccessException {
        throw new NotImplementedException();
    }

    public boolean permissionsCheck(String id, int permissions)
            throws NotImplementedException, AuthenticationFailedException,
            AuthorizationFailedException, PermissionDeniedException,
            BadParameterException, TimeoutException, NoSuccessException {
        throw new NotImplementedException();
    }

    public void permissionsDeny(String id, int permissions)
            throws NotImplementedException, AuthenticationFailedException,
            AuthorizationFailedException, PermissionDeniedException,
            BadParameterException, TimeoutException, NoSuccessException {
        throw new NotImplementedException();
    }

    static GATContext initializeGatContext(SessionImpl sessionImpl) {
        org.ogf.saga.adaptors.javaGAT.session.Session gatSession;

        synchronized (sessionImpl) {
            gatSession = (org.ogf.saga.adaptors.javaGAT.session.Session) sessionImpl
                    .getAdaptorSession("JavaGAT");
            if (gatSession == null) {
                gatSession = new org.ogf.saga.adaptors.javaGAT.session.Session();
                sessionImpl.putAdaptorSession("JavaGAT", gatSession);
            }
        }

        return gatSession.getGATContext();
    }

    static String getAdvertName(GATContext gatContext) {
        String s = SAGAEngine.getProperty("saga.adaptor.javagat.advertService");
        if (s != null) {
            return s;
        }
        return "file://" + System.getProperty("user.home") + "/.GatAdvertDB";
    }

    // we can't just set state to ERROR in StreamListener
    // there could be data in the buffer that should be able to retrie
    // by a user before the error occured
    // if the buffer is empty now we set ERROR state immediately

    // it is synchronized because it shouldn't be possible to read
    // streamListenerException
    // value while invoking this method

    public synchronized void signalReaderException(StreamExceptionalSituation e) {
        this.streamListenerException = e;
        if (listeningReader.isEmpty()) {
            try {
                StreamStateUtils.setStreamState(streamState,
                        streamListenerException.getTargetState());
                onStateChange(streamListenerException.getTargetState());
                this.streamListenerException = null; // important to be
                // synchronized
            } catch (NoSuccessException ex) {
                // shouldn't happen
                logger.debug("oops");
            }
        }
    }

    private void onStateChange(StreamState newState) {
        streamState.internalFire();
        if (newState == StreamState.ERROR)
            streamException.internalFire();
        else if (newState == StreamState.DROPPED)
            streamDropped.internalFire();
    }

    public StreamInputStream getInputStream() throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, IncorrectStateException,
            TimeoutException, NoSuccessException, SagaIOException {
        StreamStateUtils.checkStreamState(streamState, StreamState.OPEN);
        try {
            return new org.ogf.saga.impl.stream.InputStream(sessionImpl, pipe
                    .getInputStream());
        } catch (GATInvocationException e) {
            throw new SagaIOException(e);
        }
    }

    public StreamOutputStream getOutputStream() throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, IncorrectStateException,
            TimeoutException, NoSuccessException, SagaIOException {
        StreamStateUtils.checkStreamState(streamState, StreamState.OPEN);
        try {
            return new org.ogf.saga.impl.stream.OutputStream(sessionImpl, pipe
                    .getOutputStream());
        } catch (GATInvocationException e) {
            throw new SagaIOException(e);
        }
    }
}
