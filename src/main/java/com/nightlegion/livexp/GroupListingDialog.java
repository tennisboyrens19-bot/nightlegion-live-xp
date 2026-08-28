package com.nightlegion.livexp;

import com.google.gson.JsonObject;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

class GroupListingDialog
{
    static JsonObject show(Window owner, List<String> activities, int currentWorld)
    {
        JComboBox<String> activity = new JComboBox<>(activities.toArray(new String[0]));
        JSpinner teamSize = new JSpinner(new SpinnerNumberModel(5, 2, 20, 1));
        JTextField kc = new JTextField();
        JComboBox<String> role = new JComboBox<>(new String[]{"ANY", "DPS", "TANK", "LEARNER", "SUPPORT"});
        JComboBox<String> region = new JComboBox<>(new String[]{"ANY", "EU", "NA", "OCE", "ASIA"});
        JComboBox<String> language = new JComboBox<>(new String[]{"English", "Norwegian", "Swedish", "Danish", "German", "Other"});
        JTextArea note = new JTextArea(4, 22);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        JTextField world = new JTextField(currentWorld > 0 ? String.valueOf(currentWorld) : "");
        JTextField location = new JTextField();
        JSpinner ttl = new JSpinner(new SpinnerNumberModel(60, 5, 1440, 5));
        JCheckBox discordContact = new JCheckBox("Allow Discord contact", true);
        JCheckBox approval = new JCheckBox("Host approval required", true);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        form.add(new JLabel("Activity"));
        form.add(activity);
        form.add(new JLabel("Team size"));
        form.add(teamSize);
        form.add(new JLabel("Your KC"));
        form.add(kc);
        form.add(new JLabel("Role"));
        form.add(role);
        form.add(new JLabel("Region"));
        form.add(region);
        form.add(new JLabel("Language"));
        form.add(language);
        form.add(new JLabel("Preferred world"));
        form.add(world);
        form.add(new JLabel("Meeting location"));
        form.add(location);
        form.add(new JLabel("Listing duration (min)"));
        form.add(ttl);
        form.add(discordContact);
        form.add(approval);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.add(form, BorderLayout.NORTH);
        JPanel notePanel = new JPanel(new BorderLayout());
        notePanel.setBorder(BorderFactory.createTitledBorder("Note"));
        notePanel.add(new JScrollPane(note), BorderLayout.CENTER);
        root.add(notePanel, BorderLayout.CENTER);

        JOptionPane pane = new JOptionPane(
            root,
            JOptionPane.PLAIN_MESSAGE,
            JOptionPane.OK_CANCEL_OPTION
        );
        JDialog dialog = pane.createDialog(owner, "Create Listing");
        dialog.setModal(true);
        dialog.pack();
        dialog.setVisible(true);

        Object result = pane.getValue();
        if (!(result instanceof Integer) || ((Integer) result) != JOptionPane.OK_OPTION)
        {
            return null;
        }

        JsonObject data = new JsonObject();
        String selected = String.valueOf(activity.getSelectedItem());
        data.addProperty("activity", selected);
        data.addProperty("title", selected + " group");
        data.addProperty("max_players", ((Number) teamSize.getValue()).intValue());
        data.addProperty("kc", kc.getText().trim());
        data.addProperty("role", String.valueOf(role.getSelectedItem()));
        data.addProperty("region", String.valueOf(region.getSelectedItem()));
        data.addProperty("language", String.valueOf(language.getSelectedItem()));
        data.addProperty("note", note.getText().trim());
        data.addProperty("location", location.getText().trim());
        data.addProperty("ttl_minutes", ((Number) ttl.getValue()).intValue());
        data.addProperty("discord_contact", discordContact.isSelected());
        data.addProperty("approval_required", approval.isSelected());

        try
        {
            String rawWorld = world.getText().trim();
            if (!rawWorld.isEmpty())
            {
                data.addProperty("world", Integer.parseInt(rawWorld));
            }
        }
        catch (NumberFormatException ignored)
        {
        }

        return data;
    }
}
