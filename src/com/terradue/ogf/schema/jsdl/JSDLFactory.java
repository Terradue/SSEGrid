/**
 * 
 */
package com.terradue.ogf.schema.jsdl;

import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.ogf.saga.url.URL;

import com.terradue.ogf.schema.jsdl.JSDLDocument;
import com.terradue.ogf.schema.jsdl.impl.ApplicationType;
import com.terradue.ogf.schema.jsdl.impl.JSDLDocumentImpl;
import com.terradue.ogf.schema.jsdl.impl.JobDefinitionType;
import com.terradue.ogf.schema.jsdl.impl.POSIXApplicationType;
import com.terradue.ogf.schema.jsdl.impl.SPMDApplicationType;

/**
 * This object contains factory methods for Java content interface and Java
 * element interface generated in the com.terradue.ogf.schema.jsdl.impl.posix
 * package.
 * <p>
 * An JSDLFactory allows you to programatically construct new instances of the
 * Java representation for JSDL content. The Java representation of JSDL content
 * can consist of schema derived interfaces and classes representing the binding
 * of schema type definitions, element declarations and model groups. Factory
 * methods for loading a JSDL from physical file and writing a Java
 * representation in a JSDL file <br/>
 * 
 * @date $Date: 2011-10-04 17:15:27 +0200 (Tue, 04 Oct 2011) $
 * @author $Author: emathot $
 * @version $Revision: 15918 $
 * 
 */
public abstract class JSDLFactory {

	/**
	 * This function will create a Java representation of the JSDL document
	 * pointed by Url
	 * 
	 * @param Url
	 *          URL of the JSDL Document
	 * @return JSDLDocument Java representation
	 * @throws JSDLException
	 *           is thrown if an error occurs during the load of the JSDL Document
	 */
	@SuppressWarnings("unchecked")
	public static JSDLDocument createJSDLDocument(java.net.URL Url)
			throws JSDLException {
		JSDLDocument JSDLDoc;

		try {
			JAXBContext jc = JAXBContext
					.newInstance("com.terradue.ogf.schema.jsdl.impl");
			Unmarshaller u = jc.createUnmarshaller();
			JAXBElement<JobDefinitionType> element = (JAXBElement<JobDefinitionType>) u
					.unmarshal(Url);
			JSDLDoc = new JSDLDocumentImpl(element);
		} catch (JAXBException e) {
			throw new JSDLException("Error when loading JSDL from URL: "
					+ e.getMessage());
		}

		return JSDLDoc;
	}

	/**
	 * This function will create a Java representation of the JSDL document
	 * pointed by Url
	 * 
	 * @param Url
	 *          URL of the JSDL Document
	 * @return JSDLDocument Java representation
	 * @throws JSDLException
	 *           is thrown if an error occurs during the load of the JSDL Document
	 */
	public static JSDLDocument createJSDLDocument(URL JSDLUrl)
			throws JSDLException {
		try {
			return createJSDLDocument(new java.net.URL(JSDLUrl.normalize()
					.getString()));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * This function will create a Java representation of the JSDL document in
	 * file File
	 * 
	 * @param Url
	 *          URL of the JSDL Document
	 * @return JSDLDocument Java representation
	 * @throws JSDLException
	 *           is thrown if an error occurs during the load of the JSDL Document
	 */
	@SuppressWarnings("unchecked")
	public static JSDLDocument createJSDLDocument(java.io.File File)
			throws JSDLException {

		JSDLDocument JSDLDoc;

		try {
			JAXBContext jc = JAXBContext
					.newInstance("com.terradue.ogf.schema.jsdl.impl");
			Unmarshaller u = jc.createUnmarshaller();
			JAXBElement<JobDefinitionType> element = (JAXBElement<JobDefinitionType>) u
					.unmarshal(File);
			JSDLDoc = new JSDLDocumentImpl(element);
		} catch (JAXBException e) {
			e.printStackTrace();
			throw new JSDLException("Error when loading JSDL from file"
					+ File.getPath());
		}

		return JSDLDoc;
	}

	/**
	 * 
	 * @param jsdl
	 * @param destination
	 * @throws java.io.FileNotFoundException
	 * @throws java.io.IOException
	 * @throws JSDLException
	 */
	public static void writeJSDLDocument(JSDLDocument jsdl,
			java.io.File destination) throws java.io.FileNotFoundException,
			java.io.IOException, JSDLException {

		// Create the output stream
		java.io.FileOutputStream fos = new FileOutputStream(destination);

		try {
			JAXBContext jc = JAXBContext
					.newInstance("com.terradue.ogf.schema.jsdl.impl");
			Marshaller marshaller = jc.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
			marshaller.marshal(jsdl, fos);
			fos.flush();
			fos.close();
		} catch (JAXBException e) {
			e.printStackTrace();
			throw new JSDLException("");
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static SPMDApplicationType getSPMDApplication(ApplicationType app) {
		try {
			List any = app.getAny();
			// System.out.println(any.size());
			JAXBElement<SPMDApplicationType> spmdApp = (JAXBElement<SPMDApplicationType>) any
					.get(0);
			return spmdApp.getValue();
		} catch (ClassCastException e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static POSIXApplicationType getPOSIXApplication(ApplicationType app) {
		try {
			List any = app.getAny();
			// System.out.println(any.size());
			JAXBElement<POSIXApplicationType> spmdApp = (JAXBElement<POSIXApplicationType>) any
					.get(0);
			return spmdApp.getValue();
		} catch (ClassCastException e) {
			return null;
		}
	}
}