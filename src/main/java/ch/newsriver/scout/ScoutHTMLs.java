package ch.newsriver.scout;

import ch.newsriver.dao.ElasticsearchPoolUtil;
import ch.newsriver.data.content.Article;
import ch.newsriver.data.html.HTML;
import ch.newsriver.data.source.BaseSource;
import ch.newsriver.data.source.FeedSource;
import ch.newsriver.data.source.SourceFactory;
import ch.newsriver.data.source.URLSeedSource;
import ch.newsriver.data.url.*;
import ch.newsriver.executable.Main;
import ch.newsriver.executable.poolExecution.BatchInterruptibleWithinExecutorPool;
import ch.newsriver.performance.MetricsLogger;
import ch.newsriver.scout.cache.ResolvedURLs;
import ch.newsriver.scout.cache.ScannedURLs;
import ch.newsriver.scout.feed.FeedFetcher;
import ch.newsriver.scout.feed.FeedFetcherResult;
import ch.newsriver.scout.url.URLResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.webkit.ThemeClient;
import org.apache.commons.codec.binary.*;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Created by eliapalme on 10/03/16.
 */
public class ScoutHTMLs extends BatchInterruptibleWithinExecutorPool implements Runnable {

    private static final Logger logger = LogManager.getLogger(ScoutHTMLs.class);
    private static final MetricsLogger metrics = MetricsLogger.getLogger(ScoutHTMLs.class, Main.getInstance().getInstanceName());
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private static int MAX_EXECUTUION_DURATION = 120;


    private static final ObjectMapper mapper = new ObjectMapper();
    private int batchSize;
    private boolean run = false;
    Consumer<String, String> consumer;
    Producer<String, String> producer;

    public ScoutHTMLs(int poolSize, int batchSize, int queueSize) throws IOException {
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


        consumer = new KafkaConsumer(props);
        consumer.subscribe(Arrays.asList("seed-html"));
        producer = new KafkaProducer(props);

    }

    public void stop() {
        run = false;
        this.shutdown();
        consumer.close();
        metrics.logMetric("shutdown", null);
    }


