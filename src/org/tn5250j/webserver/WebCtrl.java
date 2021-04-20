package org.tn5250j.webserver;

import com.sun.net.httpserver.HttpExchange;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Optional;
import org.tn5250j.tools.logging.TN5250jLogFactory;
import org.tn5250j.tools.logging.TN5250jLogger;

/**
 * A base class providing utility methods for web-based controllers.
 * @author michael
 */
public abstract class WebCtrl {
    
    private final TN5250jLogger log = TN5250jLogFactory.getLogger(this.getClass());
    
    protected void sendText( int code, String text, HttpExchange exchange ) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/plain;charset=UTF-8");
        exchange.sendResponseHeaders(code, (text!=null) ? text.length(): -1);
        try (OutputStream os = exchange.getResponseBody()) {
            if ( text != null ) os.write(text.getBytes());
        }
    }
    
    protected String readRequestText( HttpExchange exchange ) throws IOException {
        StringBuilder inBld = new StringBuilder();
        // decide on charset
        String charset = "UTF-8";
        if ( exchange.getRequestHeaders().containsKey("Content-Type") ) {
            final String contentType = exchange.getRequestHeaders().getFirst("Content-Type").toLowerCase();
            if ( contentType.contains("charset") ) {
                String comps[] = contentType.split(";", -1);
                Optional<String> maybeCS = Arrays.asList(comps).stream().filter(s->s.trim().startsWith("charset=")).findAny();
                if ( maybeCS.isPresent() ) {
                    charset = maybeCS.get().split("=")[1].trim();
                }
            }
        }
        log.info("Reading request body using charset '" + charset + "'");
        try ( BufferedReader in = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), charset)) ) {
            String line;
            while( (line=in.readLine()) != null ) {
                inBld.append(line);
                inBld.append("\n");
            }
            inBld.setLength( inBld.length()-1 );
        }
        return inBld.toString();
    }
    
}
