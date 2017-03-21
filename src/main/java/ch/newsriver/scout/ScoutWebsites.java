package ch.newsriver.scout;

import ch.newsriver.data.url.FeedURL;
import ch.newsriver.data.url.SeedURL;
import ch.newsriver.data.url.SourceRSSURL;
import ch.newsriver.data.website.WebSiteFactory;
import ch.newsriver.data.website.source.BaseSource;
import ch.newsriver.data.website.source.FeedSource;
import ch.newsriver.data.website.source.SourceFactory;
import ch.newsriver.data.website.source.URLSeedSource;
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
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * Created by eliapalme on 10/03/16.
 */
public class ScoutWebsites extends BatchInterruptibleWithinExecutorPool implements Runnable {

    private static final Logger logger = LogManager.getLogger(ScoutWebsites.class);
    private static final MetricsLogger metrics = MetricsLogger.getLogger(ScoutWebsites.class, Main.getInstance().getInstanceName());
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static final ObjectMapper mapper = new ObjectMapper();
    private static int MAX_EXECUTUION_DURATION = 120;
    Producer<String, String> producer;
    private int batchSize;
    private boolean run = false;

    public ScoutWebsites(int poolSize, int batchSize, int queueSize) throws IOException {
        super(poolSize, queueSize, Duration.ofSeconds(MAX_EXECUTUION_DURATION));
        run = true;
        this.batchSize = batchSize;
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
        } catch (Exception e) {
            logger.error("Unable to load kafka properties", e);
        } finally {
            try {
                inputStream.close();
            } catch (Exception e) {
            }
        }


        producer = new KafkaProducer(props);
    }

    public void stop() {
        run = false;
        this.shutdown();
        producer.close();
        metrics.logMetric("shutdown", null);
    }


    public void run() {
        metrics.logMetric("start", null);


        while (run) {

            try {
                this.waitFreeBatchExecutors(this.batchSize);
                //TODO: decide if we should keep this
                //metrics.logMetric("processing batch",null);


                HashMap<String, BaseSource> sources = WebSiteFactory.getInstance().nextWebsiteSourcesToVisits();

                for (String id : sources.keySet()) {

                    BaseSource source = sources.get(id);
                    CompletableFuture x = supplyAsyncInterruptExecutionWithin(() ->
                    {

                        WebSiteFactory.getInstance().updateSourceLastVisit(id, source);

                        if (source instanceof FeedSource) {


                            SourceRSSURL processingSource = new SourceRSSURL();
                            processingSource.setUrl(source.getUrl());
                            processingSource.setDiscoverDate(dateFormatter.format(new Date()));
                            metrics.logMetric("processing feed", processingSource);

                            FeedFetcher fetcher = new FeedFetcher(source.getUrl());
                            FeedFetcherResult result = fetcher.fetch();
                            ScoutMain.addMetric("Scanned Feeds", 1);

                            if (result != null) {
                                for (FeedURL feedURL : result.getUrls()) {
                                    feedURL.setCategory(((FeedSource) source).getCategory());
                                    feedURL.setCountry(((FeedSource) source).getCountryName());
                                    feedURL.setRegion(((FeedSource) source).getRegion());
                                    try {
                                        String json = mapper.writeValueAsString(feedURL);
                                        producer.send(new ProducerRecord<String, String>("raw-urls", feedURL.getUrl(), json));
                                        metrics.logMetric("submitted url", feedURL);
                                        ScoutMain.addMetric("Submitted URLs", 1);
                                    } catch (Exception e) {
                                        logger.fatal("Unable to serialize feedURL", e);
                                    }
                                }
                            }

                            return null;
                        } else if (source instanceof URLSeedSource) {


                            //TODO: implement remove source in WebSiteFactory
                            /*if (!((URLSeedSource) source).isPermanent()) {
                                WebSiteFactory.getInstance().removeSource(source);
                            }*/


                            SeedURL seedUR = new SeedURL();
                            seedUR.setRawURL(source.getUrl());
                            seedUR.setUrl(source.getUrl());
                            seedUR.setReferralURL(((URLSeedSource) source).getReferralURL());
                            seedUR.setDepth(((URLSeedSource) source).getDepth());
                            seedUR.setCategory(((URLSeedSource) source).getCategory());
                            seedUR.setCountryCode(((URLSeedSource) source).getCountryCode());
                            seedUR.setCountryName(((URLSeedSource) source).getCountryName());
                            seedUR.setLanguageCode(((URLSeedSource) source).getLanguageCode());
                            seedUR.setRegion(((URLSeedSource) source).getRegion());

                            try {
                                String json = mapper.writeValueAsString(seedUR);
                                producer.send(new ProducerRecord<String, String>("raw-urls", source.getUrl(), json));
                            } catch (Exception e) {
                                logger.fatal("Unable to serialize seedURL result", e);
                            }

                            return null;
                        } else {
                            return null;
                        }
                    }, this).exceptionally(throwable -> {
                        logger.error("FeedFetcher unrecoverable error.", throwable);
                        return null;
                    });
                }


            } catch (InterruptedException ex) {
                logger.warn("ScoutSources job interrupted", ex);
                run = false;
                return;
            } catch (BatchSizeException ex) {
                logger.fatal("Requested a batch size bigger than pool capability.");
            }
        }
    }


}
