package com.terradue.ssegrid.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLConnection;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.junit.Test;
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
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.terradue.ogf.saga.impl.job.JobDescription;
import com.terradue.ogf.saga.impl.job.JobFactory;
import com.terradue.ogf.saga.impl.job.JobImpl;
import com.terradue.ogf.saga.impl.job.JobServiceImpl;
import com.terradue.ogf.schema.jsdl.JSDLException;
import com.terradue.ssegrid.sagaext.JSDLNotApplicableException;
import com.terradue.ssegrid.sagaext.JobServiceAssistant;
import com.terradue.ssegrid.sagaext.ProcessingRegistry;

public class JobServiceAssistantTest extends TestCase{

	java.net.URL inputURLList;
	URLConnection inputURLListuc;
	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	DocumentBuilder db;
	Document inputXML;	
	java.util.Map<String, org.w3c.dom.Document> inputXMLMap = new java.util.HashMap<String, org.w3c.dom.Document>();
	JobDescription jd;
	JobImpl jobs;
	JobServiceImpl js;
	JobServiceAssistant jsa;
	java.util.Map<String, String> WPSSubstitutionVar;
	java.util.Map<String, String> WPS_INPUT = new java.util.HashMap<String, String>();
	
	@Override
	protected void setUp() throws IOException, ParserConfigurationException, SAXException, NotImplementedException, 
								BadParameterException, IncorrectURLException, AuthenticationFailedException, 
								AuthorizationFailedException, PermissionDeniedException, TimeoutException, NoSuccessException, 
								JSDLNotApplicableException, JSDLException, IncorrectStateException, DoesNotExistException
	{
		try {
			super.setUp();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String myproc = "multipleJob";
		String WPSHome = System.getenv("WPS_HOME");
		if (WPSHome == null)
			fail("WPS_HOME environment variable not set");
		// prepare Map with all persistent substitution variable
		WPSSubstitutionVar = new java.util.HashMap<String, String>();
		WPSSubstitutionVar.put("WPS_DEPLOY_PROCESS_DIR",
				WPSHome + "/deploy/process/");
		WPSSubstitutionVar.put("WPS_DEPLOY_AUXDATA_DIR",
				WPSHome + "/deploy/auxdata/");
		WPSSubstitutionVar.put("WPS_USER_ID","emathot");
		WPSSubstitutionVar.put("WPS_PROCESS_ID",myproc);
		WPSSubstitutionVar.put("WPS_PROCESS_INST_ID","INST_1");
		WPSSubstitutionVar.put("WPS_JOB_INPUTS_DIR",
				WPSHome + "/execute/${WPS_USER_ID}/${WPS_PROCESS_ID}/${WPS_PROCESS_INST_ID}/${GAI_JOB_UID}/inputs");
		WPSSubstitutionVar.put("WPS_JOB_OUTPUTS_DIR",
				WPSHome + "/execute/${WPS_USER_ID}/${WPS_PROCESS_ID}/${WPS_PROCESS_INST_ID}/${GAI_JOB_UID}/outputs");
		WPSSubstitutionVar.put("WPS_JOB_AUDITS_DIR",
				WPSHome + "/execute/${WPS_USER_ID}/${WPS_PROCESS_ID}/${WPS_PROCESS_INST_ID}/${GAI_JOB_UID}/audits");

		WPS_INPUT.put("WPS_INPUT_roiTopLeftLat",     "72");
		WPS_INPUT.put("WPS_INPUT_roiTopLeftLon",     "-15");
		WPS_INPUT.put("WPS_INPUT_roiBottomRightLat", "28");
		WPS_INPUT.put("WPS_INPUT_roiBottomRightLon", "60");
	
		inputURLList = new java.net.URL("http://storage.terradue.com/ssegrid/simpleJob/InputURLList.xml");
		inputURLListuc = inputURLList.openConnection();
		db = dbf.newDocumentBuilder();
		inputXML = db.parse(inputURLListuc.getInputStream());
		inputXMLMap.put("inputURLs", inputXML);
		js = JobFactory.createJobService();
		jsa = new JobServiceAssistant(js);
		ProcessingRegistry pr = new ProcessingRegistry(true);
		jd = (JobDescription) JobFactory.createJobDescription(pr
				.getJSDLFromProc(myproc));
		jsa.addSubstitutionVariables(WPSSubstitutionVar);
		jobs = ((JobServiceImpl) js).createJob(jd);
	//	jsa.substituteSimpleInputs(jd, WPS_INPUT);
	}
/**
 * Test method for {@link com.terradue.ssegrid.sagaext.JobServiceAssistant#writeComplexInputs(JobImpl, Map)
 * @throws XPathExpressionException
 * @throws BadParameterException
 * @throws IncorrectStateException
 * @throws NoSuccessException
 * @throws IOException
 * @throws NotImplementedException
 * @throws AuthenticationFailedException
 * @throws AuthorizationFailedException
 * @throws PermissionDeniedException
 * @throws DoesNotExistException
 * @throws TimeoutException
 */
	@Test
	public void testWriteComplexInputs() throws XPathExpressionException, BadParameterException, 
					IncorrectStateException, NoSuccessException, IOException, NotImplementedException, 
					AuthenticationFailedException, AuthorizationFailedException, PermissionDeniedException, 
					DoesNotExistException, TimeoutException
	{
		jsa.writeComplexInputs(jobs, inputXMLMap);
		String inputsDir = jobs.getSubstitutedVariable("WPS_JOB_INPUTS_DIR");
		File inpF = new File(inputsDir + File.separator + "inputURLs");
		assertFalse(inpF == null);
	}
	/**
	 * Test method for {@link com.terradue.ssegrid.sagaext.JobServiceAssistant#readSimpleOutputs(JobImpl)
	 * n.b. No jobs are submitted and run
	 * @throws IncorrectStateException
	 * @throws IOException
	 * @throws BadParameterException
	 */
	@Test
	public void testReadSimpleOutputs() throws IncorrectStateException, IOException, BadParameterException
	{
		String outputDir=null;
		outputDir = jobs.getSubstitutedVariable("WPS_JOB_OUTPUTS_DIR");
		if (outputDir == null || outputDir.equals(""))
			fail("Error retrieving substitution variable ${WPS_JOB_OUTPUTS_DIR}: " + outputDir);
		(new File(outputDir)).mkdirs();
		File f = new File(outputDir + File.separator + "simpleOutputs");
		BufferedWriter out = new BufferedWriter(new FileWriter(f));
		out.write("Output1");
		out.newLine();
		out.write("1");
		out.close();
		Map<java.lang.String, java.lang.String> simpleOutput = jsa.readSimpleOutputs(jobs);
		String out1 = (String) simpleOutput.get("Output1");
		assertEquals(out1, "1");
	}
	
	/**
	 * Test method for {@link com.terradue.ssegrid.sagaext.JobServiceAssistant#readSimpleOutputs(JobImpl)
	 * exceptions
	 * n.b. No jobs are submitted and run
	 * @throws IncorrectStateException
	 * @throws IOException
	 * @throws BadParameterException
	 */
	@Test
	public void testReadSimpleOutputsExceptions() throws IncorrectStateException, IOException, BadParameterException
	{
		try{
			jsa.readSimpleOutputs(jobs);
			fail("readSimpleOutputs(JobImpl) does not catch FileNotFoundException");
		}catch (FileNotFoundException e){

		}
		
	}
	
	
	/**
	 * Test method for {@link com.terradue.ssegrid.sagaext.JobServiceAssistant#readComplexOutputs(JobImpl, String[])
	 * n.b. No jobs are submitted and run
	 * @throws IncorrectStateException
	 * @throws BadParameterException
	 * @throws NotImplementedException
	 * @throws AuthenticationFailedException
	 * @throws AuthorizationFailedException
	 * @throws PermissionDeniedException
	 * @throws DoesNotExistException
	 * @throws TimeoutException
	 * @throws NoSuccessException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	@Test
	public void testReadComplexOutputs() throws IncorrectStateException, BadParameterException, 
				NotImplementedException, AuthenticationFailedException, AuthorizationFailedException, 
				PermissionDeniedException, DoesNotExistException, TimeoutException, NoSuccessException, 
				IOException, ParserConfigurationException 
	{
		String outputDir = jobs.getSubstitutedVariable("WPS_JOB_OUTPUTS_DIR");
		(new File(outputDir)).mkdirs();
		String Spmd_Posix = jd.getAttribute(JobDescription.SPMD_POSIX);
		for (int j = 0; j < jobs.getTotalTask(); j++){
			String taskID = j+1+""; 
			String dotTaskID = "";
			//if there is more than a job, the taskid extension is added
			if (Spmd_Posix.equals(JobDescription.SPMD))
				dotTaskID = "." + taskID;
			File f = new File(outputDir + File.separator + "importedList" + dotTaskID);
			BufferedWriter out = new BufferedWriter(new FileWriter(f));
			out.write("importedList\n");
			out.write("gsiftp://ce1.intern.vgt.vito.be:2811//EO_DATA/RESULTS_DIRS/550e8400-e29b-41d4-a716-446655440000/20101128//MOD02QKM.A2010332.0855.005.2010332163737.hdf\n");
			out.write("gsiftp://ce1.intern.vgt.vito.be:2811//EO_DATA/RESULTS_DIRS/550e8400-e29b-41d4-a716-446655440000/20101128//MOD02QKM.A2010332.0720.005.2010332162918.hdf\n");
			out.close();
		}
		java.util.Map<String, Document> complexoutputXMLMap = jsa.readComplexOutputs(jobs,
				new String[] { "importedList" });
		Document complexoutputXML = (Document) complexoutputXMLMap.get("importedList");
		assertTrue(complexoutputXML != null);
		
	}
	
	/**
	 * Test method for {@link com.terradue.ssegrid.sagaext.JobServiceAssistant#readComplexOutputs(JobImpl, String[])
	 * Exceptions
	 * n.b. No jobs are submitted and run
	 * @throws IncorrectStateException
	 * @throws BadParameterException
	 * @throws NotImplementedException
	 * @throws AuthenticationFailedException
	 * @throws AuthorizationFailedException
	 * @throws PermissionDeniedException
	 * @throws DoesNotExistException
	 * @throws TimeoutException
	 * @throws NoSuccessException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	@Test
	public void testReadComplexOutputsExceptions() throws IncorrectStateException, BadParameterException, 
				NotImplementedException, AuthenticationFailedException, AuthorizationFailedException, 
				PermissionDeniedException, DoesNotExistException, TimeoutException, NoSuccessException, 
				IOException, ParserConfigurationException 
	{
		try{
			jsa.readComplexOutputs(jobs,
					new String[] { "importedList" });
		} catch (FileNotFoundException e){
					
		} catch (IOException e) {

		}
	}
	
	/**
	 * Test method for {@link com.terradue.ssegrid.sagaext.JobServiceAssistant#readExitMessages(JobImpl)
	 * n.b. No jobs are submitted and run
	 * @throws NotImplementedException
	 * @throws AuthenticationFailedException
	 * @throws AuthorizationFailedException
	 * @throws PermissionDeniedException
	 * @throws IncorrectStateException
	 * @throws DoesNotExistException
	 * @throws TimeoutException
	 * @throws NoSuccessException
	 * @throws IOException
	 * @throws BadParameterException
	 */
	
	@Test
	public void testReadExitMessages() throws NotImplementedException, AuthenticationFailedException, 
				AuthorizationFailedException, PermissionDeniedException, IncorrectStateException, DoesNotExistException, 
				TimeoutException, NoSuccessException, IOException, BadParameterException 
	{
		String outputDir = jobs.getSubstitutedVariable("WPS_JOB_OUTPUTS_DIR");
		(new File(outputDir)).mkdirs();
		String Spmd_Posix = jd.getAttribute(JobDescription.SPMD_POSIX);
		for (int j = 0; j < jobs.getTotalTask(); j++){
			String taskID = j+1+""; 
			String dotTaskID = "";
			//if there is more than a job, the taskid extension is added
			if (Spmd_Posix.equals(JobDescription.SPMD))
				dotTaskID = "." + taskID;
			File f = new File(outputDir + File.separator + "exitMessage" + dotTaskID);
			BufferedWriter out = new BufferedWriter(new FileWriter(f));
			out.write("exitCode");
			out.newLine();
			out.write("0");
			out.newLine();
			out.write("exitText");
			out.newLine();
			out.write("processing completed");
			out.close();
		}
		String[][] map = jsa.readExitMessages(jobs);
		assertTrue(map[0][0].equals("0") && map[0][1].equals("processing completed"));
	}

	/**
	 * Test method for {@link com.terradue.ssegrid.sagaext.JobServiceAssistant#substituteSimpleInputs(JobDescription, 
	 * 																java.util.Map<java.lang.String, java.lang.String>)

	 * @throws NoSuccessException 	 
	 * @throws TimeoutException 
	 * @throws DoesNotExistException 
	 * @throws IncorrectStateException 
	 * @throws PermissionDeniedException 
	 * @throws AuthorizationFailedException 
	 * @throws AuthenticationFailedException 
	 * @throws NotImplementedException 
	 * @throws BadParameterException */
	
	@Test
	public void testSubstituteSimpleInputs() throws NoSuccessException, NotImplementedException, 
							AuthenticationFailedException, AuthorizationFailedException, PermissionDeniedException, 
							IncorrectStateException, DoesNotExistException, TimeoutException, BadParameterException
	{		
		WPS_INPUT.put("WPS_INPUT_roiTopLeftLat",     "72");
		WPS_INPUT.put("WPS_INPUT_roiTopLeftLon",     "-15");
		WPS_INPUT.put("WPS_INPUT_roiBottomRightLat", "28");
		WPS_INPUT.put("WPS_INPUT_roiBottomRightLon", "60");
		String[] argVett = jd.getVectorAttribute(JobDescription.ARGUMENTS);
		boolean isBefore = false, isAfter = false; 
		for (int i = 0; i < argVett.length; i++){
			if(argVett[i].equals("${WPS_INPUT_roiTopLeftLat}")){
				isBefore = true;
				break;
			}
		}
		jsa.substituteSimpleInputs(jd, WPS_INPUT);
		for (int i = 0; i < argVett.length; i++)
		{
			if(argVett[i].equals("${WPS_INPUT_roiTopLeftLat}")){
				isAfter = true;
				break;
			}
		}
		assert (isBefore && !isAfter);
	}
	
	/**
	 * Test method for {@link com.terradue.ssegrid.sagaext.JobServiceAssistant#substituteSimpleInputs(JobDescription, 
	 * 																java.util.Map<java.lang.String, java.lang.String>)
	 * exception, checking if a typo in the wps_input file raises an exception.

	 * @throws NoSuccessException 	 
	 * @throws TimeoutException 
	 * @throws DoesNotExistException 
	 * @throws IncorrectStateException 
	 * @throws PermissionDeniedException 
	 * @throws AuthorizationFailedException 
	 * @throws AuthenticationFailedException 
	 * @throws NotImplementedException 
	 * @throws BadParameterException */
	
	@Test
	public void testSubstituteSimpleInputsException() throws NoSuccessException, NotImplementedException, 
							AuthenticationFailedException, AuthorizationFailedException, PermissionDeniedException, 
							IncorrectStateException, DoesNotExistException, TimeoutException, BadParameterException
	{		
		WPS_INPUT.put("errorWPS_INPUT_roiBottomRightLon", "60");
		try {
			jsa.substituteSimpleInputs(jd, WPS_INPUT);
			fail("Exception not raised");
		} catch (BadParameterException b) {
			if ( ! b.getMessage().contains("errorWPS_INPUT")){
				throw new BadParameterException();
			}
		}		
	}	
	
	public static TestSuite suite() { 
		return new TestSuite(JobServiceAssistantTest.class); 
    } 

	/** 

    * This main method is used for run tests for this class only 

    * from command line. 

    */ 

	public static void main(String[] args) { 
        /* to use command line interface  */ 
		junit.textui.TestRunner.run(suite()); 
    }
	
}
