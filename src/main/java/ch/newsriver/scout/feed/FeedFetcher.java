package ch.newsriver.scout.feed;


import ch.newsriver.util.normalization.text.TextNormaliser;
import ch.newsriver.util.normalization.url.URLUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.FetcherException;
import com.sun.syndication.fetcher.impl.HttpClientFeedFetcher;
import com.sun.syndication.io.FeedException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Created by eliapalme on 11/03/16.
 */
public class FeedFetcher {


    private static final Logger logger = LogManager.getLogger(FeedFetcher.class);

    private static final int READ_TIMEOUT = 60000;
    private static final int CONNECTION_TIMEOUT = 60000;
    //Please not that HttpClientFeedFetcher is using the old httpclient 3.0 therefore we added common-http legacy jar file
    private static final HttpClientFeedFetcher clientFeedFetcher = new HttpClientFeedFetcher();

    private static final Random rand = new Random();

    private static final int MAX_ARTICLES_PER_FETCH = 10;

    private static final ObjectMapper mapper = new ObjectMapper();

    static {

        clientFeedFetcher.setConnectTimeout(CONNECTION_TIMEOUT);
        clientFeedFetcher.setReadTimeout(READ_TIMEOUT);

        clientFeedFetcher.setHttpClientMethodCallback(new HttpClientFeedFetcher.HttpClientMethodCallbackIntf() {
            public void afterHttpClientMethodCreate(HttpMethod method) {
                method.setRequestHeader("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/600.2.5 (KHTML, like Gecko) Version/7.1.2 Safari/537.85.11");
            }
        });
    }


    public void process(String feedURL){


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
            logger.error("Unable to load kafka properties",e);
        } finally {
            try {
                inputStream.close();
            } catch (java.lang.Exception e) { }
        }

        FeedFetcherResult result = null;
        try {
            result = FeedFetcher.fetchUrls(feedURL, MAX_ARTICLES_PER_FETCH);
        }catch (Throwable e){}

        Producer<String, String> producer = new KafkaProducer(props);


        for(FeedURL url : result.getUrls()) {
            try {
            RecordMetadata test =producer.send(new ProducerRecord<String, String>("raw-urls", url.getUrl(), mapper.writeValueAsString(url))).get();
                test.topic();
            }catch (Exception e){
                logger.fatal("Unable to send to kafka",e);
            }
        }
        producer.close();
    }



    public static FeedURL fetchRandomLink(String feedURL) throws MalformedURLException, IllegalArgumentException, IOException, FeedException, FetcherException {

        //retrieveFeed will create a httpclient object at every call therefore it is safe to keep HttpClientFeedFetcher static
        SyndFeed feed = clientFeedFetcher.retrieveFeed(new URL(feedURL));
        if (feed.getEntries().isEmpty()) {
            return null;
        }
        SyndEntry article = (SyndEntry) feed.getEntries().get(rand.nextInt(feed.getEntries().size()));
        return getFeedUrl(article, feedURL);

    }

    public static FeedFetcherResult fetchUrls(String feedURL, int limit) throws IllegalArgumentException, IOException, FeedException, FetcherException {

        List<FeedURL> urls = new ArrayList();

        SyndFeed feed = clientFeedFetcher.retrieveFeed(new URL(feedURL));
        int remainingArticles = 0;
        for (Object entryObject : feed.getEntries()) {
            SyndEntry entry = (SyndEntry) entryObject;
            FeedURL url = getFeedUrl(entry, feedURL);
            if (url != null) {
                if (limit > 0) {
                    urls.add(url);
                    limit--;
                } else {
                    remainingArticles++;
                }
            }
        }
        return new FeedFetcherResult(urls, remainingArticles, feed.getEntries().size());
    }


    private static FeedURL getFeedUrl(SyndEntry feedEntry, String referal) {
        FeedURL feedURL = new FeedURL();
        String cleanLink = feedEntry.getLink();

        if (cleanLink == null) {
            return null;
        }
        cleanLink = cleanLink.trim();


        try {
            cleanLink = URLUtils.normalizeUrl(cleanLink, feedEntry.getLink());
        } catch (MalformedURLException ex) {
            logger.fatal("Unable to resolve link for feed entry:" + feedEntry.getLink(), ex);
            return null;
        }


        feedURL.setReferal(referal);
        feedURL.setUrl(cleanLink);
        feedURL.setDiscoveryDate(new Date().getTime());
        if (feedEntry.getPublishedDate() != null) {
            feedURL.setPublicationDate(feedEntry.getPublishedDate().getTime());
        }

        feedURL.setTitle(TextNormaliser.caseNormalizer(feedEntry.getTitle()));

        String abstractText = null;

        if (feedEntry.getDescription() != null && feedEntry.getDescription().getValue() != null) {
            abstractText = Jsoup.parse(feedEntry.getDescription().getValue()).body().text();
        }

        //Check in case the description was not user
        if (abstractText == null && feedEntry.getContents() != null) {
            for (SyndContent content : (List<SyndContent>) feedEntry.getContents()) {
                if (content.getType().equalsIgnoreCase("html")) {
                    abstractText = Jsoup.parse(content.getValue()).body().text();
                    break;
                }
            }
        }
        if(abstractText!=null && !abstractText.isEmpty()) {
            feedURL.setHeadlines(TextNormaliser.cleanText(abstractText));
        }

        return feedURL;
    }
}

