package org.newproject.npceditor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemBrowserPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private JTree categoryTree;
    private JTable itemTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField searchField;
    private JLabel statusLabel;
    
    // Για τη λειτουργία Popup/Picker
    private JDialog parentDialog; 
    private Integer selectedId = null; 

    private record ItemData(int id, String name, String category, String type, String grade) {}
    private List<ItemData> allItems = new ArrayList<>();

    // Κανονικός Constructor
    public ItemBrowserPanel() {
        this(null);
    }

    // Constructor που δέχεται Dialog (για όταν δουλεύει ως Picker)
    public ItemBrowserPanel(JDialog dialog) {
        this.parentDialog = dialog;
        
        setLayout(new BorderLayout(10, 10));
        setBackground(UIStyle.BG_MAIN);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        loadDataFromGameMemory();

        // --- LEFT: TREE ---
        DefaultMutableTreeNode root = createTreeStructure();
        categoryTree = new JTree(root);
        categoryTree.setBackground(UIStyle.BG_FIELD);
        categoryTree.setCellRenderer(new UIStyle.DarkTreeCellRenderer());
        categoryTree.addTreeSelectionListener(_ -> filterTableByTree());

        JScrollPane treeScroll = new JScrollPane(categoryTree);
        UIStyle.styleScrollPane(treeScroll);
        treeScroll.setPreferredSize(new Dimension(220, 0));

        // --- RIGHT: TABLE ---
        String[] columns = {"ID", "Name", "Grade", "Type/Part"};
        tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        itemTable = new JTable(tableModel);
        itemTable.setBackground(UIStyle.BG_FIELD);
        itemTable.setForeground(Color.WHITE);
        itemTable.setFillsViewportHeight(true);
        itemTable.setShowGrid(false);
        itemTable.setRowHeight(22);
        
        // Setup στηλών
        itemTable.getColumnModel().getColumn(0).setPreferredWidth(60); 
        itemTable.getColumnModel().getColumn(1).setPreferredWidth(250); 
        itemTable.getColumnModel().getColumn(2).setPreferredWidth(60); 
        
        sorter = new TableRowSorter<>(tableModel);
        itemTable.setRowSorter(sorter);
        
        // --- EVENT: DOUBLE CLICK TO SELECT ---
        itemTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && parentDialog != null) {
                    confirmSelection();
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(itemTable);
        UIStyle.styleScrollPane(tableScroll);

        // --- TOP: SEARCH ---
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBackground(UIStyle.BG_MAIN);
        
        JPanel searchBox = new JPanel(new BorderLayout(5, 0));
        searchBox.setOpaque(false);
        JLabel searchLbl = new JLabel("Search Item:");
        searchLbl.setForeground(Color.WHITE);
        searchField = new JTextField();
        UIStyle.styleField(searchField);
        
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { search(); }
            public void removeUpdate(DocumentEvent e) { search(); }
            public void changedUpdate(DocumentEvent e) { search(); }
        });
        
        searchBox.add(searchLbl, BorderLayout.WEST);
        searchBox.add(searchField, BorderLayout.CENTER);
        
        statusLabel = new JLabel("Items loaded: " + allItems.size());
        statusLabel.setForeground(Color.GRAY);
        
        topPanel.add(searchBox, BorderLayout.CENTER);
        topPanel.add(statusLabel, BorderLayout.SOUTH);

        // --- SPLIT PANE ---
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, tableScroll);
        split.setDividerLocation(230);
        split.setBackground(UIStyle.BG_MAIN);
        split.setBorder(null);
        if (split.getUI() instanceof javax.swing.plaf.basic.BasicSplitPaneUI) {
            ((javax.swing.plaf.basic.BasicSplitPaneUI) split.getUI()).getDivider().setBorder(null);
        }

        add(topPanel, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        
        // Αν είναι σε mode επιλογής, προσθέτουμε ένα κουμπί "Select" κάτω
        if (parentDialog != null) {
            JButton btnSelect = new JButton("Select Item");
            UIStyle.styleButton(btnSelect, new Color(50, 120, 50), Color.WHITE);
            btnSelect.addActionListener(_ -> confirmSelection());
            add(btnSelect, BorderLayout.SOUTH);
        }
        
        updateTable(allItems);
    }
    
    // --- STATIC METHOD: Κάλεσε αυτήν για να ανοίξει το παράθυρο επιλογής ---
    public static Integer pickItem(Component parent) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Select Item", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(1000, 700);
        dialog.setLocationRelativeTo(parent);
        
        ItemBrowserPanel browser = new ItemBrowserPanel(dialog);
        dialog.add(browser);
        dialog.setVisible(true);
        
        return browser.selectedId; // Επιστρέφει το ID ή null αν το έκλεισε
    }
    
    private void confirmSelection() {
        int row = itemTable.getSelectedRow();
        if (row != -1) {
            selectedId = (Integer) itemTable.getValueAt(row, 0);
            parentDialog.dispose(); // Κλείνει το παράθυρο
        }
    }

    // --- DATA & UI LOGIC (ΙΔΙΑ ΜΕ ΠΡΙΝ) ---

    private void loadDataFromGameMemory() {
        allItems.clear();
        if (GameData.itemInfoMap != null && !GameData.itemInfoMap.isEmpty()) {
            for (ItemParser.ItemInfo info : GameData.itemInfoMap.values()) {
                allItems.add(new ItemData(info.id(), info.name(), info.type(), info.subType(), info.grade()));
            }
            allItems.sort(Comparator.comparing(ItemData::name));
        }
    }

    private DefaultMutableTreeNode createTreeStructure() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("All Items");
        String[] grades = {"No Grade", "D Grade", "C Grade", "B Grade", "A Grade", "S Grade", "S80 Grade", "S84 Grade"};

        // WEAPONS
        DefaultMutableTreeNode wNode = new DefaultMutableTreeNode("Weapon");
        String[] wTypes = {"Sword", "Blunt", "Dagger", "Bow", "Pole", "Fist", "Dual", "Big Sword", "Big Blunt", "Etc"};
        for (String t : wTypes) {
            DefaultMutableTreeNode tNode = new DefaultMutableTreeNode(t);
            for (String g : grades) tNode.add(new DefaultMutableTreeNode(g));
            wNode.add(tNode);
        }
        root.add(wNode);

        // ARMORS
        DefaultMutableTreeNode aNode = new DefaultMutableTreeNode("Armor");
        DefaultMutableTreeNode setsNode = new DefaultMutableTreeNode("Armor Sets");
        for (String g : grades) setsNode.add(new DefaultMutableTreeNode(g));
        aNode.add(setsNode);
        DefaultMutableTreeNode jewelNode = new DefaultMutableTreeNode("Jewels");
        for (String g : grades) jewelNode.add(new DefaultMutableTreeNode(g));
        aNode.add(jewelNode);
        DefaultMutableTreeNode accNode = new DefaultMutableTreeNode("Accessory");
        for (String g : grades) accNode.add(new DefaultMutableTreeNode(g));
        aNode.add(accNode);
        root.add(aNode);

        // ETC ITEMS
        DefaultMutableTreeNode eNode = new DefaultMutableTreeNode("EtcItem");
        eNode.add(new DefaultMutableTreeNode("Enchant Scrolls")); 
        eNode.add(new DefaultMutableTreeNode("Scrolls"));         
        eNode.add(new DefaultMutableTreeNode("Life Stones"));
        eNode.add(new DefaultMutableTreeNode("Soulshots-Spiritshots"));
        eNode.add(new DefaultMutableTreeNode("Potions"));
        eNode.add(new DefaultMutableTreeNode("Consumables")); 
        eNode.add(new DefaultMutableTreeNode("Dyes"));
        eNode.add(new DefaultMutableTreeNode("Arrows"));
        eNode.add(new DefaultMutableTreeNode("Other"));           
        root.add(eNode);

        return root;
    }

    private void filterTableByTree() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) categoryTree.getLastSelectedPathComponent();
        if (node == null) return;
        Object[] path = node.getUserObjectPath();
        if (path.length < 2) { updateTable(allItems); return; } 

        String mainCategory = path[1].toString(); 
        String subCategory = (path.length > 2) ? path[2].toString() : null; 
        String grade = (path.length > 3) ? path[3].toString() : null;       

        List<ItemData> filtered = new ArrayList<>();
        for (ItemData item : allItems) {
            boolean isSetPart = isArmorSetPart(item.type);
            if (mainCategory.equals("Armor")) {
                if (subCategory != null && subCategory.equals("Armor Sets")) {
                    if (!isSetPart) continue;
                } else {
                    if (!item.category.equalsIgnoreCase("Armor") && !isSetPart) continue;
                }
                if (subCategory != null) {
                    if (subCategory.equals("Armor Sets")) { } 
                    else if (subCategory.equals("Jewels")) { if (!item.type.equalsIgnoreCase("Jewels")) continue; } 
                    else if (subCategory.equals("Accessory")) { if (!item.type.equalsIgnoreCase("Accessory")) continue; }
                }
            } else {
                if (!item.category.equalsIgnoreCase(mainCategory)) continue;
                if (subCategory != null) {
                    if (subCategory.equals("Soulshots-Spiritshots")) { if (!item.type.equalsIgnoreCase("Shots")) continue; }
                    else if (subCategory.equals("Consumables")) {
                         if (item.type.equalsIgnoreCase("Shots")) continue;
                         if (!item.type.equalsIgnoreCase("Materials") && !item.type.equalsIgnoreCase("Consumables")) continue;
                    }
                    else if (subCategory.equals("Other")) { if (!item.type.equalsIgnoreCase("Quest")) continue; } 
                    else { if (!item.type.equalsIgnoreCase(subCategory)) continue; }
                }
            }
            if (grade != null) { if (!item.grade.equalsIgnoreCase(grade)) continue; }
            filtered.add(item);
        }
        
        // Sorting Logic (ίδια με πριν)
        if (subCategory != null) {
            if (subCategory.equals("Enchant Scrolls")) {
                filtered.sort((i1, i2) -> {
                    int g1 = getGradeValue(i1.grade);
                    int g2 = getGradeValue(i2.grade);
                    if (g1 != g2) return Integer.compare(g1, g2);
                    return Integer.compare(getScrollTypeValue(i1.name), getScrollTypeValue(i2.name));
                });
            } else if (subCategory.equals("Life Stones")) {
                filtered.sort((i1, i2) -> {
                    int lvl1 = getLifeStoneLevel(i1.name);
                    int lvl2 = getLifeStoneLevel(i2.name);
                    if (lvl1 != lvl2) return Integer.compare(lvl1, lvl2);
                    return Integer.compare(getLifeStoneGradeValue(i1.name), getLifeStoneGradeValue(i2.name));
                });
            } else if (subCategory.equals("Soulshots-Spiritshots")) {
                filtered.sort((i1, i2) -> {
                    int type1 = getShotGroup(i1.name);
                    int type2 = getShotGroup(i2.name);
                    if (type1 != type2) return Integer.compare(type1, type2);
                    int g1 = getGradeValue(i1.grade);
                    int g2 = getGradeValue(i2.grade);
                    if (g1 != g2) return Integer.compare(g1, g2);
                    return i1.name.compareTo(i2.name);
                });
            } else { filtered.sort(Comparator.comparing(ItemData::name)); }
        } else { filtered.sort(Comparator.comparing(ItemData::name)); }

        updateTable(filtered);
    }
    
    // --- Helpers (ΙΔΙΑ ΜΕ ΠΡΙΝ) ---
    private int getShotGroup(String name) {
        String n = name.toLowerCase();
        if (n.startsWith("recipe")) return 3;
        if (n.contains("pack") || n.contains("compress")) return 2;
        if (n.contains("beast")) return 1;
        return 0;
    }
    private int getGradeValue(String grade) {
        if (grade.startsWith("No")) return 0;
        if (grade.startsWith("D")) return 1;
        if (grade.startsWith("C")) return 2;
        if (grade.startsWith("B")) return 3;
        if (grade.startsWith("A")) return 4;
        if (grade.startsWith("S80")) return 6;
        if (grade.startsWith("S84")) return 7;
        if (grade.startsWith("S")) return 5;
        return 0;
    }
    private int getLifeStoneLevel(String name) {
        Matcher m = Pattern.compile("\\d+").matcher(name);
        if (m.find()) { try { return Integer.parseInt(m.group()); } catch (Exception e) {} }
        return 0; 
    }
    private int getLifeStoneGradeValue(String name) {
        String n = name.toLowerCase();
        if (n.contains("top")) return 3;
        if (n.contains("high")) return 2;
        if (n.contains("mid")) return 1;
        return 0;
    }
    private int getScrollTypeValue(String name) {
        String n = name.toLowerCase();
        if (n.contains("blessed")) return 2; 
        if (n.contains("crystal")) return 1; 
        return 0; 
    }
    private boolean isArmorSetPart(String type) {
        if (type == null) return false;
        String t = type.toLowerCase();
        return t.contains("chest") || t.contains("full") || t.contains("legs") || 
               t.contains("helmet") || t.contains("gloves") || t.contains("boots") || 
               t.contains("shield") || t.contains("sigil");
    }
    private void updateTable(List<ItemData> items) {
        tableModel.setRowCount(0);
        for (ItemData item : items) {
            tableModel.addRow(new Object[]{item.id, item.name, item.grade, item.type});
        }
    }
    private void search() {
        String text = searchField.getText();
        if (text.trim().length() == 0) sorter.setRowFilter(null);
        else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
    }
}