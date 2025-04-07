package org.itsadigitaltrust.hardwarelogger.views


import org.itsadigitaltrust.hardwarelogger.core.ui.*
import org.itsadigitaltrust.hardwarelogger.delegates.TabDelegate
import org.itsadigitaltrust.hardwarelogger.viewmodels.{HardwareLoggerRootViewModel, TableRowViewModel, ViewModel}
import org.itsadigitaltrust.hardwarelogger.views.tabs.{GeneralInfoTabView, HardDrivesTabView, MediaTabView, MemoryTabView, ProcessorTabView, TabTableView}
import scalafx.application.Platform
import scalafx.scene.control.TabPane.TabClosingPolicy.Unavailable
import scalafx.scene.input.KeyCode



class HardwareLoggerRootView extends BorderPane with View[HardwareLoggerRootViewModel]:
  override given viewModel: HardwareLoggerRootViewModel = new HardwareLoggerRootViewModel

  stylesheets +=  "org/itsadigitaltrust/hardwarelogger/stylesheets/common.css"
  Seq(minWidth,minHeight, maxWidth, maxHeight).map(_.value = Double.NegativeInfinity)
//  minWidth = Double.NegativeInfinity
//  minHeight = Double.NegativeInfinity
//  maxWidth = Double.NegativeInfinity
//  maxHeight = Double.NegativeInfinity
  prefWidth = 600.0
  prefHeight = 400.0



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
    focused.onChange: (change, oldValue, focused) =>
      if !focused then
        viewModel.validateID()
    onAction = _ =>
      viewModel.save()





  private val idErrorLabel = new Label:
    styleClass += "error"
    padding = Insets(0, 0, 0, 35.0)
    margin =  Insets(0, 0, 5.0, 0)
    text <== viewModel.idErrorStringProperty


  private val idContainer = new HBox:
    vgrow = Always
    margin = Insets(5.0, 0.0, 0.0, 0.0)
    spacing = 10.0
    children ++= Seq(idLabel, idTextField)

  top = new VBox:
    private val region = new Region:
      maxWidth = 10.0
    alignment = CenterLeft
    prefHeight = 50.0
    prefWidth = 200.0
    spacing = 10.0
    children ++= Seq(region, idContainer, idErrorLabel)

  private val tabPane = new TabPane:
    prefHeight = prefWidth.get
    prefWidth = 200.0
    tabClosingPolicy = Unavailable
    alignmentInParent = Pos.Center
    vgrow = Always
    tabs ++= Seq(
      createTab("General", new GeneralInfoTabView),
      createTab("Memory", new MemoryTabView),
      createTab("Processor", new ProcessorTabView),
      createTab("HDD", new HardDrivesTabView),
      createTab("Media", new MediaTabView),
    )

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

  center = new VBox:
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










