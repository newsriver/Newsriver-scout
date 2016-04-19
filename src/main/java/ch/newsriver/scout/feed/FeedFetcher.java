package ch.newsriver.scout.feed;


import ch.newsriver.data.url.BaseURL;
import ch.newsriver.data.url.FeedURL;
import ch.newsriver.scout.cache.VisitedURLs;
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
import org.apache.logging.log4j.util.Supplier;
import org.jsoup.Jsoup;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Created by eliapalme on 11/03/16.
 */
public class FeedFetcher {


    private static final Logger logger = LogManager.getLogger(FeedFetcher.class);


    private static final int READ_TIMEOUT = 60000;
    private static final int CONNECTION_TIMEOUT = 60000;
    //Please not that HttpClientFeedFetcher is using the old httpclient 3.0 therefore we added common-http legacy jar file
    private static final HttpClientPoolFeedFetcher clientFeedFetcher = new HttpClientPoolFeedFetcher();

    private static final Random rand = new Random();
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    //Some feeds do generate links dinamically like the google ones
    private static final int MAX_ARTICLES_PER_FETCH = 50;


    private String feedURL;

    public FeedFetcher(String url) {
        feedURL = url;
    }


    public FeedFetcherResult fetch() {
        FeedFetcherResult result = null;
        try {
            return FeedFetcher.fetchUrls(feedURL, MAX_ARTICLES_PER_FETCH);
        } catch (Throwable e) {
            logger.error(e);
            return null;
        }
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
            if (limit > 0) {
                SyndEntry entry = (SyndEntry) entryObject;
                FeedURL url = getFeedUrl(entry, feedURL);
                if (url != null) {

                    //TODO:
                    //Note that we are cheking the normalised link version but NOT the resolved one
                    //this to allow multiple referals in case the same Article is linked with
                    //two different urls;
                    if (VisitedURLs.getInstance().isVisited(feedURL, url.getUlr())) {
                        continue;
                    }
                    VisitedURLs.getInstance().setVisited(feedURL, url.getUlr());

                    urls.add(url);
                    limit--;
                }
            } else {
                remainingArticles++;
            }
        }
        return new FeedFetcherResult(urls, remainingArticles, feed.getEntries().size());
    }



    private static FeedURL getFeedUrl(SyndEntry feedEntry, String referalURL) {
        FeedURL feedURL = new FeedURL();

        if (feedEntry.getLink() == null) {
            return null;
        }
        feedURL.setRawURL(feedEntry.getLink());


        String cleanLink = null;
        try {
            cleanLink = URLUtils.normalizeUrl(feedURL.getRawURL().trim(), feedEntry.getLink());
        } catch (MalformedURLException ex) {
            logger.fatal("Unable to resolve link for feed entry:" + feedEntry.getLink(), ex);
            return null;
        }


        feedURL.setReferralURL(referalURL);
        feedURL.setUlr(cleanLink);
        feedURL.setDiscoverDate(dateFormatter.format(new Date()));
        if (feedEntry.getPublishedDate() != null) {
            feedURL.setPublicationDate(simpleDateFormat.format(feedEntry.getPublishedDate()));
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
        if (abstractText != null && !abstractText.isEmpty()) {
            feedURL.setHeadlines(TextNormaliser.cleanText(abstractText));
        }

        return feedURL;
    }


}

