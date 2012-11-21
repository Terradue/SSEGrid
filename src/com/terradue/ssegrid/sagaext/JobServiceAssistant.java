/**
 * 
 */
package com.terradue.ssegrid.sagaext;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.ogf.saga.error.AuthenticationFailedException;
import org.ogf.saga.error.AuthorizationFailedException;
import org.ogf.saga.error.BadParameterException;
import org.ogf.saga.error.DoesNotExistException;
import org.ogf.saga.error.IncorrectStateException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.error.PermissionDeniedException;
import org.ogf.saga.error.TimeoutException;
import org.ogf.saga.job.JobService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.terradue.ogf.saga.impl.job.JobImpl;
import com.terradue.ogf.saga.impl.job.JobDescription;
import com.terradue.ogf.saga.impl.job.JobServiceImpl;

/**
 * This class provides useful methods for the backend WPS process manager
 * 
 * @author emathot
 * 
 */
public class JobServiceAssistant {
	Logger log = Logger.getLogger(this.getClass());
	private JobServiceImpl js;

	/**
	 * 
	 * @param js
	 *          JobService
	 */
	public JobServiceAssistant(JobService js) {
		this.js = (JobServiceImpl) js;
	}

	/**
	 * The method adds new substitution variables related to WPS directories (i.e
	 * <code>${WPS_JOB_INPUTS_DIR}, ${WPS_JOB_OUTPUTS_DIR}</code>, etc.).

	 * 
	 * These variables should be defined one time (when WPS starts) then used in
	 * any job instance. Typically, the PI uses these variables in the JSDL.
	 * 
	 * WPS Context - The method is called once, when WPS is initialized, in order
	 * to provide useful substitution variables available for the PI.
	 * 
	 * @param wpsVariables
	 *          substitution variables key-values pairs <br>
	 * <br>
	 *          Example:<br>
	 *          <code>	"WPS_JOB_INPUTS_DIR" -> "/home/wps/execute/jobs/${GAI_JOB_UID}/inputs"<br>
	 * 					"WPS_JOB_OUTPUTS_DIR" -> "/home/wps/execute/jobs/${GAI_JOB_UID}/outputs"</code>
	 * 
	 */
	public void addSubstitutionVariables(
			java.util.Map<java.lang.String, java.lang.String> wpsVariables) {

		js.addSubstitutionVariables(wpsVariables);
	}

	/**
	 * The method replaces every occurrence of the given "substitution" variables
	 * (in ANY job description attribute) with the given values in the
	 * JobDescription <code>jd</code>
	 * 
	 * 1. The given value of a variable could itself include another substitution
	 * variable (such as ${JOB_ID}).
	 * 
	 * 2. When the given variable name is "xxx", the method looks for (and
	 * replaces) occurrences of "${xxx}" in the JobDescription.
	 * 
	 * @param jd
	 *          the job description (created from the JSDL template)
	 * @param inputValues
	 *          a mapping of the substitution variables with their value (WPS
	 *          previously builds this map from the Execute request inputs) <br>
	 * <br>
	 *          Examples:<br>
	 *          <code>	"WPS_INPUT_distance" --> "10"
	 *          	"WPS_INPUT_numberOfCPUs" --> "5"
	 *          	"WPS_INPUT_outputFormat" --> "DAT"</code>
	 * @throws NoSuccessException
	 * @throws DoesNotExistException 
	 * @throws BadParameterException 
	 */
	public void substituteSimpleInputs(JobDescription jd,
			java.util.Map<java.lang.String, java.lang.String> inputValues)
			throws NoSuccessException, DoesNotExistException, BadParameterException {

		try {
			js.substituteVariables(inputValues, jd);
		} catch (BadParameterException b) {
			throw new BadParameterException(b.getMessage());
		} catch (Exception e) {
			throw new NoSuccessException("Problem when substituting variables: "
					+ e.getMessage());
		}
	}

