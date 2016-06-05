package ch.newsriver.scout.cache;

import ch.newsriver.dao.RedisPoolUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.sortedset.ZAddParams;

import java.net.URI;

/**
 * Created by eliapalme on 29/05/16.
 */
public class ScannedURLs {


    private final static String REDIS_KEY_PREFIX     = "scanURL";
    private final static String REDIS_KEY_VERSION     = "2";
    private final static Long   REDIS_KEY_EXP_SECONDS = 60l * 60l * 24l * 30l * 3l; //about 3 months
    static ScannedURLs instance=null;

    public static synchronized ScannedURLs getInstance(){
        if(instance==null){
            instance = new ScannedURLs();
        }
        return instance;
    }

    private ScannedURLs(){}

    private String getKey(String host, String url){
        StringBuilder builder = new StringBuilder();
        return builder.append(REDIS_KEY_PREFIX).append(":")
                .append(REDIS_KEY_VERSION).append(":")
                .append(host).append(":")
                .append(url).toString();
    }

    public boolean isVisited(String host, String url){

        try (Jedis jedis = RedisPoolUtil.getInstance().getResource(RedisPoolUtil.DATABASES.VISITED_URLS)) {

            return  jedis.exists(getKey(host,url));

        }

    }

    public void setVisited(String host, String url){
        try (Jedis jedis = RedisPoolUtil.getInstance().getResource(RedisPoolUtil.DATABASES.VISITED_URLS)) {
            jedis.set(getKey(host,url),"","NX","EX",REDIS_KEY_EXP_SECONDS);
        };
    }

}
