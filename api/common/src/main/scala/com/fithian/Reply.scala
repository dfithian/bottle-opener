package com.fithian.api.common

import com.paytronix.utils.interchange.format.json
import com.paytronix.utils.interchange.format.json.JsonCoder
import com.paytronix.utils.interchange.format.json.coders.{stringJsonCoder, booleanJsonCoder, intJsonCoder}

sealed abstract class Reply
case class PingReply() extends Reply
object PingReply {
    implicit val jsonCoder = json.derive.structure.coder[PingReply]
}
case class BottleOpenReply() extends Reply
object BottleOpenReply {
    implicit val jsonCoder = json.derive.structure.coder[BottleOpenReply]
}
