import org.junit.Test;

/**
 * Created by eliapalme on 29/04/16.
 */
public class ResetVisitedDate {


    @Test
    public void resetVisitedDate() throws Exception {

        /*


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


        */


    }
}
