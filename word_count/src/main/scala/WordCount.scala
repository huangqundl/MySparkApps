/* WordCount.scala */
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf

object WordCount {
  def main(args: Array[String]) {
    val logFile = "/home/qhuang/workspace/spark-1.4.1-bin-hadoop2.6/README.md" // Should be some file on your system
    val conf = new SparkConf().setAppName("Word Count")
    val sc = new SparkContext(conf)
    val lines = sc.textFile(logFile, 2).cache()
    val words = lines.flatMap(_.split(" "))
    val wordCounts = words.map(x => (x, 1)).reduceByKey(_ + _)
    wordCounts.saveAsTextFile("/home/qhuang/workspace/spark_app/word_count/output");
  }
}
