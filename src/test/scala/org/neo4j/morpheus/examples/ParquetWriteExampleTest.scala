package org.neo4j.morpheus.examples

import org.neo4j.morpheus.util.ExampleTest

class ParquetWriteExampleTest extends ExampleTest {
  it("runs Neo4jAndParquetExample") {
    validate(ParquetWriteExample.main(Array.empty), "")
  }

}
