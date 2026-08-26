//package org.itsadigitaltrust.hardwarelogger
//
//import org.itsadigitaltrust.common.Operators.??
//import org.itsadigitaltrust.common.forkJoin
//import org.itsadigitaltrust.hardwarelogger.models.HLModel.GeneralInfoModel
//import org.itsadigitaltrust.hardwarelogger.shared.HardwareLoggerSharedData
//import org.itsadigitaltrust.hardwarelogger.traits.HardwareLoggerTestSuit
//import org.scalatest.funsuite.AnyFunSuite
//import org.scalatest.matchers.should.Matchers.should
//import ox.either.ok
//
//class ComputerModeLoggerTests extends AnyFunSuite with HardwareLoggerTestSuit:
//
//  private final val testID = "85977.0"
//  private final val testID2 = "40938.0"
//
//  def data: HardwareLoggerSharedData = applicationState.getData
//
//
//
//  test("Can connect to Database"):
//    val result = databaseService.connect()
//    result.right.value `should` be(())
//
//
//  test("Delete all rows")
//
//  test("Valid ID"):
//    hardwareIDValidationService.validate(testID) match
//      case Left(value) => assert(false, value)
//      case Right(value) => true
//
//
//  test("Remove all data from all tables in the database"):
//    databaseService.truncateAllTables()
//
//  test("Enter brand new Records without issue"):
//    ox.supervised:
//      applicationState.itsaID = Some(testID)
//      applicationState.reload().forkJoin()
//
//      println:
//        databaseService.dataSource.map(_.getDatabaseName)
//    val info =
//      applicationState.generalInfo
//        .flatMap(_.headOption)
//        ??
//        fail("Expected applicationState.generalInfo to contain at least one GeneralInfoModel after reload()")
//
//
//    val infoInsertResult = databaseService.insert(info)
//    assert(infoInsertResult.isRight, s"Failed to insert GeneralInfoModel: $infoInsertResult")
//    val memoryInsertResult = databaseService.insertAll(data.memory ?? Seq() *)
//    assert(memoryInsertResult.isRight, s"Failed to insert MemoryModel: $memoryInsertResult")
//    val driveInsertResult = databaseService.insertAll(data.drives ?? Seq() *)
//    assert(driveInsertResult.isRight, s"Failed to insert DriveModel: $driveInsertResult")
//    val mediaInsertResult = databaseService.insertAll(data.media ?? Seq() *)
//    assert(mediaInsertResult.isRight, s"Failed to insert MediaModel: $mediaInsertResult")
//
//    //
//    given infoFinder: FindByID[GeneralInfoModel] = databaseService.infoFinder
//
//    //    databaseService.saveData(data)
//    val findResult = databaseService.findByID[GeneralInfoModel](testID)
//    assert(findResult.isRight, s"Failed to find inserted GeneralInfoModel for id $testID: $findResult")
//
//  test("Replace info row with error"):
//    ox.supervised:
//      ox.either:
//        applicationState.itsaID = Some(testID)
//        applicationState.reload().forkJoin()
//
//        val i = data.info.map(_.head).ok()
//
//        val startingCountResult = databaseService.findAllInfoModelsWithErrorNumber(testID)
//        val startingCount =
//          if startingCountResult.isLeft then
//            if !databaseService.doesPCRecordExist(i) then
////              databaseService.insert(i).ok()
//            1
//          else
//            startingCountResult.ok().size
//          end if
//        end startingCount
//
//        val result = databaseService.insert(i)
//        assert(result.isRight, s"Failed to insert GeneralInfoModel: $result")
//        val value = databaseService.findAllInfoModelsWithErrorNumber(testID).ok()
//
//        assert( value.length > startingCount)
//
//
//  test("Should Update existing ITSA ID if the record's serial number is the same"):
//    ox.supervised:
//      ox.either:
//        applicationState.itsaID = Some(testID2)
//        applicationState.reload().forkJoin()
//
//        applicationState.
//
