package org.itsadigitaltrust.hardwarelogger.core

import com.softwaremill.macwire.*
import org.itsadigitaltrust.hardwarelogger.models.HardwareModel
import org.itsadigitaltrust.hardwarelogger.services.{HardwareGrabberService, OshiHardwareGrabberService}
import org.itsadigitaltrust.hardwarelogger.viewmodels.{TabTableViewModel, TableRowViewModel}
import org.itsadigitaltrust.hardwarelogger.views.tabs.TabTableView

object DIManager:
  def getHardwareGrabberService: HardwareGrabberService =
    autowire[HardwareGrabberService](classOf[OshiHardwareGrabberService])

  def getNotificationCentre: NotificationCentre = autowire[NotificationCentre](classOf[SimpleNotificationCentre])

