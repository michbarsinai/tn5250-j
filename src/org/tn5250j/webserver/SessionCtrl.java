package org.tn5250j.webserver;

import com.sun.net.httpserver.HttpExchange;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tn5250j.SessionPanel;
import org.tn5250j.framework.tn5250.Screen5250;
import org.tn5250j.framework.tn5250.ScreenField;
import org.tn5250j.framework.tn5250.ScreenFields;
import org.tn5250j.tools.logging.TN5250jLogFactory;
import org.tn5250j.tools.logging.TN5250jLogger;

/**
 * A web interface for controlling a single AS/400 screen.
 * 
 * @author michael
 */
public class SessionCtrl extends WebCtrl {
    
    private final SessionPanel panel;
    private final String sessionKey;
    private final TN5250jLogger log = TN5250jLogFactory.getLogger(this.getClass());

    public SessionCtrl(SessionPanel panel, String sessionKey) {
        this.panel = panel;
        this.sessionKey = sessionKey;
    }

    public void handle(List<String> path, HttpExchange exchange) throws IOException {
        String actionName = path.remove(0);
        try {
            switch (actionName) {
                case "keys":  sendKeys(exchange); break;
                case "text":  getScreenText(exchange); break;
                case "fields":  handleFields(path,exchange); break;
                case "image": grabScreenImage(exchange); break;
                default:
                    sendText(404, "No action '"+actionName+"' for this session", exchange);
            }
        } catch ( Exception ex ) {
            log.warn("Error handling exception", ex);
            sendText(404, "error: " + ex.getMessage(), exchange);
        }
    }

    private void grabScreenImage(HttpExchange exchange) throws IOException {
        Rectangle bounds = panel.getDrawingBounds();
        BufferedImage img = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        panel.paint(g2);
        g2.dispose();
        
        exchange.getResponseHeaders().set("Content-Type","image/png");
        exchange.sendResponseHeaders(200, 0);
        ImageIO.write(img, "png", exchange.getResponseBody());
        exchange.getResponseBody().close();
        
    }

    private void getScreenText(HttpExchange exchange) throws IOException {
        final Screen5250 scrn = panel.getScreen();
        char[] content = scrn.getScreenAsChars();
        int screenWidth = scrn.getColumns();
        
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(200, 0);
        
        try ( BufferedWriter out = new BufferedWriter(new OutputStreamWriter(exchange.getResponseBody())) ) {
            for ( int pos=0; pos<content.length; pos+=screenWidth ) {
                out.write( new String(content, pos, screenWidth) );
                out.write("\n");
            }
        }
    }

    private void sendKeys(HttpExchange exchange) throws IOException {
        try {
            String keysToSend = readRequestText(exchange);
            log.info(sessionKey + ": sending keys '" + keysToSend + "'");
            panel.getScreen().sendKeys(keysToSend);
            sendText(200,"keys sent OK", exchange);
        } catch ( Exception ex ) {
            log.warn("Error handling exception in sendkeys: ", ex);
            sendText(500, "Error: " + ex.getMessage(), exchange);
        }
    }
    
