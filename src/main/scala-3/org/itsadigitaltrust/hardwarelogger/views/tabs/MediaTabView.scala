package org.itsadigitaltrust.hardwarelogger.views.tabs

import org.itsadigitaltrust.common.Operators.{??, |>, ||>}
import org.itsadigitaltrust.hardwarelogger.core.BeanConversions.given
import org.itsadigitaltrust.hardwarelogger.models.*
import org.itsadigitaltrust.hardwarelogger.services.{HLDatabaseService, HardwareGrabberService}
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.*
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.{MediaTabViewModel, TabTableViewModel}
import org.itsadigitaltrust.hardwarelogger.core.ui.*
import org.itsadigitaltrust.hardwarelogger.delegates.TabDelegate
import org.itsadigitaltrust.hardwarelogger.services.validation.GlobalValidationContext
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.itsadigitaltrust.hardwarelogger.views.View

import scala.jdk.CollectionConverters.*
import scala.jdk.javaapi.CollectionConverters.asJavaCollection
import scala.reflect.classTag
private[tabs] given[T]: Conversion[T, Seq[T]] with
  override def apply(x: T): Seq[T] = Seq(x)

private[views] final class MediaTabTableView private[tabs](using mediaTabViewModel: MediaTabViewModel)(using HardwareGrabberService) extends TabTableView[MediaTabViewModel]:
  MediaTableRowViewModel.apply ||> sharedApplicationData.media ?? Seq() |> asJavaCollection |> items.value.setAll
  private val descriptionColumn = createAndAddColumn("Description", minimumWidth = 125): cellValue =>
    cellValue.descriptionProperty

  private val handleColumn = createAndAddColumn("Handle", minimumWidth = 125): cellValue =>
    cellValue.handleProperty
end MediaTabTableView

class MediaTabView(using databaseService: HLDatabaseService, hardwareGrabberService: HardwareGrabberService) extends VBox with TabDelegate with View[MediaTabViewModel]:
  override given viewModel: MediaTabViewModel = new MediaTabViewModel
  
  private val tableView = new MediaTabTableView(using viewModel)
  
  children += tableView
  
  
end MediaTabView
