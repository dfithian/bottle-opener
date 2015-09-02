package com.fithian.api.common

import spray.http.{StatusCode, StatusCodes}
import com.fithian.common.interchange.coders.statusCodeCoder
import com.paytronix.utils.interchange.format.json
import com.paytronix.utils.interchange.format.json.{JsonCoder, JsonEncoder, JsonDecoder, InterchangeJsonGenerator, InterchangeJsonParser}
import com.paytronix.utils.interchange.format.json.coders.{stringJsonCoder, booleanJsonCoder, intJsonCoder}
import com.paytronix.utils.scala.result.{Failed, FailedG, Result, Okay, optionOps, tryCatchResultG, FailedParameter, FailedParameterDefault}

trait Reply { val responseCode: StatusCode }
case class PingReply(responseCode: StatusCode) extends Reply
object PingReply {
    implicit val jsonCoder = json.derive.structure.coder[PingReply]
}
case class BottleOpenReply(responseCode: StatusCode, transactionId: Int) extends Reply
object BottleOpenReply {
    implicit val jsonCoder = json.derive.structure.coder[BottleOpenReply]
}
case class FailureReply(responseCode: StatusCode, errorCode: String, errorMessage: String) extends Reply with FailedParameter
object FailureReply {
    implicit val jsonCoder = json.derive.structure.coder[FailureReply]
    implicit val default = new FailedParameterDefault[FailureReply] {
        val default = FailureReply(StatusCodes.InternalServerError, "server.error", "Server error")
    }
}
