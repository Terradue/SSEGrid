package org.ogf.saga.adaptors.javaGAT;

import java.io.File;

import org.ogf.saga.context.Context;
import org.ogf.saga.engine.SAGAEngine;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.impl.AdaptorBase;
import org.ogf.saga.impl.context.ContextImpl;
import org.ogf.saga.impl.context.ContextInitializerSPI;

public class ContextInitializerAdaptor extends AdaptorBase<Object> implements
        ContextInitializerSPI {

    public ContextInitializerAdaptor() {
        super(null, null);
    }

    public void setDefaults(ContextImpl context, String type)
            throws NotImplementedException {
        try {
            if ("ftp".equals(type)) {
                // Default is anonymous
                context.setValueIfEmpty(Context.USERID, "anonymous");
                context.setValueIfEmpty(Context.USERPASS,
                                "anonymous@localhost");
            } else if ("ssh".equals(type) || "sftp".equals(type)) {
                // setValue(Context.USERID, "");
                // setValue(Context.USERPASS, "");
                // setValue(Context.USERKEY, "");
            } else if ("globus".equals(type) || "gridftp".equals(type)
                    || "glite".equals(type)) {
                String proxy = System.getenv("X509_USER_PROXY");
                if (proxy == null) {
                    proxy = SAGAEngine.getProperty("x509.user.proxy");
                }
                if (proxy != null && new File(proxy).exists()) {
                    context.setValueIfEmpty(Context.USERPROXY, proxy);
                } else {
                    String home = System.getProperty("user.home");
                    String key = home + File.separator
                            + ".globus" + File.separator + "userkey.pem";
                    String cert = home + File.separator
                            + ".globus" + File.separator + "usercert.pem";
                    if (new File(key).exists()) {
                        context.setValueIfEmpty(Context.USERKEY, key);
                    }
                    if (new File(cert).exists()) {
                        context.setValueIfEmpty(Context.USERCERT, cert);
                    }
                }
                // attributes.setValue(Context.USERPASS, "");
            } else if ("preferences".equals(type)) {
                // nothing
            } else {
                throw new NotImplementedException(
                        "This adaptor does not recognize the " + type
                                + " context");
            }
        } catch (NotImplementedException e) {
            throw e;
        } catch (Throwable e) {
            // Should not happen.
            throw new Error("Internal error: got exception", e);
        }
    }
}
