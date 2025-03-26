package org.itsadigitaltrust.hardwarelogger.views.tabs

import org.itsadigitaltrust.hardwarelogger.models.HardDriveModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.TabTableViewModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.HardDriveTableRowViewModel
import scalafx.scene.control.cell.CheckBoxTableCell

private given viewModel: TabTableViewModel[HardDriveModel,HardDriveTableRowViewModel] = new TabTableViewModel(HardDriveTableRowViewModel.apply, _.hardDrives)

class HardDrivesTabView extends TabTableView[HardDriveModel, HardDriveTableRowViewModel]:

  import org.itsadigitaltrust.hardwarelogger.core.BeanConversions.given

  private val healthColumn = createAndAddColumn("Health"): cellValue =>
    cellValue.healthProperty

  private val sizeColumn = createAndAddColumn("Size"): cellValue =>
    cellValue.sizeProperty

  private val modelColumn = createAndAddColumn("Model"): cellValue =>
    cellValue.modelProperty

  private val serialColumn = createAndAddColumn("Serial"): cellValue =>
    cellValue.serialProperty

  private val typeColumn = createAndAddColumn("Type"): cellValue =>
    cellValue.typeProperty

  private val idColumn = createAndAddColumn("ID"): cellValue =>
    cellValue.idProperty

  private val isSSDColumn = createAndAddColumn("Is SSD"): cellValue =>
    cellValue.isSSDProperty


//  private val isSSDColumn: TableTabColumn[Boolean] = new TableTabColumn[Boolean]:
//    cellValueFactory =  cellValue =>
//      cellValue.isSSDProperty
//    cellFactory = CheckBoxTableCell.forTableColumn(this)
//




