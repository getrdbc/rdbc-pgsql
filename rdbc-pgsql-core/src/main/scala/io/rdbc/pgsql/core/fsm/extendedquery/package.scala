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

package io.rdbc.pgsql.core.fsm

import io.rdbc.pgsql.core.PgRowPublisher
import io.rdbc.pgsql.core.messages.backend.StatusMessage

import scala.concurrent.Promise

package object extendedquery {
  trait ExtendedQuerying extends State

  case class AfterDescData(publisher: PgRowPublisher,
                           warningsPromise: Promise[Vector[StatusMessage.Notice]],
                           rowsAffectedPromise: Promise[Long])
}
