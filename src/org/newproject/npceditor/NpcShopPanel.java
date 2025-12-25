package org.newproject.npceditor;

import org.w3c.dom.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NpcShopPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private String multisellPath;
    // TREE COMPONENTS
    private JTree shopTree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    private JTextField searchField;
    private JTable itemTable;
    private DefaultTableModel tableModel;
    private File currentFile;
    private JLabel statusLabel;
    private JTextField linkedNpcsField;

    public NpcShopPanel(String dataRoot) {
        this.multisellPath = dataRoot + File.separator + "shopdata" + File.separator + "multisell";

        setLayout(new BorderLayout(10, 10));
        setBackground(UIStyle.BG_MAIN);
        setBorder(new EmptyBorder(10, 10, 10, 10));
        new File(multisellPath).mkdirs();

        // --- LEFT PANEL (TREE) ---
        JPanel leftPanel = new JPanel(new BorderLayout(0, 5));
        leftPanel.setOpaque(false);
        leftPanel.setPreferredSize(new Dimension(380, 0));

        searchField = new JTextField();
        UIStyle.styleField(searchField);
        searchField.setToolTipText("Live search by NPC name or ID");

        // Live search + auto expand
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void filter() {
                SwingUtilities.invokeLater(() -> {
                    refreshTree();
                    autoExpandMatchingNodes();
                });
            }
            @Override public void insertUpdate(DocumentEvent e) { filter(); }
            @Override public void removeUpdate(DocumentEvent e) { filter(); }
            @Override public void changedUpdate(DocumentEvent e) { filter(); }
        });

        // Initialize Tree
        rootNode = new DefaultMutableTreeNode("All Shops");
        treeModel = new DefaultTreeModel(rootNode);
        shopTree = new JTree(treeModel);
        shopTree.setBackground(UIStyle.BG_FIELD);
        shopTree.setCellRenderer(new UIStyle.DarkTreeCellRenderer());
        shopTree.setRootVisible(false);
        shopTree.setShowsRootHandles(true);

        refreshTree();

        shopTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) shopTree.getLastSelectedPathComponent();
            if (node != null && node.isLeaf()) {
                Object obj = node.getUserObject();
                if (obj instanceof ShopFileNode) {
                    loadMultisell(((ShopFileNode) obj).file);
                }
            }
        });

        JScrollPane scrollTree = new JScrollPane(shopTree);
        UIStyle.styleScrollPane(scrollTree);

        leftPanel.add(searchField, BorderLayout.NORTH);
        leftPanel.add(scrollTree, BorderLayout.CENTER);
        leftPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), "NPC Shop Tree", 0, 0, null, Color.WHITE));

        // --- CENTER PANEL ---
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setOpaque(false);

        JPanel headerPanel = new JPanel(new BorderLayout(5, 5));
        headerPanel.setOpaque(false);

        JLabel lblNpcs = new JLabel("Linked NPC IDs: ");
        lblNpcs.setForeground(Color.WHITE);
        linkedNpcsField = new JTextField();
        UIStyle.styleField(linkedNpcsField);

        headerPanel.add(lblNpcs, BorderLayout.WEST);
        headerPanel.add(linkedNpcsField, BorderLayout.CENTER);
        centerPanel.add(headerPanel, BorderLayout.NORTH);

        String[] columns = {"Item ID", "Item Name", "Type", "Count", "->", "Cost ID", "Cost Name", "Price"};

        tableModel = new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0 || column == 3 || column == 5 || column == 7;
            }
        };

        itemTable = new JTable(tableModel);
        itemTable.setBackground(UIStyle.BG_PANEL);
        itemTable.setForeground(Color.WHITE);
        itemTable.setGridColor(Color.GRAY);
        itemTable.setRowHeight(25);

        tableModel.addTableModelListener(e -> {
            int row = e.getFirstRow();
            int col = e.getColumn();
            if (row < 0) return;
            if (col == 0) updateProductInfo(row);
            else if (col == 5) updateCostInfo(row);
        });

        JScrollPane scrollTable = new JScrollPane(itemTable);
        UIStyle.styleScrollPane(scrollTable);
        centerPanel.add(scrollTable, BorderLayout.CENTER);

        // --- TOOLBAR ---
        JToolBar toolbar = new JToolBar();
        toolbar.setBackground(UIStyle.BG_MAIN);
        toolbar.setFloatable(false);

        JButton btnAdd = new JButton("Add Item");
        UIStyle.styleButton(btnAdd, new Color(0, 100, 0), Color.WHITE);
        btnAdd.addActionListener(e -> addItemRow());

        JButton btnRem = new JButton("Remove Item");
        UIStyle.styleButton(btnRem, new Color(150, 50, 50), Color.WHITE);
        btnRem.addActionListener(e -> removeSelectedRow());

        JButton btnSave = new JButton("Save Shop");
        UIStyle.styleButton(btnSave, new Color(0, 100, 150), Color.WHITE);
        btnSave.addActionListener(e -> saveMultisell());

        JButton btnNew = new JButton("New Shop");
        UIStyle.styleButton(btnNew, new Color(100, 100, 0), Color.WHITE);
        btnNew.addActionListener(e -> createNewShop());

        JButton btnPick = new JButton("Pick Item ID");
        UIStyle.styleButton(btnPick, new Color(100, 100, 100), Color.WHITE);
        btnPick.addActionListener(e -> pickItemForTable());

        toolbar.add(btnNew);
        toolbar.addSeparator();
        toolbar.add(btnAdd);
        toolbar.add(btnRem);
        toolbar.add(btnPick);
        toolbar.addSeparator();
        toolbar.add(btnSave);

        add(leftPanel, BorderLayout.WEST);
        add(centerPanel, BorderLayout.CENTER);
        add(toolbar, BorderLayout.NORTH);

        statusLabel = new JLabel("Select a shop to edit.");
        statusLabel.setForeground(Color.CYAN);
        add(statusLabel, BorderLayout.SOUTH);
    }

    // --- TREE LOGIC ---
    private void refreshTree() {
        rootNode.removeAllChildren();
        Map<Integer, String> comments = ShopData.getNpcComments();
        String query = searchField.getText().toLowerCase().trim();

        // Categories
        DefaultMutableTreeNode groupGatekeeper = new DefaultMutableTreeNode("Gatekeepers");
        DefaultMutableTreeNode groupMerchant = new DefaultMutableTreeNode("Merchants");
        DefaultMutableTreeNode groupBlacksmith = new DefaultMutableTreeNode("Blacksmiths");
        DefaultMutableTreeNode groupTeleporter = new DefaultMutableTreeNode("Teleporters");
        DefaultMutableTreeNode groupOther = new DefaultMutableTreeNode("Others / Custom");

        // Track orphans
        List<File> allFiles = new ArrayList<>();
        File dir = new File(multisellPath);
        if (dir.exists()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".xml"));
            if (files != null) for (File f : files) allFiles.add(f);
        }

        // Add NPCs to Tree
        for (Map.Entry<Integer, String> entry : comments.entrySet()) {
            int npcId = entry.getKey();
            String originalComment = entry.getValue();

            // Live search filter
            if (!query.isEmpty() && 
                !originalComment.toLowerCase().contains(query) && 
                !String.valueOf(npcId).contains(query)) {
                continue;
            }

            // Καθαρίζουμε comment
            String cleanComment = originalComment;
            String lower = originalComment.toLowerCase();
            if (lower.contains("gatekeeper")) cleanComment = cleanComment.replaceAll("(?i)gatekeeper", "").trim();
            if (lower.contains("teleporter")) cleanComment = cleanComment.replaceAll("(?i)teleporter", "").trim();
            if (lower.contains("teleport")) cleanComment = cleanComment.replaceAll("(?i)teleport", "").trim();
            if (cleanComment.isEmpty()) cleanComment = "NPC";

            // Categorize
            DefaultMutableTreeNode targetGroup = groupOther;
            if (lower.contains("gatekeeper")) targetGroup = groupGatekeeper;
            else if (lower.contains("merchant") || lower.contains("trader") || lower.contains("grocer")) targetGroup = groupMerchant;
            else if (lower.contains("blacksmith") || lower.contains("pushkin")) targetGroup = groupBlacksmith;
            else if (lower.contains("teleport")) targetGroup = groupTeleporter;

            List<File> shops = ShopData.getShopsForNpc(npcId);

            if (!shops.isEmpty()) {
                DefaultMutableTreeNode npcNode = new DefaultMutableTreeNode(cleanComment + " [" + npcId + "]");
                boolean hasVisibleChildren = false;

                for (File f : shops) {
                    allFiles.remove(f);
                    String shopType = ShopData.getShopCategory(f.getName());
                    npcNode.add(new DefaultMutableTreeNode(new ShopFileNode(f, f.getName() + " - " + shopType)));
                    hasVisibleChildren = true;
                }

                if (hasVisibleChildren) targetGroup.add(npcNode);
            }
        }

        // Add Orphans
        if (query.isEmpty() && !allFiles.isEmpty()) {
            DefaultMutableTreeNode unassignedNode = new DefaultMutableTreeNode("Unassigned Shops");
            for (File f : allFiles) {
                String shopType = ShopData.getShopCategory(f.getName());
                unassignedNode.add(new DefaultMutableTreeNode(new ShopFileNode(f, f.getName() + " - " + shopType)));
            }
            if (unassignedNode.getChildCount() > 0) groupOther.add(unassignedNode);
        }

        // Add Groups to Root
        if (groupGatekeeper.getChildCount() > 0) rootNode.add(groupGatekeeper);
        if (groupMerchant.getChildCount() > 0) rootNode.add(groupMerchant);
        if (groupBlacksmith.getChildCount() > 0) rootNode.add(groupBlacksmith);
        if (groupTeleporter.getChildCount() > 0) rootNode.add(groupTeleporter);
        if (groupOther.getChildCount() > 0) rootNode.add(groupOther);

        treeModel.reload();

        // Κλείνουμε όλα στην αρχή
        collapseAllTrees();
    }

    // Κλείνει όλα τα nodes
    private void collapseAllTrees() {
        for (int i = shopTree.getRowCount() - 1; i >= 0; i--) {
            shopTree.collapseRow(i);
        }
    }

    // ΝΕΟ: Ανοίγει αυτόματα τα nodes που ταιριάζουν με το search
    private void autoExpandMatchingNodes() {
        String query = searchField.getText().toLowerCase().trim();
        if (query.isEmpty()) return;

        TreeNode root = (TreeNode) shopTree.getModel().getRoot();
        expandAllMatching(shopTree, new TreePath(root), query);
    }

    private boolean expandAllMatching(JTree tree, TreePath parent, String query) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getLastPathComponent();
        String nodeText = node.toString().toLowerCase();

        if (nodeText.contains(query)) {
            tree.expandPath(parent);
            return true;
        }

        boolean found = false;
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            TreePath path = parent.pathByAddingChild(child);
            if (expandAllMatching(tree, path, query)) {
                tree.expandPath(parent);
                found = true;
            }
        }
        return found;
    }

    private static class ShopFileNode {
        File file;
        String display;
        public ShopFileNode(File file, String display) { this.file = file; this.display = display; }
        @Override public String toString() { return display; }
    }

    // --- TABLE & EDITOR LOGIC ---
    private void updateProductInfo(int row) {
        try {
            String idStr = (String) tableModel.getValueAt(row, 0);
            int id = Integer.parseInt(idStr);
            ItemParser.ItemInfo info = GameData.getItem(id);
            String name = (info != null) ? info.name() : "Unknown";
            String type = (info != null && info.type() != null) ? info.type() : "-";
            SwingUtilities.invokeLater(() -> {
                tableModel.setValueAt(name, row, 1);
                tableModel.setValueAt(type, row, 2);
            });
        } catch (Exception e) {}
    }

    private void updateCostInfo(int row) {
        try {
            String idStr = (String) tableModel.getValueAt(row, 5);
            int id = Integer.parseInt(idStr);
            String name = GameData.getItemName(id);
            SwingUtilities.invokeLater(() -> tableModel.setValueAt(name, row, 6));
        } catch (Exception e) {}
    }

    private void createNewShop() {
        String id = JOptionPane.showInputDialog(this, "Enter Shop ID:");
        if (id != null && !id.trim().isEmpty()) {
            File f = new File(multisellPath + File.separator + id + ".xml");
            if (!f.exists()) {
                try {
                    FileWriter fw = new FileWriter(f);
                    fw.write("<?xml version='1.0' encoding='UTF-8'?>\n<list>\n</list>");
                    fw.close();
                    refreshTree();
                } catch (Exception e) {}
            }
        }
    }

    private void loadMultisell(File file) {
        currentFile = file;
        tableModel.setRowCount(0);
        linkedNpcsField.setText("");
        statusLabel.setText("Editing: " + file.getName());
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();
            // Load NPCs
            NodeList npcsList = doc.getElementsByTagName("npcs");
            if (npcsList.getLength() > 0) {
                Element npcsElement = (Element) npcsList.item(0);
                NodeList npcNodes = npcsElement.getElementsByTagName("npc");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < npcNodes.getLength(); i++) {
                    Node node = npcNodes.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        String id = node.getTextContent().trim();
                        if (id.contains(" ")) id = id.split(" ")[0];
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(id);
                    }
                }
                linkedNpcsField.setText(sb.toString());
            }
            // Load Items
            NodeList nList = doc.getElementsByTagName("item");
            for (int i = 0; i < nList.getLength(); i++) {
                Node nNode = nList.item(i);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    String prodId = "0", prodName = "-", prodType = "-", prodCount = "1";
                    String costId = "0", costName = "-", costCount = "0";
                    NodeList prodList = eElement.getElementsByTagName("production");
                    if (prodList.getLength() > 0) {
                        Element prod = (Element) prodList.item(0);
                        prodId = prod.getAttribute("id");
                        prodCount = prod.getAttribute("count");
                        ItemParser.ItemInfo info = GameData.getItem(Integer.parseInt(prodId));
                        if (info != null) { prodName = info.name(); prodType = info.type(); }
                    }
                    NodeList ingList = eElement.getElementsByTagName("ingredient");
                    if (ingList.getLength() > 0) {
                        Element ing = (Element) ingList.item(0);
                        costId = ing.getAttribute("id");
                        costCount = ing.getAttribute("count");
                        costName = GameData.getItemName(Integer.parseInt(costId));
                    }
                    tableModel.addRow(new Object[]{prodId, prodName, prodType, prodCount, "->", costId, costName, costCount});
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void addItemRow() {
        tableModel.addRow(new Object[]{"23", "Short Sword", "Weapon", "1", "->", "57", "Adena", "100"});
    }

    private void pickItemForTable() {
        int row = itemTable.getSelectedRow();
        int col = itemTable.getSelectedColumn();
        if (row != -1 && (col == 0 || col == 5)) {
            Integer id = ItemBrowserPanel.pickItem(this);
            if (id != null) itemTable.setValueAt(String.valueOf(id), row, col);
        } else {
            JOptionPane.showMessageDialog(this, "Select an ID cell (Column 1 or 6) first.");
        }
    }

    private void removeSelectedRow() {
        int row = itemTable.getSelectedRow();
        if (row != -1) tableModel.removeRow(row);
    }

    private void saveMultisell() {
        if (currentFile == null) return;
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("list");
            doc.appendChild(rootElement);
            String npcText = linkedNpcsField.getText().trim();
            if (!npcText.isEmpty()) {
                Element npcsElem = doc.createElement("npcs");
                rootElement.appendChild(npcsElem);
                for (String id : npcText.split(",")) {
                    id = id.trim();
                    if (!id.isEmpty()) {
                        Element npc = doc.createElement("npc");
                        npc.setTextContent(id);
                        npcsElem.appendChild(npc);
                    }
                }
            }
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String prodId = (String) tableModel.getValueAt(i, 0);
                String prodCount = (String) tableModel.getValueAt(i, 3);
                String costId = (String) tableModel.getValueAt(i, 5);
                String costCount = (String) tableModel.getValueAt(i, 7);
                Element item = doc.createElement("item");
                rootElement.appendChild(item);
                Element ingredient = doc.createElement("ingredient");
                ingredient.setAttribute("id", costId);
                ingredient.setAttribute("count", costCount);
                item.appendChild(ingredient);
                Element production = doc.createElement("production");
                production.setAttribute("id", prodId);
                production.setAttribute("count", prodCount);
                item.appendChild(production);
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(currentFile);
            transformer.transform(source, result);
            JOptionPane.showMessageDialog(this, "Saved!");

            ShopData.loadAll(new File(multisellPath).getParentFile().getParent());
            refreshTree();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
}