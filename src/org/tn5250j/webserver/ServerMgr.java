package org.tn5250j.webserver;

import com.sun.net.httpserver.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import static java.util.stream.Collectors.joining;
import org.tn5250j.My5250;
import org.tn5250j.SessionPanel;
import org.tn5250j.TN5250jConstants;
import org.tn5250j.connectdialog.Configure;
import org.tn5250j.connectdialog.SessionsDataModel;
import org.tn5250j.framework.common.SessionManager;
import org.tn5250j.tools.logging.TN5250jLogFactory;
import org.tn5250j.tools.logging.TN5250jLogger;

/**
 * The web server used for external automation.
 * 
 * @author michael
 */
public class ServerMgr extends WebCtrl {
    
    HttpServer server;
        
    private final SessionManager sessions;
    private final Properties properties;
    private final My5250 terminalApp;
    private final TN5250jLogger log = TN5250jLogFactory.getLogger(this.getClass());
    private final Map<String, SessionCtrl> activeSessions = new HashMap<>();

    public ServerMgr(SessionManager sessions, Properties properties, My5250 terminalApp) {
        this.sessions = sessions;
        this.properties = properties;
        this.terminalApp = terminalApp;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(8421),0);
        
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                System.out.println("global handler hit. URI: " + exchange.getRequestURI() );
                sendText(200, "AS/400 HTTP Automation by Testory\n", exchange);
            }
        });
        
        server.createContext("/configs/", makeConfigsLister() );
        server.createContext("/sessions/", makeSessionEndpoint() );
        
        server.start();
    }
    
    private HttpHandler makeSessionEndpoint() {
        return new HttpHandler(){
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String path = exchange.getRequestURI().getPath();
                if ( exchange.getRequestMethod().equals("PUT") && path.matches("/sessions/[^/]*") ) {
                     startNewSession(exchange);
                     
                } else if ( path.equals("/sessions/") ){
                    // handle global operations.
                    switch (exchange.getRequestMethod()){
                        case "GET": listActiveSessions(exchange); break;
                        default:
                            sendText(405,("No endpoint for http call " + 
                                         exchange.getRequestMethod() +
                                         exchange.getRequestURI() + "\n"), exchange );

                    }
                    
                } else {
                    // path is of the form /sessions/(.+)$
                    String[] compArr = path.split("/",-1);
                    List<String> comps = new LinkedList<>(Arrays.asList(compArr));
                    comps.remove(0); // first element always empty (>/<XXXX)
                    comps.remove(0); // sessions is now deleted
                    String sessionName = comps.remove(0);
                    SessionCtrl asc = activeSessions.get(sessionName);
                    if ( asc != null ) {
                        if ( comps.isEmpty() || (comps.size()==1 && comps.get(0).trim().isEmpty()) ) {
                            if ( "DELETE".equals(exchange.getRequestMethod()) ) {
                                asc.deleteSesstion();
                                final SessionCtrl removed = activeSessions.remove(sessionName);
                                terminalApp.getMainGUIFrame().removeSessionView(removed.getPanel());
                                sendText(204,null,exchange);
                                
                            } else sendText(405,"Method " + exchange.getRequestMethod() + " not allowed", exchange );
                        } else {
                            asc.handle(comps, exchange);
                        }
                        
                    } else {
                        sendText(404,"No active session named '" + 
                                     sessionName + "'.\n", exchange );
                    }
                }
                
            }
        };
    }
    
    private HttpHandler makeConfigsLister() {
        return new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, 0);
                log.info("Listing session types");
                
                StringBuilder sb = new StringBuilder();
                Enumeration<Object> e = properties.keys();
                String ses = null;
                sb.append("[");
                boolean hadPrev = false;
                while (e.hasMoreElements()) {
                  ses = (String) e.nextElement();

                  if (!ses.startsWith("emul.")) {
                    String[] args = new String[TN5250jConstants.NUM_PARMS];
                    Configure.parseArgs(properties.getProperty(ses), args);
                    SessionsDataModel sdm = new SessionsDataModel(ses, args[0], false);
                    if ( hadPrev ) sb.append(", ");
                    sb.append("\"").append(sdm.name).append("\"");
                    hadPrev = true;
                  }
                  
                }
                
                sb.append("]");
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write( sb.toString().getBytes() ) ;
                    os.flush();
                }
            }
        };
    }
    
    private void startNewSession( HttpExchange exchange ) throws IOException {
        
        String sel = readRequestText(exchange).trim();
        
        log.info("Starting new session named '" + sel + "'");
        
		if (sel != null) {
			String selArgs = properties.getProperty(sel);
            if ( selArgs != null ) {
                String[] sessionArgs = new String[TN5250jConstants.NUM_PARMS];
                My5250.parseArgs(selArgs, sessionArgs);
                
                String[] comps = exchange.getRequestURI().getPath().split("/");
                String sessionName = comps[comps.length-1];
                SessionPanel sp = terminalApp.newSession(sel, sessionArgs);
                
                activeSessions.put(sessionName, new SessionCtrl(sp, sessionName));
                
                exchange.sendResponseHeaders(201, 0);
                exchange.getResponseBody().close();
                
            } else {
                sendText(400, "No configuration named '" + sel + "'", exchange);
                exchange.sendResponseHeaders(400, 0);
                try (OutputStream os = exchange.getResponseBody();
                     PrintWriter out = new PrintWriter(os)) {
                   out.println("No configuration named '" + sel + "'");
                }
            }
            
		} else {
            exchange.sendResponseHeaders(400, 0);
		}
    }
    
    private void listActiveSessions( HttpExchange exchange ) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, 0);
        
        try (OutputStream os = exchange.getResponseBody();
             PrintWriter out = new PrintWriter(os)) {
            out.print("[");
            if ( ! activeSessions.isEmpty() ) {
                String idJsonList = activeSessions.keySet().stream()
                    .collect( joining("\",\"", "\"", "\""));
                out.print(idJsonList);
            }
            out.print("]");
        }
    }
    
}
