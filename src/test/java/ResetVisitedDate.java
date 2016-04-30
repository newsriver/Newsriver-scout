import ch.newsriver.dao.ElasticsearchPoolUtil;
import ch.newsriver.data.source.FeedSource;
import ch.newsriver.data.source.SourceFactory;
import ch.newsriver.data.website.WebSite;
import ch.newsriver.data.website.WebSiteFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.queryparser.xml.FilterBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by eliapalme on 29/04/16.
 */
public class ResetVisitedDate {



    @Test
    public void resetVisitedDate() throws Exception {




        Client client = null;
        client = ElasticsearchPoolUtil.getInstance().getClient();

        try {
            QueryBuilder qb = QueryBuilders.queryStringQuery("*");

            FilterBuilder filter = null;

            SearchRequestBuilder searchRequestBuilder = client.prepareSearch()
                    .setIndices("newsriver-source")
                    .setTypes("source")
                    .setSize(100)
                    .addSort("_id", SortOrder.ASC)
                    .setScroll(new TimeValue(600000))
                    .addFields("_id")
                    .setQuery(qb);

            SearchResponse response = searchRequestBuilder.execute().actionGet();
            do {

                response.getHits().forEach(hit -> {
                    String id = hit.getId();
                    SourceFactory.getInstance().updateLastVisit(id);
                });


                response = client.prepareSearchScroll(response.getScrollId()).setScroll(new TimeValue(600000)).execute().actionGet();

            }while (response.getHits().getHits().length >0);

        } catch (Exception e) {
            System.out.print(e.getLocalizedMessage());
        } finally {
        }





    }
}
