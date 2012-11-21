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
import org.ogf.saga.proxies.file.FileOutputStreamWrapper;
import org.ogf.saga.url.URL;

public class FileOutputStreamAdaptor extends
        org.ogf.saga.spi.file.FileOutputStreamAdaptorBase {

    static {
        Initialize.initialize();
    }

    private org.gridlab.gat.io.FileOutputStream out;

    public FileOutputStreamAdaptor(FileOutputStreamWrapper wrapper,
            SessionImpl sessionImpl, URL source, boolean append)
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
            out = GAT.createFileOutputStream(gatContext, gatURI, append);
        } catch (GATObjectCreationException e) {
            throw new NoSuccessException("Could not create output stream", e);
        }
    }
    
    private void checkNotClosed() throws IOException {
        if (out == null) {
            throw new IOException("Stream was closed");
        }
    }

    public Object clone() throws CloneNotSupportedException {
        FileOutputStreamAdaptor clone = (FileOutputStreamAdaptor) super.clone();
        clone.setWrapper(clone.wrapper);
        return clone;
    }

    public void write(int b) throws IOException {
        checkNotClosed();
        out.write(b);
    }

    public void close() throws IOException {
        if (out != null) {
            out.close();
        }
        out = null;
    }

    public void flush() throws IOException {
        checkNotClosed();
        out.flush();
    }

    public void write(byte[] b, int off, int len) throws IOException {
        checkNotClosed();
        out.write(b, off, len);
    }

    public void write(byte[] b) throws IOException {
        checkNotClosed();
        out.write(b);
    }
}
