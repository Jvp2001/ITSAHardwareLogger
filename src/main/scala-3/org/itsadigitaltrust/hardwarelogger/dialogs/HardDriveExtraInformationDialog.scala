package org.itsadigitaltrust.hardwarelogger.dialogs

import org.itsadigitaltrust.hardwarelogger.models.HardDriveModel

import org.scalafx.extras.generic_pane.GenericDialogFX
import scalafx.scene.control.Label
import org.itsadigitaltrust.hardwarelogger.core.ui.*

import scalafx.scene.text.Text

private[dialogs] class HardDriveExtraInformationDialog(model: HardDriveModel) extends GenericDialogFX("Extra Information"):
  override val header: String = s"${model.serial}'s Information"
  private var gridPane: Option[GridPane] = None
  private val descriptionContainer =  new StackPane:
    private val container = new VBox(20):
      prefWidth = 400
      prefHeight = 150
      private def addHDSentinelText(text: String): Label =
        val label = new Label(text):
          styleClass ++= List("hdsentinel-text", "value-label")
          wrapText = true
        children += label
        label
      end addHDSentinelText
      addHDSentinelText(model.description)
      addHDSentinelText(model.actions)
    end container
    children += container
    styleClass ++= List("hdsentinel-background")
  end descriptionContainer

  private def createHDSentinelText(text: String): Label =
    new Label(text):
      styleClass ++= List("hdsentinel-text", "value-label")

  def addHDSentinelText(label: String, text: String): Label =
    val hdSentinelText = createHDSentinelText(text)
    addNode(label, hdSentinelText)
    hdSentinelText

  private final val commonStyleSheet = "org/itsadigitaltrust/hardwarelogger/stylesheets/extrainfodialog.css"




  override def addNode(node: Node): Unit =
    super.addNode(node)
    gridPane = Option(node.parent.value.parentProperty().getValue.asInstanceOf[GridPane])
    node.parent.value.stylesheets += commonStyleSheet

  addHDSentinelText("Current Temperature:", model.currentTemperature)
  addHDSentinelText("Maximum Temperature:", model.maximumTemperature)
  addNode(descriptionContainer)
  addHDSentinelText("Power on time:",model.powerOnTime)
  addHDSentinelText("Estimated remaining lifetime:", model.estimatedRemainingLifetime)


  gridPane.foreach: gp =>
    gp.vgap = 20
    gp.stylesheets += commonStyleSheet
    gp.children.map(jfxNode2sfx).map:
      case label: Label =>
        label.stylesheets += commonStyleSheet
        label.font = Font(16D)

end HardDriveExtraInformationDialog

extension(sd: ShowDialogs)
  def showHardDriveExtraInfoDialog(model: HardDriveModel): Unit =
    val dialog = new HardDriveExtraInformationDialog(model)
    sd.showGenericDialog[HardDriveExtraInformationDialog, Unit](dialog)(_ => None)


