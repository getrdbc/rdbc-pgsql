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

package io.rdbc.pgsql.netty.fsm.extendedquery

import io.rdbc.pgsql.core.messages.backend.{CommandComplete, ReadyForQuery}
import io.rdbc.pgsql.netty.{PgNettyConnection, PgRowPublisher}
import io.rdbc.pgsql.netty.fsm.Idle

import scala.concurrent.{ExecutionContext, Future}

class CompletedWaitingForReadyAfterCommit(publisher: PgRowPublisher)(implicit ec: ExecutionContext) extends ExtendedQueryingCommon {
  private var complete = false

  def handleMsg = {
    case CommandComplete("COMMIT", _) if !complete =>
      complete = true
      stay

    case ReadyForQuery(txStatus) if complete =>
      goto(Idle(txStatus)) andThen {
        publisher.complete()
      }
  }

  val shortDesc = "extended_querying.waiting_for_ready_after_commit"
}
