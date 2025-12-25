package org.newproject.npceditor;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.basic.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;

public class UIStyle {
    
    // --- DARK THEME PALETTE ---
    public static final Color BG_MAIN = new Color(30, 30, 30);
    public static final Color BG_PANEL = new Color(45, 45, 45);
    public static final Color BG_FIELD = new Color(60, 60, 60);
    public static final Color TEXT_COLOR = new Color(255, 255, 255);
    public static final Color BORDER_COLOR = new Color(100, 100, 100);
    public static final Color ACCENT_COLOR = new Color(0, 140, 240);
    public static final Color FOLDER_COLOR = new Color(80, 200, 120); // Πράσινο για φακέλους
    public static final Color HEADER_BG = new Color(50, 50, 50);
    public static final Color HEADER_FG = new Color(0, 180, 255);

    public static void applyGlobalTheme() {
        try {
            // Force Metal Look (Αγνοεί τα Windows Themes για απόλυτο έλεγχο)
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            
            // --- GLOBAL DEFAULTS ---
            ColorUIResource bgMain = new ColorUIResource(BG_MAIN);
            ColorUIResource bgPanel = new ColorUIResource(BG_PANEL);
            ColorUIResource bgField = new ColorUIResource(BG_FIELD);
            ColorUIResource textCol = new ColorUIResource(TEXT_COLOR);

            // OPTION PANE (POPUPS)
            UIManager.put("OptionPane.background", bgMain);
            UIManager.put("OptionPane.messageForeground", textCol);
            UIManager.put("Panel.background", bgMain);
            UIManager.put("Dialog.background", bgMain);
            
            // BUTTONS (Global)
            UIManager.put("Button.background", bgField);
            UIManager.put("Button.foreground", textCol);
            UIManager.put("Button.select", bgPanel);
            UIManager.put("Button.focus", new ColorUIResource(ACCENT_COLOR));
            UIManager.put("Button.border", BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR),
                    BorderFactory.createEmptyBorder(5, 15, 5, 15)));

            // TEXT FIELDS
            UIManager.put("TextField.background", bgField);
            UIManager.put("TextField.foreground", textCol);
            UIManager.put("TextField.caretForeground", new ColorUIResource(Color.WHITE));
            UIManager.put("TextField.border", BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR),
                    BorderFactory.createEmptyBorder(4, 6, 4, 6)));

            // LABELS
            UIManager.put("Label.foreground", textCol);
            UIManager.put("Label.background", bgMain);

            // MENUS
            UIManager.put("MenuBar.background", bgMain);
            UIManager.put("Menu.background", bgMain);
            UIManager.put("Menu.foreground", textCol);
            UIManager.put("MenuItem.background", bgPanel);
            UIManager.put("MenuItem.foreground", textCol);
            UIManager.put("PopupMenu.background", bgPanel);
            UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(BORDER_COLOR));

            // TABS / TABLES / TREES
            UIManager.put("TabbedPane.background", bgMain);
            UIManager.put("TabbedPane.foreground", textCol);
            UIManager.put("TabbedPane.contentAreaColor", bgMain);
            UIManager.put("Table.background", bgField);
            UIManager.put("Table.foreground", textCol);
            UIManager.put("TableHeader.background", bgPanel);
            UIManager.put("TableHeader.foreground", textCol);
            UIManager.put("Tree.background", bgPanel);
            UIManager.put("Tree.textForeground", textCol);
            
            // SCROLLBARS
            UIManager.put("ScrollBar.background", bgMain);
            UIManager.put("ScrollBar.thumb", bgField);
            UIManager.put("ScrollBar.track", bgMain);

        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- HELPER METHODS ---

    public static void styleField(JTextField f) {
        f.setBackground(BG_FIELD);
        f.setForeground(TEXT_COLOR);
        f.setCaretColor(Color.WHITE);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
    }

    public static void styleButton(JButton btn, Color bg, Color fg) {
        btn.setUI(new BasicButtonUI()); // Ξηλώνει το στυλ των Windows
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 1),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)));
        
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(bg.brighter()); }
            @Override public void mouseExited(MouseEvent e) { btn.setBackground(bg); }
        });
    }

    public static void styleTable(JTable table) {
        table.setBackground(BG_FIELD);
        table.setForeground(TEXT_COLOR);
        table.setGridColor(BORDER_COLOR);
        table.setRowHeight(25);
        table.setSelectionBackground(ACCENT_COLOR);
        table.setSelectionForeground(Color.WHITE);
        
        JTableHeader header = table.getTableHeader();
        header.setBackground(BG_PANEL);
        header.setForeground(TEXT_COLOR);
        header.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBackground(BG_PANEL); 
                setForeground(TEXT_COLOR);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 1, BORDER_COLOR),
                        BorderFactory.createEmptyBorder(2, 5, 2, 5)));
                return this;
            }
        });
    }

    public static void styleScrollPane(JScrollPane scroll) {
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        scroll.getViewport().setBackground(BG_PANEL);
        scroll.setBackground(BG_PANEL);
    }

    // --- INNER CLASSES (RENDERERS & UI) ---

    // Renderer για το JTree (Φάκελοι και Items)
    public static class DarkTreeCellRenderer extends DefaultTreeCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override
        public Component getTreeCellRendererComponent(JTree t, Object v, boolean s, boolean e, boolean l, int r, boolean h) {
            super.getTreeCellRendererComponent(t, v, s, e, l, r, h);
            setOpaque(true);
            setBackgroundNonSelectionColor(BG_PANEL);
            setBackgroundSelectionColor(new Color(70, 70, 70));
            
            if (v instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) v;
                // Έλεγχος αν είναι NPC ή Κατηγορία
                if (node.getUserObject() instanceof Npc) {
                    setText(node.getUserObject().toString());
                    setForeground(Color.WHITE);
                    setIcon(null); // Χωρίς εικονίδιο φακέλου για τα NPCs
                } else {
                    setForeground(FOLDER_COLOR); // Πράσινο για τις κατηγορίες (L2Monster, L2RaidBoss)
                    setFont(getFont().deriveFont(Font.BOLD));
                }
            }
            return this;
        }
    }
    
    // UI για τα Tabs (Box Style)
    public static class BoxTabbedPaneUI extends BasicTabbedPaneUI { 
        @Override protected void installDefaults() { super.installDefaults(); tabInsets = new Insets(5, 15, 5, 15); } 
        @Override protected void paintTabBackground(Graphics g, int p, int i, int x, int y, int w, int h, boolean s) { 
            g.setColor(s ? BG_FIELD : BG_MAIN); 
            g.fillRect(x+2, y+2, w-4, h-2); 
        } 
        @Override protected void paintTabBorder(Graphics g, int p, int i, int x, int y, int w, int h, boolean s) { 
            ((Graphics2D)g).setStroke(new BasicStroke(1)); 
            ((Graphics2D)g).setColor(s ? ACCENT_COLOR : BORDER_COLOR); 
            g.drawRect(x+2, y+2, w-5, h-3); 
        } 
    }
}