package com.fithian.common.config

import com.typesafe.config.{Config, ConfigObject, ConfigList, ConfigValue}
import scala.collection.mutable.Stack
import scala.collection.JavaConverters.{mapAsScalaMapConverter, asScalaBufferConverter}

import com.fasterxml.jackson.core.{JsonLocation, JsonParseException, JsonToken}

import com.paytronix.utils.interchange.base.{CoderFailure, CoderResult, Receiver, formatFailedPath}
import com.paytronix.utils.interchange.format.json.{JsonDecoder, InterchangeJsonParser}
import com.paytronix.utils.scala.result.{Failed, FailedG, Okay, Result, ResultG, tryCatchValue, iterableResultOps}

package object helpers {
    implicit class jsonDecoderOps[A](jsonDecoder: JsonDecoder[A]) {
        def fromConfig(in: Config): Result[A] = {
            val rec = new Receiver[A]
            val par = new ConfigJsonParser(in)
            formatFailedPath(jsonDecoder.run(par, rec)) map { _ => rec.value }
        }
    }
}

final class ConfigJsonParser(root: Config) extends InterchangeJsonParser {
    class ConfigOps(in: Config) {
        def asCValue = new CObj(in.root.unwrapped.asScala.toList.flatMap { case (s, o) => CValue.unapply(o) match {
                case Some(cv) => Some(new CField(s, cv))
                case _ => None
            }})
    }
    implicit def configOps(in: Config) = new ConfigOps(in)
    sealed abstract class CValue
    case class CString(s: String) extends CValue
    case class CNumber(n: Number) extends CValue
    case class CBool(b: Boolean) extends CValue
    case class CField(key: String, value: CValue) extends CValue
    case class CList(l: List[CValue]) extends CValue
    case class CObj(l: List[CField]) extends CValue
    object CValue {
        def unapply(in: Object): Option[CValue] = in match {
            case s: String => Some(new CString(s))
            case n: Number => Some(new CNumber(n))
            case b: java.lang.Boolean => Some(new CBool(b))
            case (key: String, o: Object) => o match {
                    case cv: CValue => Some(new CField(key, cv))
                    case _ => None
                }
            case l: List[_] =>
                val fields = l.flatMap { _ match {
                        case cf: CField => Some(cf)
                        case _ => None
                    }}
                val values = l.flatMap { _ match {
                        case cv: CValue => Some(cv)
                        case _ => None
                    }}
                if (fields.size > 0) Some(new CObj(fields)) else Some(new CList(values))
            case other => None
        }
    }
    sealed abstract class Frame { def dup: Frame }
    final case class TopFrame(var visited: Boolean) extends Frame {
        def dup = copy()
    }
    final case class ArrayFrame(var rest: List[CValue]) extends Frame {
        def dup = copy()
        override def toString = s"ArrayFrame${rest.size} els"
    }
    final case class ObjectFrame(var lookingAtFieldName: Boolean, var rest: List[CField]) extends Frame {
        def dup = copy()
        override def toString = s"ObjectFrame($lookingAtFieldName, ${rest.map { _.toString }.mkString(", ")})"
    }

    type Mark = List[Frame]

    private var _stack: Stack[Frame] = Stack(TopFrame(false))
    private var _currentValueIsMissing = false
    private var _didCheckMissingValue = false

    def hasValue: Boolean = {
        _didCheckMissingValue = true
        !_currentValueIsMissing
    }

    def currentValueIsMissing(): Unit = {
        _didCheckMissingValue = false
        _currentValueIsMissing = true
    }

    def currentToken: JsonToken =
        if (!_didCheckMissingValue) sys.error("decoder should have checked whether a value was present prior to calling currentToken")
        else {
            def cvToTok(cv: CValue): JsonToken =
                cv match {
                    case CString(_)         => JsonToken.VALUE_STRING
                    case CNumber(_)         => JsonToken.VALUE_NUMBER_INT
                    case CBool(true)        => JsonToken.VALUE_TRUE
                    case CBool(false)       => JsonToken.VALUE_FALSE
                    case CField(_, _)       => JsonToken.FIELD_NAME
                    case CList(_)           => JsonToken.START_ARRAY
                    case CObj(_)            => JsonToken.START_OBJECT
                    case other              => sys.error(other.toString + " not convertible to JsonToken")
                }

            val tok = _stack.top match {
                case TopFrame(false)                        => cvToTok(root.asCValue)
                case TopFrame(true)                         => sys.error("past end of input")
                case ArrayFrame(Nil)                        => JsonToken.END_ARRAY
                case ArrayFrame(cv :: _)                    => cvToTok(cv)
                case ObjectFrame(_, Nil)                    => JsonToken.END_OBJECT
                case ObjectFrame(true, _)                   => JsonToken.FIELD_NAME
                case ObjectFrame(false, CField(_, cv) :: _) => cvToTok(cv)
            }

            // println(s"${Thread.currentThread}: currentToken = $tok | stack = ${_stack}")

            tok
        }

    def currentLocation: JsonLocation = JsonLocation.NA

