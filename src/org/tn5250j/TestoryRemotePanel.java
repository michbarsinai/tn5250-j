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
import io.codeworth.panelmatic.util.Groupings;
import io.codeworth.panelmatic.util.PanelPostProcessors;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.PrintStream;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import org.tn5250j.framework.tn5250.Screen5250;
import org.tn5250j.framework.tn5250.ScreenField;
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
    JLabel lblScreenCoords = new JLabel();
    JTextField txtClickAt = new JTextField();
    JButton btnClickAt = new JButton("Click at:");
    private final TN5250jLogger log = TN5250jLogFactory.getLogger(this.getClass());
    
    Session5250 session ;

    public void setSession(Session5250 aSession) {
        session = aSession;
        updatePanel();
        session.getScreen().getOIA().addOIAListener((ScreenOIA oia, int change) -> {
            log.info("ScreenOIA change: " + change);
            System.out.println("ScreenOIA change: " + change);
        });
        
        session.getGUI().addMouseMotionListener(new MouseMotionAdapter(){
            @Override
            public void mouseMoved(MouseEvent e) {
                int pos = session.getGUI().getPosFromView(e.getX(), e.getY());
                int row = session.getScreen().getRow(pos) + 1;
                int col = session.getScreen().getCol(pos) + 1;
                final String mouseCoord = "r" + row + " c" + col + " p" + pos;

                lblScreenCoords.setText(mouseCoord);
            }
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
        
        lblScreenCoords.setFont( new Font(Font.MONOSPACED, Font.PLAIN, 12) );
        
        btnClickAt.addActionListener(this::clickAt);
        txtClickAt.addActionListener(this::clickAt);
        
        return PanelMatic.begin()
            .addHeader(HeaderLevel.H1, "Testory TN5250 Remote")
            .add(refreshBtn)
            .add("Mouse pos", lblScreenCoords)
            .add("Clicker", Groupings.lineGroup(btnClickAt, txtClickAt, new JLabel("r,c")) )
            .addHeader(HeaderLevel.H6, "r, c are 1-based, pos is 0-based")
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
                return PanelPostProcessors.wrapInScrollPane(Boolean.FALSE, Boolean.TRUE).process(jc);
            });
    }

    private void clickAt(ActionEvent e){
        String coordStr = txtClickAt.getText();
        String coord[] = coordStr.split(",");
        switch ( coord.length ) {
            case 1:
                
        }
        if ( coord.length != 2 ) {
            System.out.println("Bad coords");
            return;
        }
        
        session.getScreen().setCursor(
            Integer.parseInt(coord[0].trim()),
            Integer.parseInt(coord[1].trim())
        );
    }
        
    private void updatePanel() {
        Screen5250 screen = session.getScreen();

        fieldBtnCtnr.removeAll();
        if (screen != null) {
            ScreenFields screenFields = screen.getScreenFields();
            if (screenFields != null) {
                Arrays.asList(screenFields.getFields()).forEach(f -> {
                    final JButton fldButton = new JButton("field #" + f.getFieldId() + ":" + f.getString());
                    fldButton.addActionListener(a -> dumpField(f));
                    if ( f.equals(screenFields.getCurrentField()) ) {
                        fldButton.setText( "√ " + fldButton.getText() );
                    }
                    fieldBtnCtnr.add(fldButton);
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
    
    private void dumpField( ScreenField f ) {
        session.getScreen().gotoField(f.getFieldId());
        final PrintStream out = System.out;
        out.println("Field Data");
        out.println("==========");
        out.println("id:\t" + f.getFieldId());
        out.println("startPos:\t" + f.startPos());
        out.println("endPos:\t" + f.endPos());
        out.println("startCol:\t" + f.startCol());
        out.println("startRow:\t" + f.startRow());
        out.println("adjustment:\t" + f.getAdjustment());
        out.println("attr:\t" + f.getAttr());
        out.println("CurrentPos:\t" + f.getCurrentPos());
        out.println("CursorCol:\t" + f.getCursorCol());
        out.println("CursorProgression:\t" + f.getCursorProgression());
        out.println("getCursorRow:\t" + f.getCursorRow());
        out.println("FCW1:\t" + f.getFCW1());
        out.println("FCW2:\t" + f.getFCW2());
        out.println("FFW1:\t" + f.getFFW1());
        out.println("FFW2:\t" + f.getFFW2());
        out.println("FieldLength:\t" + f.getFieldLength());
        out.println("FieldShift:\t" + f.getFieldShift());
        out.println("HighlightedAttr:\t" + f.getHighlightedAttr());
        out.println("Length:\t" + f.getLength());
        out.println("isAutoEnter:\t" + f.isAutoEnter());
        out.println("isBypassField:\t" + f.isBypassField());
        out.println("isContinued:\t" + f.isContinued());
        out.println("isContinuedFirst:\t" + f.isContinuedFirst());
        out.println("isContinuedMiddle:\t" + f.isContinuedMiddle());
        out.println("isContinuedLast:\t" + f.isContinuedLast());
        out.println("isDupEnabled:\t" + f.isDupEnabled());
        out.println("isFER:\t" + f.isFER());
        out.println("isHiglightedEntry:\t" + f.isHiglightedEntry());
        out.println("isMandatoryEnter:\t" + f.isMandatoryEnter());
        out.println("isNumeric:\t" + f.isNumeric());
        out.println("isRightToLeft:\t" + f.isRightToLeft());
        out.println("isSelectionField:\t" + f.isSelectionField());
        out.println("isSignedNumeric:\t" + f.isSignedNumeric());
        out.println("isToUpper:\t" + f.isToUpper());
        out.println("Content:");
        out.println(f.getString());
        out.println("==========");
    }
}
