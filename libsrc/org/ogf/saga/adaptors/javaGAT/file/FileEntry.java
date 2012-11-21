package org.ogf.saga.adaptors.javaGAT.file;

import java.io.InputStream;
import java.io.OutputStream;

import org.gridlab.gat.GAT;
import org.gridlab.gat.GATContext;
import org.gridlab.gat.GATInvocationException;
import org.gridlab.gat.GATObjectCreationException;
import org.gridlab.gat.URI;
import org.ogf.saga.adaptors.javaGAT.namespace.NSEntryAdaptor;
import org.ogf.saga.error.AlreadyExistsException;
import org.ogf.saga.error.AuthenticationFailedException;
import org.ogf.saga.error.AuthorizationFailedException;
import org.ogf.saga.error.BadParameterException;
import org.ogf.saga.error.DoesNotExistException;
import org.ogf.saga.error.IncorrectURLException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.error.PermissionDeniedException;
import org.ogf.saga.error.TimeoutException;
import org.ogf.saga.impl.session.SessionImpl;
import org.ogf.saga.url.URL;

// Make protected fields from NSEntry available for this package.
class FileEntry extends NSEntryAdaptor {

    FileEntry(SessionImpl sessionImpl, URL name, int flags)
            throws NotImplementedException, IncorrectURLException,
            BadParameterException, DoesNotExistException,
            PermissionDeniedException, AuthorizationFailedException,
            AuthenticationFailedException, TimeoutException,
            NoSuccessException, AlreadyExistsException {
        super(null, sessionImpl, name, flags);
    }

    GATContext getGatContext() {
        return gatContext;
    }

    URI getGatURI() {
        return gatURI;
    }

    long size() throws NoSuccessException {
        try {
            return file.length();
        } catch (GATInvocationException e) {
            throw new NoSuccessException("Got exception", e);
        }
    }

    InputStream getInputStream() throws NoSuccessException {
        try {
            return GAT.createFileInputStream(gatContext, gatURI);
        } catch (GATObjectCreationException e) {
            throw new NoSuccessException("Could not create input stream", e,
                    wrapper);
        }
    }

    OutputStream getOutputStream(boolean append) throws NoSuccessException {
        try {
            return GAT.createFileOutputStream(gatContext, gatURI, append);
        } catch (GATObjectCreationException e) {
            throw new NoSuccessException("Could not create output stream", e,
                    wrapper);
        }
    }
}
