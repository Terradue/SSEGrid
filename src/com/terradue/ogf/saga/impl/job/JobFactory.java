/**
 * 
 */
package com.terradue.ogf.saga.impl.job;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.apache.log4j.Logger;
import org.globus.myproxy.MyProxyException;
import org.ietf.jgss.GSSException;
import org.ogf.saga.bootstrap.ImplementationBootstrapLoader;
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
import org.ogf.saga.job.JobService;
import org.ogf.saga.session.Session;
import org.ogf.saga.session.SessionFactory;
import org.ogf.saga.task.TaskMode;
import org.ogf.saga.task.Task;
import org.ogf.saga.url.URL;
import org.ogf.saga.url.URLFactory;

import com.terradue.ogf.schema.glue.GLUEDocument;
import com.terradue.ogf.schema.glue.GLUEException;
import com.terradue.ogf.schema.glue.GLUEFactory;
import com.terradue.ogf.schema.glue.impl.AdminDomainT;
import com.terradue.ogf.schema.glue.impl.ComputingServiceT;
import com.terradue.ogf.schema.glue.impl.DomainsT;
import com.terradue.ogf.schema.jsdl.JSDLDocument;
import com.terradue.ogf.schema.jsdl.JSDLFactory;
import com.terradue.ogf.schema.jsdl.impl.ApplicationType;
import com.terradue.ogf.schema.jsdl.impl.ArgumentType;
import com.terradue.ogf.schema.jsdl.impl.CPUArchitectureType;
import com.terradue.ogf.schema.jsdl.impl.CreationFlagEnumeration;
import com.terradue.ogf.schema.jsdl.impl.EnvironmentType;
import com.terradue.ogf.schema.jsdl.impl.NumberOfProcessesType;
import com.terradue.ogf.schema.jsdl.impl.OperatingSystemType;
import com.terradue.ogf.schema.jsdl.impl.POSIXApplicationType;
import com.terradue.ogf.schema.jsdl.impl.RangeValueType;
import com.terradue.ogf.schema.jsdl.impl.JobDefinitionType;
import com.terradue.ogf.schema.jsdl.impl.DataStagingType;
import com.terradue.ogf.schema.jsdl.impl.SPMDApplicationType;
import com.terradue.ogf.schema.jsdl.impl.SourceTargetType;
import com.terradue.ogf.schema.jsdl.impl.CandidateHostsType;
import com.terradue.ogf.schema.jsdl.impl.ResourcesType;
import com.terradue.ssegrid.sagaext.MissingConfigurationException;

/**
 * Factory for objects from the job package. This class wraps the original
 * implementation of <a href=
 * 'http://static.saga.cct.lsu.edu/apidoc/java/org/ogf/saga/job/JobFactory.html'
 * > JobFactory</a > and implements some extensions transparently with JavaSAGA
 * API. In fact, the JavaSAGA original implementation is still used and simply
 * called implementation. When the documentation refers to extension, it means
 * the additional implementation of the extension. For instance,
 * {@link #createJobService()} enables the creation of the extension
 * {@link JobServiceImpl} with the information on the resources.
 * 
 * 
 * @author emathot
 */
public abstract class JobFactory extends org.ogf.saga.job.JobFactory {

	private static org.ogf.saga.job.JobFactory factory;
	private static Logger log = Logger.getLogger(JobFactory.class);

	private static synchronized void initializeFactory()
			throws NotImplementedException, NoSuccessException {
		if (factory == null) {
			factory = ImplementationBootstrapLoader.getJobFactory(null);
		}

	}

	/**
	 * Creates a job description. To be provided by the implementation.
	 * 
	 * @return the job description.
	 */
	protected abstract org.ogf.saga.job.JobDescription doCreateJobDescription()
			throws NotImplementedException, NoSuccessException;

	/**
	 * Creates a job service. To be provided by the implementation.
	 * 
	 * @param session
	 *          the session handle.
	 * @param rm
	 *          contact string for the resource manager.
	 * @return the job service.
	 */
	protected abstract JobService doCreateJobService(Session session, URL rm)
			throws NotImplementedException, BadParameterException,
			IncorrectURLException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			TimeoutException, NoSuccessException;

	/**
	 * Creates an empty job description.
	 * 
	 * @return the job description.
	 * @exception NotImplementedException
	 *              is thrown when this method is not implemented.
	 * @throws NoSuccessException
	 *           is thrown when the Saga factory could not be created.
	 */
	public static org.ogf.saga.job.JobDescription createJobDescription()
			throws NotImplementedException, NoSuccessException {
		initializeFactory();
		return /**/null;/* factory.doCreateJobDescription(); */
	}

