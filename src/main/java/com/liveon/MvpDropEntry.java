package com.liveon;

import com.google.gson.annotations.SerializedName;
final class MvpDropEntry
{
	@SerializedName("player_name")
	private String playerName;
	@SerializedName("total_value")
	private long totalValue;
	@SerializedName("drop_count")
	private int dropCount;
	@SerializedName("top_drops")
	private MvpDropDetail[] topDrops;
	@SerializedName("account_type")
	private String accountType;

	MvpDropEntry()
	{
	}

	MvpDropEntry(String playerName, long totalValue, int dropCount)
	{
		this.playerName = playerName;
		this.totalValue = totalValue;
		this.dropCount = dropCount;
	}

	String getPlayerName() { return playerName; }
	long getTotalValue() { return totalValue; }
	int getDropCount() { return dropCount; }
	MvpDropDetail[] getTopDrops() { return topDrops; }
	String getAccountType() { return accountType; }
}
