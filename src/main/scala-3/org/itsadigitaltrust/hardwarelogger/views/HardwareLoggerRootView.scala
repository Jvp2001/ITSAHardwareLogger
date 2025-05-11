package org.itsadigitaltrust.hardwarelogger.views


import org.itsadigitaltrust.hardwarelogger.core.ui.*
import org.itsadigitaltrust.hardwarelogger.delegates.{ProgramMode, ProgramModeChangedDelegate, TabDelegate}
import org.itsadigitaltrust.hardwarelogger.viewmodels.{HardwareLoggerRootViewModel, TableRowViewModel, ViewModel}
import org.itsadigitaltrust.hardwarelogger.views.tabs.{GeneralInfoTabView, HardDrivesTabView, MediaTabView, MemoryTabView, ProcessorTabView, TabTableView}
import scalafx.application.Platform
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.{CheckMenuItem, Menu, MenuBar, MenuItem}
import scalafx.scene.control.TabPane.TabClosingPolicy.Unavailable
import scalafx.scene.input.KeyCode


class HardwareLoggerRootView extends BorderPane with View[HardwareLoggerRootViewModel] with ProgramModeChangedDelegate:
  root =>
  override given viewModel: HardwareLoggerRootViewModel = new HardwareLoggerRootViewModel

  private val tabs = ObservableBuffer[Tab]()
  stylesheets += "org/itsadigitaltrust/hardwarelogger/stylesheets/common.css"
  Seq(minWidth, minHeight, maxWidth, maxHeight).map(_.value = Double.NegativeInfinity)
  //  minWidth = Double.NegativeInfinity
  //  minHeight = Double.NegativeInfinity
  //  maxWidth = Double.NegativeInfinity
  //  maxHeight = Double.NegativeInfinity
  prefWidth = 600.0
  prefHeight = 400.0


  protected val menuBar = new MenuBar:
    menus += new Menu("_View"):
      // Changes the program's mode
      items ++= Seq(
        new Menu("Mode"):
          items ++= Seq(
            new CheckMenuItem("Normal"):
              onAction = _ => ProgramMode.mode = "Normal"
              selected <==> ProgramMode.isModeNormal
            ,
            new CheckMenuItem("HardDrive"):
              onAction = _ =>
                ProgramMode.mode = "HardDrive"
                selected <==> ProgramMode.isHardDriveMode
            ,
          )

      )

  top = menuBar
  private val contentBorderPane = new BorderPane()
  center = contentBorderPane

  private val idLabel = new Label:
    text = "ID"
    minWidth = 0.0
    textAlignment = TextAlignment.Center
    hgrow = Always
    margin = Insets(0, 0, 0, 10.0)

  private val idTextField = new TextField:
    hgrow = Always
    margin = Insets(0, 10.0, 0, 0)
    text <==> viewModel.idStringProperty
    onAction = _ =>
      viewModel.save()


  private val idErrorLabel = new Label:
    styleClass += "error"
    padding = Insets(0, 0, 0, 35.0)
    margin = Insets(0, 0, 5.0, 0)
    text <== viewModel.idErrorStringProperty


  private val idContainer = new HBox:
    vgrow = Always
    margin = Insets(5.0, 0.0, 0.0, 0.0)
    spacing = 10.0
    children ++= Seq(idLabel, idTextField)
    visible <== viewModel.isInNormalMode

   
  val topContainer = new VBox:
    private val region = new Region:
      maxWidth = 10.0
      visible <== viewModel.isInNormalMode
    alignment = CenterLeft
    prefHeight = 50.0
    prefWidth = 200.0
    spacing = 10.0
    children ++= Seq(region, idContainer, idErrorLabel)
    visible <== viewModel.isInNormalMode
  end topContainer
  viewModel.isInNormalMode.onChange: (op, oldValue, newValue) =>
    contentBorderPane.top = if newValue then
      topContainer
    else null
  private val tabPane = new TabPane:
    prefHeight = prefWidth.get
    prefWidth = 200.0
    tabClosingPolicy = Unavailable
    alignmentInParent = Pos.Center
    vgrow = Always


  private val reloadButton = new Button:
    text = "Reload"
    onAction = _ => viewModel.reload()
    alignment = Center
    margin = Insets(0, 20.0, 0, 0)


  private val saveButton = new Button:
    text = "Save"
    onAction = _ => viewModel.save()
    alignment = Center
    margin = Insets(0, 10.0, 0, 0)

    disable <== viewModel.validIDProperty


  private val centerButtonsContainer = new HBox:
    alignment = Center
    prefWidth = 200.0
    prefHeight = 100.0
    children += new Region:
      prefWidth = 200.0
      prefHeight = prefWidth.get
      hgrow = Always

    children ++= Seq(reloadButton, saveButton)

  contentBorderPane.center = new VBox:
    BorderPane.setAlignment(this, Center)
    children ++= Seq(tabPane, centerButtonsContainer)


  private def createTab(title: String, rootContent: Node): Tab =
    val tab: Tab = new Tab:

      text = title
      closable = false
      content = rootContent
      onSelectionChanged = _ =>
        rootContent match
          case tabDelegate: TabDelegate => tabDelegate.onSelected(this)
          case _ => ()
    tab
  end createTab

  viewModel.idFieldFocusProperty.onChange: (_, oldValue, newValue) =>
    if newValue then
      idTextField.requestFocus()

  idTextField.requestFocus()

  onProgramModeChanged(ProgramMode.mode)

  override def onProgramModeChanged(mode: ProgramMode): Unit =
    mode match
      case "HardDrive" =>
        tabPane.tabs = Seq(
          createTab("HDD", new HardDrivesTabView)
        )
        ProgramMode.isHardDriveMode.value = true
        ProgramMode.isModeNormal.value = false


      case "Normal" =>
        tabPane.tabs = Seq(
          createTab(" General", new GeneralInfoTabView),
          createTab("Memory", new MemoryTabView),
          createTab("Processor", new ProcessorTabView),
          createTab("HDD", new HardDrivesTabView),
          createTab("Media", new MediaTabView)
        )
        ProgramMode.isHardDriveMode.value = false
        ProgramMode.isModeNormal.value = true









