package com.nightlegion.livexp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.client.ui.PluginPanel;

class NightLegionPanel extends PluginPanel
{
    private final Client client;
    private final NightLegionApi api;

    private final JComboBox<String> section = new JComboBox<>(new String[]{
        "BOTW", "SOTW", "GIVEAWAY", "GROUP FINDER"
    });
    private final JLabel connection = new JLabel("Not connected");
    private final JPanel body = new JPanel();
    private JsonObject latest;
    private List<String> activities = new ArrayList<>();

    NightLegionPanel(Client client, NightLegionApi api)
    {
        this.client = client;
        this.api = api;

        setLayout(new BorderLayout());
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("NIGHTLEGION");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        connection.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton refresh = new JButton("Refresh");
        refresh.setAlignmentX(Component.LEFT_ALIGNMENT);
        refresh.addActionListener(e -> refresh());

        root.add(title);
        root.add(Box.createVerticalStrut(3));
        root.add(connection);
        root.add(Box.createVerticalStrut(10));
        root.add(section);
        root.add(Box.createVerticalStrut(6));
        root.add(refresh);
        root.add(Box.createVerticalStrut(10));

        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(body);

        section.addActionListener(e -> render());
        add(new JScrollPane(root), BorderLayout.CENTER);

        refresh();
    }

    void refresh()
    {
        setStatus("Refreshing...");
        api.action("overview", rsn(), new JsonObject(), json ->
            SwingUtilities.invokeLater(() ->
            {
                latest = json;
                connection.setText("● Connected" + (json.has("discord_name") ? " as " + json.get("discord_name").getAsString() : ""));
                activities = new ArrayList<>();
                if (json.has("activities") && json.get("activities").isJsonArray())
                {
                    for (JsonElement e : json.getAsJsonArray("activities"))
                    {
                        activities.add(e.getAsString());
                    }
                }
                render();
            }), this::showError);
    }

    private void render()
    {
        body.removeAll();
        if (latest == null)
        {
            body.add(new JLabel("Waiting for NightLegion..."));
            revalidate();
            repaint();
            return;
        }

        String selected = String.valueOf(section.getSelectedItem());
        if ("BOTW".equals(selected))
        {
            renderEvent("botw", "JOIN BOTW");
        }
        else if ("SOTW".equals(selected))
        {
            renderEvent("sotw", "JOIN SOTW");
        }
        else if ("GIVEAWAY".equals(selected))
        {
            renderGiveaway();
        }
        else
        {
            renderGroups();
        }

        revalidate();
        repaint();
    }

    private void renderEvent(String key, String joinText)
    {
        if (!latest.has(key) || latest.get(key).isJsonNull())
        {
            cardTitle("No active " + key.toUpperCase());
            return;
        }

        JsonObject event = latest.getAsJsonObject(key);
        cardTitle(event.get("label").getAsString());

        addLine("Entry", formatGp(event.get("entry_fee_gp").getAsLong()));
        addLine("Participants", String.valueOf(event.get("participants").getAsInt()));
        addLine("Ends", formatTime(event.get("end_time").getAsLong()));

        if (event.has("prizes") && event.get("prizes").isJsonArray())
        {
            JsonArray prizes = event.getAsJsonArray("prizes");
            for (int i = 0; i < prizes.size(); i++)
            {
                addLine(i == 0 ? "Prizes" : "", prizes.get(i).getAsString());
            }
        }

        boolean entered = event.has("entered") && event.get("entered").getAsBoolean();
        boolean pending = event.has("pending_buyin") && event.get("pending_buyin").getAsBoolean();

        JButton join = new JButton(entered ? "✓ ENTERED" : pending ? "BUY-IN PENDING" : joinText);
        join.setEnabled(!entered && !pending);
        join.setAlignmentX(Component.LEFT_ALIGNMENT);
        join.addActionListener(e ->
        {
            join.setEnabled(false);
            setStatus("Sending...");
            api.action("botw".equals(key) ? "join_botw" : "join_sotw", rsn(), new JsonObject(), result ->
                SwingUtilities.invokeLater(() ->
                {
                    showMessage(result);
                    refresh();
                }), this::showError);
        });
        body.add(Box.createVerticalStrut(8));
        body.add(join);
    }

