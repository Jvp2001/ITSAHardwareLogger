package org.itsadigitaltrust.hardwarelogger.viewmodels.tabs

import org.itsadigitaltrust.common.Operators.??
import org.itsadigitaltrust.hardwarelogger.models.HLModel
import org.itsadigitaltrust.hardwarelogger.state.HardwareLoggerApplicationState
import org.itsadigitaltrust.hardwarelogger.viewmodels.rows.TableRowViewModel
import scalafx.collections.ObservableBuffer
import sun.jvm.hotspot.runtime.PerfMemory.end

trait TableViewItemsDataContainer[M <: HLModel, RVM <: TableRowViewModel[M]](using applicationState: HardwareLoggerApplicationState)(rvmCtor: M => RVM, initialModels: HardwareLoggerApplicationState => Option[Seq[M]]):
  private val data: ObservableBuffer[RVM] = ObservableBuffer()

  def items: ObservableBuffer[RVM] = data

  def add(model: M): Unit =
    data.add(rvmCtor(model))
  end add
  def addAll(models: M*): Unit =
    models.foreach(add)
  end addAll

  def + (model: M): TableViewItemsDataContainer[M, RVM] =
    add(model)
    this
  end +
  def ++ (models: Seq[M]): this.type =
    addAll(models*)
    this
  end ++

  def map[A](f: RVM => A): ObservableBuffer[A] =
    data.map(f)

  def flatMap[A](f: RVM => IterableOnce[A] ): ObservableBuffer[A] =
    data.flatMap(f)

  def foreach(f: RVM => Unit): Unit =
    data.foreach(f)

end TableViewItemsDataContainer
