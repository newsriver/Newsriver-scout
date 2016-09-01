import ch.newsriver.dao.ElasticsearchPoolUtil;
import ch.newsriver.data.website.WebSite;
import ch.newsriver.data.website.WebSiteFactory;
import ch.newsriver.data.website.source.BaseSource;
import ch.newsriver.data.website.source.SourceFactory;
import ch.newsriver.util.url.URLUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.queryparser.xml.FilterBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 * Created by eliapalme on 16/08/16.
 */
public class AddSourceToWebsites {

    private static final ObjectMapper mapper = new ObjectMapper();


    @Ignore("Test is ignored, used for internal manipulation")
    @Test
    public void addFeedSource() throws Exception, IOException {


        Client client = null;
        client = ElasticsearchPoolUtil.getInstance().getClient();

        try {
            QueryBuilder qb = QueryBuilders.queryStringQuery("type:FeedSource");

            FilterBuilder filter = null;

            SearchRequestBuilder searchRequestBuilder = client.prepareSearch()
                    .setIndices("newsriver-source")
                    .setTypes("source")
                    .setSize(100)
                    .addSort("_id", SortOrder.ASC)
                    .setScroll(new TimeValue(600000))
                    .setQuery(qb);

            SearchResponse response = searchRequestBuilder.execute().actionGet();
            do {

                response.getHits().forEach(hit -> {
                    try {
                        BaseSource source = mapper.readValue(hit.getSourceAsString(), BaseSource.class);


                        WebSite webSite = null;
                        JsonNode node = mapper.readTree(hit.getSourceAsString());
                        if (node.has("website") && !node.get("website").isNull() && node.get("website").has("hostName") && !node.get("website").get("hostName").isNull()) {
                            String hostName = node.get("website").get("hostName").asText();
                            webSite = WebSiteFactory.getInstance().getWebsite(hostName);
                        }

                        if (webSite == null) {
                            List<WebSite> webSites = WebSiteFactory.getInstance().getWebsitesWithFeed(node.get("url").asText());
                            if (webSites != null && !webSites.isEmpty()) {
                                if (webSites.size() > 1) {
                                    System.out.println("Multiple websites found for feed:" + source.getUrl());
                                }
                                webSite = webSites.get(0);
                            }
                        }

                        if (webSite == null) {
                            List<WebSite> webSites = WebSiteFactory.getInstance().getWebsitesWithDomain(URLUtils.getDomainRoot(node.get("url").asText()));
                            if (webSites != null && !webSites.isEmpty()) {
                                if (webSites.size() > 1) {
                                    System.out.println("Multiple websites found for domain:" + source.getUrl());
                                }
                                webSite = webSites.get(0);
                            }
                        }


                        if (webSite == null) {
                            System.out.println("Website not found:" + source.getUrl());
                            return;
                        }


                        //Check if source may already exists
                        if (webSite.getSources() != null) {
                            for (BaseSource exsourcEx : webSite.getSources()) {
                                if (exsourcEx.getUrl().equals(source.getUrl()))
                                    return;
                            }
                        }

                        webSite.getSources().add(source);
                        WebSiteFactory.getInstance().updateWebsite(webSite);
                        SourceFactory.getInstance().removeSource(source);

                    } catch (IOException e) {
                        System.out.print(e.getLocalizedMessage());
                    }

                });

                response = client.prepareSearchScroll(response.getScrollId()).setScroll(new TimeValue(600000)).execute().actionGet();

            } while (response.getHits().getHits().length > 0);

        } catch (Exception e) {
            System.out.print(e.getLocalizedMessage());
        } finally {
        }


    }


}
