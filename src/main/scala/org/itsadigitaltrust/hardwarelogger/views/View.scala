package org.itsadigitaltrust.hardwarelogger.views

import org.itsadigitaltrust.hardwarelogger.services.ServicesModule
import org.itsadigitaltrust.hardwarelogger.viewmodels.ViewModel

trait View[VM <: ViewModel]:
  given viewModel: VM = scala.compiletime.deferred

