/**
 * 
 */
package com.terradue.ogf.schema.jsdl.impl;


import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import com.terradue.ogf.schema.jsdl.JSDLDocument;
import com.terradue.ogf.schema.jsdl.impl.JobDefinitionType;


/**
 * @author emathot
 *
 */
@SuppressWarnings("serial")
@XmlRootElement
public class JSDLDocumentImpl extends JAXBElement<JobDefinitionType> implements JSDLDocument  {


	public JSDLDocumentImpl(QName name, Class<JobDefinitionType> declaredType,
			JobDefinitionType value) {
		super(name, declaredType, value);
		// TODO Auto-generated constructor stub
	}

	public JSDLDocumentImpl(JAXBElement<JobDefinitionType> element) {
		// TODO Auto-generated constructor stub
		super(element.getName(),JobDefinitionType.class,element.getValue());
	}

	//@Override
	public Object getJobDefinition() {
		// TODO Auto-generated method stub
		return this.getValue();
	}

}