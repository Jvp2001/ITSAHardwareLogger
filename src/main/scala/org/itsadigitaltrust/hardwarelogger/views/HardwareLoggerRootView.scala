package org.itsadigitaltrust.hardwarelogger.views

import com.softwaremill.macwire.*
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.{FXML, FXMLLoader, Initializable}
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.{Alert, Button, ButtonType, Tab, TabPane}
import javafx.scene.layout.BorderPane
import org.itsadigitaltrust.hardwarelogger.core.DIManager
import org.itsadigitaltrust.hardwarelogger.models.Memory
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.MemoryTableRowViewModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.{HardwareLoggerRootViewModel, TabTableViewModel, TableRowViewModel}
import org.itsadigitaltrust.hardwarelogger.views.tabs.{MemoryTabView, TabTableView}

import java.net.URL
import java.util.ResourceBundle
import scala.compiletime.uninitialized



class HardwareLoggerRootView(val viewModel: HardwareLoggerRootViewModel) extends Initializable:

  @FXML
  private var tabPane: TabPane = uninitialized

  @FXML var reloadButton: Button = uninitialized
  @FXML var saveButton: Button = uninitialized



  @FXML
  def reload(event: ActionEvent): Unit =
    println("Reloading")
    viewModel.reload()


  override def initialize(url: URL, resourceBundle: ResourceBundle): Unit =
    val tables = Seq(
      "Memory" -> new MemoryTabView(TabTableViewModel[Memory, MemoryTableRowViewModel](DIManager.getHardwareGrabberService, DIManager.getNotificationCentre, hs => hs.getMemory().map(m => new MemoryTableRowViewModel(m)))
    ))
    val tabs = tables.map: (key, value) =>
      createTabFromTable(value, key)
    tabs.foreach: tab =>
      tabPane.getTabs.add(tab)

    reloadButton.setOnAction { (t: ActionEvent) =>
      reload(t)
      new Alert(AlertType.INFORMATION, "Test", ButtonType.OK).showAndWait()
    }


  def createTabFromTable[M, T <: TableRowViewModel[M]](table: TabTableView[M, T], name: String): Tab =
    val tab = new Tab(name)
    tab.setClosable(false)
    tab.setContent( table)
    tab

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







