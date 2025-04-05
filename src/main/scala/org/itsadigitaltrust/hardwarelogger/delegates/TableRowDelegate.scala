package org.itsadigitaltrust.hardwarelogger.delegates

trait TableRowDelegate[R]:
  def onSelected(row: Option[R]): Unit = ()
  def onUpdateItem(row: Option[R]): Unit = ()
