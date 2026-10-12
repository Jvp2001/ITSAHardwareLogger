package org.itsadigitaltrust.hardwarelogger.viewmodels.tabs

import org.itsadigitaltrust.common.Operators.?
import org.itsadigitaltrust.hardwarelogger.contextbasedemnubar.{KeyShortcut, MenuActionDefinition, MenuContributor, MenuElement, MenuManagerService, MenuSeparatorDefinition}
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import org.itsadigitaltrust.hardwarelogger.delegates.{ClickContext, ProgramMode, TableDelegate, TableEvent}
import org.itsadigitaltrust.hardwarelogger.models.HLModel.HardDriveModel
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.{HardDriveIDsChangedEvent, NotificationCentre}
import org.itsadigitaltrust.hardwarelogger.services.{HLDatabaseService, HardwareGrabberService, HardwareIDValidationService}
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.HardDriveTableRowViewModel
import org.scalafx.extras.generic_pane.GenericDialogFX
import ox.either
import scalafx.beans.property.{BooleanProperty, ObjectProperty}
import scalafx.scene.control.Alert.AlertType.Information
import scalafx.scene.control.TextArea

import scala.compiletime.summonFrom
import scala.reflect.Selectable.reflectiveSelectable
import scala.util.boundary
import scala.util.boundary.break


trait HardDrivesHardModeTestingHelperViewModel  (using hardwareGrabber: HardwareGrabberService, applicationState: HardwareLoggerApplicationState, hardwareIDValidationService: HardwareIDValidationService) extends MenuContributor:
  this: HardDrivesTabViewModel =>

  val xmlTextProperty = StringProperty("")
  lazy val importHardDriveDialog =
    val dialog = GenericDialogFX("Import Hard Drive")
    dialog.addNode("XML", new TextArea:
      text <==> xmlTextProperty
    )
    dialog

  def showImportDialog(): Unit =
    import ox.either.orThrow
    importHardDriveDialog.showDialog()
    if importHardDriveDialog.wasOKed then
      ox.supervised:
        log.info("Showing import dialog")
        val drives = hardwareGrabber.loadHardDrives(xmlTextProperty()).orThrow
        applicationState.addDrives(drives*)
  end showImportDialog

  override def getContributedActions: List[MenuElement] =

    MenuSeparatorDefinition("File") :: MenuActionDefinition("File", "_Import", "import", KeyShortcut("I", "Ctrl", "Alt").? ) :: List()

  /**
   * The structural entry point handler where raw user click events
   * are routed straight back into the business logic.
   *
   * @param actionId The target identifier tracking what action string fired
   */
  override def handleMenuAction(actionId: String): Unit =
    actionId match
      case "import" => showImportDialog()
      case _ => ()
end HardDrivesHardModeTestingHelperViewModel


final class HardDrivesTabViewModel(using databaseService: HLDatabaseService, hardwareIDValidationService: HardwareIDValidationService)(using HardwareGrabberService, HardwareLoggerApplicationState, MenuManagerService)
  extends TabTableViewModel(HardDriveTableRowViewModel.apply, _.drives) with TableDelegate[HardDriveTableRowViewModel] with HardDrivesHardModeTestingHelperViewModel:
  private final val unhealthyDriveClass = "unhealthy-drive"

  val editIDColumn = BooleanProperty(false)
  // When its value is changed, it will trigger the table view to edit this cell
  val editingCellProperty: ObjectProperty[Option[(row: Int, column: Int)]] = ObjectProperty(None)
  
  val editableProperty: BooleanProperty = BooleanProperty(false)
  
  var moreInfoDisabledProperty: BooleanProperty = BooleanProperty(false)
  editableProperty <== ProgramMode.modeProperty.isEqualTo("HardDrive")
  
  private var shouldAddWipingRecords = false

  override lazy val delegate: Option[TableDelegate[HardDriveTableRowViewModel]] = Some(this)

  override val cellSelectionEnabled: BooleanProperty = ProgramMode.isHardDriveMode

  override def onUpdateItem(event: TableEvent.UpdateItemEvent[HardDriveTableRowViewModel]): Unit =
    val row = event.item
    moreInfoDisabledProperty.value = row.isEmpty
    event.styleClasses += row.map: rowModel =>
      rowModel.model.`type` match
        case "SSD" if rowModel.model.health.toByte < 50 => unhealthyDriveClass
        case "HDD" | "HHD" if rowModel.model.health.toByte < 100 => unhealthyDriveClass
        case _ => ""
    .get
  end onUpdateItem



  override def onRowKeyboardKeyReleased(event: TableEvent.KeyPressedEvent[HardDriveTableRowViewModel]): Unit =
    event.key.toLowerCase match
      case "i" =>
        event.item.foreach(showExtraInfo)
      case _ => ()

  end onRowKeyboardKeyReleased

  def showExtraInfo(r: HardDriveTableRowViewModel): Unit =
    Dialogs.showHardDriveExtraInfoDialog(r.model)

  override def onRowClicked(event: TableEvent.RowClickedEvent[HardDriveTableRowViewModel]): Unit =
    event.click match
      case ClickContext.Primary if event.times == 2 =>
        event.item.foreach: r =>
          if !editableProperty() then
            showExtraInfo(r)
          else
            ()
      case _ => ()

  end onRowClicked

  override def onSelected(selectedRow: Option[HardDriveTableRowViewModel]): Unit =
    moreInfoDisabledProperty.value = selectedRow.isEmpty

  override def onCellValueChanged(event: TableEvent.CellValueChanged[HardDriveTableRowViewModel]): Unit =
    event match
      case TableEvent.CellValueChanged("ID", item, newValue, _) if newValue.isDefined && item.isDefined =>
        item.foreach(_.hasAValidIDProperty() = hardwareIDValidationService(newValue.get.toString).isRight)

        item.get.idProperty() = newValue.get.toString
        NotificationCentre.publish(HardDriveIDsChangedEvent)
  end onCellValueChanged

  override def onProgramModeChanged(mode: ProgramMode): Either[Error, Unit] =
    editIDColumn() = ProgramMode.isInHardDriveMode
    Right(())

  def isCellEditable(row: HardDriveTableRowViewModel | Null): Boolean =
    row != null && editableProperty()

  private def showDrivesNotWipedAlert(notWipedDrives: Seq[HardDriveModel]) =
    boundary:

      var serials =
        if ProgramMode.isInNormalMode then
          notWipedDrives.map(_.serial).mkString(", ")
        else
          notWipedDrives.map(_.itsaID).mkString(", ")
      end serials
      serials = serials.patch(serials.lastIndexOf(", "), " & ", 0)
      serials = if serials.split(", ").length == 2 then serials.replaceFirst(", ", "") else serials
      serials = if serials.trim.startsWith("& ") then serials.replaceFirst("& ", "") else serials

      val word = serials.length match
        case 0 => boundary.break()
        case 1 => "drive"
        case _ => "drives"


      new Alert(Information, s"The $word '$serials' have not been logged. Do you want to log them now?", ButtonType.Yes, ButtonType.No):
        headerText = "No Wiping Records Found"
        showAndWait() match
          case Some(ButtonType.Yes) => shouldAddWipingRecords = true
          case _ => ()
  end showDrivesNotWipedAlert


end HardDrivesTabViewModel





