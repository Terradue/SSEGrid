package org.ogf.saga.adaptors.javaGAT.job;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gridlab.gat.GAT;
import org.gridlab.gat.GATContext;
import org.gridlab.gat.GATInvocationException;
import org.gridlab.gat.GATObjectCreationException;
import org.gridlab.gat.URI;
import org.gridlab.gat.io.File;
import org.gridlab.gat.monitoring.MetricEvent;
import org.gridlab.gat.monitoring.MetricListener;
import org.gridlab.gat.resources.HardwareResourceDescription;
import org.gridlab.gat.resources.SoftwareDescription;
import org.gridlab.gat.resources.SoftwareResourceDescription;
import org.gridlab.gat.resources.Job.JobState;
import org.gridlab.gat.security.SecurityContext;
import org.ogf.saga.adaptors.javaGAT.util.GatURIConverter;
import org.ogf.saga.error.AuthenticationFailedException;
import org.ogf.saga.error.AuthorizationFailedException;
import org.ogf.saga.error.BadParameterException;
import org.ogf.saga.error.DoesNotExistException;
import org.ogf.saga.error.IncorrectStateException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.error.PermissionDeniedException;
import org.ogf.saga.error.TimeoutException;
import org.ogf.saga.impl.SagaRuntimeException;
import org.ogf.saga.impl.job.JobDescriptionImpl;
import org.ogf.saga.impl.session.SessionImpl;
import org.ogf.saga.job.JobDescription;
import org.ogf.saga.task.State;
import org.ogf.saga.url.URL;
import org.ogf.saga.url.URLFactory;

/**
 * This is an implementation of the SAGA Job SPI on top of the JavaGAT. Some
 * JobDescription attributes, Job Attributes and Job Metrics unfortunately
 * cannot be implemented on top of the JavaGAT. These are the JobDescription
 * attributes JobDescription.THREADSPERPROCESS, JobDescription.JOBCONTACT,
 * JobDescription.JOBSTARTTIME, the Job attribute Job.TERMSIG, the Job Metrics
 * JOB_SIGNAL, JOB_CPUTIME, JOB_MEMORYUSE, JOB_VMEMORYUSE, JOB_PERFORMANCE. In
 * addition, the method {@link #signal(int)} cannot be implemented. Apart from
 * that, JavaGAT at least has the interface to support SAGA Jobs. How much
 * actually is implemented depends on the JavaGAT adaptor at hand.
 * 
 * One thing to note is that in JavaGAT job descriptions using setStdin,
 * setStdout, setStderr, the filenames specified there (if not absolute) are
 * relative to the working directory on the submitting host. In Saga, they are
 * supposed to be relative to the directory in which the job is run.
 */

