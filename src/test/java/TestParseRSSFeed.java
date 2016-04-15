import ch.newsriver.data.url.FeedURL;
import ch.newsriver.scout.feed.FeedFetcher;
import ch.newsriver.scout.feed.FeedFetcherResult;
import ch.newsriver.util.http.HttpClientPool;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by eliapalme on 08/04/16.
 */
public class TestParseRSSFeed {


    public TestParseRSSFeed() {

    }

    private static final ObjectMapper mapper = new ObjectMapper();
    Producer<String, String> producer;

    @Before
    public void initialize() throws Exception {

        HttpClientPool.initialize();

        Properties props = new Properties();
        String propFileName = "kafka.properties";
        try (InputStream inputStream = FeedFetcher.class.getClassLoader().getResourceAsStream(propFileName)) {
            props.load(inputStream);
        }
        producer = new KafkaProducer(props);

    }

    @After
    public void shutdown() throws Exception {
        HttpClientPool.shutdown();
        producer.close();
    }

    @Test
    public void parseGoogleFeed() throws Exception {

        //String url = "https://news.google.com/news/section?q=+Rock+Climbing&output=rss";
        String url = "https://www.google.com/alerts/feeds/08403177152869353340/8406544804626500550";

        FeedFetcher fetcher = new FeedFetcher(url);
        FeedFetcherResult result = fetcher.fetch();
        assertNotNull(result);

        for (FeedURL feedURL : result.getUrls()) {
            assertNotNull(feedURL.getUlr());
            assertNotNull(feedURL.getRawURL());
            assertNotNull(feedURL.getReferralURL());

            assertFalse(feedURL.getUlr().isEmpty());
            assertFalse(feedURL.getRawURL().isEmpty());
            assertFalse(feedURL.getReferralURL().isEmpty());

            String json = mapper.writeValueAsString(feedURL);
            producer.send(new ProducerRecord<String, String>("raw-urls", feedURL.getUlr(), json));

        }


    }

}



