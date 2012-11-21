package com.terradue.ssegrid.test;

import java.io.FileNotFoundException;
import java.io.IOException;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.junit.Test;
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
import org.ogf.saga.session.Session;
import org.ogf.saga.session.SessionFactory;
import org.ogf.saga.url.URL;
import org.ogf.saga.url.URLFactory;

import com.terradue.ogf.saga.impl.job.JobDescription;
import com.terradue.ogf.saga.impl.job.JobFactory;
import com.terradue.ogf.saga.impl.job.JobServiceImpl;
import com.terradue.ogf.schema.jsdl.JSDLException;
import com.terradue.ssegrid.sagaext.JSDLNotApplicableException;
import com.terradue.ssegrid.sagaext.ProcessingRegistry;

public class JobFactoryTest extends TestCase{

	/**Method that tests the {@link com.terradue.ogf.saga.impl.job.JobFactory#createJobDescription(com.terradue.ogf.schema.jsdl.JSDLDocument)}
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws JSDLNotApplicableException
	 * @throws JSDLException
	 * @throws BadParameterException
	 * @throws NoSuccessException
	 * @throws NotImplementedException
	 * @throws AuthenticationFailedException
	 * @throws AuthorizationFailedException
	 * @throws PermissionDeniedException
	 * @throws IncorrectStateException
	 * @throws DoesNotExistException
	 * @throws TimeoutException
	 */
	@Test
	public void testCreateJobDescriptionJSDLDocument() throws FileNotFoundException, IOException, JSDLNotApplicableException, 
		JSDLException, BadParameterException, NoSuccessException, NotImplementedException, AuthenticationFailedException, 
		AuthorizationFailedException, PermissionDeniedException, IncorrectStateException, DoesNotExistException, 
		TimeoutException 
	{
		URL procJSDL;
		ProcessingRegistry pr = new ProcessingRegistry(false);
		procJSDL = URLFactory.createURL("http://storage.terradue.com/ssegrid/simpleJob/simpleJob_JSDLTemplate.xml");
		String myproc = pr.registerProcessing(com.terradue.ogf.schema.jsdl.JSDLFactory
						.createJSDLDocument(procJSDL));
		JobDescription jd = (JobDescription) JobFactory.createJobDescription(pr.getJSDLFromProc(myproc));
		assertTrue(jd.listAttributes().length > 0);
	}

/*
	/**Method that tests the {@link com.terradue.ogf.saga.impl.job.JobFactory#createJobDescription(com.terradue.ogf.schema.jsdl.JSDLDocument)}
	 * Exception
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws JSDLNotApplicableException
	 * @throws JSDLException
	 * @throws BadParameterException
	 * @throws NoSuccessException
	 * @throws NotImplementedException
	 * @throws AuthenticationFailedException
	 * @throws AuthorizationFailedException
	 * @throws PermissionDeniedException
	 * @throws IncorrectStateException
	 * @throws DoesNotExistException
	 * @throws TimeoutException
	 */
/*	@Test
	public void testCreateJobDescriptionJSDLDocumentException() throws FileNotFoundException, IOException, JSDLNotApplicableException, 
		JSDLException, BadParameterException, NoSuccessException, NotImplementedException, AuthenticationFailedException, 
		AuthorizationFailedException, PermissionDeniedException, IncorrectStateException, DoesNotExistException, 
		TimeoutException 
	{
		try{
			JobFactory.createJobDescription(null);
			fail("createJobDescription(null) did not throw an exception");
		}catch (BadParameterException b){
			
		}
	}*/
	
