/**
 * 
 */
package com.terradue.ssegrid.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.junit.Test;
import org.ogf.saga.error.BadParameterException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.url.URL;
import org.ogf.saga.url.URLFactory;

import com.terradue.ogf.schema.jsdl.JSDLDocument;
import com.terradue.ogf.schema.jsdl.JSDLException;
import com.terradue.ogf.schema.jsdl.impl.JSDLDocumentImpl;
import com.terradue.ogf.schema.jsdl.impl.JobDefinitionType;
import com.terradue.ssegrid.sagaext.JSDLNotApplicableException;
import com.terradue.ssegrid.sagaext.ProcessingRegistry;

/**
 * @author fbarchetta
 *
 */
public class ProcessingRegistryTest  extends TestCase {
	/**
	 * Test for constructor ProcessingRegistry(). It is supposed that the environment variables and at least one JSDLTemplate
	 * is present in ${WPS_DEPLOY_PATH}
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws JSDLNotApplicableException
	 * @throws JSDLException
	 */
	@Test
	public void testProcessingRegistry() throws FileNotFoundException, IOException, JSDLNotApplicableException, 
				JSDLException
	{
		ProcessingRegistry pr = new ProcessingRegistry(true);
		Map<String, String> procList = pr.listProcessing();
		assertFalse(procList.size() == 0);
	}
	
	/**
	 * Test method for {@link com.terradue.ssegrid.sagaext.ProcessingRegistry#registerProcessing(com.terradue.ogf.schema.jsdl.JSDLDocument)}.
	 * @throws JSDLException 
	 * @throws JSDLNotApplicableException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws NoSuccessException 
	 * @throws BadParameterException 
	 */
	@Test
	public void testRegisterProcessingJSDLDocument() throws FileNotFoundException, IOException, JSDLNotApplicableException, 
				JSDLException, BadParameterException, NoSuccessException 
	{
		ProcessingRegistry pr = new ProcessingRegistry(false);
		URL procJSDL = URLFactory
				.createURL("http://storage.terradue.com/ssegrid/simpleJob/simpleJob_JSDLTemplate.xml");
		String myproc = pr.registerProcessing(com.terradue.ogf.schema.jsdl.JSDLFactory
				.createJSDLDocument(procJSDL));
		Map<String, String> procList = pr.listProcessing();
		String name = procList.keySet().toArray(new String[0])[0];
		assertEquals (myproc, name);
	}
	/**
	 * Test method for {@link com.terradue.ssegrid.sagaext.ProcessingRegistry#registerProcessing(com.terradue.ogf.schema.jsdl.JSDLDocument)}
	 * checking the creation of File on File System
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws JSDLNotApplicableException
	 * @throws JSDLException
	 * @throws BadParameterException
	 * @throws NoSuccessException
	 */
	@Test
	public void testRegisterProcessingJSDLDocumentFS() throws FileNotFoundException, IOException, JSDLNotApplicableException, 
				JSDLException, BadParameterException, NoSuccessException 
	{
		ProcessingRegistry pr = new ProcessingRegistry(true);
		URL procJSDL = URLFactory
				.createURL("http://storage.terradue.com/ssegrid/simpleJob/simpleJob_JSDLTemplate.xml");
		String myproc = pr.registerProcessing(com.terradue.ogf.schema.jsdl.JSDLFactory
				.createJSDLDocument(procJSDL));
		String GDP = System.getProperty("gai.deploy.process.path");
		String JSDLString = GDP + File.separator + myproc + File.separator + "JSDLTemplate.xml";
		File JSDLFile = new File(JSDLString); 
		assertTrue (JSDLFile != null);
	}

	/**
	 * Test method for {@link com.terradue.ssegrid.sagaext.ProcessingRegistry#registerProcessing(com.terradue.ogf.schema.jsdl.JSDLDocument)}
	 * checking the behavior in the case of registering a registered process (and then replace the previous).
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws JSDLNotApplicableException
	 * @throws JSDLException
	 * @throws BadParameterException
	 * @throws NoSuccessException
	 */
	@Test
	public void testRegisterProcessingJSDLDocumentReplacing() throws FileNotFoundException, IOException, JSDLNotApplicableException, 
				JSDLException, BadParameterException, NoSuccessException 
	{
		ProcessingRegistry pr = new ProcessingRegistry(false);
		URL procJSDL = URLFactory
				.createURL("http://storage.terradue.com/ssegrid/simpleJob/simpleJob_JSDLTemplate.xml");
		//try to register a simpleJob with procID = "multipleJob"
		pr.registerProcessing(com.terradue.ogf.schema.jsdl.JSDLFactory
				.createJSDLDocument(procJSDL), "multipleJob");
		JSDLDocumentImpl jdoc = (JSDLDocumentImpl) pr.getJSDLFromProc("multipleJob");
		JobDefinitionType jdt = (JobDefinitionType) jdoc.getJobDefinition();
		String newName = jdt.getJobDescription().getJobIdentification().getJobName();
		assert(newName.equals("simpleJob"));
	}
	
	/**
	 * Test method for {@link com.terradue.ssegrid.sagaext.ProcessingRegistry#unregisterProcessing(java.lang.String)}.
	 * @throws JSDLException 
	 * @throws JSDLNotApplicableException 
	 * @throws IOException 
	 * @throws NoSuccessException 
	 * @throws BadParameterException 
	 * @throws FileNotFoundException 
	 */
	
	public void testUnregisterProcessing() throws FileNotFoundException, BadParameterException, NoSuccessException, 
			IOException, JSDLNotApplicableException, JSDLException 
	{		
		ProcessingRegistry pr = new ProcessingRegistry(false);
		URL procJSDL = URLFactory
				.createURL("http://storage.terradue.com/ssegrid/simpleJob/simpleJob_JSDLTemplate.xml");
		String myproc = pr.registerProcessing(com.terradue.ogf.schema.jsdl.JSDLFactory
				.createJSDLDocument(procJSDL));
		pr.unregisterProcessing(myproc);
		Map<String, String> procList = pr.listProcessing();
		assertEquals(0, procList.size());
	}
	
	/**
	 * Test method for UnregisterProcessing(String ProcID) when ProcID does'nt exist
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws JSDLNotApplicableException
	 * @throws JSDLException
	 */
	
	public void testUnregisterProcessingExceptions() throws FileNotFoundException, IOException, JSDLNotApplicableException, 
		JSDLException
	{
		ProcessingRegistry pr = new ProcessingRegistry(false);
		String ProcID = null;
		try{
			pr.unregisterProcessing(ProcID);
			fail("unregisterProcessing did not throw an Exception!");
		}catch(NoSuchElementException e){
			
		}
	}
	
	/**
	 * Test method for getJSDLFromProc()
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws JSDLNotApplicableException
	 * @throws JSDLException
	 * @throws BadParameterException
	 * @throws NoSuccessException
	 */
	public void testGetJSDLFromProc() throws FileNotFoundException, IOException, JSDLNotApplicableException, 
	JSDLException, BadParameterException, NoSuccessException
	{
		ProcessingRegistry pr = new ProcessingRegistry(false);
		URL procJSDL = URLFactory
				.createURL("http://storage.terradue.com/ssegrid/simpleJob/simpleJob_JSDLTemplate.xml");
		String myproc = pr.registerProcessing(com.terradue.ogf.schema.jsdl.JSDLFactory
				.createJSDLDocument(procJSDL));
		JSDLDocument jd = pr.getJSDLFromProc(myproc);
		assertTrue(jd != null);
	}
	
	public static TestSuite suite() { 
		return new TestSuite(ProcessingRegistryTest.class); 
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
