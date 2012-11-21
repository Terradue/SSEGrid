/**
 * 
 */
package com.terradue.ogf.saga.impl.job;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.ogf.saga.context.Context;
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
import org.ogf.saga.impl.job.JobAttributes;
import org.ogf.saga.job.Job;
import org.ogf.saga.job.JobDescription;
import org.ogf.saga.job.JobService;
import org.ogf.saga.monitoring.Callback;
import org.ogf.saga.monitoring.Metric;
import org.ogf.saga.monitoring.Monitorable;
import org.ogf.saga.session.Session;
import org.ogf.saga.task.State;
import org.ogf.saga.task.Task;
import org.ogf.saga.task.TaskMode;
import org.ogf.saga.url.URL;

/**
 * The JobImpl class is an array of n jobs. It provides the exact same
 * operations and attributes as per Job interface but performs the operations
 * aggregated for all jobs it contains
 * 
 * JobImpl is created by a {@link JobService}, using a {@link JobDescription}.
 * The <code>JobImpl</code> class simply include an array of {@link Job}
 * accessible publicly.
 * 
 * @author $Author: $
 * @version $Revision: -1 $
 * @date $Date: $
 * 
 */
public class JobImpl implements Job, Callback {

	private Job prepJob;
	private Job endJob;
	private JobDescription jd;
	private Job[] jobArray;
	private String JobUID;

	private JobAttributes attributes;

	private int totalTask;

	private int phase = 0;
	// overall state
	private State jobState = State.NEW;

	// Logger
	private Logger log = Logger.getLogger(this.getClass());

	private int status = 0;

	private HashMap<String, String> SubstitutedVars = new HashMap<String, String>();
	private HashMap<String, Object> Callbacks = new HashMap<String, Object>();

	private Session session;
	private URL rm;

	// Create callback orchestrator
	JobImplOrchestrator jic = null;

	/**
	 * @param postJob
	 * @param prepJob
	 * @param jobDescriptionImpl
	 * @param session
	 * @throws NotImplementedException
	 * @throws BadParameterException
	 */
	public JobImpl(String JobUid, Job prepJob2, Job[] ja, Job postJob, Session s)
			throws NotImplementedException, BadParameterException {
		this.JobUID = JobUid;
		this.prepJob = prepJob2;
		this.endJob = postJob;
		this.jobArray = ja;
		totalTask = ja.length;
		jic = new JobImplOrchestrator(this);
		this.session = s;
		attributes = new JobAttributes(this, s);
	}

	public int getTotalTask() {
		return totalTask;
	}

	public int addCallback(String jobState, Callback m)
			throws NotImplementedException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			DoesNotExistException, TimeoutException, NoSuccessException,
			IncorrectStateException {

		Callbacks.put(jobState, m);

		return 0;
	}

	public void addSubstitutedVariable(String var, String value) {
		SubstitutedVars.put(var, value);
	}

	/**
	 * Cancels all the jobs. This is a non-blocking version, which may continue to
	 * try and cancel the jobs in the background. The job array state will remain
	 * RUNNING until the cancel operation succeeds.
	 * 
	 * @exception NotImplementedException
	 *              is thrown if the implementation does not provide an
	 *              implementation of this method.
	 * @exception TimeoutException
	 *              is thrown when a remote operation did not complete
	 *              successfully because the network communication or the remote
	 *              service timed out.
	 * @exception IncorrectStateException
	 *              is thrown when the task is in NEW state.
	 * @exception NoSuccessException
	 *              is thrown when the operation was not successfully performed,
	 *              and none of the other exceptions apply.
	 */
	public void cancel() throws NotImplementedException, IncorrectStateException,
			TimeoutException, NoSuccessException {

		if (phase == 1) {
			if (prepJob.getState() == State.RUNNING)
				prepJob.cancel(true);
			return;
		}

		if (phase == 2) {
			for (Job i : jobArray) {
				if (i.getState() == State.RUNNING)
					i.cancel();
			}
		}

		if (phase == 3) {
			if (endJob.getState() == State.RUNNING)
				endJob.cancel(true);
		}

	}

