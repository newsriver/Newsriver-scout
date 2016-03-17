package ch.newsriver.scout;
import ch.newsriver.dao.RedisPoolUtil;
import ch.newsriver.executable.Main;
import ch.newsriver.util.http.HttpClientPool;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Elia Palme on 10/03/16.
 */
public class ScoutMain extends Main {

    private static final int DEFAUTL_PORT = 9096;
    private static final Logger logger = LogManager.getLogger(ScoutMain.class);

    static Scout scout;

    public int getDefaultPort(){
        return DEFAUTL_PORT;
    }

    public ScoutMain(String[] args, Options options){
        super(args,options);
    }

    public static void main(String[] args){

        Options options = new Options();

        options.addOption("f","pidfile", true, "pid file location");
        options.addOption(Option.builder("p").longOpt("port").hasArg().type(Number.class).desc("port number").build());

        new ScoutMain(args,options);
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
            scout = new Scout();
            new Thread(scout).start();
        } catch (Exception e) {
            logger.fatal("Unable to initialize scout", e);
        }
    }

}