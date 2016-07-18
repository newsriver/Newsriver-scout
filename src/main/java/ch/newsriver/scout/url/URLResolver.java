package ch.newsriver.scout.url;

import ch.newsriver.data.html.HTML;
import ch.newsriver.util.HTMLUtils;
import ch.newsriver.util.http.HttpClientPool;
import ch.newsriver.util.normalization.url.URLUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.RedirectLocations;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by eliapalme on 31/03/16.
 */
public class URLResolver {

    private static final Logger logger = LogManager.getLogger(URLResolver.class);

    private static final HashSet<String> bannedHosts = new HashSet<String>();
    static final String MOBILE_USER_AGENT = "Mozilla/5.0 (iPhone; U; CPU iPhone OS 3_0 like Mac OS X; en-us) AppleWebKit/528.18 (KHTML, like Gecko) Version/4.0 Mobile/7A341 Safari/528.16";
    static final String DEFAUL_USER_AGENT = "curl/7.24.0 (x86_64-apple-darwin12.0) libcurl/7.24.0 OpenSSL/0.9.8r zlib/1.2.5";
    static final String DESKTOP_EMULATE_USER_AGENT = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36";

    static final Pattern REFRESHMETAREGEX = Pattern.compile("meta.+content=\\\".+url=(.*)\\\".*");
    static final Pattern GOOGLEREDIRECTSREGEX = Pattern.compile(".+www.google.com/url.+url=([^&]*).*?");

    static {
        //Google
        bannedHosts.add("www.google.com");
        bannedHosts.add("news.google.com");
        bannedHosts.add("news.google.com");
        bannedHosts.add("www.google.co.uk");
        bannedHosts.add("plus.google.com");
        //Twitter
        bannedHosts.add("twitter.com");
        bannedHosts.add("www.twitter.com");
        //Facebook
        bannedHosts.add("facebook.com");
        bannedHosts.add("www.facebook.com");
        bannedHosts.add("www.facebook.com");
    }



    private static URLResolver instance=null;

    private URLResolver(){

    }

    public static synchronized URLResolver getInstance(){

        if(instance==null){
            instance = new URLResolver();
        }
        return instance;
    }

    public String resolveURL(String rawURL) throws InvalidURLException{

/*
        try {
            articleId = URLLookup.getInstance().getUrlId(inputSource.getURL());
        } catch (MalformedURLException ex) {
            java.util.logging.Logger.getLogger(ArticleImporterJob.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
*/


    //Try to clean URL by following redirects and removing google analytics campaign queries
    String URL = null;
    try {
        URL = resolveUrl(rawURL,false,null);
    } catch (MalformedURLException ex) {
        logger.warn("Unable to clean stand alone url. Article=" + rawURL + " " + ex.getMessage());
    } catch (URISyntaxException ex) {
        logger.warn("Unable to clean stand alone url. Article=" + rawURL + " " + ex.getMessage());
    } catch (IOException ex) {
        logger.warn("Unable to clean stand alone url. Article=" + rawURL + " " + ex.getMessage());
    } catch (IllegalArgumentException ex) {
        logger.warn("Unable to clean stand alone url. Article=" + rawURL + " " + ex.getMessage());
    }
    if (URL == null) {
        logger.warn("Unable to clean stand alone url. Artile=" + rawURL);
        throw new InvalidURLException("Invalid Article url:" + rawURL);
    }

    String host=null;
    try {
        URI uri;
        uri = new URI(URL);
        host = uri.getHost();
    } catch (URISyntaxException ex) {
        logger.warn("Unable to exctract host from URL", ex);
    }

    if (host == null) {
        logger.warn("Unable to extract host from url. Artile=" + rawURL);
        throw new InvalidURLException("Invalid Article url:" + rawURL);
    }

    if(bannedHosts.contains(host)){
        logger.warn("Unable to import Article, its domain is banned. Artile=" + rawURL);
        throw new InvalidURLException("Banned Article url:" + rawURL);
    }

        return URL;
    }



