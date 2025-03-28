package org.itsadigitaltrust.hardwarelogger.services

//package org.itsadigitaltrust.hardwarelogger.services
//
//
//import com.augustnagro.magnum.DbCodec
//import org.itsadigitaltrust.hardwarelogger.models.{GeneralInfoModel, HLModel, HardDriveModel, MediaModel, MemoryModel}
//import org.itsadigitaltrust.common.Maths.*
//import org.itsadigitaltrust.hardwarelogger.backend.backend.*
//
//import java.util.concurrent.{ArrayBlockingQueue, ConcurrentLinkedQueue, Executors}
//import scala.annotation.tailrec
//import scala.collection.immutable.Queue
//import scala.collection.mutable
//import scala.jdk.CollectionConverters.*
//import scala.concurrent.*
//import scala.util.{Failure, Success, boundary}
//
//trait HLDatabaseService:
//  def setitsaid(value: String = ""): Unit
//
//  def connect(klazz: Class[?], dbPropsFilePath: String): Option[String]
//
//  def +=[M <: HLModel](model: M): Unit
//  def stop(): Unit
//  enum Error:
//    case TransactionFailed extends Error
//
//
//object SimpleHLDatabaseService extends HLDatabaseService, ServicesModule:
//  private given ec: ExecutionContext = new ExecutionContext:
//    private final val threadPool = Executors.newFixedThreadPool(1000)
//
//    override def execute(runnable: Runnable): Unit =
//      threadPool.submit(runnable)
//
//    override def reportFailure(cause: Throwable): Unit = ()
//
//
//  private final val processTransactionsGroupSize = 2
//  private final val maxTransactionQueueSize = 20
//
//  enum Transaction:
//    case Memory(memory: MemoryModel)
//    case Disk(disk: HardDriveModel)
//
//  private var promises = Queue[Promise[HLEntityCreator]]()
//  private val transactionQueue = new ConcurrentLinkedQueue[HLEntityCreator]()
//
//  private var db: Option[HLDatabase] = None
//  @volatile
//  private var itsaid = ""
////
////  private val transactionsWatcherThread = new Thread("Promises Queue Watcher"):
////    @tailrec
////    private def loop(): Unit =
//////      blocking:
////      val hasId = itsaid != "" || itsaid != null
////
////      val bool = hasId && transactionQueue.size() >= processTransactionsGroupSize && db.isDefined
////      print(bool)
////      if bool then
////        val frags = scala.collection.mutable.ArrayBuffer[HLEntityCreator]()
////        transactionQueue.(frags.asJavaCollection, processTransactionsGroupSize)
////        val futures = frags.map: frag =>
////          Future:
////            db.get.insertOrUpdate(frag)
////        .toSeq
////        Future.sequence(futures).onComplete: t =>
////          notificationCentre.publish(NotificationChannel.DBSuccess)
////      loop()
////    end loop
////
////    override def run(): Unit =
////      loop()
////  end transactionsWatcherThread
////
//
//  override def setitsaid(value: String = ""): Unit =
//    itsaid = value
//
//  override def connect(klazz: Class[?], dbPropsFilePath: String): Option[String] =
//    boundary:
//      db = HLDatabase(klazz.getResource(dbPropsFilePath).toURI.toURL) match
//        case Left(value) => return Option(value.toString)
//        case Right(value) => Option(value)
//
////    transactionsWatcherThread.start()
//    None
//
//  private def createEC[M](model: M): HLEntityCreator =
//    model match
//      case memory: MemoryModel => createMemory(memory)
//      case hardDriveModel: HardDriveModel => createHardDrive(hardDriveModel)
//      case infoModel: GeneralInfoModel => createInfo(infoModel)
//      case mediaModel: MediaModel => createMedia(mediaModel)
//      case _ => scala.sys.error("Unknown Type!")
//
//  private def createPromise[M <: HLModel](model: M)(body: HLEntityCreator => Unit): Promise[HLEntityCreator] =
//    Promise[HLEntityCreator]().completeWith:
//      Future:
//        val creator = createEC(model)
//        body(creator)
//        creator
//  end createPromise
//
//
//  given [T]: Conversion[T, Option[T]] with
//    override def apply(x: T): Option[T] = Some(x)
//
//  private def createMemory(memoryModel: MemoryModel): MemoryCreator =
//    MemoryCreator(memoryModel.size.toString, itsaid, memoryModel.description)
//
//  private def createHardDrive(hardDriveModel: HardDriveModel): DiskCreator =
//    // TODO: Check if this is the correct description, if not, where should I get it from?
//    DiskCreator(itsaid, hardDriveModel.model, hardDriveModel.size.toString, hardDriveModel.`type`.toString, "ATA Disk")
//
//  private def createInfo(infoModel: GeneralInfoModel): InfoCreator =
//    val totalMemory = hardwareGrabberService.getMemory.map: m =>
//      m.size
//    .sum.GiB
//    val processor = hardwareGrabberService.getProcessors.head
//    InfoCreator("itsa-hwlogger", "TODO", infoModel.description, infoModel.vendor,
//      itsaid, infoModel.serial, s"$totalMemory GiB",
//      "CPU", processor.speed.toString, processor.longDescription, processor.chipType, processor.serial,
//      processor.width.toString, infoModel.os, processor.cores.toString)
//
//  private def createMedia(media: MediaModel): MediaCreator =
//    MediaCreator(itsaid, media.description, media.handle)
//
//
//  def +=[M <: HLModel](model: M): Unit =
//    //    val promise = createPromise(model): t =>
//    val result = createEC(model) match
//      case mc: MemoryCreator => mc
//      case media: MediaCreator => media
//      case disk: DiskCreator => disk
//      case info: InfoCreator => info
//    transactionQueue.add(result)
//    if transactionQueue.size() >= processTransactionsGroupSize then
//      Future:
//        while !transactionQueue.isEmpty do
//          val creator = transactionQueue.poll()
//          if db.isDefined then
//            db.get.insertOrUpdate(creator)
//        notificationCentre.publish(NotificationChannel.Save)
//
//
//
//    println(transactionQueue.size())
//
//  override def stop(): Unit =
//    ()
////    transactionsWatcherThread.join()
//
//
//
//
//

