package org.itsadigitaltrust.hardwarelogger.application

import javafx.application.{Application, Preloader}
import org.itsadigitaltrust.hardwarelogger.consumers.PreloadedDataConsumer
import org.scalafx.extras
import scalafx.application.{JFXApp3, JFXApp3AccessHook}
import scalafx.application.JFXApp3.PrimaryStage

import scala.compiletime.uninitialized
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait JavaFXApp3WithPreloader[D] extends JFXApp3 with PreloadedDataConsumer[D]:
  

  val application: Application =
    JFXApp3AccessHook.getActiveJFXApp

  private var data: D = uninitialized

  
  def preloadData(): Unit = ()

  private lazy val loadingTask = Future(preloadData())

  protected final def notifyPreloader(notification: Preloader.PreloaderNotification): Unit =
    JFXApp3AccessHook.getActiveJFXApp.notifyPreloader(notification)
    
    

  final override def start(): Unit =
    loadingTask


    this.stage = new PrimaryStage()

  final override def receiveData(data: D): Unit =
    this.data = data
     extras.onFX:
       onDataReceived(data)


  protected  def onDataReceived(d: D): Unit
end JavaFXApp3WithPreloader

