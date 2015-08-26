package com.fithian.api.user

import com.fithian.api.common.{Endpoints, PingRequest, PingReply}
import com.paytronix.utils.scala.result.{Result, Failed, Okay}

class UserService extends Endpoints {
    val route = pathPrefix("user") {
        endpoint[PingRequest, PingReply]("ping", _ => Okay(PingReply()))
    }
}