    private void renderGiveaway()
    {
        if (!latest.has("giveaway") || latest.get("giveaway").isJsonNull())
        {
            cardTitle("No active giveaway");
            return;
        }

        JsonObject g = latest.getAsJsonObject("giveaway");
        cardTitle(g.get("prize").getAsString());
        addLine("Entries", String.valueOf(g.get("entries").getAsInt()));
        addLine("Ends", formatTime(g.get("end_time").getAsLong()));

        if (g.has("required_role_name") && !g.get("required_role_name").isJsonNull())
        {
            addLine("Required rank", g.get("required_role_name").getAsString());
        }
        else
        {
            addLine("Required rank", "None");
        }

        boolean entered = g.get("entered").getAsBoolean();
        boolean eligible = g.get("eligible").getAsBoolean();
        JButton enter = new JButton(entered ? "✓ ENTERED" : eligible ? "ENTER GIVEAWAY" : "RANK REQUIRED");
        enter.setEnabled(!entered && eligible);
        enter.setAlignmentX(Component.LEFT_ALIGNMENT);
        enter.addActionListener(e ->
        {
            JsonObject data = new JsonObject();
            data.addProperty("giveaway_id", g.get("id").getAsString());
            api.action("enter_giveaway", rsn(), data, result ->
                SwingUtilities.invokeLater(() ->
                {
                    showMessage(result);
                    refresh();
                }), this::showError);
        });
        body.add(Box.createVerticalStrut(8));
        body.add(enter);
    }

