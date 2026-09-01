#!/usr/bin/env python3
from pathlib import Path
import re
import shutil

ROOT = Path(__file__).resolve().parents[1]
LIVE = ROOT / "src/main/java/com/liveon"
NL = ROOT / "src/main/java/com/nightlegion/livexp"
RES = ROOT / "src/main/resources"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        if new in text:
            return text
        raise RuntimeError(f"missing patch anchor: {label}")
    return text.replace(old, new, 1)


# ---------------------------------------------------------------------------
# Exact Live On config, with only NightLegion connection/English substitutions.
# ---------------------------------------------------------------------------
config = '''package com.liveon;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("live-on-clan-messages")
public interface ClanMessagesConfig extends Config
{
    String THIRD_PARTY_WARNING =
        "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers";

    @ConfigItem(
        keyName = "personalLinkToken",
        name = "Personal Link Token",
        description = "Your NightLegion Discord/RuneLite link token",
        warning = THIRD_PARTY_WARNING,
        secret = true,
        position = 0
    )
    default String personalLinkToken() { return ""; }

    @ConfigItem(
        keyName = "enabled",
        name = "Connect to clan server",
        description = "Enable NightLegion online clan features",
        warning = THIRD_PARTY_WARNING,
        position = 1
    )
    default boolean enabled() { return false; }

    @ConfigItem(
        keyName = "liveStatusEnabled",
        name = "Show live streams",
        description = "Show NightLegion members who are live on Twitch",
        warning = THIRD_PARTY_WARNING,
        position = 2
    )
    default boolean liveStatusEnabled() { return false; }

    @ConfigItem(
        keyName = "statsEnabled",
        name = "Participate in monthly MVP",
        description = "Submit eligible 1M+ drops to the monthly NightLegion MVP ranking",
        warning = THIRD_PARTY_WARNING,
        position = 3
    )
    default boolean statsEnabled() { return false; }

    @ConfigItem(
        keyName = "pbRankingEnabled",
        name = "Participate in PB rankings",
        description = "Submit detected PB times to the private NightLegion leaderboard",
        warning = THIRD_PARTY_WARNING,
        position = 4
    )
    default boolean pbRankingEnabled() { return false; }

    @ConfigItem(
        keyName = "discordDropsEnabled",
        name = "Send drops to Discord",
        description = "Post eligible rare drops to the NightLegion Hall of Fame",
        warning = THIRD_PARTY_WARNING,
        position = 5
    )
    default boolean discordDropsEnabled() { return false; }

    @Range(min = -20, max = 20)
    @ConfigItem(
        keyName = "sidebarIconPriority",
        name = "Sidebar position",
        description = "Move the NightLegion icon on the RuneLite sidebar",
        position = 6
    )
    default int sidebarIconPriority() { return 0; }

    @ConfigItem(
        keyName = "staffAccessKey",
        name = "Staff key",
        description = "Optional server-side staff security key",
        secret = true,
        hidden = true
    )
    default String staffAccessKey() { return ""; }

    @ConfigItem(
        keyName = "discordDropMinimumValue",
        name = "Minimum drop value",
        description = "Minimum GE value for a Discord drop notification",
        hidden = true
    )
    default int discordDropMinimumValue() { return 1_000_000; }

    @ConfigItem(
        keyName = "serverUrl",
        name = "Server",
        description = "NightLegion API address",
        hidden = true
    )
    default String serverUrl() { return "https://nightlegion-livexp.onrender.com/"; }

    @ConfigItem(
        keyName = "pollIntervalSeconds",
        name = "Refresh interval",
        description = "How often to check for new clan messages",
        hidden = true
    )
    default int pollIntervalSeconds() { return 30; }
}
'''
(LIVE / "ClanMessagesConfig.java").write_text(config, encoding="utf-8")

# ---------------------------------------------------------------------------
# Reuse the existing NightLegion event client, but let the exact Live On plugin
# supply the same Personal Link Token without exposing the old plugin config.
# ---------------------------------------------------------------------------
api_path = NL / "NightLegionApi.java"
api = api_path.read_text(encoding="utf-8")
if "java.util.function.Supplier" not in api:
    api = api.replace("import java.util.function.Consumer;", "import java.util.function.Consumer;\nimport java.util.function.Supplier;")
