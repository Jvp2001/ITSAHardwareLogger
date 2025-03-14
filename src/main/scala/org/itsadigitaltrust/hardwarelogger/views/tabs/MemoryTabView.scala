package org.itsadigitaltrust.hardwarelogger.views.tabs

import javafx.scene.control.Tab
import org.itsadigitaltrust.hardwarelogger.models.Memory
import org.itsadigitaltrust.hardwarelogger.services.HardwareGrabberService
import org.itsadigitaltrust.hardwarelogger.viewmodels.TabTableViewModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.MemoryTableRowViewModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
final class MemoryTabView(viewModel: TabTableViewModel[Memory, MemoryTableRowViewModel]) extends TabTableView[Memory, MemoryTableRowViewModel](viewModel):

  private val sizeColumn = createAndAddColumn("Size"): cellValue =>
    cellValue.sizeProperty

  private val descriptionColumn = createAndAddColumn("Description"): cellValue =>
    cellValue.descriptionProperty
