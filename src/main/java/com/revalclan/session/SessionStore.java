package com.revalclan.session;

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