api = api.replace("    private final NightLegionLiveXpConfig config;\n", "    private final Supplier<String> tokenSupplier;\n")
old_ctor = '''    NightLegionApi(OkHttpClient client, ScheduledExecutorService executor, NightLegionLiveXpConfig config, Gson gson)
    {
        this.client = client;
        this.executor = executor;
        this.config = config;
        this.gson = gson;
    }
'''
new_ctor = '''    NightLegionApi(OkHttpClient client, ScheduledExecutorService executor, NightLegionLiveXpConfig config, Gson gson)
    {
        this(client, executor, () -> config.token(), gson);
    }

    NightLegionApi(OkHttpClient client, ScheduledExecutorService executor, Supplier<String> tokenSupplier, Gson gson)
    {
        this.client = client;
        this.executor = executor;
        this.tokenSupplier = tokenSupplier;
        this.gson = gson;
    }
'''
if old_ctor in api:
    api = api.replace(old_ctor, new_ctor)
api = api.replace('        return config.token() == null ? "" : config.token().trim();',
                  '        String value = tokenSupplier == null ? "" : tokenSupplier.get();\n        return value == null ? "" : value.trim();')
api_path.write_text(api, encoding="utf-8")

bridge = '''package com.nightlegion.livexp;

import com.google.gson.Gson;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import javax.swing.JPanel;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import okhttp3.OkHttpClient;

/** Keeps NightLegion-only BOTW/SOTW/Giveaway/Group Finder inside the exact Live On shell. */
public final class NightLegionExtrasBridge
{
    private final NightLegionPanel botw;
    private final NightLegionPanel sotw;
    private final NightLegionPanel giveaway;
    private final NightLegionPanel groups;

    public NightLegionExtrasBridge(Client client, OkHttpClient httpClient, ScheduledExecutorService executor,
        Supplier<String> tokenSupplier, Gson gson, ItemManager itemManager)
    {
        NightLegionApi api = new NightLegionApi(httpClient, executor, tokenSupplier, gson);
        botw = panel(client, api, itemManager, "BOTW");
        sotw = panel(client, api, itemManager, "SOTW");
        giveaway = panel(client, api, itemManager, "GIVEAWAY");
        groups = panel(client, api, itemManager, "GROUP FINDER");
    }

    private static NightLegionPanel panel(Client client, NightLegionApi api, ItemManager itemManager, String section)
    {
        NightLegionPanel panel = new NightLegionPanel(client, api, itemManager, section);
        panel.putClientProperty("nightlegionOwnScroll", Boolean.TRUE);
        return panel;
    }

    public JPanel botwPanel() { return botw; }
    public JPanel sotwPanel() { return sotw; }
    public JPanel giveawayPanel() { return giveaway; }
    public JPanel groupsPanel() { return groups; }

    public void refreshAll()
    {
        botw.refresh();
        sotw.refresh();
        giveaway.refresh();
        groups.refresh();
    }
}
'''
(NL / "NightLegionExtrasBridge.java").write_text(bridge, encoding="utf-8")

# ---------------------------------------------------------------------------
# Exact panel: same layout/components, plus four NightLegion-only pages.
# Manual MVP management is intentionally removed because MVP is automatic.
# ---------------------------------------------------------------------------
panel_path = LIVE / "ClanMessagesPanel.java"
panel = panel_path.read_text(encoding="utf-8")
field_anchor = "\tprivate final ClanTagsPanel clanTagsTab;\n"
if "private final JPanel botwTab;" not in panel:
    panel = panel.replace(field_anchor, field_anchor +
        "\tprivate final JPanel botwTab;\n\tprivate final JPanel sotwTab;\n\tprivate final JPanel giveawayTab;\n\tprivate final JPanel groupsTab;\n")
old_tail = "Runnable refreshPbCategoriesAction, java.util.function.Consumer<PbCategory> selectPbCategoryAction, String initialStaffAccessKey, java.util.function.Consumer<String> saveStaffAccessKeyAction)"
new_tail = "Runnable refreshPbCategoriesAction, java.util.function.Consumer<PbCategory> selectPbCategoryAction, String initialStaffAccessKey, java.util.function.Consumer<String> saveStaffAccessKeyAction, JPanel botwTab, JPanel sotwTab, JPanel giveawayTab, JPanel groupsTab)"
if old_tail in panel:
    panel = panel.replace(old_tail, new_tail, 1)
