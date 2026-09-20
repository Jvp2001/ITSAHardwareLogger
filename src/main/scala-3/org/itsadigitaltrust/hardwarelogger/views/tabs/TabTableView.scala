package org.itsadigitaltrust.hardwarelogger.views.tabs

import org.itsadigitaltrust.common.Operators.{in, |>}
import scalafx.Includes.*
import org.itsadigitaltrust.hardwarelogger.delegates.{TabDelegate, TableRowDelegate}
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.TableRowViewModel
import javafx.beans.value
import javafx.beans.value.ChangeListener
import javafx.event.EventHandler
import scalafx.beans.value.ObservableValue
import scalafx.scene.control.{TableCell, TableColumn, TableRow, TableView}
import javafx.scene.control as jfxsc
import javafx.event as jfxe
import javafx.scene.input as jfxsi
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.itsadigitaltrust.hardwarelogger.viewmodels.BaseViewModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.TabTableViewModel
import org.itsadigitaltrust.hardwarelogger.views.View
import org.itsadigitaltrust.hardwarelogger.core.ui.*
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



abstract class TabTableView[Model <: HLModel, RowViewModel <: TableRowViewModel[Model], VM  <: TabTableViewModel[Model, RowViewModel]](using hardwareGrabberService: HardwareGrabberService)(using viewModel: VM) extends TableView[RowViewModel] with View[VM]:

  import TabTableView.*

  given reorderableColumns: Boolean = false

  protected class TabTableColumn[P] extends TableColumn[RowViewModel, P]
  protected class TabTableCell[P] extends jfxsc.TableCell[RowViewModel, P]
  vgrow = Always


  items() = viewModel.rows

  /**
   * Generates a TableColumn with proper ScalaFX Observable bounds.
   *
   * = Why the explicit `.asInstanceOf` type cast is required here: =
   *
   * 1. Java vs. Scala Variance Stance:
   *    Java relies purely on Use-Site Variance (e.g., `ObservableValue<? extends T>`). 
   *    Scala prefers Declaration-Site Variance (`[+T]`). Because JavaFX was built on 
   *    Java's invariant generics foundation, ScalaFX's `cellValueFactory` strictly 
   *    demands a function signature returning an invariant `ObservableValue[P, P]`.
   *
   * 2. The Wildcard/Boxing Impedance Mismatch:
   *    Our function argument `f` yields `ObservableValue[P, ?]`. The wildcard (`?`) 
   *    is required because JavaFX primitives are boxed under the hood (e.g., a 
   *    `DoubleProperty` implements `ObservableValue[Number, Number]`).
   *
   * 3. The Scala 3 Compiler Enforcement:
   *    Because Scala enforces type invariance far more strictly than Java's loose runtime 
   *    type erasure, the compiler refuses to bind an asymmetric wildcard wrapper `[P, ?]`
   *    to a symmetric signature expecting `[P, P]`.
   *
   * Using `.asInstanceOf` safely downcasts the underlying wildcard parameter right 
   * at the assignment boundary. This satisfies Scala's compile-time verification 
   * and safely hands the execution loop over to the underlying JavaFX layout engine.
   */
  protected def createAndAddColumn[P](name: String, minimumWidth: Double = 50D )(f: RowViewModel => ObservableValue[P, ?]): TabTableColumn[P] =
    val column = new TabTableColumn[P]():
      text = name
      minWidth = minimumWidth
      cellValueFactory = cellData =>
        (cellData.value |> f).asInstanceOf[ObservableValue[P, P]]
      sortable = false
      delegate.setReorderable(reorderableColumns)
    columns += column
    column

  //  selectionModel.apply().selectedItem.onChange: (op, newValue, oldValue) =>
//    onValidRowDelegate(_.foreach(_.onSelected(selectionModel.apply().getSelectedItem |> Option[T])))


  

end TabTableView

object TabTableView:
  private[TabTableView] var triggerRowClicked: Boolean = false
