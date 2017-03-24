package ch.newsriver.scout;

import ch.newsriver.dao.ElasticsearchUtil;
import ch.newsriver.data.content.Article;
import ch.newsriver.data.content.ArticleFactory;
import ch.newsriver.data.html.AjaxHTML;
import ch.newsriver.data.html.HTML;
import ch.newsriver.data.url.BaseURL;
import ch.newsriver.data.url.LinkURL;
import ch.newsriver.data.url.SeedURL;
import ch.newsriver.executable.Main;
import ch.newsriver.executable.poolExecution.BatchInterruptibleWithinExecutorPool;
import ch.newsriver.performance.MetricsLogger;
import ch.newsriver.scout.cache.ResolvedURLs;
import ch.newsriver.scout.cache.ScannedURLs;
import ch.newsriver.scout.feed.FeedFetcher;
import ch.newsriver.scout.url.URLResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Created by eliapalme on 10/03/16.
 */
public class ScoutHTMLs extends BatchInterruptibleWithinExecutorPool implements Runnable {

    private static final Logger logger = LogManager.getLogger(ScoutHTMLs.class);
    private static final MetricsLogger metrics = MetricsLogger.getLogger(ScoutHTMLs.class, Main.getInstance().getInstanceName());
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static final ObjectMapper mapper = new ObjectMapper();
    private static int MAX_EXECUTUION_DURATION = 120;
    Consumer<String, String> consumer;
    Producer<String, String> producer;
    private int batchSize;
    private boolean run = false;

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


                        //This is a one pass or deep 0 scan
                        //Seed URLs are sent for HTML retreival from the scout. The HTML of the seed URL is sent back to the scout by the miner.
                        //The scout will than extract all links of the seed URL html and check if the links have been already visited or not
                        //Anay newly discovered link is set as visited and passed to the miner as raw-URL for mining and article extraction.
                        //New unvisited URL are immediatelly marked as visited to avoid recurring issues. For example if the url is marked as visited only
                        //after it has been downloaded by the miner there is a possible risk caused by the miner unability to download the file.
                        //If this happen the url will never be marked as visited and the scout will infinitilly try to fetch it.


                        final HTML html;
                        try {
                            html = mapper.readValue(record.value(), HTML.class);
                        } catch (IOException e) {
                            logger.error("Error deserialising HTML", e);
                            return null;
                        }

                        final BaseURL referral = html.getReferral();

                        //The seedHTML url now becomes the referral of all links discovered in the page
                        URI referralURL = null;
                        try {
                            referralURL = new URI(referral.getUrl());
                        } catch (Exception e) {
                            logger.error("Unvalid SeedURL", e);
                            return null;
                        }


                        //we only scan links of the index seed url
                        if (((SeedURL) html.getReferral()).getDepth() != 0) {

                            return null;
                        }


                        Document doc = Jsoup.parse(html.getRawHTML(), html.getUrl());
                        Elements links = doc.select("a[href]");

                        Set<String> seenURLs = new HashSet<String>();

                        //add seedURL to skip aouto references
                        seenURLs.add(html.getReferral().getUrl());
                        seenURLs.add(html.getReferral().getUrl() + "/");
                        seenURLs.add(html.getReferral().getRawURL());

                        Set<String> urlStrs = new HashSet<String>();
                        for (Element link : links) {
                            urlStrs.add(link.attr("abs:href"));
                        }

                        if (html instanceof AjaxHTML) {
                            urlStrs.addAll(((AjaxHTML) html).getDynamicURLs());
                        }


                        for (String urlStr : urlStrs) {

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
                            if (!url.getHost().equalsIgnoreCase(referralURL.getHost())) {
                                continue;
                            }


                            //skip if url path does not start with expected path
                            if (referral != null && referral instanceof SeedURL) {
                                SeedURL seedURLReferral = (SeedURL) referral;
                                if (seedURLReferral.getExpectedPath() != null && !seedURLReferral.getExpectedPath().isEmpty()) {
                                    if (!url.getPath().toLowerCase().startsWith(seedURLReferral.getExpectedPath().toLowerCase())) {
                                        continue;
                                    }
                                }
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

                            //if we have already visited this page from this referral continue;
                            if (ScannedURLs.getInstance().isVisited(html.getUrl(), resolvedURL)) {
                                continue;
                                //TODO: eventually if we want to track how long a page is featured
                                //here we could update the article referrals list.
                            }

                            //any url that is processed by this method is set as visited
                            ScannedURLs.getInstance().setVisited(html.getUrl(), resolvedURL);

                            //skip self referencing pages
                            if (html.getUrl().equals(resolvedURL)) {
                                continue;
                            }

                            //TODO: eventually test the expectedPath also after the URL resolving, redirects may change that

                            //New unknow link found send it for article extraction

                            //TODO: run classifier to establish if HTML contains and article
                            LinkURL linkURL = new LinkURL();
                            linkURL.setUrl(resolvedURL);
                            linkURL.setReferralURL(html.getUrl());
                            linkURL.setDiscoverDate(dateFormatter.format(new Date()));
                            linkURL.setRawURL(urlStr);

                            if(html.getReferral() instanceof SeedURL ){
                                linkURL.setRegion(((SeedURL) html.getReferral()).getRegion());
                                linkURL.setCountry(((SeedURL) html.getReferral()).getCountryName());
                                linkURL.setCategory(((SeedURL) html.getReferral()).getCategory());
                            }


                            if (!updatedExistingArticle(linkURL)) {

                                try {
                                    String json = mapper.writeValueAsString(linkURL);
                                    producer.send(new ProducerRecord<String, String>("raw-urls", linkURL.getUrl(), json));
                                    metrics.logMetric("submitted url", linkURL);
                                    ScoutMain.addMetric("Submitted URLs", 1);
                                } catch (Exception e) {
                                    logger.fatal("Unable to serialize seedURL", e);
                                }
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


    private boolean updatedExistingArticle(LinkURL linkURL) {

        Client client = null;
        client = ElasticsearchUtil.getInstance().getClient();

        String urlHash = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hash = digest.digest(linkURL.getUrl().getBytes(StandardCharsets.UTF_8));
            urlHash = org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(hash);

            Article article = ArticleFactory.getInstance().getArticle(urlHash);

            if (article != null) {
                //Check if the article already contains this
                boolean notFound = article.getReferrals().stream().noneMatch(baseURL -> linkURL.getReferralURL() != null && baseURL.getReferralURL().equals(linkURL.getReferralURL()));
                if (notFound) {
                    article.getReferrals().add(linkURL);
                    ArticleFactory.getInstance().updateArticle(article);
                }
                return true;
            }

        } catch (NoSuchAlgorithmException e) {
            logger.fatal("Unable to compute URL hash", e);
            return false;
        } catch (Exception e) {
            logger.error("Unable to get article from elasticsearch", e);
            return false;
        }
        return false;
    }


}
