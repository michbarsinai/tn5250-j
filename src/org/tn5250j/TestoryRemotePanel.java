/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tn5250j;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.util.Arrays;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import org.panelmatic.PanelBuilder.HeaderLevel;
import org.panelmatic.PanelMatic;
import org.panelmatic.PanelPostProcessor;
import org.panelmatic.componentbehavior.Modifiers;
import org.tn5250j.framework.tn5250.Screen5250;
import org.tn5250j.framework.tn5250.ScreenFields;

/**
 *
 * @author michael
 */
public class TestoryRemotePanel {
    JButton refreshBtn = new JButton("Refresh");
    JPanel fieldBtnCtnr = new JPanel();
    JTextField keysTxt = new JTextField();
    JButton sendBtn = new JButton("Send Keys");
    JTextArea screenTxt = new JTextArea();
    
    Session5250 session;
    
    public void setSession( Session5250 aSession ) {
        session = aSession;
        updatePanel();
    }
    
    public void showWindow() {
        SwingUtilities.invokeLater(()->{
            JFrame window = new JFrame("Testory Remote");
            window.getContentPane().add( createPanel() );
            window.pack();
            window.setVisible(true);
        });
    }
    
    public JComponent createPanel() { 
        fieldBtnCtnr.setLayout( new BoxLayout(fieldBtnCtnr, BoxLayout.PAGE_AXIS) );
        
        screenTxt.setFont( new Font("Courier", Font.PLAIN, 12) );
        screenTxt.setEditable(false);
        screenTxt.setBackground(Color.DARK_GRAY);
        screenTxt.setForeground(Color.GREEN);
        screenTxt.setMinimumSize( new Dimension(200, 400) );
        
        refreshBtn.addActionListener(e->updatePanel());
        ActionListener sendKeys = e->{
            session.getScreen().sendKeys(keysTxt.getText());
        };
        
        sendBtn.addActionListener(sendKeys);
        keysTxt.addActionListener(sendKeys);
        
        return PanelMatic.begin()
            .addHeader(HeaderLevel.H1, "Testory AS400 Remote")
            .add(refreshBtn)
            .add("Fields", fieldBtnCtnr)
            .addHeader(HeaderLevel.H2, "Send Text")
            .add( keysTxt )
            .add( sendBtn, Modifiers.L_END, Modifiers.NO_STRETCH )
            .addHeader(HeaderLevel.H2, "Screen")
            .add( screenTxt, Modifiers.GROW )
            .get((PanelPostProcessor) (JComponent jc) -> {
                Border padding = new EmptyBorder(8, 8, 8, 8);
                jc.setBorder(padding);
                return jc;
        });
    }
    
    private void updatePanel() {
        Screen5250 screen = session.getScreen();
        
        fieldBtnCtnr.removeAll();
        if ( screen != null ) {
            screen.dumpScreen();
            
            ScreenFields screenFields = screen.getScreenFields();
            if ( screenFields != null ) {
                System.out.println("Current Fields");
                Arrays.asList(screenFields.getFields()).forEach( f -> {
                    fieldBtnCtnr.add( new JButton("field #" + f.getFieldId() + ":" + f.getString() ));
                });
                fieldBtnCtnr.invalidate();
            }
            screenTxt.setText(drawScreen(screen));
        }
    }
    
    private String drawScreen( Screen5250 scrn ) {
        char[] content = scrn.getScreenAsChars();
        System.out.println("content = " + content);
        int screenWidth = scrn.getColumns();
        
        StringBuilder sb = new StringBuilder();
        for ( int pos=0; pos<content.length; pos+=screenWidth ) {
            sb.append( new String(content, pos, screenWidth) );
            sb.append("\n");
        }
        
        return sb.toString();
    }
}