	public boolean cancel(boolean mayInterruptIfRunning) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * Cancels all the jobs. This is a non-blocking version, which may continue to
	 * try and cancel the jobs in the background. The job array state will remain
	 * RUNNING until the cancel operation succeeds.
	 * 
	 * @param timeoutInSeconds
	 *          maximum time for freeing resources.
	 * @exception NotImplementedException
	 *              is thrown if the implementation does not provide an
	 *              implementation of this method.
	 * @exception TimeoutException
	 *              is thrown when a remote operation did not complete
	 *              successfully because the network communication or the remote
	 *              service timed out.
	 * @exception IncorrectStateException
	 *              is thrown when the task is in NEW state.
	 * @exception NoSuccessException
	 *              is thrown when the operation was not successfully performed,
	 *              and none of the other exceptions apply.
	 */
	public void cancel(float timeoutInSeconds) throws NotImplementedException,
			IncorrectStateException, TimeoutException, NoSuccessException {

		if (phase == 1) {
			prepJob.cancel(timeoutInSeconds);
		}

		if (phase == 2) {
			for (Job i : jobArray) {
				i.cancel(timeoutInSeconds);
			}
		}

		if (phase == 3) {
			this.endJob.cancel(timeoutInSeconds);
		}
	}

	public boolean cb(Monitorable mt, Metric metric, Context ctx)
			throws NotImplementedException, AuthorizationFailedException {

		// Keep the callback.
		return true;
	}

	public void triggerCb(Monitorable mt, Metric metric, Context ctx)
			throws NotImplementedException, AuthorizationFailedException {

		try {
			String name = metric.getAttribute(Metric.NAME);

			Callback m = (Callback) Callbacks.get(name);
			if (m != null) {
				log.debug("Trigger callback " + name);
				m.cb(mt, metric, ctx);
			}
		} catch (Throwable e) {
			System.err.println("error" + e);
			if (log.isDebugEnabled()) {
				e.printStackTrace();
			}
		}

	}

	public void checkpoint() throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, IncorrectStateException, TimeoutException,
			NoSuccessException {
		// TODO Auto-generated method stub

	}

	public Task<Job, Void> checkpoint(TaskMode mode)
			throws NotImplementedException {
		// TODO Auto-generated method stub
		return null;
	}

	public Object clone() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean existsAttribute(String key) throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, TimeoutException, NoSuccessException {
		// TODO Auto-generated method stub
		return false;
	}

	public Task<Job, Boolean> existsAttribute(TaskMode mode, String key)
			throws NotImplementedException {
		// TODO Auto-generated method stub
		return null;
	}

	public String[] findAttributes(String... patterns)
			throws NotImplementedException, BadParameterException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, TimeoutException, NoSuccessException {
		// TODO Auto-generated method stub
		return null;
	}

	public Task<Job, String[]> findAttributes(TaskMode mode, String... patterns)
			throws NotImplementedException {
		// TODO Auto-generated method stub
		return null;
	}

	public Void get() throws InterruptedException, ExecutionException {
		// TODO Auto-generated method stub
		return null;
	}

