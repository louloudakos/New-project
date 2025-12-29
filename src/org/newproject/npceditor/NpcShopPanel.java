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
import java.util.HashMap;
import java.util.TreeMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.stream.Stream;

public class NpcShopPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private String multisellPath;
    private String customNpcPath;
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
        this.customNpcPath = dataRoot + File.separator + "CustomNpc's";
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
        shopTree.addTreeSelectionListener(_ -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) shopTree.getLastSelectedPathComponent();
            if (node != null) {
                if (node.isLeaf()) {
                    Object obj = node.getUserObject();
                    if (obj instanceof ShopFileNode) {
                        loadMultisell(((ShopFileNode) obj).file);
                    }
                } else {
                    // If NPC node selected, load the first linked shop or show list
                    if (node.getChildCount() > 0) {
                        DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode) node.getChildAt(0);
                        Object obj = firstChild.getUserObject();
                        if (obj instanceof ShopFileNode) {
                            loadMultisell(((ShopFileNode) obj).file);
                        }
                    }
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
        btnAdd.addActionListener(_ -> addItemRow());
        JButton btnRem = new JButton("Remove Item");
        UIStyle.styleButton(btnRem, new Color(150, 50, 50), Color.WHITE);
        btnRem.addActionListener(_ -> removeSelectedRow());
        JButton btnSave = new JButton("Save Shop");
        UIStyle.styleButton(btnSave, new Color(0, 100, 150), Color.WHITE);
        btnSave.addActionListener(_ -> saveMultisell());
        JButton btnNew = new JButton("New Shop");
        UIStyle.styleButton(btnNew, new Color(100, 100, 0), Color.WHITE);
        btnNew.addActionListener(_ -> createNewShop());
        JButton btnPick = new JButton("Pick Item ID");
        UIStyle.styleButton(btnPick, new Color(100, 100, 100), Color.WHITE);
        btnPick.addActionListener(_ -> pickItemForTable());
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
        String query = searchField.getText().toLowerCase().trim();
        // Load all multisell files from multisellPath and customNpcPath/**/ItemList/*.xml
        List<File> allFiles = new ArrayList<>();
        allFiles.addAll(findXmlFiles(multisellPath));
        allFiles.addAll(findCustomItemListXmls(customNpcPath));
        // Parse each XML for <npcs> and comments
        Map<String, List<NpcInfo>> fileToNpcs = new HashMap<>();
        for (File f : allFiles) {
            List<NpcInfo> npcs = parseNpcsFromXml(f);
            fileToNpcs.put(f.getAbsolutePath(), npcs); // Use absolute path to avoid duplicates
        }
        // Reverse mapping: NPC -> linked files
        Map<Integer, List<File>> npcToFiles = new HashMap<>();
        Map<Integer, String> npcToComment = new HashMap<>();
        for (Map.Entry<String, List<NpcInfo>> entry : fileToNpcs.entrySet()) {
            File f = new File(entry.getKey());
            for (NpcInfo info : entry.getValue()) {
                npcToFiles.computeIfAbsent(info.id, _ -> new ArrayList<>()).add(f);
                if (info.comment != null) npcToComment.put(info.id, info.comment);
            }
        }
        // Dynamic categories based on comments
        Map<String, DefaultMutableTreeNode> categoryGroups = new TreeMap<>();
        DefaultMutableTreeNode unassignedNode = new DefaultMutableTreeNode("Unassigned Shops");
        // Add NPCs to categories
        for (Map.Entry<Integer, List<File>> entry : npcToFiles.entrySet()) {
            int npcId = entry.getKey();
            List<File> files = entry.getValue();
            String comment = npcToComment.getOrDefault(npcId, "NPC");
            // Filter by query
            if (!query.isEmpty() &&
                !comment.toLowerCase().contains(query) &&
                !String.valueOf(npcId).contains(query)) {
                continue;
            }
            // Clean comment for display
            String cleanComment = comment;
            String lower = comment.toLowerCase();
            if (lower.contains("gatekeeper")) cleanComment = cleanComment.replaceAll("(?i)gatekeeper", "").trim();
            if (lower.contains("teleporter")) cleanComment = cleanComment.replaceAll("(?i)teleporter", "").trim();
            if (lower.contains("teleport")) cleanComment = cleanComment.replaceAll("(?i)teleport", "").trim();
            if (cleanComment.isEmpty()) cleanComment = "NPC";
            // Determine category
            String category = determineCategoryFromComment(comment, npcId, files);
            // Create NPC node
            DefaultMutableTreeNode npcNode = new DefaultMutableTreeNode(cleanComment + " [" + npcId + "]");
            // Add linked shops
            for (File f : files) {
                allFiles.remove(f);
                String nodeDisplay = f.getName();
                ShopFileNode shopNode = new ShopFileNode(f, nodeDisplay);
                npcNode.add(new DefaultMutableTreeNode(shopNode));
            }
            // Add to category if has children
            if (npcNode.getChildCount() > 0) {
                categoryGroups.computeIfAbsent(category, k -> new DefaultMutableTreeNode(k)).add(npcNode);
            }
        }
        // Add unassigned shops
        for (File f : allFiles) {
            String nodeDisplay = f.getName();
            ShopFileNode shopNode = new ShopFileNode(f, nodeDisplay);
            unassignedNode.add(new DefaultMutableTreeNode(shopNode));
        }
        // Add groups to root
        categoryGroups.values().forEach(rootNode::add);
        if (unassignedNode.getChildCount() > 0) rootNode.add(unassignedNode);
        treeModel.reload();
        // Κλείνουμε όλα στην αρχή
        collapseAllTrees();
    }

    // Find all .xml in a directory
    private List<File> findXmlFiles(String path) {
        List<File> files = new ArrayList<>();
        File dir = new File(path);
        if (dir.exists()) {
            File[] xmlFiles = dir.listFiles((_, name) -> name.endsWith(".xml"));
            if (xmlFiles != null) files.addAll(List.of(xmlFiles));
        }
        return files;
    }

    // Find all .xml in CustomNpc's/**/ItemList/
    private List<File> findCustomItemListXmls(String customPath) {
        List<File> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(Paths.get(customPath))) {
            walk.filter(p -> p.toString().endsWith(".xml") && p.toString().contains(File.separator + "ItemList" + File.separator))
                .forEach(p -> files.add(p.toFile()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return files;
    }

    // Parse <npcs> from XML
    private List<NpcInfo> parseNpcsFromXml(File file) {
        List<NpcInfo> npcs = new ArrayList<>();
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();
            NodeList npcsList = doc.getElementsByTagName("npcs");
            if (npcsList.getLength() > 0) {
                Element npcsElement = (Element) npcsList.item(0);
                Node child = npcsElement.getFirstChild();
                while (child != null) {
                    if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals("npc")) {
                        String idText = child.getTextContent().trim();
                        int id = Integer.parseInt(idText);
                        String comment = findCommentForNpc(child);
                        npcs.add(new NpcInfo(id, comment));
                    }
                    child = child.getNextSibling();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return npcs;
    }

    // Helper to find comment after <npc>
    private String findCommentForNpc(Node npcNode) {
        Node next = npcNode.getNextSibling();
        while (next != null) {
            if (next.getNodeType() == Node.COMMENT_NODE) {
                return next.getTextContent().trim();
            }
            if (next.getNodeType() == Node.ELEMENT_NODE) {
                break; // Stop if another element is found
            }
            next = next.getNextSibling();
        }
        return null;
    }

    // Determine category from comment, ID, or file path
    private String determineCategoryFromComment(String comment, int npcId, List<File> files) {
        if (npcId >= 30000 || files.stream().anyMatch(f -> f.getAbsolutePath().contains("CustomNpc's"))) {
            return "CustomNpc's";
        }
        if (comment == null) return "Others / Custom";
        String lower = comment.toLowerCase();
        if (lower.contains("gatekeeper")) return "Gatekeepers";
        if (lower.contains("merchant") || lower.contains("trader") || lower.contains("grocer")) return "Merchants";
        if (lower.contains("blacksmith")) return "Blacksmiths";
        if (lower.contains("teleporter") || lower.contains("teleport")) return "Teleporters";
        return "Others / Custom";
    }

    // NPC Info class
    private static class NpcInfo {
        int id;
        String comment;
        NpcInfo(int id, String comment) {
            this.id = id;
            this.comment = comment;
        }
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
            // Reload ShopData and refresh tree
            ShopData.loadAll(new File(multisellPath).getParentFile().getParent());
            refreshTree();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
}