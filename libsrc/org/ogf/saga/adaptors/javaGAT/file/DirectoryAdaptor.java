package org.ogf.saga.adaptors.javaGAT.file;

import java.util.List;

import org.ogf.saga.adaptors.javaGAT.util.Initialize;
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
import org.ogf.saga.impl.session.SessionImpl;
import org.ogf.saga.namespace.Flags;
import org.ogf.saga.proxies.file.DirectoryWrapper;
import org.ogf.saga.url.URL;

public class DirectoryAdaptor extends
        org.ogf.saga.spi.file.DirectoryAdaptorBase {

    static {
        Initialize.initialize();
    }

    DirectoryEntry dir;

    public DirectoryAdaptor(DirectoryWrapper wrapper, SessionImpl sessionImpl,
            URL name, int flags) throws NotImplementedException,
            IncorrectURLException, BadParameterException,
            DoesNotExistException, PermissionDeniedException,
            AuthorizationFailedException, AuthenticationFailedException,
            TimeoutException, NoSuccessException, AlreadyExistsException {
        super(wrapper, sessionImpl, name, flags);
        dir = new DirectoryEntry(wrapper, sessionImpl, name, flags
                & Flags.ALLNAMESPACEFLAGS.getValue());
    }

    public Object clone() throws CloneNotSupportedException {
        DirectoryAdaptor clone = (DirectoryAdaptor) super.clone();
        clone.dir = (DirectoryEntry) dir.clone();
        clone.dir.setWrapper(clone.directoryWrapper);
        return clone;
    }

    public void close(float timeoutInSeconds) throws NotImplementedException,
            NoSuccessException {
        dir.close(timeoutInSeconds);
        super.close(timeoutInSeconds);
    }

    public long getSize(URL name, int flags) throws NotImplementedException,
            IncorrectURLException, AuthenticationFailedException,
            AuthorizationFailedException, PermissionDeniedException,
            BadParameterException, IncorrectStateException,
            DoesNotExistException, TimeoutException, NoSuccessException {

        name = resolveToDir(name);

        FileEntry file = null;
        try {
            file = new FileEntry(sessionImpl, name, Flags.NONE.getValue());
        } catch (AlreadyExistsException e) {
            throw new NoSuccessException("This should not happen! " + e,
                    directoryWrapper);
        }
        long sz = file.size();
        file.close(0.0F);
        return sz;
    }

    public boolean isFile(URL name) throws NotImplementedException,
            IncorrectURLException, AuthenticationFailedException,
            AuthorizationFailedException, PermissionDeniedException,
            BadParameterException, IncorrectStateException, TimeoutException,
            NoSuccessException, DoesNotExistException {
        return isEntry(name);
    }

    public void copy(URL target, int flags) throws IncorrectStateException,
            NoSuccessException, BadParameterException, AlreadyExistsException,
            IncorrectURLException, NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, TimeoutException, DoesNotExistException {
        dir.copy(target, flags);
    }

    public void move(URL target, int flags) throws IncorrectStateException,
            NoSuccessException, BadParameterException, AlreadyExistsException,
            NotImplementedException, AuthenticationFailedException,
            AuthorizationFailedException, PermissionDeniedException,
            TimeoutException, IncorrectURLException, DoesNotExistException {
        dir.move(target, flags);
    }

    public void changeDir(URL d) throws NotImplementedException,
            IncorrectURLException, AuthenticationFailedException,
            AuthorizationFailedException, PermissionDeniedException,
            BadParameterException, IncorrectStateException,
            DoesNotExistException, TimeoutException, NoSuccessException {
        this.dir.changeDir(d);
    }

    public void copy(URL source, URL target, int flags)
            throws NotImplementedException, AuthenticationFailedException,
            AuthorizationFailedException, PermissionDeniedException,
            IncorrectURLException, BadParameterException,
            IncorrectStateException, AlreadyExistsException,
            DoesNotExistException, TimeoutException, NoSuccessException {
        dir.copy(source, target, flags);
    }

    public boolean exists(URL name) throws NotImplementedException,
            IncorrectURLException, AuthenticationFailedException,
            AuthorizationFailedException, PermissionDeniedException,
            BadParameterException, IncorrectStateException, TimeoutException,
            NoSuccessException {
        return dir.exists(name);
    }

    public List<URL> find(String pattern, int flags)
            throws NotImplementedException, AuthenticationFailedException,
            AuthorizationFailedException, PermissionDeniedException,
            BadParameterException, IncorrectStateException, TimeoutException,
            NoSuccessException {
        return dir.find(pattern, flags);
    }

    public URL getEntry(int entry) throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, IncorrectStateException,
            DoesNotExistException, TimeoutException, NoSuccessException {
        return dir.getEntry(entry);
    }

    public int getNumEntries() throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, IncorrectStateException,
            TimeoutException, NoSuccessException {
        return dir.getNumEntries();
    }

    public boolean isDir(URL name) throws NotImplementedException,
            IncorrectURLException, DoesNotExistException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, BadParameterException,
            IncorrectStateException, TimeoutException, NoSuccessException {
        return dir.isDir(name);
    }

    public boolean isEntry(URL name) throws NotImplementedException,
            IncorrectURLException, DoesNotExistException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, BadParameterException,
            IncorrectStateException, TimeoutException, NoSuccessException {
        return dir.isEntry(name);
    }

    public boolean isLink(URL name) throws NotImplementedException,
            IncorrectURLException, DoesNotExistException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, BadParameterException,
            IncorrectStateException, TimeoutException, NoSuccessException {
        return dir.isLink(name);
    }

    public void link(URL source, URL target, int flags)
            throws NotImplementedException, AuthenticationFailedException,
            AuthorizationFailedException, PermissionDeniedException,
            IncorrectURLException, BadParameterException,
            IncorrectStateException, AlreadyExistsException,
            DoesNotExistException, TimeoutException, NoSuccessException {
        dir.link(source, target, flags);

    }

    public void move(URL source, URL target, int flags)
            throws NotImplementedException, AuthenticationFailedException,
            AuthorizationFailedException, PermissionDeniedException,
            IncorrectURLException, BadParameterException,
            IncorrectStateException, AlreadyExistsException,
            DoesNotExistException, TimeoutException, NoSuccessException {
        dir.move(source, target, flags);

    }

    public void permissionsAllow(URL target, String id, int permissions,
            int flags) throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, IncorrectStateException,
            BadParameterException, TimeoutException, NoSuccessException {
        dir.permissionsAllow(target, id, permissions, flags);
    }

    public void permissionsDeny(URL target, String id, int permissions,
            int flags) throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, BadParameterException, TimeoutException,
            NoSuccessException {
        dir.permissionsDeny(target, id, permissions, flags);
    }

    public URL readLink(URL name) throws NotImplementedException,
            IncorrectURLException, DoesNotExistException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, BadParameterException,
            IncorrectStateException, TimeoutException, NoSuccessException {
        return dir.readLink(name);
    }

    public void remove(URL target, int flags) throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, IncorrectURLException,
            BadParameterException, IncorrectStateException,
            DoesNotExistException, TimeoutException, NoSuccessException {
        dir.remove(target, flags);
    }

    public boolean isDir() throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, IncorrectStateException,
            TimeoutException, NoSuccessException {
        return dir.isDir();
    }

    public boolean isEntry() throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, IncorrectStateException,
            TimeoutException, NoSuccessException {
        return dir.isEntry();
    }

    public boolean isLink() throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, IncorrectStateException,
            TimeoutException, NoSuccessException {
        return dir.isLink();
    }

    public void link(URL target, int flags) throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, BadParameterException,
            IncorrectStateException, AlreadyExistsException, TimeoutException,
            NoSuccessException, IncorrectURLException {
        dir.link(target, flags);
    }

    public void permissionsAllow(String id, int permissions, int flags)
            throws NotImplementedException, AuthenticationFailedException,
            AuthorizationFailedException, PermissionDeniedException,
            IncorrectStateException, BadParameterException, TimeoutException,
            NoSuccessException {
        dir.permissionsAllow(id, permissions, flags);
    }

    public void permissionsDeny(String id, int permissions, int flags)
            throws NotImplementedException, AuthenticationFailedException,
            AuthorizationFailedException, IncorrectStateException,
            PermissionDeniedException, BadParameterException, TimeoutException,
            NoSuccessException {
        dir.permissionsDeny(id, permissions, flags);
    }

    public URL readLink() throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, IncorrectStateException,
            TimeoutException, NoSuccessException {
        return dir.readLink();
    }

    public void remove(int flags) throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, BadParameterException,
            IncorrectStateException, TimeoutException, NoSuccessException {
        dir.remove(flags);
    }

    public String getGroup() throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, TimeoutException, NoSuccessException {
        return dir.getGroup();
    }

    public String getOwner() throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, TimeoutException, NoSuccessException {
        return dir.getOwner();
    }

    public boolean permissionsCheck(String id, int permissions)
            throws NotImplementedException, AuthenticationFailedException,
            AuthorizationFailedException, PermissionDeniedException,
            BadParameterException, TimeoutException, NoSuccessException {
        return dir.permissionsCheck(id, permissions);
    }

    public void copy(String source, URL target, int flags)
            throws NotImplementedException, AuthenticationFailedException,
            AuthorizationFailedException, PermissionDeniedException,
            IncorrectURLException, BadParameterException,
            IncorrectStateException, AlreadyExistsException,
            DoesNotExistException, TimeoutException, NoSuccessException {
        dir.copy(source, target, flags);
    }

    public void link(String source, URL target, int flags)
            throws NotImplementedException, AuthenticationFailedException,
            AuthorizationFailedException, PermissionDeniedException,
            IncorrectURLException, BadParameterException,
            IncorrectStateException, AlreadyExistsException,
            DoesNotExistException, TimeoutException, NoSuccessException {
        dir.link(source, target, flags);
    }

    public void move(String source, URL target, int flags)
            throws NotImplementedException, AuthenticationFailedException,
            AuthorizationFailedException, PermissionDeniedException,
            IncorrectURLException, BadParameterException,
            IncorrectStateException, AlreadyExistsException,
            DoesNotExistException, TimeoutException, NoSuccessException {
        dir.move(source, target, flags);
    }

    public void permissionsAllow(String target, String id, int permissions,
            int flags) throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, IncorrectStateException,
            BadParameterException, TimeoutException, NoSuccessException {
        dir.permissionsAllow(target, id, permissions, flags);
    }

    public void permissionsDeny(String target, String id, int permissions,
            int flags) throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, BadParameterException, TimeoutException,
            NoSuccessException {
        dir.permissionsDeny(target, id, permissions, flags);
    }

    public void remove(String target, int flags)
            throws NotImplementedException, AuthenticationFailedException,
            AuthorizationFailedException, PermissionDeniedException,
            IncorrectURLException, BadParameterException,
            IncorrectStateException, DoesNotExistException, TimeoutException,
            NoSuccessException {
        dir.remove(target, flags);
    }

    public List<URL> listCurrentDir(int flags) throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, BadParameterException,
            IncorrectStateException, TimeoutException, NoSuccessException {
        return dir.listCurrentDir(flags);
    }

    public long getMTime() throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, IncorrectStateException,
            TimeoutException, NoSuccessException {
        return dir.getMTime();
    }

    public long getMTime(URL name) throws NotImplementedException,
            IncorrectURLException, DoesNotExistException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, BadParameterException,
            IncorrectStateException, TimeoutException, NoSuccessException {
        return dir.getMTime(name);
    }
}
