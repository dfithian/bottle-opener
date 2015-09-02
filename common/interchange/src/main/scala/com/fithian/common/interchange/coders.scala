package com.fithian.common.interchange

import spray.http.{StatusCode, StatusCodes}
import com.fasterxml.jackson.core.{JsonParseException, JsonToken}
import com.paytronix.utils.interchange.format.json.{JsonDecoder, InterchangeJsonParser, JsonEncoder, InterchangeJsonGenerator, JsonCoder}
import com.paytronix.utils.interchange.base.{Receiver, CoderFailure, CoderResult}
import com.paytronix.utils.scala.result.{Result, ResultG, FailedG, Okay, tryCatchResultG}

package object coders {
    implicit object statusCodeCoder extends JsonCoder[StatusCode] {
        object encode extends JsonEncoder[StatusCode] {
            val mightBeNull = false
            val codesAsObject = false
            def run(in: StatusCode, out: InterchangeJsonGenerator) =
                tryCatchResultG(CoderFailure.terminal) {
                    out.writeNumber(in.intValue)
                    Okay.unit
                }
        }
        object decode extends JsonDecoder[StatusCode] {
            val mightBeNull = false
            val codesAsObject = false
            def run(in: InterchangeJsonParser, out: Receiver[StatusCode]): CoderResult[Unit] =
                FailedG("not implemented", CoderFailure.terminal)
        }
    }
}
