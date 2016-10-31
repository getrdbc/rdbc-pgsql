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

package io.rdbc.pgsql.transport.netty

import java.nio.charset.Charset

import com.typesafe.scalalogging.StrictLogging
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.rdbc.pgsql.core._
import io.rdbc.pgsql.core.messages.backend._
import io.rdbc.pgsql.core.scheduler.TaskScheduler
import io.rdbc.pgsql.core.types.PgTypeRegistry
import io.rdbc.sapi._

import scala.concurrent.{ExecutionContext, Future}

class NettyPgConnection(pgTypeRegistry: PgTypeRegistry,
                        rdbcTypeConvRegistry: TypeConverterRegistry,
                        out: ChannelWriter,
                        decoder: PgMsgDecoderHandler,
                        encoder: PgMsgEncoderHandler,
                        ec: ExecutionContext,
                        scheduler: TaskScheduler,
                        requestCanceler: (BackendKeyData) => Future[Unit])
  extends PgConnection(pgTypeRegistry, rdbcTypeConvRegistry, out, ec, scheduler, requestCanceler)
    with StrictLogging {

  val handler = new SimpleChannelInboundHandler[PgBackendMessage] {
    override def channelRead0(ctx: ChannelHandlerContext, msg: PgBackendMessage): Unit = {
      onMessage(msg)
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
      logger.error("Unhandled exception occurred", cause)
      doRelease(cause)
    }
  }

  protected def clientCharsetChanged(charset: Charset): Unit = {
    encoder.charset = charset
  }

  protected def serverCharsetChanged(charset: Charset): Unit = {
    decoder.charset = charset
  }
}