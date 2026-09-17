package org.itsadigitaltrust.hardwarelogger.views

import org.itsadigitaltrust.common.logging.HWLLoggable
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.NotificationCentre
import org.itsadigitaltrust.hardwarelogger.shared.HardwareLoggerSharedData
import org.itsadigitaltrust.hardwarelogger.viewmodels.{BaseViewModel, ViewModel}
import scalafx.scene.Node

trait View[VM <: BaseViewModel] extends HWLLoggable:
  this: Node => /** This means only subclasses of [[scalafx.scene.Node]] can implement this trait. */
  given viewModel: VM = scala.compiletime.deferred
  
  def sharedApplicationData: HardwareLoggerSharedData = NotificationCentre.getFetchedData.get

end View
