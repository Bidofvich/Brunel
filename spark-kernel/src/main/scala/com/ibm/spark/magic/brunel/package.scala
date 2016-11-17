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

package com.ibm.spark.magic.brunel

import com.ibm.spark.kernel.api.Kernel
//import org.apache.spark.repl.SparkIMain

package object brunel {

  private var _the_kernel: Kernel = _

  def initWidgets(implicit kernel: Kernel): Unit = {
    _the_kernel = kernel
  }

  def getKernel: Kernel = {
    _the_kernel
  }

//  def sparkIMain: SparkIMain = {
//    val sparkIMainMethod = getKernel.interpreter.getClass.getMethod("sparkIMain")
//    val sparkIMain = sparkIMainMethod.invoke(getKernel.interpreter).asInstanceOf[org.apache.spark.repl.SparkIMain]
//    sparkIMain
//  }
}
