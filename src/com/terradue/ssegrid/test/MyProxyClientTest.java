package com.terradue.ssegrid.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.globus.myproxy.MyProxyException;
import org.ietf.jgss.GSSException;
import org.junit.Test;
import org.ogf.saga.context.Context;
import org.ogf.saga.context.ContextFactory;
import org.ogf.saga.error.AuthenticationFailedException;
import org.ogf.saga.error.AuthorizationFailedException;
import org.ogf.saga.error.BadParameterException;
import org.ogf.saga.error.DoesNotExistException;
import org.ogf.saga.error.IncorrectStateException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.error.PermissionDeniedException;
import org.ogf.saga.error.TimeoutException;

import com.terradue.ssegrid.sagaext.MissingConfigurationException;
import com.terradue.ssegrid.sagaext.MyProxyClient;

public class MyProxyClientTest extends TestCase {

	@Test
	public void testDelegateProxyFromMyProxyServer() throws IncorrectStateException, TimeoutException, 
											NoSuccessException, MyProxyException, FileNotFoundException, IOException, 
											GSSException, MissingConfigurationException, NotImplementedException, 
											AuthenticationFailedException, AuthorizationFailedException, 
											PermissionDeniedException, BadParameterException, DoesNotExistException 
	{
		String WPSHome = System.getenv("WPS_HOME");
		Context context = ContextFactory.createContext("globus");
		context.setAttribute(Context.USERPROXY, WPSHome + "/proxy");
		MyProxyClient.delegateProxyFromMyProxyServer("ify-ce03.terradue.com",
				7512, "emathot", "myproxy", 604800, context); 
		File proxy = new File(WPSHome + "/proxy");
		assert (proxy != null);
	}

	public static TestSuite suite() { 
		return new TestSuite(MyProxyClientTest.class); 
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
