package org.newproject.npceditor;

import java.util.ArrayList;
import java.util.List;

public class DropGroup {
    public int id;
    public boolean isSpoil;
    public double chance = 100.0; // Η πιθανότητα να πέσει κάτι από αυτό το group
    public List<Npc.Item> items;

    public DropGroup(int id, boolean isSpoil) {
        this.id = id;
        this.isSpoil = isSpoil;
        this.items = new ArrayList<>();
    }

    public void addItem(Npc.Item item) {
        this.items.add(item);
    }
}