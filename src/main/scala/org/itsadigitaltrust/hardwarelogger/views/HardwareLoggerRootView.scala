package org.itsadigitaltrust.hardwarelogger.views


import org.itsadigitaltrust.hardwarelogger.core.ui.*
import org.itsadigitaltrust.hardwarelogger.viewmodels.HardwareLoggerRootViewModel
import org.itsadigitaltrust.hardwarelogger.views.tabs.{HardDrivesTabView, MemoryTabView}
import scalafx.application.Platform
import scalafx.scene.control.TabPane.TabClosingPolicy.Unavailable
import scalafx.scene.input.KeyCode



class HardwareLoggerRootView extends BorderPane with View[HardwareLoggerRootViewModel]:
  override given viewModel: HardwareLoggerRootViewModel = new HardwareLoggerRootViewModel

  stylesheets += "org/itsadigitaltrust/hardwarelogger/stylesheets/common.css"
  minWidth = Double.NegativeInfinity
  minHeight = Double.NegativeInfinity
  maxWidth = Double.NegativeInfinity
  maxHeight = Double.NegativeInfinity
  prefWidth = 600.0
  prefHeight = 400.0

  Platform.runLater:
    viewModel.reload()

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
      createTab("Memory", new MemoryTabView),
      createTab("HDD", new HardDrivesTabView)
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

  private def createTab[N <: Node](title: String, rootContent: N): Tab =
    new Tab:
      text = title
      closable = false
      content = rootContent

  viewModel.idFieldFocusProperty.onChange: (_, oldValue, newValue) =>
    if newValue then
      idTextField.requestFocus()

  idTextField.requestFocus()







//
//
//
//  private val memoryTab: MemoryTabView = new MemoryTabView
//
//  @FXML
//  def reload(event: ActionEvent): Unit =
//    println("Reloading")
//    viewModel.reload()
//
//
//  override def initialize(url: URL, resourceBundle: ResourceBundle): Unit =
//    val tables = Seq(
//      //      "MemoryModel" -> new MemoryTabView(TabTableViewModel[MemoryModel, MemoryTableRowViewModel](DIManager.getHardwareGrabberService, DIManager.getNotificationCentre, hs => hs.getMemory().map(m => new MemoryTableRowViewModel(m))))
//      "MemoryModel" -> memoryTab
//    )
//
//    val tabs = tables.map: (key, value) =>
//      createTabFromTable(value, key)
//    tabs.foreach: tab =>
//      tabPane.getTabs.add(tab)
//
//    reloadButton.setOnAction { (t: ActionEvent) =>
//      reload(t)
//      new Alert(AlertType.INFORMATION, "Test", ButtonType.OK).showAndWait()
//    }
//
//    saveButton.disableProperty().bind(viewModel.validIDProperty)
//    idTextField.textProperty().bindBidirectional(viewModel.idStringProperty)
//    idErrorLabel.textProperty().bind(viewModel.idErrorStringProperty)
//
//    idTextField.requestFocus()
//
//    idTextField.focusedProperty().addListener: change =>
//      if !idTextField.isFocused then
//        viewModel.validateID()
//
//    idTextField.setOnKeyPressed: event =>
//      event.getCode match
//        case KeyCode.ENTER => viewModel.save()
//        case _ => ()
//
//
//  def createTabFromTable[M, T <: TableRowViewModel[M]](table: TabTableView[M, T], name: String): Tab =
//    val tab = new Tab(name)
//    tab.setClosable(false)
//    tab.setContent(table)
//    tab

//
//class RootView extends BorderPane:
//  minWidth = Double.NegativeInfinity
//  minHeight = Double.NegativeInfinity
//  maxWidth = Double.NegativeInfinity
//  maxHeight = Double.NegativeInfinity
//  prefHeight = 400D
//  prefHeight = 600D
//
//  //  minWidth = Double.MaxValue
//  //  minHeight = Double.MaxValue
//  /*
//   new HBox:
//          alignment = Pos.Center
//          children = List(
//            new Region:
//              minHeight = 200
//              minWidth = 200
//              hgrow = Priority.Always,
//            new Button:
//              text = "Reload"
//              alignment = Pos.Center
//              id = "reloadButton"
//            //              onAction = _ => reload()
//              margin = Insets(0, 20, 0, 0),
//            new Button:
//              text = "Save"
//              alignment = Pos.Center
//              id = "saveButton"
//              margin = Insets(0, 10, 0, 0)
//   */
//
//  top = new HBox:
//    alignment = CenterLeft
//    prefWidth = 100D
//    prefHeight = 200D
//    spacing = 10D
//
//    private val label = new Label:
//      text = "ID"
//      textAlignment = TextAlignment.Center
//      hgrow = Always
//      opaqueInsets = Insets(10D)
//      padding = Insets(0, 0, 10, 0)
//
//    private val textField = new TextField:
//      hgrow = Always
//      margin = Insets(0, 0, 0, 10D)
//      text.onChange((value, newValue, oldValue)  =>
//        println(newValue)
//      )
//
//
//
//    children ++= Seq(label, textField)
//
//
//  center = new VBox:
//    prefHeight = Double.PositiveInfinity
//    prefWidth = Double.PositiveInfinity
//    alignment = Center
//
//    private val tabPane = new TabPane:
//      prefWidth = 200D
//      prefHeight = 200D
//      tabClosingPolicy = Unavailable
//      alignment = Center
//
//    private val hbox = new HBox:
//      prefWidth = 200D
//      prefHeight = 100D
//
//      private val region = new Region:
//        prefWidth = 200D
//        prefHeight = 200D
//        hgrow = Always
//
//      private def createButton(buttonText: String, clickAction: () => Unit = () => {}): Button =
//        new Button:
//          alignment = Center
//          text = buttonText
//          onAction = event =>
//            clickAction()
//
//      private val reloadButton = createButton("reload")
//      private val saveButton = createButton("save")
//
//      children ++= Seq(region, reloadButton, saveButton)
//    end hbox
//    children ++= Seq(tabPane, hbox)
//
//
//
//
//
//
//
////  top = new TextField():
////    promptText = "ID"
////    minWidth = Region.USE_PREF_SIZE
////    minHeight = Region.USE_PREF_SIZE
////    prefWidth =  Double.MaxValue
////    prefHeight = 50D
////  top = new HBox:
////    minWidth = Double.MaxValue
////    alignment = Pos.CenterLeft
////    spacing = 10
////    children = List(
////      new Label:
////        text = "ID"
////
////        alignmentInParent = Pos.Center
////        padding = Insets(0, 0, 0, 10),
////      new TextField:
////        hgrow = Priority.Always
////        id = "idTextField"
////        margin = Insets(0, 10, 0, 0)
////    )
//
////    center = new VBox:
////      vgrow = Priority.Always
////      hgrow = Priority.Always
////      children = List(
////        new TabsView:
////          //        prefHeight = 200
////          //        prefWidth = 200
////          tabClosingPolicy = TabPane.TabClosingPolicy.Unavailable
////          )
//







