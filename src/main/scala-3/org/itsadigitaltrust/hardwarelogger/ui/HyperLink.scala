package org.itsadigitaltrust.hardwarelogger.ui

import org.itsadigitaltrust.common.Operators.{in, |>}
import scalafx.Includes.*
import scalafx.stage.{Stage, Window}
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import scalafx.Includes.*
import org.itsadigitaltrust.hardwarelogger.dialogs.Dialogs

import scalafx.scene.control.Hyperlink

class ConfirmHyperLink extends Hyperlink:

  protected var skipConfirmation: Boolean = false
  onKeyTyped  = e =>
    if e.isAltDown then
      skipConfirmation = true

  onKeyReleased = _ =>
    skipConfirmation = false


  def showConfirmationAlert(): Option[ButtonType] =
    Dialogs.showConfirmationAlert("", "")

  onAction = _ =>
    if !skipConfirmation then
      showConfirmationAlert() match
        case Some(value) => if value in Seq(ButtonType.Yes, ButtonType.OK) then
          dialogDismissed(Some(true)) // User confirmed the action
        case None => dialogDismissed(Some(false)
        )
    else
      skipConfirmation = false
      dialogDismissed(None) // No confirmation, just proceed with the action


  /**
   *
   * @param result If this is Some(true), the action should proceed, if Some(false), it should not. If it is None, it means the dialog was purposfully not shown, so continue with the action.
   */
  protected def dialogDismissed(result: Option[Boolean]): Unit =
    ()



end ConfirmHyperLink

class WebSiteHyperLink(name: String, url: String) extends ConfirmHyperLink:

  text = if name == "" || name == null then url else name

  override def showConfirmationAlert(): Option[ButtonType] =
    Dialogs.showConfirmationAlert("Open Web Site", s"Do you want to open the web site $url?")

  override def dialogDismissed(result: Option[Boolean]): Unit =
    result match
      case Some(true) | None =>
      // No confirmation dialog was shown; proceed with the action
        val dialog = new org.itsadigitaltrust.hardwarelogger.dialogs.WebViewDialog(url)
        dialog.showAndWait() match
          case Some(value) => dialog.close()
          case None => ()


      case _ => ()
end WebSiteHyperLink
