package org.itsadigitaltrust.hardwarelogger.views

import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{NotificationName, Reload, ReloadMessageArgs}
import org.itsadigitaltrust.hardwarelogger.services.{ApplicationServicesRegistry, NotificationCentreModule}
import org.itsadigitaltrust.hardwarelogger.viewmodels.ViewModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.HardDrivesTabViewModel
import org.scalafx.extras.offFX
import scalafx.application.Platform
import scalafx.scene.Node

trait Reloadable extends Reload:
  override def reload(): Unit = reload(true)
  def reload(shouldClearData: Boolean): Unit
trait View[VM <: ViewModel] extends Reloadable with NotificationCentreModule:
  this: Node => /** This means only subclasses of [[scalafx.scene.Node]] can implement this trait. */
  given viewModel: VM = scala.compiletime.deferred

  notificationCentre.addObserver(NotificationName.Reload)(reload)

//  Platform.runLater:
//    offFX:
//      viewModel.setup()


  override def reload(shouldClearData: Boolean = true): Unit =
    viewModel.reload(shouldClearData)