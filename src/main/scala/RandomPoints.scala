package spark.perf

import scala.math
import scala.util.Random
import spark.util.Vector
import spark.SparkContext
import spark.SparkContext._

object RandomPoints {
  /**
    * Generates a random point where each element
    * is a value between -range to range
    */
  def generatePoint(dim: Int, range: Double) = {
    Vector(dim, Int => math.random * 2 * range - range)
  }

  /**
    * Returns rdd of points around random centers.
    * Points in a cluster are uniformly distributed.
    *
    * @param numClusters approximate number of clusters
    * @param numPoints approximate total number of points
    * @param dim number of dimensions for each point
    * @param range elements of generated centers are between -range to range
    * @param radius how widely spread each cluster is
    */
  def generateClusteredPoints(sc: SparkContext, numClusters: Int, numPoints: Int, 
    dim: Int, range: Double, radius: Double, numPartitions: Int) = {

    // Assuming a small number of clusters, so just generate centers locally
    val centers = (0 until numClusters).map { i => generatePoint(dim, range) }
    
    sc.parallelize(1 to numPartitions, numPartitions).flatMap { partition =>
      val numPointsPerPartition = numPoints/numPartitions
      
      (0 until numPointsPerPartition).map { i =>
        val center = centers(Random.nextInt(numClusters))
        center + generatePoint(dim, radius)
      }
    }  
  }
  
  def generateClusteredPoints(sc: SparkContext, numClusters: Int, numPoints: Int, numPartitions: Int): 
    spark.RDD[Vector] = {
    generateClusteredPoints(sc: SparkContext, numClusters, numPoints, 10, 10, 1, numPartitions)
  }  
  
  def main(args: Array[String]) {
    val sparkHome = System.getenv("SPARK_HOME")
    val jars = List(System.getenv("SPARK_PERF"))
    val sc = new SparkContext(args(0), "Random Points", sparkHome, jars)
    val numPoints = args(1).toInt
    val dim = args(2).toInt
    val range = args(3).toDouble
    val outputDir = args(5)
    generateClusteredPoints(sc, 1, numPoints, dim, 0, range, 10).saveAsTextFile(outputDir)
    sc.stop()
  }
}
