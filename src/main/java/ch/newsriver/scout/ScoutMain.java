package ch.newsriver.scout;
import ch.newsriver.dao.RedisPoolUtil;
import ch.newsriver.executable.Main;
import ch.newsriver.executable.poolExecution.MainWithPoolExecutorOptions;
import ch.newsriver.util.http.HttpClientPool;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by Elia Palme on 10/03/16.
 */
public class ScoutMain extends MainWithPoolExecutorOptions {



    private static final int DEFAUTL_PORT = 9096;
    private static final Logger logger = LogManager.getLogger(ScoutMain.class);

    public static void main(String[] args){

        new ScoutMain(args);
    }

    static Scout scout;

    public int getDefaultPort(){
        return DEFAUTL_PORT;
    }

    public ScoutMain(String[] args){
        super(args,true);
    }


    public void shutdown(){
        if(scout!=null)scout.stop();
        RedisPoolUtil.getInstance().destroy();
        HttpClientPool.shutdown();
    }

    public void start(){
        try {
            HttpClientPool.initialize();
        } catch (NoSuchAlgorithmException e) {
            logger.fatal("Unable to initialize HttpClientPool",e);
        } catch (KeyStoreException e) {
            logger.fatal("Unable to initialize HttpClientPool",e);
        } catch (KeyManagementException e) {
            logger.fatal("Unable to initialize HttpClientPool",e);
        }
        try {
            System.out.println("Threads pool size:" + this.getPoolSize() +"\tbatch size:"+this.getBatchSize()+"\tqueue size:"+this.getBatchSize());
            scout = new Scout(this.getPoolSize(),this.getBatchSize(),this.getQueueSize());
            new Thread(scout).start();
        } catch (Exception e) {
            logger.fatal("Unable to initialize scout", e);
        }
    }

}