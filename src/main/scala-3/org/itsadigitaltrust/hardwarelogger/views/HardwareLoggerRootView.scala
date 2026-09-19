package org.itsadigitaltrust.hardwarelogger.views

import org.itsadigitaltrust.common.Operators.{??, |>}
import org.itsadigitaltrust.hardwarelogger.core.HWLoggerShowMessage
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import org.itsadigitaltrust.hardwarelogger.delegates.{ProgramMode, TabDelegate}
import org.itsadigitaltrust.hardwarelogger.dialogs.Dialogs
import org.itsadigitaltrust.hardwarelogger.models.HLModel.MemoryModel
import org.itsadigitaltrust.hardwarelogger.services.notificationcentre.NotificationCentre
import org.itsadigitaltrust.hardwarelogger.services.{HLDatabaseService, HardwareGrabberService, HardwareIDValidationService, IssueReporterService}
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.itsadigitaltrust.hardwarelogger.ui.ThemeSelectionMenu
import org.itsadigitaltrust.hardwarelogger.viewmodels.HardwareLoggerRootViewModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.{MemoryTableRowViewModel, TableRowViewModel}
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.{MemoryTabViewModel, TabTableViewModel}
import org.itsadigitaltrust.hardwarelogger.views.tabs.*
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.beans.value.ObservableValue
import scalafx.scene.control.TabPane.TabClosingPolicy.Unavailable
import scalafx.scene.control.{CheckMenuItem, Menu, MenuBar, MenuItem, ProgressBar}
import scalafx.scene.input.{KeyCodeCombination, KeyCombination}

import scala.collection.mutable
import scala.util.Try


