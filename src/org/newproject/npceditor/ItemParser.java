package org.newproject.npceditor;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ItemParser {

    // Record για εύκολη μεταφορά δεδομένων (Java 14+)
    public record ItemInfo(int id, String name, String type, String subType, String grade) {}

    public static List<ItemInfo> loadItems(String rootPath) {
        List<ItemInfo> items = new ArrayList<>();
        File itemsDir = new File(rootPath); 

        if (!itemsDir.exists()) {
            System.err.println("Item folder not found at: " + rootPath);
            return items;
        }

        parseDirectory(itemsDir, items);
        System.out.println("ItemParser: Loaded " + items.size() + " items total.");
        return items;
    }

    private static void parseDirectory(File dir, List<ItemInfo> items) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory()) {
                parseDirectory(f, items);
            } else if (f.getName().endsWith(".xml")) {
                parseFile(f, items);
            }
        }
    }

    private static void parseFile(File xmlFile, List<ItemInfo> items) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("item");

            for (int i = 0; i < nList.getLength(); i++) {
                Node nNode = nList.item(i);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element) nNode;

                    try {
                        int id = Integer.parseInt(el.getAttribute("id"));
                        String name = el.getAttribute("name");
                        String type = el.getAttribute("type"); 

                        String subType = "Other";
                        String grade = "No Grade";
                        boolean isQuest = false;

                        NodeList sets = el.getElementsByTagName("set");
                        for (int j = 0; j < sets.getLength(); j++) {
                            Element s = (Element) sets.item(j);
                            String paramName = s.getAttribute("name");
                            String val = s.getAttribute("val");

                            switch (paramName) {
                                case "weapon_type": subType = val; break;
                                case "crystal_type":
                                    if (val.equalsIgnoreCase("none")) grade = "No Grade";
                                    else grade = val.toUpperCase() + " Grade";
                                    break;
                                case "bodypart":
                                    if (type.equalsIgnoreCase("Armor") || isArmorBodyPart(val)) {
                                        subType = mapBodyPart(val);
                                    }
                                    break;
                                case "etcitem_type": subType = mapEtcType(val); break;
                                case "is_questitem": 
                                    if (val.equalsIgnoreCase("true")) isQuest = true; 
                                    break;
                            }
                        }

                        // Fix Names formatting
                        if (name.contains(" - ")) {
                            name = name.replace(" - ", " (") + ")";
                        } else if (name.contains("*")) {
                            String[] parts = name.split("\\*");
                            if (parts.length > 1) {
                                name = parts[0] + " (" + formatText(parts[1]) + ")";
                            } else {
                                name = parts[0];
                            }
                        }
                        
                        // --- OVERRIDES ---
                        String lowerName = name.toLowerCase();
                        
                        if (lowerName.contains("soulshot") || lowerName.contains("spiritshot")) {
                            subType = "Shots";
                        }
                        else if (lowerName.contains("life stone")) {
                            subType = "Life Stones";
                        }
                        else if (lowerName.contains("enchant scroll") || lowerName.contains("enchant weapon") || lowerName.contains("enchant armor")) {
                            subType = "Enchant Scrolls";
                        }
                        else if (isQuest) {
                            subType = "Quest";
                        }

                        // Προσθήκη στη λίστα
                        items.add(new ItemInfo(id, name, type, formatText(subType), grade));

                    } catch (Exception e) {
                        System.err.println("Skipped item in " + xmlFile.getName() + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String formatText(String input) {
        if (input == null) return "";
        if (input.length() <= 1) return input.toUpperCase();
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase().replace("_", " ");
    }
    
    private static boolean isArmorBodyPart(String val) {
        val = val.toLowerCase();
        return val.contains("chest") || val.contains("full") || val.contains("onepiece") || 
               val.contains("legs") || val.contains("gaiters") || val.contains("gloves") || 
               val.contains("feet") || val.contains("head");
    }

    private static String mapBodyPart(String val) {
        val = val.toLowerCase();
        if (val.contains("chest") || val.contains("fullarmor") || val.contains("onepiece") || val.contains("alldress")) return "Chest/Full";
        if (val.contains("legs") || val.contains("gaiters")) return "Legs";
        if (val.contains("head")) return "Helmet";
        if (val.contains("gloves")) return "Gloves";
        if (val.contains("feet")) return "Boots";
        if (val.contains("face") || val.contains("hair")) return "Accessory";
        if (val.contains("rear") || val.contains("lear") || val.contains("neck") || val.contains("finger")) return "Jewels";
        if (val.contains("lhand")) return "Shield/Sigil";
        return "Other";
    }

    private static String mapEtcType(String val) {
        val = val.toUpperCase();
        if (val.equals("SCROLL")) return "Scrolls";
        if (val.equals("POTION") || val.equals("ELIXIR")) return "Potions";
        if (val.equals("ARROW") || val.equals("BOLT")) return "Arrows";
        if (val.equals("MATERIAL")) return "Materials";
        if (val.equals("RECIPE")) return "Recipes";
        if (val.equals("DYE")) return "Dyes";
        if (val.equals("LURE")) return "Fishing";
        if (val.equals("SHOT") || val.contains("SOULSHOT") || val.contains("SPIRITSHOT")) return "Shots";
        return "Other";
    }
}