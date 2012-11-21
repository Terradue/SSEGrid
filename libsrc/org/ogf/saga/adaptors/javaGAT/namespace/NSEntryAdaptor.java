package org.ogf.saga.adaptors.javaGAT.namespace;

import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gridlab.gat.GAT;
import org.gridlab.gat.GATContext;
import org.gridlab.gat.GATInvocationException;
import org.gridlab.gat.GATObjectCreationException;
import org.gridlab.gat.URI;
import org.gridlab.gat.io.File;
import org.gridlab.gat.io.FileInterface;
import org.ogf.saga.adaptors.javaGAT.util.Initialize;
import org.ogf.saga.adaptors.javaGAT.util.GatURIConverter;
import org.ogf.saga.error.AlreadyExistsException;
import org.ogf.saga.error.AuthenticationFailedException;
import org.ogf.saga.error.AuthorizationFailedException;
import org.ogf.saga.error.BadParameterException;
import org.ogf.saga.error.DoesNotExistException;
import org.ogf.saga.error.IncorrectStateException;
import org.ogf.saga.error.IncorrectURLException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.error.PermissionDeniedException;
import org.ogf.saga.error.TimeoutException;
import org.ogf.saga.impl.SagaRuntimeException;
import org.ogf.saga.impl.attributes.AttributesImpl;
import org.ogf.saga.impl.session.SessionImpl;
import org.ogf.saga.namespace.Flags;
import org.ogf.saga.proxies.namespace.NSEntryWrapper;
import org.ogf.saga.spi.namespace.NSEntryAdaptorBase;
import org.ogf.saga.url.URL;
import org.ogf.saga.url.URLFactory;

public class NSEntryAdaptor extends NSEntryAdaptorBase {

    private static Logger logger = LoggerFactory
            .getLogger(NSEntryAdaptor.class);

    static {
        Initialize.initialize();
    }

    protected File fileImpl;
    protected FileInterface file;
    protected GATContext gatContext;
    protected boolean isDirectory;
    protected URI gatURI;

    AttributesImpl attributesImpl = new AttributesImpl();

    public NSEntryAdaptor(NSEntryWrapper wrapper, SessionImpl sessionImpl,
            URL name, int flags) throws NotImplementedException,
            IncorrectURLException, BadParameterException,
            DoesNotExistException, PermissionDeniedException,
            AuthorizationFailedException, AuthenticationFailedException,
            TimeoutException, NoSuccessException, AlreadyExistsException {
        this(wrapper, sessionImpl, name, flags, false);
    }

    NSEntryAdaptor(NSEntryWrapper wrapper, SessionImpl sessionImpl, URL name,
            int flags, boolean isDir) throws NotImplementedException,
            IncorrectURLException, BadParameterException,
            DoesNotExistException, PermissionDeniedException,
            AuthorizationFailedException, AuthenticationFailedException,
            TimeoutException, NoSuccessException, AlreadyExistsException {
        super(wrapper, sessionImpl, name, flags);

        // Make sure that the "local" scheme enforces the local file adaptor.
        String scheme = name.getScheme();
        if ("local".equals(scheme)) {
            throw new IncorrectURLException("the javagat file/namespace adaptor does not recognize the local scheme");
        }
        org.ogf.saga.adaptors.javaGAT.session.Session gatSession;

        synchronized (sessionImpl) {
            gatSession = (org.ogf.saga.adaptors.javaGAT.session.Session) sessionImpl
                    .getAdaptorSession("JavaGAT");
            if (gatSession == null) {
                gatSession = new org.ogf.saga.adaptors.javaGAT.session.Session();
                sessionImpl.putAdaptorSession("JavaGAT", gatSession);
            }
        }

        gatContext = gatSession.getGATContext();
        try {
            gatURI = GatURIConverter.cvtToGatURI(getEntryURL());
        } catch (URISyntaxException e1) {
            throw new IncorrectURLException(e1);
        }

        try {
            fileImpl = GAT.createFile(gatContext, gatURI);
            file = fileImpl.getFileInterface();
        } catch (GATObjectCreationException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("GAT.createFile failed");
            }
            throw new NoSuccessException(e);
        }
        
        boolean exists = false;
        try {
            exists = file.exists();
        } catch(Throwable e) {
            // ignore ...
        }
        
        if (!Flags.CREATE.isSet(flags) && ! exists) {
            throw new DoesNotExistException(name.toString()
                    + " does not exist");
        }

        if (Flags.CREATE.isSet(flags)
                && Flags.EXCL.isSet(flags) && exists) {
            throw new AlreadyExistsException(name.toString()
                    + " already exists");
        }

