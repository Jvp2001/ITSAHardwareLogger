package org.itsadigitaltrust.hdsentinelreader.data;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class SerialAtaFeatures
{
    @JacksonXmlElementWrapper
    @JacksonXmlProperty(localName = "S_ATA_Compliance")
    private String sataCompliance;
    @JacksonXmlElementWrapper
    @JacksonXmlProperty(localName = "S_ATA_I_Signaling_Speed_1.5_Gps")
    private String sataISignalingSpeed1_5Gps;

    @JacksonXmlElementWrapper
    @JacksonXmlProperty(localName = "S_ATA_II_Signaling_Speed_3_Gps")
    private String sataIISignalingSpeed3Gps;

    @JacksonXmlElementWrapper
    @JacksonXmlProperty(localName = "S_ATA_Gen3_Signaling_Speed_6_Gps")
    private String sataGen3SignalingSpeed6Gps;

    @JacksonXmlElementWrapper
    @JacksonXmlProperty(localName = "Receipt_Of_Power_Management_Requests_From_Host")
    private String receiptOfPowerManagementRequestsFromHost;

    @JacksonXmlElementWrapper
    @JacksonXmlProperty(localName = "PHY_Event_Counters")
    private String phyEventCounters;

    @JacksonXmlElementWrapper
    @JacksonXmlProperty(localName = "Non_Zero_Buffer_Offsets_In_DMA_Setup_FIS")
    private String nonZeroBufferOffsetsInDmaSetupFis;

    @JacksonXmlElementWrapper
    @JacksonXmlProperty(localName = "DMA_Setup_Auto_Activate_Optimization")
    private String dmaSetupAutoActivateOptimization;

    @JacksonXmlElementWrapper
    @JacksonXmlProperty(localName = "Device_Initiating_Interface_Power_Management")
    private String deviceInitiatingInterfacePowerManagement;

    @JacksonXmlElementWrapper
    @JacksonXmlProperty(localName = "In_Order_Data_Delivery")
    private String inOrderDataDelivery;

    @JacksonXmlElementWrapper
    @JacksonXmlProperty(localName = "Asynchronous_Notification")
    private String asynchronousNotification;

    @JacksonXmlElementWrapper
    @JacksonXmlProperty(localName = "Software_Settings_Preservation")
    private String softwareSettingsPreservation;

    @JacksonXmlElementWrapper
    @JacksonXmlProperty(localName = "Native_Command_Queuing_NCQ")
    private String nativeCommandQueuingNCQ;

    @JacksonXmlElementWrapper
    @JacksonXmlProperty(localName = "Queue_Length")
    private int queueLength;
}
