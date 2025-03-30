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
import org.itsadigitaltrust.common.Result
import org.itsadigitaltrust.hardwarelogger.backend.entities.*
import org.itsadigitaltrust.hardwarelogger.backend.{DataStoreLoader, HLDatabase}
import org.itsadigitaltrust.hardwarelogger.models.*
import ox.*

import java.sql.Timestamp
import java.time.OffsetDateTime
import java.util.concurrent.ArrayBlockingQueue

private type HLEntityType[M <: HLModel] = M match
  case GeneralInfoModel | ProcessorModel => Info
  case MemoryModel => Memory
  case HardDriveModel => Disk
  case MediaModel => Media

trait HLDatabaseService:
  var itsaid: String = ""

  def connect(klazz: Class[?], dbPropsFilePath: String): Result[Unit, String]

  def +=[M <: HLModel](model: M): Unit

  def stop(): Unit

object SimpleHLDatabaseService extends HLDatabaseService, ServicesModule:

  private var db: Option[HLDatabase] = None
  private val minQueueSize = 8
  private val transactionQueue = new ArrayBlockingQueue[HLEntityCreatorWithItsaID](minQueueSize)


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
    transactionQueue.add(creator)


    if transactionQueue.size() >= minQueueSize then
      supervised:
        fork:
          while transactionQueue.size() > 0 do
            val c = transactionQueue.remove()
            db.get.insertOrUpdate(c)
        .join()

    if transactionQueue.size() < 1 then
      showAlertBox()
      println("Updated database.")


  override def stop(): Unit = ()

  private var processor: Option[ProcessorModel] = None

  given [T]: Conversion[T, Option[T]] with
    override def apply(x: T): Option[T] = Some(x)

  private def createEC[M](model: M): HLEntityCreatorWithItsaID =
    model match
      case memory: MemoryModel => createMemory(memory)
      case hardDriveModel: HardDriveModel => createHardDrive(hardDriveModel)
      case infoModel: GeneralInfoModel => createInfo(infoModel)
      case mediaModel: MediaModel => createMedia(mediaModel)
      case processorModel: ProcessorModel =>
        processor = processorModel
        createInfo(hardwareGrabberService.generalInfo)
      case _ => scala.sys.error("Unknown Type!")

  private def showAlertBox(): Unit =
    notificationCentre.publish(NotificationChannel.DBSuccess)


  private def createMemory(memoryModel: MemoryModel): MemoryCreator =
    MemoryCreator(memoryModel.size.dbString, itsaid, memoryModel.description)

  private def createHardDrive(hardDriveModel: HardDriveModel): DiskCreator =
    DiskCreator(itsaid, hardDriveModel.model, hardDriveModel.size.dbString, hardDriveModel.serial, hardDriveModel.`type`.toString, "ATA Disk")

  private def createInfo(infoModel: GeneralInfoModel): InfoCreator =
    val totalMemory = hardwareGrabberService.memory.map(_.size.value).sum
    val processor = this.processor.getOrElse(hardwareGrabberService.processors.head)
    val creator = InfoCreator(cpuVendor = infoModel.vendor,
      itsaid = this.itsaid, cpuSerial = Some(processor.serial), totalMemory = s"$totalMemory GiB",
      cpuSpeed = processor.speed.toString, cpuDescription = processor.longDescription, cpuProduct = processor.name,
      genDesc = "TODO", genId = "itsa-hwlogger", genProduct = "CPU", genSerial = infoModel.serial, genVendor = infoModel.vendor,
      cpuWidth = processor.width.toString, os = infoModel.os, cpuCores = processor.cores.toString, insertionDate = Timestamp.from(OffsetDateTime.now().toInstant), lastUpdated = Timestamp.from(OffsetDateTime.now().toInstant))
    this.processor = None

    creator

  private def createMedia(media: MediaModel): MediaCreator =

    MediaCreator(itsaid, media.description, media.handle)