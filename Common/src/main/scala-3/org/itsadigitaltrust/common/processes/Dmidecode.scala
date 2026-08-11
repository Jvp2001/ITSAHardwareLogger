package org.itsadigitaltrust.common.processes

import org.itsadigitaltrust.common.OSUtils
import org.itsadigitaltrust.common.logging.HWLLoggable
import ox.either.ok

import scala.Conversion.into

object Dmidecode:
  type Keyword =
    "bios-vendor" |
      "bios-version" |
      "bios-release-date" |
      "bios-revision" |
      "firmware-revision" |
      "system-manufacturer" |
      "system-product-name" |
      "system-version" |
      "system-serial-number" |
      "system-uuid" |
      "system-sku-number" |
      "system-family" |
      "baseboard-manufacturer" |
      "baseboard-product-name" |
      "baseboard-version" |
      "baseboard-serial-number" |
      "baseboard-asset-tag" |
      "chassis-manufacturer" |
      "chassis-type" |
      "chassis-version" |
      "chassis-serial-number" |
      "chassis-asset-tag" |
      "processor-family" |
      "processor-manufacturer" |
      "processor-version" |
      "processor-frequency"

  given Conversion[Keyword, String] with
    def apply(keyword: Keyword): String = keyword

  def apply(keyword: Keyword)(using ProcessConfig, HWLLoggable): Either[SudoError, String] = 
    ox.either:
      if OSUtils.onLinux then
        sudo"dmidecode -s $keyword".ok()
      else
        ""

