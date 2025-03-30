package org.itsadigitaltrust.hardwarelogger.views.tabs

import org.itsadigitaltrust.hardwarelogger.models.HardDriveModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.HardDriveTableRowViewModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.TabTableViewModel
import scalafx.beans.property.{BooleanProperty, ObjectProperty}
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

  private val isSSDColumn = createAndAddColumn[Boolean]("Is SSD"): cellValue =>
    cellValue.isSSDProperty

  isSSDColumn.setCellFactory: column =>
    new CheckBoxTableCell[HardDriveTableRowViewModel, Boolean]():
      editable = false










