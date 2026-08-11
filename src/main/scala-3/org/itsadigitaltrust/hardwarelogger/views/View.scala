package org.itsadigitaltrust.hardwarelogger.views

import org.itsadigitaltrust.hardwarelogger.viewmodels.{BaseViewModel, ViewModel}
import scalafx.scene.Node

trait Reloadable:
  def reload(shouldClearData: Boolean): Unit
trait View[VM <: BaseViewModel]:
  this: Node => /** This means only subclasses of [[scalafx.scene.Node]] can implement this trait. */
  given viewModel: VM = scala.compiletime.deferred



//  Platform.runLater:
//    offFX:
//      viewModel.setup()


end View
