package org.itsadigitaltrust.hardwarelogger.views.tabs

import javafx.scene.control
import org.itsadigitaltrust.common.Operators.{??, |>, ||>}
import org.itsadigitaltrust.hardwarelogger.models.ProcessorModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.ProcessorTableRowViewModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.{ProcessorTabViewModel, TabTableViewModel}
import org.itsadigitaltrust.hardwarelogger.core.BeanConversions.given
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.itsadigitaltrust.hardwarelogger.views.View
import org.itsadigitaltrust.hardwarelogger.views.tabs.given
import scalafx.scene.control.TableCell

import scala.jdk.javaapi.CollectionConverters.asJavaCollection
import scala.reflect.{ClassTag, classTag}

private[views] final class ProcessorTabView(using HardwareGrabberService)(using vm: ProcessorTabViewModel = new ProcessorTabViewModel) extends TabTableView:

  private val nameColumn = createAndAddColumn("Chip Type", minimumWidth = ColumnSize.massive): cellValue =>
    cellValue.nameProperty
  private val speedColumn = createAndAddColumn("Speed"): cellValue =>
    cellValue.speedProperty

  speedColumn.delegate.setCellFactory(tableColumn => new control.TableCell[ProcessorTableRowViewModel, String]:

    override def updateItem(item: String, empty: Boolean): Unit =
      super.updateItem(item, empty)
      if item == null || empty then
        setText(null)

      else
        item.substring(0, 4) + " GHz" |> setText
  )




  //  private val shortDescriptionColumn = createAndAddColumn("Short Description"): cellValue =>
  //    cellValue.shortDescriptionProperty

  private val longDescriptionColumn = createAndAddColumn("Description"): cellValue =>
    cellValue.longDescriptionProperty

  //  private val serialColumn = createAndAddColumn("Serial"): cellValue =>
  //    cellValue.serialProperty

  //  private val widthColumn = createAndAddColumn("Width"): cellValue =>
  //    cellValue.widthProperty


  private val coresColumn = createAndAddColumn("Cores"): cellValue =>
    cellValue.coresProperty

  private val threadsColumn = createAndAddColumn("Threads"): cellValue =>
    cellValue.threadsProperty