public final class SagaJob extends org.ogf.saga.impl.job.JobImpl implements
		MetricListener {

	private static final Logger logger = LoggerFactory.getLogger(SagaJob.class);

	private final JobServiceAdaptor service;
	private org.gridlab.gat.resources.Job gatJob = null;
	private final GATContext gatContext;
	private org.gridlab.gat.resources.JobDescription gatJobDescription;
	private JobState savedState = JobState.UNKNOWN;
	private boolean interactive = false;

	private static int jobCount = 0;

	public SagaJob(JobServiceAdaptor service,
			JobDescriptionImpl jobDescriptionImpl, SessionImpl sessionImpl,
			GATContext gatContext) throws NotImplementedException,
			BadParameterException, NoSuccessException {
		super(jobDescriptionImpl, sessionImpl);
		this.service = service;
		this.gatContext = gatContext;
		gatJobDescription = new org.gridlab.gat.resources.JobDescription(
				createSoftwareDescription(), createHardwareResourceDescription());
		try {
			int count = Integer.parseInt(getV(JobDescriptionImpl.NUMBEROFPROCESSES));
			gatJobDescription.setProcessCount(count);
			setValue(SERVICEURL, service.url);
		} catch (Throwable e) {
			// ignored
		}

		try {
			int hostCount = Integer
					.parseInt(getV(JobDescriptionImpl.NUMBEROFPROCESSES))
					/ Integer.parseInt(getV(JobDescriptionImpl.PROCESSESPERHOST));
			// What to do if PROCESSESPERHOST is set but NUMBEROFPROCESSES is
			// not???
			gatJobDescription.setResourceCount(hostCount);
		} catch (Throwable e) {
			// ignored
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Created gatJobDescription " + gatJobDescription);
		}
		try {
			String w = jobDescriptionImpl
					.getAttribute(JobDescription.WORKINGDIRECTORY);
			setValue(WORKINGDIRECTORY, w);
		} catch (Throwable e) {
			// ignored, should not happen.
		}
	}

	private SagaJob(SagaJob orig) {
		super(orig);
		synchronized (orig) {
			gatContext = new GATContext();
			gatContext.addPreferences(orig.gatContext.getPreferences());
			for (SecurityContext c : orig.gatContext.getSecurityContexts()) {
				gatContext.addSecurityContext(c);
			}
			service = orig.service;
			gatJob = orig.gatJob;
			savedState = orig.savedState;
			gatJobDescription = new org.gridlab.gat.resources.JobDescription(
					orig.gatJobDescription.getSoftwareDescription(),
					createHardwareResourceDescription());
			interactive = orig.interactive;
		}
	}

	private String getV(String s) {
		try {
			s = jobDescriptionImpl.getAttribute(s);
			if ("".equals(s)) {
				throw new Error("Not initialized");
			}

		} catch (Throwable e) {
			throw new Error("Not present");
		}
		return s;
	}

	private String[] getVec(String s) {
		String[] result;
		try {
			result = jobDescriptionImpl.getVectorAttribute(s);
			if (result == null || result.length == 0) {
				throw new Error("Not initialized");
			}

		} catch (Throwable e) {
			throw new Error("Not present");
		}
		return result;
	}

	private SoftwareDescription createSoftwareDescription()
			throws BadParameterException, NotImplementedException, NoSuccessException {

		SoftwareDescription sd = new SoftwareDescription();

		try {
			String s = getV(JobDescriptionImpl.EXECUTABLE);
			sd.setExecutable(s);
		} catch (Throwable e) {
			throw new BadParameterException("Could not get Executable for job", e);
		}
		try {
			sd.setArguments(getVec(JobDescriptionImpl.ARGUMENTS));
		} catch (Throwable e) {
			// ignored
		}
		try {
			String[] env = getVec(JobDescriptionImpl.ENVIRONMENT);
			HashMap<String, Object> environment = new HashMap<String, Object>();
			for (String e : env) {
				int index = e.indexOf('=');
				if (index == -1) {
					environment.put(e, "");
				} else {
					environment.put(e.substring(0, index), e.substring(index + 1));
				}
			}
			sd.setEnvironment(environment);
		} catch (Throwable e) {
			// ignored
		}
		try {
			sd.addAttribute(SoftwareDescription.JOB_TYPE,
					getV(JobDescriptionImpl.SPMDVARIATION));
		} catch (Throwable e) {
			// ignored
		}

		// notImplemented(JobDescription.THREADSPERPROCESS);
		// notImplemented(JobDescription.JOBCONTACT);
		// notImplemented(JobDescription.JOBSTARTTIME);

		try {
			String v = getV(JobDescriptionImpl.WORKINGDIRECTORY);
			if (!".".equals(v)) {
				sd.addAttribute(SoftwareDescription.DIRECTORY, v);
				// ... but, apparently, none of the javagat adaptors look at this
				// attribute, so
				// explicitly added sandbox attributes. --Ceriel
				sd.addAttribute(SoftwareDescription.SANDBOX_USEROOT, "true");
				sd.addAttribute(SoftwareDescription.SANDBOX_ROOT, v);
			}
		} catch (Throwable e) {
			// ignored
		}
		try {
			sd.addAttribute(SoftwareDescription.JOB_QUEUE,
					getV(JobDescriptionImpl.QUEUE));
		} catch (Throwable e) {
			// ignored
		}
		try {
			sd.addAttribute(SoftwareDescription.MEMORY_MAX,
					getV(JobDescriptionImpl.TOTALPHYSICALMEMORY));
		} catch (Throwable e) {
			// ignored
		}
		try {
			sd.addAttribute(SoftwareDescription.CPUTIME_MAX,
					getV(JobDescriptionImpl.TOTALCPUTIME));
		} catch (Throwable e) {
			// ignored
		}
		try {
			String v = getV(JobDescriptionImpl.CLEANUP);
			if ("False".equals(v)) {
				sd.addAttribute(SoftwareDescription.SAVE_STATE, "true");
				sd.addAttribute(SoftwareDescription.SANDBOX_DELETE, "false");
			}
			// Default behaviour of javagat is to cleanup.
		} catch (Throwable e) {
			// ignored
		}

		try {
			String s = getV(JobDescriptionImpl.INTERACTIVE);
			interactive = "True".equals(s);
		} catch (Throwable ignored) {
			// ignore
		}

		sd.enableStreamingStdin(interactive);
		sd.enableStreamingStdout(interactive);
		sd.enableStreamingStderr(interactive);

		URI stdin = null;
		URI stdout = null;
		URI stderr = null;

		if (!interactive) {
			stdin = getURI(JobDescriptionImpl.INPUT);
			stdout = getURI(JobDescriptionImpl.OUTPUT);
			stderr = getURI(JobDescriptionImpl.ERROR);
		}

		boolean stdinReplaced = false;
		boolean stdoutReplaced = false;
		boolean stderrReplaced = false;

		String[] transfers = null;

		try {
			transfers = getVec(JobDescriptionImpl.FILETRANSFER);
		} catch (Throwable e) {
			// ignored
		}

		if (transfers != null) {
			for (int i = 0; i < transfers.length; i++) {
				String[] parts = transfers[i].split(" << ");
				if (parts.length == 1) {
					// no match
				} else {
					throw new NotImplementedException(
							"PostStage append is not supported", this);
				}
				parts = transfers[i].split(" >> ");
				if (parts.length == 1) {
					// no match
				} else {
					throw new NotImplementedException("PreStage append is not supported",
							this);
				}
				boolean prestage = true;
				parts = transfers[i].split(" > ");
				if (parts.length == 1) {
					prestage = false;
					parts = transfers[i].split(" < ");
					if (parts.length == 1) {
						throw new BadParameterException("Unrecognized FileTransfer part: "
								+ transfers[i], this);
					}
				}

				URI s1 = null;
				URI s2 = null;
				try {
					s1 = GatURIConverter.cvtToGatURI(URLFactory.createURL(MY_FACTORY,
							parts[0]));
					s2 = GatURIConverter.cvtToGatURI(URLFactory.createURL(MY_FACTORY,
							parts[1]));
				} catch (URISyntaxException e) {
					throw new BadParameterException(e, this);
				}

				if (!prestage) {
					if (stdout != null && !stdout.isAbsolute() && s2.equals(stdout)) {
						// In SAGA, a non-absolute stdout is relative to target
						// and is probably staged-out explicitly.
						// In javaGAT we should use the destination instead.
						stdout = s1;
						stdoutReplaced = true;
						continue;
					}
					if (stderr != null && !stderr.isAbsolute() && s2.equals(stderr)) {
						stderr = s1;
						stderrReplaced = true;
						continue;
					}
				} else if (stdin != null && !stdin.isAbsolute() && s2.equals(stdin)) {
					// In SAGA, a non-absolute stdin is relative to target
					// and is probably staged-in explicitly.
					// In javaGAT we should use the source instead.
					stdin = s1;
					stdinReplaced = true;
					continue;
				}

				File f1 = createFile(s1);
				File f2 = createFile(s2);

				if (prestage) {
					if (logger.isDebugEnabled()) {
						logger.debug("Add prestage: src = " + s1 + ", dst = " + s2);
					}
					sd.addPreStagedFile(f1, f2);
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("Add poststage: dst = " + s1 + ", src = " + s2);
					}
					sd.addPostStagedFile(f2, f1);
				}
			}
		}

		if (stdin != null) {
			if (!stdinReplaced && !stdin.isAbsolute()) {
				throw new NotImplementedException(
						"Relative non-staged-in Input is not supported");
			}
			sd.setStdin(createFile(stdin));
		}

		if (stdout != null) {
			if (!stdoutReplaced && !stdout.isAbsolute()) {
				throw new NotImplementedException(
						"Relative non-staged-out Output is not supported");
			}
			sd.setStdout(createFile(stdout));
		}

		if (stderr != null) {
			if (!stderrReplaced && !stderr.isAbsolute()) {
				throw new NotImplementedException(
						"Relative non-staged-out Error is not supported");
			}
			sd.setStderr(createFile(stderr));
		}

		return sd;
	}

	private File createFile(URI uri) throws BadParameterException {
		try {
			return GAT.createFile(gatContext, uri);
		} catch (GATObjectCreationException e) {
			throw new BadParameterException("Could not create GAT File for " + uri,
					e, this);
		}
	}

	void notImplemented(String s) throws NotImplementedException {
		try {
			s = getV(s);
		} catch (Throwable e) {
			// If getV throws an exception, the attribute string is not used,
			// so it does not matter that it is not implemented :-)
			return;
		}
		throw new NotImplementedException(s + " not implemented", this);
	}

	private HardwareResourceDescription createHardwareResourceDescription() {
		HardwareResourceDescription hd = new HardwareResourceDescription();
		try {
			String s = getV(JobDescriptionImpl.TOTALCPUCOUNT);
			hd.addResourceAttribute("cpu.count", s);
		} catch (Throwable e) {
			// ignored
		}

		try {
			String[] hosts = getVec(JobDescriptionImpl.CANDIDATEHOSTS);
			hd.addResourceAttribute("machine.node", hosts);
		} catch (Throwable e) {
			// ignored
		}

		try {
			hd.addResourceAttribute("cpu.type",
					getV(JobDescriptionImpl.CPUARCHITECTURE));
		} catch (Throwable e) {
			// ignored
		}

		try {
			String s = getV(JobDescriptionImpl.OPERATINGSYSTEMTYPE);
			SoftwareResourceDescription sd = new SoftwareResourceDescription();
			sd.addResourceAttribute("os.type", s);
			hd.addResourceDescription(sd);
		} catch (Throwable e) {
			// ignored
		}

		return hd;
	}

	private URI getURI(String s) throws BadParameterException {
		try {
			URL url = URLFactory.createURL(MY_FACTORY, getV(s));
			return GatURIConverter.cvtToGatURI(url);
		} catch (BadParameterException e) {
			throw new BadParameterException(e.getMessage(), e.getCause(), this);
		} catch (URISyntaxException e) {
			throw new BadParameterException(e, this);
		} catch (Throwable e) {
			return null;
		}
	}

	@Override
	public synchronized void cancel(float timeoutInSeconds)
			throws NotImplementedException, IncorrectStateException,
			TimeoutException, NoSuccessException {
		if (state == State.NEW) {
			throw new IncorrectStateException("cancel() called on job in state New",
					this);
		}
		if (isDone()) {
			return;
		}
		try {
			gatJob.stop();
		} catch (GATInvocationException e) {
			throw new NoSuccessException("Could not cancel job", this);
		} catch (UnsupportedOperationException e) {
			throw new NotImplementedException("cancel() not implemented", e, this);
		}
		setState(State.CANCELED);

	}

	@Override
	public synchronized boolean cancel(boolean mayInterruptIfRunning) {
		if (state == State.RUNNING || state == State.SUSPENDED) {
			if (mayInterruptIfRunning) {
				try {
					gatJob.stop();
				} catch (GATInvocationException e) {
					throw new SagaRuntimeException("Could not cancel job");
				}
				setState(State.CANCELED);
				return true;
			}
			return false;
		}
		if (state == State.CANCELED || state == State.FAILED || state == State.DONE) {
			return false;
		}
		setState(State.CANCELED);
		return true;
	}

	@Override
	public synchronized boolean isCancelled() {
		return state == State.CANCELED;
	}

	@Override
	public synchronized boolean isDone() {
		return state == State.FAILED || state == State.DONE
				|| state == State.CANCELED;
	}

	@Override
	public void run() throws NotImplementedException, IncorrectStateException,
			TimeoutException, NoSuccessException {

		if (state != State.NEW) {
			throw new IncorrectStateException(
					"run() called on job in state " + state, this);
		}

		setState(State.RUNNING);

		try {
			gatJob = service.broker.submitJob(gatJobDescription, this, "job.status");
		} catch (GATInvocationException e) {
			setState(State.FAILED);
			throw new NoSuccessException("Job.run() failed", e, this);
		}
		String id;
		try {
			id = "" + gatJob.getJobID();
		} catch (Throwable e) {
			// Apparently not provided by JavaGAT adaptor ...`
			id = "" + jobCount++;
		}
		id = "[" + JobServiceAdaptor.JAVAGAT + "]-[" + id + "]";
		try {
			setValue(JOBID, id);
		} catch (Throwable e) {
			// Should not happen.
		}
		service.addJob(this, id);
	}

	private void setDetail(String s) {
		try {
			jobStateDetail.setValue(JobServiceAdaptor.JAVAGAT + "." + s);
			jobStateDetail.internalFire();
		} catch (Throwable e) {
			// ignored
		}
	}

	public void processMetricEvent(MetricEvent val) {
		JobState gatState = (JobState) val.getValue();
		if (gatState == savedState) {
			return;
		}
		savedState = gatState;

		Map<String, Object> info = null;
		if (logger.isDebugEnabled()) {
			logger.debug("processMetricEvent: " + val);
		}
		try {
			info = gatJob.getInfo();
		} catch (Throwable e) {
			// ignored
		}
		if (logger.isDebugEnabled()) {
			if (info != null) {
				logger.debug("processMetricEvent: info = " + info);
			}
			logger.debug("state = " + savedState);
		}
		switch (savedState) {
		case ON_HOLD:
			setState(State.SUSPENDED);
			setDetail("ON_HOLD");
			break;

		case POST_STAGING:
			setDetail("POST_STAGING");
			break;

		case PRE_STAGING:
			setDetail("PRE_STAGING");
			break;

		case RUNNING:
			if (info != null) {
				Long l = (Long) info.get("starttime");
				String s = (String) info.get("hostname");
				if (l != null) {
					try {
						setValue(STARTED, l.toString());
					} catch (Throwable e) {
						// ignored
					}
				}
				if (s != null) {
					try {
						setVectorValue(EXECUTIONHOSTS, s.split(" "));
					} catch (Throwable e) {
						// ignored
					}
				}
			}
			setDetail("RUNNING");
			break;
		case SCHEDULED:
			setDetail("SCHEDULED");
			if (info != null) {
				Long l = (Long) info.get("submissiontime");
				if (l != null) {
					try {
						setValue(CREATED, l.toString());
					} catch (Throwable e) {
						// ignored
					}
				}
			}

			break;

		case STOPPED:
			if (state == State.RUNNING) {
				setDetail("STOPPED");
				if (info != null) {
					Exception pse = (Exception) info.get("poststage.exception");
					if (pse != null && info.get("globus.state") == null) {
						setException(new NoSuccessException("Poststage error: " + pse, this));
						setState(State.FAILED);
						synchronized (this) {
							notifyAll();
						}
						break;
					}
				}
				try {
					int n = gatJob.getExitStatus();
					setValue(EXITCODE, "" + n);
					logger.debug("EXIT CODE: " + n);
				} catch (Throwable e) {
					// ignored
				}
				setState(State.DONE);
				synchronized (this) {
					notifyAll();
				}
			}
			if (info != null) {
				Long l = (Long) info.get("stoptime");
				if (l != null) {
					try {
						setValue(FINISHED, l.toString());
					} catch (Throwable e) {
						// ignored
					}
				}
			}
			try {
				int n = gatJob.getExitStatus();
				setValue(EXITCODE, "" + n);
			} catch (Throwable e) {
				// ignored
			}

			break;

		case SUBMISSION_ERROR:
			setDetail("SUBMISSION_ERROR");
			setException(new NoSuccessException("Submission error", this));
			setState(State.FAILED);
			synchronized (this) {
				notifyAll();
			}
			break;
		case INITIAL:
			setDetail("INITIAL");
			break;
		case UNKNOWN:
			setDetail("UNKNOWN");
			break;
		}

	}

	@Override
	public synchronized boolean waitFor(float timeoutInSeconds)
			throws NotImplementedException, IncorrectStateException,
			TimeoutException, NoSuccessException {
		switch (state) {
		case NEW:
			throw new IncorrectStateException("waitFor called on new job", this);
		case DONE:
		case CANCELED:
		case FAILED:
			return true;
		case SUSPENDED:
		case RUNNING:
			if (timeoutInSeconds < 0) {
				while (state == State.SUSPENDED || state == State.RUNNING) {
					try {
						wait();
					} catch (Exception e) {
						// ignored
					}
				}
			} else {
				long interval = (long) (timeoutInSeconds * 1000.0);
				long currentTime = System.currentTimeMillis();
				long endTime = currentTime + interval;
				while ((state == State.SUSPENDED || state == State.RUNNING)
						&& currentTime < endTime) {
					interval = endTime - currentTime;
					try {
						wait(interval);
					} catch (Exception e) {
						// ignored
					}
					currentTime = System.currentTimeMillis();
				}
			}
		}

		return state != State.SUSPENDED && state != State.RUNNING;
	}

	public void checkpoint() throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, IncorrectStateException, TimeoutException,
			NoSuccessException {
		throw new NotImplementedException(
				"checkpoint() not implemented: JavaGAT does not support it", this);
	}

	private void checkInteractive(String stream) throws IncorrectStateException,
			DoesNotExistException {
		if (!interactive) {
			throw new IncorrectStateException("The job is not interactive");
		}
		if (gatJob == null) {
			throw new DoesNotExistException(stream + " is not available yet");
		}
	}

	public InputStream getStderr() throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, BadParameterException, DoesNotExistException,
			TimeoutException, IncorrectStateException, NoSuccessException {
		checkInteractive("stderr");
		try {
			return gatJob.getStderr();
		} catch (GATInvocationException e) {
			throw new NoSuccessException("getStderr() failed", e, this);
		} catch (UnsupportedOperationException e) {
			throw new NotImplementedException("getStderr() not implemented", e, this);
		}

	}

	public OutputStream getStdin() throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, BadParameterException, DoesNotExistException,
			TimeoutException, IncorrectStateException, NoSuccessException {
		checkInteractive("stdin");
		try {
			return gatJob.getStdin();
		} catch (GATInvocationException e) {
			throw new NoSuccessException("getStdin() failed", e, this);
		} catch (UnsupportedOperationException e) {
			throw new NotImplementedException("getStdin() not implemented", e, this);
		}
	}

	public InputStream getStdout() throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, BadParameterException, DoesNotExistException,
			TimeoutException, IncorrectStateException, NoSuccessException {
		checkInteractive("stdout");
		try {
			return gatJob.getStdout();
		} catch (GATInvocationException e) {
			throw new NoSuccessException("getStdout() failed", e, this);
		} catch (UnsupportedOperationException e) {
			throw new NotImplementedException("getStdout() not implemented", e, this);
		}
	}

	public void migrate(org.ogf.saga.job.JobDescription jd)
			throws NotImplementedException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			BadParameterException, IncorrectStateException, TimeoutException,
			NoSuccessException {
		throw new NotImplementedException(
				"migrate() not implemented: JavaGAT does not support it", this);
	}

	public void resume() throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, IncorrectStateException, TimeoutException,
			NoSuccessException {
		State s = getState();
		if (s != State.SUSPENDED) {
			throw new IncorrectStateException("resume() called when job state was "
					+ s, this);
		}
		try {
			gatJob.resume();
		} catch (GATInvocationException e) {
			throw new NoSuccessException("resume failed", e, this);
		} catch (UnsupportedOperationException e) {
			throw new NotImplementedException("resume() not implemented", e, this);
		}
		setState(State.RUNNING);
	}

	public void signal(int signum) throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, BadParameterException,
			IncorrectStateException, TimeoutException, NoSuccessException {
		throw new NotImplementedException(
				"signal() not implemented: javaGAT does not support this", this);

	}

	public void suspend() throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, IncorrectStateException, TimeoutException,
			NoSuccessException {
		State s = getState();
		if (s != State.RUNNING) {
			throw new IncorrectStateException("suspend() called when job state was "
					+ s, this);
		}
		try {
			gatJob.hold();
		} catch (GATInvocationException e) {
			throw new NoSuccessException("suspend() failed", e, this);
		} catch (UnsupportedOperationException e) {
			throw new NotImplementedException("suspend() not implemented", e, this);
		}
		setState(State.SUSPENDED);
	}

	public String getGroup() throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, TimeoutException, NoSuccessException {
		throw new NotImplementedException("getGroup() not supported", this);
	}

	public String getOwner() throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, TimeoutException, NoSuccessException {
		throw new NotImplementedException("getOwner not supported", this);
	}

	public void permissionsAllow(String id, int permissions)
			throws NotImplementedException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			BadParameterException, TimeoutException, NoSuccessException {
		throw new NotImplementedException("permissionsAllow not supported", this);
	}

	public boolean permissionsCheck(String id, int permissions)
			throws NotImplementedException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			BadParameterException, TimeoutException, NoSuccessException {
		throw new NotImplementedException("permissionsCheck not supported", this);
	}

	public void permissionsDeny(String id, int permissions)
			throws NotImplementedException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			BadParameterException, TimeoutException, NoSuccessException {
		throw new NotImplementedException("permissionsDeny not supported", this);
	}

	@Override
	public Object clone() {
		return new SagaJob(this);
	}

}
