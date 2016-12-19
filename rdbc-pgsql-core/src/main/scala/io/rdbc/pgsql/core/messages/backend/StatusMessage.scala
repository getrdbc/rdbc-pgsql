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

package io.rdbc.pgsql.core.messages.backend

import io.rdbc.pgsql.core.exception.PgProtocolViolationException

case class StatusData(severity: String,
                      sqlState: String,
                      message: String,
                      detail: Option[String],
                      hint: Option[String],
                      position: Option[Int],
                      internalPosition: Option[Int],
                      internalQuery: Option[String],
                      where: Option[String],
                      schemaName: Option[String],
                      tableName: Option[String],
                      columnName: Option[String],
                      dataTypeName: Option[String],
                      constraintName: Option[String],
                      file: String,
                      line: String,
                      routine: String) {

  def shortInfo: String = s"$severity-$sqlState: $message"

  override def toString: String = {
    s"""
       |severity=$severity
       |sqlState=$sqlState
       |message=$message
       |detail=${detail.getOrElse("none")}
       |hint=${hint.getOrElse("none")}
       |position=${position.map(_.toString).getOrElse("none")}
       |internalPosition=${internalPosition.map(_.toString).getOrElse("none")}
       |internalQuery=${internalQuery.getOrElse("none")}
       |where=${where.getOrElse("none")}
       |schemaName=${schemaName.getOrElse("none")}
       |tableName=${tableName.getOrElse("none")}
       |columnName=${columnName.getOrElse("none")}
       |dataTypeName=${dataTypeName.getOrElse("none")}
       |constraintName=${constraintName.getOrElse("none")}
       |file=$file
       |line=$line
       |routine=$routine
       |""".stripMargin
  }
}


sealed trait StatusMessage extends PgBackendMessage {
  def statusData: StatusData
  def isWarning: Boolean = statusData.sqlState.startsWith("01") || statusData.sqlState.startsWith("02")
}

object StatusMessage {
  def error(fields: Map[Byte, String]): StatusMessage.Error = {
    StatusMessage.Error(statusData(fields))
  }

  def notice(fields: Map[Byte, String]): StatusMessage.Notice = {
    StatusMessage.Notice(statusData(fields))
  }

  private def notNullField(key: Byte, fields: Map[Byte, String]): String = {
    fields.getOrElse(key,
      throw new PgProtocolViolationException(s"Mandatory field '$key' was not found in the status data")
    )
  }

  private def intField(key: Byte, fields: Map[Byte, String]): Option[Int] = {
    try {
      fields.get(key).map(_.toInt)
    } catch {
      case ex: NumberFormatException => throw new PgProtocolViolationException(s"Field '$key' could not be parsed as an integer", ex)
    }
  }

  private def statusData(fields: Map[Byte, String]): StatusData = {
    StatusData(
      severity = fields.get('V').orElse(fields.get('S'))
        .getOrElse(throw new PgProtocolViolationException(s"Neither 'V' nor 'S' severity field was found in the status data")),
      sqlState = notNullField('C', fields),
      message = fields.getOrElse('M', "dupa"),
      detail = fields.get('D'),
      hint = fields.get('H'),
      position = intField('P', fields),
      internalPosition = intField('p', fields),
      internalQuery = fields.get('q'),
      where = fields.get('W'),
      schemaName = fields.get('s'),
      tableName = fields.get('t'),
      columnName = fields.get('c'),
      dataTypeName = fields.get('d'),
      constraintName = fields.get('n'),
      file = notNullField('F', fields),
      line = notNullField('L', fields),
      routine = notNullField('R', fields)
    )
  }

  case class Error(statusData: StatusData) extends StatusMessage {
    def isFatal: Boolean = {
      if (statusData.sqlState == "57014") {
        false //query canceled
      } else {
        val errCat = statusData.sqlState.take(2)
        errCat match {
          case "57" => true //operator intervention
          case "58" => true //system error
          case "XX" => true //PG internal error
          case _ => false
        }
      }
    }
  }

  case class Notice(statusData: StatusData) extends StatusMessage
}

