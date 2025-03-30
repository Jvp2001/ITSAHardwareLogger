package org.itsadigitaltrust.hardwarelogger.views

import org.itsadigitaltrust.hardwarelogger.services.ServicesModule
import org.itsadigitaltrust.hardwarelogger.viewmodels.ViewModel
import scalafx.application.Platform

trait View[VM <: ViewModel]:
  given viewModel: VM = scala.compiletime.deferred

  Platform.runLater:
    viewModel.setup()