	public Void get(long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, java.util.concurrent.TimeoutException {
		// TODO Auto-generated method stub
		return null;
	}

	public String getAttribute(String key) throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, IncorrectStateException,
			DoesNotExistException, TimeoutException, NoSuccessException {
		if (key == Job.EXITCODE) {
			String exitcodes = "";
			// check for all other jobs
			for (Job j : jobArray) {
				exitcodes += j.getAttribute(Job.EXITCODE) + ";";
			}
			return exitcodes;
		}
		return attributes.getAttribute(key);
	}

	public Task<Job, String> getAttribute(TaskMode mode, String key)
			throws NotImplementedException {
		// TODO Auto-generated method stub
		return null;
	}

	public String getGroup() throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, TimeoutException, NoSuccessException {
		// TODO Auto-generated method stub
		return null;
	}

	public Task<Job, String> getGroup(TaskMode mode)
			throws NotImplementedException {
		// TODO Auto-generated method stub
		return null;
	}

	public String getId() {
		return this.JobUID;
	}

	public Job[] getJobArray() {
		// TODO Auto-generated method stub
		return jobArray;
	}

	/**
	 * Retrieves the job description that was used to submit this job array
	 * instance.
	 * 
	 * @return the job description
	 * @exception NotImplementedException
	 *              is thrown if the implementation does not provide an
	 *              implementation of this method.
	 * @exception PermissionDeniedException
	 *              is thrown when the method failed because the identity used did
	 *              not have sufficient permissions to perform the operation
	 *              successfully.
	 * @exception AuthorizationFailedException
	 *              is thrown when none of the available contexts of the used
	 *              session could be used for successful authorization. This error
	 *              indicates that the resource could not be accessed at all, and
	 *              not that an operation was not available due to restricted
	 *              permissions.
	 * @exception AuthenticationFailedException
	 *              is thrown when operation failed because none of the available
	 *              session contexts could successfully be used for
	 *              authentication.
	 * @exception TimeoutException
	 *              is thrown when a remote operation did not complete
	 *              successfully because the network communication or the remote
	 *              service timed out.
	 * @exception DoesNotExistException
	 *              is thrown in cases where the job description is not available,
	 *              for instance when the job was not submitted through SAGA and
	 *              the job was obtained using the
	 *              {@link JobService#getJob(String)} call.
	 * @exception NoSuccessException
	 *              is thrown when the operation was not successfully performed,
	 *              and none of the other exceptions apply.
	 */
	public JobDescription getJobDescription() throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, DoesNotExistException, TimeoutException,
			NoSuccessException {
		return this.jd;
	}

	public Task<Job, JobDescription> getJobDescription(TaskMode mode)
			throws NotImplementedException {
		// TODO Auto-generated method stub
		return null;
	}

	public Metric getMetric(String name) throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, DoesNotExistException, TimeoutException,
			NoSuccessException {
		// TODO Auto-generated method stub
		return null;
	}

	public Void getObject() throws NotImplementedException, TimeoutException,
			NoSuccessException {
		// TODO Auto-generated method stub
		return null;
	}

	public String getOwner() throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, TimeoutException, NoSuccessException {
		// TODO Auto-generated method stub
		return null;
	}

	public int getPhase() throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, TimeoutException, NoSuccessException {
		return phase;
	}

	public Task<Job, String> getOwner(TaskMode mode)
			throws NotImplementedException {
		// TODO Auto-generated method stub
		return null;
	}

	public Void getResult() throws NotImplementedException,
			IncorrectStateException, TimeoutException, NoSuccessException {
		// TODO Auto-generated method stub
		return null;
	}

	public Session getSession() throws DoesNotExistException {
		return this.session;
	}

	/**
	 * Gets the state of the job array. This is an aggregated state derived from
	 * all jobs states.
	 * 
	 * @return the state of the task.
	 * @exception NotImplementedException
	 *              is thrown if the implementation does not provide an
	 *              implementation of this method.
	 * @exception TimeoutException
	 *              is thrown when a remote operation did not complete
	 *              successfully because the network communication or the remote
	 *              service timed out.
	 * @exception NoSuccessException
	 *              is thrown when the operation was not successfully performed,
	 *              and none of the other exceptions apply.
	 */
	public State getState() {
		return this.jobState;
	}

	public InputStream getStderr() throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, BadParameterException, DoesNotExistException,
			TimeoutException, IncorrectStateException, NoSuccessException {
		// TODO Auto-generated method stub
		return null;
	}

	public Task<Job, InputStream> getStderr(TaskMode mode)
			throws NotImplementedException {
		// TODO Auto-generated method stub
		return null;
	}

	public OutputStream getStdin() throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, BadParameterException, DoesNotExistException,
			TimeoutException, IncorrectStateException, NoSuccessException {
		// TODO Auto-generated method stub
		return null;
	}

	public Task<Job, OutputStream> getStdin(TaskMode mode)
			throws NotImplementedException {
		// TODO Auto-generated method stub
		return null;
	}

	public InputStream getStdout() throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, BadParameterException, DoesNotExistException,
			TimeoutException, IncorrectStateException, NoSuccessException {
		// TODO Auto-generated method stub
		return null;
	}

	public Task<Job, InputStream> getStdout(TaskMode mode)
			throws NotImplementedException {
		// TODO Auto-generated method stub
		return null;
	}

	public String getSubstitutedVariable(String var) {
		return this.SubstitutedVars.get(var);
	}

	public String[] getVectorAttribute(String key)
			throws NotImplementedException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			IncorrectStateException, DoesNotExistException, TimeoutException,
			NoSuccessException {
		// TODO Auto-generated method stub
		return null;
	}

	public Task<Job, String[]> getVectorAttribute(TaskMode mode, String key)
			throws NotImplementedException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ogf.saga.impl.job.JobImpl#isCancelled()
	 */
	public boolean isCancelled() {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ogf.saga.impl.job.JobImpl#isDone()
	 */
	public boolean isDone() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isReadOnlyAttribute(String key)
			throws NotImplementedException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			DoesNotExistException, TimeoutException, NoSuccessException {
		// TODO Auto-generated method stub
		return false;
	}

	public Task<Job, Boolean> isReadOnlyAttribute(TaskMode mode, String key)
			throws NotImplementedException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isRemovableAttribute(String key)
			throws NotImplementedException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			DoesNotExistException, TimeoutException, NoSuccessException {
		// TODO Auto-generated method stub
		return false;
	}

	public Task<Job, Boolean> isRemovableAttribute(TaskMode mode, String key)
			throws NotImplementedException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isVectorAttribute(String key) throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, DoesNotExistException, TimeoutException,
			NoSuccessException {
		// TODO Auto-generated method stub
		return false;
	}

	public Task<Job, Boolean> isVectorAttribute(TaskMode mode, String key)
			throws NotImplementedException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isWritableAttribute(String key)
			throws NotImplementedException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			DoesNotExistException, TimeoutException, NoSuccessException {
		// TODO Auto-generated method stub
		return false;
	}

	public Task<Job, Boolean> isWritableAttribute(TaskMode mode, String key)
			throws NotImplementedException {
		// TODO Auto-generated method stub
		return null;
	}

	public String[] listAttributes() throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, TimeoutException, NoSuccessException {
		// TODO Auto-generated method stub
		return null;
	}

	public Task<Job, String[]> listAttributes(TaskMode mode)
			throws NotImplementedException {
		// TODO Auto-generated method stub
		return null;
	}

	public String[] listMetrics() throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, TimeoutException, NoSuccessException {
		// TODO Auto-generated method stub
		return null;
	}

	public void migrate(JobDescription jd) throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, BadParameterException,
			IncorrectStateException, TimeoutException, NoSuccessException {
		// TODO Auto-generated method stub

	}

	public Task<Job, Void> migrate(TaskMode mode, JobDescription jd)
			throws NotImplementedException {
		// TODO Auto-generated method stub
		return null;
	}

	public void permissionsAllow(String id, int permissions)
			throws NotImplementedException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			BadParameterException, TimeoutException, NoSuccessException {
		// TODO Auto-generated method stub

	}

	public Task<Job, Void> permissionsAllow(TaskMode mode, String id,
			int permissions) throws NotImplementedException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean permissionsCheck(String id, int permissions)
			throws NotImplementedException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			BadParameterException, TimeoutException, NoSuccessException {
		// TODO Auto-generated method stub
		return false;
	}

	public Task<Job, Boolean> permissionsCheck(TaskMode mode, String id,
			int permissions) throws NotImplementedException {
		// TODO Auto-generated method stub
		return null;
	}

	public void permissionsDeny(String id, int permissions)
			throws NotImplementedException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			BadParameterException, TimeoutException, NoSuccessException {
		// TODO Auto-generated method stub

	}

	public Task<Job, Void> permissionsDeny(TaskMode mode, String id,
			int permissions) throws NotImplementedException {
		// TODO Auto-generated method stub
		return null;
	}

	public void removeAttribute(String key) throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, DoesNotExistException, TimeoutException,
			NoSuccessException {
		// TODO Auto-generated method stub

	}

	public Task<Job, Void> removeAttribute(TaskMode mode, String key)
			throws NotImplementedException {
		// TODO Auto-generated method stub
		return null;
	}

	public void removeCallback(String name, int cookie)
			throws NotImplementedException, DoesNotExistException,
			BadParameterException, TimeoutException, NoSuccessException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException {
		// TODO Auto-generated method stub

	}

	/**
	 * Asks the resource manager to perform a resume operation on all suspended
	 * jobs in the job array.
	 * 
	 * NOT IMPLEMENTED!
	 * 
	 * 
	 * @exception NotImplementedException
	 *              is thrown if the implementation does not provide an
	 *              implementation of this method.
	 * @exception PermissionDeniedException
	 *              is thrown when the method failed because the identity used did
	 *              not have sufficient permissions to perform the operation
	 *              successfully.
	 * @exception AuthorizationFailedException
	 *              is thrown when none of the available contexts of the used
	 *              session could be used for successful authorization. This error
	 *              indicates that the resource could not be accessed at all, and
	 *              not that an operation was not available due to restricted
	 *              permissions.
	 * @exception AuthenticationFailedException
	 *              is thrown when operation failed because none of the available
	 *              session contexts could successfully be used for
	 *              authentication.
	 * @exception TimeoutException
	 *              is thrown when a remote operation did not complete
	 *              successfully because the network communication or the remote
	 *              service timed out.
	 * @exception IncorrectStateException
	 *              is thrown when the job is not in SUSPENDED state.
	 * @exception NoSuccessException
	 *              is thrown when the operation was not successfully performed,
	 *              and none of the other exceptions apply.
	 */
	public void resume() throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, IncorrectStateException, TimeoutException,
			NoSuccessException {

		for (Job i : jobArray) {
			i.resume();
		}
	}

	public Task<Job, Void> resume(TaskMode mode) throws NotImplementedException {
		// TODO Auto-generated method stub
		return null;
	}

	public void rethrow() throws NotImplementedException, IncorrectURLException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, BadParameterException,
			IncorrectStateException, AlreadyExistsException, DoesNotExistException,
			TimeoutException, NoSuccessException {
		// TODO Auto-generated method stub

	}

	/**
	 * Starts the asynchronous operation on all jobs in job array.
	 * 
	 * @exception NotImplementedException
	 *              is thrown if the implementation does not provide an
	 *              implementation of this method.
	 * @exception TimeoutException
	 *              is thrown when a remote operation did not complete
	 *              successfully because the network communication or the remote
	 *              service timed out.
	 * @exception IncorrectStateException
	 *              is thrown when the task is not in NEW state.
	 * @exception NoSuccessException
	 *              is thrown when the operation was not successfully performed,
	 *              and none of the other exceptions apply.
	 */
	public void run() throws NotImplementedException, IncorrectStateException,
			TimeoutException, NoSuccessException {

		log.debug("Starting preparation Job ...");

		

		// preparation phase
		phase = 1;

		// start preparation job
		try {
			prepJob.run();
			prepJob.addCallback(Job.JOB_STATE, jic);
			prepJob.addCallback(Job.JOB_STATEDETAIL, jic);
		} catch (Exception e) {
			try {
				log.debug(e.getMessage());
				log.warn("[" + this.getId() + "] Proparation Job failed! Orchestrator will retry...");
				if (jic.retryJob(prepJob) <= 0) {
					log.error("[" + this.getId() + "] Preparation Job failed. Maximum retries reached!");
					throw new NoSuccessException("Preparation Job failed. Maximum retries reached!");
				}
			} catch (Exception e1) {
				log.error("[" + this.getId() + "] Job preparation failed. Job failed!");
				throw new NoSuccessException("Preparation Job failed!");
			}
		}


		log.debug("Preparation job started!");

	}

	public boolean runTasks() throws NotImplementedException,
			IncorrectStateException, TimeoutException, NoSuccessException {
		this.phase = 2;
		for (int j = 0; j < jobArray.length; j++) {
			log.debug("Starting task #" + (j + 1) + " ...");
			try {
				jobArray[j].run();
			} catch (NoSuccessException e) {
				if (log.isDebugEnabled()) {
					e.printStackTrace();
				}
				try {
					log.warn("[" + this.getId() + "] Task #" + (j + 1) + " submission"
							+ " failed! Orchestrator will retry...");
					if (jic.retryJob(jobArray[j]) <= 0) {
						log.error("[" + this.getId() + "] Task #" + (j + 1)
								+ " submission failed. Maximum retries reached!");
						return false;
					}
				} catch (Exception e1) {
					log.error("[" + this.getId() + "] Task #" + (j + 1)
							+  " submission failed. Job failed!");
					return false;
				}
			}
			try {
				jobArray[j].addCallback(Job.JOB_STATE, jic);
				jobArray[j].addCallback(Job.JOB_STATEDETAIL, jic);
			} catch (Exception e) {
				if (log.isDebugEnabled()) {
					e.printStackTrace();
				}
			}
			log.debug("Task #" + (j + 1) + " started!");
		}
		return true;
	}

	public boolean closeJob() throws NotImplementedException,
			IncorrectStateException, TimeoutException, NoSuccessException {

		// Add callback to preparation Job
		log.debug("Starting completion Job ...");

		this.phase = 3;

		try {
			endJob.run();
			endJob.addCallback(Job.JOB_STATE, jic);
			endJob.addCallback(Job.JOB_STATEDETAIL, jic);
		} catch (Exception e1) {
			if (log.isDebugEnabled()) {
				e1.printStackTrace();
			}
			return false;
		}

		log.debug("Completion Job started!");
		return true;

	}

	public void setAttribute(String key, String value)
			throws NotImplementedException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			IncorrectStateException, BadParameterException, DoesNotExistException,
			TimeoutException, NoSuccessException {
		attributes.setAttribute(key, value);

	}

	public Task<Job, Void> setAttribute(TaskMode mode, String key, String value)
			throws NotImplementedException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param jd
	 *          the jd to set
	 */
	public void setJobDescription(JobDescription jd) {
		this.jd = jd;
	}

	public void setVectorAttribute(String key, String[] values)
			throws NotImplementedException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			IncorrectStateException, BadParameterException, DoesNotExistException,
			TimeoutException, NoSuccessException {
		// TODO Auto-generated method stub

	}

	public Task<Job, Void> setVectorAttribute(TaskMode mode, String key,
			String[] values) throws NotImplementedException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Asks the resource manager to deliver an arbitrary signal to all dispatched
	 * job in the job array.
	 * 
	 * @exception NotImplementedException
	 *              is thrown if the implementation does not provide an
	 *              implementation of this method.
	 * @exception PermissionDeniedException
	 *              is thrown when the method failed because the identity used did
	 *              not have sufficient permissions to perform the operation
	 *              successfully.
	 * @exception AuthorizationFailedException
	 *              is thrown when none of the available contexts of the used
	 *              session could be used for successful authorization. This error
	 *              indicates that the resource could not be accessed at all, and
	 *              not that an operation was not available due to restricted
	 *              permissions.
	 * @exception AuthenticationFailedException
	 *              is thrown when operation failed because none of the available
	 *              session contexts could successfully be used for
	 *              authentication.
	 * @exception TimeoutException
	 *              is thrown when a remote operation did not complete
	 *              successfully because the network communication or the remote
	 *              service timed out.
	 * @exception IncorrectStateException
	 *              is thrown when the job is not in RUNNING or SUSPENDED state.
	 * @exception BadParameterException
	 *              is thrown if the specified signal is not supported by the
	 *              backend.
	 * @exception NoSuccessException
	 *              is thrown when the operation was not successfully performed,
	 *              and none of the other exceptions apply.
	 */
	public void signal(int arg0) throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, BadParameterException,
			IncorrectStateException, TimeoutException, NoSuccessException {

		for (Job i : jobArray) {
			i.signal(arg0);
		}

	}

	public Task<Job, Void> signal(TaskMode mode, int signum)
			throws NotImplementedException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Asks the resource manager to perform a suspend operation on all running
	 * jobs in the job array.
	 * 
	 * @exception NotImplementedException
	 *              is thrown if the implementation does not provide an
	 *              implementation of this method.
	 * @exception PermissionDeniedException
	 *              is thrown when the method failed because the identity used did
	 *              not have sufficient permissions to perform the operation
	 *              successfully.
	 * @exception AuthorizationFailedException
	 *              is thrown when none of the available contexts of the used
	 *              session could be used for successful authorization. This error
	 *              indicates that the resource could not be accessed at all, and
	 *              not that an operation was not available due to restricted
	 *              permissions.
	 * @exception AuthenticationFailedException
	 *              is thrown when operation failed because none of the available
	 *              session contexts could successfully be used for
	 *              authentication.
	 * @exception TimeoutException
	 *              is thrown when a remote operation did not complete
	 *              successfully because the network communication or the remote
	 *              service timed out.
	 * @exception IncorrectStateException
	 *              is thrown when the job is not in RUNNING state.
	 * @exception NoSuccessException
	 *              is thrown when the operation was not successfully performed,
	 *              and none of the other exceptions apply.
	 */
	public void suspend() throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, IncorrectStateException, TimeoutException,
			NoSuccessException {

		for (Job i : jobArray) {
			i.suspend();
		}

	}

	public Task<Job, Void> suspend(TaskMode mode) throws NotImplementedException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Waits for ALL jobs end up in a final state.
	 * 
	 * @exception NotImplementedException
	 *              is thrown if the implementation does not provide an
	 *              implementation of this method.
	 * @exception TimeoutException
	 *              is thrown when a remote operation did not complete
	 *              successfully because the network communication or the remote
	 *              service timed out.
	 * @exception IncorrectStateException
	 *              is thrown when the task is in NEW state.
	 * @exception NoSuccessException
	 *              is thrown when the operation was not successfully performed,
	 *              and none of the other exceptions apply.
	 */
	public void waitFor() throws NotImplementedException,
			IncorrectStateException, TimeoutException, NoSuccessException {

		jic.waitFor();

	}

	/**
	 * Waits for ALL jobs end up in a final state.
	 * 
	 * @param timeoutInSeconds
	 *          maximum number of seconds to wait.
	 * @return <code>true</code> if all jobs are finished within the specified
	 *         time.
	 * @exception NotImplementedException
	 *              is thrown if the implementation does not provide an
	 *              implementation of this method.
	 * @exception TimeoutException
	 *              is thrown when a remote operation did not complete
	 *              successfully because the network communication or the remote
	 *              service timed out.
	 * @exception IncorrectStateException
	 *              is thrown when the task is in NEW state.
	 * @exception NoSuccessException
	 *              is thrown when the operation was not successfully performed,
	 *              and none of the other exceptions apply.
	 */
	public boolean waitFor(float timeoutInSeconds)
			throws NotImplementedException, IncorrectStateException,
			TimeoutException, NoSuccessException {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @param rm
	 *          the rm to set
	 */
	public void setRm(URL rm) {
		this.rm = rm;
	}

	/**
	 * @return the rm
	 */
	public URL getRm() {
		return rm;
	}

	public void setState(State s) {
		this.jobState = s;
	}

}