//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0-b52-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.05.03 at 05:26:33 PM CEST 
//


package com.terradue.ogf.schema.glue.impl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ActivityProperties_t complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ActivityProperties_t">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.ogf.org/glue/2008/05/spec_2.0_d42_r01}Entity">
 *       &lt;sequence>
 *         &lt;element name="ID" type="{http://schemas.ogf.org/glue/2008/05/spec_2.0_d42_r01}ID_t"/>
 *       &lt;/sequence>
 *       &lt;attribute name="BaseType" use="required" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" fixed="Activity" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ActivityProperties_t", propOrder = {
    "id"
})
public class ActivityPropertiesT
    extends Entity
{

    @XmlElement(name = "ID", required = true)
    protected String id;
    @XmlAttribute(name = "BaseType", required = true)
    protected String baseType;

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getID() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setID(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the baseType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBaseType() {
        if (baseType == null) {
            return "Activity";
        } else {
            return baseType;
        }
    }

    /**
     * Sets the value of the baseType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBaseType(String value) {
        this.baseType = value;
    }

}