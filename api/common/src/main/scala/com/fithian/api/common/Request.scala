package com.fithian.api.common

import com.paytronix.utils.interchange.format.json
import com.paytronix.utils.interchange.format.json.JsonCoder
import com.paytronix.utils.interchange.format.json.coders.{stringJsonCoder, booleanJsonCoder, intJsonCoder}

sealed abstract class Request
case class PingRequest() extends Request
object PingRequest {
    implicit val jsonCoder = json.derive.structure.coder[PingRequest]
}
case class BottleOpenRequest(bottleId: Int) extends Request
object BottleOpenRequest {
    implicit val jsonCoder = json.derive.structure.coder[BottleOpenRequest]
}
