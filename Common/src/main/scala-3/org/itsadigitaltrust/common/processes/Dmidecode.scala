package org.itsadigitaltrust.common.processes

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

  def apply(keyword: Keyword)(using config: ProcessConfig): String =
    sudo"dmidecode -s $keyword"
