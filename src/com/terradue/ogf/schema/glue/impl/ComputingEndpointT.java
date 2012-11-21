//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0-b52-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.05.03 at 05:26:33 PM CEST 
//


package com.terradue.ogf.schema.glue.impl;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import com.terradue.ogf.schema.glue.impl.ComputingEndpointT.Associations;


/**
 * <p>Java class for ComputingEndpoint_t complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ComputingEndpoint_t">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.ogf.org/glue/2008/05/spec_2.0_d42_r01}EndpointProperties_t">
 *       &lt;sequence>
 *         &lt;element name="Staging" type="{http://schemas.ogf.org/glue/2008/05/spec_2.0_d42_r01}Staging_t" minOccurs="0"/>
 *         &lt;element name="JobDescription" type="{http://schemas.ogf.org/glue/2008/05/spec_2.0_d42_r01}JobDescription_t" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="Extensions" type="{http://schemas.ogf.org/glue/2008/05/spec_2.0_d42_r01}Extensions_t" minOccurs="0"/>
 *         &lt;element name="AccessPolicy" type="{http://schemas.ogf.org/glue/2008/05/spec_2.0_d42_r01}AccessPolicy_t" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="Associations" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="ComputingShareLocalID" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *                   &lt;element name="ComputingActivityID" type="{http://schemas.ogf.org/glue/2008/05/spec_2.0_d42_r01}ID_t" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ComputingEndpoint_t", propOrder = {
    "staging",
    "jobDescription",
    "extensions",
    "accessPolicy",
    "associations"
})
public class ComputingEndpointT
    extends EndpointPropertiesT
{

    @XmlElement(name = "Staging")
    protected String staging;
    @XmlElement(name = "JobDescription", required = true)
    protected List<String> jobDescription;
    @XmlElement(name = "Extensions")
    protected ExtensionsT extensions;
    @XmlElement(name = "AccessPolicy", required = true)
    protected List<AccessPolicyT> accessPolicy;
    @XmlElement(name = "Associations")
    protected Associations associations;

    /**
     * Gets the value of the staging property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStaging() {
        return staging;
    }

    /**
     * Sets the value of the staging property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStaging(String value) {
        this.staging = value;
    }

    /**
     * Gets the value of the jobDescription property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the jobDescription property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getJobDescription().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getJobDescription() {
        if (jobDescription == null) {
            jobDescription = new ArrayList<String>();
        }
        return this.jobDescription;
    }

    /**
     * Gets the value of the extensions property.
     * 
     * @return
     *     possible object is
     *     {@link ExtensionsT }
     *     
     */
    public ExtensionsT getExtensions() {
        return extensions;
    }

    /**
     * Sets the value of the extensions property.
     * 
     * @param value
     *     allowed object is
     *     {@link ExtensionsT }
     *     
     */
    public void setExtensions(ExtensionsT value) {
        this.extensions = value;
    }

    /**
     * Gets the value of the accessPolicy property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the accessPolicy property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAccessPolicy().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AccessPolicyT }
     * 
     * 
     */
    public List<AccessPolicyT> getAccessPolicy() {
        if (accessPolicy == null) {
            accessPolicy = new ArrayList<AccessPolicyT>();
        }
        return this.accessPolicy;
    }

    /**
     * Gets the value of the associations property.
     * 
     * @return
     *     possible object is
     *     {@link Associations }
     *     
     */
    public Associations getAssociations() {
        return associations;
    }

    /**
     * Sets the value of the associations property.
     * 
     * @param value
     *     allowed object is
     *     {@link Associations }
     *     
     */
    public void setAssociations(Associations value) {
        this.associations = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="ComputingShareLocalID" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
     *         &lt;element name="ComputingActivityID" type="{http://schemas.ogf.org/glue/2008/05/spec_2.0_d42_r01}ID_t" maxOccurs="unbounded" minOccurs="0"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "computingShareLocalID",
        "computingActivityID"
    })
    public static class Associations {

        @XmlElement(name = "ComputingShareLocalID", required = true)
        protected List<String> computingShareLocalID;
        @XmlElement(name = "ComputingActivityID", required = true)
        protected List<String> computingActivityID;

        /**
         * Gets the value of the computingShareLocalID property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the computingShareLocalID property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getComputingShareLocalID().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link String }
         * 
         * 
         */
        public List<String> getComputingShareLocalID() {
            if (computingShareLocalID == null) {
                computingShareLocalID = new ArrayList<String>();
            }
            return this.computingShareLocalID;
        }

        /**
         * Gets the value of the computingActivityID property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the computingActivityID property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getComputingActivityID().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link String }
         * 
         * 
         */
        public List<String> getComputingActivityID() {
            if (computingActivityID == null) {
                computingActivityID = new ArrayList<String>();
            }
            return this.computingActivityID;
        }

    }

}