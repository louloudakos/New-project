package org.newproject.npceditor;

public class ItemInfo {
    private int id;
    private String name;
    private String type; // Weapon, Armor, EtcItem
    private String grade; // S, A, B...

    public ItemInfo(int id, String name, String type, String grade) {
        this.id = id;
        this.name = (name == null || name.isEmpty()) ? "Unknown Item" : name;
        this.type = (type == null || type.isEmpty()) ? "EtcItem" : type;
        this.grade = (grade == null || grade.isEmpty()) ? "None" : grade;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getGrade() { return grade; }

    @Override
    public String toString() {
        return name + " (" + id + ")";
    }
}