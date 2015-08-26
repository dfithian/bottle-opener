package com.fithian.common.actor

import org.slf4j.LoggerFactory
import akka.actor.{Actor, ActorRef}

sealed abstract class ActorStateMessage
object Start extends ActorStateMessage
object Stop extends ActorStateMessage

trait ActorOps {
    val logger = LoggerFactory.getLogger(getClass)
    def actorStateMessage: PartialFunction[ActorStateMessage, Unit] = {
        case Start =>
            logger.info(getClass + " received message Start")
            children.foreach(_ ! Start)
            start()
        case Stop =>
            logger.info(getClass + " received message Stop")
            children.foreach(_ ! Stop)
            stop()
    }
    val children: Seq[ActorRef] = Seq.empty[ActorRef]
    def start(): Unit
    def stop(): Unit
}
