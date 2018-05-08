package org.neo4j.morpheus.examples

import java.net.URI

import org.apache.hadoop.fs.Path
import org.neo4j.cypher.spark.EnterpriseNeo4jGraphSource
import org.neo4j.hdfs.parquet.HdfsParquetGraphSource
import org.neo4j.morpheus.utils.Neo4jHelpers
import org.neo4j.morpheus.utils.Neo4jHelpers._
import org.opencypher.okapi.api.graph.Namespace
import org.opencypher.spark.api.CAPSSession

object Neo4jAndParquetExample extends App {

  // Create CAPS session
  implicit val session: CAPSSession = CAPSSession.local()

  // Start a Neo4j instance and populate it with social network data
  val neo4j = Neo4jHelpers.startNeo4j(personNetwork)

  // Register Graph Data Sources (GDS)
  session.registerSource(Namespace("neo4j"), EnterpriseNeo4jGraphSource(neo4j.uri))

  // Access the graph via its qualified graph name
  val socialNetwork = session.catalog.graph("neo4j.graph")

  // Register a File-based data source in the Cypher session
  session.registerSource(Namespace("parquet"), HdfsParquetGraphSource(root = new Path(getClass.getResource("/parquet").getFile)))

  // Access the graph via its qualified graph name
  val purchaseNetwork = session.catalog.graph("parquet.products")

  // Build new recommendation graph that connects the social and product graphs and
  // create new edges between users and customers with the same name
  session.cypher(
    """|CREATE GRAPH session.linkGraph {
       |  FROM GRAPH neo4j.graph
       |  MATCH (p:Person)
       |  FROM GRAPH parquet.products
       |  MATCH (c:Customer)
       |  WHERE p.name = c.name
       |  CONSTRUCT
       |    ON neo4j.graph, parquet.products
       |    NEW (p)-[:IS]->(c)
       |  RETURN GRAPH
       |}
    """.stripMargin
  )

  // Query for product recommendations
  val recommendations = session.cypher(
    """|CREATE GRAPH neo4j.recommendations {
       |  FROM GRAPH session.linkGraph
       |  MATCH (person:Person)-[:FRIEND_OF]-(friend:Person),
       |        (friend)-[:IS]->(customer:Customer),
       |        (customer)-[:BOUGHT]->(product:Product)
       |  CONSTRUCT
       |    ON session.linkGraph
       |    NEW (person)-[:SHOULD_BUY]->(product)
       |  RETURN GRAPH
       |}""".stripMargin)

  // Proof that the write-back to Neo4j worked, retrieve and print updated Neo4j results
  val recommendationGraph = session.catalog.graph("neo4j.recommendations")
  val res = recommendationGraph.cypher(
    s"""
       |MATCH (n:Person)-[:SHOULD_BUY]->(p:Product)
       |RETURN n.name AS person, p.title AS product
     """.stripMargin)

  // Show the results
  res.show

  // Shutdown Neo4j test instance
  neo4j.close()

  def personNetwork =
    s"""|CREATE (a:Person:___graph { name: 'Alice', age: 23 })
        |CREATE (b:Person:___graph { name: 'Bob', age: 42})
        |CREATE (c:Person:___graph { name: 'Carol', age: 1984})
        |CREATE (a)-[:FRIEND_OF { since: '23/01/1987' }]->(b)
        |CREATE (b)-[:FRIEND_OF { since: '12/12/2009' }]->(c)""".stripMargin

}