assign_anchor = "\t\tsuper(false);\n"
if "this.botwTab = botwTab;" not in panel:
    panel = panel.replace(assign_anchor, assign_anchor +
        "\t\tthis.botwTab = botwTab;\n\t\tthis.sotwTab = sotwTab;\n\t\tthis.giveawayTab = giveawayTab;\n\t\tthis.groupsTab = groupsTab;\n", 1)
panel = panel.replace("if (component instanceof MvpPanel || component == staffTab)",
                      "if (component instanceof MvpPanel || component == staffTab || Boolean.TRUE.equals(component.getClientProperty(\"nightlegionOwnScroll\")))")
nav_anchor = '\t\t\taddNavigationButton("PBs", "trophy", pbTab, "pbs", "Recordes pessoais do clã");\n'
if '"BOTW", "star", botwTab' not in panel:
    panel = panel.replace(nav_anchor, nav_anchor +
        '\t\t\taddNavigationButton("BOTW", "star", botwTab, "botw", "Boss of the Week");\n'
        '\t\t\taddNavigationButton("SOTW", "star", sotwTab, "sotw", "Skill of the Week");\n'
        '\t\t\taddNavigationButton("Giveaways", "trophy", giveawayTab, "giveaways", "NightLegion giveaways");\n'
        '\t\t\taddNavigationButton("Groups", "live", groupsTab, "groups", "NightLegion Group Finder");\n')
# Automatic MVP only: never expose the original Add/Remove MVP staff tab.
panel = panel.replace('\t\t\taddStaffSection("MVP", mvpManagementTab, "Gerenciar membros MVP", "trophy");\n', '')
panel_path.write_text(panel, encoding="utf-8")

# ---------------------------------------------------------------------------
# Exact plugin: NightLegion identity, endpoint, token, staff rank gate, extras.
# ---------------------------------------------------------------------------
plugin_path = LIVE / "ClanMessagesPlugin.java"
plugin = plugin_path.read_text(encoding="utf-8")
if "import com.nightlegion.livexp.NightLegionExtrasBridge;" not in plugin:
    plugin = plugin.replace("package com.liveon;\n", "package com.liveon;\n\nimport com.nightlegion.livexp.NightLegionExtrasBridge;\n")
if "import net.runelite.client.util.ImageUtil;" not in plugin:
    plugin = plugin.replace("import net.runelite.client.util.LinkBrowser;", "import net.runelite.client.util.LinkBrowser;\nimport net.runelite.client.util.ImageUtil;")
plugin = plugin.replace('@PluginDescriptor(name = "Live On Clan")', '@PluginDescriptor(name = "NightLegion")')
plugin = plugin.replace('private static final String WOM_USER_AGENT = "Live-On-RuneLite-Plugin";',
                        'private static final String WOM_USER_AGENT = "NightLegion-RuneLite-Plugin";')
if "private NightLegionExtrasBridge nightLegionExtras;" not in plugin:
    plugin = plugin.replace("\tprivate ClanLiveBadgeDecorator clanLiveBadgeDecorator;", "\tprivate ClanLiveBadgeDecorator clanLiveBadgeDecorator;\n\tprivate NightLegionExtrasBridge nightLegionExtras;")
