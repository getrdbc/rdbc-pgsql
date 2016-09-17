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

package io.rdbc.pgsql.core.messages.backend.auth

import scodec.bits.ByteVector

trait AuthRequest extends AuthBackendMessage {
  def authMechanismName: String
}

case class AuthRequestMd5(salt: ByteVector) extends AuthRequest {
  override val authMechanismName: String = "md5"

  override def toString = {
    val saltHex = salt.toArray.map("%02X" format _).mkString
    s"${getClass.getSimpleName}(salt = 0x$saltHex)"
  }
}
