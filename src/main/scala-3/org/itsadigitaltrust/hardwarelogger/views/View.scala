package org.itsadigitaltrust.hardwarelogger.views

import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.NotificationCentre
import org.itsadigitaltrust.hardwarelogger.shared.HardwareLoggerSharedData
import org.itsadigitaltrust.hardwarelogger.viewmodels.{BaseViewModel, ViewModel}
import scalafx.scene.Node

trait View[VM <: BaseViewModel](using viewModel: VM) extends HWLLoggable:
  this: Node => /** This means only subclasses of [[scalafx.scene.Node]] can implement this trait. */
  
  def sharedApplicationData: HardwareLoggerSharedData = NotificationCentre.getFetchedData.get

end View
