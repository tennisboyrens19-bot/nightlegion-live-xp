package com.nightlegion.livexp;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.Text;

/**
 * Exact Live On-style decorations inside RuneLite's native clan member list.
 * MVP is gold and LIVE is green, appended after the player's name.
 */
final class NightLegionClanBadgeDecorator
{
    static final String MVP_MARKUP = " <col=ffc628>MVP</col>";
    static final String LIVE_MARKUP = " <col=96ffaa>LIVE</col>";

    private final Client client;
    private final NightLegionLiveXpPlugin plugin;

    NightLegionClanBadgeDecorator(Client client, NightLegionLiveXpPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
    }

    void refresh()
    {
        Widget playerList = client.getWidget(InterfaceID.ClansSidepanel.PLAYERLIST);
        if (playerList == null || playerList.isHidden())
        {
            return;
        }
        Set<Widget> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        decorateWidgetTree(playerList, plugin.badgesEnabled(), visited, false);
    }

    void clearDecorations()
    {
        Widget playerList = client.getWidget(InterfaceID.ClansSidepanel.PLAYERLIST);
        if (playerList == null)
        {
            return;
        }
        Set<Widget> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        decorateWidgetTree(playerList, false, visited, true);
    }

    private void decorateWidgetTree(Widget widget, boolean enabled, Set<Widget> visited, boolean includeHidden)
    {
        if (widget == null || (!includeHidden && widget.isHidden()) || !visited.add(widget))
        {
            return;
        }

        String rawText = widget.getText();
        if (rawText != null && !rawText.isEmpty())
        {
            String baseText = removeOwnMarkup(rawText);
            String displayedText = Text.removeTags(baseText).trim();
            String playerName = enabled ? plugin.badgePlayerNameIn(displayedText) : null;
            String decoratedText = baseText;
            if (playerName != null)
            {
                if (plugin.isPlayerMvp(playerName))
                {
                    decoratedText += MVP_MARKUP;
                }
                if (plugin.isPlayerLive(playerName))
                {
                    decoratedText += LIVE_MARKUP;
                }
            }
            if (!rawText.equals(decoratedText))
            {
                widget.setText(decoratedText);
            }
        }

        decorateChildren(widget.getChildren(), enabled, visited, includeHidden);
        decorateChildren(widget.getDynamicChildren(), enabled, visited, includeHidden);
        decorateChildren(widget.getStaticChildren(), enabled, visited, includeHidden);
        decorateChildren(widget.getNestedChildren(), enabled, visited, includeHidden);
    }

    private void decorateChildren(Widget[] children, boolean enabled, Set<Widget> visited, boolean includeHidden)
    {
        if (children == null)
        {
            return;
        }
        for (Widget child : children)
        {
            decorateWidgetTree(child, enabled, visited, includeHidden);
        }
    }

    static String removeOwnMarkup(String text)
    {
        return text == null ? "" : text.replace(MVP_MARKUP, "").replace(LIVE_MARKUP, "");
    }
}
