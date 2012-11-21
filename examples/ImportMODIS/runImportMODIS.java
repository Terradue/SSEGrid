package com.terradue.ssegrid.demo;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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
import org.w3c.dom.Document;

import com.terradue.ogf.saga.impl.job.JobArrayImpl;
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

public class runImportMODIS implements Callback {

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
			} catch (Exception e) {
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

			// GRID Map GLUE definition from XML configuration
			if (gridmapGLUE == null)
				gridmapGLUE = URLFactory
						.createURL("http://maps.terradue.com/ssegrid/examples/ResourceManager/VITOGrid_GLUE.xml");

			// Initialization of the processing registry
			// By default, the class will load the JSDL templates already present in
			// the physical directory pointed by the environment variable
			// "GAI_DEPLOY_PROCESS_DIR"
			ProcessingRegistry pr = new ProcessingRegistry(true);

			// Registration of a processing described by a JSDL template
			if (procJSDL == null)
				procJSDL = URLFactory
						.createURL("http://maps.terradue.com/ssegrid/example/ImportMODIS/importMODIS_JSDLTemplate.xml");
			String myproc = pr.registerProcessing(JSDLFactory
					.createJSDLDocument(procJSDL));
			
			String WPSHome = System.getenv("WPS_HOME");

			// prepare Map with all persistent substitution variable
			java.util.Map WPSSubstitutionVar = new java.util.HashMap<String, String>();
			WPSSubstitutionVar.put("WPS_DEPLOY_PROCESS_DIR",
					WPSHome+"/deploy/process/");
			WPSSubstitutionVar.put("WPS_DEPLOY_AUXDATA_DIR",
					WPSHome+"/deploy/auxdata/");
			WPSSubstitutionVar.put("WPS_JOB_INPUTS_DIR",
					WPSHome+"/execute/jobs/${GAI_JOB_UID}/inputs");
			WPSSubstitutionVar.put("WPS_JOB_OUTPUTS_DIR",
					WPSHome+"/execute/jobs/${GAI_JOB_UID}/outputs");

			/*
			 * -- End of javaSAGA extension setup ---
			 */

			/*
			 * -- Job Session Start ---
			 */

			// First create a session containing at least a context
			// for user credentials information
			Session session = SessionFactory.createSession(true);
			Context context = ContextFactory.createContext();
			context.setAttribute(Context.USERPROXY, WPSHome+"/proxy");
			session.addContext(context);

			// Get delegation to that user proxy and set propoerly context
			MyProxyClient.delegateProxyFromMyProxyServer("ify-ce03.terradue.com",
					7512, "emathot", "***password***", 604800, context);

			// then create a JobService from the JobFactory
			// that is ready to handle job submission passing the session information
			// and the Resource Manager
			// N.B. here is an "extended" JobService
			JobService js = JobFactory.createJobService(session, gridmapGLUE);
		  // Pass the additional substitution
			js.addSubstitutionVariables(WPSSubstitutionVar);

			// create the JobServiceAssistant in order to provide
			// useful methods for the backend WPS process manager
			JobServiceAssistant jsa = new JobServiceAssistant(js);
			

			// initialize a job description from the registered processing
			JobDescription jd = JobFactory.createJobDescription(pr
					.getJSDLFromProc(myproc));

			// Simulation of WPS input substitution method
			java.util.Map WPS_INPUT = new java.util.HashMap<String, String>();
			WPS_INPUT.put("WPS_INPUT_roiTopLeftLat", "72");
			WPS_INPUT.put("WPS_INPUT_roiTopLeftLon", "-15");
			WPS_INPUT.put("WPS_INPUT_roiBottomRightLat", "28");
			WPS_INPUT.put("WPS_INPUT_roiBottomRightLon", "60");
			WPS_INPUT.put("WPS_INPUT_stopOnError", "true");
			// Run 3 tasks
			WPS_INPUT.put("WPS_INPUT_numberOfTasks", "3");

			jsa.substituteSimpleInputs(jd, WPS_INPUT);

			// Once JobDescription ready to run, create the jobs, run them, and wait
			// for them.
			// N.B. In this case, the "extension" provides a new class to handle an
			// array of jobs; they are still accessible individually from the
			// attributes
			System.out.println("Creating job in " + jd.getAttribute("NUMBEROFPROCESSES") + " instances");
			JobArrayImpl jobs = ((JobServiceImpl) js).createNJob(jd);

			// create now the local job execute dirs
			String WPSJobExecuteDir = "/home/wps/execute/jobs/" + jobs.JOBID;
			String WPSJobInputDir = WPSJobExecuteDir + "/inputs";
			String WPSJobOutputDir = WPSJobExecuteDir + "/outputs";
			(new File(WPSJobExecuteDir)).mkdir();
			(new File(WPSJobInputDir)).mkdir();
			(new File(WPSJobOutputDir)).mkdir();

			// In the processing template, the input is passed through an agreed
			// standard: inputURLs.${GAI_TASK_ID}
			// we simulate the ouput from the previous processing in InputURLList.xml
			File file = new File("./InputURLList.xml");
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document inputXML = db.parse(file);
			java.util.Map inputXMLMap = new java.util.HashMap<String, org.w3c.dom.Document>();
			inputXMLMap.put("InputURLs", inputXML);
			jsa.writeComplexInputs(jobs, inputXMLMap);

			jobs.addCallback(Job.JOB_STATE, new runImportMODIS());
			jobs.addCallback(Job.JOB_STATEDETAIL, new runImportMODIS());
			// Let's GO!
			jobs.run();
			// wait for all jobs in the job array
			jobs.waitFor();

			if (jobs.getState() == State.FAILED) {
				throw new Error("Jobs failed" + jsa.readExitMessages(jobs));
			}
			if (jobs.getState() == State.DONE) {
				System.out.println("Jobs completed");
				java.util.Map simpleoutputMap = jsa.readComplexOutputs(jobs,
						new String[] { "NbrImportedProduct" });
				System.out.println("Here is the number of output by process:");
				System.out.println(simpleoutputMap);
				java.util.Map complexoutputXMLMap = jsa.readComplexOutputs(jobs,
						new String[] { "ImportedList" });
				Document complexoutputXML = (Document) complexoutputXMLMap
						.get("importedList");
				System.out.println("Here is the output URLs");
				System.out.println(complexoutputXML);
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