old_panel_ctor = '''\t\tpanel = new ClanMessagesPanel(() -> publishDraft("BROADCAST"), () -> publishDraft("CLAN"), () -> verifyToken(true), this::clearMessages, this::refreshRanks, this::resetRanks, this::requestRank, this::fetchRankRequests, this::deleteRankRequest, this::confirmRankRequest, this::declineRankRequest, this::fetchSentMessages, this::deleteSentMessage, this::resendSentMessage, this::togglePinnedMessage, this::publishPanelNotice, this::removePanelNotice, this::fetchLives, this::saveLiveChannel, this::deleteLiveChannel, this::fetchMvpMembers, this::saveMvpMember, this::deleteMvpMember, this::fetchClanTags, this::createClanTag, this::addClanTagMember, this::deleteClanTag, this::removeClanTagMember, this::fetchPbCategories, this::fetchPbRanking, config.staffAccessKey(), this::saveStaffAccessKey);'''
new_panel_ctor = '''\t\tnightLegionExtras = new NightLegionExtrasBridge(client, okHttpClient, executor, config::personalLinkToken, gson, itemManager);
\t\tpanel = new ClanMessagesPanel(() -> publishDraft("BROADCAST"), () -> publishDraft("CLAN"), () -> verifyToken(true), this::clearMessages, this::refreshRanks, this::resetRanks, this::requestRank, this::fetchRankRequests, this::deleteRankRequest, this::confirmRankRequest, this::declineRankRequest, this::fetchSentMessages, this::deleteSentMessage, this::resendSentMessage, this::togglePinnedMessage, this::publishPanelNotice, this::removePanelNotice, this::fetchLives, this::saveLiveChannel, this::deleteLiveChannel, this::fetchMvpMembers, this::saveMvpMember, this::deleteMvpMember, this::fetchClanTags, this::createClanTag, this::addClanTagMember, this::deleteClanTag, this::removeClanTagMember, this::fetchPbCategories, this::fetchPbRanking, config.staffAccessKey(), this::saveStaffAccessKey, nightLegionExtras.botwPanel(), nightLegionExtras.sotwPanel(), nightLegionExtras.giveawayPanel(), nightLegionExtras.groupsPanel());
\t\tnightLegionExtras.refreshAll();'''
plugin = replace_once(plugin, old_panel_ctor, new_panel_ctor, "panel ctor")
# Clean shutdown reference.
plugin = plugin.replace("\t\tpanel = null;\n", "\t\tnightLegionExtras = null;\n\t\tpanel = null;\n", 1)
# Rank access is based on actual in-game clan rank/title, not WOM/Discord role.
staff_block = re.compile(r'''boolean staff = WomMembership\.isStaffRole\(roleName\);\s*if \(roleName != null\)\s*\{.*?\}\s*isStaff = staff;\s*isDeputyOwner = isDeputyOwnerRole\(roleName\);\s*canPublishBroadcast = WomMembership\.canPublishBroadcast\(roleName\);''', re.S)
plugin, staff_count = staff_block.subn('boolean staff = isNightLegionStaffRank(rsn);\n\t\t\t\t\tisStaff = staff;\n\t\t\t\t\tisDeputyOwner = isNightLegionDeputyOrOwner(rsn);\n\t\t\t\t\tcanPublishBroadcast = staff;', plugin)
if staff_count < 2:
    raise RuntimeError(f"expected two staff role blocks, patched {staff_count}")
helper = '''
\tprivate boolean isNightLegionStaffRank(String playerName)
\t{
\t\tif (playerName == null || client.getClanSettings() == null) return false;
\t\tnet.runelite.api.clan.ClanMember member = client.getClanSettings().findMember(playerName);
\t\tif (member == null || member.getRank() == null) return false;
\t\tClanRank rank = member.getRank();
\t\tif (rank == ClanRank.OWNER || rank == ClanRank.DEPUTY_OWNER) return true;
\t\tClanTitle title = client.getClanSettings().titleForRank(rank);
\t\tString name = title == null ? "" : title.getName();
\t\treturn "Major".equalsIgnoreCase(name) || "General".equalsIgnoreCase(name);
\t}

\tprivate boolean isNightLegionDeputyOrOwner(String playerName)
\t{
\t\tif (playerName == null || client.getClanSettings() == null) return false;
\t\tnet.runelite.api.clan.ClanMember member = client.getClanSettings().findMember(playerName);
\t\tif (member == null || member.getRank() == null) return false;
\t\treturn member.getRank() == ClanRank.OWNER || member.getRank() == ClanRank.DEPUTY_OWNER;
\t}

'''
if "private boolean isNightLegionStaffRank" not in plugin:
    marker = "\tprivate void postJson(String path, String json, okhttp3.Callback callback)"
    plugin = plugin.replace(marker, helper + marker, 1)
# Every exact Live On REST request is authenticated with the existing personal link token.
rb_anchor = "\t\tRequest.Builder builder = new Request.Builder().url(url);\n"
if 'builder.header("X-NightLegion-Token"' not in plugin:
    plugin = plugin.replace(rb_anchor, rb_anchor +
        '\t\tString personalLinkToken = config.personalLinkToken() == null ? "" : config.personalLinkToken().trim();\n'
        '\t\tif (!personalLinkToken.isEmpty()) builder.header("X-NightLegion-Token", personalLinkToken);\n', 1)
