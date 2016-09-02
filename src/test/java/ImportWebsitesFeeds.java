import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Ignore;
import org.junit.Test;

import java.text.SimpleDateFormat;

/**
 * Created by eliapalme on 19/04/16.
 */
public class ImportWebsitesFeeds {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    @Ignore("Test is ignored, used for internal manipulation")
    @Test
    public void importWebSiteFeeds() throws Exception {


        /*


        Client client = null;
        client = ElasticsearchUtil.getInstance().getClient();
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

        */

    }


}
