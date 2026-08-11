package org.itsadigitaltrust.hardwarelogger.tasks

import org.scalafx.extras
import extras.BusyWorker.SimpleTask

trait FiniteSimpleTask[R](totalNumberOfItems: Int) extends SimpleTask[R]:
  private var _index = 0

  def index: Int = _index

  def index_=(value: Int): Unit = _index = value

  def outOfString: String =
    s"(${index + 1}/$totalNumberOfItems)"

  def incrementProgress(): Unit =
    extras.onFXAndWait:
      index += 1
      progress() = (index - 1) / totalNumberOfItems.toDouble
end FiniteSimpleTask

