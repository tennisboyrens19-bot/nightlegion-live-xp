"""Verify pinned upstream source with exact, documented NightLegion adaptations."""
from __future__ import annotations
import argparse, hashlib, io, json, pathlib, re, shutil, tarfile, urllib.request
from reval_account_fixes import adapt_profile_account_boundary
from reval_sync_fixes import FIXES as SYNC_FIXES, adapt_sync
from reval_acceptance_fixes import FIXES as ACCEPTANCE_FIXES, adapt_acceptance

COMMIT = '179faa521b14e4541151effd0f5d28f12ec89597'
ARCHIVE_SHA256 = '65972faa05010ecf641772ea3f25490235d35f13da8c2da6648decd7638c6f8a'
ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA = 'src/main/java/com/revalclan/'
AUTH_PATH = JAVA + 'nightlegion/NightLegionAuthentication.java'
AUTH_SHA256 = 'ec437056b4ea8fca09f04a9c9639f9002cb040da49eb205fb39728885bb6ecd9'
FILEPATH_PATH = JAVA + 'session/SessionStore.java'
FILEPATH_SHA256 = '0d0bd8d6feddf13f788dde3e02a84dd305551db8ab86e487144ef15dd2601f07'
ICON = 'src/main/resources/com/revalclan/ui/assets/reval.png'
ICON_SHA256 = '10eb9ff2d4ac1b1b41bd2399eb43542ceb75f34cbaf8cd865147fc5d03cf4b4a'
AUTH_IMPORT = 'import com.revalclan.nightlegion.NightLegionAuthentication;\n'
SCOPED_FIXES = {
    JAVA+'collectionlog/CollectionLogManager.java': 'Reject invalid/guest/temporary-world ownership; canonicalize item IDs; resolve exact pet IDs from All Pets.',
    JAVA+'notifiers/PetNotifier.java': 'Attach the cache-resolved item ID to the existing pet event.',
    JAVA+'notifiers/DiaryNotifier.java': 'Use the same 48 completion varbits as AchievementDiaryManager.',
    JAVA+'notifiers/ClueNotifier.java': 'Accept RuneLite 1.13.1 long item prices without narrowing; preserve clue quantity arithmetic.',
    JAVA+'notifiers/DeathNotifier.java': 'Accept RuneLite 1.13.1 long item prices throughout death valuation and sorting without changing item selection.',
    JAVA+'notifiers/LootNotifier.java': 'Accept RuneLite 1.13.1 long item prices without narrowing; preserve unit-price filters and stack-value arithmetic.',
    JAVA+'api/account/AccountResponse.java': 'Read account-specific combat achievement thresholds already returned by the backend.',
    JAVA+'ui/ProfilePanel.java': 'Use account combat thresholds; clear account data on logout and reject stale profile callbacks.',
    JAVA+'util/UIAssetLoader.java': 'Read both bundled-image paths through Class.getResourceAsStream with scoped stream closure; preserve paths, caches and null fallbacks.',
    JAVA+'PlayerDataCollector.java': 'Include a bounded staff-observed CC RSN/rank roster and current clan rank in existing sync/login payloads for Bingo registration; no Discord identifiers are added.',
}
SCOPED_FIXES.update(SYNC_FIXES)
for path, reason in ACCEPTANCE_FIXES.items():
    SCOPED_FIXES[path] = (SCOPED_FIXES.get(path, '') + ' ' + reason).strip()
AUTH_SEAMS = [JAVA+path for path in ('RevalClanConfig.java', 'RevalClanPlugin.java',
    'api/RevalApiService.java', 'util/EventFilterManager.java', 'util/WebhookService.java')]
