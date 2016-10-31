/*
 * Copyright 2016 Krzysztof Pado
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.rdbc.pgsql.scodec

import java.nio.charset.Charset

import _root_.scodec.Attempt.{Failure, Successful}
import _root_.scodec.DecodeResult
import _root_.scodec.bits.BitVector
import io.rdbc.pgsql.core.codec.{Decoded, Decoder}
import io.rdbc.pgsql.core.exception.PgDecodeException
import io.rdbc.pgsql.core.messages.backend.{Header, PgBackendMessage}
import io.rdbc.pgsql.scodec.msg.backend._

object ScodecDecoder extends Decoder {
  override def decodeMsg(bytes: Array[Byte])(implicit charset: Charset): Decoded[PgBackendMessage] = {
    pgBackendMessage.decode(BitVector.view(bytes)) match {
      case Successful(DecodeResult(msg, remainder)) => Decoded(msg, remainder.toByteArray)
      case Failure(err) => throw PgDecodeException(err.messageWithContext)
    }
  }

  override def decodeHeader(bytes: Array[Byte]): Decoded[Header] = {
    header.decode(BitVector.view(bytes)) match {
      case Successful(DecodeResult(msg, remainder)) => Decoded(msg, remainder.toByteArray)
      case Failure(err) => throw PgDecodeException(err.messageWithContext)
    }
    //TODO code dupl
  }
}
