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
import io.codeworth.panelmatic.*;
import io.codeworth.panelmatic.PanelBuilder.HeaderLevel;
import io.codeworth.panelmatic.componentbehavior.Modifiers;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import org.tn5250j.event.ScreenOIAListener;
import org.tn5250j.framework.tn5250.Screen5250;
import org.tn5250j.framework.tn5250.ScreenFields;
import org.tn5250j.framework.tn5250.ScreenOIA;
import org.tn5250j.tools.logging.TN5250jLogFactory;
import org.tn5250j.tools.logging.TN5250jLogger;

enum ScreenPlanes {
    PLANE_TEXT(1),
    PLANE_COLOR(2),
    PLANE_FIELD(3),
    PLANE_EXTENDED(4),
    PLANE_EXTENDED_GRAPHIC(5),
    PLANE_EXTENDED_FIELD(6),
    PLANE_ATTR(7),
    PLANE_IS_ATTR_PLACE(8);

    final int constant;

    ScreenPlanes(int c) {
        constant = c;
    }
    
    String title(){
        return toString().split("_",2)[1];
    }
}

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
    JComboBox<ScreenPlanes> planeCmb = new JComboBox<>();
    private final TN5250jLogger log = TN5250jLogFactory.getLogger(this.getClass());
    
    Session5250 session ;

    public void setSession(Session5250 aSession) {
        session = aSession;
        updatePanel();
        session.getScreen().getOIA().addOIAListener((ScreenOIA oia, int change) -> {
            log.info("ScreenOIA change: " + change);
        });
    }

    public void showWindow() {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("Testory Remote");
            window.getContentPane().add(createPanel());
            window.pack();
            window.setVisible(true);
        });
    }

    public JComponent createPanel() {
        fieldBtnCtnr.setLayout(new BoxLayout(fieldBtnCtnr, BoxLayout.PAGE_AXIS));

        screenTxt.setFont(new Font("Courier", Font.PLAIN, 12));
        screenTxt.setEditable(false);
        screenTxt.setBackground(Color.DARK_GRAY);
        screenTxt.setForeground(Color.GREEN);
        screenTxt.setMinimumSize(new Dimension(200, 400));

        refreshBtn.addActionListener(e -> updatePanel());
        ActionListener sendKeys = e -> {
            session.getScreen().sendKeys(keysTxt.getText());
        };

        sendBtn.addActionListener(sendKeys);
        keysTxt.addActionListener(sendKeys);
        
        planeCmb.setEditable(false);
        DefaultComboBoxModel<ScreenPlanes> model = new DefaultComboBoxModel<>();
        model.addAll( Arrays.asList(ScreenPlanes.values()) );
        planeCmb.setModel(model);
        planeCmb.setSelectedItem(ScreenPlanes.PLANE_TEXT);
        
        return PanelMatic.begin()
            .addHeader(HeaderLevel.H1, "Testory AS400 Remote")
            .add(refreshBtn)
            .add("Fields", fieldBtnCtnr)
            .addHeader(HeaderLevel.H2, "Send Text")
            .add(keysTxt)
            .add(sendBtn, Modifiers.L_END, Modifiers.NO_STRETCH)
            .addHeader(HeaderLevel.H2, "Screen")
            .add("Plane", planeCmb)
            .add(screenTxt, Modifiers.GROW)
            .get((PanelPostProcessor) (JComponent jc) -> {
                Border padding = new EmptyBorder(8, 8, 8, 8);
                jc.setBorder(padding);
                return jc;
            });
    }

    private void updatePanel() {
        Screen5250 screen = session.getScreen();

        fieldBtnCtnr.removeAll();
        if (screen != null) {
//            screen.dumpScreen();

            ScreenFields screenFields = screen.getScreenFields();
            if (screenFields != null) {
                Arrays.asList(screenFields.getFields()).forEach(f -> {
                    fieldBtnCtnr.add(new JButton("field #" + f.getFieldId() + ":" + f.getString()));
                });
                fieldBtnCtnr.invalidate();
            }
            screenTxt.setText(drawScreen(screen));
        }
    }

    private String drawScreen(Screen5250 scrn) {
        ScreenPlanes plane = (ScreenPlanes) planeCmb.getSelectedItem();
        if ( plane == null ) {
            plane = ScreenPlanes.PLANE_TEXT;
        }
        char[] content;
        int screenWidth = scrn.getColumns();
        StringBuilder sb = new StringBuilder();
        
        if ( plane == ScreenPlanes.PLANE_TEXT ) {
            content = scrn.getScreenAsChars();        

            for (int pos = 0; pos < content.length; pos += screenWidth) {
                sb.append(new String(content, pos, screenWidth));
                sb.append("\n");
            }

        } else {
            content = new char[scrn.getScreenLength()];
            scrn.GetScreen(content, content.length, plane.constant);
            int rowCount = scrn.getRows();
            
            for (int row = 0; row < rowCount; row++) {
                for ( int col = 0; col<screenWidth; col++ ) {
                    char c = content[row*screenWidth+col];
                    String hex = ((c<0xF) ? "0" : "") +  Integer.toHexString(c) + " ";
                    sb.append(hex);
                }
                sb.append("\n");
            }
            
        }
        
        return sb.toString();
    }
}
