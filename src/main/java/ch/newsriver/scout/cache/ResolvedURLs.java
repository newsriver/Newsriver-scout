package ch.newsriver.scout.cache;

import ch.newsriver.dao.RedisPoolUtil;
import redis.clients.jedis.Jedis;

/**
 * Created by eliapalme on 01/04/16.
 */
public class ResolvedURLs {

    private final static String REDIS_KEY_PREFIX     = "resURL";
    private final static String REDIS_KEY_VERSION     = "1";
    private final static Long   REDIS_KEY_EXP_SECONDS = 60l * 60l * 24l * 30l * 3l; //about 3 months
    static ResolvedURLs instance=null;

    public static synchronized ResolvedURLs getInstance(){
        if(instance==null){
            instance = new ResolvedURLs();
        }
        return instance;
    }

    private ResolvedURLs(){}

    private String getKey(String url){
        StringBuilder builder = new StringBuilder();
        return builder.append(REDIS_KEY_PREFIX).append(":")
                .append(REDIS_KEY_VERSION).append(":")
                .append(url).toString();
    }

    public String getResolved(String url){
        try (Jedis jedis = RedisPoolUtil.getInstance().getResource(RedisPoolUtil.DATABASES.RESOLVED_URLS)) {
            return  jedis.get(getKey(url));
        }
    }

    public void setResolved(String url,String resolvedURL){
        try (Jedis jedis = RedisPoolUtil.getInstance().getResource(RedisPoolUtil.DATABASES.RESOLVED_URLS)) {
            jedis.set(getKey(url),resolvedURL,"NX","EX",REDIS_KEY_EXP_SECONDS);
        }
    }

}
