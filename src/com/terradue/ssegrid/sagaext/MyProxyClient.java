/**
 * 
 */
package com.terradue.ssegrid.sagaext;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.log4j.Logger;
import org.globus.myproxy.MyProxy;
import org.globus.myproxy.MyProxyException;
import org.gridforum.jgss.ExtendedGSSCredential;
import org.ietf.jgss.GSSCredential;
import org.ogf.saga.context.Context;

/**
 * @author emathot
 * 
 */
public class MyProxyClient {

	// Define the logger for the Java class.
	private static Logger log = Logger.getLogger(MyProxyClient.class);

	/**
	 * Retrieve credentials from a MyProxy Server
	 * 
	 * @param MYPROXY_SERVER
	 *          Hostname of the MyProxy Server to contact.
	 * @param MYPROXY_PORT
	 *          The MyProxy Server port on which the server is listening.
	 * @param MYPROXY_USER_ACCOUNT
	 *          The user account on the MyProxy Server.
	 * @param MYPROXY_PROXYLIFETIME
	 *          The life time for the user proxy.
	 * @param MYPROXY_FILE
	 *          The file containing the user proxy.
	 * @param USERID
	 *          The Unix ID for the user.
	 * @exception IOException
	 * @throws MissingConfigurationException 
	 * 
	 * 
	 */

	public static GSSCredential delegateProxyFromMyProxyServer(
			String MYPROXY_SERVER, int MYPROXY_PORT, String MYPROXY_USER_ACCOUNT,
			String MYPROXY_PASSPHRASE, int MYPROXY_PROXYLIFETIME, Context context) throws java.io.IOException,
			org.ietf.jgss.GSSException, org.globus.myproxy.MyProxyException,
			java.io.FileNotFoundException, MissingConfigurationException {
		
		log.setLevel(org.apache.log4j.Level.INFO);

		// Initialize the MyProxy class object
		org.globus.myproxy.MyProxy myProxyServer = new MyProxy();

		// Initialize the credential class object
		org.ietf.jgss.GSSCredential credential = null;

		// Set MyProxy Server hostname
		myProxyServer.setHost(MYPROXY_SERVER);

		// Set MyProxy Server port
		myProxyServer.setPort(MYPROXY_PORT);

		// Retrieve delegated credentials from MyProxy Server anonymously (without
		// local credentials)
		credential = myProxyServer.get(MYPROXY_USER_ACCOUNT, MYPROXY_PASSPHRASE,
				MYPROXY_PROXYLIFETIME);

		if (credential != null) {

			byte[] buf = ((ExtendedGSSCredential) credential)
					.export(ExtendedGSSCredential.IMPEXP_OPAQUE);

			String X509_USER_PROXY = null;
			try {
				X509_USER_PROXY = context.getAttribute(context.USERPROXY);
			} catch (Exception e) {
				
			} 
			if (X509_USER_PROXY == null |  X509_USER_PROXY == ""){
				X509_USER_PROXY = System.getProperty("X509_USER_PROXY");
				if (X509_USER_PROXY == null |  X509_USER_PROXY == ""){
						throw new MissingConfigurationException("The X509_USER_PROXY environement variable in not present in the context or in the environment.");
				}
			}
			
			FileOutputStream out = new FileOutputStream(X509_USER_PROXY);
			out.write(buf);
			out.close();

			// Check if the proxy file has been successfully created.
			File file_proxy = new File(X509_USER_PROXY);
			if (!file_proxy.exists()) {
					throw new org.globus.myproxy.MyProxyException("The proxy cannot be created at "+X509_USER_PROXY+". Please chack the configuration.");
			}

		}
		
		else{
			throw new MyProxyException("No credential found");
		}

		// Return the user credential to the main program.
		return credential;
	}
}
