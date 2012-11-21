/**
 * 
 */
package com.terradue.ogf.saga.impl.job;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.log4j.Logger;
import org.globus.myproxy.MyProxyException;
import org.ietf.jgss.GSSException;
import org.ogf.saga.SagaObject;
import org.ogf.saga.context.Context;
import org.ogf.saga.context.ContextFactory;
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
import org.ogf.saga.impl.url.URLImpl;
import org.ogf.saga.job.Job;
import org.ogf.saga.proxies.job.JobServiceWrapper;
import org.ogf.saga.session.Session;
import org.ogf.saga.url.URL;
import org.ogf.saga.job.JobService;
import com.terradue.ogf.saga.impl.resource.GridResource;
import com.terradue.ogf.schema.glue.GLUEDocument;
import com.terradue.ogf.schema.glue.GLUEException;
import com.terradue.ogf.schema.glue.GLUEFactory;
import com.terradue.ogf.schema.glue.impl.ComputingEndpointT;
import com.terradue.ogf.schema.glue.impl.ComputingManagerT;
import com.terradue.ogf.schema.glue.impl.ComputingServiceT;
import com.terradue.ogf.schema.glue.impl.ComputingShareT;
import com.terradue.ogf.schema.glue.impl.DomainsT;
import com.terradue.ogf.schema.glue.impl.ExecutionEnvironmentT;
import com.terradue.ogf.schema.glue.impl.ExtensionT;
import com.terradue.ogf.schema.glue.impl.ToStorageServiceT;
import com.terradue.ogf.schema.jsdl.JSDLDocument;
import com.terradue.ogf.schema.jsdl.JSDLFactory;
import com.terradue.ssegrid.sagaext.MissingConfigurationException;

/**
 * @author $Author: fbarchetta $
 * @version $Revision: 16399 $
 * @date $Date: 2011-10-21 15:24:23 +0200 (Fri, 21 Oct 2011) $
 * 
 */
