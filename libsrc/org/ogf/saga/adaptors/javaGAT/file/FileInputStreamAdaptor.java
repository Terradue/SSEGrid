package org.ogf.saga.adaptors.javaGAT.file;

import java.io.IOException;
import java.net.URISyntaxException;

import org.gridlab.gat.GAT;
import org.gridlab.gat.GATContext;
import org.gridlab.gat.GATObjectCreationException;
import org.gridlab.gat.URI;
import org.ogf.saga.adaptors.javaGAT.util.GatURIConverter;
import org.ogf.saga.adaptors.javaGAT.util.Initialize;
import org.ogf.saga.error.IncorrectURLException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.impl.session.SessionImpl;
import org.ogf.saga.proxies.file.FileInputStreamWrapper;
import org.ogf.saga.url.URL;

public class FileInputStreamAdaptor extends
        org.ogf.saga.spi.file.FileInputStreamAdaptorBase {

    static {
        Initialize.initialize();
    }

    private org.gridlab.gat.io.FileInputStream in;

    public FileInputStreamAdaptor(FileInputStreamWrapper wrapper,
            SessionImpl sessionImpl, URL source)
            throws IncorrectURLException, NoSuccessException {

        super(sessionImpl, wrapper);

        org.ogf.saga.adaptors.javaGAT.session.Session gatSession;

        synchronized (sessionImpl) {
            gatSession = (org.ogf.saga.adaptors.javaGAT.session.Session) sessionImpl
                    .getAdaptorSession("JavaGAT");
            if (gatSession == null) {
                gatSession = new org.ogf.saga.adaptors.javaGAT.session.Session();
                sessionImpl.putAdaptorSession("JavaGAT", gatSession);
            }
        }

        GATContext gatContext = gatSession.getGATContext();

        URI gatURI;
        try {
            gatURI = GatURIConverter.cvtToGatURI(source);
        } catch (URISyntaxException e1) {
            throw new IncorrectURLException(e1);
        }

        try {
            in = GAT.createFileInputStream(gatContext, gatURI);
        } catch (GATObjectCreationException e) {
            throw new NoSuccessException("Could not create input stream", e);
        }
    }
    
    private void checkNotClosed() throws IOException {
        if (in == null) {
            throw new IOException("Stream was closed");
        }
    }

    public int read() throws IOException {
        checkNotClosed();
        return in.read();
    }

    public int available() throws IOException {
        checkNotClosed();
        return in.available();
    }

    public void close() throws IOException {
        if (in != null) {
            in.close();
        }
        in = null;
    }

    public void mark(int arg0) {
        if (in != null) {
            in.mark(arg0);
        }
    }

    public boolean markSupported() {
        if (in != null) {
            return in.markSupported();
        }
        return false;
    }

    public int read(byte[] arg0, int arg1, int arg2) throws IOException {
        checkNotClosed();
        return in.read(arg0, arg1, arg2);
    }

    public int read(byte[] arg0) throws IOException {
        checkNotClosed();
        return in.read(arg0);
    }

    public void reset() throws IOException {
        checkNotClosed();
        in.reset();
    }

    public long skip(long arg0) throws IOException {
        checkNotClosed();
        return in.skip(arg0);
    }

    public Object clone() throws CloneNotSupportedException {
        FileInputStreamAdaptor clone = (FileInputStreamAdaptor) super.clone();
        clone.setWrapper(clone.wrapper);
        return clone;
    }
}
