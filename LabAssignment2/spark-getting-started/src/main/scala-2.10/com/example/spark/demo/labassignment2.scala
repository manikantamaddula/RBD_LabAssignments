package com.example.spark.demo

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkContext, SparkConf}

/**
  * Created by Manikanta on 9/7/2016.
  */
object labassignment2 {

  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName("wordCount") .set("spark.eventLog.enabled", "true")
    System.setProperty("hadoop.home.dir", "C:/Users/Manikanta/Documents/UMKC Subjects//PB/hadoopforspark/")
    //val inputFile = args(0)
    val inputFile = "data/textfile.txt"
    //val outputFile = "C:/Users/Manikanta/IdeaProjects/wordcount"
    val outputFile = "data/wordcount"
    // Create a Scala Spark Context.
    val sc = new SparkContext(conf)
    // Load our input data.
    val input =  sc.textFile(inputFile)
    // Split up into words.
    val words = input.flatMap(line => line.split("\\W+"))
    words.foreach(f=>println(f))
    // Transform into word and count.
    val counts = words.map(words => (words, 1)).reduceByKey{case (x, y) => x + y}
    // Save the word count back out to a text file, causing evaluation.
    //counts.coalesce(1,true).saveAsTextFile(outputFile)
    val tf=counts.sortBy(f=>f._2,ascending = false)
    tf.foreach(f=>println(f))
    tf.saveAsTextFile(outputFile)

    tf.take(10).foreach(f=>println(f))
    sc.stop()

  }

}
