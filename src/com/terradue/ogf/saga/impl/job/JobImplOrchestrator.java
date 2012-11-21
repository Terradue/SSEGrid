/**
 * 
 */
package com.terradue.ogf.saga.impl.job;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import org.ogf.saga.error.SagaIOException;
import org.ogf.saga.error.TimeoutException;
import org.ogf.saga.impl.monitoring.MetricImpl;
import org.ogf.saga.job.Job;
import org.ogf.saga.monitoring.Callback;
import org.ogf.saga.monitoring.Metric;
import org.ogf.saga.monitoring.Monitorable;
import org.ogf.saga.task.State;

/**
 * @author emathot
 * 
 */
public class JobImplOrchestrator implements Callback {

	private JobImpl jobcb = null;

	// Logger
	private Logger log = Logger.getLogger(this.getClass());

	// properties
	private int submission_retry = 5;

	// Action ongoing
	private Map<Job, Integer> jobretries = null;

	// overall state
	private State jobState = State.NEW;
	private State tasksState = State.NEW;

	private boolean submissionError = false;

	JobImplOrchestrator(JobImpl cbJob) {
		this.jobcb = cbJob;
		if (System.getProperty("gai.submission.retry") != null) {
			submission_retry = Integer.parseInt(System
					.getProperty("gai.submission.retry"));
		}
		jobretries = new ConcurrentHashMap<Job, Integer>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ogf.saga.monitoring.Callback#cb(org.ogf.saga.monitoring.Monitorable,
	 * org.ogf.saga.monitoring.Metric, org.ogf.saga.context.Context)
	 */
	public boolean cb(Monitorable mt, Metric metric, Context ctx)
			throws NotImplementedException, AuthorizationFailedException {

		Job job = null;
		String value = null;
		String name = null;

		int phase;

		// Detects if it is a JobImpl Object
		try {
			job = (Job) mt;
			log.debug("Callback triggered from Job " + jobcb.getId());
			phase = jobcb.getPhase();
		} catch (Exception e) {
			// ignore because it is not a callback from a JobImpl object
			log.debug("Callback for unidentified object: " + e.getMessage());
			return true;
		}

		// Detects the callback
		try {
			value = metric.getAttribute(Metric.VALUE);
			name = metric.getAttribute(Metric.NAME);

			log.info("[" + jobcb.getId() + "] Phase " + phase + ", " + name + ": "
					+ value);

			// When preparation job
			if (phase == 1) {

				// Job is running
				if (value.equals("RUNNING") && name.equals(Job.JOB_STATE)) {
					// trigger global call back: Job running
					this.setState(State.RUNNING);
					jobcb.triggerCb(jobcb, metric, ctx);
					return true;
				}

				// if preparation job is prestaging
				if (value.contains("PRE_STAGING") && name.equals(Job.JOB_STATEDETAIL)) {
					// trigger global callback: Job prestaging
					jobcb.triggerCb(jobcb, metric, ctx);
					return true;
				}

				// preparation fails in submission
				if (name.equals(Job.JOB_STATEDETAIL)
						&& value.contains("SUBMISSION_ERROR")) {

					// retry to submit preparation job
					int rc = this.retryJob(job);
					if (rc > 0) {
						log.warn("[" + jobcb.getId()
								+ "] Preparation job cannot be submitted. Retry #" + rc);
						return true;
					} else {
						// Job failed
						log.error("[" + jobcb.getId()
								+ "] Maximum retries reached. Job failed!");
						jobcb.triggerCb(jobcb, metric, ctx);
						this.setState(State.FAILED);
						this.cleanJob();
						return false;
					}
				}

				// preparation did not run correctly
				if (name.equals(Job.JOB_STATEDETAIL) && value.contains("STOPPED")) {

					// never started
					if (job.getAttribute(Job.STARTED) == "0") {
						// retry to submit preparation job
						int rc = this.retryJob(job);
						if (rc > 0) {
							log.warn("[" + jobcb.getId()
									+ "] Preparation job seems to have not started. Retry #" + rc);
							return true;
						} else {
							// Job failed
							log.error("[" + jobcb.getId()
									+ "] Maximum retries reached. Job failed!");
							jobcb.triggerCb(jobcb, metric, ctx);
							this.setState(State.FAILED);
							this.cleanJob();
							return false;
						}
					}
					return true;
				}

				// preparation fails
				if (name.equals(Job.JOB_STATE) && value.contains("FAILED")) {

					// retry to submit preparation job
					log.warn("[" + jobcb.getId()
							+ "] Preparation failed! Orchestrator will retry...");
					int rc = this.retryJob(job);
					if (rc > 0) {
						return true;
					} else {
						// Job failed
						log.error("[" + jobcb.getId()
								+ "] Maximum retries reached. Job failed!");
						jobcb.triggerCb(jobcb, metric, ctx);
						this.setState(State.FAILED);
						this.cleanJob();
						return false;
					}
				}

				// preparation has been canceled
				if (value.equals("CANCELED") && name.equals(Job.JOB_STATE)) {
					jobcb.triggerCb(jobcb, metric, ctx);
					this.setState(State.CANCELED);
					this.cleanJob();
					return false;
				}

				// preparation job done
				if (value.equals("DONE") && name.equals(Job.JOB_STATE)) {

					// get exit code
					log.debug("EXIT CODE: " + job.getAttribute(Job.EXITCODE));
					int prepec;
					try {
						prepec = Integer.parseInt(job.getAttribute(Job.EXITCODE));
						log.info("[" + jobcb.getId()
								+ "] Preparation Job DONE with exit code " + prepec);
					} catch (NumberFormatException e) {
						log.warn("["
								+ jobcb.getId()
								+ "] No exit code returned! Assuming Preparation failed. Orchestrator will retry...");
						prepec = -1;
					}
					if (prepec != 0) {
						// retry to submit preparation job
						log.warn("[" + jobcb.getId()
								+ "] Preparation failed! Orchestrator will retry...");
						int rc = this.retryJob(job);
						if (rc > 0) {
							log.warn("[" + jobcb.getId() + "] Retry #" + rc);
							return true;
						} else {
							// preparation job failed
							log.error("[" + jobcb.getId()
									+ "] Maximum retries reached. Job Failed!");
							log.debug(job.listMetrics());
							MetricImpl m = new MetricImpl(
									job,
									job.getSession(),
									Job.JOB_STATE,
									"fires on state changes of the job, and has the literal value of the job state enum",
									"ReadOnly", "1", "Enum", "New");
							m.setValue("FAILED");
							jobcb.triggerCb(jobcb, m, ctx);
							this.setState(State.FAILED);
							this.cleanJob();
							return false;
						}
					} else {
						this.jobretries.remove(job);
						if (jobcb.runTasks() == false) {
							this.setState(State.FAILED);
							return false;
						}
						log.info("All Tasks started successfully! Waiting for callbacks.");
					}
				}

				return true;
			}

			// When jobs are running
			if (phase == 2) {

				// Find which job
				int jobnbr = 0;
				Job[] jobArray = jobcb.getJobArray();
				for (int j = 0; j < jobArray.length; j++) {
					if (((Job) jobArray[j]).getId().equals(((Job) mt).getId())) {
						jobnbr = j + 1;
					}
				}
				log.debug("[" + jobcb.getId() + "] Task #" + jobnbr + " " + name
						+ " is " + value);

				// When it is a state detail
				if (name.equals(Job.JOB_STATEDETAIL)) {
					// and is running or scheduled or stopped
					jobcb.triggerCb(jobcb, metric, ctx);
					if (value.contains("SUBMISSION_ERROR")) {
						log.error("[" + jobcb.getId() + "] Task #" + jobnbr + " "
								+ "submission error.");
						submissionError = true;
						return true;
					}
				}

				if (name.equals(Job.JOB_STATE)) {
					// If one of the jobs fails
					if (value.equals("FAILED")) {
						if (this.submissionError == true) {
							log.error("[" + jobcb.getId() + "] Task #" + jobnbr
									+ " submission failed for unknown reason (see infrastrcuture log)! Job Failed. Cancelling other tasks!");
							this.cleanJob();
						} else {
							// retry to submit task job
							log.warn("[" + jobcb.getId() + "] Task #" + jobnbr + " "
									+ " failed! Orchestrator will retry...");
							int rc = this.retryJob(job);
							if (rc > 0) {
								return true;
							} else {
								log.error("[" + jobcb.getId() + "] Task #" + jobnbr
										+ " Maximum retries reached. Job Failed!");
								log.error("[" + jobcb.getId() + "] Task #" + jobnbr + " "
										+ "FAILED. Canceling other tasks!");
								jobcb.triggerCb(jobcb, metric, ctx);
							}
						}
						boolean allFinal = true;
						// check for all other jobs
						for (Job j : jobArray) {
							allFinal &= (j.isCancelled() || j.isDone());
						}
						// If all canceled
						if (allFinal) {
							log.info("[" + jobcb.getId() + "] All tasks canceled!");
							jobcb.triggerCb(jobcb, metric, ctx);
							if (jobcb.closeJob() == false) {
								log.error("[" + jobcb.getId() + "] Completion job failed!");
								this.setState(State.FAILED);
								return false;
							}
							this.tasksState = State.FAILED;
						}
						return true;
					}
					// If one of the jobs is canceled
					if (value.equals("CANCELED")) {
						log.error("[" + jobcb.getId() + "] Task #" + jobnbr + " "
								+ "Canceled!");
						boolean allCanceled = true;
						// check for all other jobs
						for (Job j : jobArray) {
							allCanceled &= (j.isCancelled() || j.isDone());
						}
						// If all canceled
						if (allCanceled) {
							log.info("[" + jobcb.getId() + "] All tasks canceled!");
							this.setState(State.CANCELED);
							jobcb.triggerCb(jobcb, metric, ctx);
							if (jobcb.closeJob() == false) {
								log.error("[" + jobcb.getId() + "] Completion job failed!");
								this.setState(State.FAILED);
								return false;
							}
							this.tasksState = State.CANCELED;
						}
						return true;
					}
					// If one of the jobs is done
					if (value.equals("DONE")) {
						log.info("[" + jobcb.getId() + "] Task #" + jobnbr + " DONE");
						boolean allDone = true;
						// check for all other jobs
						for (Job j : jobArray) {
							allDone &= j.isDone();
						}
						// If all done
						if (allDone) {
							this.tasksState = State.DONE;
							log.info("[" + jobcb.getId() + "] All tasks completed!");
							if (jobcb.closeJob() == false) {
								log.error("[" + jobcb.getId()
										+ "] Completion job failed. Job failed!");
								this.setState(State.DONE);
							} else {
								log.debug("Waiting for Completion Job...");
							}
						}
						return true;
					}
				}
			}

			// When completion job is running
			if (phase == 3) {
				// Job is running
				if (value.equals("RUNNING") && name.equals(Job.JOB_STATE)) {
					// skip
					return true;
				}

				// if completion job is prestaging
				if (value.contains("POST_STAGING") && name.equals(Job.JOB_STATEDETAIL)) {
					jobcb.triggerCb(jobcb, metric, ctx);
					return true;
				}

				// preparation fails in submission
				if (name.equals(Job.JOB_STATEDETAIL)
						&& value.contains("SUBMISSION_ERROR")) {

					log.warn("[" + jobcb.getId()
							+ "] Completion job cannot be submitted.");
					this.setState(this.tasksState);
					return false;
				}

				// completion fails
				if (name.equals(Job.JOB_STATE) && value.contains("FAILED")) {

					// retry to submit preparation job
					log.warn("[" + jobcb.getId()
							+ "] Completion job failed! Orchestrator will retry...");
					int rc = this.retryJob(job);
					if (rc > 0) {
						log.info("[" + jobcb.getId() + "] Retry completion job #" + rc);
						return true;
					} else {
						// Job failed
						log.error("[" + jobcb.getId()
								+ "] Maximum retries reached. Job failed!");
						this.setState(this.tasksState);
						return false;
					}
				}

				// completion job done
				if (value.equals("DONE") && name.equals(Job.JOB_STATE)) {

					// get exit code
					log.debug("EXIT CODE: " + job.getAttribute(Job.EXITCODE));
					int prepec = Integer.parseInt(job.getAttribute(Job.EXITCODE));
					if (prepec != 0) {
						log.info("[" + jobcb.getId()
								+ "] Completion Job DONE but exit code is " + prepec);
						// retry to submit preparation job
						log.warn("[" + jobcb.getId()
								+ "] Completion job failed! Orchestrator will retry...");
						int rc = this.retryJob(job);
						if (rc > 0) {
							this.setState(this.tasksState);
							return false;
						} else {
							// completion job failed
							log.error("[" + jobcb.getId()
									+ "] Maximum retries reached. Job Failed!");
							this.setState(this.tasksState);
							return true;
						}
					} else {
						this.setState(this.tasksState);
						return false;
					}
				}
			}

		} catch (Throwable e) {
			e.printStackTrace(System.err);
		}
		// Keep the callback.
		return true;
	}