# Linked NightLegion token + in-game rank gate replaces the old required server staff key.
plugin = re.sub(r'\tprivate boolean hasStaffAccessKey\(\)\s*\{\s*return config\.staffAccessKey\(\) != null && !config\.staffAccessKey\(\)\.trim\(\)\.isEmpty\(\);\s*\}',
                '\tprivate boolean hasStaffAccessKey()\n\t{\n\t\treturn true;\n\t}', plugin)
# Use the NightLegion logo for the sidebar icon.
icon_pattern = re.compile(r'\tprivate BufferedImage createIcon\(\)\s*\{.*?\n\t\}', re.S)
icon_replacement = '''\tprivate BufferedImage createIcon()
\t{
\t\tBufferedImage source = ImageUtil.loadImageResource(getClass(), "/live-on-logo.png");
\t\tif (source == null) return new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
\t\tjava.awt.Image scaled = source.getScaledInstance(32, 32, java.awt.Image.SCALE_SMOOTH);
\t\tBufferedImage icon = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
\t\tGraphics2D graphics = icon.createGraphics();
\t\tgraphics.drawImage(scaled, 0, 0, null);
\t\tgraphics.dispose();
\t\treturn icon;
\t}'''
plugin, icon_count = icon_pattern.subn(icon_replacement, plugin, count=1)
if icon_count != 1:
    raise RuntimeError("createIcon patch failed")
plugin_path.write_text(plugin, encoding="utf-8")

# NightLegion WOM membership, exact Live On verification logic otherwise unchanged.
wom_path = LIVE / "WomMembership.java"
wom = re.sub(
    r"LIVE_ON_GROUP_ID\s*=\s*\d+",
    "NIGHTLEGION_GROUP_ID = 26182",
    wom_path.read_text(encoding="utf-8"),
)
wom_path.write_text(wom, encoding="utf-8")

