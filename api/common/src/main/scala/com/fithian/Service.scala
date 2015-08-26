package com.fithian.api.common

import org.slf4j.{Logger, LoggerFactory}
import akka.actor.Actor
import spray.routing._
import spray.http._
import MediaTypes._
import spray.httpx.marshalling.{BasicMarshallers, ToResponseMarshaller, ToResponseMarshallingContext}
import spray.httpx.unmarshalling.{BasicUnmarshallers, FromRequestUnmarshaller, MalformedContent, Deserialized}
import com.paytronix.utils.interchange.format.json.JsonCoder
import com.paytronix.utils.scala.result.{Result, ResultG, Failed, FailedG, Okay, optionOps, eitherOps}
import com.paytronix.utils.scala.log.resultLoggerOps

class Service(route: Route) extends Actor with HttpService {
    def actorRefFactory = context
    def receive = runRoute(route)
}

trait Endpoints extends Directives with Marshalling {
    implicit val logger = LoggerFactory.getLogger(getClass)
    def endpoint[A <: Request, B <: Reply](name: String, f: A => Result[B])(implicit requestCoder: JsonCoder[A], replyCoder: JsonCoder[B]) = {
        (path(name) & post) {
            entity[A](unmarshal[A]) { request => ctx =>
                f(request)
                    .toEither
                    .fold(
                        f => ctx.failWith(f._1),
                        b => ctx.complete(b)(marshal[B])
                    )
            }
        }
    }
}

trait Marshalling {
    implicit val logger: Logger
    class Unmarshal[A](implicit jsonCoder: JsonCoder[A]) extends FromRequestUnmarshaller[A] {
        def apply(request: HttpRequest): Deserialized[A] =
            (for {
                string <- BasicUnmarshallers.StringUnmarshaller(request.entity).toResult
                decoded <- jsonCoder
                    .decode
                    .fromString(string)
            } yield decoded)
                .logError("failed to unmarshal " + request)
                .toEither
                .left
                .map(f => MalformedContent(f._1.getMessage))
    }
    class Marshal[A](implicit jsonCoder: JsonCoder[A]) extends ToResponseMarshaller[A] {
        def apply(value: A, ctx: ToResponseMarshallingContext): Unit =
            (for {
                string <- jsonCoder
                    .encode
                    .toString(value)
            } yield ctx.marshalTo(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, string))))
                .logError("failed to marshal " + value)
    }
    implicit def unmarshal[A](implicit jsonCoder: JsonCoder[A]) = new Unmarshal[A]
    implicit def marshal[A](implicit jsonCoder: JsonCoder[A]) = new Marshal[A]
}
