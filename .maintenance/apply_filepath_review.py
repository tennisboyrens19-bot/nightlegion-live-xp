from pathlib import Path
import hashlib

ROOT = Path(".")

SESSION_STORE = r'''package com.revalclan.session;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Filepath;

/**
 * Crash-safe persistence for in-progress sessions in the plugin data directory,
 * written synchronously. File access is intentionally restricted to RuneLite's
 * {@link Filepath} API.
 */
@Slf4j
@Singleton
public class SessionStore
{
	private static final long MAX_AGE_MS = 14L * 24 * 60 * 60 * 1000;

	private final Gson gson;
	private Filepath sessionDir;

	@Inject
	SessionStore(Gson gson)
	{
		this.gson = gson;
	}

	/**
	 * Called by the plugin during startup with its RuneLite-owned data directory.
	 */
	public void initialize(Filepath pluginDirectory) throws IOException
	{
		sessionDir = pluginDirectory.joinSegment("sessions");
		sessionDir.createDirectories();
	}

	/** Persisted shape. {@code summary} stays a JsonObject so numbers survive exactly. */
	public static class PersistedSession
	{
		public long accountHash;
		public String username;
		public int world;
		public List<String> worldFlags;
		/** Membership was proven while this session was recorded — safe to send at startup */
		public boolean member;
		public JsonObject summary;

		public String sessionId()
		{
			try
			{
				return summary.get("sessionId").getAsString();
			}
			catch (Exception e)
			{
				return null;
			}
		}

		public long startedAt()
		{
			try
			{
				return summary.get("startedAt").getAsLong();
			}
			catch (Exception e)
			{
				return 0;
			}
		}
	}

	private Filepath dir()
	{
		if (sessionDir == null)
		{
			throw new IllegalStateException("Session store not initialized");
		}
		return sessionDir;
	}

	private Filepath fileFor(String sessionId)
	{
		return dir().joinSegment(sessionId + ".json");
	}

	/** Atomic write: temp file then move, so a crash mid-write never leaves a torn file. */
	public void write(PersistedSession session)
	{
		String id = session.sessionId();
		if (id == null)
		{
			return;
		}

		Filepath target = fileFor(id);
		Filepath tmp = dir().joinSegment(id + ".tmp");
		try
		{
			tmp.write(gson.toJson(session));
			try
			{
				tmp.moveTo(target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			}
			catch (IOException atomicUnsupported)
			{
				tmp.moveTo(target, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		catch (IOException e)
		{
			log.warn("Failed to persist session {}: {}", id, e.getMessage());
		}
	}

	public void delete(String sessionId)
	{
		if (sessionId == null)
		{
			return;
		}
		try
		{
			fileFor(sessionId).deleteIfExists();
		}
		catch (IOException e)
		{
			log.warn("Failed to delete persisted session {}: {}", sessionId, e.getMessage());
		}
	}

	/** Every readable persisted session, newest first. Unreadable or expired files are removed. */
	public List<PersistedSession> readAll()
	{
		List<PersistedSession> out = new ArrayList<>();
		long now = System.currentTimeMillis();

		try (Stream<Filepath> files = dir().walk(1))
		{
			files.filter(Filepath::isFile)
				.filter(file -> file.getFileName().endsWith(".json"))
				.forEach(file -> read(file, now, out));
		}
		catch (IOException e)
		{
			log.warn("Failed to read persisted sessions: {}", e.getMessage());
		}

		out.sort((a, b) -> Long.compare(b.startedAt(), a.startedAt()));
		return out;
	}

	private void read(Filepath file, long now, List<PersistedSession> out)
	{
		PersistedSession session = null;
		try (var reader = file.openReader())
		{
			session = gson.fromJson(reader, PersistedSession.class);
		}
		catch (Exception ignored)
		{
		}

		if (session == null || session.summary == null || session.sessionId() == null
			|| now - session.startedAt() > MAX_AGE_MS)
		{
			try
			{
				file.deleteIfExists();
			}
			catch (IOException e)
			{
				log.warn("Could not remove stale session file {}: {}", file.getFileName(), e.getMessage());
			}
			return;
		}

		out.add(session);
	}
}
'''