    public URI escapeURI(String dirtyUrl) {
        try {
            //Next two steps are used to escape disalowed carachters
            URL uri = new URL(URLDecoder.decode(dirtyUrl.trim(),"utf-8")); //decode in case there are urlencoded chars
            URI escaped_uri = new URI(uri.getProtocol(), uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getRef()); //Re-encode and ensure that all caracters are correctly encoded
            return escaped_uri;
        } catch (URISyntaxException ex) {
            logger.fatal("url: " +dirtyUrl, ex);
        } catch (MalformedURLException ex) {
            logger.fatal("url: " +dirtyUrl, ex);
        } catch (UnsupportedEncodingException ex) {
            logger.fatal("url: " +dirtyUrl, ex);
        }
        return null;
    }

    public class ResolveResult {
        public boolean isResolved;
        public String url;
    }



    private String resolveUrl(String dirtyUrl, boolean emulateMobile, String expected) throws MalformedURLException, IOException, URISyntaxException {

        String url =  dirtyUrl;
        ResolveResult result = null;

        //Also detects body redirects like refresh meta
        //but this may cause circular redirects therefore we only loop 3 times
        for(int i=0;i<3;i++) {
            result = resolveUrlInternal(url, emulateMobile ? MOBILE_USER_AGENT : DEFAUL_USER_AGENT, expected);

            if (!result.isResolved && !emulateMobile) { // try to resolve with fake userAgent
                result = resolveUrlInternal(url, DESKTOP_EMULATE_USER_AGENT, expected);
            }


            //Search for GOOGLE redirect URLs
            url = findGoogleRedirect(url);
            if(url!=null){
                continue;
            }


            // Avoid an exception with publisher paywall. For example, new yourk times
            try {
                HTML html = HTMLUtils.getHTML(result.url, emulateMobile);
                //Search for HTML redirects in the page
                url = findMetaRefresh(html.getRawHTML());
                if(url!=null){
                    continue;
                }

            } catch (Exception ex) {}

            //here other kind of HTML redirects could be added, like javascript redirects

            break;
        }

        if(result==null)return null;
        return result.url;
    }

    public String findMetaRefresh(String body){

        String bodyFlat = body.replaceAll(" ", "");
        int index =  bodyFlat.indexOf("meta");
        while (index>0){
            bodyFlat = bodyFlat.substring(index,bodyFlat.length());
            int end =  bodyFlat.indexOf(">");
            if(end <= 0){
                break;
            }
            String meta = bodyFlat.substring(0,end);

            if (meta.indexOf("http-equiv=\"refresh\"") <=0) {
                break;
            }

            Matcher metaMatch = REFRESHMETAREGEX.matcher(meta);
            if(metaMatch.matches() && metaMatch.groupCount()==1) {
                return StringUtils.strip(metaMatch.group(1), "'");
            }
            index= bodyFlat.indexOf("meta",4);
        }

        return null;
    }

    public String findGoogleRedirect(String url){

        Matcher googleMatch = GOOGLEREDIRECTSREGEX.matcher(url);
        if(googleMatch.matches() && googleMatch.groupCount()==1) {
            try {
                return URLDecoder.decode(googleMatch.group(1), "utf-8");
            }catch (Exception e){
                logger.error("Unable to decode google redirect url",e);
            }
        }
        return null;
    }