# Literal-only substitutions. Wire keys, class/package names and protocol headers stay unchanged.
EXACT = {
    'https://api.revalosrs.ee/plugin':'https://nightlegion-livexp.onrender.com/plugin',
    'https://api.revalosrs.ee':'https://nightlegion-livexp.onrender.com',
    'https://api.revalosrs.ee/reval-webhook':'https://nightlegion-livexp.onrender.com/reval-webhook',
    'https://api.revalosrs.ee/event-filters':'https://nightlegion-livexp.onrender.com/event-filters',
    'https://discord.gg/reval':'https://discord.gg/AP2aK742SZ',
    'https://revalosrs.ee':'https://nightlegion-web.vercel.app/',
    'revalclan':'nightlegion', 'revalclanclogpb':'nightlegionclogpb',
    'reval-clan/sessions':'nightlegion/sessions',
    'RuneLite-RevalClan-Plugin/':'RuneLite-NightLegion-Plugin/',
}
TOKEN_CONFIG = '''\n\t@ConfigSection(name = "NightLegion Connection", description = "Connect your NightLegion Discord account", position = -1)
\tString connectionSection = "connectionSection";

\t@ConfigItem(keyName = "personalLinkToken", name = "Personal Link Token",
\t\tdescription = "Paste the private token returned by /runelite_link in NightLegion Discord",
\t\tsection = connectionSection, position = 0, secret = true)
\tdefault String personalLinkToken() { return ""; }
'''
TOKEN_CHANGED = '''
        if ("personalLinkToken".equals(event.getKey())) {
            // Authentication boundary only: replay upstream's existing login path.
            // No new snapshot scheduler, resync event type or refresh loop.
            clientThread.invokeLater(() -> {
                revalApiService.clearCache();
                clanMembership.reset();
                if (revalPanel != null) revalPanel.onLoggedOut();
                if (client.getGameState() == GameState.LOGGED_IN) {
                    nightLegionAuthentication.capture(client);
                    onLoggedIn();
                }
            });
            return;
        }
'''
# Java lexical tokens prevent replacements in code, comments, imports or protocol names.
TOKENS = re.compile(r'//[^\n]*|/\*[\s\S]*?\*/|"(?:[^"\\]|\\.)*"|\'(?:[^\'\\]|\\.)*\'')
def branding(source):
    def replace(m):
        token=m.group()
        if not token.startswith('"'):return token
        value=token[1:-1]
        if value in EXACT:return '"'+EXACT[value]+'"'
        # Runtime human-facing labels only; X-Reval-Filters-Version is a wire contract.
        if value == 'X-Reval-Filters-Version' or value.startswith('/com/revalclan/'):
            return token
        value=re.sub(r'\bREVAL CLAN\b', 'NIGHTLEGION', value)
        value=re.sub(r'\bReval Clan\b', 'NightLegion', value)
        value=re.sub(r'\bReval\b', 'NightLegion', value)
        value=re.sub(r'\bREVAL\b', 'NIGHTLEGION', value)
        return '"'+value+'"'
    return TOKENS.sub(replace,source)

def one(source, before, after):
    if source.count(before)!=1:raise ValueError('Upstream anchor changed: '+before[:100])
    return source.replace(before,after,1)

