package com.terradue.ssegrid.demo;

import org.ogf.saga.context.Context;
import org.ogf.saga.context.ContextFactory;
import org.ogf.saga.job.Job;
import org.ogf.saga.job.JobDescription;
import org.ogf.saga.job.JobService;
import org.ogf.saga.monitoring.Callback;
import org.ogf.saga.monitoring.Metric;
import org.ogf.saga.monitoring.Monitorable;
import org.ogf.saga.session.Session;
import org.ogf.saga.session.SessionFactory;
import org.ogf.saga.task.State;
import org.ogf.saga.url.URL;
import org.ogf.saga.url.URLFactory;
import com.terradue.grid.drivers.globus.GlobusGrid;
import com.terradue.grid.resource.Grid;
import com.terradue.ogf.saga.impl.job.JobImpl;
import com.terradue.ogf.saga.impl.job.JobDescriptionImpl;
import com.terradue.ogf.saga.impl.job.JobFactory;
import com.terradue.ogf.saga.impl.job.JobServiceImpl;
import com.terradue.ogf.schema.jsdl.JSDLDocument;
import com.terradue.ogf.schema.jsdl.impl.JSDLDocumentImpl;
import com.terradue.ogf.schema.jsdl.impl.JSDLFactory;
import com.terradue.ssegrid.sagaext.JobServiceAssistant;
import com.terradue.ssegrid.sagaext.MyProxyClient;
import com.terradue.ssegrid.sagaext.ProcessingRegistry;
import com.terradue.ssegrid.sagaext.ResourceBroker;
import com.terradue.ssegrid.sagaext.ResourceBrokerGlobus;

public class DemoJobSSEGrid implements Callback {

	private static URL gridmapGLUE;
	private static URL procJSDL;

	public static void main(String[] args) {

		if (args.length > 2) {
			System.err
					.println("Usage: java com.terradue.ssegrid.demo.DemoJobSSEGrid [<gridmapXML> <proctestJSDL]");
			System.exit(1);
		} else if (args.length == 2) {
			try {
				gridmapGLUE = URLFactory.createURL(args[0]);
				procJSDL = URLFactory.createURL(args[1]);
			}
			catch (Exception e){
				System.out.println("Got exception " + e);
				e.getCause().printStackTrace();
			}
		}

		try {

			/*
			 * --- javaSAGA extension setup ---
			 * 
			 * The following operations replaces the context configuration of javaSAGA
			 */

			// GRID Map definition from XML configuration
			if (gridmapGLUE == null)
				gridmapGLUE = URLFactory.createURL("http://maps.terradue.com/ssegrid/demo/gridmap.xml");

			// Initialization of the processing registry
			// By default, the class will load the JSDL templates already present in
			// the physical directory pointed by the environment variable
			// "SSEGRID_PROC_DIR"
			ProcessingRegistry pr = new ProcessingRegistry();

			// Registration of a processing described by a JSDL template
			if (procJSDL == null)
				procJSDL = URLFactory.createURL(
						"http://maps.terradue.com/ssegrid/demo/importMODIS_JSDLTemplate.xml");
			int myproc = pr.registerProcessing(JSDLFactory.createJSDLDocument(procJSDL));

			/*
			 * -- End of javaSAGA extension setup ---
			 */
			
			
			/*
			 * -- Job Session Start ---
			 */

			// First create a session containing at least a context
			// with user credentials information
			Session session = SessionFactory.createSession(true);
			Context context = ContextFactory.createContext();
			context.setAttribute(context.USERPROXY, "/home/wps/execute/jobs/12/proxy");
			session.addContext(context);
			
			// Get delegation to that user proxy
			MyProxyClient.delegateProxyFromMyProxyServer("ify-ce03.terradue.com", 7512, "emathot", "***password***", 604800, 501, "/home/wps/execute/jobs/12/proxy");
			
			// First create a JobService from the ResourceBroker
			// that is ready to handle job submission
			// N.B. here is an "extended" JobService
			JobServiceImpl js = JobFactory.createJobService(session, gridmapGLUE);
			
			// Then the JobServiceAssistant in order to provide
			// useful methods for the backend WPS process manager
			JobServiceAssistant jsa = new JobServiceAssistant(js);

			// Then initialize a job description from a registered processing
			JobDescription jd = JobFactory.createJobDescriptionFromJSDLDocument(pr.getJSDLFromProc(myproc));

			// In principle, there are not many parameters to be changed if the
			// job is already described in the JSDL template but it is still
			// possible to make change here on the fly.
			// Here our agreed standard INPUT: inputURLs.${TASK_ID}
			// N.B. This should have been defined before with the PI and therefore 
			// already defined in the JSDL template
			jd.setAttribute(JobDescription.INPUT, "inputURLs.${TASK_ID}");
			
			// Add here WPS input substitution method
			//jsa.substituteInput(jd,WPSParameter);

			// Once JobDescription ready to run, create the jobs, run them, and wait for them.
			// N.B. In this case, the "extension" provides a new class to handle an array of
			// jobs; they are still accessible individually from the attributes
			System.out.println("Creating job in 5 instances");
			JobImpl jobs = js.createNJob(jd,5);
			
			// In the processing template, the input is passed through an agreed
			// standard: inputURLs.${TASK_ID}
			// Before running the job, setup the inputURLs.* from a String (e.g. XML through XSL)
			// Here I have 15 files to process, 3 in each instances.
			
			
			jobs.addCallback(Job.JOB_STATE, new DemoJobSSEGrid());
			jobs.addCallback(Job.JOB_STATEDETAIL, new DemoJobSSEGrid());
			jobs.run();
			jobs.waitFor();
			
      if (jobs.getState() == State.FAILED) {
        throw new Error("Jobs failed");
      }
      if (jobs.getState() == State.DONE) {
      	System.out.println("Jobs completed");
      }


		} catch (Throwable e) {

			System.out.println("Got exception " + e);
			e.getCause().printStackTrace();
			
		}
	}

	// Callback monitors job.
	public boolean cb(Monitorable m, Metric metric, Context ctxt) {
		try {
			String value = metric.getAttribute(Metric.VALUE);
			String name = metric.getAttribute(Metric.NAME);
			System.out.println("Callback called for metric " + name + ", value = "
					+ value);
		} catch (Throwable e) {
			System.err.println("error" + e);
			e.printStackTrace(System.err);
		}
		
		// Keep the callback.
		return true;
	}
}
