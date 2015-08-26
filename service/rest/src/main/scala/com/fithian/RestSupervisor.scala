package com.fithian.service.rest

import org.slf4j.{Logger, LoggerFactory}
import scala.concurrent.duration.DurationInt
import akka.actor.{Actor, Props}
import akka.pattern.ask
import akka.util.Timeout
import akka.io.IO
import spray.can.Http
import spray.routing._
import spray.http._
import com.fithian.common.actor.{ActorStateMessage, ActorOps}
import com.fithian.common.config.ConfigLoader
import com.fithian.api.common.Service
import com.fithian.api.bottle.BottleService
import com.fithian.api.user.UserService
import com.paytronix.utils.scala.result.{Result, ResultG, Failed, FailedG, Okay, optionOps, eitherOps}
import com.paytronix.utils.scala.log.resultLoggerOps

class RestSupervisor extends Actor with ActorOps with Directives {
    private val config = ConfigLoader.restServerConfig
    val service = context.actorOf(Props(new Service(AllRoutes())), "rest-service")
    implicit val timeout: Timeout = 5 seconds
    val io = {
        import context.system
        IO(Http)
    }
    def start() = io ? Http.Bind(service, interface = "localhost", port = 8080)
    def stop() = context stop self
    def receive = {
        case (asm: ActorStateMessage) => actorStateMessage(asm)
    }
}

object AllRoutes extends Directives {
    def apply() =
        new BottleService().route ~
        new UserService().route
}

sealed abstract class Task
case class CompletedTask[+S, +T](result: ResultG[S, T])
case class Tasks(completed: List[CompletedTask[_, _]], todo: List[Task])
sealed abstract class Finished(completed: List[CompletedTask[_, _]])
case class Success(completed: List[CompletedTask[_, _]]) extends Finished(completed)
case class Failure(completed: List[CompletedTask[_, _]], failed: CompletedTask[_, _]) extends Finished(completed)
