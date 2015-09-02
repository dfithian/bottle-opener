package com.fithian.service.rest

import org.slf4j.{Logger, LoggerFactory}
import scala.concurrent.duration.DurationInt
import akka.actor.{Actor, Props, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import akka.io.IO
import spray.can.Http
import spray.routing.Directives
import com.fithian.common.actor.{ActorStateMessage, ActorOps}
import com.fithian.common.config.ConfigLoader
import com.fithian.api.common.Service
import com.fithian.api.bottle.BottleService
import com.fithian.api.user.UserService
import com.paytronix.utils.scala.result.{Result, ResultG, Failed, FailedG, Okay, optionOps, eitherOps}
import com.paytronix.utils.scala.log.resultLoggerOps

class RestSupervisor extends Actor with ActorOps with Directives {
    private val config = ConfigLoader.restServerConfig
    import context.system
    val service = context.actorOf(Props(new Service(AllRoutes())), "rest-service")
    implicit val timeout: Timeout = 5 seconds
    val io = IO(Http)
    def start() = io ? Http.Bind(service, interface = config.address, port = config.port)
    def stop() = context stop self
    def receive = {
        case asm: ActorStateMessage => actorStateMessage(asm)
    }
}

object AllRoutes extends Directives {
    def apply() =
        new BottleService().route ~
        new UserService().route
}
