package org.tn5250j.webserver;

import com.sun.net.httpserver.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Properties;
import static java.util.stream.Collectors.joining;
import org.tn5250j.framework.common.SessionManager;

/**
 * The web server used for external automation.
 * 
 * @author michael
 */
public class ServerMgr {
    
    HttpServer server;
        
    private final SessionManager sessions;
    private final Properties properties;

    public ServerMgr(SessionManager sessions, Properties properties) {
        this.sessions = sessions;
        this.properties = properties;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(8421),0);
        
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                System.out.println("global handelr hit. URI: " + exchange.getRequestURI() );
                exchange.sendResponseHeaders(200, 0);
                exchange.setAttribute("Content-Type", "text/plain");
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write("AS/400 HTTP Automation by Testory\n".getBytes());
                }
            }
        });
        
        server.createContext("/configs/", configsLister() );
        
        server.start();
    }
    
    private HttpHandler configsLister() {
        return (HttpExchange exchange) -> {
            exchange.sendResponseHeaders(200, 0);
            exchange.setAttribute("Content-Type", "text/plain");
            final StringBuilder sb = new StringBuilder();
            properties.keySet().stream().filter(k->!k.toString().startsWith("emul.")).forEach( k->sb.append(k).append("\n") );
            try (OutputStream os = exchange.getResponseBody()) {
                os.write( sb.toString().getBytes()) ;
            }
        };
    }
    
}
