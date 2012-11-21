package org.ogf.saga.adaptors.javaGAT.session;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import org.gridlab.gat.GAT;
import org.gridlab.gat.GATContext;
import org.gridlab.gat.URI;
import org.gridlab.gat.security.CertificateSecurityContext;
import org.gridlab.gat.security.CredentialSecurityContext;
import org.gridlab.gat.security.PasswordSecurityContext;
import org.gridlab.gat.security.SecurityContext;
import org.ogf.saga.adaptors.javaGAT.util.GatURIConverter;
import org.ogf.saga.error.DoesNotExistException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.impl.SagaObjectBase;
import org.ogf.saga.impl.context.ContextImpl;
import org.ogf.saga.url.URL;
import org.ogf.saga.url.URLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Question: where to call GAT.end() ??? When there are no sessions left?
// But the default session is never removed ???

/**
 * Corresponds to a JavaGat Context.
 */
public class Session implements
        org.ogf.saga.impl.session.AdaptorSessionInterface, Cloneable {
    
    private static final Logger logger = LoggerFactory.getLogger(Session.class);

    private static int numSessions = 0;

    private GATContext gatContext = new GATContext();

    private HashMap<ContextImpl, SecurityContext> contextImpls = new HashMap<ContextImpl, SecurityContext>();

    public Session() {
        synchronized (Session.class) {
            numSessions++;
        }
    }

    // This should not be public, but then everything needs to be in a single
    // package ...
    public GATContext getGATContext() {
        return gatContext;
    }

    public synchronized void addContext(ContextImpl contextImpl) throws NoSuccessException {

        try {
            if ("preferences"
                    .equals(contextImpl.getAttribute(ContextImpl.TYPE))) {
                String[] attribs = contextImpl.listAttributes();
                for (String s : attribs) {
                    // TODO: exclude all keys predefined in the Context
                    // interface???
                    try {
                        gatContext
                                .addPreference(s, contextImpl.getAttribute(s));
                    } catch (Exception e) {
                        // ignore
                    }
                }
                return;
            }
        } catch (Throwable e) {
            // ignore
        }

        if (!contextImpls.containsKey(contextImpl)) {
            SecurityContext c = cvt2GATSecurityContext(contextImpl);
            if (c != null) {
                gatContext.addSecurityContext(c);
                contextImpls.put(contextImpl, c);
            }
        }
    }

    public synchronized void close() {
        if (gatContext != null) {
            gatContext = null;
            synchronized (Session.class) {
                numSessions--;
                if (numSessions == 0) {
                    GAT.end();
                }
            }
        }
    }

    public synchronized Object clone() throws CloneNotSupportedException {
        Session clone = (Session) super.clone();
        synchronized (clone) {
            clone.gatContext = (GATContext) gatContext.clone();
            clone.contextImpls = new HashMap<ContextImpl, SecurityContext>(
                    contextImpls);
        }
        return clone;
    }

    public void close(float timeoutInSeconds) throws NotImplementedException {
        close();
    }

    public synchronized void removeContext(ContextImpl contextImpl)
            throws DoesNotExistException {

        SecurityContext c = contextImpls.remove(contextImpl);

        if (c != null) {
            gatContext.removeSecurityContext(c);
        }
    }

    private SecurityContext cvt2GATSecurityContext(ContextImpl ctxt) throws NoSuccessException {
        String type = ctxt.getValue(ContextImpl.TYPE);
        String userId = ctxt.getValue(ContextImpl.USERID);
        if (userId == null || userId.equals("")) {
            userId = System.getProperty("user.name");
        }
        if ("ftp".equals(type)) {
            SecurityContext c = new PasswordSecurityContext(
                    userId,
                    ctxt.getValue(ContextImpl.USERPASS));
            c.addNote("adaptors", "ftp");
            return c;
        } else if ("globus".equals(type) || "gridftp".equals(type) || "glite".equals(type)) {
            if ("glite".equals(type)) {
                String userVO = ctxt.getValue(ContextImpl.USERVO);
                if (userVO != null) {
                    gatContext.addPreference("VirtualOrganisation", userVO);
                }
                String server = ctxt.getValue(ContextImpl.SERVER);
                // Format of SERVER context:
                // voms://voms.grid.sara.nl:30014/O=dutchgrid/O=hosts/OU=sara.nl/CN=voms.grid.sara.nl
                if (server != null) {
                    try {
                        URL serverURL = URLFactory.createURL(SagaObjectBase.MY_FACTORY, server);
                        String scheme = serverURL.getScheme();
                        String host = serverURL.getHost();
                        int port = serverURL.getPort();
                        String hostDN = serverURL.getPath();
                        if (! "voms".equals(scheme)) {
                            throw new NoSuccessException("SERVER attribute has unrecognized scheme");
                        }
                        if (host == null) {
                            throw new NoSuccessException("SERVER attribute has no host");
                        }
                        if (hostDN == null) {
                            throw new NoSuccessException("SERVER attribute has no path");
                        }
                        gatContext.addPreference("vomsServerURL", host);
                        gatContext.addPreference("vomsServerPort", "" + port);
                        gatContext.addPreference("vomsHostDN", hostDN);
                    } catch(NoSuccessException e) {
                        throw e;
                    } catch (Throwable e) {
                        // Just continue.
                        logger.info("Error in SERVER attribute", e);
                        throw new NoSuccessException("Error in SERVER attribute", e);
                    }
                }
            }
            String proxy = ctxt.getValue(ContextImpl.USERPROXY);
            if (proxy != null) {
                // JavaGAT does not have a security context that refers to
                // a proxy file, but it does have a CredentialSecurityContext
                // that has the credential itself as a byte array. So, we
                // try to read the proxy file here.
                try {
                    long length = new File(proxy).length();
                    FileInputStream f = new FileInputStream(proxy);
                    byte[] buf = new byte[(int) length];
                    int len = f.read(buf, 0, buf.length);
                    if (len > 0) {
                        SecurityContext c = new CredentialSecurityContext(buf);
                        if ("glite".equals(type)) {
                            c.addNote("adaptors", "glite");
                        } else {
                            c.addNote("adaptors", "globus,wsgt4new,gt42");
                        }
                        return c;
                    }
                    logger.info("read from proxy file gave " + len);
                } catch (FileNotFoundException e) {
                    logger.info("Could not open proxy file " + proxy, e);
                } catch (IOException e) {
                    logger.info("Could not read proxy file " + proxy, e);
                }
            }
            try {
                URL key = URLFactory.createURL(SagaObjectBase.MY_FACTORY, ctxt.getValue(ContextImpl.USERKEY));
                URI keyURI = GatURIConverter.cvtToGatURI(key);
                URL cert = URLFactory.createURL(SagaObjectBase.MY_FACTORY, ctxt.getValue(ContextImpl.USERCERT));
                URI certURI = GatURIConverter.cvtToGatURI(cert);
                SecurityContext c = new CertificateSecurityContext(keyURI,
                        certURI, userId, ctxt.getValue(ContextImpl.USERPASS));
                if ("glite".equals(type)) {
                    c.addNote("adaptors", "glite");
                } else {
                    c.addNote("adaptors", "globus,wsgt4new,gt42");
                }
                return c;
            } catch (Throwable e) {
                // what to do? nothing?
            }
        } else if ("ssh".equals(type) || "sftp".equals(type)) {
            if (!ctxt.getValue(ContextImpl.USERKEY).equals("")) {
                try {
                    URL key = URLFactory.createURL(SagaObjectBase.MY_FACTORY, ctxt.getValue(ContextImpl.USERKEY));
                    URI keyURI = GatURIConverter.cvtToGatURI(key);
                    SecurityContext c = new CertificateSecurityContext(
                            keyURI, null, userId, ctxt.getValue(ContextImpl.USERPASS));
                    c.addNote("adaptors", "commandlinessh,sshtrilead,sftptrilead");
                    return c;
                } catch (Throwable e) {
                    // what to do? nothing?
                }
            } else if (!ctxt.getValue(ContextImpl.USERPASS).equals("")) {
                SecurityContext c = new PasswordSecurityContext(userId,
                        ctxt.getValue(ContextImpl.USERPASS));
                c.addNote("adaptors", "commandlinessh,sshtrilead,sftptrilead");
                return c;
            }
        }
        return null;
    }
}
