import ch.newsriver.dao.ElasticsearchPoolUtil;
import ch.newsriver.dao.JDBCPoolUtil;
import ch.newsriver.data.content.Article;
import ch.newsriver.data.source.FeedSource;
import ch.newsriver.data.source.SourceFactory;
import ch.newsriver.data.website.WebSite;
import ch.newsriver.data.website.WebSiteFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.queryparser.xml.FilterBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Created by eliapalme on 19/04/16.
 */
public class ImportWebsitesFeeds {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");


    @Test
    public void importWebSiteFeeds() throws Exception {





        Client client = null;
        client = ElasticsearchPoolUtil.getInstance().getClient();
        LinkedList<String> feeds = new LinkedList<>();
        try {
            QueryBuilder qb = QueryBuilders.queryStringQuery("*");

            FilterBuilder filter = null;

            SearchRequestBuilder searchRequestBuilder = client.prepareSearch()
                    .setIndices("newsriver-website")
                    .setTypes("website")
                    .setSize(10000)
                    .addSort("_id", SortOrder.ASC)
                    .setScroll(new TimeValue(60000))
                    .addFields("feeds")
                    .setQuery(qb);

            SearchResponse response = searchRequestBuilder.execute().actionGet();
            do {

                for (SearchHit hit : response.getHits()) {
                    if (hit.getFields().get("feeds") == null) continue;
                    List<Object> websiteFeeds = hit.getFields().get("feeds").getValues();
                    for (Object feedObj : websiteFeeds) {
                        String feed = (String) feedObj;
                        if (feed.contains("/comments/")) continue;
                        feeds.add(feed);
                    }
                }

                response = client.prepareSearchScroll(response.getScrollId()).setScroll(new TimeValue(60000)).execute().actionGet();

            }while (response.getHits().getHits().length >0);

            client.prepareClearScroll().addScrollId(response.getScrollId()).execute();
        } catch (Exception e) {
            System.out.print(e.getLocalizedMessage());
        } finally {
        }




        feeds.parallelStream().forEach(url -> {

            FeedSource source = new FeedSource();

            URI feedURI = null;
            try {
                feedURI = new URI(url.trim());
                if(!feedURI.isAbsolute()){
                    System.out.println(url.trim());
                    return;
                }
                if(feedURI.getHost()==null){
                    return;
                }
                WebSite webSite = WebSiteFactory.getInstance().getWebsite(feedURI.getHost().toLowerCase());
                source.setWebsite(webSite);
            } catch (URISyntaxException e) {
                System.out.println(url.trim());
                return;
            }
            source.setUrl(feedURI.toString());
            source.setHttpStatus("200");
            source.setLastVisit(dateFormatter.format(new Date()));
            SourceFactory.getInstance().setSource(source,false);
        });

    }


}
