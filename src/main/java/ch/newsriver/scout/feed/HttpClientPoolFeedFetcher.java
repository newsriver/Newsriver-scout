package ch.newsriver.scout.feed;

import ch.newsriver.util.http.HttpClientPool;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.FetcherEvent;
import com.sun.syndication.fetcher.FetcherException;
import com.sun.syndication.fetcher.impl.FeedFetcherCache;
import com.sun.syndication.fetcher.impl.HttpClientFeedFetcher;
import com.sun.syndication.fetcher.impl.SyndFeedInfo;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;


import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

/**
 * Created by eliapalme on 16/03/16.
 */
public class HttpClientPoolFeedFetcher extends HttpClientFeedFetcher {



    public SyndFeed retrieveFeed(URL feedUrl) throws IllegalArgumentException, IOException, FeedException, FetcherException {
        if (feedUrl == null) {
            throw new IllegalArgumentException("null is not a valid URL");
        }


        HttpContext context = new BasicHttpContext();
        String urlStr = feedUrl.toString();
        HttpGet httpGetRequest = null;
        httpGetRequest = new HttpGet(urlStr);
        httpGetRequest.addHeader("Accept-Encoding", "gzip");
        httpGetRequest.addHeader("User-Agent", getUserAgent());



            // cache is not in use
            try {
                HttpResponse resp = HttpClientPool.getHttpClientInstance().execute(httpGetRequest, context);
                HttpEntity entity = resp.getEntity();
                fireEvent(FetcherEvent.EVENT_TYPE_FEED_POLLED, urlStr);
                handleErrorCodes(resp.getStatusLine().getStatusCode());

                return getFeed(null, urlStr, entity, resp.getStatusLine().getStatusCode());
            } finally {
                httpGetRequest.releaseConnection();
            }

    }

    private SyndFeed getFeed(SyndFeedInfo syndFeedInfo, String urlStr, HttpEntity entity, int statusCode) throws IOException, HttpException, FetcherException, FeedException {

        if (statusCode == HttpURLConnection.HTTP_NOT_MODIFIED && syndFeedInfo != null) {
            fireEvent(FetcherEvent.EVENT_TYPE_FEED_UNCHANGED, urlStr);
            return syndFeedInfo.getSyndFeed();
        }

        SyndFeed feed = retrieveFeed(urlStr, entity);
        fireEvent(FetcherEvent.EVENT_TYPE_FEED_RETRIEVED, urlStr, feed);
        return feed;
    }

    private SyndFeed retrieveFeed(String urlStr, HttpEntity entity) throws IOException, HttpException, FetcherException, FeedException {

        InputStream stream = null;
        if (entity.getContentEncoding() != null && entity.getContentEncoding().getValue().equalsIgnoreCase("gzip")) {
            stream = new GZIPInputStream(entity.getContent());
        } else {
            stream = entity.getContent();
        }
        try {
            XmlReader reader = null;
            if (entity.getContentType() != null) {
                reader = new XmlReader(stream, entity.getContentType().getValue(), true);
            } else {
                reader = new XmlReader(stream, true);
            }
            SyndFeedInput syndFeedInput = new SyndFeedInput();
            syndFeedInput.setPreserveWireFeed(isPreserveWireFeed());

            return syndFeedInput.build(reader);
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }
}
