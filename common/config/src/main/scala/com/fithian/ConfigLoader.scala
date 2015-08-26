package com.fithian.common.config

import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import helpers.jsonDecoderOps
import com.typesafe.config.{ConfigFactory, Config}
import com.paytronix.utils.scala.result.{Result, Okay, Failed, tryCatchValue}
import com.paytronix.utils.interchange.format.json
import com.paytronix.utils.interchange.format.json.coders.{stringJsonCoder, booleanJsonCoder, intJsonCoder}

object ConfigLoader {
    lazy val rootConfig: Config = ConfigFactory.load().getConfig("bottle")
    lazy val akkaConfig: Config = rootConfig.getConfig("akka-config")
    val restClientConfig: RestClientConfig =
        RestClientConfig.jsonCoder.decode.fromConfig(rootConfig.getConfig("client")).orThrow
    val restServerConfig: RestServerConfig =
        RestServerConfig.jsonCoder.decode.fromConfig(rootConfig.getConfig("server")).orThrow
}

object RestClientConfig {
    implicit val jsonCoder = json.derive.structure.coder[RestClientConfig]
}
case class RestClientConfig(port: Int, address: String, isSecure: Boolean)

object RestServerConfig {
    implicit val jsonCoder = json.derive.structure.coder[RestServerConfig]
}
case class RestServerConfig(port: Int)
