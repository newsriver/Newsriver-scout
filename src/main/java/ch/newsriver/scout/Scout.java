package ch.newsriver.scout;

import ch.newsriver.scout.feed.FeedFetcher;

import java.io.IOException;

/**
 * Created by eliapalme on 10/03/16.
 */
public class Scout implements Runnable  {

    private boolean run = false;



    public Scout() throws IOException {

        run = true;





    }

    public void stop(){
        run = false;
    }


    public void run() {




        while (run) {

            try {
                Thread.sleep(5000);
                FeedFetcher feedFetcher = new FeedFetcher();
                feedFetcher.process("http://feeds.tio.ch/Ticinonline-NewsTicino");

            } catch (InterruptedException ex) {
                return;
            }
        }
    }

}
