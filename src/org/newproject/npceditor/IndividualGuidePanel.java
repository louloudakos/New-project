package org.newproject.npceditor;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class IndividualGuidePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    public IndividualGuidePanel() {
        setLayout(new BorderLayout());
        setBackground(UIStyle.BG_FIELD);
        
        setBorder(new TitledBorder(
            BorderFactory.createLineBorder(new Color(0, 150, 200)), 
            "NPC Editor Guide & Logic", 
            TitledBorder.DEFAULT_JUSTIFICATION, 
            TitledBorder.DEFAULT_POSITION, 
            null, 
            Color.CYAN
        ));

        JTextPane wikiText = new JTextPane();
        wikiText.setEditable(false);
        wikiText.setContentType("text/html");
        wikiText.setBackground(UIStyle.BG_FIELD);
        
        // HTML Content with Rules and Calculations
        String content = "<html>"
            + "<body style='color: white; font-family: sans-serif; padding: 10px; font-size: 11px;'>"
            
            // --- SECTION 1: DROPS & SPOILS ---
            + "<h2 style='color: #FFD700;'>1. Drop & Spoil Logic (Crucial)</h2>"
            + "<p>L2J Drop logic works in a hierarchy (Tree Structure). Understanding this prevents bugs.</p>"
            
            + "<b style='color: #00E5FF;'>A. The Category (Group) Chance:</b>"
            + "<ul>"
            + "  <li>This is the probability that the <b>Folder opens</b>.</li>"
            + "  <li>If Category Chance is <b>70%</b>, there is a 30% chance the server looks no further and drops nothing from this group.</li>"
            + "</ul>"

            + "<b style='color: #00E5FF;'>B. The Item Chance:</b>"
            + "<ul>"
            + "  <li>This is the probability the item drops <b>PROVIDED the category opened</b>.</li>"
            + "</ul>"

            + "<div style='background-color: #333333; padding: 5px; border: 1px solid #555;'>"
            + "<b style='color: #FF5252;'>REAL CHANCE CALCULATION:</b><br>"
            + "<i>(Category Chance %) x (Item Chance %) = Real Drop Rate</i><br><br>"
            + "<b>Example:</b><br>"
            + "Category = 50% | Item = 20%<br>"
            + "0.50 * 0.20 = <b>0.10 (10% Real Chance)</b>"
            + "</div>"
            
            + "<p><b>Note:</b> If Category Chance is 100%, then the Item Chance is the Real Chance.</p>"

            // --- SECTION 2: STATS ---
            + "<hr>"
            + "<h2 style='color: #FFD700;'>2. NPC Stats Explained</h2>"
            + "<ul>"
            + "  <li><b>STR:</b> Affects P.Atk and Critical Damage.</li>"
            + "  <li><b>DEX:</b> Affects P.Spd, Accuracy, Evasion, Critical Rate.</li>"
            + "  <li><b>CON:</b> Affects Max HP and HP Regen.</li>"
            + "  <li><b>INT:</b> Affects M.Atk.</li>"
            + "  <li><b>WIT:</b> Affects Casting Speed and M.Crit Rate.</li>"
            + "  <li><b>MEN:</b> Affects M.Def and MP Regen.</li>"
            + "</ul>"
            + "<p style='color: #aaa;'><i>Changing Base Attributes (STR/INT...) automatically scales the Combat Stats (P.Atk/M.Atk) in the game engine.</i></p>"

            // --- SECTION 3: SKILLS ---
            + "<hr>"
            + "<h2 style='color: #FFD700;'>3. Skills Logic</h2>"
            + "<ul>"
            + "  <li><b>Passive Skills:</b> Define racial stats (e.g., Undead, Construct) and resistances (Bow Resist).</li>"
            + "  <li><b>Active Skills:</b> Magic or Physical attacks the NPC uses.</li>"
            + "  <li><b>Heal/Buff:</b> If you add these, ensure the NPC AI type supports using them (e.g., Cleric/Buffer AI).</li>"
            + "</ul>"
            + "<b style='color: #FF5252;'>Warning:</b> Adding a skill ID that does not exist in the server datapack will cause errors in the console."

            + "</body>"
            + "</html>";

        wikiText.setText(content);
        wikiText.setCaretPosition(0); // Scroll to top

        JScrollPane scroll = new JScrollPane(wikiText);
        UIStyle.styleScrollPane(scroll);
        
        add(scroll, BorderLayout.CENTER);
        setPreferredSize(new Dimension(450, 600));
    }
}