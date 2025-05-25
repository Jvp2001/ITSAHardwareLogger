package org.itsadigitaltrust.hardwarelogger.views.tabs

import org.itsadigitaltrust.hardwarelogger.core.BeanConversions.given
import org.itsadigitaltrust.hardwarelogger.models.*
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.*
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.TabTableViewModel
private given[T]: Conversion[T, Seq[T]] with
  override def apply(x: T): Seq[T] = Seq(x)

private[views] final class GeneralInfoTabView extends TabTableView(using TabTableViewModel(GeneralInfoTableRowViewModel.apply, _.generalInfo)):
  private val computerIDColumn = createAndAddColumn("Computer ID"): cellValue =>
    cellValue.computerIDProperty

  private val descriptionColumn = createAndAddColumn("Description"): cellValue =>
    cellValue.descriptionProperty

  private val modelColumn = createAndAddColumn("Model"): cellValue =>
    cellValue.modelProperty

  private val vendorColumn = createAndAddColumn("Vendor"): cellValue =>
    cellValue.vendorProperty
  private val serial = createAndAddColumn("Serial"): cellValue =>
    cellValue.serialProperty


private[views] final class ProcessorTabView extends TabTableView[ProcessorModel, ProcessorTableRowViewModel](using TabTableViewModel(ProcessorTableRowViewModel.apply, _.processors)):
  private val nameColumn = createAndAddColumn("Chip Type"): cellValue =>
    println(s"Name: ${cellValue.nameProperty.get}")
    cellValue.nameProperty
  private val speedColumn = createAndAddColumn("Speed"): cellValue =>
    cellValue.speedProperty

//  private val shortDescriptionColumn = createAndAddColumn("Short Description"): cellValue =>
//    cellValue.shortDescriptionProperty

  private val longDescriptionColumn = createAndAddColumn("Description"): cellValue =>
    cellValue.longDescriptionProperty

  private val serialColumn = createAndAddColumn("Serial"): cellValue =>
    cellValue.serialProperty

  private val widthColumn = createAndAddColumn("Width"): cellValue =>
    cellValue.widthProperty

  private val coresColumn = createAndAddColumn("Cores"): cellValue =>
    cellValue.coresProperty

private[views] final class MediaTabView extends TabTableView[MediaModel, MediaTableRowViewModel](using TabTableViewModel(MediaTableRowViewModel.apply, _.media)):
  private val descriptionColumn = createAndAddColumn("Description"): cellValue =>
    cellValue.descriptionProperty

  private val handleColumn = createAndAddColumn("Handle"): cellValue =>
    cellValue.handleProperty