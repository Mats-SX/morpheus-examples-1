package org.neo4j.morpheus.examples

import org.neo4j.morpheus.util.ExampleTest

class Neo4jReadExampleTest extends ExampleTest {
  it("runs Neo4jReadExample") {
    validate(
      Neo4jReadExample.main(Array.empty),
      getClass.getResource("/example_outputs/Neo4jReadExample.out").toURI
    )
  }

}
