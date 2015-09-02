package com.fithian.api.bottle

import java.util.Date
import java.sql.Timestamp
import spray.http.{StatusCodes, StatusCode}
import scala.slick.driver.SQLiteDriver.simple._
import com.fithian.common.database.{
    Transaction, CounterDetail, db,
    device_enums, devices, counters, counter_details, transactions}
import com.fithian.api.common.{Endpoints, FailedParameterImplicits, PingRequest, PingReply, BottleOpenRequest, BottleOpenReply, ServiceSuccess, ServiceFailure, ServiceResult, FailureReply, Reply}
import com.paytronix.utils.scala.result.{Result, ResultG, Failed, FailedG, Okay, tryCatchValue, optionOps, parameter}
import com.paytronix.utils.scala.log.resultLoggerOps

class BottleService() extends Endpoints with FailedParameterImplicits {
    logger.info("bottle service started")
    val route = pathPrefix("bottle") {
        endpoint[BottleOpenRequest, BottleOpenReply]("open", openBottle)
    }

    def openBottle(request: BottleOpenRequest): ServiceResult[BottleOpenReply] = {
        db.get withSession { implicit session =>
            (for {
                device <- tryCatchValue(devices.filter(_.deviceId === request.bottleId).firstOption.toResult).flatten | FailureReply(StatusCodes.BadRequest, "device.not_found", "Device not found")
                counter <- tryCatchValue(counters.filter(_.deviceId === request.bottleId).firstOption.toResult).flatten | FailureReply(StatusCodes.BadRequest, "device.not_found", "Device not found")
                transactionId <- tryCatchValue {
                    transactions returning transactions.map(_.transactionId) += Transaction(None, counter.counterId, device.deviceId, new Timestamp(new Date().getTime))
                } | FailureReply(StatusCodes.InternalServerError, "server.error", "Server error")
                counterDetailId <- tryCatchValue(
                    counter_details returning counter_details.map(_.counterDetailId) += CounterDetail(None, transactionId, counter.counterId, device.deviceId, new Timestamp(new Date().getTime))
                ) | FailureReply(StatusCodes.InternalServerError, "server.error", "Server error")
            } yield BottleOpenReply(StatusCodes.OK, transactionId)).logError("failed to process request")
        }
    }
}
