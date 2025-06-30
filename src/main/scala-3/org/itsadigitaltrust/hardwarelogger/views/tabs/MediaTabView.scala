package org.itsadigitaltrust.hardwarelogger.views.tabs

import org.itsadigitaltrust.hardwarelogger.core.BeanConversions.given
import org.itsadigitaltrust.hardwarelogger.models.*
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.*
import org.itsadigitaltrust.hardwarelogger.viewmodels.tabs.TabTableViewModel
private[tabs] given[T]: Conversion[T, Seq[T]] with
  override def apply(x: T): Seq[T] = Seq(x)

private[views] final class MediaTabView(using itsaID: String) extends TabTableView[MediaModel, MediaTableRowViewModel](using TabTableViewModel(MediaTableRowViewModel.apply, _.media)):
  private val descriptionColumn = createAndAddColumn("Description"): cellValue =>
    cellValue.descriptionProperty

  private val handleColumn = createAndAddColumn("Handle"): cellValue =>
    cellValue.handleProperty