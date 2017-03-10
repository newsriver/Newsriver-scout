import ch.newsriver.data.website.source.BaseSource;
import ch.newsriver.data.website.source.SourceFactory;
import org.apache.commons.codec.binary.Base64;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by eliapalme on 29/05/16.
 */
public class RemoveSeedURLSource {


    private String feedURL = "https://www.freelancer.hk/rss.xml";


    @Ignore("Test is ignored, used for internal manipulation")
    @Test
    public void removeSource() throws Exception {

        BaseSource source = SourceFactory.getInstance().getSource(getURLHash(feedURL));
        SourceFactory.getInstance().removeSource(source);

    }

    private String getURLHash(String url) {

        String urlHash = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hash = digest.digest(url.getBytes(StandardCharsets.UTF_8));
            urlHash = Base64.encodeBase64URLSafeString(hash);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        return urlHash;

    }

}

