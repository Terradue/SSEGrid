/**
 * 
 */
package com.terradue.ssegrid.sagaext;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;
import org.ogf.saga.job.JobDescription;
import com.terradue.ogf.schema.jsdl.JSDLDocument;
import com.terradue.ogf.schema.jsdl.JSDLFactory;
import com.terradue.ogf.schema.jsdl.JSDLException;
import com.terradue.ogf.schema.jsdl.impl.JobDefinitionType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

/**
 * This class acts as the processing shelf and manager. It registers the
 * processing described by a {@link JSDLDocument} as template using the method
 * {@link #registerProcessing(JSDLDocument)}. A registered processing template
 * is requested using {@link #getJSDLFromProc(int)} that returns the JSDL
 * document that the {@link JobFactory} object may transform into a
 * {@link JobDescription} to be used then by {@link JobService} for creating
 * Jobs.
 * 
 * At creation if the boolean <code>syncWithDisk</code> is set to
 * <code>true</code> in the constructor {@link #ProcessingRegistry(boolean)},
 * the processing registry synchronizes the operations
 * {@link #registerProcessing(JSDLDocument)} and
 * {@link #unregisterProcessing(int)} with the file system under the path set by
 * the VM property <code>${gai.deploy.process.path}</code>. Therefore,
 * when a JSDLDocument is registered, the processing registry places a file copy
 * of the template under a specific path, and deletes it when unregistered. It
 * also loads the existing JSDL file in the path enabling the recovering of the
 * registered processing template in case of crash. If the boolean is set to
 * false, no synchronization is performed.
 * 
 * 
 * @author $Author: fbarchetta $
 * @version $Revision: 14648 $
 * @date $Date: 2011-07-25 17:32:10 +0200 (Mon, 25 Jul 2011) $
 * 
 */
public class ProcessingRegistry {

	/**
	 * Create a ProcessingRegistry object ready to register and unregister
	 * processing template.
	 * 
	 * @param syncWithDisk
	 *          if set to <code>true</code>, the object registers all the JSDL
	 *          file under
	 *          <code><pre>${gai.deploy.process.path}\/*{process_id}*\/JSDLTemplate.xml</pre></code>
	 *          using <code>process_id</code> directory name as the process
	 *          identifier.
	 * @throws FileNotFoundException
	 *           is thrown when the path specified by the environment variable
	 *           ${WPS_DEPLOY_PATH} does not exist or is not readable.
	 * @throws IOException
	 *           is thrown if any I/O error occurs during operations or reading or
	 *           writing data on the disk
	 * @throws JSDLNotApplicableException
	 *           is thrown if one of the JSDL file to load on the disk is invalid
	 *
	 */
	private boolean syncWithDisk;
	private String GDP;
	private HashMap<String, JSDLDocument> templates = new HashMap<String, JSDLDocument>();
	Logger log = Logger.getLogger(this.getClass());
	
	public ProcessingRegistry(boolean syncWithDisk)
			throws java.io.FileNotFoundException, java.io.IOException,
			JSDLNotApplicableException, JSDLException {

		this.syncWithDisk = syncWithDisk;
		if (this.isSyncedWithDisk()){
			log.info("Retrieving gai.deploy.process.path system property");
			this.GDP = System.getProperty("gai.deploy.process.path");
			log.info("gai.deploy.process.path = " + GDP);
			File path;
			if (this.GDP == null || this.GDP.equals("") || ! (path = new File(GDP)).exists()){
				throw new java.io.FileNotFoundException(
					"\n Processing Registry failed: error retrieving gai.deploy.process.path property." + this.GDP);
			}else{	
				this.syncWithDisk = false; 														//To avoid re-writing files on disk
				FindFilter dirFilter = new FindFilter("*");										//Change this lines to filter the name 
				FindFilter fileFilter = new FindFilter("JSDLTemplate.xml");						//of the files to pick
				File[] dirs = path.listFiles(dirFilter);
			    for(int i=0; i<dirs.length; i++) {												//For every dir in GAI_DEPLOYPROCESS
			    	File[] files = dirs[i].listFiles(fileFilter);
			    	if ( files != null){
				    	for(int j=0; j<files.length; j++){										//For every file matching the filter in the dir
				    		log.info("Recovering processing: " + files[j].getAbsolutePath());
				    		this.registerProcessing(JSDLFactory.createJSDLDocument(files[j]));
				    	}
			    	}
			    }
			    this.syncWithDisk = true;
			}
		}
	}

	/**
	 * Registers template and returns its identifier. The template is validated
	 * beforehand. If the synchronization with the disk is enabled, the operations
	 * write a file copy of the JSDL template under
	 * <code><pre>${GAI_DEPLOYPROCESS_PATH}\/*{process_id}*\/JSDLTemplate.xml</pre></code>
	 * and creates the directory if not present.
	 * 
	 * @param template
	 *          object implementing the JSDLDocument interface and that contains
	 *          the processing template. {@link JSDLFactory} may create such an
	 *          object from a physical file.
	 * @return the processing identifier that is generated from the
	 *         <code>JobName</code> element of the JSDL document. If
	 *         <code>JobName</code> is not present, a random UUID is generated.
	 * @throws FileNotFoundException
	 *           is thrown when the path specified by the environment variable
	 *           ${WPS_DEPLOY_PATH} does not exist or is not readable.
	 * @throws IOException
	 *           is thrown if any I/O error occurs during operations or reading or
	 *           writing data on the disk
	 * @throws JSDLNotApplicableException
	 *           is thrown if the JSDL template is invalid
	 */
	public String registerProcessing(JSDLDocument template)
			throws java.io.FileNotFoundException, java.io.IOException,
			JSDLNotApplicableException, JSDLException 
	{
		if (template == null)
			throw new JSDLException("JSDL template is not valid");
		log.info("Extracting proc ID from JSDL");
		String jn = ((JobDefinitionType) template.getJobDefinition()) 		// Extract procID from the JSDL																			
				.getJobDescription().getJobIdentification().getJobName();	// template
		this.registerProcessing(template, jn); 							    
		return jn;
	}

