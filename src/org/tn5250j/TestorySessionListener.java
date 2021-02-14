/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tn5250j;

import java.util.Arrays;
import org.tn5250j.event.SessionChangeEvent;
import org.tn5250j.event.SessionListener;
import org.tn5250j.framework.tn5250.Screen5250;
import org.tn5250j.framework.tn5250.ScreenFields;

/**
 *
 * @author michael
 */
public class TestorySessionListener implements SessionListener {
    
    private final Session5250 session;

    public TestorySessionListener(Session5250 session) {
        this.session = session;
    }
    
    @Override
    public void onSessionChanged(SessionChangeEvent changeEvent) {
        System.out.println("Session changed: " + changeEvent.getMessage() + " (" + changeEvent.getState() + ")");
        dumpSessionState();
    }
    
    private void dumpSessionState() {
        Screen5250 screen = session.getScreen();
        
        if ( screen != null ) {
            ScreenFields screenFields = screen.getScreenFields();
            if ( screenFields != null ) {
                System.out.println("Current Fields");
                Arrays.asList(screenFields.getFields()).forEach( f -> {
                    System.out.println(f.getFieldId() + ": " + f.getString() );
                });
            }
        }
    }
}
