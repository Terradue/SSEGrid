//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.05.03 at 04:52:12 PM CEST 
//


package com.terradue.ogf.schema.jsdl.impl;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for OperatingSystemTypeEnumeration.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="OperatingSystemTypeEnumeration">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Unknown"/>
 *     &lt;enumeration value="MACOS"/>
 *     &lt;enumeration value="ATTUNIX"/>
 *     &lt;enumeration value="DGUX"/>
 *     &lt;enumeration value="DECNT"/>
 *     &lt;enumeration value="Tru64_UNIX"/>
 *     &lt;enumeration value="OpenVMS"/>
 *     &lt;enumeration value="HPUX"/>
 *     &lt;enumeration value="AIX"/>
 *     &lt;enumeration value="MVS"/>
 *     &lt;enumeration value="OS400"/>
 *     &lt;enumeration value="OS_2"/>
 *     &lt;enumeration value="JavaVM"/>
 *     &lt;enumeration value="MSDOS"/>
 *     &lt;enumeration value="WIN3x"/>
 *     &lt;enumeration value="WIN95"/>
 *     &lt;enumeration value="WIN98"/>
 *     &lt;enumeration value="WINNT"/>
 *     &lt;enumeration value="WINCE"/>
 *     &lt;enumeration value="NCR3000"/>
 *     &lt;enumeration value="NetWare"/>
 *     &lt;enumeration value="OSF"/>
 *     &lt;enumeration value="DC_OS"/>
 *     &lt;enumeration value="Reliant_UNIX"/>
 *     &lt;enumeration value="SCO_UnixWare"/>
 *     &lt;enumeration value="SCO_OpenServer"/>
 *     &lt;enumeration value="Sequent"/>
 *     &lt;enumeration value="IRIX"/>
 *     &lt;enumeration value="Solaris"/>
 *     &lt;enumeration value="SunOS"/>
 *     &lt;enumeration value="U6000"/>
 *     &lt;enumeration value="ASERIES"/>
 *     &lt;enumeration value="TandemNSK"/>
 *     &lt;enumeration value="TandemNT"/>
 *     &lt;enumeration value="BS2000"/>
 *     &lt;enumeration value="LINUX"/>
 *     &lt;enumeration value="Lynx"/>
 *     &lt;enumeration value="XENIX"/>
 *     &lt;enumeration value="VM"/>
 *     &lt;enumeration value="Interactive_UNIX"/>
 *     &lt;enumeration value="BSDUNIX"/>
 *     &lt;enumeration value="FreeBSD"/>
 *     &lt;enumeration value="NetBSD"/>
 *     &lt;enumeration value="GNU_Hurd"/>
 *     &lt;enumeration value="OS9"/>
 *     &lt;enumeration value="MACH_Kernel"/>
 *     &lt;enumeration value="Inferno"/>
 *     &lt;enumeration value="QNX"/>
 *     &lt;enumeration value="EPOC"/>
 *     &lt;enumeration value="IxWorks"/>
 *     &lt;enumeration value="VxWorks"/>
 *     &lt;enumeration value="MiNT"/>
 *     &lt;enumeration value="BeOS"/>
 *     &lt;enumeration value="HP_MPE"/>
 *     &lt;enumeration value="NextStep"/>
 *     &lt;enumeration value="PalmPilot"/>
 *     &lt;enumeration value="Rhapsody"/>
 *     &lt;enumeration value="Windows_2000"/>
 *     &lt;enumeration value="Dedicated"/>
 *     &lt;enumeration value="OS_390"/>
 *     &lt;enumeration value="VSE"/>
 *     &lt;enumeration value="TPF"/>
 *     &lt;enumeration value="Windows_R_Me"/>
 *     &lt;enumeration value="Caldera_Open_UNIX"/>
 *     &lt;enumeration value="OpenBSD"/>
 *     &lt;enumeration value="Not_Applicable"/>
 *     &lt;enumeration value="Windows_XP"/>
 *     &lt;enumeration value="z_OS"/>
 *     &lt;enumeration value="other"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "OperatingSystemTypeEnumeration")
@XmlEnum
public enum OperatingSystemTypeEnumeration {

