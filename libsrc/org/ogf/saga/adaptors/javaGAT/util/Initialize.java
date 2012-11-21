package org.ogf.saga.adaptors.javaGAT.util;

import org.ogf.saga.engine.SAGAEngine;

/**
 * Initialization for the system property gat.adaptor.path.
 */
public class Initialize {

    private static final String PROPNAME = "gat.adaptor.path";

    static {
        String gatAdaptorPath = System.getProperty(PROPNAME);
        if (gatAdaptorPath == null) {
            gatAdaptorPath = SAGAEngine.getProperty(PROPNAME);
            if (gatAdaptorPath != null) {
                System.setProperty(PROPNAME, gatAdaptorPath);
            }
        }
    }

    public synchronized static void initialize() {
        // Empty method, but calling it will make sure that the static
        // initializer gets executed.
    }
}
