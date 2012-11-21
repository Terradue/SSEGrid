/**
 * 
 */
package com.terradue.ogf.schema.glue.impl;


import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import com.terradue.ogf.schema.glue.GLUEDocument;
import com.terradue.ogf.schema.glue.impl.DomainsT;


/**
 * @author emathot
 *
 */
@SuppressWarnings("serial")
@XmlRootElement
public class GLUEDocumentImpl extends JAXBElement<DomainsT> implements GLUEDocument  {


	public GLUEDocumentImpl(QName name, Class<DomainsT> declaredType,
			DomainsT value) {
		super(name, declaredType, value);
		// TODO Auto-generated constructor stub
	}

	public GLUEDocumentImpl(JAXBElement<DomainsT> element) {
		// TODO Auto-generated constructor stub
		super(element.getName(),DomainsT.class,element.getValue());
	}

	//@Override
	public Object getDomains() {
		// TODO Auto-generated method stub
		return this.getValue();
	}

}