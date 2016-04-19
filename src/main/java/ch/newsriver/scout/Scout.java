package ch.newsriver.scout;

import ch.newsriver.dao.JDBCPoolUtil;
import ch.newsriver.data.url.BaseURL;
import ch.newsriver.data.url.FeedURL;
import ch.newsriver.data.url.SourceRSSURL;
import ch.newsriver.executable.Main;
import ch.newsriver.executable.poolExecution.BatchInterruptibleWithinExecutorPool;
import ch.newsriver.performance.MetricsLogger;
import ch.newsriver.scout.cache.ResolvedURLs;
import ch.newsriver.scout.cache.VisitedURLs;
import ch.newsriver.scout.feed.FeedFetcher;
import ch.newsriver.scout.feed.FeedFetcherResult;
import ch.newsriver.scout.url.URLResolver;
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
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
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
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

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
        metrics.logMetric("shutdown",null);
    }


    public void run(){
        metrics.logMetric("start",null);
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
                //TODO: decide if we should keep this
                //metrics.logMetric("processing batch",null);

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
                    CompletableFuture x = supplyAsyncInterruptExecutionWithin(() ->
                    {
                        SourceRSSURL source = new SourceRSSURL();
                        source.setUlr(url);
                        source.setDiscoverDate(dateFormatter.format(new Date()));
                        metrics.logMetric("processing feed", source);

                        FeedFetcher fetcher =  new FeedFetcher(url);
                        FeedFetcherResult result = fetcher.fetch();
                        ScoutMain.addMetric("Scanned Feeds",1);

                        if(result!=null) {
                            for (FeedURL feedURL : result.getUrls()) {

                                String resolvedURL = ResolvedURLs.getInstance().getResolved(feedURL.getUlr());
                                if (resolvedURL == null) {
                                    try {
                                        resolvedURL = URLResolver.getInstance().resolveURL(feedURL.getUlr());
                                    } catch (URLResolver.InvalidURLException e) {
                                        logger.error("Unable to resolve URL", e);
                                        return null;
                                    }
                                    ResolvedURLs.getInstance().setResolved(feedURL.getUlr(), resolvedURL);
                                }
                                feedURL.setUlr(resolvedURL);

                                //Skip articles that have been previously visited.
                                if (VisitedURLs.getInstance().isVisited(url, feedURL.getUlr())) {
                                    continue;
                                }
                                VisitedURLs.getInstance().setVisited(url, feedURL.getUlr());

                                try {
                                    String json = mapper.writeValueAsString(feedURL);
                                    producer.send(new ProducerRecord<String, String>("raw-urls", feedURL.getUlr(), json));
                                    metrics.logMetric("submitted url",feedURL);
                                    ScoutMain.addMetric("Submitted URLs",1);
                                } catch (Exception e) {
                                    logger.fatal("Unable to serialize scout result", e);
                                }
                            }
                        }

                        return  result;
                    },this).exceptionally(throwable -> {
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
