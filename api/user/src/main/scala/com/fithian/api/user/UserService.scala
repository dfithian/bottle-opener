package com.fithian.api.user

import spray.http.StatusCodes
import com.fithian.api.common.{Endpoints, PingRequest, PingReply, ServiceSuccess}
import com.paytronix.utils.scala.result.{Result, Failed, Okay}

class UserService extends Endpoints {
    logger.info("user service started")
    val route = pathPrefix("user") {
        endpoint[PingRequest, PingReply]("ping", _ => ServiceSuccess(PingReply(StatusCodes.OK)))
    }
}
