package com.terradue.ssegrid.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

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

import com.terradue.ogf.saga.impl.job.JobDescription;
import com.terradue.ogf.saga.impl.job.JobFactory;
import com.terradue.ogf.saga.impl.job.JobImpl;
import com.terradue.ogf.saga.impl.job.JobServiceImpl;
import com.terradue.ogf.schema.jsdl.JSDLException;
import com.terradue.ssegrid.sagaext.JSDLNotApplicableException;
import com.terradue.ssegrid.sagaext.JobServiceAssistant;
import com.terradue.ssegrid.sagaext.ProcessingRegistry;

public class JobServiceImplTest extends TestCase{

	JobServiceAssistant jsa;
	JobServiceImpl js;
	JobDescription jd;
	java.util.Map<String, String> WPS_INPUT;
	JobImpl jobs;
	java.util.Map<String, String> WPSSubstitutionVar;
	
	@Override
	protected void setUp() throws NotImplementedException, BadParameterException, 
				IncorrectURLException, AuthenticationFailedException, AuthorizationFailedException, 
				PermissionDeniedException, TimeoutException, NoSuccessException, FileNotFoundException, 
				IOException, JSDLNotApplicableException, JSDLException, IncorrectStateException, 
				DoesNotExistException
	{
		String myproc = "multipleJob";
		String WPSHome = System.getenv("WPS_HOME");
		if (WPSHome == null)
			fail("WPS_HOME environment variable not set");
		// prepare Map with all persistent substitution variable
		WPSSubstitutionVar = new java.util.HashMap<String, String>();
		WPSSubstitutionVar.put("WPS_DEPLOY_PROCESS_DIR",
				WPSHome + "/deploy/process");
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

		WPS_INPUT = new java.util.HashMap<String, String>();
		WPS_INPUT.put("WPS_INPUT_roiTopLeftLat",     "72");
		WPS_INPUT.put("WPS_INPUT_roiTopLeftLon",     "-15");
		WPS_INPUT.put("WPS_INPUT_roiBottomRightLat", "28");
		WPS_INPUT.put("WPS_INPUT_roiBottomRightLon", "60");

		ProcessingRegistry pr = new ProcessingRegistry(true);
		jd = (JobDescription) JobFactory.createJobDescription(pr
				.getJSDLFromProc(myproc));
		js = JobFactory.createJobService();
		jsa = new JobServiceAssistant(js);
		jsa.addSubstitutionVariables(WPSSubstitutionVar);
		jsa.substituteSimpleInputs(jd, WPS_INPUT);
	}
	
	/**
	 * Test method for {@link com.terradue.ogf.saga.impl.job.JobServiceImpl.substituteVariables(Map<String, String>, JobDescription)
	 */
	
	@Test
	public void testSubstituteVariables() 
	{
		try{
			js.substituteVariables(WPSSubstitutionVar, jd);
			assert(true);
		}catch(BadParameterException e){
			assert(true);
		}catch(Exception e){
			e.printStackTrace();
			fail("Method SubstituteVariables failed");
		}
	}	
	
	/**
	 * Test method for {@link com.terradue.ssegrid.tests.JobServiceImplTest.testCreateJob()
	 */
	
	@Test
	public void testCreateJob() {
		try {
			jobs = ((JobServiceImpl) js).createJob(jd);
			assert(jobs != null && jobs.getJobArray().length > 0);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Method createJob failed");
		}
	}

	public static TestSuite suite() { 
		return new TestSuite(JobServiceImplTest.class); 
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
