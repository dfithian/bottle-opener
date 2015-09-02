package com.fithian.common.config

import java.io.File
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import com.fithian.common.interchange.helpers.jsonDecoderOps
import com.typesafe.config.{ConfigFactory, Config}
import com.paytronix.utils.scala.result.{Result, Okay, Failed, tryCatchValue, optionOps}
import com.paytronix.utils.interchange.format.json
import com.paytronix.utils.interchange.format.json.coders.{stringJsonCoder, booleanJsonCoder, intJsonCoder}

case class RestClientConfig(port: Int, address: String, isSecure: Boolean)
object RestClientConfig {
    implicit val jsonCoder = json.derive.structure.coder[RestClientConfig]
}

case class RestServerConfig(port: Int, address: String)
object RestServerConfig {
    implicit val jsonCoder = json.derive.structure.coder[RestServerConfig]
}

case class DatabaseConfig(driver: String, url: String) {
    val file: Result[File] = {
        val dirMatcher = "((\\w)*)((/(\\w)+)*)(/)".r
        val fileMatcher = "((\\w)*)((/(\\w)+)+)(\\.((\\w)+))$".r
        (for {
            part <- url.split(":").toList.reverse.headOption
            directory <- dirMatcher.findFirstIn(part)
            filename <- fileMatcher.findFirstIn(part)
        } yield {
            val dir = new File(directory)
            if (!dir.exists) dir.mkdirs
            val file = new File(filename)
            if (!file.exists) file.createNewFile
            file
        }).toResult
    }
}
object DatabaseConfig {
    implicit val jsonCoder = json.derive.structure.coder[DatabaseConfig]
}

object ConfigLoader {
    lazy val rootConfig: Config = ConfigFactory.load().getConfig("bottle")
    lazy val akkaConfig: Config = rootConfig.getConfig("akka-config")
    val restClientConfig: RestClientConfig =
        RestClientConfig.jsonCoder.decode.fromConfig(rootConfig.getConfig("client")).orThrow
    val restServerConfig: RestServerConfig =
        RestServerConfig.jsonCoder.decode.fromConfig(rootConfig.getConfig("server")).orThrow
    val databaseConfig: DatabaseConfig =
        DatabaseConfig.jsonCoder.decode.fromConfig(rootConfig.getConfig("database").getConfig("sqlite")).orThrow
}
