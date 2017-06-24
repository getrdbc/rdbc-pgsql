/*
 * Copyright 2016 rdbc contributors
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

package io.rdbc.pgsql.core.internal

import io.rdbc.implbase.ExecutableStatementPartialImpl
import io.rdbc.pgsql.core.pgstruct.Argument
import io.rdbc.pgsql.core.pgstruct.messages.frontend.NativeSql
import io.rdbc.sapi.{ExecutableStatement, RowPublisher, Timeout}
import io.rdbc.util.Logging

import scala.concurrent.{ExecutionContext, Future}

private[core] class PgExecutableStatement(executor: PgStatementExecutor,
                                          nativeSql: NativeSql,
                                          params: Vector[Argument])
                                         (implicit protected val ec: ExecutionContext)
  extends ExecutableStatement
    with ExecutableStatementPartialImpl
    with Logging {

  def stream()(implicit timeout: Timeout): RowPublisher = traced {
    executor.statementStream(nativeSql, params)
  }

  override def executeForRowsAffected()(implicit timeout: Timeout): Future[Long] = traced {
    executor.executeStatementForRowsAffected(nativeSql, params)
  }
}
