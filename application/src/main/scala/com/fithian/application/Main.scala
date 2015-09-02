package com.fithian.application

import org.slf4j.LoggerFactory
import akka.actor.{ActorSystem, Props}
import com.fithian.common.actor.Start
import com.fithian.common.config.ConfigLoader
import com.fithian.database.DatabaseSupervisor
import com.fithian.service.rest.RestSupervisor

object Main {
    def main(args: Array[String]) = {
        // create logger
        val logger = LoggerFactory.getLogger(getClass)
        logger.info("application started with command line [" + args.mkString(",") + "]")

        implicit val actorSystem = ActorSystem("bottle", ConfigLoader.akkaConfig)
        // load database
        logger.info("starting database supervisor")
        val databaseSupervisor = actorSystem.actorOf(Props[DatabaseSupervisor], "database-supervisor")
        databaseSupervisor ! Start

        // load actor system
        logger.info("starting REST supervisor")
        val restSupervisor = actorSystem.actorOf(Props[RestSupervisor], "rest-supervisor")
        restSupervisor ! Start
    }
}
