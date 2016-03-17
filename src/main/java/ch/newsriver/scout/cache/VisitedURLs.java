package ch.newsriver.scout.cache;

import ch.newsriver.dao.RedisPoolUtil;
import redis.clients.jedis.Jedis;

/**
 * Created by eliapalme on 11/03/16.
 */
public class VisitedURLs {

    private final static String REDIS_KEY_PREFIX     = "visURL";
    private final static String REDIS_KEY_VERSION     = "1";
    private final static Long   REDIS_KEY_EXP_SECONDS = 60l * 60l * 24l * 30l * 3l; //about 3 months
    static VisitedURLs instance=null;

    public static synchronized VisitedURLs getInstance(){
        if(instance==null){
            instance = new VisitedURLs();
        }
        return instance;
    }

    private VisitedURLs(){}

    private String getKey(String referrer, String url){
        StringBuilder builder = new StringBuilder();
        return builder.append(REDIS_KEY_PREFIX).append(":")
                      .append(REDIS_KEY_VERSION).append(":")
                      .append(referrer).append(":")
                      .append(url).toString();
    }

    public boolean isVisited(String referrer, String url){

        try (Jedis jedis = RedisPoolUtil.getInstance().getResource(RedisPoolUtil.DATABASES.VISITED_URLS)) {

            return  jedis.exists(getKey(referrer,url));

        }

    }

    public void setVisited(String referrer, String url){
        try (Jedis jedis = RedisPoolUtil.getInstance().getResource(RedisPoolUtil.DATABASES.VISITED_URLS)) {
            jedis.set(getKey(referrer,url),"","NX","EX",REDIS_KEY_EXP_SECONDS);
        };
    }

}
