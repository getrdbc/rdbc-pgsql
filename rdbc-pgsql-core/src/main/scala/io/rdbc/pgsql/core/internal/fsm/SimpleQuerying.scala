/*
 * Copyright 2016-2017 Krzysztof Pado
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

package io.rdbc.pgsql.core.internal.fsm

import io.rdbc.pgsql.core.pgstruct.messages.backend._
import io.rdbc.pgsql.core.{ChannelWriter, PgMsgHandler}

import scala.concurrent.Promise

private[core]
class SimpleQuerying private[fsm](promise: Promise[Unit])(implicit out: ChannelWriter) extends State {

  protected val msgHandler: PgMsgHandler = {
    case _: RowDescription => stay
    case _: DataRow => stay
    case _: CommandComplete | EmptyQueryResponse => goto(State.waitingAfterSuccess(promise))
  }

  protected def onNonFatalError(ex: Throwable): StateAction = traced {
    goto(State.waitingAfterFailure(promise, ex))
  }

  protected def onFatalError(ex: Throwable): Unit = traced {
    promise.failure(ex)
  }
}