class HardwareLoggerRootView(using hardwareGrabberService: HardwareGrabberService, databaseService: HLDatabaseService, applicationState: HardwareLoggerApplicationState)(using HardwareIDValidationService, IssueReporterService)(using viewModel: HardwareLoggerRootViewModel = new HardwareLoggerRootViewModel) extends BorderPane with View(using viewModel) with HWLoggerShowMessage:

  //stylesheets += stylesheet"common.css"
  Seq(minWidth, minHeight, maxWidth, maxHeight).map(_.value = Double.NegativeInfinity)
  
  private type TabsMapKey = "General" | "Memory" | "Processor" | "HDD" | "Media" | "Debug" | "Battery"

    private val generalTab = createTab("General", new GeneralInfoTabView)
    private val memoryTab = createTab("Memory", new MemoryTabView)
    private val processorTab = createTab("Processor", new ProcessorTabView)
    private val hardDrivesTab = createTab("HDD", new HardDrivesTabView)
    private val mediaTab = createTab("Media", new MediaTabView)
    private val debugTab = createTab("Debug", new ItsaDebugView)
    private val batteryTab = createTab("Battery", new BatteryStatusTabView)


  private final val viewMenu = new Menu("_View"):
    private final val modeMenu = new Menu("_Mode"):
      items ++= Seq(
      new CheckMenuItem("Normal"):
        accelerator = new KeyCodeCombination(KeyCode.N, KeyCombination.ShortcutDown, KeyCombination.AltDown)
        onAction = _ =>
            switchMode("Normal")
        selected <==> ProgramMode.isModeNormal
        ,
        new CheckMenuItem("HardDrive"):
          accelerator = new KeyCodeCombination(KeyCode.H, KeyCombination.ShortcutDown, KeyCombination.AltDown)
          onAction = _ =>
            switchMode("HardDrive")
          selected <==> ProgramMode.isHardDriveMode
        ,
      )
    end modeMenu
    private val themeMenu = new ThemeSelectionMenu()
    
    items ++= Seq(modeMenu, themeMenu)

  end viewMenu
  private final val menuBar = new MenuBar:
    useSystemMenuBar = true
    disable <== viewModel.disableNodeWhileTaskRunningProperty
    private final val fileMenu = new Menu("File"):
      items ++= Seq(
        new MenuItem("Quit"):
          accelerator = new KeyCodeCombination(KeyCode.Q, KeyCombination.ShortcutDown)
          onAction = _ => Platform.exit()
      )
    private final val helpMenu = new Menu("_Help"):
      items ++= Seq(
//        new MenuItem("Report"):
//          ()
//        //          onAction = _ => ItsaDebugView.report(viewModel.issueReporterService.report)
//        ,
        new MenuItem("About"):

          onAction = _ => Dialogs.showAboutDialog()
      )
    menus ++= Seq(
      fileMenu,
      viewMenu,
      helpMenu
    )
  end menuBar


  top = menuBar

  private final val idLabel = new Label:
    text = "ID"
    minWidth = 0.0
    textAlignment = TextAlignment.Center
    hgrow = Always
    margin = Insets(0, 0, 0, 10.0)

  private final val idTextField = new TextField:
    hgrow = Always
    margin = Insets(0, 10.0, 0, 0)
    text <==> viewModel.idStringProperty
    onAction = _ =>
      viewModel.save()

  private final val idErrorLabel = new Label:
    styleClass += "error"
    padding = Insets(0, 0, 0, 35.0)
    margin = Insets(0, 0, 5.0, 0)
    text <==> viewModel.idErrorStringProperty


  private final val idContainer = new HBox:
    vgrow = Always
    margin = Insets(5.0, 0.0, 0.0, 0.0)
    spacing = 10.0
    children ++= Seq(idLabel, idTextField)

  private final val topContainer = new VBox:
    disable <== viewModel.disableNodeWhileTaskRunningProperty
    private final val region = new Region():
      maxWidth = 10.0
    alignment = CenterLeft
    prefHeight = 50.0
    prefWidth = 200.0
    spacing = 10.0
    children ++= Seq(region, idContainer, idErrorLabel)
  end topContainer

  private final val tabPane = new TabPane:
    disable <== viewModel.disableNodeWhileTaskRunningProperty
    prefHeight = prefWidth.get
    prefWidth = 200.0
    tabClosingPolicy = Unavailable
    alignmentInParent = Pos.Center
    vgrow = Always
    selectionModel().selectedItemProperty.onChange: (_, _, newTab) =>
      if newTab ne null then
        Platform.runLater:
          newTab match
            case tabDelegate: TabDelegate =>
              tabDelegate.onSelected(newTab)
            case _ => ()

  private final val reconnectButton = new Button:
    text = "Reconnect"
    disable <== viewModel.disableNodeWhileTaskRunningProperty

    onAction = _ => viewModel.reconnect()
    alignment = Center
    margin = Insets(0, 20.0, 0, 0)


  private final val reloadButton = new Button:
    text = "Reload (F5)"
    disable <== viewModel.disableNodeWhileTaskRunningProperty
    onAction = _ =>
      viewModel.reload()
    alignment = Center
    margin = Insets(0, 20.0, 0, 0)


  private final val normalSaveButton = new Button:
    text = "Save"
    onAction = _ => viewModel.save()
    disable <== viewModel.disableNodeWhileTaskRunningProperty

    alignment = Center
    margin = Insets(0, 10.0, 0, 0)
    disable <== !viewModel.validIDProperty

  private final val hardDriveModeSaveButton = new Button:
    text = "Save"
    disable <== viewModel.disableNodeWhileTaskRunningProperty
    onAction = _ => viewModel.save()
    alignment = Center
    margin = Insets(0, 10.0, 0, 0)


  private final val saveButton: Button = normalSaveButton
  private final val rightAlignRegion = new Region:
    prefWidth = 200.0
    prefHeight = prefWidth.get
    hgrow = Always
  private final val buttonsContainer = new HBox:
    alignment = Center
    prefWidth = 200.0
    minHeight = 50.0
    children ++= Seq(rightAlignRegion, reconnectButton, reloadButton, saveButton)
  end buttonsContainer


  private final val contentBorderPane = new BorderPane():
    styleClass += "page"
    alignmentInParent = Center
    top = topContainer
    disable <== viewModel.disableNodeWhileTaskRunningProperty
    center = new VBox:
      prefWidth = 600D
      prefHeight = 600D
      BorderPane.setAlignment(this, Center)

      refreshTabs()

      children ++= Seq(buttonsContainer, tabPane)

  end contentBorderPane

  center = contentBorderPane

  bottom = new HBox(20):
    private final val statusLabel = new Label:
      this.text <== viewModel.taskMessageProperty
      visible <== viewModel.disableNodeWhileTaskRunningProperty || viewModel.taskMessageProperty.isNotEmpty
      styleClass += "status-label"
      maxWidth = Double.PositiveInfinity
      minWidth = 200.0
      alignment = CenterLeft
      padding = Insets(0, 0, 0, 10.0)
    end statusLabel
    private val region = new Region:
      hgrow = Always
    private final val progressBar = new ProgressBar:
      progress <== viewModel.taskProgressProperty
      visible <== viewModel.disableNodeWhileTaskRunningProperty
      minWidth = 200.0
      alignment = CenterRight
      padding = Insets(0, 10.0, 0, 0)
    end progressBar
    visible <== viewModel.disableNodeWhileTaskRunningProperty || viewModel.taskMessageProperty.isNotEmpty
    managed <== this.visible
    children ++= Seq(statusLabel, region, progressBar)


  private def createTab(title: TabsMapKey)(
    rootContent: => Node): Tab =
    createTab(title, rootContent)

  private def createTab(title: TabsMapKey, rootContent: Node): Tab =
    val tab: Tab = new Tab:

      text = title
      closable = false
      content = rootContent
      selected.onChange: (_, _, isNowSelected) =>
        if isNowSelected then
          rootContent match
            case tabDelegate: TabDelegate =>
              tabDelegate.onSelected(this)
            case _ => ()

    tab
  end createTab


  onKeyPressed = event =>
    val code = event.code
    if code == KeyCode.F5 then
      viewModel.reload()
    end if

  viewModel.idFieldFocusProperty.onChange: (_, oldValue, newValue) =>
    if newValue then
      idTextField.requestFocus()

  idTextField.requestFocus()


  private given itsaID: String = viewModel.idStringProperty.get
  


  //  end onProgramModeChanged


  viewModel.shouldCaretBeAtEnd.onChange: (op, oldValue, newValue) =>
    if newValue then
      idTextField.end()

  private def switchMode(newMode: ProgramMode): Unit =
    ProgramMode.mode = newMode
    refreshTabs()
    viewModel.reload()


  private def refreshTabs() =
    tabPane.tabs.clear()
      if ProgramMode.isInHardDriveMode then
        tabPane.tabs.addAll(hardDrivesTab, debugTab)
      else
        tabPane.tabs.addAll(
          generalTab,
          memoryTab,
          processorTab,
          hardDrivesTab,
          mediaTab,
          debugTab,
        )
        
        if viewModel.hasABattery then
          tabPane.tabs.add(batteryTab)
      end if
  //    end if
  end refreshTabs

  switchMode(ProgramMode.mode)
  override protected def parentWindow: Option[Window] = None
end HardwareLoggerRootView