def scoped_fixes(path, source):
    # These are exact, single-anchor transformations, not whole-file exceptions.
    # Every other byte is still checked against the pinned upstream source.
    if path == JAVA+'PlayerDataCollector.java':
        source=one(source, 'import com.revalclan.util.Worlds;\n', '''import com.revalclan.util.Worlds;
import com.revalclan.util.ClanRanks;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import java.util.ArrayList;
import java.util.List;
''')
        source=one(source, '\t\t\t\tslim.put("syncFingerprint", fingerprint);', '''\t\t\t\tslim.put("syncFingerprint", fingerprint);
\t\t\t\tif (data.containsKey("currentClanRank")) slim.put("currentClanRank", data.get("currentClanRank"));
\t\t\t\tif (data.containsKey("clanRoster")) slim.put("clanRoster", data.get("clanRoster"));''')
        source=one(source, '\t\tdata.put("clogPersonalBests", clogPersonalBestCapture.sync());', '''\t\tdata.put("clogPersonalBests", clogPersonalBestCapture.sync());
\t\tClanChannel clan = client.getClanChannel();
\t\tString localName = client.getLocalPlayer() != null ? client.getLocalPlayer().getName() : null;
\t\tif (clan != null && localName != null) {
\t\t\tClanChannelMember localMember = clan.findMember(localName);
\t\t\tif (localMember != null) data.put("currentClanRank", localMember.getRank().toString());
\t\t\tif (ClanRanks.isDeputyOwnerPlus(client)) {
\t\t\t\tList<Map<String, Object>> roster = new ArrayList<>();
\t\t\t\tfor (ClanChannelMember member : clan.getMembers()) {
\t\t\t\t\tif (member == null || member.getName() == null || roster.size() >= 500) continue;
\t\t\t\t\tMap<String, Object> row = new HashMap<>();
\t\t\t\t\trow.put("rsn", member.getName());
\t\t\t\t\trow.put("rank", member.getRank().toString());
\t\t\t\t\troster.add(row);
\t\t\t\t}
\t\t\t\tdata.put("clanRoster", roster);
\t\t\t}
\t\t}''')
    if path == JAVA+'notifiers/ClueNotifier.java':
        source=one(source, 'int price = itemManager.getItemPrice(itemId);',
            'long price = itemManager.getItemPrice(itemId);')
        source=one(source, '(long) price * quantity', 'price * quantity')
    if path == JAVA+'notifiers/DeathNotifier.java':
        source=one(source, 'int gePrice = itemManager.getItemPrice(item.getId());',
            'long gePrice = itemManager.getItemPrice(item.getId());')
        source=one(source, 'int gePrice = (int) item.get("gePrice");',
            'long gePrice = ((Number) item.get("gePrice")).longValue();')
        source=one(source, '(long) gePrice * quantity', 'gePrice * quantity')
        source=one(source, 'Comparator.<Map<String, Object>>comparingInt(m -> (int) m.get("gePrice"))',
            'Comparator.<Map<String, Object>>comparingLong(m -> ((Number) m.get("gePrice")).longValue())')
    if path == JAVA+'notifiers/LootNotifier.java':
        source=one(source, 'int gePrice = itemManager.getItemPrice(itemId);',
            'long gePrice = itemManager.getItemPrice(itemId);')
        source=one(source, '(long) gePrice * item.getQuantity()', 'gePrice * item.getQuantity()')
    if path == JAVA+'util/UIAssetLoader.java':
        source=one(source, 'import java.net.URL;', 'import java.io.InputStream;')
        source=one(source,
            '      URL imageUrl = getClass().getResource(resourcePath);\n'
            '      \n'
            '      if (imageUrl == null) {\n'
            '        return null;\n'
            '      }\n'
            '      \n'
            '      try {\n'
            '''        BufferedImage image = ImageIO.read(imageUrl);
        if (image != null) {
          imageCache.put(normalizedFilename, image);
        }
        return image;
      } catch (IOException e) {
        return null;
      }''', '''      BufferedImage image;
      try (InputStream imageStream = getClass().getResourceAsStream(resourcePath)) {
        if (imageStream == null) {
          return null;
        }
        image = ImageIO.read(imageStream);
      } catch (IOException e) {
        return null;
      }
      if (image != null) {
        imageCache.put(normalizedFilename, image);
      }
      return image;''')
        source=one(source,
            '        URL imageUrl = getClass().getResource(resourcePath);\n'
            '        \n'
            '        if (imageUrl == null) {\n'
            '          return null;\n'
            '        }\n'
            '        \n'
            '        try {\n'
            '''          image = ImageIO.read(imageUrl);
          if (image != null) {
            imageCache.put(normalizedFilename, image);
          }
        } catch (IOException e) {
          return null;
        }''', '''        try (InputStream imageStream = getClass().getResourceAsStream(resourcePath)) {
          if (imageStream == null) {
            return null;
          }
          image = ImageIO.read(imageStream);
        } catch (IOException e) {
          return null;
        }
        if (image != null) {
          imageCache.put(normalizedFilename, image);
        }''')
    if path == JAVA+'collectionlog/CollectionLogManager.java':
        source=one(source, '\t\tobtainedItems.put(itemId, new ObtainedCollectionItem(itemId, itemName, itemCount));', '''\t\tif (!Collections.disjoint(com.revalclan.util.Worlds.flagNames(client),
\t\t\tArrays.asList("SEASONAL", "DEADMAN", "TOURNAMENT_WORLD", "BETA_WORLD",
\t\t\t\t"NOSAVE_MODE", "QUEST_SPEEDRUNNING", "PVP_ARENA", "LAST_MAN_STANDING"))) return;
\t\t// Use the same cache mapping as the category lists. Zero counts and POH
\t\t// guest logs must never become ownership evidence for this account.
\t\tif (itemId <= 0 || itemCount <= 0
\t\t\t|| client.getVarbitValue(net.runelite.api.gameval.VarbitID.COLLECTION_POH_HOST_BOOK_OPEN) == 1) return;
\t\tint replacement = client.getEnum(3721).getIntValue(itemId);
\t\tint canonicalId = replacement > 0 ? replacement : itemId;
\t\tif (canonicalId != itemId) itemName = client.getItemDefinition(canonicalId).getName();
\t\tobtainedItems.put(canonicalId, new ObtainedCollectionItem(canonicalId, itemName, itemCount));
\t}

\t/** Resolve an exact pet name only within the cache's All Pets category. */
\tpublic Integer getPetItemId(String name) {
\t\tInteger category = categoryStructIdMap.get("all_pets");
\t\tif (name == null || category == null) return null;
\t\tInteger found = null;
\t\tfor (Integer id : categoryItemMap.getOrDefault(category, Collections.emptySet())) {
\t\t\tif (name.equalsIgnoreCase(client.getItemDefinition(id).getName())) {
\t\t\t\tif (found != null && !found.equals(id)) return null;
\t\t\t\tfound = id;
\t\t\t}
\t\t}
\t\treturn found;''')
    if path == JAVA+'notifiers/PetNotifier.java':
        source=one(source, 'public class PetNotifier extends BaseNotifier {', '''public class PetNotifier extends BaseNotifier {
\t@javax.inject.Inject
\tprivate com.revalclan.collectionlog.CollectionLogManager collectionLogManager;''')
        source=one(source, '\t\t\tpetData.put("petName", this.petName);', '''\t\t\tpetData.put("petName", this.petName);
\t\t\tInteger itemId = collectionLogManager.getPetItemId(this.petName);
\t\t\tif (itemId != null) petData.put("itemId", itemId);''')
    if path == JAVA+'notifiers/DiaryNotifier.java':
        # Correct the constants here; queued-completion guards are applied below.
        mappings = {
            'Ardougne': ([3577,3598,3608,3630], [4458,4459,4460,4461]),
            'Desert': ([3579,3597,3610,3628], [4483,4484,4485,4486]),
            'Falador': ([3580,3596,3612,3632], [4462,4463,4464,4465]),
            'Fremennik': ([3582,3594,3615,3636], [4491,4492,4493,4494]),
            'Kandarin': ([3583,3593,3617,3638], [4475,4476,4477,4478]),
            'Karamja': ([3578,3599,3611,3631], [3578,3599,3611,4566]),
            'Lumbridge': ([3581,3595,3614,3635], [4495,4496,4497,4498]),
            'Morytania': ([3584,3592,3618,3639], [4487,4488,4489,4490]),
            'Varrock': ([3576,3601,3606,3627], [4479,4480,4481,4482]),
            'Western': ([3585,3591,3620,3641], [4471,4472,4473,4474]),
            'Wilderness': ([3586,3600,3621,3642], [4466,4467,4468,4469]),
        }
        for area, (old_ids, new_ids) in mappings.items():
            for tier, old, new in zip(('Easy','Medium','Hard','Elite'), old_ids, new_ids):
                source=one(source, f'map.put({old}, "{area}_{tier}");', f'map.put({new}, "{area}_{tier}");')
    if path == JAVA+'api/account/AccountResponse.java':
        source=one(source, 'import java.util.List;', 'import java.util.List;\nimport java.util.Map;')
        source=one(source, '        private Integer combatAchievementPoints;', '        private Integer combatAchievementPoints;\n        private Map<String, Integer> combatAchievementThresholds;')
    if path == JAVA+'ui/ProfilePanel.java':
        source=adapt_profile_account_boundary(source, one)
        source=one(source, '\t\t\t\tboolean completed = tier.getThreshold() != null && progress >= tier.getThreshold();', '''\t\t\t\tInteger threshold = tierThreshold(sourceKey, tier);
\t\t\t\tboolean completed = threshold != null && progress >= threshold;''')
        source=one(source, '\tprivate JPanel createStatCard(String value, String label, Color accentColor, String sourceType) {', '''\tprivate Integer tierThreshold(String sourceKey, PointsResponse.PointSource tier) {
\t\tif ("COMBAT_ACHIEVEMENTS".equals(sourceKey) && currentAccount != null
\t\t\t&& currentAccount.getCombatAchievementThresholds() != null
\t\t\t&& tier.getId() != null && tier.getId().startsWith("combat_achievement_")) {
\t\t\tString key = tier.getId().substring("combat_achievement_".length());
\t\t\tInteger threshold = currentAccount.getCombatAchievementThresholds().get(key);
\t\t\tif (threshold != null && threshold > 0) return threshold;
\t\t}
\t\treturn tier.getThreshold();
\t}

\tprivate JPanel createStatCard(String value, String label, Color accentColor, String sourceType) {''')
    return source