    public void run() {
        metrics.logMetric("start", null);


        while (run) {

            try {
                this.waitFreeBatchExecutors(this.batchSize);
                //TODO: decide if we should keep this
                //metrics.logMetric("processing batch",null);

                ConsumerRecords<String, String> records = consumer.poll(60000);
                for (ConsumerRecord<String, String> record : records) {

                    supplyAsyncInterruptExecutionWithin(() -> {


                        HTML html = null;
                        try {
                            html = mapper.readValue(record.value(), HTML.class);
                        } catch (IOException e) {
                            logger.error("Error deserialising HTML", e);
                            return null;
                        }
                        final SeedURL seedURL;
                        try {
                            seedURL = mapper.readValue(record.key(), SeedURL.class);
                        } catch (IOException e) {
                            logger.error("Error deserialising URLSeedSource", e);
                            return null;
                        }

                        URI referral = null;
                        try {
                            referral = new URI(seedURL.getReferralURL());
                        } catch (Exception e) {
                            logger.error("Unvalid SeedURL", e);
                            return null;
                        }

                        if (ScannedURLs.getInstance().isVisited(referral.getHost(), seedURL.getUrl())) {

                            //TODO: her we may want to update an existing document ans set the
                            //seenSince date. This will allow us to compute how long an article has been featured.


                            Client client = null;
                            client = ElasticsearchPoolUtil.getInstance().getClient();

                            String urlHash = "";
                            try {
                                MessageDigest digest = MessageDigest.getInstance("SHA-512");
                                byte[] hash = digest.digest(html.getUrl().getBytes(StandardCharsets.UTF_8));
                                urlHash = org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(hash);

                                GetResponse response = client.prepareGet("newsriver", "article", urlHash).execute().actionGet();
                                if (response.isExists()) {

                                    Article article = mapper.readValue(response.getSourceAsString(), Article.class);

                                    if (article != null) {
                                        //Check if the article already contains this

                                        if (referral != null) {
                                            boolean notFound = article.getReferrals().stream().noneMatch(baseURL -> seedURL.getReferralURL() != null && baseURL.getReferralURL().equals(seedURL.getReferralURL()));
                                            if (notFound) {

                                                LinkURL linkURL = new LinkURL();
                                                linkURL.setUrl(html.getUrl());
                                                linkURL.setReferralURL(html.getReferral().getReferralURL());
                                                linkURL.setDiscoverDate(dateFormatter.format(new Date()));
                                                linkURL.setRawURL(html.getReferral().getRawURL());

                                                article.getReferrals().add(linkURL);
                                                IndexRequest indexRequest = new IndexRequest("newsriver", "article", urlHash);
                                                indexRequest.source(mapper.writeValueAsString(article));
                                                client.index(indexRequest).actionGet();
                                            }
                                        }

                                    }
                                }


                            } catch (NoSuchAlgorithmException e) {
                                logger.fatal("Unable to compute URL hash", e);
                                return null;
                            } catch (IOException e) {
                                logger.fatal("Unable to deserialize article", e);
                                return null;
                            } catch (Exception e) {
                                logger.error("Unable to get article from elasticsearch", e);
                                return null;
                            }


                        } else {
                            ScannedURLs.getInstance().setVisited(referral.getHost(), seedURL.getUrl());

                            //TODO: run classifier to establish if HTML contains and article
                            LinkURL linkURL = new LinkURL();
                            linkURL.setUrl(seedURL.getUrl());
                            linkURL.setReferralURL(seedURL.getReferralURL());
                            linkURL.setDiscoverDate(dateFormatter.format(new Date()));
                            linkURL.setRawURL(seedURL.getRawURL());
                            try {
                                String json = mapper.writeValueAsString(linkURL);
                                producer.send(new ProducerRecord<String, String>("raw-urls", linkURL.getUrl(), json));
                                metrics.logMetric("submitted url", seedURL);
                                ScoutMain.addMetric("Submitted URLs", 1);
                            } catch (Exception e) {
                                logger.fatal("Unable to serialize seedURL", e);
                            }

                        }

                        //we only scan links of the index seed url
                        if (seedURL.getDepth() != 0) {

                            return null;
                        }

                        Document doc = Jsoup.parse(html.getRawHTML(), html.getUrl());
                        Elements links = doc.select("a[href]");

                        Set<String> seenURLs = new HashSet<String>();

                        //add seedURL to skip aouto references
                        seenURLs.add(seedURL.getUrl());
                        seenURLs.add(seedURL.getUrl() + "/");
                        seenURLs.add(seedURL.getRawURL());

                        for (Element link : links) {
                            String urlStr = link.attr("abs:href");

                            if (urlStr == null || urlStr.isEmpty()) {
                                continue;
                            }
                            //if URL has already been seen continue to next
                            if (!seenURLs.add(urlStr)) {
                                continue;
                            }

                            //Test URL is not valid continue
                            URI url = null;
                            try {
                                url = new URI(urlStr);
                            } catch (Exception e) {
                                continue;
                            }

                            //Skip url that are not the same host as the referring URL
                            if (!url.getHost().equalsIgnoreCase(referral.getHost())) {
                                continue;
                            }


                            String resolvedURL = ResolvedURLs.getInstance().getResolved(urlStr);
                            if (resolvedURL == null) {
                                try {
                                    resolvedURL = URLResolver.getInstance().resolveURL(urlStr);
                                    //Throttling resove
                                    //Thread.sleep(1000);
                                } catch (URLResolver.InvalidURLException e) {
                                    logger.error("Unable to resolve URL", e);
                                    continue;
                                }
                                ResolvedURLs.getInstance().setResolved(urlStr, resolvedURL);
                            }

                            if (!resolvedURL.equalsIgnoreCase(urlStr)) {
                                //if the resolved url is different check if we seen it before
                                if (!seenURLs.add(resolvedURL)) {
                                    continue;
                                }
                            }

                            SeedURL nextSeed = new SeedURL();
                            nextSeed.setRawURL(urlStr);
                            nextSeed.setUrl(resolvedURL);
                            nextSeed.setReferralURL(html.getUrl());
                            nextSeed.setDepth(nextSeed.getDepth() + 1);

                            try {
                                String json = mapper.writeValueAsString(nextSeed);
                                producer.send(new ProducerRecord<String, String>("raw-urls", nextSeed.getUrl(), json));
                            } catch (Exception e) {
                                logger.fatal("Unable to serialize seedURL result", e);
                            }
                        }


                        return null;
                    }, this)
                            .exceptionally(throwable -> {
                                logger.error("HTMLScout unrecoverable error.", throwable);
                                return null;
                            });

                }


            } catch (InterruptedException ex) {
                logger.warn("ScoutHTML job interrupted", ex);
                run = false;
                return;
            } catch (BatchSizeException ex) {
                logger.fatal("Requested a batch size bigger than pool capability.");
            }
        }
    }


}
