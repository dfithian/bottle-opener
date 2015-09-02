package com.fithian.api.common

import org.slf4j.{Logger, LoggerFactory}
import akka.actor.Actor
import spray.routing.{Directives, HttpService, Route}
import spray.http.{ContentTypes, HttpRequest, HttpResponse, HttpEntity, StatusCode, StatusCodes, HttpMethods, HttpMethod}
import spray.httpx.marshalling.{BasicMarshallers, ToResponseMarshaller, ToResponseMarshallingContext}
import spray.httpx.unmarshalling.{BasicUnmarshallers, FromRequestUnmarshaller, MalformedContent, UnsupportedContentType, Deserialized}
import com.paytronix.utils.interchange.format.json.JsonCoder
import com.paytronix.utils.scala.result.{Result, ResultG, Failed, FailedG, Okay, optionOps, eitherOps, parameter}
import com.paytronix.utils.scala.log.resultLoggerOps

class Service(route: Route) extends Actor with HttpService {
    def actorRefFactory = context
    def receive = runRoute(route)
}

sealed abstract class ServiceResult[+A <: Reply] {
    def fold[B](f: (FailureReply) => B, g: (A) => B): B
}
final case class ServiceSuccess[+A <: Reply](result: A) extends ServiceResult[A] {
    def fold[B](f: (FailureReply) => B, g: (A) => B): B =
        g(result)
}
final case class ServiceFailure[+A <: Reply](failure: FailureReply) extends ServiceResult[A] {
    def fold[B](f: (FailureReply) => B, g: (A) => B): B =
        f(failure)
}
trait FailedParameterImplicits {
    implicit def result2ServiceResult[T <: Reply](in: ResultG[FailureReply, T]): ServiceResult[T] = in match {
        case Okay(r) => ServiceSuccess(r)
        case FailedG(_, r) => ServiceFailure(r)
    }
}

trait Endpoints extends Directives with Marshalling {
    implicit val logger = LoggerFactory.getLogger(getClass)
    def endpoint[A <: Request, B <: Reply](name: String, f: A => ServiceResult[B], httpMethod: HttpMethod = HttpMethods.POST)(implicit requestCoder: JsonCoder[A], replyCoder: JsonCoder[B]) = {
        (path(name) & method(httpMethod)) {
            entity[A](unmarshal[A]) { request => ctx =>
                f(request).fold(
                    error => ctx.complete(error)(marshal[FailureReply]),
                    success => ctx.complete(success)(marshal[B])
                )
            }
        }
    }
}

trait Marshalling {
    implicit val logger: Logger
    val acceptedContentTypes = Seq(ContentTypes.`application/json`)
    class Unmarshal[A <: Request](implicit jsonCoder: JsonCoder[A]) extends FromRequestUnmarshaller[A] {
        def apply(request: HttpRequest): Deserialized[A] =
            (for {
                _ <- request.acceptableContentType(acceptedContentTypes).toResult | parameter(UnsupportedContentType("unacceptable content type"))
                string <- BasicUnmarshallers.StringUnmarshaller(request.entity).toResult | parameter(MalformedContent("entity was empty"))
                decoded <- jsonCoder.decode.fromString(string) | parameter(MalformedContent("failed to decode"))
            } yield decoded)
                .logError("failed to unmarshal " + request)
                .toEither.left.map(_._2)
    }
    class Marshal[B <: Reply](implicit jsonCoder: JsonCoder[B]) extends ToResponseMarshaller[B] {
        def apply(value: B, ctx: ToResponseMarshallingContext): Unit = {
            val response = (for {
                _ <- ctx.tryAccept(acceptedContentTypes).toResult
                string <- jsonCoder
                    .encode
                    .toString(value)
            } yield HttpResponse(status = value.responseCode, entity = HttpEntity(ContentTypes.`application/json`, string)))
                .logError("failed to marshal " + value)
            (response, ctx.tryAccept(acceptedContentTypes)) match {
                case (Okay(r), Some(_)) => ctx.marshalTo(r)
                case (Failed(t), _) => ctx.handleError(t)
            }
        }
    }
    implicit def unmarshal[A <: Request](implicit jsonCoder: JsonCoder[A]) = new Unmarshal[A]
    implicit def marshal[B <: Reply](implicit jsonCoder: JsonCoder[B]) = new Marshal[B]
}
