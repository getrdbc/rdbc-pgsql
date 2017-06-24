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

package io.rdbc.pgsql.scodec.types

import java.time._
import java.time.temporal.ChronoUnit.MICROS

import _root_.scodec.Codec
import _root_.scodec.codecs.int64
import io.rdbc.pgsql.core.SessionParams
import io.rdbc.pgsql.core.types.PgTimestampTz

object ScodecPgTimestampTz extends ScodecPgType[Instant] with PgTimestampTz with CommonCodec[Instant] {
  def codec(implicit sessionParams: SessionParams): Codec[Instant] = {
    int64.xmap(
      long2Instant,
      instant2Long
    )
  }

  private[this] val PgZero: Instant = {
    LocalDate.of(2000, Month.JANUARY, 1)
      .atStartOfDay(ZoneId.of("UTC"))
      .toInstant
  }

  private def long2Instant(l: Long): Instant = {
    PgZero.plus(l, MICROS)
  }

  private def instant2Long(inst: Instant): Long = {
    Duration.between(PgZero, inst).toMicros
  }
}
