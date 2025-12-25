package org.newproject.npceditor;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;    // Απαραίτητο Import
import javax.swing.event.DocumentListener; // Απαραίτητο Import
import java.awt.*;
import java.awt.event.FocusAdapter;        // Απαραίτητο για το Placeholder
import java.awt.event.FocusEvent;          // Απαραίτητο για το Placeholder
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MassOperationPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    // Components
    private JComboBox<String> targetSelector;
    private JTextArea idListArea;
    private JTextField txtMinLv, txtMaxLv;
    private JLabel lblTargetInfo;
    
    // Dynamic Categories Container
    private JPanel categoriesContainer;
    private List<MassCategoryPanel> categoryPanels = new ArrayList<>();
    
    private JCheckBox chkMonsters, chkRaids, chkGrandBoss, chkMinions;
    
    private List<Npc> allNpcs;
    private Set<Npc> dirtyNpcs;
    
    // Placeholder Constant
    private static final String ID_PLACEHOLDER = "Example: 20001, 20005, 25001";

    public MassOperationPanel(List<Npc> allNpcs, Set<Npc> dirtyNpcs) {
        this.allNpcs = allNpcs;
        this.dirtyNpcs = dirtyNpcs;
        
        setLayout(new BorderLayout(15, 15));
        setBackground(UIStyle.BG_PANEL);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- MAIN FORM SCROLLABLE CONTAINER ---
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setOpaque(false);

        // 1. TARGET STRATEGY
        JPanel pnlStrategy = createStyledPanel("1. Select Target Strategy");
        targetSelector = new JComboBox<>(new String[]{
            "Global: All Attackable NPCs", 
            "Range: By Level Only", 
            "Specific: Custom ID List",
            "Advanced: By Type & Level"
        });
        targetSelector.addActionListener(_ -> updateUiState());
        
        JPanel comboWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        comboWrap.setOpaque(false); comboWrap.add(targetSelector);
        
        lblTargetInfo = new JLabel("Status: Targeting all database monsters.");
        lblTargetInfo.setForeground(Color.LIGHT_GRAY);
        lblTargetInfo.setFont(new Font("SansSerif", Font.ITALIC, 11));

        pnlStrategy.add(comboWrap);
        pnlStrategy.add(Box.createVerticalStrut(5));
        pnlStrategy.add(lblTargetInfo);
        mainContent.add(pnlStrategy);
        mainContent.add(Box.createVerticalStrut(10));

        // 2. TARGET PARAMETERS
        JPanel pnlParams = createStyledPanel("2. Target Parameters");
        pnlParams.setLayout(new BoxLayout(pnlParams, BoxLayout.Y_AXIS));

        // Level Range
        JPanel lvRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        lvRow.setOpaque(false);
        txtMinLv = new JTextField("1", 4); UIStyle.styleField(txtMinLv);
        txtMaxLv = new JTextField("85", 4); UIStyle.styleField(txtMaxLv);
        lvRow.add(new JLabel("Level Min: ")); lvRow.add(txtMinLv);
        lvRow.add(Box.createHorizontalStrut(15));
        lvRow.add(new JLabel("Level Max: ")); lvRow.add(txtMaxLv);
        
        // Advanced Types
        JPanel typeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        typeRow.setOpaque(false);
        chkMonsters = createDarkCheckBox("Monsters", true);
        chkRaids = createDarkCheckBox("Raids", false);
        chkGrandBoss = createDarkCheckBox("Grand Bosses", false);
        chkMinions = createDarkCheckBox("Minions", false);
        typeRow.add(chkMonsters); typeRow.add(Box.createHorizontalStrut(10));
        typeRow.add(chkRaids); typeRow.add(Box.createHorizontalStrut(10));
        typeRow.add(chkGrandBoss); typeRow.add(Box.createHorizontalStrut(10));
        typeRow.add(chkMinions);

        // ID List (Fixed Small Size & Placeholder Logic)
        JPanel idRow = new JPanel(new BorderLayout(5, 5));
        idRow.setOpaque(false);
        idRow.setBorder(new EmptyBorder(5, 0, 0, 0));
        idRow.add(new JLabel("Paste IDs (comma separated):"), BorderLayout.NORTH);
        
        idListArea = new JTextArea(3, 20);
        idListArea.setBackground(UIStyle.BG_FIELD);
        idListArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        // --- PLACEHOLDER LOGIC ---
        idListArea.setForeground(Color.GRAY);
        idListArea.setText(ID_PLACEHOLDER);
        
        idListArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (idListArea.getText().equals(ID_PLACEHOLDER)) {
                    idListArea.setText("");
                    idListArea.setForeground(Color.WHITE);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (idListArea.getText().isEmpty()) {
                    idListArea.setForeground(Color.GRAY);
                    idListArea.setText(ID_PLACEHOLDER);
                }
            }
        });
        
        JScrollPane scrollIds = new JScrollPane(idListArea);
        scrollIds.setPreferredSize(new Dimension(0, 50)); 
        scrollIds.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        
        idRow.add(scrollIds, BorderLayout.CENTER);

        pnlParams.add(lvRow);
        pnlParams.add(Box.createVerticalStrut(5));
        pnlParams.add(typeRow); 
        pnlParams.add(Box.createVerticalStrut(5));
        pnlParams.add(idRow);
        mainContent.add(pnlParams);
        mainContent.add(Box.createVerticalStrut(10));

        // 3. DROP STRUCTURE CONFIGURATION
        JPanel pnlStructure = createStyledPanel("3. Configure Drop Structure");
        pnlStructure.setLayout(new BorderLayout());
        
        // Header Button
        JPanel headerBtn = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerBtn.setOpaque(false);
        JButton btnAddCat = new JButton("+ Add New Drop Category");
        UIStyle.styleButton(btnAddCat, new Color(255, 200, 0), Color.BLACK);
        btnAddCat.addActionListener(_ -> addCategoryPanel());
        headerBtn.add(btnAddCat);
        headerBtn.add(new JLabel(" (Create categories and add items inside them)"));
        pnlStructure.add(headerBtn, BorderLayout.NORTH);

        // Container for Categories
        categoriesContainer = new JPanel();
        categoriesContainer.setLayout(new BoxLayout(categoriesContainer, BoxLayout.Y_AXIS));
        categoriesContainer.setOpaque(false);
        
        pnlStructure.add(categoriesContainer, BorderLayout.CENTER);
        mainContent.add(pnlStructure);
        mainContent.add(Box.createVerticalStrut(20));

        // EXECUTE BUTTON
        JButton apply = new JButton("PREVIEW & EXECUTE");
        UIStyle.styleButton(apply, new Color(160, 0, 0), Color.WHITE);
        apply.setFont(new Font("Arial", Font.BOLD, 14));
        apply.setAlignmentX(Component.LEFT_ALIGNMENT);
        apply.addActionListener(_ -> executeSafe());
        
        mainContent.add(apply);
        mainContent.add(Box.createVerticalGlue()); 

        // TOP BAR WITH HELP
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.add(new JLabel(" "), BorderLayout.CENTER); 
        JButton btnHelp = new JButton("[ ? ] Help & Logic");
        UIStyle.styleButton(btnHelp, new Color(0, 100, 150), Color.WHITE);
        btnHelp.addActionListener(_ -> new InfoDialog((JFrame) SwingUtilities.getWindowAncestor(this)).setVisible(true));
        topBar.add(btnHelp, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // SCROLL FOR MAIN CONTENT
        JScrollPane mainScroll = new JScrollPane(mainContent);
        UIStyle.styleScrollPane(mainScroll);
        mainScroll.setBorder(null);

        add(mainScroll, BorderLayout.CENTER);
        
        // Init with one category
        addCategoryPanel();
        updateUiState();
    }

    private void addCategoryPanel() {
        MassCategoryPanel cat = new MassCategoryPanel(this);
        categoryPanels.add(cat);
        categoriesContainer.add(cat);
        categoriesContainer.add(Box.createVerticalStrut(10)); 
        categoriesContainer.revalidate();
        categoriesContainer.repaint();
    }

    public void removeCategoryPanel(MassCategoryPanel cat) {
        categoryPanels.remove(cat);
        categoriesContainer.remove(cat);
        categoriesContainer.revalidate();
        categoriesContainer.repaint();
    }

    private JCheckBox createDarkCheckBox(String text, boolean selected) {
        JCheckBox cb = new JCheckBox(text, selected);
        cb.setOpaque(false); cb.setForeground(Color.WHITE); return cb;
    }

    private void updateUiState() {
        int idx = targetSelector.getSelectedIndex();
        boolean showLevel = (idx == 1 || idx == 3);
        boolean showTypes = (idx == 3);
        boolean showIds = (idx == 2);

        txtMinLv.setEnabled(showLevel); txtMaxLv.setEnabled(showLevel);
        txtMinLv.setBackground(showLevel ? UIStyle.BG_FIELD : UIStyle.BG_PANEL);
        txtMaxLv.setBackground(showLevel ? UIStyle.BG_FIELD : UIStyle.BG_PANEL);

        chkMonsters.setEnabled(showTypes); chkRaids.setEnabled(showTypes);
        chkGrandBoss.setEnabled(showTypes); chkMinions.setEnabled(showTypes);

        idListArea.setEnabled(showIds);
        // Αν είναι disabled, το κάνουμε σκούρο, αλλιώς ανοιχτό
        idListArea.setBackground(showIds ? UIStyle.BG_FIELD : UIStyle.BG_PANEL); 
        // Αν ενεργοποιηθεί και έχει το placeholder, να μείνει γκρι, αλλιώς άσπρο
        if (idListArea.getText().equals(ID_PLACEHOLDER)) idListArea.setForeground(Color.GRAY);
        else idListArea.setForeground(showIds ? Color.WHITE : Color.GRAY);
        
        if (idx == 0) lblTargetInfo.setText("Targeting: ALL Attackable NPCs.");
        else if (idx == 1) lblTargetInfo.setText("Targeting: Mobs within level range.");
        else if (idx == 2) lblTargetInfo.setText("Targeting: Only specific IDs.");
        else lblTargetInfo.setText("Targeting: Specific Types within Level Range.");
    }

    private void executeSafe() {
        try {
            if (categoryPanels.isEmpty()) { JOptionPane.showMessageDialog(this, "Please add at least one drop category."); return; }
            
            List<Npc> targets = getTargets();
            if (targets == null || targets.isEmpty()) { JOptionPane.showMessageDialog(this, "No NPCs found matching selection."); return; }

            int totalItems = 0;
            for(MassCategoryPanel p : categoryPanels) totalItems += p.getItemCount();
            
            String msg = "Mass Operation Summary:\n"
                       + "Targets Found: " + targets.size() + "\n"
                       + "New Categories to Add: " + categoryPanels.size() + "\n"
                       + "Total Items to Add: " + totalItems + "\n\n"
                       + "Proceed?";
                       
            if (JOptionPane.showConfirmDialog(this, msg, "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                applyChanges(targets);
            }

        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()); }
    }

    private List<Npc> getTargets() {
        int mode = targetSelector.getSelectedIndex();
        if (mode == 0) { // Global
            return allNpcs.stream().filter(this::isAnyAttackable).collect(Collectors.toList());
        } else if (mode == 1) { // Level
            int minL = parseIntSafe(txtMinLv.getText());
            int maxL = parseIntSafe(txtMaxLv.getText());
            return allNpcs.stream().filter(n -> isAnyAttackable(n) && n.level >= minL && n.level <= maxL).collect(Collectors.toList());
        } else if (mode == 2) { // ID
            String raw = idListArea.getText().trim();
            // Check if empty OR contains only placeholder
            if(raw.isEmpty() || raw.equals(ID_PLACEHOLDER)) return new ArrayList<>();
            
            try {
                Set<Integer> idSet = Arrays.stream(raw.split(","))
                    .map(s -> s.trim())
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .collect(Collectors.toSet());
                return allNpcs.stream().filter(n -> idSet.contains(n.id)).collect(Collectors.toList());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid ID format. Please use comma-separated numbers.");
                return new ArrayList<>();
            }
        } else { // Advanced
            int minL = parseIntSafe(txtMinLv.getText());
            int maxL = parseIntSafe(txtMaxLv.getText());
            return allNpcs.stream().filter(n -> {
                if (n.level < minL || n.level > maxL) return false;
                String t = (n.type == null) ? "" : n.type.toLowerCase();
                boolean isMon = (t.contains("monster") || t.contains("boss")) && !t.contains("raid") && !t.contains("grand") && !t.contains("minion");
                boolean isRaid = t.contains("raidboss");
                boolean isGrand = t.contains("grandboss");
                boolean isMinion = t.contains("minion");
                if (chkMonsters.isSelected() && isMon) return true;
                if (chkRaids.isSelected() && isRaid) return true;
                if (chkGrandBoss.isSelected() && isGrand) return true;
                if (chkMinions.isSelected() && isMinion) return true;
                return false;
            }).collect(Collectors.toList());
        }
    }

    private void applyChanges(List<Npc> targets) {
        try {
            for (Npc n : targets) {
                for (MassCategoryPanel catPanel : categoryPanels) {
                    int nId = 1; for(DropGroup g : n.dropGroups) nId = Math.max(nId, g.id + 1);
                    
                    double grpChance = Double.parseDouble(catPanel.txtGroupChance.getText().trim());
                    DropGroup ng = new DropGroup(nId, false);
                    ng.chance = grpChance;
                    
                    List<MassItemRow> rows = catPanel.getItemRows();
                    for (MassItemRow row : rows) {
                        ng.addItem(row.getItemData());
                    }
                    
                    if (!ng.items.isEmpty()) {
                        n.dropGroups.add(ng);
                        dirtyNpcs.add(n);
                    }
                }
            }
            JOptionPane.showMessageDialog(this, "Success! Updated " + targets.size() + " NPCs.");
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Apply Error: " + ex.getMessage()); }
    }

    private boolean isAnyAttackable(Npc n) {
        if (n.type == null) return false;
        String t = n.type.toLowerCase();
        return t.contains("monster") || t.contains("boss") || t.contains("raid") || t.contains("minion");
    }
    
    private int parseIntSafe(String s) { try { return Integer.parseInt(s.trim()); } catch(Exception e) { return 0; } }
    
    private JPanel createStyledPanel(String title) {
        JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(UIStyle.BG_PANEL);
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(80, 80, 80)), title, TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 12), Color.CYAN));
        return p;
    }

    // --- INNER CLASS: CATEGORY PANEL ---
    private class MassCategoryPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        JTextField txtGroupChance;
        JPanel itemContainer;
        List<MassItemRow> itemRows = new ArrayList<>();
        
        public MassCategoryPanel(MassOperationPanel parent) {
            setLayout(new BorderLayout());
            setBackground(UIStyle.BG_FIELD);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, Color.ORANGE),
                new EmptyBorder(5, 5, 5, 5)
            ));
            
            JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
            header.setOpaque(false);
            header.add(new JLabel("Group Chance %:"));
            txtGroupChance = new JTextField("100.0", 6); UIStyle.styleField(txtGroupChance);
            header.add(txtGroupChance);
            
            JButton btnAddItem = new JButton("+ Add Item");
            UIStyle.styleButton(btnAddItem, new Color(50, 100, 150), Color.WHITE);
            btnAddItem.addActionListener(_ -> addItemRow());
            header.add(Box.createHorizontalStrut(10));
            header.add(btnAddItem);
            
            JButton btnRemove = new JButton("Remove Category");
            UIStyle.styleButton(btnRemove, new Color(150, 50, 50), Color.WHITE);
            btnRemove.addActionListener(_ -> parent.removeCategoryPanel(this));
            header.add(Box.createHorizontalStrut(20));
            header.add(btnRemove);
            
            add(header, BorderLayout.NORTH);
            
            itemContainer = new JPanel();
            itemContainer.setLayout(new BoxLayout(itemContainer, BoxLayout.Y_AXIS));
            itemContainer.setOpaque(false);
            add(itemContainer, BorderLayout.CENTER);
            
            addItemRow();
        }
        
        private void addItemRow() {
            MassItemRow row = new MassItemRow(this);
            itemRows.add(row);
            itemContainer.add(row);
            itemContainer.revalidate(); itemContainer.repaint();
        }
        
        public void removeItemRow(MassItemRow row) {
            itemRows.remove(row);
            itemContainer.remove(row);
            itemContainer.revalidate(); itemContainer.repaint();
        }
        
        public int getItemCount() { return itemRows.size(); }
        public List<MassItemRow> getItemRows() { return itemRows; }
    }

    // --- INNER CLASS: ITEM ROW ---
    private class MassItemRow extends JPanel {
        private static final long serialVersionUID = 1L;
        JTextField txtId, txtMin, txtMax, txtChance;
        JLabel lblName;

        public MassItemRow(MassCategoryPanel parent) {
            setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));
            setBackground(UIStyle.BG_FIELD);
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
            
            // ID Panel
            JPanel idPanel = new JPanel(new BorderLayout());
            idPanel.setOpaque(false);
            txtId = new JTextField(6); UIStyle.styleField(txtId);
            lblName = new JLabel("..."); lblName.setForeground(Color.YELLOW); 
            lblName.setFont(new Font("SansSerif", Font.ITALIC, 10));
            
            idPanel.add(new JLabel("ID:"), BorderLayout.WEST);
            idPanel.add(txtId, BorderLayout.CENTER);
            idPanel.add(lblName, BorderLayout.SOUTH);
            
            txtId.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { update(); }
                public void removeUpdate(DocumentEvent e) { update(); }
                public void changedUpdate(DocumentEvent e) { update(); }
                void update() { try { int id = Integer.parseInt(txtId.getText()); lblName.setText(GameData.getItemName(id)); } catch(Exception _) { lblName.setText("..."); } }
            });

            txtMin = new JTextField("1", 4); UIStyle.styleField(txtMin);
            txtMax = new JTextField("1", 4); UIStyle.styleField(txtMax);
            txtChance = new JTextField("100.0", 5); UIStyle.styleField(txtChance);

            JButton delBtn = new JButton("X");
            UIStyle.styleButton(delBtn, new Color(150, 50, 50), Color.WHITE);
            delBtn.setPreferredSize(new Dimension(40, 20));
            delBtn.addActionListener(_ -> parent.removeItemRow(this));

            add(idPanel);
            add(new JLabel(" Min:")); add(txtMin);
            add(new JLabel(" Max:")); add(txtMax);
            add(new JLabel(" Item Chance:")); add(txtChance);
            add(delBtn);
        }

        public Npc.Item getItemData() throws Exception {
            int id = Integer.parseInt(txtId.getText().trim());
            int min = Integer.parseInt(txtMin.getText().trim());
            int max = Integer.parseInt(txtMax.getText().trim());
            double ch = Double.parseDouble(txtChance.getText().trim());
            return new Npc.Item(id, min, max, ch, "");
        }
    }
}