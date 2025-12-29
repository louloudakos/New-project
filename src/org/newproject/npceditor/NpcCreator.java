package org.newproject.npceditor;

import javax.swing.*;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class NpcCreator {
    public static void createNewNpc(JFrame parent, List<Npc> allNpcs, String dataRoot) {
        // Defaults
        JTextField nameField = new JTextField("New Merchant");
        JTextField titleField = new JTextField("Trader");
        JSpinner levelSpinner = new JSpinner(new SpinnerNumberModel(70, 1, 99, 1));

        // Νέα fields για flexibility
        String[] types = {"Merchant", "L2Npc", "L2Monster", "L2Guard", "L2Teleporter"};
        JComboBox<String> typeCombo = new JComboBox<>(types);
        String[] races = {"DWARF", "HUMAN", "ELF", "DARKELF", "ORC"};
        JComboBox<String> raceCombo = new JComboBox<>(races);
        String[] sexes = {"MALE", "FEMALE"};
        JComboBox<String> sexCombo = new JComboBox<>(sexes);

        // Dialog panel
        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.add(new JLabel("NPC Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Title:"));
        panel.add(titleField);
        panel.add(new JLabel("Level:"));
        panel.add(levelSpinner);
        panel.add(new JLabel("Type:"));
        panel.add(typeCombo);
        panel.add(new JLabel("Race:"));
        panel.add(raceCombo);
        panel.add(new JLabel("Sex:"));
        panel.add(sexCombo);

        int result = JOptionPane.showConfirmDialog(parent, panel, "Create New NPC", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String npcName = nameField.getText().trim();
        String npcTitle = titleField.getText().trim();
        int npcLevel = (Integer) levelSpinner.getValue();
        String npcType = (String) typeCombo.getSelectedItem();
        String npcRace = (String) raceCombo.getSelectedItem();
        String npcSex = (String) sexCombo.getSelectedItem();

        if (npcName.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Το όνομα NPC δεν μπορεί να είναι κενό!");
            return;
        }

        // Auto-generate ID
        int newId = allNpcs.stream()
                .mapToInt(n -> n.id)
                .max()
                .orElse(30000) + 1;

        System.out.println("NpcCreator: DATA_ROOT που πήρα = " + dataRoot);
        System.out.println("NpcCreator: New NPC ID = " + newId);

        // Custom root folder
        File customRoot = new File(dataRoot, "CustomNpc's");
        System.out.println("NpcCreator: Trying CustomNpc's folder: " + customRoot.getAbsolutePath());
        if (!customRoot.exists()) {
            boolean created = customRoot.mkdirs();
            System.out.println("NpcCreator: CustomNpc's created = " + created);
            if (!created) {
                JOptionPane.showMessageDialog(parent, "Αποτυχία δημιουργίας CustomNpc's!\nPath: " + customRoot.getAbsolutePath());
                return;
            }
        }

        // Main folder with type
        String folderName = npcType + "_" + String.format("%04d", newId - 30000 + 1);
        File mainFolder = new File(customRoot, folderName);
        System.out.println("NpcCreator: Trying main folder: " + mainFolder.getAbsolutePath());
        if (!mainFolder.exists()) {
            boolean created = mainFolder.mkdirs();
            System.out.println("NpcCreator: Main folder created = " + created);
            if (!created) {
                JOptionPane.showMessageDialog(parent, "Αποτυχία δημιουργίας folder!\nPath: " + mainFolder.getAbsolutePath());
                return;
            }
        }

        // Subfolders
        File htmlFolder = new File(mainFolder, "Html");
        File merchantFolder = new File(mainFolder, "Merchant");
        File itemListFolder = new File(mainFolder, "ItemList");
        htmlFolder.mkdirs();
        merchantFolder.mkdirs();
        itemListFolder.mkdirs();
        System.out.println("NpcCreator: Subfolders created at " + mainFolder.getAbsolutePath());

        try {
            // 1. NPC XML - Υπολόγισε stats βασισμένα σε level (απλή formula για παράδειγμα)
            double baseHp = npcLevel * 35 + 100;
            double baseMp = npcLevel * 20 + 50;
            double basePAtk = npcLevel * 10 + 50;
            double baseMAtk = npcLevel * 7 + 30;
            double basePDef = npcLevel * 4 + 100;
            double baseMDef = npcLevel * 3 + 80;

            File npcFile = new File(merchantFolder, newId + ".xml");
            try (FileWriter fw = new FileWriter(npcFile)) {
                fw.write("<!-- " + npcName + " -->\n");  // Σχόλιο με name δίπλα στο ID (ήδη υπάρχει στο NpcWriter, αλλά προσθέτω εδώ για custom
                fw.write("<npc id=\"" + newId + "\" level=\"" + npcLevel + "\" type=\"" + npcType + "\" name=\"" + npcName + "\" title=\"" + npcTitle + "\">\n");
                fw.write(" <parameters>\n");
                fw.write(" <param name=\"MoveAroundSocial\" value=\"0\" />\n");
                fw.write(" <param name=\"MoveAroundSocial1\" value=\"140\" />\n");
                fw.write(" </parameters>\n");
                fw.write(" <race>" + npcRace + "</race>\n");
                fw.write(" <sex>" + npcSex + "</sex>\n");
                fw.write(" <stats str=\"40\" int=\"21\" dex=\"30\" wit=\"20\" con=\"43\" men=\"10\">\n");
                fw.write(" <vitals hp=\"" + baseHp + "\" hpRegen=\"7.5\" mp=\"" + baseMp + "\" mpRegen=\"2.7\" />\n");
                fw.write(" <attack physical=\"" + basePAtk + "\" magical=\"" + baseMAtk + "\" random=\"30\" critical=\"4\" accuracy=\"5\" attackSpeed=\"253\" type=\"SWORD\" range=\"40\" distance=\"80\" width=\"120\" />\n");
                fw.write(" <defence physical=\"" + basePDef + "\" magical=\"" + baseMDef + "\" />\n");
                fw.write(" <speed>\n");
                fw.write(" <walk ground=\"50\" />\n");
                fw.write(" <run ground=\"120\" />\n");
                fw.write(" </speed>\n");
                fw.write(" </stats>\n");
                fw.write(" <status attackable=\"false\" />\n");
                fw.write(" <skillList>\n");
                fw.write(" <skill id=\"4045\" level=\"1\" /> <!-- Resist Full Magic Attack -->\n");
                fw.write(" <skill id=\"4416\" level=\"18\" /> <!-- Dwarves -->\n");
                fw.write(" </skillList>\n");
                fw.write(" <exCrtEffect>true</exCrtEffect>\n");
                fw.write(" <ai aggroRange=\"1000\" clanHelpRange=\"300\" isAggressive=\"false\" />\n");
                fw.write(" <collision>\n");
                fw.write(" <radius normal=\"8\" />\n");
                fw.write(" <height normal=\"17\" />\n");
                fw.write(" </collision>\n");
                fw.write("</npc>");
            }

            // 2. HTML - Template με placeholders
            File htmlFile = new File(htmlFolder, newId + ".htm");
            try (FileWriter fw = new FileWriter(htmlFile)) {
                fw.write("Hello, I am " + npcName + ", the " + npcTitle + ". May I give you a piece of advice? The most expensive equipment is not always the best.<br>\n");
                fw.write("<a action=\"bypass -h npc_%objectId%_Buy 3000100\">Buy Fighter equipment.</a><br>\n");
                fw.write("<a action=\"bypass -h npc_%objectId%_Buy 3000101\">Buy Mystic equipment.</a><br>\n");
                fw.write("<a action=\"bypass -h npc_%objectId%_exc_multisell 003\">Exchange equipment.</a><br>\n");
                fw.write("<a action=\"bypass -h npc_%objectId%_Wear 3000100\">Wear Fighter equipment.</a><br>\n");
                fw.write("<a action=\"bypass -h npc_%objectId%_Wear 3000101\">Wear Mystic equipment.</a><br>\n");
                fw.write("<a action=\"bypass -h npc_%objectId%_Sell\">Sell.</a><br>\n");
                fw.write("<a action=\"bypass npc_%objectId%_TerritoryStatus\">See the Lord and get the tax rate information.</a><br>\n");
                fw.write("<a action=\"bypass npc_%objectId%_Quest\">Quest</a>\n");
                fw.write("</body></html>");
            }

            // 3. Multisell XML - Basic/empty
            File multisellFile = new File(itemListFolder, newId + ".xml");
            try (FileWriter fw = new FileWriter(multisellFile)) {
                fw.write("<list>\n");
                fw.write(" <npcs>\n");
                fw.write(" <npc>" + newId + "</npc>\n");
                fw.write(" </npcs>\n");
                fw.write(" <!-- Προσθέστε items εδώ -->\n");
                fw.write(" <item>\n");
                fw.write(" <ingredient id=\"57\" count=\"2600\" /> <!-- Adena -->\n");
                fw.write(" <production id=\"1458\" count=\"1\" /> <!-- Crystal (D-grade) -->\n");
                fw.write(" </item>\n");
                fw.write("</list>");
            }

            // Δημιουργία Npc object και προσθήκη σε allNpcs (για GUI update)
            Npc newNpc = new Npc(newId, npcName, npcType, npcLevel);
            newNpc.title = npcTitle;
            newNpc.hp = baseHp;
            newNpc.mp = baseMp;
            newNpc.pAtk = basePAtk;
            newNpc.mAtk = baseMAtk;
            newNpc.pDef = basePDef;
            newNpc.mDef = baseMDef;
            allNpcs.add(newNpc);
            ((NpcEditor) parent).addDirtyNpc(newNpc);
            ((NpcEditor) parent).refreshData();  // Full reload για να διαβάσει νέα XML

            // Reload shops
            ShopData.loadAll(dataRoot);

            JOptionPane.showMessageDialog(parent,
                    "Επιτυχής δημιουργία!\n\n" +
                    "Φάκελος: " + mainFolder.getAbsolutePath() + "\n" +
                    " ├── Html/" + htmlFile.getName() + "\n" +
                    " ├── Merchant/" + npcFile.getName() + "\n" +
                    " └── ItemList/" + multisellFile.getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "Σφάλμα: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}