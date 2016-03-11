package ch.newsriver.scout.feed;

import java.util.List;

/**
 * Created by eliapalme on 11/03/16.
 */
public class FeedFetcherResult {

    private List<FeedURL> urls;
    private int remainingUrls;
    private int totalUrls;

    public List<FeedURL> getUrls() {
        return urls;
    }

    public void setUrls(List<FeedURL> urls) {
        this.urls = urls;
    }

    public int getRemainingUrls() {
        return remainingUrls;
    }

    public void setRemainingUrls(int remainingUrls) {
        this.remainingUrls = remainingUrls;
    }

    public int getTotalUrls() {
        return totalUrls;
    }

    public void setTotalUrls(int totalUrls) {
        this.totalUrls = totalUrls;
    }

    public FeedFetcherResult(List<FeedURL> urls, int remainingUrls, int totalUrls) {
        this.urls = urls;
        this.remainingUrls = remainingUrls;
        this.totalUrls = totalUrls;
    }
}
