package org.ogf.saga.adaptors.javaGAT.job;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.gridlab.gat.GAT;
import org.gridlab.gat.GATContext;
import org.gridlab.gat.Preferences;
import org.gridlab.gat.URI;
import org.gridlab.gat.resources.ResourceBroker;
import org.ogf.saga.adaptors.javaGAT.util.Initialize;
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
import org.ogf.saga.job.Job;
import org.ogf.saga.job.JobDescription;
import org.ogf.saga.job.JobSelf;
import org.ogf.saga.proxies.job.JobServiceWrapper;
import org.ogf.saga.spi.job.JobServiceAdaptorBase;
import org.ogf.saga.url.URL;

public class JobServiceAdaptor extends JobServiceAdaptorBase {

    static {
        Initialize.initialize();
    }

    final ResourceBroker broker;
    private final GATContext gatContext;
    private HashMap<String, SagaJob> jobs = new HashMap<String, SagaJob>();

    static final String JAVAGAT = "JavaGAT";
    
    final String url;

    public JobServiceAdaptor(JobServiceWrapper wrapper,
            SessionImpl sessionImpl, URL rm) throws NoSuccessException, IncorrectURLException {
        super(wrapper, sessionImpl, rm);
        
        // To make sure that the gridsam scheme enforces the gridsam adaptor:
        String scheme = rm.getScheme();
        if ("gridsam".equals(scheme)) {
            throw new IncorrectURLException("The javagat adaptor does not do the gridsam scheme");
        }

        org.ogf.saga.adaptors.javaGAT.session.Session s;

        synchronized (sessionImpl) {
            s = (org.ogf.saga.adaptors.javaGAT.session.Session) sessionImpl
                    .getAdaptorSession(JAVAGAT);
            if (s == null) {
                s = new org.ogf.saga.adaptors.javaGAT.session.Session();
                sessionImpl.putAdaptorSession(JAVAGAT, s);
            }
        }
        gatContext = s.getGATContext();
        Preferences prefs = gatContext.getPreferences();

        url = rm.toString();
        try {
            URI gatURI = new URI(url);
            if (!"".equals(url)) {
                prefs
                        .put("ResourceBroker.jobmanagerContact", gatURI
                                .toString());
            }

            broker = GAT.createResourceBroker(gatContext, prefs, gatURI);
        } catch (Throwable e) {
            throw new NoSuccessException(
                    "Could not create GAT resource broker", e);
        }
    }

    public Job createJob(JobDescription jd) throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, BadParameterException, TimeoutException,
            NoSuccessException {
        SagaJob job = new SagaJob(this,
                (org.ogf.saga.impl.job.JobDescriptionImpl) jd, sessionImpl,
                gatContext);
        return job;
    }

    synchronized void addJob(SagaJob job, String id) {
        jobs.put(id, job);
    }

    public synchronized Job getJob(String jobId)
            throws NotImplementedException, AuthenticationFailedException,
            AuthorizationFailedException, PermissionDeniedException,
            BadParameterException, DoesNotExistException, TimeoutException,
            NoSuccessException {
        if (!jobId.startsWith("[" + JAVAGAT + "]-")) {
            throw new BadParameterException("Unrecognized job id " + jobId);
        }
        Job job = jobs.get(jobId);
        if (job == null) {
            throw new DoesNotExistException("Job " + jobId + " does not exist");
        }
        return job;
    }

    public JobSelf getSelf() throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, TimeoutException, NoSuccessException {
        // TODO Implement this!
        throw new NotImplementedException("getSelf");
    }

    public List<String> list() throws NotImplementedException,
            AuthenticationFailedException, AuthorizationFailedException,
            PermissionDeniedException, TimeoutException, NoSuccessException {
        return new ArrayList<String>(jobs.keySet());
    }
}