# ---------------------------------------------------------------------------
# English/NightLegion text substitution. Ranking rules/thresholds are untouched.
# Rank words are translated consistently, including internal comparison strings.
# ---------------------------------------------------------------------------
replacements = [
    ("Live ON", "NightLegion"), ("Live On", "NightLegion"), ("Live on clan", "NightLegion"),
    ("Bem-vindo ao NightLegion Clan", "Welcome to NightLegion"),
    ("Painel", "Home"), ("PAINEL", "HOME"),
    ("Verificar agora", "Verify now"), ("Verificar em ", "Verify in "),
    ("Desconectado", "Disconnected"), ("Conectando...", "Connecting..."),
    ("Aguardando verificação", "Waiting for verification"),
    ("Conexão desativada", "Connection disabled"),
    ("Membro não identificado. Este plugin é exclusivo para membros do NightLegion.", "Member not found. This plugin is exclusive to NightLegion members."),
    ("Não é membro do clã (WOM)", "Not a clan member (WOM)"),
    ("Falha ao verificar grupo (WOM)", "Failed to verify WOM group"),
    ("Não foi possível validar sua conta no Wise Old Man.", "Could not validate your account on Wise Old Man."),
    ("Verificando...", "Verifying..."),
    ("Exclusivo para membros", "Exclusive to NightLegion members"),
    ("Acompanhe a disputa pelos MVPs", "Follow the monthly MVP race"),
    ("Confira os melhores PBs do clã", "See the clan's best PBs"),
    ("Saiba quem está ao vivo na Twitch", "See who is live on Twitch"),
    ("Atualize seu rank automaticamente", "Track your rank automatically"),
    ("Receba os comunicados oficiais do clã.", "Receive official clan announcements."),
    ("Avisos", "Announcements"), ("AVISOS", "ANNOUNCEMENTS"),
    ("ATIVIDADE RECENTE DO CLÃ", "RECENT CLAN ACTIVITY"),
    ("ONLINE NA TWITCH", "LIVE ON TWITCH"), ("Nenhuma live ativa no momento.", "No active streams right now."),
    ("Ranking mensal", "Monthly ranking"), ("Drops de 1m+", "Drops 1m+"),
    ("CLASSIFICAÇÃO", "RANKING"), ("SUA POSIÇÃO", "YOUR POSITION"),
    ("Líder do mês", "Month leader"),
    ("Como registrar seus PBs?", "How to register your PBs?"),
    ("MELHORES TEMPOS DO CLÃ", "CLAN BEST TIMES"),
    ("Pesquisar", "Search"), ("Atualizar", "Refresh"),
    ("Jogador", "Player"), ("Data", "Date"),
    ("Aceitar", "Accept"), ("Recusar", "Decline"), ("Excluir", "Delete"),
    ("Atualizações recentes", "Recent updates"),
    ("Mensagem para o clã", "Message to clan"), ("Expandir", "Expand"), ("Recolher", "Collapse"),
    ("Fixar broadcast", "Pin broadcast"), ("Enviar mensagem", "Send message"),
    ("Mensagens enviadas", "Sent messages"), ("Tipo", "Type"), ("Mensagem", "Message"),
    ("Segurança da staff", "Staff security"), ("Salvar chave", "Save key"),
    ("Chave removida", "Key removed"), ("Chave salva", "Key saved"),
    ("Aviso fixo no painel", "Pinned Home announcement"),
    ("Publicar aviso fixado", "Publish pinned announcement"), ("Remover aviso atual", "Remove current announcement"),
    ("Nenhum aviso carregado", "No announcement loaded"), ("Nenhum aviso publicado", "No announcement published"),
    ("Aviso atual carregado", "Current announcement loaded"), ("Publicando...", "Publishing..."), ("Removendo...", "Removing..."),
    ("Rank atual", "Current rank"), ("Cargo especial", "Special rank"),
    ("Você está com um cargo especial no momento", "You currently have a special rank"),
    ("Verifique", "Check"), ("Resetar", "Reset"),
    ("solicitação", "request"), ("solicitações", "requests"),
    ("Promoção", "Promotion"), ("promovido para", "promoted to"),
    ("foi promovido para", "was promoted to"),
    ("solicitou um rank", "requested a rank"),
    ("Recruta", "Recruit"), ("recruta", "recruit"),
    ("Soldado", "Soldier"), ("soldado", "soldier"),
    ("Cabo", "Corporal"), ("cabo", "corporal"),
    ("Aluno", "Student"), ("aluno", "student"),
    ("Sargento", "Sergeant"), ("sargento", "sergeant"),
    ("Cadete", "Cadet"), ("cadete", "cadet"),
    ("Tenente", "Lieutenant"), ("tenente", "lieutenant"),
    ("Capitão", "Captain"), ("capitão", "captain"), ("Capitao", "Captain"), ("capitao", "captain"),
    ("Coronel", "Colonel"), ("coronel", "colonel"),
    ("jogadores", "players"),
    (" por ", " by "),
]
for java in LIVE.glob("*.java"):
    text = java.read_text(encoding="utf-8")
    text = re.sub(r"https://(?:www\.)?discord\.gg/[A-Za-z0-9_-]+", "https://discord.gg/AP2aK742SZ", text)
    text = re.sub(r"https://wiseoldman\.net/groups/\d+", "https://wiseoldman.net/groups/26182", text)
    for old, new in replacements:
        text = text.replace(old, new)
    java.write_text(text, encoding="utf-8")

# Rank icon filenames changed with the English internal names; retain identical pixels.
rank_dir = RES / "ranks"
for old, new in {
    "aluno.png": "student.png",
    "cabo.png": "corporal.png",
    "cadete.png": "cadet.png",
    "capitao.png": "captain.png",
    "coronel.png": "colonel.png",
    "sargento.png": "sergeant.png",
    "tenente.png": "lieutenant.png",
}.items():
    src = rank_dir / old
    if src.exists(): shutil.copyfile(src, rank_dir / new)

# Our existing approved NightLegion artwork replaces the Live On logo resource.
logo = RES / "com/nightlegion/livexp/nightlegion-welcome.png"
if logo.exists(): shutil.copyfile(logo, RES / "live-on-logo.png")

# Manifest: the exact Live On implementation is now the active NightLegion plugin.
props = ROOT / "runelite-plugin.properties"
text = props.read_text(encoding="utf-8")
text = re.sub(r'^plugins=.*$', 'plugins=com.liveon.ClanMessagesPlugin', text, flags=re.M)
text = re.sub(r'^description=.*$', 'description=NightLegion clan hub with ranks, MVPs, PBs, Twitch, BOTW, SOTW, giveaways and Group Finder.', text, flags=re.M)
props.write_text(text, encoding="utf-8")

print("Exact Live On source adapted to NightLegion.")
