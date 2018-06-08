package org.neo4j.morpheus.examples.accidents

import org.apache.spark.sql.SparkSession
import org.apache.spark.storage.StorageLevel
import org.opencypher.okapi.api.graph.PropertyGraph
import org.opencypher.spark.api.io.CAPSNodeTable
import org.opencypher.okapi.api.io.conversion.NodeMapping
import org.opencypher.spark.api.CAPSSession
import org.opencypher.spark.impl.CAPSConverters._

import scala.reflect.io.File

class Casualties(csvDir:String)(implicit spark:SparkSession, session: CAPSSession) extends Graphable {
  val fields = Seq(
    "Accident_Index", "Vehicle_Reference",
    "Casualty_Reference", "Casualty_Class", "Sex_of_Casualty",
    "Age_of_Casualty", "Age_Band_of_Casualty",
    "Casualty_Severity", "Pedestrian_Location",
    "Pedestrian_Movement", "Car_Passenger", "Bus_or_Coach_Passenger",
    "Pedestrian_Road_Maintenance_Worker",
    "Casualty_Type", "Casualty_Home_Area_Type", "Casualty_IMD_Decile")

  val df = spark.read
    .format("csv")
    .option("header", "true") //reading the headers
    .option("mode", "DROPMALFORMED")
    .load(csvDir + File.separator + "Cas.csv")
    .select(fields.head, fields.tail:_*)
    .withColumn(reservedId, curId)

  df.persist(StorageLevel.MEMORY_ONLY)

  def asGraph(label:String = "Casualty") : PropertyGraph = {
    val mapping = NodeMapping.withSourceIdKey(reservedId)
      .withImpliedLabel(label)
      .withPropertyKeys(fields:_*)

    val g = session.readFrom(CAPSNodeTable(mapping, df)).asCaps
    g.cache()
    g
  }
}
