package org.itsadigitaltrust.hardwarelogger.viewmodels.rows

import org.itsadigitaltrust.hardwarelogger.models.GeneralInfoModel
import org.itsadigitaltrust.hardwarelogger.viewmodels.TableRowViewModel
import scalafx.beans.property.*
import org.itsadigitaltrust.hardwarelogger.services.given

final class GeneralInfoTableRowViewModel(model: GeneralInfoModel)(using itsaID: String) extends TableRowViewModel[GeneralInfoModel](model):
  def computerIDProperty: StringProperty =
    wrapper.field("computerID", _.computerID, "")(StringProperty.apply)

  def descriptionProperty: StringProperty =
    wrapper.field("description", _.description, "")(StringProperty.apply)

  def modelProperty: StringProperty =
    wrapper.field("model", _.model, "")(StringProperty.apply)

  def vendorProperty: StringProperty =
    wrapper.field("vendor", _.vendor, "")(StringProperty.apply)

  def serialProperty: StringProperty =
    wrapper.field("serial", _.serial, "")(StringProperty.apply)

  def osProperty: StringProperty =
    wrapper.field("os", _.os, "")(StringProperty.apply)