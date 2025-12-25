package org.newproject.npceditor;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NpcEditor extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(NpcEditor.class.getName());
    
    private static String DATA_ROOT = System.getProperty("user.dir") + File.separator + "data"; 
    
    private static final int DEFAULT_WIDTH = 1550;
    private static final int DEFAULT_HEIGHT = 950;
    private static final Color SEARCH_NPC_COLOR = Color.WHITE;
    private static final Color SEARCH_ITEM_COLOR = Color.YELLOW;

    private JTextField npcSearchField, itemSearchField;
    private JLabel statusLabel;
    private JPanel mainPanel, topBarContainer, searchBarPanel, centerCardPanel, npcDetailPanel;
    private CardLayout centerCards;
    
    private JTree monsterTree, worldTree;
    private DefaultTreeModel monsterModel, worldModel;
    private DefaultMutableTreeNode monsterRoot, worldRoot;
    private List<Npc> allNpcs;
    private Set<Npc> dirtyNpcs = new HashSet<>();
    
    public NpcEditor(List<Npc> npcs) {
        this.allNpcs = npcs;
        UIStyle.applyGlobalTheme();
        setTitle("L2J NPC XML Editor - Pro Organizer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setLocationRelativeTo(null);
        getContentPane().setBackground(UIStyle.BG_MAIN);
        
        setupMenuBar();
        
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(UIStyle.BG_MAIN);
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(mainPanel);
        
        setupTopNavigation();
        
        centerCards = new CardLayout();
        centerCardPanel = new JPanel(centerCards);
        centerCardPanel.setOpaque(false);
        npcDetailPanel = new JPanel(new BorderLayout());
        npcDetailPanel.setBackground(UIStyle.BG_MAIN);
        
        // --- ADDED PANELS ---
        centerCardPanel.add(npcDetailPanel, "INDIVIDUAL");
        centerCardPanel.add(new MassOperationPanel(allNpcs, dirtyNpcs), "MASS");
        // Προσθήκη του Shop Panel
        centerCardPanel.add(new NpcShopPanel(DATA_ROOT), "SHOPS"); 
        
        mainPanel.add(centerCardPanel, BorderLayout.CENTER);
        
        setupLeftPanel();
        setupSearchListeners();
        refreshList();
    }
    
    private void setupMenuBar() { 
        JMenuBar mb = new JMenuBar(); 
        mb.setBackground(UIStyle.BG_MAIN); 
        JMenu fileMenu = new JMenu("File"); 
        fileMenu.setForeground(Color.WHITE); 
        
        JMenuItem changeRoot = new JMenuItem("Change ROOT Folder");
        changeRoot.addActionListener(e -> changeDataFolder());
        
        JMenuItem exportCsv = new JMenuItem("Export NPCs to CSV"); 
        exportCsv.addActionListener(e -> exportToCsv());
        
        fileMenu.add(changeRoot);
        fileMenu.add(exportCsv);
        mb.add(fileMenu); 
        setJMenuBar(mb); 
    }

    private void setupTopNavigation() {
        topBarContainer = new JPanel(new BorderLayout(10, 10));
        topBarContainer.setOpaque(false);
        JPanel navButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        navButtons.setOpaque(false);
        
        JButton btnModeSearch = new JButton("Search & Individual Edit");
        JButton btnModeMass = new JButton("Mass Operations");
        // ΝΕΟ ΚΟΥΜΠΙ SHOP
        JButton btnModeShop = new JButton("NPC Shops (Multisell)");
        JButton btnReload = new JButton("Reload Data"); 
        JButton btnWiki = new JButton("Help / Wiki");
        
        UIStyle.styleButton(btnModeSearch, new Color(50, 120, 50), Color.WHITE);
        UIStyle.styleButton(btnModeMass, new Color(120, 50, 50), Color.WHITE);
        // Style για το Shop button (Μωβ)
        UIStyle.styleButton(btnModeShop, new Color(100, 0, 150), Color.WHITE);
        UIStyle.styleButton(btnReload, new Color(50, 50, 120), Color.WHITE);
        UIStyle.styleButton(btnWiki, new Color(0, 150, 200), Color.WHITE);
        
        navButtons.add(btnModeSearch); 
        navButtons.add(btnModeMass);
        navButtons.add(btnModeShop); // Προσθήκη
        navButtons.add(btnReload);
        navButtons.add(btnWiki);
        
        searchBarPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        searchBarPanel.setOpaque(false);
        npcSearchField = new JTextField(); 
        UIStyle.styleField(npcSearchField);
        npcSearchField.setToolTipText("Search by NPC name or ID (case-insensitive)"); 
        
        itemSearchField = new JTextField(); 
        UIStyle.styleField(itemSearchField);
        itemSearchField.setToolTipText("Search by item ID or name in drops/spoils (case-insensitive)");
        
        searchBarPanel.add(createSearchBox("Search NPC:", npcSearchField, SEARCH_NPC_COLOR));
        searchBarPanel.add(createSearchBox("Search Item:", itemSearchField, SEARCH_ITEM_COLOR));
        
        JButton saveAllBtn = new JButton("Save All Modified");
        UIStyle.styleButton(saveAllBtn, new Color(160, 60, 0), Color.WHITE);
        saveAllBtn.addActionListener(e -> saveAllModified());
        
        topBarContainer.add(navButtons, BorderLayout.WEST);
        topBarContainer.add(searchBarPanel, BorderLayout.CENTER);
        topBarContainer.add(saveAllBtn, BorderLayout.EAST);
        mainPanel.add(topBarContainer, BorderLayout.NORTH);
        
        // Action Listeners
        btnModeSearch.addActionListener(e -> { 
            searchBarPanel.setVisible(true); 
            centerCards.show(centerCardPanel, "INDIVIDUAL"); 
        });
        btnModeMass.addActionListener(e -> { 
            searchBarPanel.setVisible(false); 
            centerCards.show(centerCardPanel, "MASS"); 
        });
        // Action Listener για το Shop
        btnModeShop.addActionListener(e -> {
            searchBarPanel.setVisible(false); // Κρύβουμε την αναζήτηση NPC
            centerCards.show(centerCardPanel, "SHOPS");
        });
        
        btnReload.addActionListener(e -> refreshData()); 
        btnWiki.addActionListener(e -> showWikiDialog());
    }
    
    private void showWikiDialog() {
        JDialog dialog = new JDialog(this, "NpcEditor Wiki & Rules", false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        WikiPanel wiki = new WikiPanel();
        dialog.add(wiki);
        dialog.setSize(500, 650); 
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void setupLeftPanel() {
        JTabbedPane leftTabs = new JTabbedPane();
        leftTabs.setUI(new UIStyle.BoxTabbedPaneUI());
        
        monsterRoot = new DefaultMutableTreeNode("Hunting Grounds");
        monsterModel = new DefaultTreeModel(monsterRoot);
        monsterTree = new JTree(monsterModel);
        setupTreeStyle(monsterTree);
        
        worldRoot = new DefaultMutableTreeNode("Towns & World");
        worldModel = new DefaultTreeModel(worldRoot);
        worldTree = new JTree(worldModel);
        setupTreeStyle(worldTree);
        
        leftTabs.addTab("Hunting", new JScrollPane(monsterTree));
        leftTabs.addTab("World", new JScrollPane(worldTree));
        
        JPanel leftPanel = new JPanel(new BorderLayout(0, 5));
        leftPanel.setOpaque(false); 
        leftPanel.setPreferredSize(new Dimension(350, 0));
        leftPanel.add(leftTabs, BorderLayout.CENTER);
        
        statusLabel = new JLabel("Found: 0 NPCs"); 
        statusLabel.setForeground(Color.LIGHT_GRAY);
        leftPanel.add(statusLabel, BorderLayout.SOUTH);
        mainPanel.add(leftPanel, BorderLayout.WEST);
    }
    
    private void setupTreeStyle(JTree tree) {
        tree.setBackground(UIStyle.BG_PANEL);
        tree.setCellRenderer(new UIStyle.DarkTreeCellRenderer());
        tree.setRootVisible(false); 
        tree.setShowsRootHandles(true);
        tree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (node != null && node.getUserObject() instanceof Npc) {
                searchBarPanel.setVisible(true); 
                centerCards.show(centerCardPanel, "INDIVIDUAL");
                showNpcDetails((Npc) node.getUserObject());
            }
        });
    }
    
    private void showNpcDetails(Npc npc) {
        npcDetailPanel.removeAll();
        npcDetailPanel.add(new NpcDetailsPanel(npc, dirtyNpcs, DATA_ROOT), BorderLayout.CENTER);
        npcDetailPanel.revalidate(); 
        npcDetailPanel.repaint();
    }
    
    private void refreshList() {
        monsterRoot.removeAllChildren(); 
        worldRoot.removeAllChildren();
        if (allNpcs == null || allNpcs.isEmpty()) {
            statusLabel.setText("Found: 0 NPCs");
            return;
        }
        
        String nQ = npcSearchField.getText().toLowerCase().trim();
        String iQ = itemSearchField.getText().toLowerCase().trim();
        
        DefaultMutableTreeNode grandBossNode = new DefaultMutableTreeNode("GRAND BOSSES");
        DefaultMutableTreeNode raidBossesMainNode = new DefaultMutableTreeNode("RAID BOSSES");
        DefaultMutableTreeNode monstersMainNode = new DefaultMutableTreeNode("MONSTERS");
        DefaultMutableTreeNode chestNode = new DefaultMutableTreeNode("CHESTS");
        
        Map<Integer, DefaultMutableTreeNode> rbGroups = new TreeMap<>();
        Map<Integer, DefaultMutableTreeNode> mGroups = new TreeMap<>();
        Map<String, DefaultMutableTreeNode> worldGroups = new TreeMap<>();
        
        int total = populateGroups(nQ, iQ, grandBossNode, raidBossesMainNode, monstersMainNode, chestNode, rbGroups, mGroups, worldGroups);
        
        attachGroupsToRoots(grandBossNode, raidBossesMainNode, monstersMainNode, chestNode, rbGroups, mGroups, worldGroups);
        
        monsterModel.reload(); 
        worldModel.reload();
        statusLabel.setText("Found: " + total + " NPCs");
        
        if (!nQ.isEmpty() || !iQ.isEmpty()) { 
            expandAll(monsterTree); 
            expandAll(worldTree); 
        }
    }
    
    private int populateGroups(String nQ, String iQ, DefaultMutableTreeNode grandBossNode, DefaultMutableTreeNode raidBossesMainNode,
                               DefaultMutableTreeNode monstersMainNode, DefaultMutableTreeNode chestNode,
                               Map<Integer, DefaultMutableTreeNode> rbGroups, Map<Integer, DefaultMutableTreeNode> mGroups,
                               Map<String, DefaultMutableTreeNode> worldGroups) {
        int total = 0;
        for (Npc n : allNpcs) {
            boolean matches = matchesSearch(n, nQ, iQ);
            if (matches) {
                total++;
                String type = getNpcType(n);
                
                if (type.contains("chest") || (n.name != null && n.name.toLowerCase().contains("chest"))) {
                    chestNode.add(new DefaultMutableTreeNode(n));
                } else if (type.contains("grandboss")) {
                    grandBossNode.add(new DefaultMutableTreeNode(n));
                } else if (type.contains("raidboss")) {
                    int bucket = (n.level - 1) / 10;
                    rbGroups.computeIfAbsent(bucket, b -> new DefaultMutableTreeNode("Raids Lv " + String.format("%02d-%02d", b*10+1, (b+1)*10)))
                            .add(new DefaultMutableTreeNode(n));
                } else if (type.contains("monster") || type.contains("minion")) {
                    int bucket = (n.level - 1) / 10;
                    mGroups.computeIfAbsent(bucket, b -> new DefaultMutableTreeNode("Monsters Lv " + String.format("%02d-%02d", b*10+1, (b+1)*10)))
                            .add(new DefaultMutableTreeNode(n));
                } else {
                    String folderName = (n.type == null || n.type.trim().isEmpty()) ? "Custom NPC's" : n.type;
                    worldGroups.computeIfAbsent(folderName, k -> new DefaultMutableTreeNode(k))
                            .add(new DefaultMutableTreeNode(n));
                }
            }
        }
        return total;
    }
    
    private String getNpcType(Npc n) {
        return (n.type == null || n.type.trim().isEmpty()) ? "" : n.type.toLowerCase();
    }
    
    private void attachGroupsToRoots(DefaultMutableTreeNode grandBossNode, DefaultMutableTreeNode raidBossesMainNode,
                                     DefaultMutableTreeNode monstersMainNode, DefaultMutableTreeNode chestNode,
                                     Map<Integer, DefaultMutableTreeNode> rbGroups, Map<Integer, DefaultMutableTreeNode> mGroups,
                                     Map<String, DefaultMutableTreeNode> worldGroups) {
        if (grandBossNode.getChildCount() > 0) {
            grandBossNode.setUserObject(grandBossNode.getUserObject() + " (" + grandBossNode.getChildCount() + ")");
            monsterRoot.add(grandBossNode);
        }
        if (!rbGroups.isEmpty()) {
            for (DefaultMutableTreeNode node : rbGroups.values()) {
                node.setUserObject(node.getUserObject() + " (" + node.getChildCount() + ")");
                raidBossesMainNode.add(node);
            }
            raidBossesMainNode.setUserObject(raidBossesMainNode.getUserObject() + " (" + raidBossesMainNode.getLeafCount() + ")");
            monsterRoot.add(raidBossesMainNode);
        }
        if (!mGroups.isEmpty()) {
            for (DefaultMutableTreeNode node : mGroups.values()) {
                node.setUserObject(node.getUserObject() + " (" + node.getChildCount() + ")");
                monstersMainNode.add(node);
            }
            monstersMainNode.setUserObject(monstersMainNode.getUserObject() + " (" + monstersMainNode.getLeafCount() + ")");
            monsterRoot.add(monstersMainNode);
        }
        if (chestNode.getChildCount() > 0) {
            chestNode.setUserObject(chestNode.getUserObject() + " (" + chestNode.getChildCount() + ")");
            monsterRoot.add(chestNode);
        }
        for (DefaultMutableTreeNode node : worldGroups.values()) {
            node.setUserObject(node.getUserObject() + " (" + node.getChildCount() + ")");
            worldRoot.add(node);
        }
    }
    
    private void expandAll(JTree t) { 
        for (int i = 0; i < t.getRowCount(); i++) t.expandRow(i); 
    }
    
    private boolean matchesSearch(Npc n, String nQ, String iQ) {
        if (!iQ.isEmpty()) {
            return containsItemOrName(n, iQ);
        } else {
            return nQ.isEmpty() || (n.name != null && n.name.toLowerCase().contains(nQ)) || String.valueOf(n.id).startsWith(nQ);
        }
    }
    
    private boolean containsItemOrName(Npc n, String q) {
        if (n.dropGroups != null) {
            for (DropGroup g : n.dropGroups) {
                if (g.items != null) {
                    for (Npc.Item it : g.items) {
                        String itemName = GameData.getItemName(it.id);
                        boolean matchesId = String.valueOf(it.id).equals(q);
                        boolean matchesName = (itemName != null && itemName.toLowerCase().contains(q));
                        if (matchesId || matchesName) return true;
                    }
                }
            }
        }
        if (n.spoilItems != null) {
            for (Npc.Item it : n.spoilItems) {
                String itemName = GameData.getItemName(it.id);
                boolean matchesId = String.valueOf(it.id).equals(q);
                boolean matchesName = (itemName != null && itemName.toLowerCase().contains(q));
                if (matchesId || matchesName) return true;
            }
        }
        return false;
    }
    
    private void saveAllModified() {
        if (dirtyNpcs.isEmpty()) { 
            JOptionPane.showMessageDialog(this, "No changes to save."); 
            return; 
        }
        if (JOptionPane.showConfirmDialog(this, "Save " + dirtyNpcs.size() + " modified NPCs? (Backup created)", "Confirm Save", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            backupNpcs();
            
            JProgressBar progressBar = new JProgressBar(0, dirtyNpcs.size());
            JDialog progressDialog = createProgressDialog(progressBar, "Saving...");
            
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    try {
                        NpcWriter.saveAllNpcs(allNpcs, DATA_ROOT + File.separator + "npcs");
                        int count = 0;
                        for (@SuppressWarnings("unused") Npc npc : dirtyNpcs) {
                            progressBar.setValue(++count);
                        }
                        dirtyNpcs.clear();
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error saving NPCs", e);
                        JOptionPane.showMessageDialog(NpcEditor.this, "Error during save: " + e.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
                    }
                    return null;
                }
                
                @Override
                protected void done() {
                    progressDialog.dispose();
                    JOptionPane.showMessageDialog(NpcEditor.this, "All modified NPCs saved successfully.");
                }
            };
            
            worker.execute(); 
            progressDialog.setVisible(true);
        }
    }
    
    private void backupNpcs() {
        try {
            String backupDir = DATA_ROOT + File.separator + "backup_" + System.currentTimeMillis();
            File backupFolder = new File(backupDir);
            backupFolder.mkdirs();
            LOGGER.info("Backup created at: " + backupDir);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Backup failed", e);
        }
    }
    
    private JDialog createProgressDialog(JProgressBar progressBar, String title) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.add(BorderLayout.CENTER, progressBar);
        dialog.setSize(300, 75);
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        return dialog;
    }
    
    private void changeDataFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select ROOT 'data' Folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (!DATA_ROOT.isEmpty()) { 
            File f = new File(DATA_ROOT); 
            if (f.exists() && f.isDirectory()) chooser.setCurrentDirectory(f); 
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            DATA_ROOT = chooser.getSelectedFile().getAbsolutePath();
            refreshData();
        }
    }
    
    private void refreshData() {
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        JDialog progressDialog = createProgressDialog(progressBar, "Reloading Data...");
        
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    GameData.loadAll(DATA_ROOT);
                    ShopData.loadAll(DATA_ROOT); // Φόρτωση Shop Data
                    allNpcs = NpcReader.loadNpcs(DATA_ROOT + File.separator + "npcs");
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error loading data", e);
                    JOptionPane.showMessageDialog(NpcEditor.this, "Error loading data: " + e.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
                }
                return null;
            }
            
            @Override
            protected void done() {
                progressDialog.dispose();
                refreshList();
                // Ενημέρωση των Panels
                centerCardPanel.add(new MassOperationPanel(allNpcs, dirtyNpcs), "MASS");
                centerCardPanel.add(new NpcShopPanel(DATA_ROOT), "SHOPS"); // Re-add Shop Panel
                JOptionPane.showMessageDialog(NpcEditor.this, "Data reloaded successfully.");
            }
        };
        
        worker.execute();
        progressDialog.setVisible(true);
    }
    
    private void exportToCsv() {
        if (allNpcs == null || allNpcs.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No NPCs to export.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save CSV File");
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().endsWith(".csv")) {
                file = new File(file.getAbsolutePath() + ".csv");
            }
            try {
                java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(file));
                writer.write("ID,Name,Level,Type\n"); 
                for (Npc n : allNpcs) {
                    writer.write(n.id + "," + (n.name != null ? n.name : "") + "," + n.level + "," + (n.type != null ? n.type : "") + "\n");
                }
                writer.close();
                JOptionPane.showMessageDialog(this, "Exported to " + file.getAbsolutePath());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Export failed", e);
                JOptionPane.showMessageDialog(this, "Error exporting: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void setupSearchListeners() { 
        DocumentListener dl = new DocumentListener() { 
            public void handle() { 
                SwingUtilities.invokeLater(NpcEditor.this::refreshList); 
            } 
            public void insertUpdate(DocumentEvent e) { handle(); } 
            public void removeUpdate(DocumentEvent e) { handle(); } 
            public void changedUpdate(DocumentEvent e) { handle(); } 
        }; 
        npcSearchField.getDocument().addDocumentListener(dl); 
        itemSearchField.getDocument().addDocumentListener(dl); 
    }
    
    private JPanel createSearchBox(String labelText, JTextField field, Color labelColor) { 
        JPanel panel = new JPanel(new BorderLayout(5, 0)); 
        panel.setOpaque(false); 
        JLabel label = new JLabel(labelText); 
        label.setForeground(labelColor); 
        panel.add(label, BorderLayout.WEST); 
        panel.add(field, BorderLayout.CENTER); 
        return panel; 
    }
    
    public static void main(String[] args) {
        try { 
            UIStyle.applyGlobalTheme(); 
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Theme application failed", e);
        }
        SwingUtilities.invokeLater(() -> { 
            try {
                GameData.loadAll(DATA_ROOT);
                ShopData.loadAll(DATA_ROOT); // Φόρτωση Shop Data
                new NpcEditor(NpcReader.loadNpcs(DATA_ROOT + File.separator + "npcs")).setVisible(true);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Startup failed", e);
                JOptionPane.showMessageDialog(null, "Error starting app: " + e.getMessage(), "Startup Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}