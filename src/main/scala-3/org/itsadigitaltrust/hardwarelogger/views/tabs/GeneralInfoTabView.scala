package org.itsadigitaltrust.hardwarelogger.views.tabs

import org.itsadigitaltrust.common.Operators.|>

import org.itsadigitaltrust.hardwarelogger.core.BuildInfo
import org.itsadigitaltrust.hardwarelogger.models.GeneralInfoModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.GeneralInfoTableRowViewModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.TabTableViewModel

import scalafx.beans.property.StringProperty

import scala.reflect.classTag


private[views] final class GeneralInfoTabView(using itsaID: String) extends TabTableView(using classTag[GeneralInfoModel], TabTableViewModel(GeneralInfoTableRowViewModel.apply, _.generalInfo)):
  private val computerIDColumn = createAndAddColumn("Version ID", minWidth=ColumnSize.big): cellValue =>
    cellValue.computerIDProperty.value + s" ${BuildInfo.version.replace("-SNAPSHOT", "")}" |> StringProperty.apply

  private val descriptionColumn = createAndAddColumn("Description"): cellValue =>
    cellValue.descriptionProperty

  private val modelColumn = createAndAddColumn("Model", minWidth=ColumnSize.big): cellValue =>
    cellValue.modelProperty

  private val vendorColumn = createAndAddColumn("Vendor"): cellValue =>
    cellValue.vendorProperty
