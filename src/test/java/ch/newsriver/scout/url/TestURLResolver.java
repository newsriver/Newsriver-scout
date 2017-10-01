package ch.newsriver.scout.url;

import ch.newsriver.util.http.HttpClientPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Created by eliapalme on 01/09/16.
 */
public class TestURLResolver {

    private URLResolver resolver;

    @Before
    public void initialize() throws Exception {

        HttpClientPool.initialize();
        resolver = URLResolver.getInstance();


    }

    @After
    public void shutdown() throws Exception {
        HttpClientPool.shutdown();
    }


    /*
            TODO: currently due to a stric SSL implementation
            Some server are not supported see: http://stackoverflow.com/questions/7615645/ssl-handshake-alert-unrecognized-name-error-since-upgrade-to-java-1-7-0
            SSL handshake alert: unrecognized_name

            If one day we implement a fix, hopfully this test will not fail. Also not that if netcetera will correctly configure thier servers in the future this test
            will fail and shuld be removed.
     */
    @Ignore //Unfortunatelly netcetera has updated the SSL cert and this test no longer fails
    @Test(expected = URLResolver.InvalidURLException.class)
    public void testUnrecognized_name() throws Exception {

        String url = "https://www.netcetera.com/home.html";

        resolver.resolveURL(url);

    }


}
