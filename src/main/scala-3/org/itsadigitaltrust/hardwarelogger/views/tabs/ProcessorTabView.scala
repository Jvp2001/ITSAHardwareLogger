package org.itsadigitaltrust.hardwarelogger.views.tabs

import org.itsadigitaltrust.hardwarelogger.models.ProcessorModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.ProcessorTableRowViewModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.TabTableViewModel
import org.itsadigitaltrust.hardwarelogger.core.BeanConversions.given
import org.itsadigitaltrust.hardwarelogger.views.tabs.given

import scala.reflect.{ClassTag, classTag}

private[views] final class ProcessorTabView(using itsaID: String) extends TabTableView(using classTag[ProcessorModel], TabTableViewModel(ProcessorTableRowViewModel.apply, _.processors)):
  private val nameColumn = createAndAddColumn("Chip Type", minWidth = ColumnSize.massive): cellValue =>
    cellValue.nameProperty
  private val speedColumn = createAndAddColumn("Speed"): cellValue =>
    cellValue.speedProperty

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
