import ch.newsriver.data.source.BaseSource;
import ch.newsriver.data.source.SourceFactory;
import ch.newsriver.data.source.URLSeedSource;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

/**
 * Created by eliapalme on 29/05/16.
 */
public class AddSeedURLSource {

    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    /*
    @Test
    public void addSeedURLSource() throws Exception {


        URLSeedSource source = new URLSeedSource();


        source.setUrl("http://www.nzz.ch");
        source.setReferralURL("http://www.nzz.ch");
        source.setPermanent(true);
        source.setDepth(0);

        source.setHttpStatus("200");
        source.setLastVisit(dateFormatter.format(new Date()));

        SourceFactory.getInstance().setSource(source, true);
    }
*/


/*
    @Test
    public void clearNonPermanent() throws Exception {


        Set<String> sourceIds = SourceFactory.getInstance().nextToVisits("type:URLSeedSource");

        while (!sourceIds.isEmpty()) {

            for (String sourceId : sourceIds) {

                BaseSource source = SourceFactory.getInstance().getSource(sourceId);


                if (source instanceof URLSeedSource) {

                    if (!((URLSeedSource) source).isPermanent()) {
                        SourceFactory.getInstance().removeSource(source);
                    }

                }
            }
            sourceIds = SourceFactory.getInstance().nextToVisits("type:URLSeedSource");
        }

    }
*/


}