	/**
	 * Creates a job description from a JSDLDocument processing template. The
	 * mapping is done according to the SSEGrid-T2-D2420-3 Grid Infrastructure ICD
	 * - section 8.1.4
	 * 
	 * @return the job description filled with element of JSDLDocument.
	 * @throws BadParameterException
	 *           is thrown if the JSDL Document is null.
	 * @throws NoSuccessException
	 *           is thrown when the Saga factory could not be created.
	 * @throws NotImplementedException
	 *           - is thrown if the implementation does not provide an
	 *           implementation of this method.
	 * @throws AuthenticationFailedException
	 *           - is thrown when operation failed because none of the available
	 *           session contexts could successfully be used for authentication.
	 * @throws AuthorizationFailedException
	 *           - is thrown when none of the available contexts of the used
	 *           session could be used for successful authorization. This error
	 *           indicates that the resource could not be accessed at all, and not
	 *           that an operation was not available due to restricted
	 *           permissions.
	 * @throws PermissionDeniedException
	 *           - is thrown when the method failed because the identity used did
	 *           not have sufficient permissions to perform the operation
	 *           successfully.
	 * @throws IncorrectStateException
	 *           - is thrown when the attribute is not a vector attribute.
	 * @throws BadParameterException
	 *           - is thrown when the specified value does not conform to the
	 *           attribute type. In particular, this exception is thrown when the
	 *           specified value is null. An array with no elements is allowed,
	 *           though.
	 * @throws DoesNotExistException
	 *           - is thrown when the attribute does not exist.
	 * @throws TimeoutException
	 *           - is thrown when a remote operation did not complete successfully
	 *           because the network communication or the remote service timed
	 *           out.
	 * @throws NoSuccessException
	 *           - is thrown when the Saga factory could not be created or the
	 *           operation was not successfully performed, and none of the other
	 *           exceptions apply.
	 * */
	public static org.ogf.saga.job.JobDescription createJobDescription(
			JSDLDocument jsdl) throws BadParameterException, NoSuccessException,
			NotImplementedException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			IncorrectStateException, DoesNotExistException, TimeoutException {
		if (jsdl == null)
			throw new BadParameterException("JSDL Document is null");
		try {
			initializeFactory();
		} catch (NotImplementedException e) {
			e.printStackTrace();
			throw new NoSuccessException("Error initializing Factory");
		}

		JobDescriptionImpl jd = new JobDescriptionImpl();

		// JobDescription jd = factory.doCreateJobDescription();
		// jd = new JobDescriptionImpl();
		/*
		 * extracts the elements from the JSDLDocument and fills them into the
		 * JobDescription
		 */
		String attribute = null;
		String jobName = ((JobDefinitionType) jsdl.getJobDefinition())
				.getJobDescription().getJobIdentification().getJobName();
		try {
			attribute = com.terradue.ogf.saga.impl.job.JobDescription.JOBNAME;
			jd.setAttribute(attribute, jobName);
			log.info("Attribute added: " + attribute + " = "
					+ jd.getAttribute(attribute));
			attribute = com.terradue.ogf.saga.impl.job.JobDescription.JOBDESCRIPTION;
			String jobDescription = ((JobDefinitionType) jsdl.getJobDefinition())
					.getJobDescription().getJobIdentification().getDescription();
			jd.setAttribute(attribute, jobDescription);
			log.info("Attribute added: " + attribute + " = "
					+ jd.getAttribute(attribute));
			ApplicationType application;
			if ((application = ((JobDefinitionType) jsdl.getJobDefinition())
					.getJobDescription().getApplication()) != null) {

				String applicationName;
				
				String applicationVersion;
				if ((applicationName = application.getApplicationName()) != null) {
					attribute = com.terradue.ogf.saga.impl.job.JobDescription.APPLICATIONNAME;
					jd.setAttribute(attribute, applicationName);
					log.info("Attribute added: " + attribute + " = "
							+ jd.getAttribute(attribute));
				}
				if ((applicationVersion = application.getApplicationVersion()) != null) {
					attribute = com.terradue.ogf.saga.impl.job.JobDescription.APPLICATIONVERSION;
					jd.setAttribute(attribute, applicationVersion);
					log.info("Attribute added: " + attribute + " = "
							+ jd.getAttribute(attribute));
				}
				POSIXApplicationType posixApp;
				SPMDApplicationType spmdApp;
				if ((posixApp = JSDLFactory.getPOSIXApplication(application)) != null) {
					jd.setAttribute(JobDescription.SPMD_POSIX, JobDescription.POSIX);
					ArgumentType[] atVector;
					if (posixApp.getArgument() != null) {
						atVector = ((ArgumentType[]) posixApp.getArgument().toArray(
								new ArgumentType[0]));
						// create a String array from an ArgumentType array
						String[] stVector = new String[atVector.length];
						for (int i = 0; i < atVector.length; i++) {
							stVector[i] = atVector[i].getValue();
						}
						attribute = JobDescription.ARGUMENTS;
						jd.setVectorAttribute(attribute, stVector);
						log.info("Attribute added: " + attribute);
					}
					EnvironmentType[] envVector;
					if (posixApp.getEnvironment() != null) {
						envVector = (EnvironmentType[]) posixApp.getEnvironment().toArray(
								new EnvironmentType[0]);
						String[] env_stVector = new String[envVector.length];
						for (int i = 0; i < envVector.length; i++) {
							env_stVector[i] = envVector[i].getName() + "="
									+ envVector[i].getValue();
						}
						attribute = JobDescription.ENVIRONMENT;
						jd.setVectorAttribute(attribute, env_stVector);
						log.info("Attribute added: " + attribute);
					}
					if (posixApp.getExecutable() != null) {
						attribute = JobDescription.EXECUTABLE;
						jd.setAttribute(attribute, posixApp.getExecutable().getValue());
						log.info("Attribute added: " + attribute + " = "
								+ jd.getAttribute(attribute));
					}
					if (posixApp.getInput() != null) {
						attribute = JobDescription.INPUT;
						jd.setAttribute(attribute, posixApp.getInput().getValue());
						log.info("Attribute added: " + attribute + " = "
								+ jd.getAttribute(attribute));
					}
					if (posixApp.getOutput() != null) {
						attribute = JobDescription.OUTPUT;
						jd.setAttribute(attribute, posixApp.getOutput().getValue());
						log.info("Attribute added: " + attribute + " = "
								+ jd.getAttribute(attribute));
					}
					if (posixApp.getError() != null) {
						attribute = JobDescription.ERROR;
						jd.setAttribute(attribute, posixApp.getError().getValue());
						log.info("Attribute added: " + attribute + " = "
								+ jd.getAttribute(attribute));
					}
					if (posixApp.getWorkingDirectory() != null) {
						attribute = JobDescription.WORKINGDIRECTORY;
						jd.setAttribute(attribute, posixApp.getWorkingDirectory()
								.getValue());
						log.info("Attribute added: " + attribute + " = "
								+ jd.getAttribute(attribute));
					}
					attribute = JobDescription.NUMBEROFPROCESSES;
					jd.setAttribute(attribute, "1");
					log.info("Attribute added: " + attribute + " = "
							+ jd.getAttribute(attribute));
				}

				if ((spmdApp = JSDLFactory.getSPMDApplication(application)) != null) {
					jd.setAttribute(JobDescription.SPMD_POSIX, JobDescription.SPMD);
					ArgumentType[] atVector;
					if (spmdApp.getArgument() != null) {
						atVector = ((ArgumentType[]) spmdApp.getArgument().toArray(
								new ArgumentType[0]));
						String[] stVector = new String[atVector.length];
						for (int i = 0; i < atVector.length; i++) {
							stVector[i] = atVector[i].getValue();
						}
						attribute = JobDescription.ARGUMENTS;
						jd.setVectorAttribute(attribute, stVector);
						log.info("Attribute added: " + attribute);
					}
					EnvironmentType[] envVector;
					if (spmdApp.getEnvironment() != null) {
						envVector = (EnvironmentType[]) spmdApp.getEnvironment().toArray(
								new EnvironmentType[0]);
						String[] env_stVector = new String[envVector.length];
						for (int i = 0; i < envVector.length; i++) {
							env_stVector[i] = envVector[i].getName() + "="
									+ envVector[i].getValue();
						}
						attribute = JobDescription.ENVIRONMENT;
						jd.setVectorAttribute(attribute, env_stVector);
						log.info("Attribute added: " + attribute);
					}
					if (spmdApp.getExecutable() != null) {
						attribute = JobDescription.EXECUTABLE;
						jd.setAttribute(attribute, spmdApp.getExecutable().getValue());
						log.info("Attribute added: " + attribute + " = "
								+ jd.getAttribute(attribute));
					}
					if (spmdApp.getInput() != null) {
						attribute = JobDescription.INPUT;
						jd.setAttribute(attribute, spmdApp.getInput().getValue());
						log.info("Attribute added: " + attribute + " = "
								+ jd.getAttribute(attribute));
					}
					if (spmdApp.getOutput() != null) {
						attribute = JobDescription.OUTPUT;
						jd.setAttribute(attribute, spmdApp.getOutput().getValue());
						log.info("Attribute added: " + attribute + " = "
								+ jd.getAttribute(attribute));
					}
					if (spmdApp.getError() != null) {
						attribute = JobDescription.ERROR;
						jd.setAttribute(attribute, spmdApp.getError().getValue());
						log.info("Attribute added: " + attribute + " = "
								+ jd.getAttribute(attribute));
					}
					if (spmdApp.getWorkingDirectory() != null) {
						attribute = JobDescription.WORKINGDIRECTORY;
						jd.setAttribute(attribute, spmdApp.getWorkingDirectory().getValue());
						log.info("Attribute added: " + attribute + " = "
								+ jd.getAttribute(attribute));
					}/*else{
						throw new IncorrectStateException("The WorkingDirectory is not specified in the JSDL template");
					}*/
					if (spmdApp.getSPMDVariation() != null) {
						attribute = JobDescription.SPMDVARIATION;
						// jd.setAttribute(attribute, spmdApp.getSPMDVariation());
						log.info("Attribute added: " + attribute + " = "
								+ jd.getAttribute(attribute));
					}
					BigInteger numberOfProject;
					NumberOfProcessesType nOfProject = spmdApp.getNumberOfProcesses();
					attribute = JobDescription.NUMBEROFPROCESSES;
					if (nOfProject != null){
						if ((numberOfProject = nOfProject.getValue()) != null) {							
							if (spmdApp.getNumberOfProcesses().getValue().intValue() < 1)
								throw new IncorrectStateException("Number Of Processes is not valid. Please check your JSDLTemplate.xml");							
							jd.setAttribute(attribute, numberOfProject.toString());
							log.info("Attribute added: " + attribute + " = "
									+ jd.getAttribute(attribute));
						}else{
							//throw new IncorrectStateException("The number of processes is incorrect or not specified. Please check your JSDLTemplate.xml");
							log.warn("The number of processes is incorrect or not specified in the JSDLTemplate. If not provided a default value (1) will be used");
						}
					}else{
						//throw new IncorrectStateException("The number of processes is incorrect or not specified.Please check your JSDLTemplate.xml");
						log.warn("The number of processes is incorrect or not specified in the JSDLTemplate. If not provided a default value (1) will be used");
					}
				}
			}

			List<String> jobProject = ((JobDefinitionType) jsdl.getJobDefinition())
					.getJobDescription().getJobIdentification().getJobProject();
			// System.out.println("project " + jobProject.size() +
			// jd.isVectorAttribute(JobDescription.JOBPROJECT));
			// jd.setVectorAttribute(JobDescription.JOBPROJECT,
			// ((String[])jobProject.toArray(new String[0])));

			CandidateHostsType candidateHosts;
			ResourcesType res;
			if ((res = ((JobDefinitionType) jsdl.getJobDefinition())
					.getJobDescription().getResources()) != null) {
				if ((candidateHosts = res.getCandidateHosts()) != null) {
					List<String> candidateHostsNames = candidateHosts.getHostName();
					attribute = JobDescription.CANDIDATEHOSTS;
					jd.setVectorAttribute(attribute,
							(String[]) candidateHostsNames.toArray(new String[0]));
					log.info("Attribute added: " + attribute);
				}
				Iterator<Object> anyI = res.getAny().iterator();
				attribute = "";
				while (anyI.hasNext()) {
					org.w3c.dom.Element el = (org.w3c.dom.Element) anyI.next();
					log.debug(el.getNodeName() + ":" + el.getTextContent());
					if (el.getNodeName().endsWith("Endpoint")) {
						attribute = JobDescription.ENDPOINT;
					}
					if (el.getNodeName().endsWith("JobManager")) {
						attribute = JobDescription.JOBMANAGER;
					}
					if (el.getNodeName().endsWith("Queue")) {
						attribute = JobDescription.QUEUE;
					}
					if (attribute.equals(""))
						continue;
					jd.setAttribute(attribute, el.getTextContent());
					log.info("Attribute added: " + attribute + " = "
							+ jd.getAttribute(attribute));
					;
				}
				String operatingSysType;
				OperatingSystemType ost;
				if ((ost = res.getOperatingSystem()) != null) {
					operatingSysType = ost.getDescription();
					attribute = JobDescription.OPERATINGSYSTEMTYPE;
					jd.setAttribute(attribute, operatingSysType);
					log.info("Attribute added: " + attribute + " = "
							+ jd.getAttribute(attribute));
				}
				CPUArchitectureType cpuAT;
				if ((cpuAT = res.getCPUArchitecture()) != null) {
					String cpuArchitecture = cpuAT.getCPUArchitectureName().value();
					attribute = JobDescription.CPUARCHITECTURE;
					jd.setAttribute(attribute, cpuArchitecture);
					log.info("Attribute added: " + attribute + " = "
							+ jd.getAttribute(attribute));
				}
				RangeValueType totCPUTime;
				if ((totCPUTime = res.getTotalCPUTime()) != null) {
					Double tot = totCPUTime.getLowerBoundedRange().getValue();
					attribute = JobDescription.TOTALCPUTIME;
					jd.setAttribute(attribute, Double.toString(tot));
					log.info("Attribute added: " + attribute + " = "
							+ jd.getAttribute(attribute));
				}
				RangeValueType totCPUCount;
				if ((totCPUCount = res.getTotalCPUCount()) != null) {
					Double tot = totCPUCount.getLowerBoundedRange().getValue();
					attribute = JobDescription.TOTALCPUCOUNT;
					jd.setAttribute(attribute, Double.toString(tot));
					log.info("Attribute added: " + attribute + " = "
							+ jd.getAttribute(attribute));
				}
				RangeValueType totalPhysicalMemory;
				if ((totalPhysicalMemory = res.getTotalPhysicalMemory()) != null) {
					Double tot = totalPhysicalMemory.getLowerBoundedRange().getValue();
					attribute = JobDescription.TOTALPHYSICALMEMORY;
					jd.setAttribute(attribute, Double.toString(tot));
					log.info("Attribute added: " + attribute + " = "
							+ jd.getAttribute(attribute));
				}
			}

			List<DataStagingType> dts;
			if ((dts = ((JobDefinitionType) jsdl.getJobDefinition())
					.getJobDescription().getDataStaging()) != null) {
				String[] filetransfers = new String[dts.size()];
 				String comp = null, filetransfersStr = "";
				int i = 0;
				for (Iterator<DataStagingType> it = dts.iterator(); it.hasNext();) {
					DataStagingType dtsI = ((DataStagingType) it.next());
					String fileName = dtsI.getFileName();

					CreationFlagEnumeration flag;
					String creationFlag;
					if ((flag = dtsI.getCreationFlag()) == null) {
						creationFlag = CreationFlagEnumeration.OVERWRITE.value();
					} else {
						creationFlag = flag.value();
					}
					SourceTargetType source, target;
					String URI = null;
					boolean magg = true;
					if ((source = dtsI.getSource()) != null) {
						URI = source.getURI();
						magg = true;
					} else {
						if ((target = dtsI.getTarget()) != null) {
							URI = target.getURI();
							magg = false;
						} else
							continue;
					}
					/*
					 * according to the creationFlag, adds a separator ( " > " source
					 * OVERWRITE, " < " target APPEND, " >> " source OVERWRITE, " << "
					 * target APPEND)
					 */
					if (creationFlag.equals(CreationFlagEnumeration.OVERWRITE.value()))
						if (magg)
							comp = " > ";
						else
							comp = " < ";
					else if (creationFlag.equals(CreationFlagEnumeration.APPEND.value()))
						if (magg)
							comp = " >> ";
						else
							comp = " << ";
					filetransfers[i] = URI + comp + fileName;
					filetransfersStr = filetransfersStr + " " + filetransfers[i] + ";\n";
					i++;
				}
				if (!dts.isEmpty()) {
					attribute = JobDescription.FILETRANSFER;
					jd.setVectorAttribute(attribute, filetransfers);
					log.info("Attribute added: " + attribute);
					log.debug("FileTransfers = " + filetransfersStr);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new NoSuccessException("Error setting attribute " + attribute
					+ " in Job Description");
		}
		return (org.ogf.saga.job.JobDescription) jd;
	}

	/**
	 * Creates a job service.
	 * 
	 * @param session
	 *          the session handle.
	 * @param rm
	 *          contact string for the resource manager. If the contact string
	 *          returns a GLUE schema, the operation is extended to read resource
	 *          information according to the GLUE specification according to the
	 *          SSEGrid-T2-D2420-3 Grid Infrastructure ICD - section 7.1.2
	 * @return the job service.
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
	 * @exception IncorrectURLException
	 *              is thrown when an implementation cannot handle the specified
	 *              protocol, or that access to the specified entity via the given
	 *              protocol is impossible.
	 * @exception BadParameterException
	 *              is thrown if the specified URL cannot be contacted, or a
	 *              default contact point does not exist or cannot be found.
	 * @exception NoSuccessException
	 *              is thrown when the operation was not successfully performed,
	 *              and none of the other exceptions apply.
	 * @throws ResourceException
	 * @throws MissingConfigurationException
	 *           is thrown if a parameter is null
	 * @throws GSSException
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws MyProxyException
	 */
	public static JobServiceImpl createJobService(Session session, URL rm)
			throws NotImplementedException, BadParameterException,
			IncorrectURLException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			TimeoutException, NoSuccessException {
		initializeFactory();

		JobServiceImpl js = null;
		try {
			js = new JobServiceImpl(session, rm);
		} catch (Exception e) {
			e.printStackTrace();
			throw new NoSuccessException("Error creating JobService");
		}
		return js;

	}

	/**
	 * Creates a job service using the default contact string. The default contact
	 * string is pointed by the gai.default.rm system property
	 * 
	 * @param session
	 *          the session handle.
	 * @return the job service.
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
	 * @exception IncorrectURLException
	 *              is thrown when an implementation cannot handle the specified
	 *              protocol, or that access to the specified entity via the given
	 *              protocol is impossible.
	 * @exception BadParameterException
	 *              is thrown if a default contact point does not exist or cannot
	 *              be found.
	 * @exception NoSuccessException
	 *              is thrown when the operation was not successfully performed,
	 *              and none of the other exceptions apply.
	 */
	public static JobServiceImpl createJobService(Session session)
			throws NotImplementedException, BadParameterException,
			IncorrectURLException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			TimeoutException, NoSuccessException {
		URL url;
		url = URLFactory.createURL(System.getProperty("gai.default.rm"));
		if (url == null) {
			throw new IncorrectURLException(
					"Error retrieving gai.default.rm system property");
		}
		initializeFactory();
		return createJobService(session, url);
	}

	/**
	 * Creates a job service, using the default session.
	 * 
	 * @param rm
	 *          contact string for the resource manager. If the contact string
	 *          returns a GLUE schema, the operation is extended to read resource
	 *          information according to the GLUE specification according to the
	 *          SSEGrid-T2-D2420-3 Grid Infrastructure ICD - section 7.1.2
	 * @return the job service.
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
	 * @exception IncorrectURLException
	 *              is thrown when an implementation cannot handle the specified
	 *              protocol, or that access to the specified entity via the given
	 *              protocol is impossible.
	 * @exception BadParameterException
	 *              is thrown if the specified URL cannot be contacted, or a
	 *              default contact point does not exist or cannot be found.
	 * @exception NoSuccessException
	 *              is thrown when the operation was not successfully performed,
	 *              and none of the other exceptions apply.
	 */
	public static JobServiceImpl createJobService(URL rm)
			throws NotImplementedException, BadParameterException,
			IncorrectURLException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			TimeoutException, NoSuccessException {
		Session session = SessionFactory.createSession(true);
		initializeFactory();
		return createJobService(session, rm);
	}

	/**
	 * Creates a job service, using the default session and default contact
	 * string. The default contact string is pointed by the gai.default.rm system
	 * property
	 * 
	 * @return the job service.
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
	 * @exception IncorrectURLException
	 *              is thrown when an implementation cannot handle the specified
	 *              protocol, or that access to the specified entity via the given
	 *              protocol is impossible.
	 * @exception BadParameterException
	 *              is thrown if a default contact point does not exist or cannot
	 *              be found.
	 * @exception NoSuccessException
	 *              is thrown when the operation was not successfully performed,
	 *              and none of the other exceptions apply.
	 * @throws IncorrectStateException
	 */
	public static JobServiceImpl createJobService()
			throws NotImplementedException, BadParameterException,
			IncorrectURLException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			TimeoutException, NoSuccessException {
		URL url;
		url = URLFactory.createURL(System.getProperty("gai.default.rm"));
		if (url == null) {
			throw new IncorrectURLException(
					"Error retrieving gai.default.rm system property");
		}
		Session session = SessionFactory.createSession(true);
		initializeFactory();
		return createJobService(session, url);
	}

}