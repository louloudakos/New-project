package org.newproject.npceditor;

import javax.swing.*;
import java.awt.*;

public class InfoDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    public InfoDialog(JFrame parent) {
        super(parent, "Editor Guide & Drop Mechanics", true); // Modal window
        setSize(700, 600); // Increased size for better reading
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UIStyle.BG_MAIN);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setUI(new UIStyle.BoxTabbedPaneUI());
        
        // Add the detailed tabs
        tabs.addTab("How to Use (Step-by-Step)", createScrollPane(getMassGuideContent()));
        tabs.addTab("Drop Theory & Logic", createScrollPane(getDropLogicContent()));

        add(tabs, BorderLayout.CENTER);
        
        // Close Button Area
        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(UIStyle.BG_PANEL);
        JButton close = new JButton("Close Guide");
        UIStyle.styleButton(close, new Color(70, 70, 70), Color.WHITE);
        close.addActionListener(_ -> dispose());
        btnPanel.add(close);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private JScrollPane createScrollPane(String content) {
        JTextPane textPane = new JTextPane();
        textPane.setContentType("text/html");
        textPane.setEditable(false);
        textPane.setBackground(UIStyle.BG_FIELD);
        textPane.setText(content);
        textPane.setCaretPosition(0); // Scroll to top
        
        JScrollPane scroll = new JScrollPane(textPane);
        UIStyle.styleScrollPane(scroll); // Apply dark theme to scrollbar
        return scroll;
    }

    private String getMassGuideContent() {
        return "<html><body style='color:#E0E0E0; font-family:Segoe UI, sans-serif; font-size:11px; padding:10px;'>"
             
             + "<h2 style='color:#00E5FF; border-bottom:1px solid #00E5FF;'>1. Target Strategy Selection</h2>"
             + "<p>The editor offers 4 ways to find which NPCs to update:</p>"
             + "<ul>"
             + "  <li><b style='color:#FFD700;'>Global:</b> Targets EVERYTHING (Monsters, RaidBosses, GrandBosses, Minions). Use with extreme caution.</li>"
             + "  <li><b style='color:#FFD700;'>Range (By Level):</b> Selects all mobs between Min and Max levels. Good for tier-based updates (e.g., 'All mobs 76-80').</li>"
             + "  <li><b style='color:#FFD700;'>Specific (ID List):</b> The safest method. Paste explicit NPC IDs separated by commas (e.g., <i>20001, 29014, 25001</i>).</li>"
             + "  <li><b style='color:#FFD700;'>Advanced (Type & Level):</b> The most powerful filter. Allows you to target specific Levels AND specific Types (e.g., 'Only RaidBosses, No Minions').</li>"
             + "</ul>"

             + "<h2 style='color:#00E5FF; border-bottom:1px solid #00E5FF;'>2. Configuring Drop Structure</h2>"
             + "<p>This panel allows you to design the structure that will be injected into the NPCs.</p>"
             + "<ul>"
             + "  <li><b>Add New Category:</b> Creates a new Drop Group container. You can set the chance for the whole group here.</li>"
             + "  <li><b>Add Item:</b> Inserts an item into that specific category.</li>"
             + "  <li><b>Multiple Categories:</b> You can add multiple groups at once. For example, one group for 'Currency' (100% chance) and another for 'Rare Gear' (20% chance).</li>"
             + "</ul>"

             + "<h2 style='color:#FF5252; border-bottom:1px solid #FF5252;'>3. Execution & Saving</h2>"
             + "<p><b>Preview & Execute:</b> Clicking this does NOT write to disk immediately. It updates the editor's memory and marks NPCs as 'Dirty' (Modified).</p>"
             + "<p><b><u>IMPORTANT:</u></b> To make changes permanent, you MUST click the <b>'Save All Modified'</b> button in the top-right corner of the main window.</p>"
             + "</body></html>";
    }

    private String getDropLogicContent() {
        return "<html><body style='color:#E0E0E0; font-family:Segoe UI, sans-serif; font-size:11px; padding:10px;'>"
             
             + "<h1 style='color:#FFA500;'>Lineage 2 Drop Engine Mechanics</h1>"
             + "<p>Understanding how the server calculates drops is vital to prevent bugs.</p>"

             + "<h2 style='color:#90EE90;'>The Two-Step Roll System</h2>"
             + "<p>When a monster dies, the server performs two distinct calculations:</p>"
             + "<ol>"
             + "  <li><b>Step 1: Category Roll (The 'Bucket')</b><br>"
             + "      The server rolls a dice (0-100%) against the <i>Category Chance</i>.<br>"
             + "      <i>Example:</i> If Category Chance is 70%, there is a 30% chance this group is skipped entirely.</li>"
             + "  <li><b>Step 2: Item Roll (The 'Content')</b><br>"
             + "      If Step 1 succeeds, the server looks INSIDE the category to pick an item.</li>"
             + "</ol>"

             + "<h2 style='color:#FF5252;'>CRITICAL RULE: The 100% Limit</h2>"
             + "<p>Inside a single category, the sum of all Item Chances implies a relative probability logic (depending on L2J version, but usually cumulative).</p>"
             + "<div style='background-color:#333333; padding:5px; border-left:4px solid #FF5252;'>"
             + "  <b>BAD CONFIGURATION (Example):</b><br>"
             + "  - Item A: 60%<br>"
             + "  - Item B: 50%<br>"
             + "  <b>Total: 110%</b> -> <span style='color:#FF5252;'>BROKEN.</span> Item B might never drop because Item A consumes the roll range."
             + "</div>"
             + "<br>"
             + "<div style='background-color:#333333; padding:5px; border-left:4px solid #90EE90;'>"
             + "  <b>GOOD CONFIGURATION:</b><br>"
             + "  - Item A: 50%<br>"
             + "  - Item B: 50%<br>"
             + "  <b>Total: 100%</b> -> Perfect. The server rolls. If < 50, gives A. If > 50, gives B."
             + "</div>"

             + "<h2>Monsters vs. Minions</h2>"
             + "<p><b>Minions</b> are the guards of a Raid Boss or Group Mob.</p>"
             + "<ul>"
             + "  <li><b>Risk:</b> If you add valuable drops to Minions, players can kill minions, wait for respawn, kill again, and farm infinitely without killing the Boss.</li>"
             + "  <li><b>Solution:</b> Use the 'Advanced' filter in this tool and <b>uncheck 'Minions'</b> when adding Event Items or valuable currency.</li>"
             + "</ul>"

             + "</body></html>";
    }
}