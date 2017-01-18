package ch.newsriver.sources.newscron;

import ch.newsriver.dao.JDBCPoolUtil;
import ch.newsriver.data.website.WebSite;
import ch.newsriver.data.website.WebSiteFactory;
import ch.newsriver.data.website.source.FeedSource;
import ch.newsriver.data.website.source.SourceFactory;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Created by eliapalme on 19/04/16.
 */
public class ImportDBFeeds {

    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    @Ignore("Test is ignored, used for internal manipulation")
    @Test
    public void importDBFeeds() throws Exception {

        List<String> urls = new LinkedList();
        String sql = "SELECT url,C.name as category,P.name as region,K.ISOCode as country, L.name as language FROM NewscronConfiguration.feed AS F\n" +
                "LEFT JOIN NewscronConfiguration.category AS C ON C.id=F.categoryID\n" +
                "LEFT JOIN NewscronConfiguration.package AS P ON P.id=F.packageID\n" +
                "LEFT JOIN NewscronConfiguration.country AS K ON K.id=P.countryID\n" +
                "LEFT JOIN NewscronConfiguration.language AS L ON L.id=P.defaultLanguage\n";

        try (Connection conn = JDBCPoolUtil.getInstance().getConnection(JDBCPoolUtil.DATABASES.Sources); PreparedStatement stmt = conn.prepareStatement(sql);) {
            try (ResultSet rs = stmt.executeQuery();) {
                while (rs.next()) {

                    FeedSource source = new FeedSource();
                    WebSite webSite = null;
                    URI feedURI = null;
                    try {
                        feedURI = new URI(rs.getString("url").trim());
                        if (!feedURI.isAbsolute()) {
                            System.out.println(rs.getString("url").trim());
                            continue;
                        }
                        webSite = WebSiteFactory.getInstance().getWebsite(feedURI.getHost().toLowerCase());
                    } catch (URISyntaxException e) {
                        System.out.println(rs.getString("url").trim());
                        continue;
                    }
                    source.setUrl(feedURI.toString());
                    String category = rs.getString("category");
                    if (!rs.wasNull()) {
                        source.setCategory(category);
                    }
                    String region = rs.getString("region");
                    if (!rs.wasNull()) {
                        source.setRegion(region);
                    }
                    String language = rs.getString("language");
                    if (!rs.wasNull()) {
                        Locale.Builder builder = new Locale.Builder();
                        builder.setLanguage(language);
                        Locale locale = builder.build();
                        source.setLanguageCode(locale.getLanguage());
                    }

                    String country = rs.getString("country");
                    if (!rs.wasNull()) {
                        Locale.Builder builder = new Locale.Builder();
                        builder.setRegion(country);
                        Locale locale = builder.build();

                        source.setRegion(region);
                        source.setCountryCode(locale.getISO3Country());
                        source.setCountryName(locale.getDisplayCountry());
                    } else if (webSite != null && webSite.getCountryCode() != null) {
                        source.setCountryCode(webSite.getCountryCode());
                        source.setCountryName(webSite.getCountryName());
                    }

                    source.setHttpStatus("200");
                    source.setLastVisit(dateFormatter.format(new Date()));

                    SourceFactory.getInstance().setSource(source, true);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }


}
