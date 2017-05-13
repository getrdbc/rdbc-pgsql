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

package io.rdbc.pgsql.core.internal

import akka.stream.scaladsl.Source
import io.rdbc.api.exceptions._
import io.rdbc.implbase.StatementPartialImpl
import io.rdbc.pgsql.core.SessionParams
import io.rdbc.pgsql.core.pgstruct.{Oid, ParamValue}
import io.rdbc.pgsql.core.types.{PgType, PgTypeRegistry}
import io.rdbc.sapi._
import io.rdbc.util.Logging
import org.reactivestreams.Publisher

import scala.concurrent.{ExecutionContext, Future}

private[core] class PgStatement(stmtExecutor: PgStatementExecutor,
                                pgTypes: PgTypeRegistry,
                                sessionParams: SessionParams,
                                nativeStmt: PgNativeStatement)
                               (implicit ec: ExecutionContext)
  extends Statement
    with StatementPartialImpl
    with Logging {

  def bind(params: (String, Any)*): ParametrizedStatement = traced {
    val pgParamValues = toPgParamValueSeq(Map(params: _*))
    pgParametrizedStatement(pgParamValues)
  }

  def bindByIdx(params: Any*): ParametrizedStatement = traced {
    if (params.size < nativeStmt.params.size) {
      throw new MissingParamValException(nativeStmt.params(params.size))
    } else if (params.size > nativeStmt.params.size) {
      throw new TooManyParamsException(provided = params.size, expected = nativeStmt.params.size)
    } else {
      val pgParamValues = params.map(toPgParamValue).toVector
      pgParametrizedStatement(pgParamValues)
    }
  }

  def noParams: ParametrizedStatement = traced(bindByIdx())

  def streamParams(paramsPublisher: Publisher[Map[String, Any]]): Future[Unit] = traced {
    val pgParamsSource = Source.fromPublisher(paramsPublisher).map { paramMap =>
      toPgParamValueSeq(paramMap)
    }
    stmtExecutor.executeParamsStream(nativeStmt.sql, pgParamsSource)
  }

  private def pgParametrizedStatement(pgParamValues: Vector[ParamValue]): PgParametrizedStatement = traced {
    new PgParametrizedStatement(
      executor = stmtExecutor,
      nativeSql = nativeStmt.sql,
      params = pgParamValues
    )
  }

  private def toPgParamValueSeq(params: Map[String, Any]): Vector[ParamValue] = traced {

    case class Acc(res: Vector[ParamValue], left: Set[String])

    val pgParamsMap = params.mapValues(toPgParamValue)
    val init = Acc(res = Vector.empty, left = params.keySet)
    val indexedPgParams = nativeStmt.params.foldLeft(init) { (acc, paramName) =>
      acc.copy(
        res = acc.res :+ pgParamsMap.getOrElse(paramName, throw new MissingParamValException(paramName)),
        left = acc.left - paramName
      )
    }
    if (indexedPgParams.left.nonEmpty) {
      throw new NoSuchParamException(indexedPgParams.left.head)
    } else {
      indexedPgParams.res
    }
  }

  private def toPgParamValue(value: Any): ParamValue = traced {
    //TODO document in bind null/None/Some support
    value match {
      case null | None => ParamValue.Null(Oid.unknownDataType)
      case NullParam(cls) => withPgType(cls)(pgType => ParamValue.Null(pgType.typeOid))
      case NotNullParam(notNullVal) => notNullToPgParamValue(notNullVal)
      case Some(notNullVal) => notNullToPgParamValue(notNullVal)
      case notNullVal => notNullToPgParamValue(notNullVal)
    }
  }

  private def notNullToPgParamValue(value: Any): ParamValue = traced {
    withPgType(value.getClass) { pgType =>
      val binVal = pgType.asInstanceOf[PgType[Any]].toPgBinary(value)(sessionParams)
      ParamValue.Binary(binVal, pgType.typeOid)
    }
  }

  private def withPgType[A, B](cls: Class[A])(body: PgType[A] => B): B = {
    pgTypes
      .typeByClass(cls)
      .map(body)
      .getOrElse(throw new NoSuitableConverterFoundException(cls))
  }
}