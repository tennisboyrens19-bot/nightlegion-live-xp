package com.liveon;

import com.google.gson.annotations.SerializedName;
final class MvpEfficiencyEntry
{
	@SerializedName("player_name")
	private String playerName;
	@SerializedName("account_type")
	private String accountType;
	private double gained;
	private MvpEfficiencyContribution[] breakdown;

	String getPlayerName() { return playerName; }
	String getAccountType() { return accountType; }
	double getGained() { return gained; }
	MvpEfficiencyContribution[] getBreakdown() { return breakdown; }
}
