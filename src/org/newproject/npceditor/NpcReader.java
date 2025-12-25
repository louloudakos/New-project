package org.newproject.npceditor;

import java.io.File;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public class NpcReader {

    public static List<Npc> loadNpcs(String folderPath) {
        System.out.println("--- Loading NPCs from: " + folderPath + " ---");
        Map<Integer, Npc> npcMap = new HashMap<>();
        try {
            File folder = new File(folderPath);
            if (!folder.exists()) {
                System.out.println("ERROR: Folder not found!");
                return new ArrayList<>();
            }
            processFolder(folder, npcMap);
        } catch (Exception e) { e.printStackTrace(); }
        
        List<Npc> list = new ArrayList<>(npcMap.values());
        list.sort(Comparator.comparingInt(n -> n.id));
        System.out.println("--- Total Loaded: " + list.size() + " ---");
        return list;
    }

    private static void processFolder(File folder, Map<Integer, Npc> npcMap) {
        File[] files = folder.listFiles();
        if (files == null) return;

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setIgnoringElementContentWhitespace(true);

        for (File file : files) {
            if (file.isDirectory()) {
                processFolder(file, npcMap);
            } else if (file.getName().endsWith(".xml")) {
                parseFile(file, npcMap, dbFactory);
            }
        }
    }

    private static void parseFile(File file, Map<Integer, Npc> npcMap, DocumentBuilderFactory dbFactory) {
        try {
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();

            NodeList npcNodes = doc.getElementsByTagName("npc");

            for (int i = 0; i < npcNodes.getLength(); i++) {
                Node node = npcNodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) continue;
                Element el = (Element) node;

                int id = parseInt(el.getAttribute("id"), 0);
                String name = el.getAttribute("name");
                String type = el.getAttribute("type");
                int level = parseInt(el.getAttribute("level"), 1);

                Npc npc = new Npc(id, name, type, level);
                if (el.hasAttribute("title")) npc.title = el.getAttribute("title");

                NodeList children = el.getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    if (children.item(j).getNodeType() != Node.ELEMENT_NODE) continue;
                    Element childEl = (Element) children.item(j);
                    String tagName = childEl.getTagName(); 

                    if (tagName.equalsIgnoreCase("stats")) parseStats(npc, childEl);
                    else if (tagName.equalsIgnoreCase("parameters")) parseParams(npc, childEl);
                    else if (tagName.equalsIgnoreCase("acquire")) {
                        npc.exp = parseLong(childEl.getAttribute("exp"), 0);
                        npc.sp = parseLong(childEl.getAttribute("sp"), 0);
                    } 
                    else if (tagName.toLowerCase().contains("skill")) parseSkills(npc, childEl);
                    
                    // --- DROPS WRAPPER DETECT ---
                    else if (tagName.equalsIgnoreCase("droplists")) {
                        NodeList dropChildren = childEl.getChildNodes();
                        for (int k = 0; k < dropChildren.getLength(); k++) {
                            if (dropChildren.item(k).getNodeType() != Node.ELEMENT_NODE) continue;
                            Element dEl = (Element) dropChildren.item(k);
                            
                            if (dEl.getTagName().equalsIgnoreCase("drop")) {
                                parseDropsAggressive(npc, dEl);
                            } else if (dEl.getTagName().equalsIgnoreCase("spoil")) {
                                parseSpoilsAggressive(npc, dEl);
                            }
                        }
                    }
                    // --- DIRECT DROPS SUPPORT ---
                    else if (tagName.equalsIgnoreCase("drops") || tagName.equalsIgnoreCase("drop")) {
                        parseDropsAggressive(npc, childEl);
                    } 
                    else if (tagName.equalsIgnoreCase("spoils") || tagName.equalsIgnoreCase("spoil")) {
                        parseSpoilsAggressive(npc, childEl);
                    }
                }
                
                npcMap.put(npc.id, npc);
            }
        } catch (Exception e) {
            System.err.println("Error parsing " + file.getName() + ": " + e.getMessage());
        }
    }

    private static void parseDropsAggressive(Npc npc, Element parent) {
        NodeList children = parent.getChildNodes();
        boolean foundGroup = false;
        int autoId = 1;

        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() != Node.ELEMENT_NODE) continue;
            Element el = (Element) children.item(i);
            String tag = el.getTagName();

            if (tag.equalsIgnoreCase("group") || tag.equalsIgnoreCase("category")) {
                foundGroup = true;
                int gid = parseInt(el.getAttribute("id"), autoId++);
                double chance = parseDouble(el.getAttribute("chance"), 100.0);
                
                DropGroup grp = new DropGroup(gid, false);
                grp.chance = chance;
                
                // Read items inside group
                NodeList items = el.getChildNodes();
                for(int k=0; k<items.getLength(); k++) {
                    if(items.item(k).getNodeType() == Node.ELEMENT_NODE) {
                        Element itemEl = (Element) items.item(k);
                        if (isItemTag(itemEl.getTagName())) {
                            Npc.Item it = parseItem(itemEl);
                            if(it.id != 0) grp.addItem(it);
                        }
                    }
                }
                npc.addDropGroup(grp);
            }
        }

        if (!foundGroup) {
            DropGroup defaultGroup = new DropGroup(1, false);
            defaultGroup.chance = 100.0; 
            
            List<Element> items = new ArrayList<>();
            collectItemsRecursive(parent, items);
            
            for(Element el : items) {
                Npc.Item it = parseItem(el);
                if(it.id != 0) defaultGroup.addItem(it);
            }
            
            if(!defaultGroup.items.isEmpty()) {
                npc.addDropGroup(defaultGroup);
            }
        }
    }

    private static void parseSpoilsAggressive(Npc npc, Element parent) {
        List<Element> items = new ArrayList<>();
        collectItemsRecursive(parent, items);
        for(Element el : items) {
            Npc.Item it = parseItem(el);
            if(it.id != 0) npc.addSpoilItem(it);
        }
    }

    private static void collectItemsRecursive(Element parent, List<Element> result) {
        NodeList children = parent.getChildNodes();
        for(int i=0; i<children.getLength(); i++) {
            if(children.item(i).getNodeType() != Node.ELEMENT_NODE) continue;
            Element el = (Element) children.item(i);
            
            if (isItemTag(el.getTagName())) {
                result.add(el);
            } else {
                collectItemsRecursive(el, result); 
            }
        }
    }

    private static boolean isItemTag(String tag) {
        return tag.equalsIgnoreCase("item") || tag.equalsIgnoreCase("drop") || tag.equalsIgnoreCase("reward");
    }

    private static Npc.Item parseItem(Element el) {
        int id = 0;
        if (el.hasAttribute("itemid")) id = parseInt(el.getAttribute("itemid"), 0);
        else if (el.hasAttribute("itemId")) id = parseInt(el.getAttribute("itemId"), 0);
        else if (el.hasAttribute("id")) id = parseInt(el.getAttribute("id"), 0);

        int min = 1, max = 1;
        if (el.hasAttribute("min")) min = parseInt(el.getAttribute("min"), 1);
        if (el.hasAttribute("max")) max = parseInt(el.getAttribute("max"), 1);
        if (el.hasAttribute("count")) { 
            min = parseInt(el.getAttribute("count"), 1); 
            max = min;
        }

        double chance = 0;
        if (el.hasAttribute("chance")) chance = parseDouble(el.getAttribute("chance"), 0);
        else if (el.hasAttribute("rate")) chance = parseDouble(el.getAttribute("rate"), 0);

        return new Npc.Item(id, min, max, chance, "");
    }

    private static void parseStats(Npc npc, Element el) {
        npc.str = parseInt(el.getAttribute("str"), 0);
        npc.dex = parseInt(el.getAttribute("dex"), 0);
        npc.con = parseInt(el.getAttribute("con"), 0);
        npc.intel = parseInt(el.getAttribute("int"), 0);
        npc.wit = parseInt(el.getAttribute("wit"), 0);
        npc.men = parseInt(el.getAttribute("men"), 0);
        NodeList c = el.getChildNodes();
        for(int i=0; i<c.getLength(); i++) {
            if(c.item(i).getNodeType()!=Node.ELEMENT_NODE) continue;
            Element s = (Element)c.item(i);
            String n = s.getTagName().toLowerCase();
            if(n.equals("vitals")) { npc.hp = parseDouble(s.getAttribute("hp"),0); npc.mp = parseDouble(s.getAttribute("mp"),0); }
            if(n.equals("attack")) { npc.pAtk = parseDouble(s.getAttribute("physical"),0); npc.mAtk = parseDouble(s.getAttribute("magical"),0); }
            if(n.equals("defence")) { npc.pDef = parseDouble(s.getAttribute("physical"),0); npc.mDef = parseDouble(s.getAttribute("magical"),0); }
            if(n.equals("speed")) { 
                NodeList r = s.getElementsByTagName("run"); 
                if(r.getLength()>0) npc.pSpd = parseInt(((Element)r.item(0)).getAttribute("ground"), 0); 
            }
        }
    }

    private static void parseParams(Npc npc, Element el) {
        NodeList p = el.getElementsByTagName("param");
        for(int i=0; i<p.getLength(); i++) {
            Element e = (Element)p.item(i);
            npc.parameters.put(e.getAttribute("name"), e.getAttribute("value"));
        }
    }

    private static void parseSkills(Npc npc, Element el) {
        NodeList s = el.getElementsByTagName("skill");
        for(int i=0; i<s.getLength(); i++) {
            Element e = (Element)s.item(i);
            npc.addSkill(parseInt(e.getAttribute("id"),0), parseInt(e.getAttribute("level"),1), "");
        }
    }

    private static int parseInt(String s, int def) { 
        if(s == null || s.trim().isEmpty()) return def;
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; } 
    }
    private static long parseLong(String s, long def) { 
        if(s == null || s.trim().isEmpty()) return def;
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return def; } 
    }
    private static double parseDouble(String s, double def) { 
        if(s == null || s.trim().isEmpty()) return def;
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return def; } 
    }
}