def normalized(path, content):
    # Git's Windows checkout may expand LF to CRLF. Only that text encoding
    # difference is ignored; whitespace, comments and code remain significant.
    if path.endswith('.java') or path == 'LICENSE' or path.startswith('LICENSES/'):
        return content.replace(b'\r\n', b'\n')
    return content

def adapted(path, content):
    content=normalized(path, content)
    if not path.endswith('.java'):return content
    source=branding(content.decode('utf-8'))
    if path == JAVA+'RevalClanConfig.java':
        source=one(source,'public interface RevalClanConfig extends Config {','public interface RevalClanConfig extends Config {'+TOKEN_CONFIG)
    if path in {JAVA+'RevalClanPlugin.java', JAVA+'api/RevalApiService.java',JAVA+'util/WebhookService.java', JAVA+'util/EventFilterManager.java'}:
        pos=source.index('\n\n')+2;source=source[:pos]+AUTH_IMPORT+source[pos:]
    if path == JAVA+'api/RevalApiService.java':
        source=one(source,'@Inject\n    public RevalApiService(OkHttpClient httpClient, Gson gson)',
    '@Inject\n    public RevalApiService(OkHttpClient httpClient, Gson gson, NightLegionAuthentication authentication) {\n        this(authentication.decorate(httpClient), gson);\n    }\n\n    public RevalApiService(OkHttpClient httpClient, Gson gson)')
    if path in {JAVA+'util/WebhookService.java',JAVA+'util/EventFilterManager.java'}:
        before='\t@Inject\n\tprivate OkHttpClient httpClient;' if path.endswith('WebhookService.java') else '\t@Inject private OkHttpClient httpClient;'
        source=one(source,before,'''\tprivate OkHttpClient httpClient;
\t@Inject void connectNightLegion(OkHttpClient client, NightLegionAuthentication authentication) {
\t\tthis.httpClient = authentication.decorate(client);
\t}''')
    if path==JAVA+'RevalClanPlugin.java':
        source=one(source,'\t@Inject private Client client;','\t@Inject private Client client;\n\t@Inject private NightLegionAuthentication nightLegionAuthentication;')
        source=one(source,'\t\t\tcollectionLogManager.parseCacheForCollectionLog();', '\t\t\tnightLegionAuthentication.capture(client);\n\t\t\tcollectionLogManager.parseCacheForCollectionLog();')
        source=one(source,'public void onGameTick(GameTick gameTick) {','public void onGameTick(GameTick gameTick) {\n\t\tnightLegionAuthentication.capture(client);')
        source=one(source,'if (!"nightlegion".equals(event.getGroup())) return;', 'if (!"nightlegion".equals(event.getGroup())) return;\n'+TOKEN_CHANGED)
        source=one(source,'import com.revalclan.session.SessionTracker;', 'import com.revalclan.session.SessionStore;\nimport com.revalclan.session.SessionTracker;')
        source=one(source,'@PluginDescriptor(\n\tname = "NightLegion"\n)', '@PluginDescriptor(\n\tname = "NightLegion",\n\tinternalName = "nightlegion",\n\tlegacyDataDirectory = "nightlegion"\n)')
        source=one(source,'\t@Inject\tprivate SessionTracker sessionTracker;', '\t@Inject\tprivate SessionStore sessionStore;\n\t@Inject\tprivate SessionTracker sessionTracker;')
        source=one(source,'\t\tclanMembership.reset();\n\t\tsessionTracker.setOnHeartbeatResponse(this::onChanges);', '\t\tclanMembership.reset();\n\t\tsessionStore.initialize(getPluginDirectory());\n\t\tsessionTracker.setOnHeartbeatResponse(this::onChanges);')
    source = adapt_sync(path, scoped_fixes(path, source), one)
    return adapt_acceptance(path, source, one).encode('utf-8')

