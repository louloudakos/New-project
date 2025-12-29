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

    private static String DATA_ROOT = "C:\\Users\\flowe\\eclipse-workspace\\NpcXmlEditor\\data";

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

        centerCardPanel.add(npcDetailPanel, "INDIVIDUAL");
        centerCardPanel.add(new MassOperationPanel(allNpcs, dirtyNpcs), "MASS");
        centerCardPanel.add(new NpcShopPanel(DATA_ROOT), "SHOPS");

        mainPanel.add(centerCardPanel, BorderLayout.CENTER);

        setupLeftPanel();
        setupSearchListeners();
        refreshList();

        collapseAll(monsterTree);
        collapseAll(worldTree);
    }

    private void setupMenuBar() {
        JMenuBar mb = new JMenuBar();
        mb.setBackground(UIStyle.BG_MAIN);
        JMenu fileMenu = new JMenu("File");
        fileMenu.setForeground(Color.WHITE);

        JMenuItem changeRoot = new JMenuItem("Change ROOT Folder");
        changeRoot.addActionListener(_ -> changeDataFolder());

        JMenuItem exportCsv = new JMenuItem("Export NPCs to CSV");
        exportCsv.addActionListener(_ -> exportToCsv());

        JMenuItem createNpc = new JMenuItem("Create New NPC");
        createNpc.addActionListener(_ -> createNewNpcWizard());

        fileMenu.add(changeRoot);
        fileMenu.add(exportCsv);
        fileMenu.addSeparator();
        fileMenu.add(createNpc);

        mb.add(fileMenu);
        setJMenuBar(mb);
    }

    private void createNewNpcWizard() {
        NpcCreator.createNewNpc(this, allNpcs, DATA_ROOT);
        refreshData();
    }

    private void setupTopNavigation() {
        topBarContainer = new JPanel(new BorderLayout(10, 10));
        topBarContainer.setOpaque(false);
        JPanel navButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        navButtons.setOpaque(false);

        JButton btnModeSearch = new JButton("Search & Individual Edit");
        JButton btnModeMass = new JButton("Mass Operations");
        JButton btnModeShop = new JButton("NPC Shops (Multisell)");
        JButton btnReload = new JButton("Reload Data");
        JButton btnWiki = new JButton("Help / Wiki");

        UIStyle.styleButton(btnModeSearch, new Color(50, 120, 50), Color.WHITE);
        UIStyle.styleButton(btnModeMass, new Color(120, 50, 50), Color.WHITE);
        UIStyle.styleButton(btnModeShop, new Color(100, 0, 150), Color.WHITE);
        UIStyle.styleButton(btnReload, new Color(50, 50, 120), Color.WHITE);
        UIStyle.styleButton(btnWiki, new Color(0, 150, 200), Color.WHITE);

        navButtons.add(btnModeSearch);
        navButtons.add(btnModeMass);
        navButtons.add(btnModeShop);
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
        saveAllBtn.addActionListener(_ -> saveAllModified());

        topBarContainer.add(navButtons, BorderLayout.WEST);
        topBarContainer.add(searchBarPanel, BorderLayout.CENTER);
        topBarContainer.add(saveAllBtn, BorderLayout.EAST);
        mainPanel.add(topBarContainer, BorderLayout.NORTH);

        btnModeSearch.addActionListener(_ -> {
            searchBarPanel.setVisible(true);
            centerCards.show(centerCardPanel, "INDIVIDUAL");
        });
        btnModeMass.addActionListener(_ -> {
            searchBarPanel.setVisible(false);
            centerCards.show(centerCardPanel, "MASS");
        });
        btnModeShop.addActionListener(_ -> {
            searchBarPanel.setVisible(false);
            centerCards.show(centerCardPanel, "SHOPS");
        });

        btnReload.addActionListener(_ -> refreshData());
        btnWiki.addActionListener(_ -> showWikiDialog());
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
        tree.setCellRenderer(new UIStyle.DarkTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                if (value instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof Npc npc) {
                    String name = npc.name != null ? npc.name : "Unknown";
                    String lower = name.toLowerCase();
                    if (lower.contains("gatekeeper")) name = name.replaceAll("(?i)gatekeeper", "").trim();
                    if (lower.contains("teleporter")) name = name.replaceAll("(?i)teleporter", "").trim();
                    if (name.isEmpty()) name = "NPC";
                    setText(name + " (ID: " + npc.id + ")");
                }
                return this;
            }
        });
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.addTreeSelectionListener(_ -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (node != null && node.getUserObject() instanceof Npc npc) {
                searchBarPanel.setVisible(true);
                centerCards.show(centerCardPanel, "INDIVIDUAL");
                showNpcDetails(npc);
            }
        });
    }

    private void showNpcDetails(Npc npc) {
        npcDetailPanel.removeAll();
        npcDetailPanel.add(new NpcDetailsPanel(npc, dirtyNpcs, DATA_ROOT), BorderLayout.CENTER);
        npcDetailPanel.revalidate();
        npcDetailPanel.repaint();
    }

    // Τώρα public αντί private
    public void refreshList() {
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
        } else {
            collapseAll(monsterTree);
            collapseAll(worldTree);
        }
    }

    private void collapseAll(JTree tree) {
        int row = tree.getRowCount() - 1;
        while (row >= 0) {
            tree.collapseRow(row);
            row--;
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
                // Νέο check για custom NPCs βασισμένο σε ID >= 30000
                if (n.id >= 30000) {
                    worldGroups.computeIfAbsent("CustomNpc's", k -> new DefaultMutableTreeNode(k))
                            .add(new DefaultMutableTreeNode(n));
                } else if (type.contains("chest") || (n.name != null && n.name.toLowerCase().contains("chest"))) {
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
                    String folderName = (n.type == null || n.type.trim().isEmpty()) ? "CustomNpc's" : n.type;
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
        if (grandBossNode.getChildCount() > 0) monsterRoot.add(grandBossNode);
        if (!rbGroups.isEmpty()) {
            monsterRoot.add(raidBossesMainNode);
            rbGroups.values().forEach(raidBossesMainNode::add);
        }
        if (!mGroups.isEmpty()) {
            monsterRoot.add(monstersMainNode);
            mGroups.values().forEach(monstersMainNode::add);
        }
        if (chestNode.getChildCount() > 0) monsterRoot.add(chestNode);
        if (!worldGroups.isEmpty()) worldGroups.values().forEach(worldRoot::add);
    }

    private boolean matchesSearch(Npc npc, String nQ, String iQ) {
        boolean npcMatch = nQ.isEmpty() || (npc.name != null && npc.name.toLowerCase().contains(nQ)) || String.valueOf(npc.id).contains(nQ);
        if (!npcMatch) return false;
        if (iQ.isEmpty()) return true;
        for (DropGroup dg : npc.dropGroups) {
            for (Npc.Item item : dg.items) {
                if (String.valueOf(item.id).contains(iQ) || (item.description != null && item.description.toLowerCase().contains(iQ))) return true;
            }
        }
        for (Npc.Item spoil : npc.spoilItems) {
            if (String.valueOf(spoil.id).contains(iQ) || (spoil.description != null && spoil.description.toLowerCase().contains(iQ))) return true;
        }
        return false;
    }

    private void expandAll(JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
    }

    private void setupSearchListeners() {
        DocumentListener dl = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { refreshList(); }
            @Override
            public void removeUpdate(DocumentEvent e) { refreshList(); }
            @Override
            public void changedUpdate(DocumentEvent e) { refreshList(); }
        };
        npcSearchField.getDocument().addDocumentListener(dl);
        itemSearchField.getDocument().addDocumentListener(dl);
    }

    // Τώρα public αντί private
    public void refreshData() {
        try {
            allNpcs = NpcReader.loadNpcs(DATA_ROOT);
            dirtyNpcs.clear();
            refreshList();
            JOptionPane.showMessageDialog(this, "Data reloaded successfully!", "Reload", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error reloading data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.SEVERE, "Reload failed", ex);
        }
    }

    private void saveAllModified() {
        if (dirtyNpcs.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No changes to save!", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            NpcWriter.saveAllNpcs(new ArrayList<>(dirtyNpcs), DATA_ROOT);
            dirtyNpcs.clear();
            JOptionPane.showMessageDialog(this, "All changes saved!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error saving: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.SEVERE, "Save failed", ex);
        }
    }

    private JPanel createSearchBox(String labelText, JTextField field, Color bg) {
        JPanel box = new JPanel(new BorderLayout(5, 0));
        box.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setForeground(Color.LIGHT_GRAY);
        box.add(label, BorderLayout.WEST);
        box.add(field, BorderLayout.CENTER);
        return box;
    }

    private void changeDataFolder() {
        JFileChooser fc = new JFileChooser(DATA_ROOT);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            DATA_ROOT = fc.getSelectedFile().getAbsolutePath();
            refreshData();
        }
    }

    private void exportToCsv() {
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if (!file.getName().endsWith(".csv")) file = new File(file.getParentFile(), file.getName() + ".csv");
            try (java.io.PrintWriter pw = new java.io.PrintWriter(file)) {
                pw.println("ID,Name,Level,Type,Title,Exp,SP,HP,MP,PAtk,MAtk,PDef,MDef");
                for (Npc n : allNpcs) {
                    pw.printf("%d,%s,%d,%s,%s,%d,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f%n",
                            n.id, n.name, n.level, n.type, n.title, n.exp, n.sp, n.hp, n.mp, n.pAtk, n.mAtk, n.pDef, n.mDef);
                }
                JOptionPane.showMessageDialog(this, "Exported to " + file.getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Νέα public method για add σε dirtyNpcs
    public void addDirtyNpc(Npc npc) {
        dirtyNpcs.add(npc);
    }

    public static void main(String[] args) {
        try {
            List<Npc> npcs = NpcReader.loadNpcs(DATA_ROOT);
            SwingUtilities.invokeLater(() -> new NpcEditor(npcs).setVisible(true));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Startup error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.SEVERE, "Startup failed", ex);
        }
    }
}