package ch.newsriver.scout.feed;

/**
 * Created by eliapalme on 11/03/16.
 */
public class FeedURL {

    private String referal;
    private String url;
    private String title;
    private Long   publicationDate;
    private Long   discoveryDate;
    private String headlines;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(Long publicationDate) {
        this.publicationDate = publicationDate;
    }

    public Long getDiscoveryDate() {
        return discoveryDate;
    }

    public void setDiscoveryDate(Long discoveryDate) {
        this.discoveryDate = discoveryDate;
    }

    public String getHeadlines() {
        return headlines;
    }

    public void setHeadlines(String headlines) {
        this.headlines = headlines;
    }

    public String getReferal() {
        return referal;
    }

    public void setReferal(String referal) {
        this.referal = referal;
    }
}
