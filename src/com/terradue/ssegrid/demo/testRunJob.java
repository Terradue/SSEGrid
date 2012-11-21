package com.terradue.ssegrid.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URLConnection;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.ogf.saga.context.Context;
import org.ogf.saga.context.ContextFactory;
import org.ogf.saga.error.BadParameterException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.job.Job;
import org.ogf.saga.monitoring.Callback;
import org.ogf.saga.monitoring.Metric;
import org.ogf.saga.monitoring.Monitorable;
import org.ogf.saga.session.Session;
import org.ogf.saga.session.SessionFactory;
import org.ogf.saga.task.State;
import org.ogf.saga.url.URL;
import org.ogf.saga.url.URLFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.terradue.ogf.saga.impl.job.JobImpl;
import com.terradue.ogf.saga.impl.job.JobDescription;
import com.terradue.ogf.saga.impl.job.JobFactory;
import com.terradue.ogf.saga.impl.job.JobServiceImpl;
import com.terradue.ogf.schema.jsdl.JSDLFactory;
import com.terradue.ssegrid.sagaext.FindFilter;
import com.terradue.ssegrid.sagaext.JobServiceAssistant;
import com.terradue.ssegrid.sagaext.MyProxyClient;
import com.terradue.ssegrid.sagaext.ProcessingRegistry;

public class testRunJob implements Callback {