def upstream(archive=None):
    if archive is not None:raw=pathlib.Path(archive).read_bytes()
    else:
        request=urllib.request.Request(f'https://codeload.github.com/revalOSRS/reval-cc-plugin/tar.gz/{COMMIT}',headers={'User-Agent':'NightLegion-source-parity'})
        with urllib.request.urlopen(request,timeout=45) as r:raw=r.read()
    if hashlib.sha256(raw).hexdigest()!=ARCHIVE_SHA256:raise ValueError('Upstream archive checksum mismatch')
    files={}
    with tarfile.open(fileobj=io.BytesIO(raw)) as tar:
        for member in tar.getmembers():
            if not member.isfile():continue
            path=pathlib.PurePosixPath(*pathlib.PurePosixPath(member.name).parts[1:])
            if '..' in path.parts or path.is_absolute():raise ValueError('Unsafe path')
            files[str(path)]=tar.extractfile(member).read()
    return files

def verify(files, root=ROOT):
    """Verify a whole runtime tree. Shared by the CLI and negative mutation tests."""
    scope={p:b for p,b in files.items() if p.startswith('src/main/') or p=='LICENSE' or p.startswith('LICENSES/')}
    actual={p.relative_to(root).as_posix() for p in (root/'src/main').rglob('*') if p.is_file()}
    expected={p for p in scope if p.startswith('src/main/')}|{AUTH_PATH}
    if actual!=expected:raise AssertionError({'missing':sorted(expected-actual),'extra':sorted(actual-expected)})
    mismatches=[];identical=[];raw_identical=[];branding_only=[];auth_seams=[];filepath_seams=[];bug_fixes=[]
    if hashlib.sha256(normalized(AUTH_PATH, (root/AUTH_PATH).read_bytes())).hexdigest()!=AUTH_SHA256:
        mismatches.append(AUTH_PATH)
    if hashlib.sha256((root/ICON).read_bytes()).hexdigest()!=ICON_SHA256:
        mismatches.append(ICON)
    for p,b in scope.items():
        if p==ICON:continue
        raw=(root/p).read_bytes()
        got=normalized(p, raw)
        if p==FILEPATH_PATH:
            if hashlib.sha256(got).hexdigest()!=FILEPATH_SHA256:mismatches.append(p)
        elif got!=adapted(p,b):mismatches.append(p)
        if p.endswith('.java'):
            if raw==b:raw_identical.append(p)
            if p==FILEPATH_PATH:filepath_seams.append(p)
            elif p in SCOPED_FIXES:bug_fixes.append(p)
            elif got==normalized(p,b):identical.append(p)
            elif got==branding(normalized(p,b).decode()).encode():branding_only.append(p)
            else:auth_seams.append(p)
    if mismatches:raise AssertionError('Unapproved upstream deviations: '+str(mismatches))
    return {'upstreamCommit':COMMIT,'upstreamArchiveSha256':ARCHIVE_SHA256,
        'upstreamJavaFiles':len(identical)+len(branding_only)+len(auth_seams)+len(filepath_seams)+len(bug_fixes),
        'byteIdentical':len(raw_identical),'identicalAfterLineEndingNormalization':len(identical),
        'comparisonNormalization':'CRLF to LF in Java and license text only',
        'brandingOrDestinationOnly':len(branding_only),'authenticationSeams':AUTH_SEAMS,
        'authenticationOnlySeams':auth_seams,
        'reviewerFilepathSeams':filepath_seams,'scopedBugFixes':{p:SCOPED_FIXES[p] for p in bug_fixes},
        'addedAuthenticationOnly':[AUTH_PATH],'authenticationSourceSha256':AUTH_SHA256,
        'missing':[],'unexpectedRuntimeFiles':[],
        'modifiedAsset':ICON,'assetSha256':hashlib.sha256((root/ICON).read_bytes()).hexdigest(),
        'unlistedUpstreamDeviationsRejected':True,'backendParityNotProvedByThisCheck':True}