store = ROOT / "src/main/java/com/revalclan/session/SessionStore.java"
old_store = store.read_text()
for forbidden in ("import java.io.File;", "import java.nio.file.Files;", "import java.nio.file.Path;", "new File(", "Files."):
    assert forbidden in old_store, forbidden
store.write_text(SESSION_STORE)

plugin = ROOT / "src/main/java/com/revalclan/RevalClanPlugin.java"
s = plugin.read_text()
assert "import com.revalclan.session.SessionStore;" not in s
s = s.replace(
    "import com.revalclan.session.SessionTracker;",
    "import com.revalclan.session.SessionStore;\nimport com.revalclan.session.SessionTracker;",
    1,
)
s = s.replace(
    '@PluginDescriptor(\n\tname = "NightLegion"\n)',
    '@PluginDescriptor(\n\tname = "NightLegion",\n\tinternalName = "nightlegion",\n\tlegacyDataDirectory = "nightlegion"\n)',
    1,
)
s = s.replace(
    "\t@Inject private SessionTracker sessionTracker;",
    "\t@Inject private SessionStore sessionStore;\n\t@Inject private SessionTracker sessionTracker;",
    1,
)
s = s.replace(
    "\t\tclanMembership.reset();\n\t\tsessionTracker.setOnHeartbeatResponse(this::onChanges);",
    "\t\tclanMembership.reset();\n\t\tsessionStore.initialize(getPluginDirectory());\n\t\tsessionTracker.setOnHeartbeatResponse(this::onChanges);",
    1,
)
plugin.write_text(s)

props = ROOT / "runelite-plugin.properties"
props_text = props.read_text()
assert "version=2.20.1-nightlegion.1" in props_text
props.write_text(props_text.replace("version=2.20.1-nightlegion.1", "version=2.20.1-nightlegion.2", 1))