    public ResolveResult resolveUrlInternal(String dirtyUrl, String userAgent, String expected) throws MalformedURLException, IOException, URISyntaxException {
        // Fix: We are doing this cleaning twice, otherwise we are loosing URL when crawling
        if (dirtyUrl != null) {
            dirtyUrl = dirtyUrl.trim().replaceAll(" ", "%20");
        }

        ResolveResult result = new ResolveResult();
        result.isResolved = false;
        result.url = dirtyUrl;

        String url = dirtyUrl;
        HttpGet httpGetRequest = null;
        try {
            HttpClientContext context = new HttpClientContext();

            URI escaped_uri = escapeURI(url);

            httpGetRequest = new HttpGet(escaped_uri);
            httpGetRequest.addHeader("User-Agent", userAgent);


            //logger.log(Level.INFO, "URL-Cleaner execute request url="+dirtyUrl);
            HttpResponse response = null;
            try {
                response = HttpClientPool.getHttpClientInstance().execute(httpGetRequest, context);
            } catch (ClientProtocolException ex) {

                //Workarond in case some stupid wesites have a wrongly formatted redirect
                if (ex.getCause()!=null && ex.getCause().getClass().isAssignableFrom(ProtocolException.class)) {
                    if (ex.getCause().getCause().getClass().isAssignableFrom(URISyntaxException.class)) {
                        URISyntaxException e = (URISyntaxException) ex.getCause().getCause();

                        URL uri = new URL(URLDecoder.decode(e.getInput(),"utf-8"));
                        escaped_uri = new URI(uri.getProtocol(), uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getRef());
                        httpGetRequest = new HttpGet(escaped_uri);
                        httpGetRequest.addHeader("User-Agent", userAgent);
                        response = HttpClientPool.getHttpClientInstance().execute(httpGetRequest, context);
                    }
                }
            } catch (Exception ex) {
                //Check new exception;
                logger.fatal("Unable to resolve URL: " + url, ex);
                throw ex;
            }

            //we no longer check for: response.getStatusLine().getStatusCode() == HttpStatus.SC_OK
            //because the redirect could lead to a not found but still the resolved URL should be used.
            if (response != null) {
                URI uri = extractHttpLocation(httpGetRequest, context);
                url = uri.toASCIIString();
                result.isResolved = true;
            }

            // TODO: Strange behaviour!!!
            // We try to find the first URL matching the expectedURL string.
            // This is the hack to prevent to have a complex regular expression required to gemerate mobile url
            if (expected != null) {
                List<URI> urls = context.getRedirectLocations();
                if (urls != null) {
                    for (URI re_url : urls) {
                        if (re_url.toASCIIString().toLowerCase().contains(expected.toLowerCase())) {
                            url = re_url.toASCIIString();
                            break;
                        }
                    }
                }
            }
            result.url = URLUtils.normalizeUrl(url, false);

        } finally {

            if (httpGetRequest != null) {
                httpGetRequest.releaseConnection();
            }

        }

        return result;
    }


    private URI extractHttpLocation(HttpUriRequest originalRequest, HttpContext context) {
        try {
            // last request-uri
            final HttpRequest request = (HttpRequest) context
                    .getAttribute(HttpCoreContext.HTTP_REQUEST);
            if (request == null) {
                return null;
            }
            final String requestUri = request.getRequestLine().getUri();
            final URIBuilder uribuilder = new URIBuilder(requestUri);
            // last target origin
            final HttpHost target = (HttpHost) context
                    .getAttribute(HttpCoreContext.HTTP_TARGET_HOST);
            if (target != null) {
                uribuilder.setScheme(target.getSchemeName());
                uribuilder.setHost(target.getHostName());
                // normalize port
                int port = target.getPort();
                if (80 != port && 443 != port) {
                    uribuilder.setPort(port);
                }
            }
            // read interpreted fragment identifier from redirect locations
            String frag = null;
            final RedirectLocations redirectLocations = (RedirectLocations) context
                    .getAttribute(DefaultRedirectStrategy.REDIRECT_LOCATIONS);
            if (redirectLocations != null) {
                List<URI> all = redirectLocations.getAll();
                for (int i = all.size() - 1; frag == null && i >= 0; i--) {
                    frag = all.get(i).getFragment();
                }
            }
            // read interpreted fragment identifier from original request
            if (frag == null && originalRequest != null) {
                frag = originalRequest.getURI().getFragment();
            }
            uribuilder.setFragment(frag);
            return uribuilder.build();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public static class  InvalidURLException extends Exception{

        public InvalidURLException(String message) {
            super(message);
        }

    }
}
