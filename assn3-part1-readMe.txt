Assignmen 3 - part 1

How to run:
1. start a zookeeper server
Locate to the folder of kafka, and run the following command:
bin/zookeeper-server-start.sh config/zookeeper.properties

2. start a Kafka server
Locate to the folder of kafka, and run the following command:
bin/kafka-server-start.sh config/server.properties

3. start elasticSearch
Locate to the folder of elasticSearch, and run the following command:
bin/elasticSearch

4. start kibana
Locate to the folder of kibana, and run the following command:
bin/kibana

5. start logstash
	5.1 Locate to the folder of logstash, create a config file at the root of the folder, named logstash-simple.conf with following content. Suppose we would like the analyze the tweets about iphone:
	input { 
		kafka { 
			bootstrap_servers => "localhost:9092" 
			topics => ["iphone"] 
		} 
	}

	output { 
		elasticsearch { 
			hosts => ["localhost:9200"] 
			index => "iphone-index" 
		} 
	}

	5.2 run the following command:
	bin/logstash -f logstash-simple.conf

6. run spark
Go to the spark folder, and run the following command:
bin/spark-submit --packages org.apache.spark:spark-sql-kafka-0-10_2.11:2.4.0 --class TweetSentiment <jar Path> iphone

7. visualize the result
Go to http://localhost:5601 to visualize the results