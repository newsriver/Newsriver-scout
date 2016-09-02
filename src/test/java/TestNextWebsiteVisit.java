import ch.newsriver.data.website.WebSiteFactory;
import ch.newsriver.data.website.source.BaseSource;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;

/**
 * Created by eliapalme on 16/08/16.
 */
public class TestNextWebsiteVisit {

    @Ignore("Test is ignored, used for internal manipulation")
    @Test
    public void nextToVisti() throws Exception {

        HashMap<String, BaseSource> soruces = WebSiteFactory.getInstance().nextWebsiteSourcesToVisits();
        for (String hostname : soruces.keySet()) {

            BaseSource source = soruces.get(hostname);
            WebSiteFactory.getInstance().updateSourceLastVisit(hostname, source);


        }
    }


}