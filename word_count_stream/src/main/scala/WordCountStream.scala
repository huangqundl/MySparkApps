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

import java.io.File
import java.nio.charset.Charset

import com.google.common.io.Files

import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.{Time, Seconds, StreamingContext}
import org.apache.spark.storage.StorageLevel
//import org.apache.spark.util.IntParam

import org.apache.spark.Logging
import org.apache.log4j.{Level, Logger}

/**
 * Counts words in text encoded with UTF8 received from the network every second.
 *
 * Usage: WordCountStream <hostname> <port> <checkpoint-directory> <output-file>
 *   <hostname> and <port> describe the TCP server that Spark Streaming would connect to receive
 *   data. <checkpoint-directory> directory to HDFS-compatible file system which checkpoint data
 *   <output-file> file to which the word counts will be appended
 *
 * <checkpoint-directory> and <output-file> must be absolute paths
 *
 * To run this on your local machine, you need to first run a Netcat server
 *
 *      `$ nc -lk 9999`
 *
 * and run the example as
 *
 *      `$ ./bin/run-example org.apache.spark.examples.streaming.WordCountStream \
 *              localhost 9999 ~/checkpoint/ ~/out`
 *
 * If the directory ~/checkpoint/ does not exist (e.g. running for the first time), it will create
 * a new StreamingContext (will print "Creating new context" to the console). Otherwise, if
 * checkpoint data exists in ~/checkpoint/, then it will create StreamingContext from
 * the checkpoint data.
 *
 * Refer to the online documentation for more details.
 */
object WordCountStream {

  def createContext(ip: String, port: Int, checkpointDirectory: String,
        num_receiver: Int, interval: Int)
    : StreamingContext = {

    // If you do not see this printed, that means the StreamingContext has been loaded
    // from the new checkpoint
    //val outputFile = new File(outputPath)
    //if (outputFile.exists()) outputFile.delete()

    println("Creating new context")
    val sparkConf = new SparkConf().setAppName("WordCountStream")
    // Create the context with a 1 second batch size
    val ssc = new StreamingContext(sparkConf, Seconds(interval))
    ssc.checkpoint(checkpointDirectory)

    val receivers = (0 to num_receiver-1).map(i => ssc.socketTextStream(ip, port+i, StorageLevel.MEMORY_AND_DISK_SER))
    val unionedStreaming = ssc.union(receivers)  
    val words = unionedStreaming.flatMap(_.split(" "))

    //val words = lines.flatMap(_.split(" "))
    val wordCounts = words.map(x => (x, 1)).reduceByKey(_ + _)
    wordCounts.foreachRDD((rdd: RDD[(String, Int)], time: Time) => {
      val counts = "Counts at time " + time + " " + rdd.collect().mkString("[", ", ", "]")
      //println(counts)
      //println("Appending to " + outputFile.getAbsolutePath)
      //Files.append(counts + "\n", outputFile, Charset.defaultCharset())
    })
    ssc
  }

  def main(args: Array[String]) {
    if (args.length != 5) {
      System.err.println("You arguments were " + args.mkString("[", ", ", "]"))
      System.err.println(
        """
          |Usage: WordCountStream <hostname> <port> <checkpoint-directory> <interval>
          |     <output-file>. <hostname> and <port> describe the TCP server that Spark
          |     Streaming would connect to receive data. <checkpoint-directory> directory to
          |     HDFS-compatible file system which checkpoint data <output-file> file to which the
          |     word counts will be appended
          |
          |In local mode, <master> should be 'local[n]' with n > 1
          |Both <checkpoint-directory> and <output-file> must be absolute paths
        """.stripMargin
      )
      System.exit(1)
    }
    StreamingExamples.setStreamingLogLevels()
    val Array(ip, IntParam(port), checkpointDirectory, IntParam(num_receiver), IntParam(interval)) = args
    val ssc = StreamingContext.getOrCreate(checkpointDirectory,
      () => {
        createContext(ip, port, checkpointDirectory, num_receiver, interval)
      })
    ssc.start()
    ssc.awaitTermination()
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

object IntParam {
  def unapply(str: String): Option[Int] = {
    try {
      Some(str.toInt)
    } catch {
      case e: NumberFormatException => None
    }
  }
}