public class JobServiceImpl extends JobServiceWrapper implements
		org.ogf.saga.job.JobService {
	// Logger
	Logger log;

	URL rm;

	// Customizable Substitution variables
	private java.util.Map<String, String> SubstitutionVars = new java.util.HashMap<String, String>();

	/**
	 * Constructor
	 * 
	 * @param session
	 * @param rm
	 * @throws NoSuccessException
	 * @throws TimeoutException
	 * @throws PermissionDeniedException
	 * @throws AuthorizationFailedException
	 * @throws AuthenticationFailedException
	 * @throws BadParameterException
	 * @throws IncorrectURLException
	 * @throws NotImplementedException
	 * @throws MyProxyException
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws GSSException
	 * @throws MissingConfigurationException
	 * @throws ResourceException
	 */
	public JobServiceImpl(Session session, URL rm) throws NoSuccessException,
			TimeoutException, PermissionDeniedException,
			AuthorizationFailedException, AuthenticationFailedException,
			BadParameterException, IncorrectURLException, NotImplementedException,
			MyProxyException, FileNotFoundException, IOException, GSSException,
			MissingConfigurationException, ResourceException {
		super(session, rm);
		if (session == null)
			throw new MissingConfigurationException("session is null");
		if (rm == null)
			throw new MissingConfigurationException("Resource Manager Url is null");

		// Instantiate a logger
		log = Logger.getLogger(this.getClass());

		// We keep the session
		this.sessionImpl = (SessionImpl) session;

		// Init the computing services with the resource manager
		this.rm = rm;

	}

	private void addEnvironmentVariables(Map<String, String> vars,
			JobDescription jd) throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, IncorrectStateException,
			BadParameterException, DoesNotExistException, TimeoutException,
			NoSuccessException {
		String[] envVars = jd.getVectorAttribute(JobDescription.ENVIRONMENT);
		String[] newEnvVars = new String[envVars.length + vars.size()];
		for (int s = 0; s < envVars.length; s++) {
			newEnvVars[s] = envVars[s];
		}
		int i = envVars.length;
		for (Iterator<String> it = vars.keySet().iterator(); it.hasNext();) {
			String key = it.next();
			String value = vars.get(key);
			newEnvVars[i] = key + "=" + value;
			i++;
		}
		jd.setVectorAttribute(JobDescription.ENVIRONMENT, newEnvVars);
	}

	/**
	 * 
	 * @param wpsVariables
	 */
	public void addSubstitutionVariables(
			java.util.Map<java.lang.String, java.lang.String> wpsVariables) {
		this.SubstitutionVars.putAll(wpsVariables);
	}

	private Job createGenericJob(JobService js, Map<String, String> GAISubVar,
			JobDescription jd) throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, BadParameterException, TimeoutException,
			NoSuccessException {

		JobDescription jdt;
		Job job;

		try {
			jdt = (JobDescription) jd.clone();
		} catch (CloneNotSupportedException e1) {
			throw new NoSuccessException("Impposible to clone JobDescription");
		}

		try {
			this.addEnvironmentVariables(GAISubVar, jdt);
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		try {
			this.substituteVariables(GAISubVar, jdt);
		} catch (BadParameterException b){
						
		} catch (DoesNotExistException e) {
			throw new NoSuccessException("Variables cannot be substituted:"
					+ e.getMessage());
		}

		log.debug("<<< substituted JobDescription content >>>");
		this.debugJobDescription((JobDescriptionImpl) jdt);

		job = js.createJob((org.ogf.saga.job.JobDescription) jdt);

		return job;
	}

	private JobService createJobService(GridResource gr)
			throws NotImplementedException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			IncorrectStateException, DoesNotExistException, TimeoutException,
			NoSuccessException, BadParameterException {

		JobService js;

		Session session = null;
		try {
			session = (Session) this.sessionImpl.clone();
		} catch (CloneNotSupportedException e1) {
			throw new NoSuccessException("Invalid session: " + e1.getMessage());
		}
		Context context = null, contextg = null;

		// get default context if exists
		Context contextt[] = session.listContexts();
		for (int i = 0; i < contextt.length; i++) {
			String type = contextt[i].getAttribute(Context.TYPE);
			if (type != null && type == "preferences") {
				context = contextt[i];
			}
			if (type != null && type == "globus") {
				contextg = contextt[i];
			}
		}
		if (context == null) {
			context = ContextFactory.createContext("preferences");
		}

		if (gr.computingEndpoint.getJobDescription().get(0).equals("globus:rsl")) {
			context.setAttribute("ResourceBroker.adaptor.name", "wsgt4new");
			context.setAttribute("File.adaptor.name", "Local,GridFTP");
			if (contextg != null)
				this.setX509proxy(contextg.getAttribute(Context.USERPROXY));
		} else {
			throw new NotImplementedException("Computing Endpoint "
					+ gr.computingEndpoint.getJobDescription().get(0)
					+ " is not implemented.");
		}

		if (gr.computingManager.getOtherInfo().equals("PBS")) {
			context.setAttribute("wsgt4new.factory.type", "PBS");
		} else {
			throw new NotImplementedException("Computing Manager "
					+ gr.computingManager.getOtherInfo() + " is not implemented.");
		}

		session.addContext(context);

		// create the JobService from the modified session and the Resource URL
		// extracted from computing service
		try {
			js = org.ogf.saga.job.JobFactory.createJobService(session,
					this.getURL(gr));
		} catch (IncorrectURLException e) {
			throw new BadParameterException("Incorrect URL for Computing EndPoint "
					+ gr.computingEndpoint.getID() + ": " + e.getMessage());
		}

		return js;
	}

	public JobImpl createJob(JobDescription jd) throws NotImplementedException,
	AuthenticationFailedException, AuthorizationFailedException,
	PermissionDeniedException, BadParameterException, TimeoutException,
	NoSuccessException {
		return createJob(jd, 0);
	}
	
	public JobImpl createJob(JobDescription jd, int numberOfProcesses) throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, BadParameterException, TimeoutException,
			NoSuccessException {

		// JobArray
		int numberOfJobs = numberOfProcesses;
		Job prepJob = null;
		Job[] ja = null;
		Job postJob = null;
		JobImpl jai = null;

		// Temporary JobDescription
		JobDescription jdt;

		// Grid Resource
		GridResource gr = null;

		// JobService
		JobService js = null;

		// Grid Access Infrastructure substitution variables
		HashMap<String, String> GAISubstitutionVars = new HashMap<String, String>();

		// Test if valid JobDescription
		if (jd == null)
			throw new BadParameterException(
					"Error when creating JobArray: JobDescription is null");

		// clone the JobDescription to work on a new one
		try {
			jdt = (JobDescription) jd.clone();
		} catch (CloneNotSupportedException e) {
			throw new NoSuccessException("JobDescription cannot be cloned:"
					+ e.getMessage());
		}

		log.debug("<<< original JobDescription content >>>");
		this.debugJobDescription((JobDescriptionImpl) jdt);

		// Substitute custom variables
		try {
			this.substituteVariables(this.SubstitutionVars, jdt);
		} catch (BadParameterException b){
			
		} catch (Exception e) {
			throw new NoSuccessException("Variables cannot be substituted:"
					+ e.getMessage());
		}

		// search for the right ComputingService
		try {
			gr = getGridResourceFromJobDescription(jdt);
		} catch (ResourceException e) {
			throw new NoSuccessException("Computing Service matching failed : "
					+ e.getMessage());
		}

		if (gr != null) {

			// Now we have the right computing element, we need to extract
			// new information about storage to substitute other variables
			GAISubstitutionVars.putAll(getGAISubstitutionVars(gr));

			try {
				js = this.createJobService(gr);
			} catch (Exception e1) {
				throw new NoSuccessException(
						"Creation of JobService for JobDescription failed: "
								+ e1.getMessage());
			}

			// extract the mapping Queue from the Resource Manager to fill the queue
			// of the DRM to submit to
			String mappingQueue = gr.computingShare.getMappingQueue();
			try {
				jd.setAttribute(JobDescription.QUEUE, mappingQueue);
			} catch (Exception e) {
				throw new NoSuccessException(
						"Impossible to setup the queue for the job: " + e.getMessage());
			}

			// retrieve the number of jobs from the attribute
			if(numberOfJobs == 0){
				try {
					numberOfJobs = Integer.parseInt(jd
							.getAttribute(JobDescription.NUMBEROFPROCESSES));
				} catch (Exception e) {
					throw new NoSuccessException(
							"Error when retrieving the NUMBEROFPROCESSES attribute from JobDescription >"
									+ e.getMessage());
				}
			}
			ja = new Job[numberOfJobs];
			// Creating a job UID for all jobs
			UUID JobUUID = UUID.randomUUID();
			String JobUID = JobUUID.toString();
			// Set the JobDescription as per Job
			// ja.setJobDescription((org.ogf.saga.job.JobDescription) jdt);

			GAISubstitutionVars.put("GAI_JOB_UID", JobUID);
			ExecutionEnvironmentT ee = gr.computingManager.getExecutionEnvironment()
					.get(0);
			if (ee == null)
				throw new NoSuccessException(
						"Error: ExecutionEnvironment is null for ComputingManager "
								+ gr.computingManager.getName());
			GAISubstitutionVars.put("GAI_TOTAL_TASKS", numberOfJobs + "");
			GAISubstitutionVars.put("GAI_ARCH", ee.getPlatform());
			GAISubstitutionVars.put("GAI_JOB_TEMP_DIR",
					gr.computingManager.getTmpDir());
			GAISubstitutionVars.put("GAI_JOB_WORKING_DIR",
					GAISubstitutionVars.get("GAI_ROOT_WORKING_DIR") + "/" + JobUID);
			GAISubstitutionVars.put("GAI_JOB_RESULTS_DIR",
					GAISubstitutionVars.get("GAI_ROOT_RESULTS_DIR") + "/" + JobUID);
			GAISubstitutionVars.put("GAI_CE_HOSTNAME", "CE");
			GAISubstitutionVars.put("GAI_CLIENT_HOME",
					System.getProperty("saga.location"));
			String gayDebugWorking = System.getProperty("gai.debug.working.dir");
			if (gayDebugWorking != null)
				GAISubstitutionVars.put("GAI_DEBUG_WORKING_DIR", gayDebugWorking);
			// Get common staging transfers
			try {
				String[] fileTxs = jdt.getVectorAttribute(JobDescription.FILETRANSFER);
				ArrayList<String> newFileTxs = new ArrayList<String>();
				ArrayList<String> prepFileTxs = new ArrayList<String>();
				ArrayList<String> postFileTxs = new ArrayList<String>();
				boolean isPosix = false;
				if (numberOfJobs == 1) {
					log.debug("Application is POSIX");
					isPosix = true;
				}
				log.debug("Checking WorkingDirectory");
				String workingDirectory = jdt.getAttribute(JobDescription.WORKINGDIRECTORY);
				if (workingDirectory == null || workingDirectory.equals(".")){
					jdt.setAttribute(JobDescription.WORKINGDIRECTORY, "${GAI_JOB_WORKING_DIR}");
				}
				String temp = jdt.getAttribute(JobDescription.EXECUTABLE);
				if ( ( ! temp.equals("")) && ! new File(temp).isAbsolute() && ! temp.contains("GAI_JOB_WORKING_DIR")) {
					jdt.setAttribute(JobDescription.EXECUTABLE, "${GAI_JOB_WORKING_DIR}/" + temp);
				}
				temp = jdt.getAttribute(JobDescription.INPUT);
				if ( ( ! temp.equals("")) && ! new File(temp).isAbsolute() && ! temp.contains("GAI_JOB_WORKING_DIR")) {
					jdt.setAttribute(JobDescription.INPUT, "${GAI_JOB_WORKING_DIR}/" + temp);
				}
				temp = jdt.getAttribute(JobDescription.OUTPUT);
				if ( ( ! temp.equals("")) && ! new File(temp).isAbsolute() && ! temp.contains("GAI_JOB_WORKING_DIR")) {
					jdt.setAttribute(JobDescription.OUTPUT, "${GAI_JOB_WORKING_DIR}/" + temp);
				}
				temp = jdt.getAttribute(JobDescription.ERROR);
				if ( ( ! temp.equals("")) && ! new File(temp).isAbsolute() && ! temp.contains("GAI_JOB_WORKING_DIR")) {
					jdt.setAttribute(JobDescription.ERROR, "${GAI_JOB_WORKING_DIR}/" + temp);
				}
				for (String s : fileTxs) {
					String[] sVett = s.split("> ");
					if (sVett.length > 1 && ! new File(temp).isAbsolute() && ! sVett[1].contains("GAI_JOB_WORKING_DIR")){
						s = sVett[0] + ">" + " ${GAI_JOB_WORKING_DIR}/" + sVett[1];
					}
					
					sVett = s.split("< ");
					if (sVett.length > 1 && ! new File(temp).isAbsolute() && ! sVett[1].contains("GAI_JOB_WORKING_DIR")){
						s = sVett[0] + "<" + " ${GAI_JOB_WORKING_DIR}/" + sVett[1];
					}
					
					if ((isPosix && this.isIOErr(jdt, s)) || s.contains("${GAI_TASK_ID}")) {
						newFileTxs.add(s);
					} else {
						// System.err.println("passed " + s);
						if (s.contains(">")) {
							log.debug("Transfer " + s + "passed in preparation");
							prepFileTxs.add(s);
						} else {
							postFileTxs.add(s);
						}
					}
				}

				// put back the transfers to the jobDescription
				String nfts[] = (String[]) newFileTxs.toArray(new String[newFileTxs
						.size()]);
				jdt.setVectorAttribute(JobDescription.FILETRANSFER, nfts);
				jdt.setAttribute(JobDescription.NUMBEROFPROCESSES, "1");

				// create prep and post job
				prepJob = this.createPreparationJob(js, GAISubstitutionVars,
						(String[]) prepFileTxs.toArray(new String[prepFileTxs.size()]));
				postJob = this.createPostJob(js, GAISubstitutionVars,
						postFileTxs.toArray(new String[postFileTxs.size()]));

			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			// Let's create each job
			for (int i = 1; i <= numberOfJobs; i++) {

				// The Job
				Job job = null;

				GAISubstitutionVars.put("GAI_TASK_ID", i + "");

				job = this.createGenericJob(js, GAISubstitutionVars, jdt);

				if (job == null)
					throw new NoSuccessException(
							"Cannot create the job from the Job Description provided");

				log.debug("<<< Job content for task " + i + " >>>");
				this.debugJob(job);

				ja[i - 1] = job;
			}
			jai = new JobImpl(JobUID, prepJob, ja, postJob, this.sessionImpl);
			jai.setJobDescription((org.ogf.saga.job.JobDescription)jd);
			jai.setRm(this.getURL(gr));
			// substitute each variables
			for (Iterator<String> it = SubstitutionVars.keySet().iterator(); it
					.hasNext();) {
				String key = it.next();
				String value = SubstitutionVars.get(key);
				try {
					value = this.substituteVariables(SubstitutionVars, value);
					value = this.substituteVariables(GAISubstitutionVars, value);
				} catch (DoesNotExistException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				jai.addSubstitutedVariable(key, value);
			}

		} else {
			throw new NoSuccessException(
					"No compliant Computing Service found in the Resource Manager");
		}

		return jai;
	}

	private boolean isIOErr(JobDescription jd, String fileName)
			throws NotImplementedException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			IncorrectStateException, DoesNotExistException, TimeoutException,
			NoSuccessException 
	{
		if ( ! jd.getAttribute(JobDescription.INPUT).isEmpty()
				&& fileName.contains(jd.getAttribute(JobDescription.INPUT)))
			return true;
		if ( ! jd.getAttribute(JobDescription.OUTPUT).isEmpty()
				&& fileName.contains(jd.getAttribute(JobDescription.OUTPUT)))
			return true;
		if ( ! jd.getAttribute(JobDescription.ERROR).isEmpty()
				&& fileName.contains(jd.getAttribute(JobDescription.ERROR)))
			return true;
		return false;
	}

	private Job createPostJob(JobService js, Map<String, String> GAISubVar,
			String[] fileTxs) throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, BadParameterException, TimeoutException,
			NoSuccessException {

		String jobPreparationJSDL = System.getProperty("saga.location")
				+ File.separatorChar + "etc/postProc/postProc_JSDLTemplate.xml";
		JobDescription jdt;
		Job job;

		try {
			JSDLDocument JSDLDocument = JSDLFactory.createJSDLDocument(new File(
					jobPreparationJSDL));
			jdt = (JobDescription) JobFactory.createJobDescription(JSDLDocument);
			String[] oft = jdt.getVectorAttribute(JobDescription.FILETRANSFER);
			/* String[] ft = Arrays.copyOf(oft, oft.length + fileTxs.length); */
			String[] ft = new String[oft.length + fileTxs.length];
			for (int j = 0; j < oft.length; j++) {
				ft[j] = oft[j];
			}
			System.arraycopy(fileTxs, 0, ft, oft.length, fileTxs.length);

			jdt.setVectorAttribute(JobDescription.FILETRANSFER, ft);
			jdt.setAttribute(JobDescription.CLEANUP, "False");
			this.addEnvironmentVariables(GAISubVar, jdt);

		} catch (Exception e1) {
			throw new NoSuccessException("Cannot create preparation job!", e1);
		}

		try {
			this.substituteVariables(GAISubVar, jdt);
		} catch (BadParameterException b){
						
		} catch (DoesNotExistException e) {
			throw new NoSuccessException("Variables cannot be substituted:"
					+ e.getMessage());
		}

		log.debug("<<< substituted prep JobDescription content >>>");
		this.debugJobDescription((JobDescriptionImpl) jdt);

		job = js.createJob((org.ogf.saga.job.JobDescription) jdt);

		return job;
	}

	private Job createPreparationJob(JobService js,
			Map<String, String> GAISubVar, String[] fileTxs)
			throws NotImplementedException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			BadParameterException, TimeoutException, NoSuccessException {

		String jobPreparationJSDL = System.getProperty("saga.location")
				+ File.separatorChar + "etc/prepProc/preparationProc_JSDLTemplate.xml";
		JobDescription jdt;
		Job job;

		try {
			JSDLDocument JSDLDocument = JSDLFactory.createJSDLDocument(new File(
					jobPreparationJSDL));
			jdt = (JobDescription) JobFactory.createJobDescription(JSDLDocument);
			String[] oft = jdt.getVectorAttribute(JobDescription.FILETRANSFER);
			// String[] ft = Arrays.copyOf(oft, oft.length + fileTxs.length);
			String[] ft = new String[oft.length + fileTxs.length];
			for (int j = 0; j < oft.length; j++) {
				ft[j] = oft[j];
			}
			System.arraycopy(fileTxs, 0, ft, oft.length, fileTxs.length);

			jdt.setVectorAttribute(JobDescription.FILETRANSFER, ft);

			try {
				jdt.setAttribute(JobDescription.QUEUE, "short");
			} catch (Exception e) {
				throw new NoSuccessException(
						"Impossible to setup the queue for the prep job: " + e.getMessage());
			}

			this.addEnvironmentVariables(GAISubVar, jdt);

		} catch (Exception e1) {
			throw new NoSuccessException("Cannot create preparation job!", e1);
		}

		try {
			this.substituteVariables(GAISubVar, jdt);
		} catch (BadParameterException b){
			
		} catch (DoesNotExistException e) {
			throw new NoSuccessException("Variables cannot be substituted:"
					+ e.getMessage());
		}

		log.debug("<<< substituted prep JobDescription content >>>");
		this.debugJobDescription((JobDescriptionImpl) jdt);

		job = js.createJob((org.ogf.saga.job.JobDescription) jdt);

		return job;
	}

	private void debugJob(Job job) {
		String[] attrs;
		try {
			attrs = job.listAttributes();
			for (int i = 0; i < attrs.length; i++) {
				log.debug("S   " + attrs[i] + " = " + job.getAttribute(attrs[i]));
			}
		} catch (Exception e) {
		}

	}

	private void debugJobDescription(JobDescriptionImpl jd) {
		String[] attrs;
		try {
			attrs = jd.listAttributes();
			for (int i = 0; i < attrs.length; i++) {
				if (jd.isVectorAttribute(attrs[i])) {
					for (int j = 0; j < jd.getVectorAttribute(attrs[i]).length; j++) {
						log.debug("VS  " + attrs[i] + " = "
								+ jd.getVectorAttribute(attrs[i])[j]);
					}
				} else {
					log.debug("S   " + attrs[i] + " = " + jd.getAttribute(attrs[i]));
				}
			}
		} catch (Exception e) {
		}

	}

	/**
	 * 
	 * @param gr
	 * @return
	 */
	private Map<String, String> getGAISubstitutionVars(GridResource gr) {

		// prepare the Map
		HashMap<String, String> GAISubstitutionVars = new HashMap<String, String>();

		// Get the storage services available on the computing service
		List<ToStorageServiceT> tssl = gr.computingService.getToStorageService();

		// Search for storage service with an extension with a key starting with
		// "GAI"
		for (int i = 0; i < tssl.size(); i++) {
			ToStorageServiceT tss = tssl.get(i);
			List<ExtensionT> extl = tss.getExtensions().getExtension();
			for (int j = 0; j < extl.size(); j++) {
				ExtensionT ext = extl.get(j);
				if (ext.getKey().startsWith("GAIVar")) {
					log.debug("adding the GAI substirution variable "
							+ extl.get(j).getValue() + " > " + tss.getLocalPath());
					GAISubstitutionVars.put(extl.get(j).getValue(), tss.getLocalPath());
				}
			}
		}

		return GAISubstitutionVars;
	}

	@SuppressWarnings("unchecked")
	private GridResource getGridResourceFromJobDescription(JobDescription jd)
			throws ResourceException {

		GridResource gr = new GridResource();

		GLUEDocument glueDocument = null;

		try {
			// creates a glueDocument from the URL of the Resource Manager
			log.debug("Reading GLUE Document at " + rm.getString());
			glueDocument = GLUEFactory.createGLUEDocument(rm);
		} catch (GLUEException e) {
			throw new ResourceException("Problem reading GLUE document: "
					+ e.getMessage());
		}
		// check new document
		if (glueDocument == null)
			throw new ResourceException("GlUE configuration is null");

		JXPathContext jc = JXPathContext.newContext((DomainsT) glueDocument
				.getDomains());

		// Build the xpath query based on jobdescription resources config
		String XPathCondition = "true()";
		String Endpoint = "";
		String JobManager = "";
		String Queue = "";
		String CPUArch = "";
		String OS = "";

		try {
			if (jd.getAttribute(JobDescription.ENDPOINT) != "") {
				XPathCondition += " and computingEndpoint/jobDescription = '"
						+ jd.getAttribute(JobDescription.ENDPOINT) + "'";
				Endpoint = jd.getAttribute(JobDescription.ENDPOINT);
			}
			if (jd.getAttribute(JobDescription.JOBMANAGER) != "") {
				XPathCondition += " and computingManager/otherInfo = '"
						+ jd.getAttribute(JobDescription.JOBMANAGER) + "'";
				JobManager = jd.getAttribute(JobDescription.JOBMANAGER);
			}
			if (jd.getAttribute(JobDescription.QUEUE) != "") {
				XPathCondition += " and computingShares/computingShare/mappingQueue = '"
						+ jd.getAttribute(JobDescription.QUEUE) + "'";
				Queue = jd.getAttribute(JobDescription.QUEUE);
			}
			if (jd.getAttribute(JobDescription.CPUARCHITECTURE) != "") {
				XPathCondition += " and computingManager/executionEnvironment/platform = '"
						+ jd.getAttribute(JobDescription.CPUARCHITECTURE) + "'";
				CPUArch = jd.getAttribute(JobDescription.CPUARCHITECTURE);
			}
			if (jd.getAttribute(JobDescription.OPERATINGSYSTEMTYPE) != "") {
				XPathCondition += " and computingManager/executionEnvironment/oSFamily = '"
						+ jd.getAttribute(JobDescription.OPERATINGSYSTEMTYPE) + "'";
				OS = jd.getAttribute(JobDescription.OPERATINGSYSTEMTYPE);
			}
		} catch (Exception e) {
		}

		log.debug("looking for " + XPathCondition);

		Iterator<ComputingServiceT> csit = jc
				.iterate("adminDomain/services/computingService[ " + XPathCondition
						+ " ]");
		if (csit.hasNext() == false) {
			throw new ResourceException(
					"No Computing Service compliant to the JobDescription has been found.");
		}
		// for each computing service found
		while (csit.hasNext()) {
			gr.computingService = csit.next();
			log.debug("Selected Computing Service :" + gr.computingService.getID());

			jc = JXPathContext.newContext(gr.computingService);

			// Get Endpoint
			if (Endpoint != "") {
				XPathCondition = "jobDescription = '" + Endpoint + "'";
			} else {
				XPathCondition = "true()";
			}
			Iterator<ComputingEndpointT> ceit = jc.iterate("computingEndpoint[ "
					+ XPathCondition + " ]");
			while (ceit.hasNext()) {
				gr.computingEndpoint = ceit.next();
				log.debug("Selected Computing EnPoint :" + gr.computingEndpoint.getID());
				break;
			}

			// Get JobManager
			if (JobManager != "") {
				XPathCondition = "otherInfo = '" + JobManager + "'";
			} else {
				XPathCondition = "true()";
			}
			if (CPUArch != "") {
				XPathCondition = " and executionEnvironment/platform = '" + CPUArch
						+ "'";
			}
			if (OS != "") {
				XPathCondition = " and executionEnvironment/oSFamily = '" + OS + "'";
			}
			Iterator<ComputingManagerT> cmit = jc.iterate("computingManager[ "
					+ XPathCondition + " ]");
			while (cmit.hasNext()) {
				gr.computingManager = cmit.next();
				log.debug("Selected Computing Manager :" + gr.computingManager.getID());
				break;
			}

			// Get Queue
			if (Queue != "") {
				XPathCondition = "mappingQueue = '" + Queue + "'";
			} else {
				XPathCondition = "true()";
			}
			Iterator<ComputingShareT> cshit = jc
					.iterate("computingShares/computingShare[ " + XPathCondition + " ]");
			while (cshit.hasNext()) {
				gr.computingShare = cshit.next();
				log.debug("Selected Computing Share :" + gr.computingShare.getLocalID());
				break;
			}

			break;

		}

		return gr;
	}

	/**
	 * Creates n jobs of the the JobDescription jd and return a new instance of
	 * JobArray
	 * 
	 * @param jd
	 *          is the JobDescription from which the job must be created
	 * @param n
	 *          is the number of instance of job to be created
	 * @return JobArray object that manages all the instances of the jobs.
	 */
	public Session getSession() {
		return this.sessionImpl;
	}

	public java.util.Map<String, String> getSubstitutionVars() {
		return this.SubstitutionVars;
	}

	/**
	 * Extract the Resource URL from the chosen ComputingService (just the first
	 * ComputingEndpoint, until the port number)
	 * 
	 * @param computingService
	 * @return the URL of the Computing Endpoint, until the 8443 port
	 * @throws NoSuccessException
	 * @throws BadParameterException
	 */
	private URL getURL(GridResource gr) throws BadParameterException,
			NoSuccessException {
		URL URLrm = null;
		if (gr != null) {
			URLrm = new URLImpl(gr.computingEndpoint.getURL());
		} else {
			throw new NoSuccessException("The resource is null");
		}

		return URLrm;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void setX509proxy(String proxy) {
		Class[] classes = Collections.class.getDeclaredClasses();
		Map<String, String> env = System.getenv();
		for (Class cl : classes) {
			if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
				try {
					Field field = cl.getDeclaredField("m");
					field.setAccessible(true);
					Object obj;
					obj = field.get(env);
					Map<String, String> map = (Map<String, String>) obj;
					map.clear();
					HashMap<String, String> hm = new HashMap<String, String>();
					hm.put("X509_USER_PROXY", proxy);
					map.putAll(hm);
				} catch (Exception e) {
					log.debug(e.getMessage());
				}

			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void substituteVariables(Map<String, String> svs, JobDescription jd)
			throws NotImplementedException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			TimeoutException, NoSuccessException, BadParameterException,
			DoesNotExistException {

		try {
			// retrieve the attributes of the jobDescriptor
			String[] attributes = jd.listAttributes();			
			// for every attribute
			Map svs2 = new HashMap (svs);
			for (int i = 0; i < attributes.length; i++) {
				String jdkey = attributes[i];
				// for every substitution variable in this JobService
				Iterator<String> it = svs.keySet().iterator();
	
				for (it = svs.keySet().iterator(); it.hasNext();) { 
					// extracts the substitution variable map key
					String WPSkey = it.next(), val = null;
					if ( ! jd.isVectorAttribute(jdkey)) {
						String exVal = "${" + WPSkey + "}";
						// replace the value eventually contained in the substitution
						// variable
						String prev = jd.getAttribute(jdkey);
						val = jd.getAttribute(jdkey).replace(exVal,	this.substituteVariables(svs, svs.get(WPSkey)));
					//	if (val.equals(prev))
						//	System.out.println(jdkey + " prova " + val + " " + prev + " " + exVal);
						if ( ! val.equals("")) {// this should prevent the exception
							try {
								// setting substituted attribute
								jd.setAttribute(jdkey, val);
							} catch (BadParameterException e) {
								log.info("cannot substitute " + jdkey
										+ " because is not a String (val: " + val + ")");
							}
						}
					} else {
						String vals[] = jd.getVectorAttribute(attributes[i]);
						String[] newVals = new String[vals.length];
						boolean found = false;
						for (int j = 0; j < vals.length; j++) {
							String svsval = svs.get(WPSkey);
							String svssubval = this.substituteVariables(svs, svsval);
							if (vals[j].contains("${" + WPSkey + "}")){								
								found = true;
							}
							val = vals[j].replace("${" + WPSkey + "}", svssubval);
							newVals[j] = val;
						}
						jd.setVectorAttribute(attributes[i], newVals);
						if (found)
							svs2.remove(WPSkey);
					}
				}
			}
			if(svs2.size() > 0){
				String myError = "";
				myError = "These variables are not found in JSDLTemplate! \n";
				for (int i = 0; i < svs2.size(); i++){
					myError = myError + "   - " + svs2.keySet().toArray()[i] + "\n"; 
				}
				throw new BadParameterException(myError);
			}
		} catch (IncorrectStateException e) {
			e.printStackTrace();
			throw new NoSuccessException("Cannot substitute variables: "
					+ e.getMessage());
		}/* catch (Exception e ){
			e.printStackTrace();
			throw new NoSuccessException("Cannot substitute variables: "
					+ e.getMessage());
		}*/
	}

	public String substituteVariables(Map<String, String> svs, String vtbs)
			throws NotImplementedException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			TimeoutException, NoSuccessException, BadParameterException,
			DoesNotExistException {

		String val = vtbs;

		Iterator<String> it = svs.keySet().iterator();
		for (it = svs.keySet().iterator(); it.hasNext();) {
			String WPSkey = it.next();
			String exVal = "${" + WPSkey + "}";
			val = val.replace(exVal, svs.get(WPSkey));
		}

		return val;
	}
}