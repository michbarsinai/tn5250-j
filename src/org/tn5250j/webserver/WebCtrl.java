package org.tn5250j.webserver;

import com.sun.net.httpserver.HttpExchange;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * A base class providing utility methods for web-based controllers.
 * @author michael
 */
public abstract class WebCtrl {
    
    protected void sendText( int code, String text, HttpExchange exchange ) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(code, (text!=null) ? text.length(): -1);
        try (OutputStream os = exchange.getResponseBody()) {
            if ( text != null ) os.write(text.getBytes());
        }
    }
    
    protected String readRequestText( HttpExchange exchange ) throws IOException {
        StringBuilder inBld = new StringBuilder();
        try ( BufferedReader in = new BufferedReader(new InputStreamReader(exchange.getRequestBody())) ) {
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
