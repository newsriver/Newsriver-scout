package ch.newsriver.scout;

import ch.newsriver.dao.JDBCPoolUtil;
import ch.newsriver.data.url.FeedURL;
import ch.newsriver.executable.Main;
import ch.newsriver.executable.poolExecution.BatchInterruptibleWithinExecutorPool;
import ch.newsriver.performance.MetricsLogger;
import ch.newsriver.scout.feed.FeedFetcher;
import ch.newsriver.scout.feed.FeedFetcherResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;

/**
 * Created by eliapalme on 10/03/16.
 */
public class Scout extends BatchInterruptibleWithinExecutorPool implements Runnable {

    private static final Logger logger = LogManager.getLogger(Scout.class);
    private static final MetricsLogger metrics = MetricsLogger.getLogger(Scout.class, Main.getInstance().getInstanceName());

    private static int  MAX_EXECUTUION_DURATION = 120;

    private static final ObjectMapper mapper = new ObjectMapper();
    private int batchSize;
    private boolean run = false;
    Producer<String, String> producer;

    public Scout(int poolSize, int batchSize, int queueSize) throws IOException {
        super(poolSize,queueSize,Duration.ofSeconds(MAX_EXECUTUION_DURATION));
        run = true;
        this.batchSize =batchSize;
        Properties props = new Properties();
        InputStream inputStream = null;
        try {

            String propFileName = "kafka.properties";
            inputStream = FeedFetcher.class.getClassLoader().getResourceAsStream(propFileName);
            if (inputStream != null) {
                props.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }
        } catch (java.lang.Exception e) {
            logger.error("Unable to load kafka properties", e);
        } finally {
            try {
                inputStream.close();
            } catch (java.lang.Exception e) {
            }
        }

        producer = new KafkaProducer(props);
    }

    public void stop() {
        run = false;
        this.shutdown();
        producer.close();
        metrics.logMetric("shutdown");
    }


    public void run(){
        metrics.logMetric("start");
        int count = 0;
        String sqlCount = "Select count(url) from feed";
        try (Connection conn = JDBCPoolUtil.getInstance().getConnection(JDBCPoolUtil.DATABASES.Sources); PreparedStatement stmt = conn.prepareStatement(sqlCount);) {
            try (ResultSet rs = stmt.executeQuery();) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        int from = 0;
        while (run) {

            try {
                this.waitFreeBatchExecutors(this.batchSize);
                metrics.logMetric("processing batch");

                List<String> urls = new LinkedList();
                String sql = "Select url from feed limit ?,?";
                try (Connection conn = JDBCPoolUtil.getInstance().getConnection(JDBCPoolUtil.DATABASES.Sources); PreparedStatement stmt = conn.prepareStatement(sql);) {
                    stmt.setInt(1, from);
                    stmt.setInt(2, this.batchSize);

                    from += this.batchSize;
                    from = from>count?0:from;

                    try (ResultSet rs = stmt.executeQuery();) {
                        while (rs.next()) {
                            urls.add(rs.getString("url"));
                        }
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                for (String url : urls) {
                    metrics.logMetric("processing feed");
                    CompletableFuture x = supplyAsyncInterruptExecutionWithin(() ->
                    {
                        FeedFetcher fetcher =  new FeedFetcher(url);
                        FeedFetcherResult result = fetcher.fetch();
                        ScoutMain.addMetric("Scanned Feeds",1);
                        return  result;
                    },this)
                            .thenAcceptAsync(result -> {
                                        if(result!=null) {
                                            for (FeedURL feedURL : result.getUrls()) {
                                                try {
                                                    String json = mapper.writeValueAsString(feedURL);
                                                    producer.send(new ProducerRecord<String, String>("raw-urls", feedURL.getUlr(), json));
                                                    metrics.logMetric("submitted url");
                                                    ScoutMain.addMetric("Submitted URLs",1);
                                                } catch (Exception e) {
                                                    logger.fatal("Unable to serialize scout result", e);
                                                }
                                            }
                                        }
                                    }
                                    , this)
                            .exceptionally(throwable -> {
                                logger.error("FeedFetcher unrecoverable error.", throwable);
                                return null;
                            });
                }


             } catch (InterruptedException ex) {
                    logger.warn("Scout job interrupted", ex);
                    run = false;
                return;
            } catch (BatchSizeException ex){
                logger.fatal("Requested a batch size bigger than pool capability.");
            }
        }
    }



}