checker = ROOT / "tools/reval_source.py"
s = checker.read_text()
assert "FILEPATH_PATH" not in s
s = s.replace(
    "AUTH_PATH = JAVA + 'nightlegion/NightLegionAuthentication.java'\n",
    "AUTH_PATH = JAVA + 'nightlegion/NightLegionAuthentication.java'\n"
    "FILEPATH_PATH = JAVA + 'session/SessionStore.java'\n"
    "FILEPATH_SHA256 = '0d0bd8d6feddf13f788dde3e02a84dd305551db8ab86e487144ef15dd2601f07'\n",
    1,
)
needle = '''        source=one(source,'if (!"nightlegion".equals(event.getGroup())) return;', 'if (!"nightlegion".equals(event.getGroup())) return;\\n'+TOKEN_CHANGED)
'''
extra = '''        source=one(source,'import com.revalclan.session.SessionTracker;', 'import com.revalclan.session.SessionStore;\\nimport com.revalclan.session.SessionTracker;')
        source=one(source,'@PluginDescriptor(\\n\\tname = "NightLegion"\\n)', '@PluginDescriptor(\\n\\tname = "NightLegion",\\n\\tinternalName = "nightlegion",\\n\\tlegacyDataDirectory = "nightlegion"\\n)')
        source=one(source,'\\t@Inject private SessionTracker sessionTracker;', '\\t@Inject private SessionStore sessionStore;\\n\\t@Inject private SessionTracker sessionTracker;')
        source=one(source,'\\t\\tclanMembership.reset();\\n\\t\\tsessionTracker.setOnHeartbeatResponse(this::onChanges);', '\\t\\tclanMembership.reset();\\n\\t\\tsessionStore.initialize(getPluginDirectory());\\n\\t\\tsessionTracker.setOnHeartbeatResponse(this::onChanges);')
'''
assert needle in s
s = s.replace(needle, needle + extra, 1)
s = s.replace(
    "        auth=(ROOT/AUTH_PATH).read_bytes()\n        icon_path = ROOT/ICON\n",
    "        auth=(ROOT/AUTH_PATH).read_bytes()\n"
    "        filepath_store=(ROOT/FILEPATH_PATH).read_bytes()\n"
    "        if hashlib.sha256(filepath_store).hexdigest()!=FILEPATH_SHA256: raise ValueError('Unexpected Filepath SessionStore')\n"
    "        icon_path = ROOT/ICON\n",
    1,
)
s = s.replace(
    "        for p,b in ((AUTH_PATH,auth),(ICON,icon)):\n",
    "        for p,b in ((AUTH_PATH,auth),(FILEPATH_PATH,filepath_store),(ICON,icon)):\n",
    1,
)
s = s.replace(
    "version=2.20.1-nightlegion.1\\n')",
    "version=2.20.1-nightlegion.2\\n')",
    1,
)
loop_old = """    mismatches=[];identical=[];branding_only=[];auth_seams=[]
    for p,b in scope.items():
        if p==ICON:continue
        got=(ROOT/p).read_bytes()
        if got!=adapted(p,b):mismatches.append(p)
        if p.endswith('.java'):
            if got==b:identical.append(p)
            elif got==branding(b.decode()).encode():branding_only.append(p)
            else:auth_seams.append(p)
    if mismatches:raise AssertionError('Unapproved upstream deviations: '+str(mismatches))
    report={'upstreamCommit':COMMIT,'upstreamJavaFiles':len(identical)+len(branding_only)+len(auth_seams),
        'byteIdentical':len(identical),'brandingOrDestinationOnly':len(branding_only),'authenticationSeams':auth_seams,
"""
loop_new = """    mismatches=[];identical=[];branding_only=[];auth_seams=[];filepath_seams=[]
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
"""
assert loop_old in s
s = s.replace(loop_old, loop_new, 1)
checker.write_text(s)

wf = ROOT / ".github/workflows/exact-release-tests.yml"
s = wf.read_text()
assert "fix/filepath-review-2026-09-18" not in s
s = s.replace(
    "branches: [fix/reval-exact-179faa]",
    "branches: [fix/reval-exact-179faa, fix/filepath-review-2026-09-18]",
    1,
)
needle = "      - name: Verify upstream source preservation\n        run: python3 tools/reval_source.py\n"
assert needle in s
s = s.replace(
    needle,
    needle
    + "      - name: Verify runtime filesystem access uses RuneLite Filepath\n"
      "        run: |\n"
      "          if grep -RInE 'import java\\\\.io\\\\.File;|import java\\\\.nio\\\\.file\\\\.(Files|Path);|new File\\\\(|Files\\\\.|\\\\.toPath\\\\(' src/main/java; then\n"
      "            echo 'Direct filesystem access found; use net.runelite.client.util.Filepath.' >&2\n"
      "            exit 1\n"
      "          fi\n",
    1,
)
wf.write_text(s)

# Self-check the reviewer request before CI.
runtime = ROOT / "src/main/java"
for path in runtime.rglob("*.java"):
    text = path.read_text(errors="ignore")
    for forbidden in ("import java.io.File;", "import java.nio.file.Files;", "import java.nio.file.Path;", "new File(", "Files.", ".toPath("):
        if forbidden in text:
            raise AssertionError(f"Direct filesystem access remains in {path}: {forbidden}")

compile(checker.read_text(), str(checker), "exec")
assert hashlib.sha256(SESSION_STORE.encode()).hexdigest() == "0d0bd8d6feddf13f788dde3e02a84dd305551db8ab86e487144ef15dd2601f07"
print("Filepath reviewer patch prepared.")