	/**Method that tests the {@link com.terradue.ogf.saga.impl.job.JobFactory#createJobService(Session, URL)}
	 * @throws NoSuccessException 
	 * @throws TimeoutException 
	 * @throws DoesNotExistException 
	 * @throws BadParameterException 
	 * @throws IncorrectStateException 
	 * @throws PermissionDeniedException 
	 * @throws AuthorizationFailedException 
	 * @throws AuthenticationFailedException 
	 * @throws NotImplementedException 
	 * @throws IncorrectURLException 
	 * 
	 */
	@Test
	public void testCreateJobServiceSessionURL() throws NotImplementedException, AuthenticationFailedException, 
				AuthorizationFailedException, PermissionDeniedException, IncorrectStateException, 
				BadParameterException, DoesNotExistException, TimeoutException, NoSuccessException, IncorrectURLException 
	{
		String WPSHome = System.getenv("WPS_HOME");
		if (WPSHome == null)
			fail("WPSHome environment variable is not set!");
		Session session = SessionFactory.createSession(false);
		Context context = ContextFactory.createContext("globus");
		context.setAttribute(Context.USERPROXY, WPSHome + "/proxy");
		session.addContext(context);
		String url = System.getProperty("gai.default.rm");
		if (url == null)
			fail("Property gai.default.rm not set!");
		URL gridmapGLUE = URLFactory
				.createURL(url);
		JobServiceImpl js = JobFactory.createJobService(session, gridmapGLUE);
		assertFalse(js == null);
	}
	
	/**Method that tests the {@link com.terradue.ogf.saga.impl.job.JobFactory#createJobService(Session)}
	 * @throws NoSuccessException 
	 * @throws TimeoutException 
	 * @throws DoesNotExistException 
	 * @throws BadParameterException 
	 * @throws IncorrectStateException 
	 * @throws PermissionDeniedException 
	 * @throws AuthorizationFailedException 
	 * @throws AuthenticationFailedException 
	 * @throws NotImplementedException 
	 * @throws IncorrectURLException 
	 * 
	 */
	@Test
	public void testCreateJobServiceSession() throws NotImplementedException, AuthenticationFailedException, 
				AuthorizationFailedException, PermissionDeniedException, IncorrectStateException, 
				BadParameterException, DoesNotExistException, TimeoutException, NoSuccessException, IncorrectURLException 
	{
		String WPSHome = System.getenv("WPS_HOME");
		if (WPSHome == null)
			fail("WPSHome environment variable is not set!");
		Session session = SessionFactory.createSession(false);
		Context context = ContextFactory.createContext("globus");
		context.setAttribute(Context.USERPROXY, WPSHome + "/proxy");
		session.addContext(context);
		JobServiceImpl js = JobFactory.createJobService(session);
		assertFalse(js == null);
	}

	/**Method that tests the {@link com.terradue.ogf.saga.impl.job.JobFactory#createJobService(URL)}
	 * @throws NoSuccessException 
	 * @throws TimeoutException 
	 * @throws DoesNotExistException 
	 * @throws BadParameterException 
	 * @throws IncorrectStateException 
	 * @throws PermissionDeniedException 
	 * @throws AuthorizationFailedException 
	 * @throws AuthenticationFailedException 
	 * @throws NotImplementedException 
	 * @throws IncorrectURLException 
	 * 
	 */
	@Test
	public void testCreateJobServiceURL() throws NotImplementedException, AuthenticationFailedException, 
				AuthorizationFailedException, PermissionDeniedException, IncorrectStateException, 
				BadParameterException, DoesNotExistException, TimeoutException, NoSuccessException, IncorrectURLException 
	{
		String url = System.getProperty("gai.default.rm");
		if (url == null)
			fail("Property gai.default.rm not set!");
		URL gridmapGLUE = URLFactory
			.createURL(url);
		JobServiceImpl js = JobFactory.createJobService(gridmapGLUE);
		assertFalse(js == null);
	}

	/**Method that tests the {@link com.terradue.ogf.saga.impl.job.JobFactory#createJobService()}
	 * @throws NoSuccessException 
	 * @throws TimeoutException 
	 * @throws DoesNotExistException 
	 * @throws BadParameterException 
	 * @throws IncorrectStateException 
	 * @throws PermissionDeniedException 
	 * @throws AuthorizationFailedException 
	 * @throws AuthenticationFailedException 
	 * @throws NotImplementedException 
	 * @throws IncorrectURLException 
	 * 
	 */
	@Test
	public void testCreateJobService() throws NotImplementedException, AuthenticationFailedException, 
				AuthorizationFailedException, PermissionDeniedException, IncorrectStateException, 
				BadParameterException, DoesNotExistException, TimeoutException, NoSuccessException, IncorrectURLException 
	{
		JobServiceImpl js = JobFactory.createJobService();
		assertFalse(js == null);
	}

	public static TestSuite suite() { 
		return new TestSuite(JobFactoryTest.class); 
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
