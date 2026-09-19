package org.itsadigitaltrust.hardwarelogger.views.tabs

import org.itsadigitaltrust.common.Operators.{??, |>, ||>}
import org.itsadigitaltrust.hardwarelogger.core.BuildInfo
import org.itsadigitaltrust.hardwarelogger.models.GeneralInfoModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.GeneralInfoTableRowViewModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.{GeneralInfoTabViewModel, TabTableViewModel}
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.services.database.HLDatabaseService
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import scalafx.beans.property.StringProperty

import scala.jdk.javaapi.CollectionConverters.asJavaCollection
import scala.reflect.classTag
import scala.language.experimental.modularity
private[views] final class GeneralInfoTabView(using HardwareGrabberService, HLDatabaseService)(using viewModel: GeneralInfoTabViewModel = new GeneralInfoTabViewModel) extends TabTableView[GeneralInfoTabViewModel#Model, GeneralInfoTabViewModel#RowViewModel, GeneralInfoTabViewModel]:

  
    private val computerIDColumn = createAndAddColumn("Version ID", minimumWidth = ColumnSize.big): cellValue =>
      val string = cellValue.computerIDProperty.value + s" ${BuildInfo.version.replace("-SNAPSHOT", "")}"
      string.replace("null ", "ITSAHardwareLogger ")
      |> StringProperty.apply

    private val descriptionColumn = createAndAddColumn("Description"): cellValue =>
      cellValue.descriptionProperty

    private val modelColumn = createAndAddColumn("Model", minimumWidth = ColumnSize.big): cellValue =>
      cellValue.modelProperty

    private val vendorColumn = createAndAddColumn("Vendor"): cellValue =>
      cellValue.vendorProperty
end GeneralInfoTabView
