import ch.newsriver.dao.ElasticsearchUtil;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Created by eliapalme on 29/04/16.
 */
public class ResetStatusToActive {


    @Ignore("Test is ignored, used for internal manipulation")
    @Test
    public void resetVisitedDate() throws Exception {


        Client client = null;
        client = ElasticsearchUtil.getInstance().getClient();

        try {
            QueryBuilder qb = QueryBuilders.queryStringQuery("*");


            SearchRequestBuilder searchRequestBuilder = client.prepareSearch()
                    .setIndices("newsriver-website")
                    .setTypes("website")
                    .setSize(100)
                    .addStoredField("_id")
                    .setFetchSource(false)
                    .setScroll(new TimeValue(600000))
                    .setQuery(qb);


            SearchResponse response = searchRequestBuilder.execute().actionGet();
            do {

                response.getHits().forEach(hit -> {
                    String id = hit.getId();
                    setStatus(id, "ACTIVE");
                });


                response = client.prepareSearchScroll(response.getScrollId()).setScroll(new TimeValue(600000)).execute().actionGet();

            } while (response.getHits().getHits().length > 0);

        } catch (Exception e) {
            System.out.print(e.getLocalizedMessage());
        } finally {
        }


    }

    public long setStatus(String id, String Status) {

        Client client = null;
        client = ElasticsearchUtil.getInstance().getClient();
        try {
            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.index("newsriver-website");
            updateRequest.type("website");
            updateRequest.id(id);
            updateRequest.doc(jsonBuilder()
                    .startObject()
                    .field("status", Status)
                    .endObject());

            return client.update(updateRequest).get().getVersion();
        } catch (IOException e) {

        } catch (ExecutionException e) {

        } catch (InterruptedException e) {

        }
        return -1;
    }

}