	/**
	 * For each input (a list of URL's), it splits the m (XPATH
	 * "/ssegrid:URLList/@count") children elements (XPATH
	 * "/ssegrid:URLList/ssegrid:url") into n lists of URL's separated by a line
	 * feed (n= total number of tasks = jobs.length), then writes the file as
	 * [input identifier].[taskId] in ${ WPS_JOB_INPUTS_DIR }.
	 * 
	 * Note: This method can be used on both POSIX Applications and SPMD
	 * Applications. For POSIX Applications, n=1 is assumed (i.e. no splitting)
	 * and the .[taskId] extension is not used.
	 * 
	 * @param jobs
	 *          the related job
	 * @param inputDocuments
	 *          a mapping of input identifier with the XML Document
	 * @throws XPathExpressionException 
	 * @throws IncorrectStateException 
	 * @throws BadParameterException 
	 * @throws NoSuccessException 
	 * @throws TimeoutException 
	 * @throws DoesNotExistException 
	 * @throws PermissionDeniedException 
	 * @throws AuthorizationFailedException 
	 * @throws AuthenticationFailedException 
	 * @throws NotImplementedException 
	 */
	@SuppressWarnings("unchecked")
	public void writeComplexInputs(
			com.terradue.ogf.saga.impl.job.JobImpl jobs,
			java.util.Map<java.lang.String, org.w3c.dom.Document> inputDocuments) throws XPathExpressionException, 
			BadParameterException, IncorrectStateException, NoSuccessException, NotImplementedException, AuthenticationFailedException, AuthorizationFailedException, PermissionDeniedException, DoesNotExistException, TimeoutException 
	{	
		
		if(jobs == null || jobs.getTotalTask() == 0)
			 throw new BadParameterException("Invalid jobs array");
		if(inputDocuments == null || inputDocuments.isEmpty())
			throw new BadParameterException("Invalid Input Documents map");
		//retrieves the ${WPS_JOB_INPUTS_DIR} variable from the first Job
		String inputsDir=null;
		try {
			inputsDir = jobs.getSubstitutedVariable("WPS_JOB_INPUTS_DIR");
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (inputsDir == null || inputsDir.equals(""))
			throw new NoSuccessException("Unable to retrieve the ${WPS_JOB_INPUTS_DIR} variable");
		log.debug("Inputs Dir = " + inputsDir);
		//instantiate and creates the xpath to query the xml documents
		XPath xpath = XPathFactory.newInstance().newXPath();
		//expression to obtain the number of urls 
		XPathExpression exprCount = xpath.compile("/URLList/@count");
		//expression to obtain the urls 
		XPathExpression expr = xpath.compile("/URLList/url/text()");
		Object result = null;
		Document[] docs;
		try{
			//for every document given in input
			for (int i = 0; i < (docs = inputDocuments.values().toArray(new Document[0])).length;i++){
				//retrieves the input identifier
				String inputIdentifier = (inputDocuments.keySet().toArray(new String[0])[i]);
				//extracts the number of urls from the query ("m")
				int m = ((Double) (exprCount.evaluate(docs[i], XPathConstants.NUMBER))).intValue();
				log.debug("number m of urls = " + m);
				//extracts the m urls from the current document into a nodeset 
				result = expr.evaluate(docs[i], XPathConstants.NODESET);			
				NodeList nodes = (NodeList) result;
				if (nodes == null || nodes.getLength() == 0)
					throw new BadParameterException("Urls list is null or no url found");
				//extracts the number of jobs ("n")
				int n = jobs.getTotalTask();
				log.debug("number of jobs = " + n);
		        List<String>[] urls = new ArrayList[n];
		        int k = 0;		       
		        //creates n lists of urls with a circular assignment
		        for (int j = 0; j < nodes.getLength(); j++) {
		        	if(urls[k] == null)
		        		urls[k] = new ArrayList<String>();
		            urls[k].add(nodes.item(j).getNodeValue());
		            k++;
		            if (k == n)
		            	k = 0;
		            log.debug("value " + nodes.item(j).getNodeValue());
		        }
		        //writes n files (one file for every urls list just created)
		        for (int j = 0; j < n; j++){
		        	String taskID = j + 1 + "";
		        	String dotTaskID = "."+taskID;
		        	JobDescription jd = (JobDescription) jobs.getJobDescription();
		        	String Spmd_Posix = jd.getAttribute(JobDescription.SPMD_POSIX);
		        	if (Spmd_Posix.equals(JobDescription.POSIX))
		        		dotTaskID = "";
		        	BufferedWriter out = new BufferedWriter(new FileWriter(inputsDir + File.separator + inputIdentifier + dotTaskID));
		        	if (urls[j] != null){
			        	for (int u = 0; u < urls[j].size();u++){
			        		out.write(urls[j].get(u));
			        		out.newLine();
			        	}
		        	}
		        	out.close();
		        }
			}
		}catch (XPathExpressionException e) {
			e.printStackTrace();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	/**
	 * Read in ${WPS_JOB_OUTPUTS_DIR } the content of every given output files
	 * (used in single-task jobs only) and construct a map of filename with file
	 * content.
	 * 
	 * Note: This method can be used only for POSIX Applications.
	 * 
	 * @param job
	 *          the (single-task) job instance
	 * @return the map of output identifiers with their string value
	 * @throws IncorrectStateException 
	 * @throws BadParameterException 
	 * @throws IOException 
	 */
	public java.util.Map<java.lang.String, java.lang.String> readSimpleOutputs(JobImpl job)
			throws IncorrectStateException, BadParameterException, IOException {
		if (job == null){
			throw new BadParameterException("Error reading Simple Outputs: job is null");
		}
		Map<java.lang.String, java.lang.String> result = new HashMap<java.lang.String, java.lang.String>();
		String outputDir=null;
		try {
			//retrieves the substitution variable ${WPS_JOB_OUTPUTS_DIR}
			try {
				outputDir = job.getSubstitutedVariable("WPS_JOB_OUTPUTS_DIR");
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			if (outputDir == null || outputDir.equals(""))
				throw new IncorrectStateException("Error retrieving substitution variable ${WPS_JOB_OUTPUTS_DIR}: " + outputDir);
			//read the file named "simpleOutputs"
			FileReader fileReader = new FileReader(outputDir + File.separator + "simpleOutputs");
	        BufferedReader bufferedReader = new BufferedReader(fileReader);
	        String line = null;
	        //read two lines: output identifier and its value and put them in the returned map
	        while ((line = bufferedReader.readLine()) != null) {
	        	String name = line;
	        	line = bufferedReader.readLine();
	        	String value = line;
	        	result.put(name, value);
	        }
	        bufferedReader.close();
	        return result;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new FileNotFoundException("Cannot read the simpleOutputs file at " 
											+ outputDir + File.separator + "simpleOutputs");
		} catch (IOException e) {
			e.printStackTrace();
			throw new IOException("Error reading the simpleOutputs file at " 
											+ outputDir + File.separator + "simpleOutputs");
		}
	}


	/**
	 * For each given output name, read in ${WPS_JOB_OUTPUTS_DIR} the tasks output
	 * files as [output identifier].[taskId] containing textual URL as lists;
	 * then merges these files into a single XML document.
	 * 
	 * Note: This method can be used on both POSIX Applications and SPMD
	 * Applications. For POSIX Applications, n=1 is assumed (i.e. no merging) and
	 * the .[taskId] extension is not used.
	 * 
	 * @param jobs
	 *          the (multi-task) job instance
	 * @param outputNames
	 *          the list of ComplexData output names (note that WPS will use the
	 *          output identifier as file name)
	 * @return the map containing one xml document for every Output file  
	 * @throws IncorrectStateException
	 * 			thrown if there is an error retrieving substitution variable ${WPS_JOB_OUTPUTS_DIR}
	 * @throws BadParameterException
	 * 			thrown if one of the parameters is null or empty
	 * @throws IOException 
	 * 			thrown if there is an error in Input/Output operations on the FileSystem
	 * @throws ParserConfigurationException
	 * 			thrown if there is an error building the document 
	 * @throws NoSuccessException 
	 * @throws TimeoutException 
	 * @throws DoesNotExistException 
	 * @throws PermissionDeniedException 
	 * @throws AuthorizationFailedException 
	 * @throws AuthenticationFailedException 
	 * @throws NotImplementedException 
	 */
	public java.util.Map<java.lang.String, org.w3c.dom.Document> readComplexOutputs(
			com.terradue.ogf.saga.impl.job.JobImpl jobs, java.lang.String[] outputNames) 
				throws IncorrectStateException, BadParameterException, IOException, 
				ParserConfigurationException, NotImplementedException, AuthenticationFailedException, 
				AuthorizationFailedException, PermissionDeniedException, DoesNotExistException, TimeoutException, 
				NoSuccessException 
	{
		
		if (jobs == null || jobs.getTotalTask() == 0)
			throw new BadParameterException("The job array is null or empty");
		if (outputNames == null || outputNames.length == 0)
			throw new BadParameterException("The output names array is null or empty");
		String outputDir = null, url;
		Map<java.lang.String, org.w3c.dom.Document> docMap = new HashMap<java.lang.String, org.w3c.dom.Document>();
		//retrieves the ${WPS_JOB_OUTPUTS_DIR} substitution variable
		try {
			outputDir = jobs.getSubstitutedVariable("WPS_JOB_OUTPUTS_DIR");
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (outputDir == null || outputDir.equals(""))
			throw new IncorrectStateException("Error retrieving substitution variable ${WPS_JOB_OUTPUTS_DIR}: " + outputDir);
		DocumentBuilderFactory factory =
		      DocumentBuilderFactory.newInstance();
		int count = 0;
		String fileName = "";
		try {
			JobDescription jd = (JobDescription) jobs.getJobDescription();
        	String Spmd_Posix = jd.getAttribute(JobDescription.SPMD_POSIX);
        	String dotOne = "";
        	if (Spmd_Posix.equals(JobDescription.SPMD))
        		dotOne = ".1";
			FileReader fileReader = null;
			String urlListName = null;
			File outDir = new File(outputDir);
			FindFilter filter = new FindFilter("*" + dotOne); //TO BE REFINED WITH exitMessage EXCLUSION
			String[] listOfFilenames = outDir.list(filter);
			if (listOfFilenames == null){
				throw new FileNotFoundException("Error retrieving list of file in the Output dir: " + outputDir);
			}
			for (int j = 0; j < listOfFilenames.length; j++) {
				String uniqueFile = outputDir + File.separator + listOfFilenames[j];
				fileReader = new FileReader(uniqueFile);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
				//reads the name of the list
				urlListName = bufferedReader.readLine();
				if (urlListName == null)
					continue;
				//for every outputName
				for (int i = 0; i < outputNames.length; i++){
					if (urlListName.equals(outputNames[i])) {	
						String outputFile = listOfFilenames[j];
						//creates one Document for every outputName
						DocumentBuilder builder =
					        factory.newDocumentBuilder();
						Document document = builder.newDocument();
						//creates the root element 
						Element root = (Element)document.createElement("ssegrid:URLList");
						
						while ((url = bufferedReader.readLine()) != null) {
							//adds a child element to the document and increments the count attribute
							Element child = (Element)document.createElement("ssegrid:url");
							child.appendChild(document.createTextNode(url));
							root.appendChild(child);
							count++;
						}
						
						//for every job (except the first)
						for (int k = 1; k < jobs.getTotalTask(); k++){
							//extracts the taskID
							String taskID = k+1+""; 
							String dotTaskID = "";
							//if there is more than a job, the taskid extension is added
							if (Spmd_Posix.equals(JobDescription.SPMD))
								dotTaskID = "." + taskID;
							outputFile = outputFile.replace(dotOne, "");
							try{
								//builds the fileReader from the path "outputDir + "/" + outputName + (eventually) .TaskID"
								fileName = outputDir + File.separator + outputFile + dotTaskID;
								fileReader = new FileReader(fileName);
								bufferedReader = new BufferedReader(fileReader);
								//reads the name of the list (and discards it)
								urlListName = bufferedReader.readLine();
								while ((url = bufferedReader.readLine()) != null) {
									//adds a child element to the document and increments the count attribute
									Element child = (Element)document.createElement("ssegrid:url");
									child.appendChild(document.createTextNode(url));
									root.appendChild(child);
									count++;
								}						
							}catch(FileNotFoundException e){
								e.printStackTrace();
								throw new FileNotFoundException("Complex Output file not found " + "\n" 
																+ outputDir + File.separator + outputNames[i] + dotTaskID);
							}
						}
						//set the attributes, including the count
						root.setAttribute("xmlns:ssegrid", "http://ssegrid.esa.int/wps/JavaSAGAProfile");
						root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
						root.setAttribute("xsi:schemaLocation","http://schemas.spacebel.be/wps http://schemas.spacebel.be/wps/urllist.xsd");
						root.setAttribute("count", "" + count);
						document.appendChild(root);
						//puts the document in the returned map
						docMap.put(urlListName, (Document)document);
						count = 0;
					}
				}
				
			}
			
			
		} catch (IOException e) {
			throw new IOException("Error reading complex output file \n" + fileName); 
		} catch (ParserConfigurationException e) {
			throw new ParserConfigurationException("Error reading complex output");
		} catch (TransformerFactoryConfigurationError e) {
			throw new TransformerFactoryConfigurationError("Error reading complex output");
		}
		return docMap;
	}

	
	/**
	 * Reads in ${WPS_JOB_OUTPUTS_DIR} the tasks exit message files as
	 * exitMessage.[taskId] containing an exit code and an optional exit text;
	 * then, for each exit message file found in that directory, merges these
	 * files into a table of exit codes and exit texts.
	 * 
	 * Note: This method can be used on both POSIX Applications and SPMD
	 * Applications. For POSIX Applications, n=1 is assumed (i.e. no merging) and
	 * the .[taskId] file extension is not used.
	 * 
	 * @param jobs
	 *          the (multi-task) job instance
	 * @return A table with the merged exit messages
	 * @return A table with the merged exit messages
	 * @throws FileNotFoundException 
	 * 			thrown if Complex Output file not found
	 * @throws IncorrectStateException 
	 * 			thrown if there is a error retrieving substitution variable ${WPS_JOB_OUTPUTS_DIR}
	 * @throws BadParameterException 
	 * 			thrown if the job array is null or empty
	 * @throws NoSuccessException 
	 * @throws TimeoutException 
	 * @throws DoesNotExistException 
	 * @throws PermissionDeniedException 
	 * @throws AuthorizationFailedException 
	 * @throws AuthenticationFailedException 
	 * @throws NotImplementedException 
	 */
	public java.lang.String[][] readExitMessages(
			com.terradue.ogf.saga.impl.job.JobImpl jobs) 
			throws BadParameterException, IncorrectStateException, FileNotFoundException, NotImplementedException, AuthenticationFailedException, AuthorizationFailedException, PermissionDeniedException, DoesNotExistException, TimeoutException, NoSuccessException {
		if (jobs == null || jobs.getTotalTask() == 0)
			throw new BadParameterException("The job array is null or empty");
		//retrieves the ${WPS_JOB_OUTPUTS_DIR} substitution variable
		String [] result = new String[2];
		ArrayList<String[]> res = new ArrayList<String[]>(); 
		String outputDir = null;
		try {
			outputDir = jobs.getSubstitutedVariable("WPS_JOB_OUTPUTS_DIR");
		} catch (Exception e1) {
			throw new IncorrectStateException("Error retrieving the substitution variable ${WPS_JOB_OUTPUTS_DIR} ");
		}
		if (outputDir == null || outputDir.equals(""))
			throw new IncorrectStateException("Error retrieving substitution variable ${WPS_JOB_OUTPUTS_DIR}: " + outputDir);
		FileReader fileReader = null;
		JobDescription jd = (JobDescription) jobs.getJobDescription();
    	String Spmd_Posix = jd.getAttribute(JobDescription.SPMD_POSIX);
		for (int i = 0; i< jobs.getTotalTask(); i++){
			String dotTaskID = "";
			try{
				String taskID = i+1+""; 				
				//if there is more than a job, the taskid extension is added
				if (Spmd_Posix.equals(JobDescription.SPMD)/*jobs.getTotalTask() > 1*/)
					dotTaskID = "." + taskID;
				//builds the fileReader by the path "outputDir + "/" + outputName + (eventually) .TaskID"
				fileReader = new FileReader(outputDir + File.separator + "exitMessage" + dotTaskID);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
				String exitCode = null, exitText = null, exitTextFinal = "";
				int j = 0;
				//read the first line
				exitCode = bufferedReader.readLine();
				//for every line (exitCode/Text) in the output file
				while (exitCode != null) {
					result = new String[2];
					if(exitCode.equals("exitCode")){
						//read the exitCode
						exitCode = bufferedReader.readLine();
						//if is null or assent before the exitText or the next exitCode
						if (exitCode == null || exitCode.equals("exitText") || exitCode.equals("exitCode")){
							log.error("Malformed exitMessage file : \n"+ 
									    outputDir + File.separator + "exitMessage" + dotTaskID );
							exitCode = null;
							continue;
						}
						log.info("exitCode read " + exitCode);
						//stores the exitCode value in a String[]
						result[j] = exitCode;
					}else{
						log.error("Malformed exitMessage file : \n"+ 
									outputDir + File.separator + "exitMessage" + dotTaskID );
						exitCode = null;
						continue;
					}
					//try reading the exitText 
					if((exitText = bufferedReader.readLine()) != null 
							&& (exitText.equals("exitText")))
					{
						exitText = bufferedReader.readLine();
						exitTextFinal = exitTextFinal + exitText;
						if (exitText == null){
							log.error("Malformed exitMessage file : \n"+ 
									    outputDir + File.separator + "exitMessage" + dotTaskID );
							exitCode = null;
							continue;
						}else if (exitText.equals("exitCode")){
							//if exitText is void							
							exitText = "";
						}else{
							while ((exitText = bufferedReader.readLine()) != null){
								//read exitText value
								exitTextFinal = exitTextFinal + " " + exitText;
							}
						}
						//next exitCode read from file
						exitCode = bufferedReader.readLine();
					}else{
						//next exitCode just read (including null at EOF)
						exitCode = exitText;
						//exitText set to "" (maybe not defined)
						exitTextFinal = "";
					}
					//exitText stored in the String[]
					result[j+1] = exitTextFinal;
					log.info("exitText read " + exitTextFinal);
					//add the String[] in an ArrayList of String[]
					res.add(result);
					j = 0;
				}						
			}catch(FileNotFoundException e){
				e.printStackTrace();
				throw new FileNotFoundException("Complex Output file not found " + "\n" 
												+ outputDir + File.separator + "exitMessage" + dotTaskID);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		String[][] returned = null;
		returned = res.toArray(new String[0][0]); 
		return returned;
	}
}
