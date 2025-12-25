package org.newproject.npceditor;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class GameData {

    // Maps για συμβατότητα
    public static Map<Integer, String> itemNameMap = new HashMap<>();
    public static Map<Integer, String> skillNameMap = new HashMap<>();
    public static Map<Integer, String> skillDescMap = new HashMap<>();

    // --- ΝΕΟ MAP ΓΙΑ ΤΟ ITEM BROWSER & SHOP ---
    // Αυτό έλειπε και χτύπαγε error το ItemBrowserPanel
    public static Map<Integer, ItemParser.ItemInfo> itemInfoMap = new HashMap<>();

    public static void loadAll(String rootPath) {
        System.out.println("GameData: Loading started...");
        loadItems(rootPath + File.separator + "items");
        loadSkills(rootPath + File.separator + "skills");
    }

    public static void loadItems(String path) {
        itemNameMap.clear();
        itemInfoMap.clear();

        // 1. Καλούμε τον Parser
        List<ItemParser.ItemInfo> parsedItems = ItemParser.loadItems(path);

        // 2. Αποθήκευση στο itemInfoMap
        for (ItemParser.ItemInfo info : parsedItems) {
            itemInfoMap.put(info.id(), info);
            
            // Ενημέρωση και του απλού map ονομάτων
            itemNameMap.put(info.id(), info.name());
        }
        
        System.out.println("GameData: Loaded " + itemInfoMap.size() + " items successfully.");
    }

    public static void loadSkills(String path) {
        skillNameMap.clear();
        skillDescMap.clear();
        processFolder(new File(path), "skill", skillNameMap, skillDescMap);
    }

    private static void processFolder(File folder, String tagName, Map<Integer, String> nameMap, Map<Integer, String> descMap) {
        if (!folder.exists()) return;
        File[] files = folder.listFiles();
        if (files == null) return;

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        for (File file : files) {
            if (file.isDirectory()) processFolder(file, tagName, nameMap, descMap);
            else if (file.getName().endsWith(".xml")) parseXml(file, dbFactory, tagName, nameMap, descMap);
        }
    }

    private static void parseXml(File file, DocumentBuilderFactory dbFactory, String tagName, Map<Integer, String> nameMap, Map<Integer, String> descMap) {
        try {
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();

            NodeList list = doc.getElementsByTagName(tagName);
            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element) node;
                    try {
                        int id = Integer.parseInt(el.getAttribute("id"));
                        if (el.hasAttribute("name")) nameMap.put(id, el.getAttribute("name"));
                        
                        if (descMap != null) {
                            if (el.hasAttribute("desc")) descMap.put(id, el.getAttribute("desc"));
                            else if (el.hasAttribute("description")) descMap.put(id, el.getAttribute("description"));
                        }
                    } catch (Exception e) {}
                }
            }
        } catch (Exception e) { }
    }
    
    // --- GETTERS ---

    public static String getItemName(int id) { 
        return itemNameMap.getOrDefault(id, "Unknown Item (" + id + ")"); 
    }
    
    // Επιστρέφει το ItemInfo Record (διορθώνει το error στο NpcShopPanel)
    public static ItemParser.ItemInfo getItem(int id) { 
        return itemInfoMap.get(id); 
    }

    public static String getSkillName(int id) { return skillNameMap.getOrDefault(id, "Unknown Skill"); }
    public static String getSkillDesc(int id) { return skillDescMap.getOrDefault(id, ""); }
}