	public int retryJob(Job job) throws NotImplementedException,
			IncorrectStateException, TimeoutException, NoSuccessException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, BadParameterException, IncorrectURLException,
			DoesNotExistException {

		Integer rj = (Integer) this.jobretries.get(job);
		// already in retry
		if (rj != null) {
			rj--;
		} else {
			// start retries
			rj = new Integer(this.submission_retry);
		}
		if (rj > 0) {
			log.warn("[" + jobcb.getId() + "] Retry #"
					+ (this.submission_retry - rj + 1));
			log.debug("Restart previous job (clone & run)");
			System.out.flush();
			job.cancel();
			Job newjob, oldjob;
			newjob = org.ogf.saga.job.JobFactory.createJobService(job.getSession(),
					jobcb.getRm()).createJob(job.getJobDescription());
			int jobnbr=-1;
			Job[] jobArray = jobcb.getJobArray();
			for (int j = 0; j < jobArray.length; j++) {
				if (((Job) jobArray[j]).getId().equals((job).getId())) {
					jobnbr = j;
				}
			}
			oldjob=job;
			job = newjob;
			if ( jobnbr >= 0 ){
				jobArray[jobnbr] = newjob;
			}
			this.jobretries.put(job, rj);
			try {
				Thread.sleep(500);
				job.run();
			} catch (NoSuccessException e) {
				if (log.isDebugEnabled()) {
					e.printStackTrace();
				}
				if (this.retryJob(job) <= 0) {
					throw (e);
				}
			} catch (InterruptedException e) {
				// skip
			}
			log.debug("Job restarted!");
			try {
				job.addCallback(Job.JOB_STATE, this);
				job.addCallback(Job.JOB_STATEDETAIL, this);
			} catch (IncorrectStateException e) {
				log.debug("Job finished before callback. Need to restart.");
				return (this.submission_retry - rj + 1);
			}

			return (this.submission_retry - rj + 1);
		}

		this.jobretries.remove(job);
		return 0;

	}

	private void cleanJob() throws NotImplementedException,
			IncorrectStateException, TimeoutException, NoSuccessException {
		jobcb.cancel();
	}

	public void waitFor() {

		while (this.getState() == State.NEW || this.getState() == State.RUNNING) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// skip
			}
		}
		log.debug("Wait for exits on State " + this.getState());
	}

	public synchronized State getState() {
		return jobcb.getState();
	}

	private synchronized void setState(State s) {
		jobcb.setState(s);
	}
}
