package org.newproject.npceditor;

import java.io.File;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

public class NpcWriter {

    public static void saveAllNpcs(List<Npc> npcs, String baseFolderPath) {
        Map<String, List<Npc>> fileGroups = new HashMap<>();
        for (Npc npc : npcs) {
            String filename = getFilenameForId(npc.id);
            fileGroups.computeIfAbsent(filename, _ -> new ArrayList<>()).add(npc);
        }

        for (Map.Entry<String, List<Npc>> entry : fileGroups.entrySet()) {
            saveFile(entry.getKey(), entry.getValue(), baseFolderPath);
        }
    }

    private static void saveFile(String filename, List<Npc> npcs, String path) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            Element root = doc.createElement("list");
            doc.appendChild(root);

            npcs.sort(Comparator.comparingInt(n -> n.id));

            for (Npc npc : npcs) {
                Element n = doc.createElement("npc");
                n.setAttribute("id", String.valueOf(npc.id));
                n.setAttribute("name", npc.name);
                n.setAttribute("type", npc.type);
                n.setAttribute("level", String.valueOf(npc.level));
                if (!npc.title.isEmpty()) n.setAttribute("title", npc.title);

                // STATS (Nested Structure for compatibility)
                Element stats = doc.createElement("stats");
                stats.setAttribute("str", String.valueOf(npc.str));
                stats.setAttribute("int", String.valueOf(npc.intel));
                stats.setAttribute("dex", String.valueOf(npc.dex));
                stats.setAttribute("wit", String.valueOf(npc.wit));
                stats.setAttribute("con", String.valueOf(npc.con));
                stats.setAttribute("men", String.valueOf(npc.men));
                
                Element vitals = doc.createElement("vitals");
                vitals.setAttribute("hp", String.valueOf(npc.hp));
                vitals.setAttribute("mp", String.valueOf(npc.mp));
                stats.appendChild(vitals);
                
                Element attack = doc.createElement("attack");
                attack.setAttribute("physical", String.valueOf(npc.pAtk));
                attack.setAttribute("magical", String.valueOf(npc.mAtk));
                stats.appendChild(attack);

                Element defence = doc.createElement("defence");
                defence.setAttribute("physical", String.valueOf(npc.pDef));
                defence.setAttribute("magical", String.valueOf(npc.mDef));
                stats.appendChild(defence);
                
                n.appendChild(stats);

                // ACQUIRE
                Element acquire = doc.createElement("acquire");
                acquire.setAttribute("exp", String.valueOf(npc.exp));
                acquire.setAttribute("sp", String.valueOf(npc.sp));
                n.appendChild(acquire);

                // PARAMETERS
                if (!npc.parameters.isEmpty()) {
                    Element params = doc.createElement("parameters");
                    for (Map.Entry<String, String> entry : npc.parameters.entrySet()) {
                        Element p = doc.createElement("param");
                        p.setAttribute("name", entry.getKey());
                        p.setAttribute("value", entry.getValue());
                        params.appendChild(p);
                    }
                    n.appendChild(params);
                }

                // SKILLS
                if (!npc.skills.isEmpty()) {
                    Element skills = doc.createElement("skills");
                    for (Npc.Skill sk : npc.skills) {
                        Element s = doc.createElement("skill");
                        s.setAttribute("id", String.valueOf(sk.id));
                        s.setAttribute("level", String.valueOf(sk.level));
                        skills.appendChild(s);
                    }
                    n.appendChild(skills);
                }

                // DROPS
                if (!npc.dropGroups.isEmpty()) {
                    Element drops = doc.createElement("drops");
                    for (DropGroup g : npc.dropGroups) {
                        Element cat = doc.createElement("category");
                        cat.setAttribute("id", String.valueOf(g.id));
                        cat.setAttribute("chance", String.valueOf(g.chance));
                        for (Npc.Item it : g.items) {
                            Element d = doc.createElement("drop");
                            d.setAttribute("itemid", String.valueOf(it.id));
                            d.setAttribute("min", String.valueOf(it.min));
                            d.setAttribute("max", String.valueOf(it.max));
                            d.setAttribute("chance", String.valueOf(it.chance));
                            cat.appendChild(d);
                        }
                        drops.appendChild(cat);
                    }
                    n.appendChild(drops);
                }

                // SPOILS
                if (!npc.spoilItems.isEmpty()) {
                    Element spoils = doc.createElement("spoils");
                    for (Npc.Item it : npc.spoilItems) {
                        Element s = doc.createElement("spoil");
                        s.setAttribute("itemid", String.valueOf(it.id));
                        s.setAttribute("min", String.valueOf(it.min));
                        s.setAttribute("max", String.valueOf(it.max));
                        s.setAttribute("chance", String.valueOf(it.chance));
                        spoils.appendChild(s);
                    }
                    n.appendChild(spoils);
                }

                root.appendChild(n);
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            File saveDir = new File(path);
            if (!saveDir.exists()) saveDir.mkdirs();
            
            File finalFile = new File(saveDir, filename);
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(finalFile);
            transformer.transform(source, result);

        } catch (Exception e) { e.printStackTrace(); }
    }

    private static String getFilenameForId(int id) {
        int start = (id / 100) * 100;
        int end = start + 99;
        return String.format("%05d-%05d.xml", start, end);
    }
}