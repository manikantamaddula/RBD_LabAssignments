import org.apache.log4j.{Level, Logger}
import org.apache.spark.{SparkContext, SparkConf}

/**
  * Created by Manikanta on 8/29/2016.
  */
object SparkWordCount {

  def main(args: Array[String]) {

    System.setProperty("hadoop.home.dir","C:\\Users\\Manikanta\\Documents\\UMKC Subjects\\PB\\hadoopforspark");

    val sparkConf = new SparkConf().setAppName("SparkWordCount").setMaster("local[*]")
    val sc=new SparkContext(sparkConf)

    // Turn off Info Logger for Console
    Logger.getLogger("org").setLevel(Level.OFF);
    Logger.getLogger("akka").setLevel(Level.OFF);

    val input=sc.textFile("input")
    val sentences=input.flatMap(line=>{line.replaceAll("\\. ",". ;;").split(";;")})
    val output=sentences.sortBy(f=>f,ascending = true,numPartitions = 1)

    output.saveAsTextFile("output")
    println("Total number of sentences: ")
    println(output.count())
    output.foreach(f=>println(f))
    sc.stop()

  }

}
