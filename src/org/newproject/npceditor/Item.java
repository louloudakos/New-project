package org.newproject.npceditor;

public class Item {

    public int id;
    public int min;
    public int max;
    public double chance;
    public String description;

    public Item() {
        this.id = 0;
        this.min = 0;
        this.max = 0;
        this.chance = 0.0;
        this.description = "";
    }

    public Item(int id, int min, int max, double chance, String description) {
        this.id = id;
        this.min = min;
        this.max = max;
        this.chance = chance;
        this.description = description != null ? description : "";
    }

    @Override
    public String toString() {
        return "Item{" +
                "id=" + id +
                ", min=" + min +
                ", max=" + max +
                ", chance=" + chance +
                ", description='" + description + '\'' +
                '}';
    }
}
