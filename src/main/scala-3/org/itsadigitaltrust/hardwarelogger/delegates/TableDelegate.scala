package org.itsadigitaltrust.hardwarelogger.delegates
import scalafx.beans.binding.ObjectBinding
import scalafx.beans.property.{BooleanProperty, ObjectProperty, ReadOnlyObjectProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.{TableRow, TableSelectionModel}
import scalafx.scene.input.MouseButton

into enum ClickContext:
  case Primary, Secondary, Middle, Unknown
object ClickContext:
  given scalafxMouseButtonToClickContext: Conversion[MouseButton, ClickContext] = _.toClickContext
  given javafxMouseButtonToClickContext: Conversion[javafx.scene.input.MouseButton, ClickContext] = _.toClickContext
extension(button: MouseButton)
  def toClickContext: ClickContext = button match
    case MouseButton.Primary => ClickContext.Primary
    case MouseButton.Secondary => ClickContext.Secondary
    case MouseButton.Middle => ClickContext.Middle
    case _ => ClickContext.Unknown
extension (button: javafx.scene.input.MouseButton)
  def toClickContext: ClickContext = button match
    case javafx.scene.input.MouseButton.PRIMARY => ClickContext.Primary
    case javafx.scene.input.MouseButton.SECONDARY => ClickContext.Secondary
    case javafx.scene.input.MouseButton.MIDDLE => ClickContext.Middle
    case _ => ClickContext.Unknown
sealed trait TableEvent[R <: Any]:
  def item: Option[R]

object TableEvent:
  final case class RowClickedEvent[R <: Any](item: Option[R], click: ClickContext, times: Int) extends TableEvent[R]
  final case class KeyPressedEvent[R <: Any](item: Option[R], key: String) extends TableEvent[R]
  final case class EditCommitEvent[R <: Any, V <: Any](item: Option[R], field: String, newValue: V) extends TableEvent[R]
  final case class UpdateItemEvent[R <: Any](item: Option[R], styleClasses: ObservableBuffer[String]) extends TableEvent[R]
  final case class CellValueChanged[R <: Any](columnName: String, item: Option[R], value: Option[Any], styleClasses: ObservableBuffer[String]) extends TableEvent[R]
trait TableDelegate[R <: Any]:
  val showHandCursorOnHover: Boolean = true

  val cellSelectionEnabled: BooleanProperty = BooleanProperty(true)

  def onSelected(row: Option[R]): Unit = ()
  // apply styles to the entire row
  def onUpdateItem(event: TableEvent.UpdateItemEvent[R]): Unit = ()


  def onRowClicked(event: TableEvent.RowClickedEvent[R]): Unit = ()
  def onRowKeyboardKeyReleased(event: TableEvent.KeyPressedEvent[R]): Unit = ()

  def onCellValueChanged(event: TableEvent.CellValueChanged[R]): Unit = ()