    @XmlEnumValue("Unknown")
    UNKNOWN("Unknown"),
    MACOS("MACOS"),
    ATTUNIX("ATTUNIX"),
    DGUX("DGUX"),
    DECNT("DECNT"),
    @XmlEnumValue("Tru64_UNIX")
    TRU_64_UNIX("Tru64_UNIX"),
    @XmlEnumValue("OpenVMS")
    OPEN_VMS("OpenVMS"),
    HPUX("HPUX"),
    AIX("AIX"),
    MVS("MVS"),
    @XmlEnumValue("OS400")
    OS_400("OS400"),
    OS_2("OS_2"),
    @XmlEnumValue("JavaVM")
    JAVA_VM("JavaVM"),
    MSDOS("MSDOS"),
    @XmlEnumValue("WIN3x")
    WIN_3_X("WIN3x"),
    @XmlEnumValue("WIN95")
    WIN_95("WIN95"),
    @XmlEnumValue("WIN98")
    WIN_98("WIN98"),
    WINNT("WINNT"),
    WINCE("WINCE"),
    @XmlEnumValue("NCR3000")
    NCR_3000("NCR3000"),
    @XmlEnumValue("NetWare")
    NET_WARE("NetWare"),
    OSF("OSF"),
    DC_OS("DC_OS"),
    @XmlEnumValue("Reliant_UNIX")
    RELIANT_UNIX("Reliant_UNIX"),
    @XmlEnumValue("SCO_UnixWare")
    SCO_UNIX_WARE("SCO_UnixWare"),
    @XmlEnumValue("SCO_OpenServer")
    SCO_OPEN_SERVER("SCO_OpenServer"),
    @XmlEnumValue("Sequent")
    SEQUENT("Sequent"),
    IRIX("IRIX"),
    @XmlEnumValue("Solaris")
    SOLARIS("Solaris"),
    @XmlEnumValue("SunOS")
    SUN_OS("SunOS"),
    @XmlEnumValue("U6000")
    U_6000("U6000"),
    ASERIES("ASERIES"),
    @XmlEnumValue("TandemNSK")
    TANDEM_NSK("TandemNSK"),
    @XmlEnumValue("TandemNT")
    TANDEM_NT("TandemNT"),
    @XmlEnumValue("BS2000")
    BS_2000("BS2000"),
    LINUX("LINUX"),
    @XmlEnumValue("Lynx")
    LYNX("Lynx"),
    XENIX("XENIX"),
    VM("VM"),
    @XmlEnumValue("Interactive_UNIX")
    INTERACTIVE_UNIX("Interactive_UNIX"),
    BSDUNIX("BSDUNIX"),
    @XmlEnumValue("FreeBSD")
    FREE_BSD("FreeBSD"),
    @XmlEnumValue("NetBSD")
    NET_BSD("NetBSD"),
    @XmlEnumValue("GNU_Hurd")
    GNU_HURD("GNU_Hurd"),
    @XmlEnumValue("OS9")
    OS_9("OS9"),
    @XmlEnumValue("MACH_Kernel")
    MACH_KERNEL("MACH_Kernel"),
    @XmlEnumValue("Inferno")
    INFERNO("Inferno"),
    QNX("QNX"),
    EPOC("EPOC"),
    @XmlEnumValue("IxWorks")
    IX_WORKS("IxWorks"),
    @XmlEnumValue("VxWorks")
    VX_WORKS("VxWorks"),
    @XmlEnumValue("MiNT")
    MI_NT("MiNT"),
    @XmlEnumValue("BeOS")
    BE_OS("BeOS"),
    HP_MPE("HP_MPE"),
    @XmlEnumValue("NextStep")
    NEXT_STEP("NextStep"),
    @XmlEnumValue("PalmPilot")
    PALM_PILOT("PalmPilot"),
    @XmlEnumValue("Rhapsody")
    RHAPSODY("Rhapsody"),
    @XmlEnumValue("Windows_2000")
    WINDOWS_2000("Windows_2000"),
    @XmlEnumValue("Dedicated")
    DEDICATED("Dedicated"),
    OS_390("OS_390"),
    VSE("VSE"),
    TPF("TPF"),
    @XmlEnumValue("Windows_R_Me")
    WINDOWS_R_ME("Windows_R_Me"),
    @XmlEnumValue("Caldera_Open_UNIX")
    CALDERA_OPEN_UNIX("Caldera_Open_UNIX"),
    @XmlEnumValue("OpenBSD")
    OPEN_BSD("OpenBSD"),
    @XmlEnumValue("Not_Applicable")
    NOT_APPLICABLE("Not_Applicable"),
    @XmlEnumValue("Windows_XP")
    WINDOWS_XP("Windows_XP"),
    @XmlEnumValue("z_OS")
    Z_OS("z_OS"),
    @XmlEnumValue("other")
    OTHER("other");
    private final String value;

    OperatingSystemTypeEnumeration(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static OperatingSystemTypeEnumeration fromValue(String v) {
        for (OperatingSystemTypeEnumeration c: OperatingSystemTypeEnumeration.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}