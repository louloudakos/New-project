package org.newproject.npceditor;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * WikiPanel class provides a dedicated documentation area within the Mass Operations tab.
 * It uses HTML rendering to display formatted rules and instructions.
 */
public class WikiPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    public WikiPanel() {
        // Basic layout and background setup
        setLayout(new BorderLayout());
        setBackground(UIStyle.BG_FIELD);
        
        // Border with a distinct color to separate it from the form
        setBorder(new TitledBorder(
            BorderFactory.createLineBorder(new Color(0, 150, 200)), 
            "Mass Edit Wiki & Rules", 
            TitledBorder.DEFAULT_JUSTIFICATION, 
            TitledBorder.DEFAULT_POSITION, 
            null, 
            Color.CYAN
        ));

        // JTextPane is used for HTML content support
        JTextPane wikiText = new JTextPane();
        wikiText.setEditable(false);
        wikiText.setContentType("text/html");
        wikiText.setBackground(UIStyle.BG_FIELD);
        
        // The English content of the Wiki
        String content = "<html>"
            + "<body style='color: white; font-family: sans-serif; padding: 10px;'>"
            + "<h2 style='color: #00E5FF;'>Mass Operations Guide</h2>"
            + "<p>This tool allows you to apply changes to multiple NPCs simultaneously. Use it with caution as changes affect a large number of files.</p>"
            + ""
            + "<b style='color: #FFD700;'>1. Target Selection:</b>"
            + "<ul>"
            + "  <li><b>All Monsters:</b> Targets every NPC classified as Monster, RaidBoss, or GrandBoss.</li>"
            + "  <li><b>Custom ID List:</b> Paste specific IDs separated by commas (e.g., 20001, 20005, 29019).</li>"
            + "</ul>"
            + ""
            + "<b style='color: #FFD700;'>2. Item Addition Logic:</b>"
            + "<ul>"
            + "  <li>The item is added into a <b>NEW Drop Category</b> for each target NPC.</li>"
            + "  <li>It will not merge with existing categories to prevent logic conflicts.</li>"
            + "</ul>"
            + ""
            + "<b style='color: #FF8A65;'>Important Rule - Category Chance:</b><br>"
            + "The chance you set in the form is for the <b>Category itself</b>. Inside that category, "
            + "the item is set to 100% relative probability. If your category total chance exceeds 100%, "
            + "the retail drop engine might skip subsequent categories!<br><br>"
            + ""
            + "<b style='color: #FF5252;'>Warning:</b><br>"
            + "Applying a mass operation marks all affected NPCs as 'Modified'. "
            + "You must click the <b>'Save All Modified'</b> button in the top bar to write these changes to the XML files."
            + "</body>"
            + "</html>";

        wikiText.setText(content);

        // Scroll pane to ensure visibility if text grows
        JScrollPane scroll = new JScrollPane(wikiText);
        UIStyle.styleScrollPane(scroll);
        
        add(scroll, BorderLayout.CENTER);
        
        // Fixed width for the side panel
        setPreferredSize(new Dimension(420, 0));
    }
}