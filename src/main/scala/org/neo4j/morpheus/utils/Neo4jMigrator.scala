/*
 * Copyright (c) 2016-2018 "Neo4j Sweden, AB" [https://neo4j.com]
 */
package org.neo4j.morpheus.utils

import java.net.URI

import com.neo4j.morpheus.api.GraphSources
import org.opencypher.okapi.api.graph.Namespace
import org.opencypher.spark.api.CAPSSession
import org.opencypher.spark.api.io.neo4j.Neo4jConfig

object Neo4jMigrationWriter extends App {

  implicit val session: CAPSSession = CAPSSession.local()

  session.registerSource(Namespace("file"), GraphSources.fs(getClass.getResource("/parquet/").getPath).parquet())
  session.registerSource(Namespace("neo4j"), GraphSources.cypher.neo4j(
    Neo4jConfig(URI.create("bolt://localhost:7687"),
      "neo4j",
      Some("passwd"))))

  val FILE_ROOT = ""

  session.cypher(
    """
      |CREATE GRAPH neo4j.sn {
      |  FROM GRAPH file.sn
      |  RETURN GRAPH
      |}
    """.stripMargin)

}

object Neo4jMigrationReader extends App {

  implicit val session: CAPSSession = CAPSSession.local()

  session.registerSource(Namespace("file"), GraphSources.fs("/tmp/graphs/orc").orc())
  session.registerSource(Namespace("neo4j"), GraphSources.cypher.neo4j(
    Neo4jConfig(URI.create("bolt://localhost:7687"),
      "neo4j",
      Some("passwd"))))

  val FILE_ROOT = ""

  session.cypher(
    """
      |CREATE GRAPH file.sn {
      |  FROM GRAPH neo4j.sn
      |  RETURN GRAPH
      |}
    """.stripMargin)

}