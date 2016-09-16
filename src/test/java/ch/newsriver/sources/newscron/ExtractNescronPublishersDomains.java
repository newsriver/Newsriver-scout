package ch.newsriver.sources.newscron;

import ch.newsriver.dao.JDBCPoolUtil;
import ch.newsriver.util.url.URLUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by eliapalme on 05/09/16.
 */
@Ignore("Test is ignored, used for internal one time data processing")
public class ExtractNescronPublishersDomains {

    @Test
    public void proccessNewscronPublishers() throws IOException {

        List<String> urls = new LinkedList();
        String sql = "SELECT id,website FROM NewscronConfiguration.publisher";
        String update = "UPDATE NewscronConfiguration.publisher SET domainName=? where id=?";

        try (Connection conn = JDBCPoolUtil.getInstance().getConnection(JDBCPoolUtil.DATABASES.Sources); PreparedStatement stmt = conn.prepareStatement(sql); PreparedStatement updateStm = conn.prepareStatement(update);) {


            try (ResultSet rs = stmt.executeQuery();) {

                while (rs.next()) {
                    if (rs.getString("website") == null || rs.getString("website").isEmpty()) continue;
                    String domain = URLUtils.getDomainRoot(new URL(rs.getString("website")).getHost());
                    updateStm.clearParameters();
                    updateStm.setString(1, domain);
                    updateStm.setLong(2, rs.getLong("id"));
                    updateStm.executeUpdate();
                }


            } catch (Exception e) {
                e.printStackTrace();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
