package org.itsadigitaltrust.hdsentinelreader.data;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class SecurityMode
{
    @JacksonXmlElementWrapper
    @JacksonXmlProperty(localName = "Security_Mode")
    private String securityMode;

    @JacksonXmlElementWrapper
    @JacksonXmlProperty(localName = "Security_Erase")
    private String securityErase;

    @JacksonXmlElementWrapper
    @JacksonXmlProperty(localName = "Security_Erase_Time")
    private String securityEraseTime;

    @JacksonXmlElementWrapper
    @JacksonXmlProperty(localName = "Security_Enhanced_Erase_Feature")
    private String securityEnhancedEraseFeature;

    @JacksonXmlElementWrapper
    @JacksonXmlProperty(localName = "Security_Enhanced_Erase_Time")
    private String securityEnhancedEraseTime;

    @JacksonXmlElementWrapper
    @JacksonXmlProperty(localName = "Security_Enabled")
    private String securityEnabled;

    @JacksonXmlElementWrapper
    @JacksonXmlProperty(localName = "Security_Locked")
    private String securityLocked;

    @JacksonXmlElementWrapper
    @JacksonXmlProperty(localName = "Security_Frozen")
    private String securityFrozen;

    @JacksonXmlElementWrapper
    @JacksonXmlProperty(localName = "Security_Counter_Expired")
    private String securityCounterExpired;

    @JacksonXmlElementWrapper
    @JacksonXmlProperty(localName = "Security_Level")
    private String securityLevel;
}