    def advanceToken(): CoderResult[Unit] = {
        _didCheckMissingValue = false
        _currentValueIsMissing = false
        _advance()
    }

    def advanceTokenUnguarded(): CoderResult[Unit] = {
        _didCheckMissingValue = true
        _currentValueIsMissing = false
        _advance()
    }

    private def _advance(): CoderResult[Unit] = {
        // println(s"${Thread.currentThread}: _advance()")
        _stack.top match {
            case TopFrame(true) =>
                FailedG("at end of input", CoderFailure.terminal)

            case f@TopFrame(_) =>
                val flds = root.asCValue.l
                _stack.push(ObjectFrame(true, flds))
                f.visited = true
                Okay.unit

            case ArrayFrame(Nil) =>
                _stack.pop
                Okay.unit

            case f@ArrayFrame(CList(els) :: tail) =>
                _stack.push(ArrayFrame(els))
                f.rest = tail
                Okay.unit

            case f@ArrayFrame(CObj(flds) :: tail) =>
                _stack.push(ObjectFrame(true, flds))
                f.rest = tail
                Okay.unit

            case f@ArrayFrame(_ :: tail) =>
                f.rest = tail
                Okay.unit

            case ObjectFrame(_, Nil) =>
                _stack.pop
                Okay.unit

            case f@ObjectFrame(true, _) =>
                f.lookingAtFieldName = false
                Okay.unit

            case f@ObjectFrame(false, CField(_, CList(els)) :: tail) =>
                _stack.push(ArrayFrame(els))
                f.lookingAtFieldName = true
                f.rest = tail
                Okay.unit

            case f@ObjectFrame(false, CField(_, CObj(flds)) :: tail) =>
                _stack.push(ObjectFrame(true, flds))
                f.lookingAtFieldName = true
                f.rest = tail
                Okay.unit

            case f@ObjectFrame(false, _ :: tail) =>
                f.lookingAtFieldName = true
                f.rest = tail
                Okay.unit
        }
    }

    def mark(): Mark = {
        if (!_didCheckMissingValue) sys.error("decoder should have checked whether a value was present prior to calling mark")
        _stack.toList.map(_.dup)
    }

    def rewind(m: Mark): Unit = {
        _stack = Stack(m: _*)
        _didCheckMissingValue = true
        _currentValueIsMissing = false
    }

    private def currentValue: CValue =
        _stack.top match {
            case TopFrame(false) => root.asCValue
            case ArrayFrame(cv :: _) => cv
            case ObjectFrame(false, CField(_, cv) :: _) => cv
            case _ => throw new JsonParseException("not at a value token", JsonLocation.NA)
        }

    def booleanValue: Boolean =
        currentValue match {
            case CBool(b)   => b
            case other      => throw new JsonParseException(other.toString + ", type: " + other.getClass + " not a boolean", JsonLocation.NA)
        }

    def byteValue: Byte =
        currentValue match {
            case CNumber(n)    => n.byteValue
            case other         => throw new JsonParseException(other.toString + ", type: " + other.getClass + " not a byte", JsonLocation.NA)
        }

    def shortValue: Short =
        currentValue match {
            case CNumber(n) => n.shortValue
            case other      => throw new JsonParseException(other.toString + ", type: " + other.getClass + " not a short", JsonLocation.NA)
        }

    def intValue: Int =
        currentValue match {
            case CNumber(n) => n.intValue
            case other      => throw new JsonParseException(other.toString + ", type: " + other.getClass + " not an int", JsonLocation.NA)
        }

    def longValue: Long =
        currentValue match {
            case CNumber(n) => n.longValue
            case other      => throw new JsonParseException(other.toString + ", type: " + other.getClass + " not a long", JsonLocation.NA)
        }

    def bigIntegerValue: java.math.BigInteger =
        currentValue match {
            case CNumber(n: java.math.BigInteger)  => n
            case other                             => throw new JsonParseException(other.getClass + "not an integer", JsonLocation.NA)
        }

    def floatValue: Float =
        currentValue match {
            case CNumber(n) => n.floatValue
            case other      => throw new JsonParseException(other.getClass + "not a number", JsonLocation.NA)
        }

    def doubleValue: Double =
        currentValue match {
            case CNumber(n) => n.doubleValue
            case other      => throw new JsonParseException(other.toString + ", type: " + other.getClass + " not a number", JsonLocation.NA)
        }

    def bigDecimalValue: java.math.BigDecimal =
        currentValue match {
            case CNumber(n: java.math.BigDecimal)  => n
            case other                                  => throw new JsonParseException(other.toString + ", type: " + other.getClass + " not a number", JsonLocation.NA)
        }

    def stringValue: String =
        currentValue match {
            case CString(s) => s
            case CNumber(n) => n.toString
            case CBool(b)   => b.toString
            case other      => throw new JsonParseException(other.toString + ", type: " + other.getClass + " not a string", JsonLocation.NA)
        }

    def fieldName: String =
        _stack.top match {
            case ObjectFrame(true, CField(name, _) :: _) => name
            case other => throw new JsonParseException(other + " not at a field name", JsonLocation.NA)
        }
}
