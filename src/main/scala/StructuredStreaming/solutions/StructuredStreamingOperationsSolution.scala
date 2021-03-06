package StructuredStreaming.solutions

import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{count, max}

object StructuredStreamingOperationsSolution {
	def main(args: Array[String]): Unit = {
		val spark = SparkSession
			.builder
			.appName("StructuredStreamingOperationsExercise")
			.master("local[*]")
			.getOrCreate()

		import spark.implicits._

		Logger.getRootLogger.setLevel(Level.ERROR)

		val kafkaParams = Map("metadata.broker.list" -> "localhost:9092")

		val df = spark
			.readStream
			.format("kafka")
			.option("kafka.bootstrap.servers", "localhost:9092")
			.option("subscribe", "tweets")
			.load()

		val query = df.selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)")
			.as[(String, String)]
			.map(_._2.split(":")) // Message format: id:userName:place:replyToScreenName:createdAt:length:firstHashtag
			.toDF("id", "userName", "place", "replyToScreenName", "createdAt", "length", "firstHashtag")
			.dropDuplicates("id")
			.select("createdAt / 60 AS minute", "id", "length")
			.groupBy("minute")
			.agg(count("id"), max("length"))
			.orderBy("minute")
			.writeStream.format("console")
			.queryName("streamingOperationsOutput").start

		query.awaitTermination()
	}
}