    private void handleFields( List<String> path, HttpExchange exchange ) throws IOException {
        if ( path.isEmpty() || path.get(0).trim().isEmpty() ) {
            listScreenFields(exchange);
            
        } else {
            String action = path.remove(0).trim().toLowerCase();
            if ( action.equals("go") ) {
                String content = readRequestText(exchange).toLowerCase().trim();
                switch (content) {
                    case "prev":
                        panel.getScreen().getScreenFields().gotoFieldPrev(); 
                        exchange.getRequestBody().close();
                        break;
                        
                    case "next": 
                        panel.getScreen().getScreenFields().gotoFieldNext(); 
                        exchange.getRequestBody().close();
                        break;
                    
                    default:
                        try {
                            int fieldId = Integer.parseInt(content.trim());
                            final Screen5250 screen = panel.getSession().getScreen();
                            int fieldCount = screen.getScreenFields().getFieldCount();
                            if ( fieldId <= fieldCount ) {
                                if ( screen.gotoField(fieldId) ) {
                                    sendText(200, "At field " + fieldId, exchange);
                                } else {
                                    sendText(500, "Cannot move to " + fieldId, exchange);
                                }
                            } else {
                                sendText(404, "Max field count is " + fieldCount + ", field " + fieldId + " not found.", exchange);
                            }
                        } catch (NumberFormatException nfe) {
                            sendText(400, "accepted values: prev, next, numeric field id", exchange);                        
                        }
                }
                
            } else {
                // action is field id.
                ScreenField fld;
                if ( action.equals("first") ) {
                    fld = panel.getScreen().getScreenFields().getFirstInputField();
                } else {
                    try {
                        int numId = Integer.parseInt(action);
                        final Optional<ScreenField> fldOpt = Arrays.asList(panel.getScreen().getScreenFields().getFields()).stream()
                            .filter( f -> f.getFieldId()==numId ).findAny();
                        if ( fldOpt.isEmpty() ) {
                            sendText(404, "Field with id " + numId + " not found", exchange);
                            return;
                        } else {
                            fld = fldOpt.get();
                        }
                        
                    } catch (NumberFormatException nfe){
                        sendText(400, "Illegal id:" + nfe.getMessage(), exchange);
                        return;
                    }
                }
                
                switch ( exchange.getRequestMethod() ) {
                    case "PUT":
                        String keys = readRequestText(exchange);
                        fld.setString(keys);
                        panel.getScreen().updateScreen();
                        sendText(200,"Set string of field " + fld.getFieldId() + " to '" + keys + "'", exchange);
                        break;
                    case "GET":
                        JSONObject o = fld2Json(fld, panel.getScreen().getScreenFields() );
                        sendText(200, o.toString(), exchange);
                        break;
                    default:
                        sendText(405,"Fields allow PUT, GET", exchange);
                        
                }
            }
        }
    }
    
    private void listScreenFields(HttpExchange exchange) throws IOException {
        final ScreenFields screenFields = panel.getScreen().getScreenFields();
        
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, 0);
        
        JSONArray outArr = new JSONArray();
        for ( ScreenField fld : screenFields.getFields() ) {
            JSONObject obj = fld2Json(fld, screenFields);
            outArr.put(obj);
        }
                
        try ( BufferedWriter out = new BufferedWriter(new OutputStreamWriter(exchange.getResponseBody())) ) {
            out.write(outArr.toString());
        }
    }

    private JSONObject fld2Json(ScreenField fld, final ScreenFields screenFields) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", fld.getFieldId());
        obj.put("is_current", (screenFields.getCurrentField().getFieldId()==fld.getFieldId()));
        obj.put("startPos", fld.startPos());
        obj.put("endPos", fld.endPos());
        obj.put("startCol", fld.startCol());
        obj.put("startRow", fld.startRow());
        obj.put("adjustment", fld.getAdjustment());
        obj.put("attr", fld.getAttr());
        obj.put("CurrentPos", fld.getCurrentPos());
        obj.put("CursorCol", fld.getCursorCol());
        obj.put("CursorProgression", fld.getCursorProgression());
        obj.put("getCursorRow", fld.getCursorRow());
        obj.put("FCW1", fld.getFCW1());
        obj.put("FCW2", fld.getFCW2());
        obj.put("FFW1", fld.getFFW1());
        obj.put("FFW2", fld.getFFW2());
        obj.put("FieldLength", fld.getFieldLength());
        obj.put("FieldShift", fld.getFieldShift());
        obj.put("HighlightedAttr", fld.getHighlightedAttr());
        obj.put("Length", fld.getLength());
        obj.put("isAutoEnter", fld.isAutoEnter());
        obj.put("isBypassField", fld.isBypassField());
        obj.put("isContinued", fld.isContinued());
        obj.put("isContinuedFirst", fld.isContinuedFirst());
        obj.put("isContinuedMiddle", fld.isContinuedMiddle());
        obj.put("isContinuedLast", fld.isContinuedLast());
        obj.put("isDupEnabled", fld.isDupEnabled());
        obj.put("isFER", fld.isFER());
        obj.put("isHiglightedEntry", fld.isHiglightedEntry());
        obj.put("isMandatoryEnter", fld.isMandatoryEnter());
        obj.put("isNumeric", fld.isNumeric());
        obj.put("isRightToLeft", fld.isRightToLeft());
        obj.put("isSelectionField", fld.isSelectionField());
        obj.put("isSignedNumeric", fld.isSignedNumeric());
        obj.put("isToUpper", fld.isToUpper());
        obj.put("Content", fld.getString());
        return obj;
    }

    void deleteSesstion() {
        panel.disconnect();
        panel.fireCloseSession();
    }

    SessionPanel getPanel() {
        return panel;
    }
    
}
