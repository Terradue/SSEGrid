//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0-b52-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.05.03 at 05:26:33 PM CEST 
//


package com.terradue.ogf.schema.glue.impl;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;


/**
 * <p>Java class for ServingState_t.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="ServingState_t">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="production"/>
 *     &lt;enumeration value="draining"/>
 *     &lt;enumeration value="queuing"/>
 *     &lt;enumeration value="closed"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlEnum
public enum ServingStateT {

    @XmlEnumValue("closed")
    CLOSED("closed"),
    @XmlEnumValue("draining")
    DRAINING("draining"),
    @XmlEnumValue("production")
    PRODUCTION("production"),
    @XmlEnumValue("queuing")
    QUEUING("queuing");
    private final String value;

    ServingStateT(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ServingStateT fromValue(String v) {
        for (ServingStateT c: ServingStateT.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v.toString());
    }

}
