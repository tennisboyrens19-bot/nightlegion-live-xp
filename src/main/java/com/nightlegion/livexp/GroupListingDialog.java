package com.nightlegion.livexp;

import com.google.gson.JsonObject;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;

class GroupListingDialog
{
    private GroupListingDialog()
    {
    }

    static JsonObject show(
        Window owner,
        List<String> activities,
        int currentWorld,
        ItemManager itemManager,
        String defaultActivity)
    {
        JsonObject[] result = new JsonObject[1];
        Map<Integer, ImageIcon> icons = new HashMap<>();

        JComboBox<String> activity = new JComboBox<>(activities.toArray(new String[0]));
        configureActivityCombo(activity, itemManager, icons);
        selectActivity(activity, defaultActivity);

        JSpinner teamSize = new JSpinner(new SpinnerNumberModel(4, 2, 20, 1));
        JSpinner kc = new JSpinner(new SpinnerNumberModel(0, 0, 1_000_000, 1));
        JComboBox<String> role = new JComboBox<>(new String[]{"ANY", "DPS", "TANK", "FREEZER", "SUPPORT", "LEARNER"});
        JComboBox<String> region = new JComboBox<>(new String[]{"EU", "US EAST", "US WEST", "AU", "ASIA", "OTHER"});
        JTextField language = new JTextField("EN");
        JTextField note = new JTextField();

        List<String> worldOptions = new ArrayList<>();
        worldOptions.add("Not set");
        if (currentWorld > 0)
        {
            worldOptions.add("World " + currentWorld);
        }
        JComboBox<String> world = new JComboBox<>(worldOptions.toArray(new String[0]));

        JCheckBox useDiscord = new JCheckBox("Share linked Discord", true);
        JTextField discordContact = new JTextField("Linked NightLegion account");
        discordContact.setEditable(false);
        discordContact.setEnabled(true);

        JLabel riskWarning = new JLabel(" ");
        riskWarning.setForeground(new Color(236, 112, 112));

        NightLegionTheme.styleCombo(activity);
        NightLegionTheme.styleCombo(role);
        NightLegionTheme.styleCombo(region);
        NightLegionTheme.styleCombo(world);
        NightLegionTheme.styleField(language);
        NightLegionTheme.styleField(note);
        NightLegionTheme.styleField(discordContact);
        styleSpinner(teamSize);
        styleSpinner(kc);

        useDiscord.setOpaque(false);
        useDiscord.setForeground(NightLegionTheme.SILVER);
        useDiscord.addActionListener(e ->
        {
            discordContact.setText(useDiscord.isSelected() ? "Linked NightLegion account" : "Hidden");
        });

        Runnable updateWarning = () ->
        {
            String selected = String.valueOf(activity.getSelectedItem());
            riskWarning.setText(NightLegionTheme.wildernessActivity(selected)
                ? "<html><b>Wilderness:</b> PvP and item loss are possible.</html>"
                : " ");
        };
        activity.addActionListener(e -> updateWarning.run());
        updateWarning.run();

        JPanel form = new JPanel(new GridLayout(0, 2, 12, 10));
        form.setBackground(NightLegionTheme.BACKGROUND);
        form.setBorder(BorderFactory.createEmptyBorder(18, 14, 12, 14));
        addRow(form, "Activity", activity);
        addRow(form, "Team size", teamSize);
        addRow(form, "Your KC", kc);
        addRow(form, "Role", role);
        addRow(form, "Region", region);
        addRow(form, "Language", language);
        addRow(form, "Note", note);
        addRow(form, "Preferred world", world);
        addRow(form, "Discord", useDiscord);
        addRow(form, "Discord contact", discordContact);
        addRow(form, "Risk warning", riskWarning);

        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        NightLegionTheme.styleButton(ok, true, false);
        NightLegionTheme.styleButton(cancel, false, false);
        ok.setPreferredSize(new Dimension(88, 28));
        cancel.setPreferredSize(new Dimension(88, 28));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        actions.setBackground(NightLegionTheme.BACKGROUND);
        actions.setBorder(BorderFactory.createEmptyBorder(0, 10, 8, 10));
        actions.add(ok);
        actions.add(cancel);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(NightLegionTheme.BACKGROUND);
        root.add(form, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);

        JDialog dialog = new JDialog(owner, "Create listing", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setContentPane(root);
        dialog.setMinimumSize(new Dimension(640, 520));
        dialog.setPreferredSize(new Dimension(640, 520));

        cancel.addActionListener(e -> dialog.dispose());
        ok.addActionListener(e ->
        {
            String selected = String.valueOf(activity.getSelectedItem());
            JsonObject data = new JsonObject();
            data.addProperty("activity", selected);
            data.addProperty("title", selected + " group");
            data.addProperty("max_players", ((Number) teamSize.getValue()).intValue());
            data.addProperty("kc", String.valueOf(((Number) kc.getValue()).intValue()));
            data.addProperty("role", String.valueOf(role.getSelectedItem()));
            data.addProperty("region", String.valueOf(region.getSelectedItem()));
            data.addProperty("language", language.getText().trim().isEmpty() ? "EN" : language.getText().trim());
            data.addProperty("note", note.getText().trim());
            data.addProperty("location", "");
            data.addProperty("ttl_minutes", 60);
            data.addProperty("discord_contact", useDiscord.isSelected());
            data.addProperty("approval_required", true);

            String selectedWorld = String.valueOf(world.getSelectedItem());
            if (selectedWorld.startsWith("World "))
            {
                try
                {
                    data.addProperty("world", Integer.parseInt(selectedWorld.substring(6)));
                }
                catch (NumberFormatException ignored)
                {
                }
            }

            if (NightLegionTheme.wildernessActivity(selected))
            {
                javax.swing.JOptionPane warning = new javax.swing.JOptionPane(
                    "This activity may take place in or require travel through the Wilderness.\n"
                        + "Other players can attack you and item loss is possible.\n"
                        + "Never bring items you are not willing to lose.\n\n"
                        + "Create this listing?",
                    javax.swing.JOptionPane.WARNING_MESSAGE,
                    javax.swing.JOptionPane.YES_NO_OPTION);
                JDialog warningDialog = warning.createDialog(dialog, "Wilderness risk warning");
                warningDialog.setVisible(true);
                Object choice = warning.getValue();
                if (!(choice instanceof Integer) || ((Integer) choice) != javax.swing.JOptionPane.YES_OPTION)
                {
                    return;
                }
            }

            result[0] = data;
            dialog.dispose();
        });

        dialog.pack();
        dialog.setSize(640, 520);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return result[0];
    }

    private static void addRow(JPanel form, String name, Component component)
    {
        JLabel label = new JLabel(name);
        label.setForeground(NightLegionTheme.SILVER);
        form.add(label);
        form.add(component);
    }

    private static void styleSpinner(JSpinner spinner)
    {
        spinner.setOpaque(true);
        spinner.setBackground(NightLegionTheme.SURFACE_ALT);
        spinner.setForeground(Color.WHITE);
        spinner.setBorder(BorderFactory.createLineBorder(NightLegionTheme.SURFACE_ALT.brighter()));
        if (spinner.getEditor() instanceof JSpinner.DefaultEditor)
        {
            JTextField field = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
            field.setBackground(NightLegionTheme.SURFACE_ALT);
            field.setForeground(Color.WHITE);
            field.setCaretColor(Color.WHITE);
        }
    }

    private static void selectActivity(JComboBox<String> combo, String preferred)
    {
        if (preferred == null || preferred.trim().isEmpty())
        {
            preferred = "Chambers of Xeric";
        }
        for (int i = 0; i < combo.getItemCount(); i++)
        {
            if (preferred.equalsIgnoreCase(combo.getItemAt(i)))
            {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private static void configureActivityCombo(
        JComboBox<String> combo,
        ItemManager itemManager,
        Map<Integer, ImageIcon> icons)
    {
        combo.setRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus)
            {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                label.setText(value == null ? "" : String.valueOf(value));
                label.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
                label.setBackground(selected ? NightLegionTheme.PURPLE : NightLegionTheme.SURFACE_ALT);
                label.setForeground(Color.WHITE);
                int itemId = NightLegionTheme.activityItemId(label.getText());
                if (itemId > 0 && itemManager != null)
                {
                    ImageIcon icon = icons.computeIfAbsent(itemId, id ->
                    {
                        AsyncBufferedImage image = itemManager.getImage(id);
                        image.onLoaded(() -> SwingUtilities.invokeLater(combo::repaint));
                        return new ImageIcon(image);
                    });
                    label.setIcon(icon);
                }
                else
                {
                    label.setIcon(null);
                }
                return label;
            }
        });
    }
}
