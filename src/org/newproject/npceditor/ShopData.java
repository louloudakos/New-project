package org.newproject.npceditor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ShopData {
    private static final Map<Integer, List<File>> npcToMultisells = new HashMap<>();
    private static final Map<Integer, List<File>> npcToBuylists = new HashMap<>();
    private static final Map<String, List<Integer>> fileToNpcIds = new HashMap<>();
    private static final Map<Integer, String> npcCommentCache = new HashMap<>();
    private static final Map<Integer, String> itemTypeCache = new HashMap<>();
    private static final Map<String, String> shopCategoryCache = new HashMap<>();

    public static void loadAll(String dataRoot) {
        System.out.println("ShopData: Indexing started...");
        npcToMultisells.clear();
        npcToBuylists.clear();
        fileToNpcIds.clear();
        npcCommentCache.clear();
        itemTypeCache.clear();
        shopCategoryCache.clear();

        File itemsDir = new File(dataRoot + File.separator + "items");
        if (itemsDir.exists()) scanItemTypes(itemsDir);

        File msDir = new File(dataRoot + File.separator + "shopdata" + File.separator + "multisell");
        if (!msDir.exists()) msDir = new File(dataRoot + File.separator + "multisell");
        if (msDir.exists()) scanDirectory(msDir, true);

        File buyDir = new File(dataRoot + File.separator + "shopdata" + File.separator + "buylists");
        if (!buyDir.exists()) buyDir = new File(dataRoot + File.separator + "buylists");
        if (buyDir.exists()) scanDirectory(buyDir, false);

        System.out.println("ShopData: Indexing complete.");
    }

    private static void scanItemTypes(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) scanItemTypes(f);
            else if (f.getName().endsWith(".xml")) parseItemXml(f);
        }
    }

    private static void parseItemXml(File f) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(f);
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("item");
            for (int i = 0; i < nList.getLength(); i++) {
                Node nNode = nList.item(i);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element e = (Element) nNode;
                    try {
                        int id = Integer.parseInt(e.getAttribute("id"));
                        String type = e.getAttribute("type");
                        if (type != null && !type.isEmpty()) itemTypeCache.put(id, type);
                    } catch (Exception ex) {}
                }
            }
        } catch (Exception e) {}
    }

    private static void scanDirectory(File dir, boolean isMultisell) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                scanDirectory(f, isMultisell);
            } else if (f.getName().endsWith(".xml")) {
                parseShopFile(f, isMultisell);
            }
        }
    }

    private static void parseShopFile(File f, boolean isMultisell) {
        try (Scanner scanner = new Scanner(f)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.contains("<npc>")) {
                    extractNpcFromLine(line, f, isMultisell);
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading file text: " + f.getName());
        }

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(f);
            doc.getDocumentElement().normalize();
            String category = analyzeShopType(doc, isMultisell);
            shopCategoryCache.put(f.getName(), category);
        } catch (Exception e) { }
    }

    private static void extractNpcFromLine(String line, File f, boolean isMultisell) {
        try {
            int startId = line.indexOf("<npc>") + 5;
            int endId = line.indexOf("</npc>");
            if (startId > 4 && endId > startId) {
                String idStr = line.substring(startId, endId).trim();
                int npcId = Integer.parseInt(idStr);

                int commentStart = line.indexOf("<!--", endId);
                String comment = "";
                if (commentStart != -1) {
                    int commentEnd = line.indexOf("-->", commentStart);
                    if (commentEnd != -1) {
                        comment = line.substring(commentStart + 4, commentEnd).trim();
                    }
                }

                if (!comment.isEmpty()) {
                    npcCommentCache.put(npcId, comment);
                }

                if (isMultisell) {
                    npcToMultisells.computeIfAbsent(npcId, _ -> new ArrayList<>()).add(f);
                } else {
                    npcToBuylists.computeIfAbsent(npcId, _ -> new ArrayList<>()).add(f);
                }

                fileToNpcIds.computeIfAbsent(f.getName(), _ -> new ArrayList<>()).add(npcId);
            }
        } catch (Exception ex) {}
    }

    private static String analyzeShopType(Document doc, boolean isMultisell) {
        Set<String> itemTypes = new HashSet<>();
        Set<Integer> ingredientIds = new HashSet<>();
        Element root = doc.getDocumentElement();
        String applyTaxesStr = root.getAttribute("applyTaxes");
        boolean applyTaxes = "true".equalsIgnoreCase(applyTaxesStr);

        NodeList itemList = doc.getElementsByTagName("item");
        for (int i = 0; i < itemList.getLength(); i++) {
            Node itemNode = itemList.item(i);
            if (itemNode.getNodeType() == Node.ELEMENT_NODE) {
                Element itemElem = (Element) itemNode;

                NodeList ingList = itemElem.getElementsByTagName("ingredient");
                for (int j = 0; j < ingList.getLength(); j++) {
                    Node ingNode = ingList.item(j);
                    if (ingNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element ingElem = (Element) ingNode;
                        try {
                            int id = Integer.parseInt(ingElem.getAttribute("id"));
                            ingredientIds.add(id);
                        } catch (Exception e) {}
                    }
                }

                NodeList prodList = itemElem.getElementsByTagName("production");
                for (int j = 0; j < prodList.getLength(); j++) {
                    Node prodNode = prodList.item(j);
                    if (prodNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element prodElem = (Element) prodNode;
                        try {
                            int id = Integer.parseInt(prodElem.getAttribute("id"));
                            String type = itemTypeCache.get(id);
                            if (type != null && !type.isEmpty()) {
                                itemTypes.add(type);
                            }
                        } catch (Exception e) {}
                    }
                }
            }
        }

        String baseCategory;
        if (itemTypes.isEmpty()) {
            baseCategory = "Unknown";
        } else if (itemTypes.size() == 1) {
            baseCategory = itemTypes.iterator().next();
        } else if (itemTypes.contains("Weapon") && itemTypes.size() < 3) {
            baseCategory = "Weapon Shop";
        } else if (itemTypes.contains("Armor") && itemTypes.size() < 3) {
            baseCategory = "Armor Shop";
        } else {
            baseCategory = "Mixed / General";
        }

        String suffix = "";
        if (ingredientIds.contains(57)) {
            suffix = " (Adena)";
        } else if (!ingredientIds.isEmpty()) {
            suffix = " (Exchange)";
        }
        if (applyTaxes) {
            suffix += " (Taxed)";
        }
        if (!isMultisell) {
            baseCategory = "Buylist: " + baseCategory;
        }

        return baseCategory + suffix;
    }

    public static Map<Integer, String> getNpcComments() {
        return npcCommentCache;
    }

    public static List<File> getShopsForNpc(int npcId) {
        List<File> allShops = new ArrayList<>();
        if (npcToMultisells.containsKey(npcId)) allShops.addAll(npcToMultisells.get(npcId));
        if (npcToBuylists.containsKey(npcId)) allShops.addAll(npcToBuylists.get(npcId));
        return allShops;
    }

    public static String getShopCategory(String fileName) {
        return shopCategoryCache.getOrDefault(fileName, "Unknown");
    }

    public static List<Integer> getNpcIdsForFile(String filename) {
        return fileToNpcIds.getOrDefault(filename, new ArrayList<>());
    }

    // ΑΛΛΑΓΗ ΕΔΩ: Καθαρίζουμε επανάληψη λέξεων από το comment
    public static String getShopDisplayInfo(String filename) {
        String comment = npcCommentCache.values().stream()
                .filter(c -> c != null)
                .findFirst()
                .orElse("No comment");

        String lower = comment.toLowerCase();
        if (lower.contains("gatekeeper")) comment = comment.replaceAll("(?i)gatekeeper", "").trim();
        if (lower.contains("teleporter")) comment = comment.replaceAll("(?i)teleporter", "").trim();
        if (comment.isEmpty()) comment = "No comment";

        return comment + " - " + filename + " [" + getShopCategory(filename) + "]";
    }
}