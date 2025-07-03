package org.itsadigitaltrust.hardwarelogger.views


import org.itsadigitaltrust.common.DoOnce

import jp.uphy.javafx.console
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import org.itsadigitaltrust.hardwarelogger.delegates.{ProgramMode, ProgramModeChangedDelegate, TabDelegate}
import org.itsadigitaltrust.hardwarelogger.viewmodels.{HardwareLoggerRootViewModel, TableRowViewModel, ViewModel}
import org.itsadigitaltrust.hardwarelogger.views.tabs.{GeneralInfoTabView, HardDrivesTabView, MediaTabView, MemoryTabView, ProcessorTabView, TabTableView}

import scalafx.application.Platform
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.{CheckMenuItem, Menu, MenuBar, MenuItem}
import scalafx.scene.control.TabPane.TabClosingPolicy.Unavailable
import scalafx.scene.input.KeyCode
import org.itsadigitaltrust.hardwarelogger.services.given

import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.scene.control

final class HardwareLoggerRootView extends BorderPane with View[HardwareLoggerRootViewModel] with ProgramModeChangedDelegate:
  override given viewModel: HardwareLoggerRootViewModel = new HardwareLoggerRootViewModel




  notificationCentre.addObserver(this)


  stylesheets += "org/itsadigitaltrust/hardwarelogger/stylesheets/common.css"
  //Seq(minWidth, minHeight, maxWidth, maxHeight).map(_.value = Double.NegativeInfinity)



  private given consoleView: ItsaDebugView = new ItsaDebugView(using viewModel.issueReporterService)

  private val viewMenu = new Menu("_View"):
      private val modeMenu = new Menu("Mode"):
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
      end modeMenu
      items += modeMenu

  end viewMenu
  private val menuBar = new MenuBar:
    useSystemMenuBar = true
    private val helpMenu = new Menu("_Help"):
      items ++= Seq(
        new MenuItem("Report"):
          onAction = _ => ItsaDebugView.report(viewModel.issueReporterService.report)
          ,
      )
    menus ++= Seq(
      viewMenu,
      helpMenu
    )
  end menuBar



  top = menuBar
  private val contentBorderPane = new BorderPane():
    alignmentInParent = Center
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

  private val topContainer = new VBox:
    private val region = new Region():
      maxWidth = 10.0
    alignment = CenterLeft
    prefHeight = 50.0
    prefWidth = 200.0
    spacing = 10.0
    children ++= Seq(region, idContainer, idErrorLabel)
  end topContainer
  contentBorderPane.top = topContainer
  private val tabPane = new TabPane:
    prefHeight = prefWidth.get
    prefWidth = 200.0
    tabClosingPolicy = Unavailable
    alignmentInParent = Pos.Center
    vgrow = Always



  private val reconnectButton = new Button:
    text = "Reconnect"
    onAction = _ => viewModel.reconnect()
    alignment = Center
    margin = Insets(0, 20.0, 0, 0)


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
    disable <== !viewModel.validIDProperty

  private val buttonsContainer = new HBox:
    alignment = Center
    prefWidth = 200.0
    prefHeight = 50.0
    children += new Region:
      prefWidth = 200.0
      prefHeight = prefWidth.get
      hgrow = Always
    children ++= Seq(reconnectButton, reloadButton, saveButton)
  end buttonsContainer

  contentBorderPane.center = new VBox:
    BorderPane.setAlignment(this, Center)
    children ++= Seq(buttonsContainer, tabPane)







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


  given itsaID: String = viewModel.idStringProperty.get

  override def onProgramModeChanged(mode: ProgramMode): Unit =
    val hardDrivesTabView = new HardDrivesTabView

    mode match
      case "HardDrive" =>
        tabPane.tabs = Seq(
          createTab("HDD", new HardDrivesTabView(using viewModel.idStringProperty.get))
        )
        ProgramMode.isHardDriveMode.value = true
        ProgramMode.isModeNormal.value = false


      case "Normal" =>


        tabPane.tabs = Seq(
          createTab(" General", new GeneralInfoTabView),
          createTab("Memory", new MemoryTabView),
          createTab("Processor", new ProcessorTabView),
          createTab("HDD", hardDrivesTabView),
          createTab("Media", new MediaTabView)
        )
        ProgramMode.isHardDriveMode.value = false
        ProgramMode.isModeNormal.value = true
    end match
    tabPane.tabs += createTab("Debug",
      consoleView)
    /** The code below is a hacky workaround to get the HDD tab's [[HardDrivesTabView]] table to display its data properly.
     * This is hacky because this should not be needed to get the table view to display its data;
     * also, I am dropping back down to using JavaFX, instead of staying in the ScalaFX universe. */
    val changeListener: ChangeListener[control.Tab] = (_: ObservableValue[? <: control.Tab], oldValue: control.Tab, newValue: control.Tab) =>
      if newValue.textProperty().get().equalsIgnoreCase("HDD") then
        hardDrivesTabView.viewModel.reload()
    tabPane.selectionModel.value.selectedItemProperty().addListener(changeListener)
  end onProgramModeChanged
  
  viewModel.shouldCaretBeAtEnd.onChange: (op, oldValue, newValue) =>
    if newValue then
      idTextField.end()



end HardwareLoggerRootView