	private static URL gridmapGLUE;
	private static String WPSHome, WPSTemplateHome;
	private static String WPS_Deploy_Process_Dir;
	private static String wps_input_file;
	private static int numberOfProcess = 0;
	public static void main(String[] args) throws BadParameterException {
		if (args.length != 2 && args.length != 3) {
			System.err
					.println("Usage: java com.terradue.ssegrid.demo.testRunJob [<Template Dir Path> <Wpa inputs file>]");
			System.exit(1);
		} else if (args.length == 2){
			try {
				WPSTemplateHome = args[0];
				wps_input_file = args[1];
			} catch (Exception e) {
				System.out.println("Got exception " + e);
				e.getCause().printStackTrace();
			}
		} else {
			try {
				WPSTemplateHome = args[0];
				wps_input_file = args[1];
				numberOfProcess = new Integer (args[2]).intValue();
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
			
			System.setProperty("JobService.adaptor.name", "javaGAT");

			// GRID Map GLUE definition from XML configuration
			if (gridmapGLUE == null)
				gridmapGLUE = URLFactory
						.createURL(/*"http://storage.terradue.com/ssegrid/T2Grid_GLUE.xml"*/
								System.getProperty("gai.default.rm"));

			// Initialization of the processing registry
			// By default, the class will load the JSDL templates already present in
			// the physical directory pointed by the environment variable
			// "GAI_DEPLOY_PROCESS_DIR"
			ProcessingRegistry pr = new ProcessingRegistry(false);
			if (WPSHome == null)
				WPSHome = System.getenv("WPS_HOME");
			// prepare Map with all persistent substitution variable
			java.util.Map<String, String> WPSSubstitutionVar = new java.util.HashMap<String, String>();
			if (WPS_Deploy_Process_Dir == null)
				WPS_Deploy_Process_Dir = WPSHome + "/deploy/process";
			WPSSubstitutionVar.put("WPS_DEPLOY_PROCESS_DIR",
					WPS_Deploy_Process_Dir);
			WPSSubstitutionVar.put("WPS_DEPLOY_AUXDATA_DIR",
					WPSHome + "/deploy/auxdata/");
			WPSSubstitutionVar.put("WPS_USER_ID","emathot");
			WPSSubstitutionVar.put("WPS_PROCESS_ID","simpleJob");
			WPSSubstitutionVar.put("WPS_PROCESS_INST_ID","INST_1");
			WPSSubstitutionVar.put("WPS_JOB_INPUTS_DIR",
					WPSHome + "/execute/${WPS_USER_ID}/${WPS_PROCESS_ID}/${WPS_PROCESS_INST_ID}/${GAI_JOB_UID}/inputs");
			WPSSubstitutionVar.put("WPS_JOB_OUTPUTS_DIR",
					WPSHome + "/execute/${WPS_USER_ID}/${WPS_PROCESS_ID}/${WPS_PROCESS_INST_ID}/${GAI_JOB_UID}/outputs");
			WPSSubstitutionVar.put("WPS_JOB_AUDITS_DIR",
					WPSHome + "/execute/${WPS_USER_ID}/${WPS_PROCESS_ID}/${WPS_PROCESS_INST_ID}/${GAI_JOB_UID}/audits");

			/*
			 * -- End of javaSAGA extension setup ---
			 */

			/*
			 * -- Job Session Start ---
			 */

			// First create a session containing at least a context  
			// for user credentials information
			Session session = SessionFactory.createSession(false);
			Context context = ContextFactory.createContext("globus");
			context.setAttribute(Context.USERPROXY, WPSHome + "/proxy");
			session.addContext(context);
			System.out.println(context.getAttribute(Context.USERPROXY));

			// Get delegation to that user proxy and set properly context
			MyProxyClient.delegateProxyFromMyProxyServer("ce1.intern.vgt.vito.be",
					7512, "t2user", "myproxy", 604800, context);

			// then create a JobService from the JobFactory
			// that is ready to handle job submission passing the session information
			// and the Resource Manager
			// N.B. here is an "extended" JobService
			JobServiceImpl js = JobFactory.createJobService(session/*, gridmapGLUE*/);
		  
			// create the JobServiceAssistant in order to provide
			// useful methods for the backend WPS process manager
			JobServiceAssistant jsa = new JobServiceAssistant(js);
			// Pass the additional substitution
			jsa.addSubstitutionVariables(WPSSubstitutionVar);
			// Simulation of WPS input substitution method
			java.util.Map<String, String> WPS_INPUT = new java.util.HashMap<String, String>();
			if (wps_input_file != null){
				importWps(wps_input_file, WPS_INPUT);
			}else{
				WPS_INPUT.put("WPS_INPUT_roiTopLeftLat",     "72");
				WPS_INPUT.put("WPS_INPUT_roiTopLeftLon",     "-15");
				WPS_INPUT.put("WPS_INPUT_roiBottomRightLat", "28");
				WPS_INPUT.put("WPS_INPUT_roiBottomRightLon", "60");
			}
			
			//for every process recovered by registerProcessing
		/*	for (int i = 0; i < procList.size(); i++){				
				String name = procList.keySet().toArray(new String[0])[i];
				System.out.println("Name " + name + " " + procList.size() + " " + System.getProperty("gai.deploy.process.path"));
				JobDescription jd = (JobDescription) JobFactory.createJobDescription(pr
						.getJSDLFromProc((String)name));
			*/
				FindFilter fileFilter = new FindFilter("*JSDLTemplate.xml");
				File[] files = new File(WPSTemplateHome).listFiles(fileFilter);
		    	if ( files != null && files.length > 0){
		    	//	System.out.println("Recovering processing: " + files[j].getAbsolutePath());	
		    		pr.registerProcessing(JSDLFactory.createJSDLDocument(files[0]));
		    		Map<String, String> procList = pr.listProcessing();
		    		String name = procList.keySet().toArray(new String[0])[0];
		    		JobDescription jd = (JobDescription) JobFactory.createJobDescription(pr
							.getJSDLFromProc((String)name));
					jsa.substituteSimpleInputs(jd, WPS_INPUT);
					// Once JobDescription ready to run, create the jobs, run them, and wait
					// for them.
					// N.B. In this case, the "extension" provides a new class to handle an
					// array of jobs; they are still accessible individually from the
					// attributes
					if (numberOfProcess > 0)
						System.out.println("Creating job in " + numberOfProcess + " instances");
					else
						System.out.println("Creating job in " + ((com.terradue.ogf.saga.impl.job.JobDescriptionImpl)jd).getAttribute("NumberOfProcesses") + " instances");
					//JobDescription jd2 = ((JobServiceImpl)js).substituteVariables(jd);
					//JobImpl job = (JobImpl) ((JobServiceImpl)js).createJob(jd);
					JobImpl jobs = ((JobServiceImpl) js).createJob(jd, numberOfProcess);
		
					// create now the local job execute dirs
					String inputsDir = jobs.getSubstitutedVariable("WPS_JOB_INPUTS_DIR");
					String outputsDir = jobs.getSubstitutedVariable("WPS_JOB_OUTPUTS_DIR");
					String auditsDir = jobs.getSubstitutedVariable("WPS_JOB_AUDITS_DIR");
					(new File(inputsDir)).mkdirs();
					(new File(outputsDir)).mkdirs();
					(new File(auditsDir)).mkdirs();
		
					// In the processing template, the input is passed through an agreed
					// standard: inputURLs.${GAI_TASK_ID}
					// we simulate the output from the previous processing in InputURLList.xml
					java.net.URL inputURLList = new java.net.URL("http://localhost/config/InputURLList.xml");
					URLConnection inputURLListuc = inputURLList.openConnection();
					DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
					DocumentBuilder db = dbf.newDocumentBuilder();
					
					Document inputXML = db.parse(inputURLListuc.getInputStream());	
					java.util.Map<String, org.w3c.dom.Document> inputXMLMap = new java.util.HashMap<String, org.w3c.dom.Document>();
					inputXMLMap.put("inputURLs", inputXML);
					jsa.writeComplexInputs(jobs, inputXMLMap);
					
				/*	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
					DocumentBuilder db = dbf.newDocumentBuilder();
					BufferedReader reader = new BufferedReader( new FileReader (WPSHome + "/execute/x.xml"));
				    String line  = null;
				    StringBuilder stringBuilder = new StringBuilder();
				    String ls = System.getProperty("line.separator");
				    while( ( line = reader.readLine() ) != null ) {
				        stringBuilder.append( line );
				        stringBuilder.append( ls );
				    }
					InputSource is = new InputSource(new StringReader(stringBuilder.toString()));
					Document inputXML = db.parse(is);	
					java.util.Map<String, org.w3c.dom.Document> inputXMLMap = new java.util.HashMap<String, org.w3c.dom.Document>();
					inputXMLMap.put("inputURLs", inputXML);
					jsa.writeComplexInputs(jobs, inputXMLMap);*/
					
					jobs.addCallback(Job.JOB_STATE, new testRunJob());
					jobs.addCallback(Job.JOB_STATEDETAIL, new testRunJob());
					// Let's GO!
					jobs.run();
					// wait for all jobs in the job array
					jobs.waitFor();
					
					System.out.println("Jobs completed!");
					
					if (jobs.getState() == State.FAILED) {
						displayMessages(jsa.readExitMessages(jobs));
						throw new Error("One of the jobs is failed");
					}
					if (jobs.getState() == State.DONE) {
						System.out.println("EXIT CODES: "+jobs.getAttribute(Job.EXITCODE));
						java.util.Map complexoutputXMLMap = jsa.readComplexOutputs(jobs,
								new String[] { "importedList" });
						Document complexoutputXML = (Document) complexoutputXMLMap.get("importedList");
						System.out.println("Here is the output URLs: " + jobs.getSubstitutedVariable("WPS_JOB_OUTPUTS_DIR") + "/outputURLs.xml");
						//System.out.println(complexoutputXML);
						createXML(complexoutputXML, jobs);
					    displayMessages(jsa.readExitMessages(jobs));
						System.out.println("END");
					}	
		    	}else{
		    		throw new FileNotFoundException ("JSDLTemplate.xml not found");
		    	}
		} catch (BadParameterException b) {
			b.printStackTrace();
			throw new BadParameterException(b.getMessage());
		} catch (Throwable e) {
			System.out.println("Got exception " + e);
				e.printStackTrace();
		}
	}

	private static void createXML(Document complexoutputXML, JobImpl jobs) 
		throws NoSuccessException{
		try {
	        // Prepare the DOM document for writing
	        Source source = new DOMSource(complexoutputXML);

	        // Prepare the output file
	        File file = new File(jobs.getSubstitutedVariable("WPS_JOB_OUTPUTS_DIR"));
	        Result result = new StreamResult(jobs.getSubstitutedVariable("WPS_JOB_OUTPUTS_DIR") + "/outputURLs.xml");

	        // Write the DOM document to the file
	        Transformer xformer = TransformerFactory.newInstance().newTransformer();
	        xformer.setOutputProperty(OutputKeys.INDENT, "yes");
	        xformer.transform(source, result);
	    } catch (TransformerConfigurationException e) {
	    	e.printStackTrace();
	    	throw new NoSuccessException("Error creating XML from complex output");
	    } catch (TransformerException e) {
	    	e.printStackTrace();
	    	throw new NoSuccessException("Error creating XML from complex output");
	    }
	}
	
	private static void displayMessages(String[][] mappa){
//		int fail = 0;
        System.out.println("--------------------");
        System.out.println("ExitCode - ExitText ");
        System.out.println("--------------------");
        for (int i = 0; i < mappa.length; i++ ){
        	System.out.println(mappa[i][0] + "         " +  mappa[i][1]);

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
/**
 * 	
 * @param wps_inputs
 * @param WPS_INPUT
 * @return a map filled by the content of the wps_inputs file
 * @throws IOException
 * @throws BadParameterException 
 */
	private static void importWps (String wps_inputs, java.util.Map<String, String> WPS_INPUT) 
					throws IOException, BadParameterException{
		String str = null;
		try { 
			BufferedReader reader = new BufferedReader( new FileReader (wps_inputs));
			while ((str = reader.readLine()) != null){
				String[] wps = str.split(" ");
				String value = "";
				String single_wps = null;
				for (int i = 1; i < wps.length; i++){
					single_wps = wps[i];
					value = value + single_wps + " ";
				}
				if (value.length() == 0)
					throw new BadParameterException ("Malformed wps_input file");
				String substr = value.substring(0, value.length() - 1);//eliminate the last space
				//System.err.println("deb " + wps[0] + " :: " + substr);
				WPS_INPUT.put(wps[0], substr);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new FileNotFoundException ("Cannot find wps_input file");
		} catch (IOException e) {
			e.printStackTrace();
			throw new IOException ("Exception reading wps_input file");
		}
	}
}
