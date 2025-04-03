package org.itsadigitaltrust.hdsentinelreader.data;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

import javax.xml.bind.annotation.XmlElement;

@Value
@NoArgsConstructor(force = true, access =  AccessLevel.PRIVATE)
@AllArgsConstructor
public class AcousticManagementConfiguration {
    @JacksonXmlElementWrapper
    @XmlElement(name = "Acoustic_Management")
    private String acousticManagement;

    @JacksonXmlElementWrapper
    @XmlElement(name = "Current_Acoustic_Level")
    private String currentAcousticLevel;

    @JacksonXmlElementWrapper
    @XmlElement(name = "Recommended_Acoustic_Level")
    private String recommendedAcousticLevel;
}
