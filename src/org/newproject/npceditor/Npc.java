package org.newproject.npceditor;

import java.util.*;

public class Npc {
    public int id;
    public int level;
    public String name;
    public String type;
    public String title = "";

    // Stats
    public long exp, sp;
    public int str, dex, con, intel, wit, men;
    public double hp, mp, pAtk, mAtk, pDef, mDef;
    public int pSpd;
    
    // Elemental
    public int fire, water, wind, earth, holy, dark;

    // Lists
    public List<DropGroup> dropGroups = new ArrayList<>(); // Λίστα με Drop Groups
    public List<Item> spoilItems = new ArrayList<>();      // Λίστα με Spoils (συνήθως χύμα)
    public List<Skill> skills = new ArrayList<>();
    public Map<String, String> parameters = new HashMap<>();

    // --- Inner Classes ---
    public static class Skill {
        public int id;
        public int level;
        public String description = "";

        public Skill(int id, int level, String description) {
            this.id = id;
            this.level = level;
            this.description = (description != null) ? description : "";
        }
    }

    public static class Item {
        public int id;
        public int min;
        public int max;
        public double chance;
        public String description;

        public Item(int id, int min, int max, double chance, String description) {
            this.id = id;
            this.min = min;
            this.max = max;
            this.chance = chance;
            this.description = (description != null) ? description : "";
        }
        
        // Empty constructor for "Add" button
        public Item() { this(57, 1, 1, 100.0, "New Item"); }
    }

    public Npc(int id, String name, String type, int level) {
        this.id = id;
        this.name = (name != null) ? name : "";
        this.type = (type != null) ? type : "L2Npc";
        this.level = level;
    }

    public void addDropGroup(DropGroup group) { this.dropGroups.add(group); }
    public void addSpoilItem(Item item) { this.spoilItems.add(item); }
    public void addSkill(int id, int level, String desc) { this.skills.add(new Skill(id, level, desc)); }

    @Override 
    public String toString() { 
        return "[" + id + "] " + name + " (Lv: " + level + ")"; 
    }
}