def main(argv=None):
    parser=argparse.ArgumentParser();parser.add_argument('--apply',action='store_true');parser.add_argument('--archive');args=parser.parse_args(argv)
    files=upstream(args.archive)
    scope={p:b for p,b in files.items() if p.startswith('src/main/') or p=='LICENSE' or p.startswith('LICENSES/')}
    if args.apply:
        # Delete only the old client runtime source/assets. Never touches a DB/user file.
        auth=(ROOT/AUTH_PATH).read_bytes()
        filepath_store=(ROOT/FILEPATH_PATH).read_bytes()
        if hashlib.sha256(filepath_store).hexdigest()!=FILEPATH_SHA256: raise ValueError('Unexpected Filepath SessionStore')
        icon_path = ROOT/ICON
        if not icon_path.exists(): icon_path=ROOT/'src/main/resources/com/revalclan/ui/assets/nightlegion.png'
        icon=icon_path.read_bytes()
        if hashlib.sha256(icon).hexdigest()!=ICON_SHA256: raise ValueError('Unexpected NightLegion icon')
        shutil.rmtree(ROOT/'src/main')
        for p,b in scope.items():
            target=ROOT/p;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(adapted(p,b))
        for p,b in ((AUTH_PATH,auth),(FILEPATH_PATH,filepath_store),(ICON,icon)):
            target=ROOT/p;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(b)
        # Old adaptation tests assert removed code; replace with the upstream launcher
        # and explicit authentication/protocol/parity tests, kept separately.
        tests={str(p.relative_to(ROOT)):p.read_bytes() for p in (ROOT/'src/test/java/com/revalclan/nightlegion').rglob('*.java')}
        if (ROOT/'src/test').exists(): shutil.rmtree(ROOT/'src/test')
        tests.update({p:b for p,b in files.items() if p.startswith('src/test/')})
        for p,b in tests.items():
            target=ROOT/p;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(b)
        for p,b in files.items():
            if p in {'gradlew','gradlew.bat'} or p.startswith('gradle/wrapper/'):
                target=ROOT/p;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(b)
        (ROOT/'gradlew').chmod(0o755)
        # Use upstream build tasks, changing test dependencies only.
        build=files['build.gradle'].decode().replace("testImplementation 'junit:junit:4.12'", "testImplementation 'junit:junit:4.13.2'\n\ttestImplementation 'org.mockito:mockito-core:4.11.0'\n\ttestImplementation 'com.squareup.okhttp3:mockwebserver:3.14.9'")
        (ROOT/'build.gradle').write_text(build)
        (ROOT/'settings.gradle').write_text("rootProject.name = 'nightlegion'\n")
        (ROOT/'runelite-plugin.properties').write_text('displayName=NightLegion\nbuild=standard\nauthor=NightLegion (upstream: Lightroom)\ndescription=NightLegion clan plugin\ntags=clan,cc,nightlegion\nplugins=com.revalclan.RevalClanPlugin\nversion=2.20.1-nightlegion.2\n')
    report=verify(files, ROOT)
    (ROOT/'build').mkdir(exist_ok=True);(ROOT/'build/source-parity.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps(report,indent=2))
if __name__=='__main__':main()