        try {
            isDirectory = exists && file.isDirectory();
        } catch (Throwable e) {
            // TODO: isDirectory fails, is a problem for GAT http adaptor.
            // For now, ignore the exception and assume that it is not a
            // directory.
            // throw new NoSuccess(e);
            logger.debug("isDirectory() gave exception! Assume non-directory",
                    e);
            isDirectory = false;
        }
        if (isDirectory && !isDir) {
            throw new BadParameterException(name.toString()
                    + " indicates a directory");
        }
        if (isDir && exists && ! isDirectory) {
            throw new BadParameterException(name.toString()
                    + " does not indicate a directory");
        }
    
        // Check existence of parent dir, and create it if required.
        if (! exists && Flags.CREATE.isSet(flags)) {
            if (Flags.CREATEPARENTS.isSet(flags)) {
                if (isDir) {
                    try {
                        if (! file.mkdirs()) {
                            throw new NoSuccessException(
                                    "Failed to create directory: "
                                    + name.toString());
                        }
                        return;
                    } catch(GATInvocationException e) {
                        throw new NoSuccessException(e);
                    }
                }
                File parentFile;
                try {
                    parentFile = file.getParentFile();
                    if (parentFile != null && ! parentFile.exists()) {
                        parentFile.mkdirs();
                    }
                } catch (GATInvocationException e) {
                    throw new NoSuccessException(e);
                }
            }
            String message = null;
            try {
                if (!isDir) {
                    if (!file.createNewFile()) {
                        message = "Failed to create new file: " + name.toString();
                    } else {
                        return;
                    }
                } else {
                    if (!file.mkdir()) {
                        message = "Failed to create directory: " + name.toString();
                    } else {
                        return;
                    }
                }
                if (! Flags.CREATEPARENTS.isSet(flags)) {
                    File parentFile = file.getParentFile();
                    boolean parentExists = parentFile == null || parentFile.exists();
                    if (! parentExists) {
                        throw new DoesNotExistException("Parent file does not exist: "
                                + name.toString());
                    }
                }
                throw new NoSuccessException(message);
            } catch (GATInvocationException e) {
                throw new NoSuccessException(e);
            }
        }
    }

    public Object clone() throws CloneNotSupportedException {
        NSEntryAdaptor clone = (NSEntryAdaptor) super.clone();
        try {
            clone.gatURI = new URI(gatURI.toString());
        } catch (URISyntaxException e) {
            throw new SagaRuntimeException("Should not happen", e);
        }
        // Should we also clone fileImpl? I don't think it is needed.
        return clone;
    }

    void init(File fileImpl, FileInterface file, URL url) {
        this.file = file;
        this.fileImpl = fileImpl;
        setEntryURL(url);
        try {
            gatURI = GatURIConverter.cvtToGatURI(url);
        } catch(Throwable e) {
            logger.debug("Should not happen", e);
        }
    }
    
    public void close(float timeoutInSeconds) throws NotImplementedException,
            NoSuccessException {
        setClosed(true);
    }

    public void copy(URL target, int flags) throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, BadParameterException,
            IncorrectStateException, AlreadyExistsException, TimeoutException,
            NoSuccessException, IncorrectURLException, DoesNotExistException {
        nonResolvingCopy(resolve(target), flags);
    }
    
    private URI copyOrMoveChecks(URL target, int flags, String method) 
            throws IncorrectStateException, NoSuccessException,
            BadParameterException, AlreadyExistsException,
            IncorrectURLException, NotImplementedException,
            DoesNotExistException {
        File targetFile = null;
        URI targetURI = null;
        
        try {
            targetURI = GatURIConverter.cvtToGatURI(target);
        } catch (URISyntaxException e) {
            throw new BadParameterException(e, wrapper);
        }
        
        try {
            targetFile = GAT.createFile(gatContext, targetURI);

            if (logger.isDebugEnabled()) {
                logger.debug("targetFile: " + targetURI.toString());
            }
        } catch (GATObjectCreationException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Target file could not be created!");
            }
            throw new NoSuccessException(e, wrapper);
        }
        
        boolean targetFileExists = targetFile.exists();
        
        if (isDirectory && Flags.RECURSIVE.isSet(flags) && targetFileExists
                && targetFile.isFile()) {
            throw new AlreadyExistsException("cannot overwrite non-directory"
                    + targetFile.toGATURI().toString() + " with directory "
                    + fileImpl.toGATURI().toString(), wrapper);
        }
        // test whether target is in existing part of the name space
        File targetParentFile = null;
        boolean targetParentExists = false;
        try {
            if (targetFileExists && targetFile.isDirectory()) {
                targetParentFile = targetFile;
                targetParentExists = true;
                try {
                    targetURI = targetURI.setPath(targetURI.getPath() + "/" + file.getName());
                    targetFile = GAT.createFile(gatContext, targetURI);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Creating target "
                                + targetFile.getName());
                    }
                    targetFileExists = targetFile.exists();
                } catch (GATObjectCreationException e) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("targetFile not created!");
                    }
                    throw new NoSuccessException(e, wrapper);
                }
            } else {

            }
        } catch (GATInvocationException e) {
            throw new NoSuccessException(e, wrapper);
        }  catch (URISyntaxException e) {
            throw new BadParameterException(e, wrapper);
        }
        if (!Flags.OVERWRITE.isSet(flags) && targetFileExists) {
            throw new AlreadyExistsException("Target already exists: "
                    + targetFile.toGATURI().toString(), wrapper);
        }
        if (Flags.CREATEPARENTS.isSet(flags)) {
            if (targetParentFile == null) {
                targetParentFile = (org.gridlab.gat.io.File) targetFile.getParentFile();
            
                if (targetParentFile == null) {
                    try {
                        targetParentFile = GAT.createFile(gatContext, ".");
                    } catch (GATObjectCreationException e) {
                        throw new NoSuccessException(e, wrapper);
                    }
                }
                targetParentExists = targetParentFile.exists();
            }
            if (! targetParentExists) {
                if (!targetParentFile.mkdirs()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("targetParentFile mkdirs failed!");
                    }
                    throw new NoSuccessException(
                            "Failed to make non-existing directories", wrapper);
                }
            }
        }
        return targetURI;
    }
    
    private boolean parentExists(URI target) {
        String path = target.getPath();
        java.io.File f = new java.io.File(path);
        String parent = f.getParent();
        if (parent == null) {
            return true;
        }
        try {
            target = target.setPath(parent);
            f = GAT.createFile(gatContext, target);
            return f.exists();
        } catch (Throwable e) {
            // ignored
        }
        return false;
    }

    // Copy, without resolving target.
    // It has already been resolved, either with respect to this NSEntry or
    // with respect to the NSDirectory object that invoked this method.
    protected void nonResolvingCopy(URL target, int flags)
            throws IncorrectStateException, NoSuccessException,
            BadParameterException, AlreadyExistsException,
            IncorrectURLException, NotImplementedException,
            DoesNotExistException {
        URI targetFile = copyOrMoveChecks(target, flags, "copy");

        // if the target already exists, it will be overwritten if the
        // 'Overwrite' flag is set, otherwise it is an 'AlreadyExists'
        // exception
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("copy: " + fileImpl.toGATURI().toString()
                        + " --> " + targetFile.toString());
            }
            file.copy(targetFile);
        } catch (GATInvocationException e) {
            if (! parentExists(targetFile)) {
                throw new DoesNotExistException(
                        "Target parent file does not exist", wrapper);
            }
            throw new NoSuccessException(e, wrapper);
        }
    }

    public URL getCWD() throws NotImplementedException,
            IncorrectStateException, TimeoutException, NoSuccessException {
        if (isClosed()) {
            throw new IncorrectStateException("NSEntry already closed", wrapper);
        }
        String path = getEntryURL().getPath();
        if (!isDirectory) {
            int i = path.lastIndexOf('/');
            if (i != -1) {
                path = path.substring(0, i);
            }
        }
        URL newURL = null;
        try {
            newURL = URLFactory.createURL(MY_FACTORY, getEntryURL().toString());
            newURL.setPath(path);
        } catch (BadParameterException e) {
            throw new NoSuccessException("Unexpected error", e, wrapper);
        }
        return newURL;
    }

    public URL getName() throws NotImplementedException,
            IncorrectStateException, TimeoutException, NoSuccessException {
        if (isClosed()) {
            throw new IncorrectStateException("NSEntry already closed", wrapper);
        }
        String path = getEntryURL().getPath();
        String[] s = path.split("/");

        try {
            return URLFactory.createURL(MY_FACTORY, s[s.length - 1]);
        } catch (BadParameterException e) {
            throw new NoSuccessException("Unexpected error", e, wrapper);
        }
    }

    public URL getURL() throws NotImplementedException,
            IncorrectStateException, TimeoutException, NoSuccessException {
        if (isClosed()) {
            throw new IncorrectStateException("NSEntry already closed", wrapper);
        }
        try {
            return URLFactory.createURL(MY_FACTORY, wrapper.getWrapperURL().normalize().toString());
        } catch (BadParameterException e) {
            throw new NoSuccessException("Unexpected error", e, wrapper);
        }
    }

    public boolean isDir() throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, IncorrectStateException,
            TimeoutException, NoSuccessException {
        if (isClosed()) {
            throw new IncorrectStateException("NSEntry already closed", wrapper);
        }
        return isDirectory;
    }

    public boolean isEntry() throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, IncorrectStateException,
            TimeoutException, NoSuccessException {
        if (isClosed()) {
            throw new IncorrectStateException("NSEntry already closed", wrapper);
        }
        return !isDirectory;
    }

    public boolean isLink() throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, IncorrectStateException,
            TimeoutException, NoSuccessException {
        throw new NotImplementedException("isLink", wrapper);
    }

    public void link(URL target, int flags) throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, BadParameterException,
            IncorrectStateException, AlreadyExistsException, TimeoutException,
            NoSuccessException, IncorrectURLException {
        throw new NotImplementedException("link", wrapper);
    }

    public void move(URL target, int flags) throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, BadParameterException,
            IncorrectStateException, AlreadyExistsException, TimeoutException,
            NoSuccessException, IncorrectURLException, DoesNotExistException {
        nonResolvingMove(resolve(target), flags);
    }

    protected void nonResolvingMove(URL target, int flags)
            throws IncorrectStateException, NoSuccessException,
            BadParameterException, AlreadyExistsException,
            NotImplementedException, AuthenticationFailedException,
            AuthorizationFailedException, PermissionDeniedException,
            TimeoutException, IncorrectURLException, DoesNotExistException {
        URI targetFile = copyOrMoveChecks(target, flags, "move");

        // if the target already exists, it will be overwritten if the
        // 'Overwrite' flag is set, otherwise it is an 'AlreadyExists'
        // exception
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("move: " + fileImpl.toGATURI().toString()
                        + " --> " + targetFile.toString());
            }
            file.move(targetFile);
        } catch (GATInvocationException e) {
            if (! parentExists(targetFile)) {
                throw new DoesNotExistException(
                        "Target parent file does not exist", wrapper);
            }
            throw new NoSuccessException(e, wrapper);
        }
    }

    public void permissionsAllow(String id, int permissions, int flags)
            throws NotImplementedException, AuthenticationFailedException,
            AuthorizationFailedException, PermissionDeniedException,
            IncorrectStateException, BadParameterException, TimeoutException,
            NoSuccessException {
        throw new NotImplementedException("permissionsAllow", wrapper);
    }

    public void permissionsDeny(String id, int permissions, int flags)
            throws NotImplementedException, AuthenticationFailedException,
            AuthorizationFailedException, PermissionDeniedException,
            IncorrectStateException, BadParameterException, TimeoutException,
            NoSuccessException {
        throw new NotImplementedException("permissionsDeny", wrapper);
    }

    public URL readLink() throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, IncorrectStateException,
            TimeoutException, NoSuccessException {
        throw new NotImplementedException("readLink", wrapper);
    }

    public void remove(int flags) throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, BadParameterException,
            IncorrectStateException, TimeoutException, NoSuccessException {
      
        try {
            if (isDirectory && Flags.RECURSIVE.isSet(flags)) {
                file.recursivelyDeleteDirectory();
            }
            else if (!file.delete()) {
                throw new NoSuccessException("Remove operation failed!",
                        wrapper);
            }
        } catch (GATInvocationException e) {
            throw new NoSuccessException(e, wrapper);
        }
        close(0.0F);
    }

    public void permissionsAllow(String id, int permissions)
            throws NotImplementedException, AuthenticationFailedException,
            AuthorizationFailedException, PermissionDeniedException,
            BadParameterException, TimeoutException, NoSuccessException {
        try {
            permissionsAllow(id, permissions, Flags.NONE.getValue());
        } catch (IncorrectStateException e) {
            // This method cannot throw this, because it implements the method
            // as
            // specified in org.ogf.saga.permissions.Permissions.
            throw new NoSuccessException("Incorrect state", e, wrapper);
        }
    }

    public boolean permissionsCheck(String id, int permissions)
            throws NotImplementedException, AuthenticationFailedException,
            AuthorizationFailedException, PermissionDeniedException,
            BadParameterException, TimeoutException, NoSuccessException {
        throw new NotImplementedException("permissionsCheck", wrapper);
    }

    public void permissionsDeny(String id, int permissions)
            throws NotImplementedException, AuthenticationFailedException,
            AuthorizationFailedException, PermissionDeniedException,
            BadParameterException, TimeoutException, NoSuccessException {
        try {
            permissionsDeny(id, permissions, Flags.NONE.getValue());
        } catch (IncorrectStateException e) {
            // This method cannot throw this, because it implements the method
            // as
            // specified in org.ogf.saga.permissions.Permissions.
            throw new NoSuccessException("Incorrect state", e, wrapper);
        }
    }

    public String getGroup() throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, TimeoutException, NoSuccessException {
        throw new NotImplementedException("getGroup", wrapper);
    }

    public String getOwner() throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, TimeoutException, NoSuccessException {
        throw new NotImplementedException("getOwner", wrapper);
    }

    public long getMTime() throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, IncorrectStateException,
            TimeoutException, NoSuccessException {
        return fileImpl.lastModified() / 1000;
    }
}