import org.itsadigitaltrust.common
import org.itsadigitaltrust.common.Maths.GiB
import org.itsadigitaltrust.common.Result
import org.itsadigitaltrust.hardwarelogger.backend.{DataStoreLoader, HLDatabase}

import java.util.concurrent.{BlockingQueue, Executors, LinkedBlockingQueue}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, boundary}
import org.itsadigitaltrust.hardwarelogger.backend.entities.*
import org.itsadigitaltrust.hardwarelogger.models.*
import ox.*
import ox.channels.*
import ox.flow.Flow
import ox.scheduling.Schedule.Fixed
import ox.scheduling.{RepeatConfig, ScheduledConfig, SleepMode}

import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.DurationInt

trait HLDatabaseService:
  def setitsaid(value: String = ""): Unit

  def connect(klazz: Class[?], dbPropsFilePath: String): Result[Unit, String]

  def +=[M <: HLModel](model: M): Unit

  def stop(): Unit

object SimpleHLDatabaseService extends HLDatabaseService, ServicesModule:
  private given ec: ExecutionContext = ExecutionContext.global

  private var db: Option[HLDatabase] = None
  @volatile private var itsaid = ""
  private val transactionQueue: BlockingQueue[HLEntityCreator] = new LinkedBlockingQueue[HLEntityCreator]()
  private val channel = Channel.rendezvous[HLEntityCreator]
  private val minQueueSize = 2

  // Start a thread to monitor the queue size
  //  private val thread = flow.Flow.usingEmit:
  //    forever:
  //      if transactionQueue.size >= minQueueSize then
  //          val buffer = new ArrayBuffer[HLEntityCreator]()
  //          transactionQueue.drainTo(buffer.asJavaCollection)
  //          supervised:
  //            buffer.foreachPar(buffer.size): creator =>
  //              db.get.insertOrUpdate(creator)

  private def thread() =
    forever:
      val operations = new mutable.ArrayBuffer[HLEntityCreator]()
      transactionQueue.drainTo(operations.asJavaCollection)
      if itsaid.nonEmpty && itsaid != null && operations.size >= minQueueSize then
        supervised:
          operations.foreachPar(operations.size): creator =>
            db.get.insertOrUpdate(creator)
      showAlertBox()

  //  fork:
  //    forever:
  //      if transactionQueue.size >= minQueueSize then
  //        val buffer = new ArrayBuffer[HLEntityCreator]()
  //        transactionQueue.drainTo(buffer.asJavaCollection)
  //        supervised:
  //          buffer.foreachPar(buffer.size): creator =>
  //            db.get.insertOrUpdate(creator)
  //        notificationCentre.publish(NotificationChannel.DBSuccess)


  override def setitsaid(value: String = ""): Unit =
    itsaid = value

  override def connect(klazz: Class[?], dbPropsFilePath: String): Result[Unit, String] =
    Result:
      try
        HLDatabase(klazz.getResource(dbPropsFilePath).toURI.toURL) match
          case common.Success(value) =>
            db = Some(value)
            println("Connected to database.")
            Result.success(())
          case common.Error(reason) =>
            Result.error(reason.toString)
      catch case _: NullPointerException => Result.error(s"Cannot find file $dbPropsFilePath")


  override def +=[M <: HLModel](model: M): Unit =
    val creator = createEC(model)
    transactionQueue.put(creator)
    if transactionQueue.size >= minQueueSize then
      val buffer = new ArrayBuffer[HLEntityCreator]()
      supervised:
        fork:
          while !transactionQueue.isEmpty do
            val item = transactionQueue.remove()

            buffer.foreachPar(buffer.size): creator =>
              db.get.insertOrUpdate(item)
        .join()

        // TODO: then update the insertionDate.
      showAlertBox()
    println("Updated database.")


  override def stop(): Unit = ()


  private def createEC[M](model: M): HLEntityCreator =
    model match
      case memory: MemoryModel => createMemory(memory)
      case hardDriveModel: HardDriveModel => createHardDrive(hardDriveModel)
      case infoModel: GeneralInfoModel => createInfo(infoModel)
      case mediaModel: MediaModel => createMedia(mediaModel)
      case _ => scala.sys.error("Unknown Type!")

  private def showAlertBox(): Unit =
    notificationCentre.publish(NotificationChannel.DBSuccess)

  given [T]: Conversion[T, Option[T]] with
    override def apply(x: T): Option[T] = Some(x)

  private def createMemory(memoryModel: MemoryModel): MemoryCreator =
    MemoryCreator(memoryModel.size.toString, itsaid, memoryModel.description)

  private def createHardDrive(hardDriveModel: HardDriveModel): DiskCreator =
    println(s"Size: ${hardDriveModel.size}")
    DiskCreator(itsaid, hardDriveModel.model, hardDriveModel.size.toString, hardDriveModel.`type`.toString, "ATA Disk")

  private def createInfo(infoModel: GeneralInfoModel): InfoCreator =
    val totalMemory = hardwareGrabberService.memory.map(_.size.value).sum
    val processor = hardwareGrabberService.processors.head
    InfoCreator("itsa-hwlogger", "TODO", infoModel.description, infoModel.vendor,
      itsaid, infoModel.serial, s"$totalMemory GiB",
      "CPU", processor.speed.toString, processor.longDescription, processor.chipType, processor.serial,
      processor.width.toString, infoModel.os, processor.cores.toString)

  private def createMedia(media: MediaModel): MediaCreator =
    MediaCreator(itsaid, media.description, media.handle)