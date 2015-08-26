package com.fithian.api.bottle

import com.fithian.api.common.{Endpoints, PingRequest, PingReply, BottleOpenRequest, BottleOpenReply}
import com.paytronix.utils.scala.result.{Result, Failed, Okay}

class BottleService extends Endpoints {
    val route = pathPrefix("bottle") {
        endpoint[PingRequest, PingReply]("ping", _ => Okay(PingReply())) ~
        endpoint[BottleOpenRequest, BottleOpenReply]("open", _ => Okay(BottleOpenReply()))
    }
}
