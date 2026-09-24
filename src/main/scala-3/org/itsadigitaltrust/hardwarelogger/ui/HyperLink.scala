package org.itsadigitaltrust.hardwarelogger.ui

import org.itsadigitaltrust.common.Operators.{in, |>}
import org.itsadigitaltrust.hardwarelogger.core.HWLoggerShowMessage
import scalafx.Includes.*
import scalafx.stage.{Stage, Window}
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import scalafx.Includes.*
import scalafx.scene.control.Hyperlink

class ConfirmHyperLink extends Hyperlink, HWLoggerShowMessage:
  protected var skipConfirmation: Boolean = false
  onKeyTyped  = e =>
    if e.isAltDown then
      skipConfirmation = true

  onKeyReleased = _ =>
    skipConfirmation = false
  onAction = _ =>
    if !skipConfirmation then
      showConfirmation("Confirm?", "Do you want open the link?", "") match
        case true =>
          dialogDismissed(Some(true)) // User confirmed the action
        case false => dialogDismissed(Some(false))
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

  def showConfirmationAlert()(using showMessage: HWLoggerShowMessage): Boolean =
    showMessage.showConfirmation("Open Web Site", s"Do you want to open the web site $url?")

  override def dialogDismissed(result: Option[Boolean]): Unit =
    result match
      case Some(true) | None =>
      // No confirmation dialog was shown; proceed with the action
        val dialog = new Dialogs.WebViewDialog(url)
        dialog.showAndWait() match
          case Some(value) => dialog.close()
          case None => ()


      case _ => ()
end WebSiteHyperLink