	/**
	 * Same as {@link #registerProcessing(JSDLDocument)} but the processing
	 * identifier is passed as an argument
	 * 
	 * @see registerProcessing(JSDLDocument)
	 * 
	 * @param procID
	 *          processing identifier to use for this template
	 */
	public void registerProcessing(JSDLDocument template, String procID)
			throws JSDLNotApplicableException, JSDLException, IOException {

		if (template == null)
			throw new JSDLException("JSDL template is not valid");		
		
		if (procID == null || procID.equals("")){
			/*generate random UUID*/
			log.info("A random UUID will be generated");
			UUID procUUID = UUID.randomUUID();
			procID = procUUID.toString();
		}
		log.info("Registring process ID: " + procID);
		this.templates.put(procID, template); 											// Adding template to the in-memory	map
		if (this.isSyncedWithDisk()) {
			// Write file on disk
			if (this.GDP == null || this.GDP.equals("")){
				throw new java.io.FileNotFoundException(
					"\n Processing Registry failed: error retrieving $GAI_DEPLOYPROCESS_PATH environment variable.");
			}
			
			File dr = new File(GDP + File.separator + procID);  			           // Create process directory (if not exists)
			if ( ! dr.exists()) {
				dr.mkdirs();
			}
			String JSDLString = GDP + File.separator + procID + File.separator + "JSDLTemplate.xml";
			File JSDLFile = new File(JSDLString);  										//Create new xml file and invoke 
																						//JSDLFactory to write the template
			log.info("Writing JSDL xml file from the template");
			JSDLFactory.writeJSDLDocument(template, JSDLFile);
		}

	}

	/**
	 * Removes the processing template identified by procID from the registry. If
	 * synchronization with the disk is enabled, the file under
	 * <code><pre>${GAI_DEPLOYPROCESS_PATH}\/*{process_id}*\/JSDLTemplate.xml</pre></code>
	 * id deleted from the disk but not the directory.
	 * 
	 * @param ProcID
	 *          the identifier of the processing template to remove
	 * @throws NoSuchElementException
	 *           if the processing identifier provided does not correspond to any
	 *           registered processing.
	 */
	public void unregisterProcessing(String ProcID)
			throws java.util.NoSuchElementException, FileNotFoundException {
		if (ProcID != null && ! ProcID.equals("") && this.templates.containsKey(ProcID)){
			log.info("Unregistring process ID: " + ProcID);
			this.templates.remove(ProcID);												//Remove template from map
		}else
			throw new java.util.NoSuchElementException("ProcID \"" + ProcID + "\" invalid or not found.");
		if (this.isSyncedWithDisk()){
			String JSDLString = GDP + File.separator + ProcID + File.separator + "JSDLTemplate.xml";		//Remove template's physical files from disk, 
																						//in the path named as the ProcessID, but not 
																						//the directory
			File JSDLFile = new File(JSDLString);
			if ( JSDLFile == null || ! JSDLFile.exists())
				throw new NoSuchElementException("The processing identifier provided does not correspond to any registered processing" + JSDLString);
			JSDLFile.delete();
		
		}
	}

	/**
	 * Returns the list of the registered processing in the registry.
	 * 
	 * @return a Map with the processing identifier as the key and the
	 *         description. Description is the string in the
	 *         <code>JobDescription</code> element in the JSDL processing
	 *         template.
	 */
	public java.util.Map<String, String> listProcessing() {
		int i;
		JSDLDocument jd;
		String jobName, jobDescription;
		java.util.HashMap<String, String> jobsMap = new java.util.HashMap<String, String>();
		for (i = 0; i < this.templates.size(); i++){
			jd = new ArrayList<JSDLDocument> (this.templates.values()).get(i);
			if (jd != null){
				jobName = ((JobDefinitionType)jd.getJobDefinition())
								.getJobDescription().getJobIdentification().getJobName();
				jobDescription = ((JobDefinitionType)jd.getJobDefinition())
								.getJobDescription().getJobIdentification().getDescription();
				jobsMap.put(jobName, jobDescription);
			}
		}
		return jobsMap;
	}

	/**
	 * 
	 * @param myproc
	 * @return JSDLDocument object corresponding to the processing ID
	 */
	public JSDLDocument getJSDLFromProc(String myproc) {
		JSDLDocument template;
		if ((template = templates.get(myproc)) != null)
			return template;
		else{
			throw new NoSuchElementException("The processing identifier provided does not correspond to any registered processing: " + myproc);
		}
	}

	/**
	 * 
	 * @return true if the processing registry is synchronized with the disk
	 */
	public boolean isSyncedWithDisk() {
		return this.syncWithDisk;
	}

}