    private void renderGroups()
    {
        JPanel buttons = new JPanel(new GridLayout(2, 2, 5, 5));
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        JButton refresh = new JButton("Refresh");
        JButton create = new JButton("Create Listing");
        JButton requests = new JButton("Requests");
        JButton mine = new JButton("My Group");

        refresh.addActionListener(e -> refresh());
        create.addActionListener(e -> createListing());
        requests.addActionListener(e -> showRequests());
        mine.addActionListener(e -> showMyGroups());

        buttons.add(refresh);
        buttons.add(create);
        buttons.add(requests);
        buttons.add(mine);
        body.add(buttons);
        body.add(Box.createVerticalStrut(10));

        if (!latest.has("groups") || !latest.get("groups").isJsonArray() || latest.getAsJsonArray("groups").size() == 0)
        {
            cardTitle("No active groups");
            return;
        }

        for (JsonElement element : latest.getAsJsonArray("groups"))
        {
            JsonObject group = element.getAsJsonObject();
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(7, 7, 7, 7)
            ));
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));

            JLabel name = new JLabel(group.get("activity").getAsString() + " — " + group.get("host_name").getAsString());
            name.setFont(name.getFont().deriveFont(Font.BOLD));
            card.add(name);
            card.add(new JLabel("Players: " + group.getAsJsonArray("members").size() + "/" + group.get("max_players").getAsInt()));
            if (group.has("world") && !group.get("world").isJsonNull())
            {
                card.add(new JLabel("World: " + group.get("world").getAsInt()));
            }
            if (group.has("role") && !group.get("role").isJsonNull())
            {
                card.add(new JLabel("Role: " + group.get("role").getAsString()));
            }
            if (group.has("kc") && !group.get("kc").isJsonNull() && !group.get("kc").getAsString().isEmpty())
            {
                card.add(new JLabel("KC: " + group.get("kc").getAsString()));
            }

            boolean joined = group.get("joined").getAsBoolean();
            boolean requested = group.get("requested").getAsBoolean();
            boolean host = group.get("is_host").getAsBoolean();
            JButton join = new JButton(host ? "YOUR GROUP" : joined ? "✓ JOINED" : requested ? "REQUEST PENDING" : "REQUEST TO JOIN");
            join.setEnabled(!host && !joined && !requested && "open".equals(group.get("status").getAsString()));
            join.addActionListener(e ->
            {
                JsonObject data = new JsonObject();
                data.addProperty("group_id", group.get("id").getAsString());
                api.action("group_join", rsn(), data, result ->
                    SwingUtilities.invokeLater(() ->
                    {
                        showMessage(result);
                        refresh();
                    }), this::showError);
            });
            card.add(Box.createVerticalStrut(5));
            card.add(join);

            body.add(card);
            body.add(Box.createVerticalStrut(7));
        }
    }

    private void createListing()
    {
        if (activities.isEmpty())
        {
            showError("Activities are still loading. Press Refresh.");
            return;
        }
        Window owner = SwingUtilities.getWindowAncestor(this);
        JsonObject data = GroupListingDialog.show(owner, activities, client.getWorld());
        if (data == null)
        {
            return;
        }
        setStatus("Creating listing...");
        api.action("group_create", rsn(), data, result ->
            SwingUtilities.invokeLater(() ->
            {
                showMessage(result);
                refresh();
            }), this::showError);
    }

    private void showRequests()
    {
        api.action("group_requests", rsn(), new JsonObject(), result ->
            SwingUtilities.invokeLater(() ->
            {
                JsonArray rows = result.has("requests") ? result.getAsJsonArray("requests") : new JsonArray();
                if (rows.size() == 0)
                {
                    JOptionPane.showMessageDialog(this, "No pending requests.", "NightLegion", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                JPanel list = new JPanel();
                list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
                for (JsonElement element : rows)
                {
                    JsonObject row = element.getAsJsonObject();
                    JPanel line = new JPanel(new BorderLayout(5, 5));
                    line.add(new JLabel(row.get("display_name").getAsString() + " → " + row.get("group_title").getAsString()), BorderLayout.CENTER);
                    JPanel actions = new JPanel();
                    JButton accept = new JButton("Accept");
                    JButton decline = new JButton("Decline");
                    accept.addActionListener(e -> decide(row, true));
                    decline.addActionListener(e -> decide(row, false));
                    actions.add(accept);
                    actions.add(decline);
                    line.add(actions, BorderLayout.SOUTH);
                    list.add(line);
                }
                JOptionPane.showMessageDialog(this, new JScrollPane(list), "Group Requests", JOptionPane.PLAIN_MESSAGE);
            }), this::showError);
    }

    private void decide(JsonObject row, boolean accept)
    {
        JsonObject data = new JsonObject();
        data.addProperty("group_id", row.get("group_id").getAsString());
        data.addProperty("user_id", row.get("user_id").getAsLong());
        data.addProperty("accept", accept);
        api.action("group_request_decide", rsn(), data, result ->
            SwingUtilities.invokeLater(this::refresh), this::showError);
    }

    private void showMyGroups()
    {
        if (latest == null || !latest.has("groups"))
        {
            return;
        }
        List<JsonObject> mine = new ArrayList<>();
        for (JsonElement e : latest.getAsJsonArray("groups"))
        {
            JsonObject g = e.getAsJsonObject();
            if (g.get("is_host").getAsBoolean())
            {
                mine.add(g);
            }
        }
        if (mine.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "You have no active groups.", "NightLegion", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        for (JsonObject g : mine)
        {
            JPanel row = new JPanel(new BorderLayout());
            row.add(new JLabel(g.get("title").getAsString()), BorderLayout.CENTER);
            JButton close = new JButton("Close");
            close.addActionListener(e ->
            {
                JsonObject data = new JsonObject();
                data.addProperty("group_id", g.get("id").getAsString());
                api.action("group_close", rsn(), data, result ->
                    SwingUtilities.invokeLater(() ->
                    {
                        showMessage(result);
                        refresh();
                    }), this::showError);
            });
            row.add(close, BorderLayout.EAST);
            list.add(row);
        }
        JOptionPane.showMessageDialog(this, list, "My Group", JOptionPane.PLAIN_MESSAGE);
    }

    private String rsn()
    {
        return client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null
            ? ""
            : client.getLocalPlayer().getName().trim();
    }

    private void cardTitle(String text)
    {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 15f));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(label);
        body.add(Box.createVerticalStrut(6));
    }

    private void addLine(String left, String right)
    {
        JLabel line = new JLabel((left == null || left.isEmpty() ? "" : left + ": ") + right);
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(line);
    }

    private static String formatGp(long gp)
    {
        if (gp <= 0)
        {
            return "FREE";
        }
        if (gp >= 1_000_000)
        {
            return (gp % 1_000_000 == 0 ? String.valueOf(gp / 1_000_000) : String.format("%.1f", gp / 1_000_000.0)) + "M GP";
        }
        if (gp >= 1_000)
        {
            return (gp / 1_000) + "K GP";
        }
        return gp + " GP";
    }

    private static String formatTime(long unix)
    {
        long seconds = Math.max(0, unix - System.currentTimeMillis() / 1000L);
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long mins = (seconds % 3600) / 60;
        if (days > 0)
        {
            return days + "d " + hours + "h";
        }
        if (hours > 0)
        {
            return hours + "h " + mins + "m";
        }
        return mins + "m";
    }

    private void showMessage(JsonObject result)
    {
        String message = result.has("message") ? result.get("message").getAsString() : "Done.";
        JOptionPane.showMessageDialog(this, message, "NightLegion", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String error)
    {
        SwingUtilities.invokeLater(() ->
        {
            setStatus(error);
            JOptionPane.showMessageDialog(this, error, "NightLegion", JOptionPane.ERROR_MESSAGE);
        });
    }

    private void setStatus(String text)
    {
        SwingUtilities.invokeLater(() -> connection.setText(text));
    }
}
