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

import com.typesafe.scalalogging.StrictLogging
import io.netty.channel.Channel
import io.rdbc.pgsql.core.ChannelWriter
import io.rdbc.pgsql.core.exception.ChannelException
import io.rdbc.pgsql.core.messages.frontend.PgFrontendMessage

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class NettyChannelWriter(ch: Channel)(implicit ec: ExecutionContext) extends ChannelWriter with StrictLogging {

  def write(msgs: PgFrontendMessage*): Future[Unit] = {
    msgs.foldLeft(Future.successful(())) { (_, msg) =>
      logger.trace(s"Writing message '$msg' to channel ${ch.id()}")
      ch.write(msg).scalaFut
    }.recoverWith {
      case NonFatal(ex) => Future.failed(ChannelException(ex))
    }
  }

  def close(): Future[Unit] = {
    logger.trace(s"Closing channel ${ch.id()}")
    ch.close().scalaFut.recoverWith {
      case NonFatal(ex) => Future.failed(ChannelException(ex))
    }
  }

  def flush(): Unit = {
    logger.trace(s"Flushing channel ${ch.id()}")
    try {
      ch.flush()
    } catch {
      case NonFatal(ex) => throw ChannelException(ex)
    }
  }
}
