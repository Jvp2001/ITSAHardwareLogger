package org.itsadigitaltrust.hardwarelogger.tasks
import org.itsadigitaltrust.common.Operators.??
import org.scalafx.extras.BusyWorker.SimpleTask
import org.scalafx.extras.runTask
import scalafx.beans.property.BooleanProperty
import scalafx.concurrent.WorkerStateEvent

import java.util.concurrent.Future
import scala.Conversion.into
trait SimpleTaskDelegate[R, Task <: SimpleTask[R]]:
  /**
   * Should contain the main logic for this task.
   * @param task The task this delegate method is being invoked on.
   * @return
   */
  def call(task: Task): R
end SimpleTaskDelegate

trait SimpleTaskStateDelegate[R, Task <: SimpleTask[R]] extends SimpleTaskDelegate[R, Task]:

  /**
   * Method called whenever the state of the Task has transitioned to the SCHEDULED state.
   * This method is invoked on the FX Application Thread after any listeners of the state property and after the
   * Task has been fully transitioned to the new state.
   */
  def onScheduled(task: Task): Unit = ()

  /**
   * Method called whenever the state of the Task has transitioned to the RUNNING state.
   * This method is invoked on the FX Application Thread after any listeners of the state property and after the
   * Task has been fully transitioned to the new state.
   */
  def onRunning(task: Task): Unit = ()

  /**
   * called whenever the state of the Task has transitioned to the SUCCEEDED state. This method is invoked on the FX Application Thread after any listeners of the state property and after the Task has been fully transitioned to the new state.
   */
  def onSucceeded(task: Task): Unit = ()

  /**
   * Method called whenever the state of the Task has transitioned to the CANCELLED state.
   * This method is invoked on the FX Application Thread after any listeners of the state property and after the
   * Task has been fully transitioned to the new state.
   */
  def onCancelled(task: Task): Unit = ()

  /**
   * Method called whenever the state of the Task has transitioned to the FAILED state.
   * This method is invoked on the FX Application Thread after any listeners of the state property and after the
   * Task has been fully transitioned to the new state.
   *
   * Implementation should consume the event to stop execution of automated error handling by
   * the `BusyWorker` for this event.
   *
   * When custom error handling is implemented, it may be more convenient to do it in `onFinish`,
   * as more information about the failure can be accessed there using `Try(result.get)`.
   *
   * @param e event state issued during the failure.
   */
  def onFailed(task: Task, e: WorkerStateEvent): Unit = ()

  /**
   * Perform some actions after `call()` completed.
   * This is executed regardless of the success or failure of `call()`.
   * Use this to prevent blocking while waiting for `call()` to finish.
   * The default implementation does nothing.
   *
   * @param result     a future containing result returned by `call()`.
   *                   The result can be obtained using `result.get()`.
   *                   Only valid if `call()` completed successfully.
   *
   * @param successful will be `true` if the call is completed successfully (without exceptions and was not cancelled).
   */
  def onFinish(task: SimpleTask[R], result: java.util.concurrent.Future[R], successful: Boolean): Unit = ()
end SimpleTaskStateDelegate



final class DelegatedSimpleTask[R](using delegate: SimpleTaskDelegate[R, DelegatedSimpleTask[R]] | Null ) extends DelegatableSimpleTask[R, DelegatedSimpleTask[R]]

trait DelegatableSimpleTask[R, T <: SimpleTask[R]] (using taskDelegate: SimpleTaskDelegate[R, T] | Null) extends SimpleTask[R]:
  this: T =>
    given delegate: SimpleTaskDelegate[R, T] | Null = taskDelegate
    private given stateDelegate: Option[SimpleTaskStateDelegate[R, this.type]] = Option(delegate.asInstanceOf[SimpleTaskStateDelegate[R, this.type]])
    override def onScheduled(): Unit =
      stateDelegate.foreach(_.onScheduled(this))
  
    override def onRunning(): Unit =
      stateDelegate.foreach(_.onRunning(this))
  
    override def onSucceeded(): Unit =
      stateDelegate.foreach(_.onSucceeded(this))
    
    override def onCancelled(): Unit =
      stateDelegate.foreach(_.onCancelled(this))
    
    override def onFailed(e: WorkerStateEvent): Unit =
      stateDelegate.foreach(_.onFailed(this, e))
    
    override def onFinish(result: Future[R], successful: Boolean): Unit =
      stateDelegate.foreach(_.onFinish(this, result, successful))
  
    override def call(): R =
      delegate.call(this)
  
end DelegatableSimpleTask


trait DisableNodesSimpleTaskDelegate[R, Task <: SimpleTask[R]](using disableNodesProperty: into[Option[BooleanProperty]]) extends SimpleTaskStateDelegate[R, Task]:
  protected final def disabled: Option[Boolean] = disableNodesProperty.map(_.value)
  protected final def disabled_=(value: Boolean): Unit = disableNodesProperty.foreach(_.value = value)



  /**
   * called whenever the state of the Task has transitioned to the SUCCEEDED state. This method is invoked on the FX Application Thread after any listeners of the state property and after the Task has been fully transitioned to the new state.
   */
  override def onSucceeded(task: Task): Unit =
    super.onSucceeded(task)
    disabled = false

  override def onFailed(task: Task, e: WorkerStateEvent): Unit =
    super.onFailed(task, e)
    disabled = false

  /**
   * Method called whenever the state of the Task has transitioned to the SCHEDULED state.
   * This method is invoked on the FX Application Thread after any listeners of the state property and after the
   * Task has been fully transitioned to the new state.
   */
  override def onScheduled(task: Task): Unit =
    super.onScheduled(task)


  /**
   * Method called whenever the state of the Task has transitioned to the RUNNING state.
   * This method is invoked on the FX Application Thread after any listeners of the state property and after the
   * Task has been fully transitioned to the new state.
   */
  override def onRunning(task: Task): Unit =
    super.onRunning(task)
    disabled = true

  /**
   * Method called whenever the state of the Task has transitioned to the CANCELLED state.
   * This method is invoked on the FX Application Thread after any listeners of the state property and after the
   * Task has been fully transitioned to the new state.
   */
  override def onCancelled(task: Task): Unit =
    super.onCancelled(task)
    disabled = false





