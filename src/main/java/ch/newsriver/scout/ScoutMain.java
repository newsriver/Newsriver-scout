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

            //Note: we allocate 1/5 of threads to the the source scout since
            //the html-seed queue needs more resources to follow

            scoutSources = new ScoutSources(this.getPoolSize() / 5 + 1, this.getBatchSize() / 5 + 1, this.getQueueSize() / 5 + 1);
            new Thread(scoutSources).start();

            scoutHTMLs = new ScoutHTMLs(this.getPoolSize() / 5 * 4 + 1, this.getBatchSize() / 5 * 4 + 1, this.getQueueSize() / 5 * 4 + 1);
            new Thread(scoutHTMLs).start();

            /*scoutWebSites = new ScoutWebsites(this.getPoolSize(), this.getBatchSize(), this.getQueueSize());
            new Thread(scoutWebSites).start();*/

        } catch (Exception e) {
            logger.fatal("Unable to initialize scoutSources", e);
        }
    }

}