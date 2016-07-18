import ch.newsriver.data.source.FeedSource;
import ch.newsriver.data.source.SourceFactory;
import ch.newsriver.data.source.URLSeedSource;
import org.junit.Test;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by eliapalme on 17/06/16.
 */
public class AddFeedSource {

    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");





    @Test
    public void addFeedSources() throws Exception {


        String allURLs = "http://www.blick.ch/,http://www.blick.ch/news/,http://www.blick.ch/sport/,http://www.blick.ch/people-tv/,http://www.blick.ch/auto/,http://www.tagesanzeiger.ch/,http://www.tagesanzeiger.ch/schweiz/,http://www.tagesanzeiger.ch/ausland/,http://www.tagesanzeiger.ch/wirtschaft/,http://www.tagesanzeiger.ch/stock_market/uebersicht/,http://www.tagesanzeiger.ch/kultur/,http://www.tagesanzeiger.ch/reisen/,http://www.tagesanzeiger.ch/wissen/,http://www.tagesanzeiger.ch/wissen/,http://www.tagesanzeiger.ch/blogs/,http://www.tagesanzeiger.ch/panorama/,http://www.letemps.ch/,http://www.letemps.ch/monde,http://www.letemps.ch/suisse,http://www.letemps.ch/economie,http://www.letemps.ch/culture,http://www.letemps.ch/sciences,http://www.letemps.ch/sport,http://www.letemps.ch/societe,http://www.letemps.ch/lifestyle,http://www.aargauerzeitung.ch/,http://www.aargauerzeitung.ch/aargau,http://www.aargauerzeitung.ch/schweiz,http://www.aargauerzeitung.ch/ausland,http://www.aargauerzeitung.ch/wirtschaft,http://www.aargauerzeitung.ch/sport,http://www.aargauerzeitung.ch/leben,http://www.aargauerzeitung.ch/kultur,http://www.aargauerzeitung.ch/blaulicht,http://www.suedostschweiz.ch/,http://www.suedostschweiz.ch/meinegemeinde,http://www.suedostschweiz.ch/suedostschweiz,http://www.suedostschweiz.ch/ueberregionales,http://www.suedostschweiz.ch/wirtschaft,http://www.suedostschweiz.ch/blaulicht,http://www.suedostschweiz.ch/sport,http://www.suedostschweiz.ch/unterhaltung,http://www.nzz.ch/,http://www.nzz.ch/meinung/,http://www.nzz.ch/international/,http://www.nzz.ch/wirtschaft/,http://www.nzz.ch/finanzen/,http://www.nzz.ch/schweiz/,http://www.nzz.ch/feuilleton/,http://www.nzz.ch/zuerich/,http://www.nzz.ch/sport/,http://www.nzz.ch/wissenschaft/,http://www.nzz.ch/panorama/,http://www.srf.ch/,http://www.srf.ch/news,http://www.srf.ch/sport,http://www.srf.ch/kultur,http://www.srf.ch/news/schweiz,http://www.srf.ch/news/international,http://www.srf.ch/news/wirtschaft,http://www.srf.ch/news/panorama,http://www.luzernerzeitung.ch/,http://www.luzernerzeitung.ch/nachrichten/zentralschweiz/lu/,http://www.luzernerzeitung.ch/nachrichten/schweiz/,http://www.luzernerzeitung.ch/nachrichten/international/,http://www.luzernerzeitung.ch/nachrichten/wirtschaft/,http://www.luzernerzeitung.ch/nachrichten/kultur/,http://www.luzernerzeitung.ch/nachrichten/panorama/,http://www.luzernerzeitung.ch/nachrichten/digital/,http://www.luzernerzeitung.ch/sport/,http://www.luzernerzeitung.ch/magazin/,http://www.20min.ch/,http://www.20min.ch/schweiz/,http://www.20min.ch/ausland/,http://www.20min.ch/finance/,http://www.20min.ch/sport/,http://www.20min.ch/people/,http://www.20min.ch/entertainment/,http://www.20min.ch/digital/,http://www.20min.ch/wissen/,http://www.20min.ch/leben/,http://www.20min.ch/community/,http://www.watson.ch/,http://www.watson.ch/Schweiz,http://www.watson.ch/International,http://www.watson.ch/Wirtschaft,http://www.watson.ch/Sport,http://www.watson.ch/Digital,http://www.watson.ch/Popul%C3%A4rkultur,http://www.watson.ch/Wissen,http://www.watson.ch/Blogs,http://www.sonntagszeitung.ch/nachrichten,http://www.sonntagszeitung.ch/fokus,http://www.sonntagszeitung.ch/sport,http://www.sonntagszeitung.ch/wirtschaft,http://www.sonntagszeitung.ch/gesellschaf,http://www.sonntagszeitung.ch/kultur,http://dok.sonntagszeitung.ch/,http://www.tagblatt.ch/,http://www.tagblatt.ch/ostschweiz/,http://www.tagblatt.ch/sport/,http://www.tagblatt.ch/magazin/,http://www.tagblatt.ch/nachrichten/schweiz/,http://www.tagblatt.ch/nachrichten/international/,http://www.tagblatt.ch/nachrichten/wirtschaft/,http://www.tagblatt.ch/nachrichten/panorama/,http://www.tagblatt.ch/nachrichten/kultur/,http://www.tagblatt.ch/nachrichten/polizeinews/,https://www.swissquote.ch/,http://www.rsi.ch/,http://www.rsi.ch/news/,http://www.rsi.ch/news/ticino-e-grigioni-e-insubria/,http://www.rsi.ch/news/svizzera/,http://www.rsi.ch/news/svizzera/,http://www.rsi.ch/news/economia/,http://www.rsi.ch/news/vita-quotidiana/,http://www.rsi.ch/news/dossier/,http://www.rts.ch/,http://www.rts.ch/info/,http://www.rts.ch/info/suisse/,http://www.rts.ch/info/monde/,http://www.rts.ch/info/economie/,http://www.rts.ch/info/culture/,http://www.swissinfo.ch/,http://www.swissinfo.ch/eng/latest-news,http://www.swissinfo.ch/eng/politics,http://www.swissinfo.ch/eng/foreign-affairs,http://www.swissinfo.ch/ita,http://www.swissinfo.ch/eng,http://www.swissinfo.ch/ger,http://www.swissinfo.ch/fre,http://www.swissinfo.ch/spa,http://www.swissinfo.ch/por,http://www.arcinfo.ch/,http://www.arcinfo.ch/articles/regions/,http://www.arcinfo.ch/articles/suisse/,http://www.arcinfo.ch/articles/sports/,http://www.arcinfo.ch/articles/economie/,http://www.arcinfo.ch/articles/monde/,http://www.arcinfo.ch/articles/horlogerie/,http://www.arcinfo.ch/articles/lifestyle/,http://www.fuw.ch/,http://www.fuw.ch/category/unternehmen/,http://www.fuw.ch/category/markte/,http://www.fuw.ch/category/kommentar/,http://www.fuw.ch/category/blogs/,http://www.fuw.ch/dossiers/,http://www.fuw.ch/category/luxus/,http://www.bilanz.ch/,http://www.bilanz.ch/unternehmen,http://www.bilanz.ch/management,http://www.bilanz.ch/invest,http://www.bilanz.ch/luxus,http://www.bilanz.ch/people,http://www.bilanz.ch/ratings/ratings-rankings,http://www.bilanz.ch/immobilien,http://www.bilanz.ch/auto,http://www.finanzen.ch/,http://www.finanzen.ch/nachrichten,http://www.finanzen.ch/finanzplanung,https://www.cash.ch/,https://www.cash.ch/news/top-news,https://www.cash.ch/news/alle,https://www.cash.ch/guru,https://www.cash.ch/insider,http://www.handelszeitung.ch/,http://www.handelszeitung.ch/unternehmen,http://www.handelszeitung.ch/management,http://www.handelszeitung.ch/invest,http://www.handelszeitung.ch/konjunktur,http://www.handelszeitung.ch/politik,http://www.handelszeitung.ch/lifestyle,http://www.handelszeitung.ch/specials,http://www.handelszeitung.ch/blogs,http://www.inside-it.ch/frontend/insideit,http://www.annabelle.ch/,http://www.annabelle.ch/leben,http://www.annabelle.ch/mode,http://www.annabelle.ch/beautyhttp://www.annabelle.ch/kochen,http://www.annabelle.ch/wohnen,http://www.annabelle.ch/reisen,http://www.femina.ch/,http://www.femina.ch/mode/news-mode,http://www.femina.ch/beaute/news-beaute,http://www.femina.ch/people/news-people,http://www.femina.ch/societe/news-societe,http://www.femina.ch/mariage/news-mariage,http://www.femina.ch/loisirs/news-loisirs,http://www.computerworld.ch/,http://www.computerworld.ch/news/,http://www.computerworld.ch/businesspraxis/,http://www.computerworld.ch/tests/,http://www.computerworld.ch/marktanalysen/,http://www.computerworld.ch/management/,http://www.computerworld.ch/whitepapers/,https://www.admin.ch/gov/de/start.html,https://www.admin.ch/gov/fr/accueil.html,https://www.admin.ch/gov/it/pagina-iniziale.html,https://www.admin.ch/gov/rm/pagina-iniziala.html,https://www.admin.ch/gov/en/start.html,https://www.admin.ch/gov/it/pagina-iniziale/documentazione/comunicati-stampa.html,https://www.admin.ch/gov/fr/accueil/documentation/communiques.html,https://www.migrosmagazin.ch/,https://www.migrosmagazin.ch/menschen,https://www.migrosmagazin.ch/leben,https://www.migrosmagazin.ch/kochen,https://www.migrosmagazin.ch/migros-welt,https://www.migrosmagazin.ch/bonus,https://www.migrosmagazine.ch/societe,https://www.migrosmagazine.ch/au-quotidien,https://www.migrosmagazine.ch/cuisine,https://www.migrosmagazine.ch/a-votre-service,https://www.migrosmagazine.ch/migros,https://www.migrosmagazine.ch/profiter,http://www.aufeminin.com/,http://www.aufeminin.com/societe/news-societe-ssc156.html,http://www.aufeminin.com/mode-sc14.html,http://www.aufeminin.com/beaute-sc8.html,http://www.aufeminin.com/maman-sc7.html,http://www.aufeminin.com/psycho-sc2.html,http://www.aufeminin.com/deco-sc13.html,http://www.aufeminin.com/cuisine-sc4.html,http://www.aufeminin.com/societe-sc26.html,http://www.aufeminin.com/culture-sc10.html,http://www.aufeminin.com/voyage-sc41.html,http://www.aufeminin.com/petites-mains-diy-do-it-yourself-sc43.html,http://www.netzwoche.ch/,http://www.netzwoche.ch/news.aspx,http://www.netzwoche.ch/News/Management.aspx,http://www.netzwoche.ch/News/Strategie.aspx,http://www.netzwoche.ch/News/Infrastructure.aspx,http://www.netzwoche.ch/News/Software.aspx,http://www.netzwoche.ch/News/Web.aspx,http://www.netzwoche.ch/News/Communication.aspx,http://www.schweizer-illustrierte.ch/,http://www.schweizer-illustrierte.ch/stars,http://www.schweizer-illustrierte.ch/gesellschaft,http://www.schweizer-illustrierte.ch/lifestyle,http://www.schweizer-illustrierte.ch/blogs,http://www.schweizer-illustrierte.ch/verlosungen,http://www.boleromagazin.ch/,http://www.boleromagazin.ch/category/mode/,http://www.boleromagazin.ch/category/schonheit/,http://www.boleromagazin.ch/category/kunst-und-kultur/,http://www.boleromagazin.ch/category/living/,http://www.boleromagazin.ch/category/reisen/,http://www.boleromagazin.ch/category/kulinarik/,http://www.boleromagazin.ch/category/verlosungen/";

        String[] urlsString = allURLs.split(",");

        List<String> urls = Arrays.asList(urlsString);

        urls.forEach(url -> {
            try {
                URI uri = new URI(url);
                addFeedSource(url);
            }catch (Exception e){
                e.printStackTrace();
            }
        });


    }


    private void addFeedSource(String url){

        FeedSource source = new FeedSource();


        source.setUrl(url);


        source.setHttpStatus("200");
        source.setLastVisit(dateFormatter.format(new Date()));

        SourceFactory.getInstance().setSource(source, true);

    }


}
