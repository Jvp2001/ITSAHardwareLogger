package org.itsadigitaltrust.hardwarelogger.views

import org.itsadigitaltrust.hardwarelogger.services.ServicesModule
import org.itsadigitaltrust.hardwarelogger.viewmodels.ViewModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.HardDrivesTabViewModel
import scalafx.application.Platform
import scalafx.scene.Node

trait View[VM <: ViewModel]:
  this: Node => /** This means only subclasses of [[scalafx.scene.Node]] can implement this trait. */
  given viewModel: VM = scala.compiletime.deferred

  Platform.runLater:
    viewModel.setup()
