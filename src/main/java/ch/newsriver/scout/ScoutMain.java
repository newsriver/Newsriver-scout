package ch.newsriver.scout;

import ch.newsriver.dao.RedisPoolUtil;
import ch.newsriver.executable.poolExecution.MainWithPoolExecutorOptions;
import ch.newsriver.util.http.HttpClientPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Elia Palme on 10/03/16.
 */
public class ScoutMain extends MainWithPoolExecutorOptions {


    private static final int DEFAUTL_PORT = 9096;
    private static final Logger logger = LogManager.getLogger(ScoutMain.class);
    static ScoutSources scoutSources;
    static ScoutHTMLs scoutHTMLs;
    static ScoutWebsites scoutWebSites;

    public ScoutMain(String[] args) {
        super(args, true);
    }

    public static void main(String[] args) {

        new ScoutMain(args);
    }

    public int getDefaultPort() {
        return DEFAUTL_PORT;
    }

    public void shutdown() {
        if (scoutSources != null) scoutSources.stop();
        if (scoutHTMLs != null) scoutHTMLs.stop();
        if (scoutWebSites != null) scoutWebSites.stop();
        RedisPoolUtil.getInstance().destroy();
        HttpClientPool.shutdown();
    }

    public void start() {
        try {
            HttpClientPool.initialize();
        } catch (NoSuchAlgorithmException e) {
            logger.fatal("Unable to initialize HttpClientPool", e);
        } catch (KeyStoreException e) {
            logger.fatal("Unable to initialize HttpClientPool", e);
        } catch (KeyManagementException e) {
            logger.fatal("Unable to initialize HttpClientPool", e);
        }

        System.out.println("Threads pool size:" + this.getPoolSize() + "\tbatch size:" + this.getBatchSize() + "\tqueue size:" + this.getBatchSize());


        try {


            //Threads allocation, more resources to html-seed queue needs more resources to follow
            // 0.5 to html
            // 0.2 to sources
            // 0.3 to website
            float website_seed = 0.3f;
            float sources_seed = 0.2f;
            float html_seed = 0.5f;

            scoutSources = new ScoutSources((int) (this.getPoolSize() * sources_seed) + 1, (int) (this.getBatchSize() * sources_seed) + 1, (int) (this.getQueueSize() * sources_seed) + 1);
            new Thread(scoutSources).start();

            scoutWebSites = new ScoutWebsites((int) (this.getPoolSize() * website_seed) + 1, (int) (this.getBatchSize() * website_seed) + 1, (int) (this.getQueueSize() * website_seed) + 1);
            new Thread(scoutWebSites).start();

            scoutHTMLs = new ScoutHTMLs((int) (this.getPoolSize() * html_seed) + 1, (int) (this.getBatchSize() * html_seed) + 1, (int) (this.getQueueSize() * html_seed) + 1);
            new Thread(scoutHTMLs).start();


        } catch (Exception e) {
            logger.fatal("Unable to initialize scoutSources", e);
        }
    }

}