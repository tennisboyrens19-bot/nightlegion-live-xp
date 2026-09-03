package com.revalclan.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.revalclan.api.NightLegionTransport;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Manages dynamic event filters fetched from the backend API
 */
@Slf4j
@Singleton
public class EventFilterManager{
	@Inject private NightLegionTransport transport;
	
	@Getter private EventFilters filters;
	
	/**
	 * Holds all filter configurations
	 */
	@Getter
	public static class EventFilters {
		// Loot filter settings
		@Getter private long lootMinValue = 1_000_000; // 1M default
		@Getter private Set<Integer> lootWhitelist = new HashSet<>();
		@Getter private Set<Integer> lootBlacklist = new HashSet<>(Arrays.asList(526, 995)); // Bones, Coins
		
		// Detailed kill filter settings
		@Getter private Set<Integer> detailedKillNpcIdWhitelist = new HashSet<>();
		@Getter private Set<Integer> detailedKillNpcIdBlacklist = new HashSet<>();
		/** NPC names (lowercase) needed by active server-side requirements; matched by containment */
		@Getter private Set<String> detailedKillNpcNameWhitelist = new HashSet<>();
		
		// Chat filter settings
		@Getter private List<String> chatPatterns = new ArrayList<>(); // Empty by default = no patterns, all messages pass
		
		// Event toggles
		@Getter private boolean lootEnabled = true;
		@Getter private boolean petEnabled = true;
		@Getter private boolean questEnabled = true;
		@Getter private boolean levelEnabled = true;
		@Getter private boolean killCountEnabled = true;
		@Getter private boolean clueEnabled = true;
		@Getter private boolean diaryEnabled = true;
		@Getter private boolean combatAchievementEnabled = true;
		@Getter private boolean collectionEnabled = true;
		@Getter private boolean deathEnabled = true;
		@Getter private boolean detailedKillEnabled = true;
		@Getter private boolean emoteEnabled = true;
		@Getter private boolean chatEnabled = true;
		@Getter private boolean musicEnabled = true;
		@Getter private boolean leaguesEnabled = false;
	}
	
	public EventFilterManager() {
		// Initialize with defaults
		filters = new EventFilters();
	}
	
	/**
	 * Fetch filters from the API
	 * @return true if successful, false otherwise
	 */
	public boolean fetchFilters() {
		fetchFiltersAsync();
		return true;
	}
	
	/**
	 * Fetch filters asynchronously
	 */
	public void fetchFiltersAsync() {
		transport.request("community_reval_filters", new JsonObject(), response -> {
			parseFilters(response);
			log.info("Successfully fetched NightLegion event filters");
		}, error -> log.warn("Failed to fetch NightLegion event filters: {}", error.getMessage()));
	}
	
	/**
	 * Parse the filters JSON response
	 */
	private void parseFilters(JsonObject json) {
		EventFilters newFilters = new EventFilters();
		
		try {
			// Parse loot filters
			if (json.has("loot")) {
				JsonObject loot = json.getAsJsonObject("loot");
				
				if (loot.has("minValue")) {
					newFilters.lootMinValue = loot.get("minValue").getAsLong();
				}
				
				// Clear defaults and only use API values
				newFilters.lootWhitelist.clear();
				if (loot.has("whitelist") && loot.get("whitelist").isJsonArray()) {
					loot.getAsJsonArray("whitelist").forEach(item -> 
						newFilters.lootWhitelist.add(item.getAsInt())
					);
				}
				
				newFilters.lootBlacklist.clear();
				if (loot.has("blacklist") && loot.get("blacklist").isJsonArray()) {
					loot.getAsJsonArray("blacklist").forEach(item -> 
						newFilters.lootBlacklist.add(item.getAsInt())
					);
				}
			}
			
			// Parse detailed kill filters
			if (json.has("detailedKill")) {
				JsonObject detailedKill = json.getAsJsonObject("detailedKill");
				
				// Clear defaults and only use API values
				newFilters.detailedKillNpcIdWhitelist.clear();
				if (detailedKill.has("npcIdWhitelist") && detailedKill.get("npcIdWhitelist").isJsonArray()) {
					detailedKill.getAsJsonArray("npcIdWhitelist").forEach(id -> 
						newFilters.detailedKillNpcIdWhitelist.add(id.getAsInt())
					);
				}
				
				newFilters.detailedKillNpcIdBlacklist.clear();
				if (detailedKill.has("npcIdBlacklist") && detailedKill.get("npcIdBlacklist").isJsonArray()) {
					detailedKill.getAsJsonArray("npcIdBlacklist").forEach(id ->
						newFilters.detailedKillNpcIdBlacklist.add(id.getAsInt())
					);
				}

				newFilters.detailedKillNpcNameWhitelist.clear();
				if (detailedKill.has("npcNameWhitelist") && detailedKill.get("npcNameWhitelist").isJsonArray()) {
					detailedKill.getAsJsonArray("npcNameWhitelist").forEach(name ->
						newFilters.detailedKillNpcNameWhitelist.add(name.getAsString().toLowerCase())
					);
				}
			}
			
			// Parse chat filters
			if (json.has("chat")) {
				JsonObject chat = json.getAsJsonObject("chat");
				
				newFilters.chatPatterns.clear();
				if (chat.has("patterns") && chat.get("patterns").isJsonArray()) {
					chat.getAsJsonArray("patterns").forEach(pattern -> 
						newFilters.chatPatterns.add(pattern.getAsString())
					);
				}
			}
			
			// Parse event toggles
			if (json.has("enabled")) {
				JsonObject enabled = json.getAsJsonObject("enabled");
				
				if (enabled.has("loot")) newFilters.lootEnabled = enabled.get("loot").getAsBoolean();
				if (enabled.has("pet")) newFilters.petEnabled = enabled.get("pet").getAsBoolean();
				if (enabled.has("quest")) newFilters.questEnabled = enabled.get("quest").getAsBoolean();
				if (enabled.has("level")) newFilters.levelEnabled = enabled.get("level").getAsBoolean();
				if (enabled.has("killCount")) newFilters.killCountEnabled = enabled.get("killCount").getAsBoolean();
				if (enabled.has("clue")) newFilters.clueEnabled = enabled.get("clue").getAsBoolean();
				if (enabled.has("diary")) newFilters.diaryEnabled = enabled.get("diary").getAsBoolean();
				if (enabled.has("combatAchievement")) newFilters.combatAchievementEnabled = enabled.get("combatAchievement").getAsBoolean();
				if (enabled.has("collection")) newFilters.collectionEnabled = enabled.get("collection").getAsBoolean();
				if (enabled.has("death")) newFilters.deathEnabled = enabled.get("death").getAsBoolean();
				if (enabled.has("detailedKill")) newFilters.detailedKillEnabled = enabled.get("detailedKill").getAsBoolean();
				if (enabled.has("emote")) newFilters.emoteEnabled = enabled.get("emote").getAsBoolean();
				if (enabled.has("chat")) newFilters.chatEnabled = enabled.get("chat").getAsBoolean();
				if (enabled.has("music")) newFilters.musicEnabled = enabled.get("music").getAsBoolean();
				if (enabled.has("leagues")) newFilters.leaguesEnabled = enabled.get("leagues").getAsBoolean();
			}
			
			// Atomically replace filters
			this.filters = newFilters;
		} catch (Exception e) {
			log.error("Error parsing filters JSON", e);
		}
	}
}

