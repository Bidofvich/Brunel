/*
 * Copyright (c) 2015 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
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

package org.brunel.scala

import org.apache.spark._
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.Row

import org.brunel.data.Dataset
import org.brunel.data.Field
import org.brunel.util.D3Integration
import org.brunel.util.BrunelD3Result
import org.brunel.data.io.CSV
import org.brunel.build.util.BuilderOptions

/**
 * Creation of Brunel output for Spark DataFrames
 */
object Brunel {

  //Brunel numeric and date column type conversions from a Spark DataFrame
  val numericTypes = List("IntegerType", "LongType", "DoubleType", "FloatType", "DecimalType")
  val dateTypes = List("DateType", "TimestampType")
  var options = BuilderOptions.makeFromENV();


  /**
   * Create Brunel notebook output from a Spark DataFrame using the provided Brunel source.  This output is currently
   * a D3 visualization.
   *
   */
  def create(df:DataFrame, brunelSrc: String, width: Int, height: Int, visId: String, controlsId: String): BrunelOutput = {
    val dataset = makeDataset(df)
    val builder = D3Integration.makeD3(dataset, brunelSrc, width, height, visId, controlsId)
    new BrunelOutput(builder.getVisualization.toString, builder.getStyleOverrides, builder.getControls)
  }
  
  /**
   * Get the names of all datasets specified in the brunel.
   */
  def getDatasetNames(brunel:String) : Array[String] = {
    return D3Integration.getDatasetNames(brunel)
  }
  
  /**
   * Cache the contents of a DataFrame using their named references in the data() statement 
   */
  def cacheData(dataKey:String, df:DataFrame) = {
    if (df != null) D3Integration.cacheData(dataKey, makeDataset(df))
  }

  //Create a Brunel Dataset from a Spark DataFrame
  def makeDataset(df: DataFrame): Dataset = {
    if (df == null) return null;
    val cols = df.columns
    val rows = df.collect()
    val dtypes = df.dtypes
    val fields = new Array[Field](cols.length)

    for (i <- 0 to cols.length - 1) {
      val name = CSV.identifier(cols(i).trim)   //Brunel-friendly field name from spark column name
      val label = CSV.identifier(cols(i).trim)  //Brunel-friendly label
      val dataType = dtypes(i)._2.split("\\(")(0)
      val provider = makeProvider(dataType, i, rows)
      fields(i) = new Field(name, label, provider)
      addTypeInfo(fields(i), dataType)
    }

    return Dataset.make(fields, false)

  }

  //Creates a SparkDataProvider that will return the appropriate data value type needed by Brunel
  def makeProvider(colType: String, index: Int, rows: Array[Row]): SparkDataProvider[Any] = {

    if (numericTypes.contains(colType)) {
      return new SparkDataProvider[Double](index, rows)
    } else if (dateTypes.contains(colType)) {
      return new SparkDataProvider[java.sql.Date](index, rows)
    } else return new SparkDataProvider[String](index, rows);
  }

  //Adds field type information to a Field based on the types defined in the Spark DataFrame
  def addTypeInfo(field: Field, colType: String) {

    if (numericTypes.contains(colType)) {
      field.set("numeric", true)
    }

    if (dateTypes.contains(colType)) {
      field.set("date", true)
      field.set("numeric", true)
    }

  }

}
