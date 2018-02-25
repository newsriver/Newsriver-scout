package ch.newsriver.scout.cache;

import ch.newsriver.scout.url.URLResolver;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Created by eliapalme on 21.02.18.
 */
public class TestVisitedURLs {


    @Ignore
    @Test(expected = URLResolver.InvalidURLException.class)
    public void testUnrecognized_name() throws Exception {

        boolean visited = VisitedURLs.getInstance().isVisited("http://www.basementmedicine.org/feed/", "https://www.basementmedicine.org/top-stories/2018/02/20/7982/");
        System.out.println(visited);
        visited = false;
    }
}
