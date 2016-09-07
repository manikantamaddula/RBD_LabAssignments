package com.example.spark.demo

/**
  * Created by Manikanta on 3/9/2016.
  */


import org.apache.spark
import org.apache.spark._
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext._


object SparkDemo {
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
    // Transform into word and count.
    val counts = words.map(words => (words, 1)).reduceByKey{case (x, y) => x + y}
    // Save the word count back out to a text file, causing evaluation.
    //counts.coalesce(1,true).saveAsTextFile(outputFile)
    counts.saveAsTextFile(outputFile)
    sc.stop()


  }
}
