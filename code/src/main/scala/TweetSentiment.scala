import java.util.HashMap

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.spark.streaming.twitter._
import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}


object TweetSentiment {
  def main(args: Array[String]): Unit = {

    if (args.length != 1) {
      System.err.println("Usage: spark-submit --packages org.apache.spark:spark-sql-kafka-0-10_2.11:2.4.0 --class TweetSentiment <jar Path> <topic>")
      System.exit(1)
    }

    // Set logging level if log4j not configured (override by adding log4j.properties to classpath)
    if (!Logger.getRootLogger.getAllAppenders.hasMoreElements) {
      Logger.getRootLogger.setLevel(Level.ERROR)
    }

    val consumerKey = "kIWBkJbg4GAZgVMFH5Wt9JRBq"
    val consumerSecret = "u5sx6xZj3LnGV1nFmffRJUd6skf6dqYnrNcpkCwpIRNIGmP7jQ"
    val accessToken = "1152983982692323328-Npdb3sY1n5IzsCzqXFkQYicCwIld3M"
    val accessTokenSecret = "ylRg3aKgKrdZ6zo0PgN3N07lwWdpGuE39I780WGEfnxNZ"

    System.setProperty("twitter4j.oauth.consumerKey", consumerKey)
    System.setProperty("twitter4j.oauth.consumerSecret", consumerSecret)
    System.setProperty("twitter4j.oauth.accessToken", accessToken)
    System.setProperty("twitter4j.oauth.accessTokenSecret", accessTokenSecret)

    //    val sparkConf = new SparkConf().setAppName("TwitterLocations").set("spark.driver.allowMultipleContexts", "true")
    val sparkConf = new SparkConf().
      setAppName("streaming-tweets-sentiment").
      setMaster(sys.env.getOrElse("spark.master", "local[*]"))


    val kafkaBrokers = "localhost:9092"
    val filters = Seq(args(0))
    val topic = args(0)

    val ssc = new StreamingContext(sparkConf, Seconds(5))

    val tweetStream = TwitterUtils.createStream(ssc, None, filters)
    val sentiments = tweetStream.filter(x => x.getLang == "en").map(t => SentimentAnalyzer.mainSentiment(t.getText))

    // write output to screen
    //    sentiments.print()

    // send data to Kafka broker

    sentiments.foreachRDD(rdd => {

      rdd.foreachPartition(partition => {
        // Print statements in this section are shown in the executor's stdout logs
        val props = new HashMap[String, Object]()
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers)
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
        val producer = new KafkaProducer[String, String](props)
        partition.foreach(record => {
          val data = record.toString
          val message = new ProducerRecord[String, String](topic, null, data)
          producer.send(message)
        })
        producer.close()
      })

    })

    // Now that the streaming is defined, start it
    ssc.start()
    // Let's await the stream to end - forever
    ssc.awaitTermination()
  }
}
