/**
 * 
 */
package com.terradue.ogf.schema.glue;

import java.io.FileOutputStream;
import java.net.MalformedURLException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.ogf.saga.url.URL;

import com.terradue.ogf.schema.glue.GLUEDocument;
import com.terradue.ogf.schema.glue.impl.GLUEDocumentImpl;
import com.terradue.ogf.schema.glue.impl.DomainsT;

/**
 * This object contains factory methods for Java content interface and Java
 * element interface generated in the com.terradue.ogf.schema.glue.impl.posix package.
 * <p>
 * An GLUEFactory allows you to programatically construct new instances of the
 * Java representation for GLUE content. The Java representation of GLUE content
 * can consist of schema derived interfaces and classes representing the binding
 * of schema type definitions, element declarations and model groups. Factory
 * methods for loading a GLUE from physical file and writing a Java
 * representation in a GLUE file <br/>
 * 
 * @date $Date: 2011-05-03 17:17:42 +0200 (Tue, 03 May 2011) $
 * @author $Author: emathot $
 * @version $Revision: 13419 $
 * 
 */
public abstract class GLUEFactory {

	/**
	 * This function will create a Java representation of the GLUE document
	 * pointed by Url
	 * 
	 * @param Url
	 *          URL of the GLUE Document
	 * @return GLUEDocument Java representation
	 * @throws GLUEException
	 *           is thrown if an error occurs during the load of the GLUE Document
	 */
	@SuppressWarnings("unchecked")
	public static GLUEDocument createGLUEDocument(java.net.URL Url)
			throws GLUEException {
		GLUEDocument GLUEDoc;

		try {
			JAXBContext jc = JAXBContext
					.newInstance("com.terradue.ogf.schema.glue.impl");
			Unmarshaller u = jc.createUnmarshaller();
			JAXBElement<DomainsT> element = (JAXBElement<DomainsT>) u
					.unmarshal(Url);
			GLUEDoc = new GLUEDocumentImpl(element);
			return GLUEDoc;
		} catch (JAXBException e) {
			e.printStackTrace();
			throw new GLUEException("Error when loading GLUE from URL: \n" + Url + "\n"
					);
		}

	}

	/**
	 * This function will create a Java representation of the GLUE document
	 * pointed by Url
	 * 
	 * @param Url
	 *          URL of the GLUE Document
	 * @return GLUEDocument Java representation
	 * @throws GLUEException
	 *           is thrown if an error occurs during the load of the GLUE Document
	 */
	public static GLUEDocument createGLUEDocument(URL GLUEUrl) throws GLUEException {
		try {
			return createGLUEDocument(new java.net.URL (GLUEUrl.normalize().getString()));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * This function will create a Java representation of the GLUE document
	 * in file File
	 * 
	 * @param Url
	 *          URL of the GLUE Document
	 * @return GLUEDocument Java representation
	 * @throws GLUEException
	 *           is thrown if an error occurs during the load of the GLUE Document
	 */
	@SuppressWarnings("unchecked")
	public static GLUEDocument createGLUEDocument(java.io.File File)
			throws GLUEException {

		GLUEDocument GLUEDoc;

		try {
			JAXBContext jc = JAXBContext
					.newInstance("com.terradue.ogf.schema.glue.impl");
			Unmarshaller u = jc.createUnmarshaller();
			JAXBElement<DomainsT> element = (JAXBElement<DomainsT>) u
					.unmarshal(File);
			GLUEDoc = new GLUEDocumentImpl(element);
		} catch (JAXBException e) {
			throw new GLUEException("Error when loading GLUE from file: "
					+ e.getMessage());
		}

		return GLUEDoc;
	}

	/**
	 * 
	 * @param glue
	 * @param destination
	 * @throws java.io.FileNotFoundException
	 * @throws java.io.IOException
	 * @throws GLUEException
	 */
	public static void writeGLUEDocument(GLUEDocument glue,
			java.io.File destination) throws java.io.FileNotFoundException,
			java.io.IOException, GLUEException {

		// Create the output stream
		java.io.FileOutputStream fos = new FileOutputStream(destination);

		try {
			JAXBContext jc = JAXBContext
					.newInstance("com.terradue.ogf.schema.glue.impl");
			Marshaller marshaller = jc.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
			marshaller.marshal(glue, fos);
			fos.flush();
			fos.close();
		} catch (JAXBException e) {
			e.printStackTrace();
			throw new GLUEException("");
		}

	}
}