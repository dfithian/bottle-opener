package com.fithian.application

import org.slf4j.LoggerFactory
import akka.actor.{ActorSystem, Props}
import com.fithian.common.actor.Start
import com.fithian.common.config.ConfigLoader
import com.fithian.service.rest.RestSupervisor

object Main {
    def main(args: Array[String]) = {
        // create logger
        val logger = LoggerFactory.getLogger(getClass)
        logger.info("Application BottleMain started with command line " + args.mkString(","))

        // load actor system
        implicit val actorSystem = ActorSystem("bottle", ConfigLoader.akkaConfig)
        val supervisor = actorSystem.actorOf(Props[RestSupervisor], "bottle-supervisor")
        supervisor ! Start
    }
}
