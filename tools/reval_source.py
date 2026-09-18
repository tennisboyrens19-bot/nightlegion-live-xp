"""Reproduce/verify the upstream client; only branding and authentication may differ."""
from __future__ import annotations
import argparse, hashlib, io, json, pathlib, re, shutil, tarfile, urllib.request

COMMIT = '179faa521b14e4541151effd0f5d28f12ec89597'
ARCHIVE_SHA256 = '65972faa05010ecf641772ea3f25490235d35f13da8c2da6648decd7638c6f8a'
ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA = 'src/main/java/com/revalclan/'
AUTH_PATH = JAVA + 'nightlegion/NightLegionAuthentication.java'
FILEPATH_PATH = JAVA + 'session/SessionStore.java'
FILEPATH_SHA256 = '0d0bd8d6feddf13f788dde3e02a84dd305551db8ab86e487144ef15dd2601f07'
ICON = 'src/main/resources/com/revalclan/ui/assets/reval.png'
AUTH_IMPORT = 'import com.revalclan.nightlegion.NightLegionAuthentication;\n'
# Literal-only substitutions. Wire keys, class/package names and protocol headers stay unchanged.
EXACT = {
    'https://api.revalosrs.ee/plugin':'https://nightlegion-livexp.onrender.com/plugin',
    'https://api.revalosrs.ee':'https://nightlegion-livexp.onrender.com',
    'https://api.revalosrs.ee/reval-webhook':'https://nightlegion-livexp.onrender.com/reval-webhook',
    'https://api.revalosrs.ee/event-filters':'https://nightlegion-livexp.onrender.com/event-filters',
    'https://discord.gg/reval':'https://discord.com/channels/1404482606241943582/',
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

def adapted(path, content):
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
    return source.encode('utf-8')

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

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--apply',action='store_true');parser.add_argument('--archive');args=parser.parse_args()
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
        if hashlib.sha256(icon).hexdigest()!='10eb9ff2d4ac1b1b41bd2399eb43542ceb75f34cbaf8cd865147fc5d03cf4b4a': raise ValueError('Unexpected NightLegion icon')
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
    actual={str(p.relative_to(ROOT)) for p in (ROOT/'src/main').rglob('*') if p.is_file()}
    expected={p for p in scope if p.startswith('src/main/')}|{AUTH_PATH}
    if actual!=expected:raise AssertionError({'missing':sorted(expected-actual),'extra':sorted(actual-expected)})
    mismatches=[];identical=[];branding_only=[];auth_seams=[];filepath_seams=[]
    for p,b in scope.items():
        if p==ICON:continue
        got=(ROOT/p).read_bytes()
        if p==FILEPATH_PATH:
            if hashlib.sha256(got).hexdigest()!=FILEPATH_SHA256:mismatches.append(p)
        elif got!=adapted(p,b):mismatches.append(p)
        if p.endswith('.java'):
            if p==FILEPATH_PATH:filepath_seams.append(p)
            elif got==b:identical.append(p)
            elif got==branding(b.decode()).encode():branding_only.append(p)
            else:auth_seams.append(p)
    if mismatches:raise AssertionError('Unapproved upstream deviations: '+str(mismatches))
    report={'upstreamCommit':COMMIT,'upstreamJavaFiles':len(identical)+len(branding_only)+len(auth_seams)+len(filepath_seams),
        'byteIdentical':len(identical),'brandingOrDestinationOnly':len(branding_only),'authenticationSeams':auth_seams,
        'reviewerFilepathSeams':filepath_seams,
        'addedAuthenticationOnly':[AUTH_PATH],'missing':[],'unexpectedRuntimeFiles':[],
        'modifiedAsset':ICON,'assetSha256':hashlib.sha256((ROOT/ICON).read_bytes()).hexdigest(),
        'clientCollectorsSessionsNotifiersPreserved':True,'backendParityNotProvedByThisCheck':True}
    (ROOT/'build').mkdir(exist_ok=True);(ROOT/'build/source-parity.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps(report,indent=2))
if __name__=='__main__':main()
