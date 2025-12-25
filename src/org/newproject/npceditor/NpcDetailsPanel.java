package org.newproject.npceditor;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class NpcDetailsPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private Map<String, JTextField> fieldMap = new HashMap<>();
    private List<ItemFields> dropFieldList = new ArrayList<>();
    private List<ItemFields> spoilFieldList = new ArrayList<>();
    private List<GroupFields> groupFieldList = new ArrayList<>();
    private List<SkillFields> skillFieldList = new ArrayList<>();
    
    private Set<Npc> dirtyNpcs;
    private String dataRoot;
    private Npc currentNpc;

    private record ItemFields(JTextField id, JTextField min, JTextField max, JTextField chance, Npc.Item originalItem, JLabel nameLabel) {}
    private record SkillFields(JTextField id, JTextField level, Npc.Skill originalSkill, JLabel nameLabel) {} 
    private record GroupFields(JTextField chance, DropGroup originalGroup) {}

    public NpcDetailsPanel(Npc npc, Set<Npc> dirtyNpcs, String dataRoot) {
        this.currentNpc = npc;
        this.dirtyNpcs = dirtyNpcs;
        this.dataRoot = dataRoot;
        
        setLayout(new BorderLayout());
        setBackground(UIStyle.BG_MAIN);
        
        // --- TOP TOOLBAR ---
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        topBar.setBackground(UIStyle.BG_MAIN);
        topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(60, 60, 60)));
        
        JButton openDbBtn = new JButton("Open Item Database");
        UIStyle.styleButton(openDbBtn, new Color(70, 70, 70), Color.WHITE);
        openDbBtn.setToolTipText("Search Item IDs without editing");
        openDbBtn.addActionListener(_ -> ItemBrowserPanel.pickItem(this));
        
        JButton guideBtn = new JButton("Guide & Logic");
        UIStyle.styleButton(guideBtn, new Color(0, 150, 200), Color.WHITE); 
        guideBtn.setToolTipText("View Drop rates logic and Stats info");
        guideBtn.addActionListener(_ -> showGuideDialog());
        
        topBar.add(openDbBtn);
        topBar.add(guideBtn);
        
        add(topBar, BorderLayout.NORTH); 
        
        JTabbedPane tabs = new JTabbedPane();
        tabs.setUI(new UIStyle.BoxTabbedPaneUI());
        tabs.setForeground(Color.WHITE);
        tabs.setBackground(UIStyle.BG_MAIN);

        // --- STATS TAB ---
        JPanel sTab = new JPanel(); sTab.setLayout(new BoxLayout(sTab, BoxLayout.Y_AXIS)); sTab.setBackground(UIStyle.BG_PANEL);
        sTab.add(createSection("General Information", new String[]{"ID:", "Name:", "Type:", "Level:", "HP:", "MP:", "Exp:", "Sp:"}, new String[]{String.valueOf(npc.id), npc.name, npc.type, String.valueOf(npc.level), String.valueOf(npc.hp), String.valueOf(npc.mp), String.valueOf(npc.exp), String.valueOf(npc.sp)}, new String[]{"id", "name", "type", "level", "hp", "mp", "exp", "sp"}, Color.CYAN));
        sTab.add(createSection("Combat Stats", new String[]{"P.Atk:", "M.Atk:", "P.Def:", "M.Def:", "P.Spd:"}, new String[]{String.valueOf(npc.pAtk), String.valueOf(npc.mAtk), String.valueOf(npc.pDef), String.valueOf(npc.mDef), String.valueOf(npc.pSpd)}, new String[]{"pAtk", "mAtk", "pDef", "mDef", "pSpd"}, Color.ORANGE));
        sTab.add(createSection("Base Attributes", new String[]{"STR:", "DEX:", "CON:", "INT:", "WIT:", "MEN:"}, new String[]{String.valueOf(npc.str), String.valueOf(npc.dex), String.valueOf(npc.con), String.valueOf(npc.intel), String.valueOf(npc.wit), String.valueOf(npc.men)}, new String[]{"str", "dex", "con", "intel", "wit", "men"}, Color.RED));
        sTab.add(Box.createVerticalGlue());
        tabs.addTab("Stats", new JScrollPane(sTab));

        // --- DROPS TAB ---
        JPanel dCont = new JPanel(); dCont.setLayout(new BoxLayout(dCont, BoxLayout.Y_AXIS)); dCont.setBackground(UIStyle.BG_PANEL);
        
        JPanel dh = new JPanel(new FlowLayout(FlowLayout.LEFT)); dh.setBackground(UIStyle.BG_PANEL);
        JButton addGrp = new JButton("+ New Category"); UIStyle.styleButton(addGrp, new Color(255, 200, 0), Color.BLACK);
        addGrp.addActionListener(_ -> { 
            int nId = 1; for(DropGroup g : npc.dropGroups) nId = Math.max(nId, g.id + 1); 
            npc.dropGroups.add(new DropGroup(nId, false)); rebuild(); 
        });
        
        dh.add(addGrp);
        dCont.add(dh);
        
        for (DropGroup g : npc.dropGroups) {
            JPanel gc = new JPanel(); gc.setLayout(new BoxLayout(gc, BoxLayout.Y_AXIS)); gc.setBackground(UIStyle.BG_FIELD);
            gc.setBorder(BorderFactory.createTitledBorder(null, "Category " + g.id, 0, 0, null, Color.YELLOW));
            JPanel gh = new JPanel(new FlowLayout(FlowLayout.LEFT)); gh.setBackground(UIStyle.BG_PANEL);
            JTextField cf = new JTextField(String.valueOf(g.chance), 6); UIStyle.styleField(cf); groupFieldList.add(new GroupFields(cf, g));
            JButton addIt = new JButton("+ Item"); UIStyle.styleButton(addIt, new Color(50, 80, 150), Color.WHITE);
            addIt.addActionListener(_ -> { Npc.Item ni = new Npc.Item(); g.items.add(ni); addItemRow(gc, ni, dropFieldList, g.items, npc); gc.revalidate(); });
            gh.add(new JLabel("Chance %:")); gh.add(cf); gh.add(addIt); gc.add(gh);
            for (Npc.Item i : g.items) addItemRow(gc, i, dropFieldList, g.items, npc); dCont.add(gc);
        }
        JPanel glueD = new JPanel(new BorderLayout()); glueD.setBackground(UIStyle.BG_PANEL); glueD.add(dCont, BorderLayout.NORTH);
        tabs.addTab("Drops", new JScrollPane(glueD));

        // --- SPOILS TAB ---
        JPanel sc = new JPanel(); sc.setLayout(new BoxLayout(sc, BoxLayout.Y_AXIS)); sc.setBackground(UIStyle.BG_FIELD);
        sc.setBorder(BorderFactory.createTitledBorder(null, "Spoils", 0, 0, null, Color.MAGENTA));
        
        JPanel sh = new JPanel(new FlowLayout(FlowLayout.LEFT)); sh.setBackground(UIStyle.BG_FIELD);
        JButton addSp = new JButton("+ Item"); UIStyle.styleButton(addSp, new Color(50, 80, 150), Color.WHITE);
        addSp.addActionListener(_ -> { Npc.Item ni = new Npc.Item(); npc.spoilItems.add(ni); addItemRow(sc, ni, spoilFieldList, npc.spoilItems, npc); sc.revalidate(); });
        
        sh.add(addSp);
        sc.add(sh);
        
        for (Npc.Item i : npc.spoilItems) addItemRow(sc, i, spoilFieldList, npc.spoilItems, npc);
        JPanel glueS = new JPanel(new BorderLayout()); glueS.setBackground(UIStyle.BG_PANEL); glueS.add(sc, BorderLayout.NORTH);
        tabs.addTab("Spoils", new JScrollPane(glueS));

        // --- SKILLS TAB ---
        JPanel skWrp = new JPanel(); skWrp.setLayout(new BoxLayout(skWrp, BoxLayout.Y_AXIS)); skWrp.setBackground(UIStyle.BG_FIELD);
        JButton addSk = new JButton("+ Skill"); UIStyle.styleButton(addSk, new Color(50, 80, 150), Color.WHITE);
        addSk.addActionListener(_ -> { Npc.Skill ns = new Npc.Skill(0, 1, ""); npc.skills.add(ns); addSkillRow(skWrp, ns, skillFieldList, npc.skills, npc); skWrp.revalidate(); });
        skWrp.add(addSk); for (Npc.Skill sk : npc.skills) addSkillRow(skWrp, sk, skillFieldList, npc.skills, npc);
        JPanel glueK = new JPanel(new BorderLayout()); glueK.setBackground(UIStyle.BG_PANEL); glueK.add(skWrp, BorderLayout.NORTH);
        tabs.addTab("Skills", new JScrollPane(glueK));

        add(tabs, BorderLayout.CENTER);

        // --- BOTTOM ACTION BAR ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(UIStyle.BG_MAIN);
        
        JButton saveBtn = new JButton("SAVE NPC CHANGES");
        UIStyle.styleButton(saveBtn, new Color(0, 120, 0), Color.WHITE);
        saveBtn.setPreferredSize(new Dimension(0, 50));
        saveBtn.addActionListener(_ -> saveNpc(npc));
        
        bottomPanel.add(saveBtn, BorderLayout.CENTER);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void showGuideDialog() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Stats & Drops Logic", Dialog.ModalityType.MODELESS);
        dialog.add(new IndividualGuidePanel());
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void rebuild() {
        removeAll();
        add(new NpcDetailsPanel(currentNpc, dirtyNpcs, dataRoot), BorderLayout.CENTER);
        revalidate(); repaint();
    }

    private void addItemRow(JPanel p, Npc.Item it, List<ItemFields> fl, List<Npc.Item> sl, Npc npc) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2)); row.setBackground(UIStyle.BG_FIELD);
        
        // --- ID PANEL (Box + Find Button Right) ---
        JPanel idPanel = new JPanel(new BorderLayout(2, 0));
        idPanel.setOpaque(false);
        
        JTextField idF = new JTextField(String.valueOf(it.id), 5); 
        UIStyle.styleField(idF);
        
        JButton pickBtn = new JButton("Find");
        pickBtn.setMargin(new Insets(0, 0, 0, 0)); 
        pickBtn.setToolTipText("Open Item Search");
        pickBtn.setFocusable(false);
        pickBtn.setFont(new Font("SansSerif", Font.BOLD, 11)); 
        UIStyle.styleButton(pickBtn, new Color(60, 60, 60), Color.WHITE);
        pickBtn.setPreferredSize(new Dimension(60, 22)); 
        
        pickBtn.addActionListener(_ -> {
            Integer pickedId = ItemBrowserPanel.pickItem(NpcDetailsPanel.this);
            if (pickedId != null) {
                idF.setText(String.valueOf(pickedId));
                dirtyNpcs.add(npc);
            }
        });
        
        // --- ΕΔΩ ΕΓΙΝΕ Η ΑΛΛΑΓΗ (Text Left, Button Right) ---
        idPanel.add(idF, BorderLayout.CENTER);   
        idPanel.add(pickBtn, BorderLayout.EAST); 
        // ---------------------------------------------------
        
        JTextField miF = new JTextField(String.valueOf(it.min), 5); UIStyle.styleField(miF);
        JTextField maF = new JTextField(String.valueOf(it.max), 5); UIStyle.styleField(maF);
        JTextField chF = new JTextField(String.valueOf(it.chance), 7); UIStyle.styleField(chF);
        JLabel nameL = new JLabel("[" + GameData.getItemName(it.id) + "]"); nameL.setForeground(Color.YELLOW);
        
        idF.getDocument().addDocumentListener(new SimpleDocListener(() -> {
            try { 
                int newId = Integer.parseInt(idF.getText());
                nameL.setText("[" + GameData.getItemName(newId) + "]"); 
                dirtyNpcs.add(npc); 
            } catch(Exception _) {}
        }));
        
        JButton del = new JButton("X"); UIStyle.styleButton(del, new Color(180, 0, 0), Color.WHITE);
        fl.add(new ItemFields(idF, miF, maF, chF, it, nameL));
        del.addActionListener(_ -> { sl.remove(it); p.remove(row); p.revalidate(); dirtyNpcs.add(npc); });
        
        row.add(new JLabel("ID:")); 
        row.add(idPanel); 
        row.add(new JLabel("Min:")); row.add(miF); 
        row.add(new JLabel("Max:")); row.add(maF); 
        row.add(new JLabel("Chance:")); row.add(chF); 
        row.add(del); row.add(nameL); 
        p.add(row);
    }

    private void addSkillRow(JPanel p, Npc.Skill sk, List<SkillFields> l, List<Npc.Skill> s, Npc npc) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2)); row.setBackground(UIStyle.BG_FIELD);
        JTextField idF = new JTextField(String.valueOf(sk.id), 6); UIStyle.styleField(idF);
        JTextField lvF = new JTextField(String.valueOf(sk.level), 4); UIStyle.styleField(lvF);
        JLabel nameL = new JLabel("[" + GameData.getSkillName(sk.id) + "]"); nameL.setForeground(Color.CYAN);
        idF.getDocument().addDocumentListener(new SimpleDocListener(() -> {
            try { int sid = Integer.parseInt(idF.getText()); nameL.setText("[" + GameData.getSkillName(sid) + "]"); dirtyNpcs.add(npc); } catch(Exception _) {}
        }));
        JButton del = new JButton("X"); UIStyle.styleButton(del, new Color(180, 0, 0), Color.WHITE);
        l.add(new SkillFields(idF, lvF, sk, nameL)); 
        del.addActionListener(_ -> { s.remove(sk); p.remove(row); p.revalidate(); dirtyNpcs.add(npc); });
        row.add(new JLabel("ID:")); row.add(idF); row.add(new JLabel("Lv:")); row.add(lvF); row.add(del); row.add(nameL); p.add(row);
    }

    private JPanel createSection(String t, String[] lb, String[] vl, String[] ky, Color c) {
        JPanel s = new JPanel(new BorderLayout()); s.setBackground(UIStyle.BG_PANEL);
        TitledBorder b = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(c), t); b.setTitleColor(c); s.setBorder(b);
        JPanel cnt = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5)); cnt.setOpaque(false);
        for (int i = 0; i < lb.length; i++) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)); p.setOpaque(false);
            JTextField f = new JTextField(vl[i], (ky[i].equals("name") || ky[i].equals("type")) ? 14 : 5); UIStyle.styleField(f);
            f.getDocument().addDocumentListener(new SimpleDocListener(() -> dirtyNpcs.add(currentNpc)));
            fieldMap.put(ky[i], f); p.add(new JLabel(lb[i])); p.add(f); cnt.add(p);
        }
        s.add(cnt, BorderLayout.CENTER); return s;
    }

    private void saveNpc(Npc npc) {
        try {
            npc.id = Integer.parseInt(fieldMap.get("id").getText()); npc.name = fieldMap.get("name").getText(); npc.type = fieldMap.get("type").getText(); npc.level = Integer.parseInt(fieldMap.get("level").getText());
            npc.hp = Double.parseDouble(fieldMap.get("hp").getText()); npc.mp = Double.parseDouble(fieldMap.get("mp").getText()); npc.exp = Long.parseLong(fieldMap.get("exp").getText()); npc.sp = Long.parseLong(fieldMap.get("sp").getText());
            npc.pAtk = Double.parseDouble(fieldMap.get("pAtk").getText()); npc.mAtk = Double.parseDouble(fieldMap.get("mAtk").getText()); npc.pDef = Double.parseDouble(fieldMap.get("pDef").getText()); npc.mDef = Double.parseDouble(fieldMap.get("mDef").getText());
            npc.pSpd = Integer.parseInt(fieldMap.get("pSpd").getText()); npc.str = Integer.parseInt(fieldMap.get("str").getText()); npc.dex = Integer.parseInt(fieldMap.get("dex").getText()); npc.con = Integer.parseInt(fieldMap.get("con").getText());
            npc.intel = Integer.parseInt(fieldMap.get("intel").getText()); npc.wit = Integer.parseInt(fieldMap.get("wit").getText()); npc.men = Integer.parseInt(fieldMap.get("men").getText());
            for (GroupFields g : groupFieldList) g.originalGroup.chance = Double.parseDouble(g.chance.getText());
            for (ItemFields f : dropFieldList) { f.originalItem.id = Integer.parseInt(f.id.getText()); f.originalItem.min = Integer.parseInt(f.min.getText()); f.originalItem.max = Integer.parseInt(f.max.getText()); f.originalItem.chance = Double.parseDouble(f.chance.getText()); }
            for (ItemFields f : spoilFieldList) { f.originalItem.id = Integer.parseInt(f.id.getText()); f.originalItem.min = Integer.parseInt(f.min.getText()); f.originalItem.max = Integer.parseInt(f.max.getText()); f.originalItem.chance = Double.parseDouble(f.chance.getText()); }
            for (SkillFields f : skillFieldList) { f.originalSkill.id = Integer.parseInt(f.id.getText()); f.originalSkill.level = Integer.parseInt(f.level.getText()); }
            NpcWriter.saveAllNpcs(Arrays.asList(npc), dataRoot + File.separator + "npcs"); dirtyNpcs.remove(npc); JOptionPane.showMessageDialog(this, "Saved: " + npc.name);
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error saving: " + e.getMessage()); }
    }

    private class SimpleDocListener implements DocumentListener {
        private Runnable action;
        public SimpleDocListener(Runnable action) { this.action = action; }
        public void insertUpdate(DocumentEvent e) { action.run(); }
        public void removeUpdate(DocumentEvent e) { action.run(); }
        public void changedUpdate(DocumentEvent e) { action.run(); }
    }
}