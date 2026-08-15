package org.itsadigitaltrust.hardwarelogger.views.tabs

import org.itsadigitaltrust.common.Operators.{in, |>}
import org.itsadigitaltrust.hardwarelogger.core.ui.{eventClosureWrapperWithParam, *}
import scalafx.Includes.*
import org.itsadigitaltrust.hardwarelogger.delegates.{TabDelegate, TableRowDelegate}
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.TableRowViewModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.TabTableViewModel
import org.itsadigitaltrust.hardwarelogger.views.Reloadable
import javafx.beans.value
import javafx.beans.value.ChangeListener
import javafx.event.EventHandler
import scalafx.beans.value.ObservableValue

import scalafx.scene.control.{TableColumn, TableRow, TableView}
import javafx.scene.control as jfxsc
import javafx.event as jfxe
import javafx.scene.input as jfxsi
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import scalafx.event.EventType
import scalafx.event.subscriptions.Subscription
import scalafx.scene.Cursor
import scalafx.scene.input.KeyEvent

import scala.ref.WeakReference
import scala.reflect.ClassTag


private class TableTabRow[R <: Object](val showHandCursorOnHover: Boolean)(using rowDelegate: Option[TableRowDelegate[R]]) extends jfxsc.TableRow[R]:




  private val onMouseButtonEventHandler = (e: jfxsi.MouseEvent) =>
    if e.getClickCount == 2 then
      rowDelegate.map: r =>
        val item1 = getItem
        r.onRowDoubleClicked(e.getButton, Option(item1))

  setOnMouseClicked( event => onMouseButtonEventHandler(event))


  hoverProperty().addListener: (_, _, newValue) =>
    if showHandCursorOnHover && newValue then
      setCursor(Cursor.Hand)
    else
      setCursor(Cursor.Default)

  override def updateItem(item: R, empty: Boolean): Unit =
    super.updateItem(item, empty)
    if empty || item == null then
      setGraphic(null)
    else if !empty && item != null && rowDelegate.isDefined then
      rowDelegate.get.onUpdateItem(Option(item), this)
  end updateItem

  setOnMouseClicked: event =>
    if event.getClickCount == 2 then
      if rowDelegate.isDefined then
        rowDelegate.get.onRowDoubleClicked(event.getButton, Option(getItem))



  override def updateSelected(b: Boolean): Unit =
    super.updateSelected(b)

    val item = Option(getItem)
    if item.isDefined then
      if rowDelegate.isDefined then
        rowDelegate.get.onSelected(item)
    
  end updateSelected


  addEventFilter[javafx.scene.input.KeyEvent](javafx.scene.input.KeyEvent.KEY_RELEASED, e => rowDelegate.map(_.onRowKeyboardKeyReleased(e, Option(getItem))))

end TableTabRow




abstract class TabTableView[M <: HLModel : ClassTag, T <: TableRowViewModel[M]](using applicationState: HardwareLoggerApplicationState)(using hardwareGrabberService: HardwareGrabberService) extends TableView[T] :

  import TabTableView.*


//
//  final val rowClickedByKeyboardKeys = Seq(KeyCode.Space)
//  val rowDelegate: WeakReference[TableRowDelegate[T]] = WeakReference(null)
//  val showHandCursorOnHover: Boolean = false
//  val reordableColumns: Boolean = false
//  val nonEditableSelectionConfirmationPressed = (event: KeyEvent) =>
//    if event.code in rowClickedByKeyboardKeys then
//      onValidRowDelegate(_.foreach(_.onRowDoubleClicked(MouseButton.Primary, Option(getSelectedItem))))
//    end if
//
//  end nonEditableSelectionConfirmationPressed
//
//  filterEvent(KeyEvent.Any): (event: KeyEvent) =>
//    event.eventType match
//      case _: EventType[KeyEvent.KeyPressed.type] if !editable.value =>
//        nonEditableSelectionConfirmationPressed(event)
//      case et: EventType[KeyEvent.KeyReleased.type] if !editable.value =>
//        onValidRowDelegate(_.foreach(_.onRowKeyboardKeyReleased(event.code, Option(getSelectedItem))))
//
//
//  editable = false
//  delegate.getSelectionModel.cellSelectionEnabled <== editable
//
//  protected def onValidRowDelegate(f: TableRowDelegate[T] => Unit): Unit =
//    rowDelegate.get.match
//      case Some(delegate) => f(delegate)
//      case None => ()
//



  class TableTabColumn[P] extends TableColumn[T, P]

//  rowFactory = _ => new TableTabRow[T](showHandCursorOnHover)(using rowDelegate.get)

//  vgrow = Always

  protected def createAndAddColumn[P](name: String, minimumWidth: Double = 50D )(f: T => ObservableValue[P, P]): TableColumn[T, P] =
    val column = new TableColumn[T, P]():
      text = name
      minWidth = minimumWidth
      cellValueFactory = _.value |> f
      sortable = false
      delegate.setReorderable(false)
    columns += column
    column

//  selectionModel.apply().selectedItem.onChange: (op, newValue, oldValue) =>
//    onValidRowDelegate(_.foreach(_.onSelected(selectionModel.apply().getSelectedItem |> Option[T])))


  

end TabTableView

object TabTableView:
  private[TabTableView] var triggerRowClicked: Boolean = false
