package com.stormanalytics.csvreader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

public class KafkaProducerClient implements Runnable {

  String path   = "";
  String broker = "";
  String topic  = "";

  KafkaProducerClient(String path, String broker, String topic) {
    this.path = path;
    this.broker = broker;
    this.topic = topic;
  }

  @Override
  public void run() {
    BufferedReader br = null;
    String line = "";

    Properties props = new Properties();
    props.put("metadata.broker.list", broker);
    props.put("serializer.class", "kafka.serializer.StringEncoder");
    props.put("key.serializer", "org.apache.kafka.common.serializa-tion.StringSerializer");
    props.put("value.serializer", "org.apache.kafka.common.serializa-tion.StringSerializer");
    props.put("request.required.acks", "1");

    ProducerConfig config = new ProducerConfig(props);

    Producer<String, String> producer = new Producer<String, String>(config);

    try {

      br = new BufferedReader(new FileReader(path));
      while ((line = br.readLine()) != null) {

        KeyedMessage<String, String> data = new KeyedMessage<String, String>(topic, line);
        if(!line.contains("EVENT_ID")){          
          producer.send(data);
        }
        try {
          Thread.sleep(50);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      producer.close();

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

}
