/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import scala.io.Source
import scala.util.control.Breaks._

import org.apache.spark.{SparkConf}
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.receiver.Receiver

import org.apache.spark.Logging
import org.apache.log4j.{Level, Logger}

import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

/**
 * Custom Receiver that receives data over a socket. Received bytes is interpreted as
 * text and \n delimited lines are considered as records. They are then counted and printed.
 *
 * To run this on your local machine, you need to first run a Netcat server
 *    `$ nc -lk 9999`
 * and then run the example
 *    `$ bin/run-example org.apache.spark.examples.streaming.CustomReceiver localhost 9999`
 */
object WordCountStream {
  def main(args: Array[String]) {
    if (args.length < 2) {
      System.err.println("Usage: WordCountStream <filename> <freq>")
      System.exit(1)
    }

    StreamingExamples.setStreamingLogLevels()

    // Create the context with a 1 second batch size
    val sparkConf = new SparkConf().setAppName("WordCountStream")
    val ssc = new StreamingContext(sparkConf, Seconds(1))

    // Create a input stream with the custom receiver on target ip:port and count the
    // words in input stream of \n delimited text (eg. generated by 'nc')
    val t0 = System.nanoTime : Double
    val lines = ssc.receiverStream(new CustomReceiver(args(0), args(1).toInt))
    val words = lines.flatMap(_.split(" "))
    val wordCounts = words.map(x => (x, 1)).reduceByKey(_ + _)
    val t1 = System.nanoTime : Double
    println("Elapsed time " + (t1 - t0) / 1000000.0 + " msecs")
    wordCounts.print()

    ssc.start()
    ssc.awaitTermination()
  }
}


class CustomReceiver(filename: String, max_line: Int)
  extends Receiver[String](StorageLevel.MEMORY_AND_DISK_2) with Logging {

  //val max_line = 10000
  val line_array = new Array[String](max_line)

  def onStart() {
    // Start the thread that receives data over a connection
    new Thread("Socket Receiver") {
      override def run() {
          receive()
      }
    }.start()
  }

  def onStop() {
   // There is nothing much to do as the thread calling receive()
   // is designed to stop by itself isStopped() returns false
  }

  /** Create a socket connection and receive data until receiver is stopped */
  private def receive() {

    /*
    //val source = Source.fromFile(filename)
    val source = Source.fromFile("/home/qhuang/dataset/word_count/input_small.txt")
    var num_line = 0
    for (line <- source.getLines) {
        line_array(num_line) = line
        num_line = num_line + 1;
        if (num_line == 10) {
            break
        }
    }
    for (line <- source.getLines) {
        store(line);
    }
    */

    for (i <- 0 to max_line-1) {
        line_array(i) = i.toString
    }
    while (true) {
        for (i <- 0 to max_line-1) {
            store(line_array(i))
        }
        Thread.sleep(1000)
    }
  }
}

object StreamingExamples extends Logging {

  /** Set reasonable logging levels for streaming if the user has not configured log4j. */
  def setStreamingLogLevels() {
    val log4jInitialized = Logger.getRootLogger.getAllAppenders.hasMoreElements
    if (!log4jInitialized) {
      // We first log something to initialize Spark's default logging, then we override the
      // logging level.
      logInfo("Setting log level to [WARN] for streaming example." +
        " To override add a custom log4j.properties to the classpath.")
      Logger.getRootLogger.setLevel(Level.WARN)
    }
  }
}
