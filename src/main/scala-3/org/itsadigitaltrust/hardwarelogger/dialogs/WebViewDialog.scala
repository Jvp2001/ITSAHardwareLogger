package org.itsadigitaltrust.hardwarelogger.dialogs
import org.itsadigitaltrust.common.Operators.??

import org.itsadigitaltrust.hardwarelogger.core.ui.*

import scalafx.scene.control.{Dialog, DialogPane}
import scalafx.scene.web.WebEngine
import scalafx.stage.StageStyle.Decorated
class WebViewDialog(_url: String, owner: Option[Window] = None) extends Dialog:
  initOwner(owner ?? null)
  initStyle(Decorated)

  val url: StringProperty = StringProperty(_url)
  url.onChange((_, _, newValue) => webView.getEngine.load(newValue))
  val webView = new scalafx.scene.web.WebView()
  webView.getEngine.load(url.value)
  title <== webView.getEngine.title
  dialogPane = new DialogPane:
    content = webView
    buttonTypes ++= Seq(ButtonType.Close)
  resizable = true
 
end WebViewDialog





