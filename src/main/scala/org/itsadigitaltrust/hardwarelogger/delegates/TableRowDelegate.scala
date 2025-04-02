package org.itsadigitaltrust.hardwarelogger.delegates

trait TableRowDelegate[R]:
  def onSelected(row: R): Unit = ()
  def onUpdateItem(row: R): Unit = ()
