package com.stormanalytics.csvreader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

public class CsvReader {
  private static final Logger LOG = Logger.getLogger(CsvReader.class);

  public static void main(String[] args) throws InterruptedException, ExecutionException {

    if (args.length < 3) {
      LOG.fatal("Incorrect number of arguments. Required arguments: <zk-hosts> <kafka-topic> <zk-path> <clientid>");
      System.exit(1);
    }

    String dir_path = args[0];
    String broker = args[1];
    String topic = args[2];
    List<String> paths = new ArrayList<>();
    try {
      Files.newDirectoryStream(Paths.get(dir_path), path -> path.toString().endsWith(".csv")).forEach(p -> {
        paths.add(p.toString());
      });

    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    
   Collections.reverse(paths);
   
   for (String p : paths) {
     System.out.println(p);
   }
   
    int scaleFactor = 2;
    int cpus = Runtime.getRuntime().availableProcessors();
    int maxThreads = cpus * scaleFactor;
    maxThreads = (maxThreads > 0 ? maxThreads : 1);
    System.out.println("cpus: " + cpus);
    System.out.println("maxThreads: " + maxThreads);

    ExecutorService executorService = new ThreadPoolExecutor(maxThreads, // core
        // thread
        // pool
        // size
        maxThreads, // maximum thread pool size
        1, // time to wait before resizing pool
        TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(maxThreads, true),
        new ThreadPoolExecutor.CallerRunsPolicy());

    for (String p : paths) {
      executorService.submit(new KafkaProducerClient(p, broker, topic));
    }

    executorService.shutdown();

    try {
      if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
        // pool didn't terminate after the first try
        executorService.shutdownNow();
      }

      if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
        // pool didn't terminate after the second try
      }
    } catch (InterruptedException ex) {
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
    }
    //
    // ExecutorService executor = Executors.newFixedThreadPool(paths.size());
    // for (String p : paths) {
    //
    // executor.execute(new KafkaProducerClient(p, broker, topic));
    // }
    // executor.shutdown();
    // // Wait until all threads are finish
    // while (!executor.isTerminated()) {
    // Thread.sleep(500);
    // }
    // System.out.println("\nFinished all threads");

  }

}
