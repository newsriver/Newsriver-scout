package ch.newsriver.scout;

import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;

/**
 * Created by eliapalme on 10/03/16.
 */
public class Console extends NanoHTTPD {


    public Console(int port) throws IOException {
        super(port);

    }

    @Override
    public Response serve(IHTTPSession session) {

        StringBuilder body = new StringBuilder();
        body.append("<html><body style='font-family: monospace'>");
        body.append(ScoutMain.getManifest().replace("\n","<br/>").replace(" ","&nbsp;"));
        body.append("</body></html>");


        return newFixedLengthResponse(body.toString());

    }
}
