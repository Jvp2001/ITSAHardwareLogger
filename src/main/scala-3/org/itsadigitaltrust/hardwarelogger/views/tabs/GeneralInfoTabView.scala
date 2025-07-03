package org.itsadigitaltrust.hardwarelogger.views.tabs

import org.itsadigitaltrust.hardwarelogger.models.GeneralInfoModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.GeneralInfoTableRowViewModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.TabTableViewModel

private[views] final class GeneralInfoTabView(using itsaID: String) extends TabTableView[GeneralInfoModel, GeneralInfoTableRowViewModel](using TabTableViewModel(GeneralInfoTableRowViewModel.apply, _.generalInfo)):
  private val computerIDColumn = createAndAddColumn("Version ID", minWidth=ColumnSize.big): cellValue =>
    cellValue.computerIDProperty

  private val descriptionColumn = createAndAddColumn("Description"): cellValue =>
    cellValue.descriptionProperty

  private val modelColumn = createAndAddColumn("Model", minWidth=ColumnSize.big): cellValue =>
    cellValue.modelProperty

  private val vendorColumn = createAndAddColumn("Vendor"): cellValue =>
    cellValue.vendorProperty
