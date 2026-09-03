package com.liveon;

import java.util.List;
import java.util.Map;

/**
 * NightLegion copy of Reval's public points response contract.
 * Adapted from revalOSRS/reval-cc-plugin at 6033d3188b18d34f4bd4c28e6cf7986c8b95f0f9.
 */
final class PointsResponse
{
	String status;
	String sourceCommit;
	PointsData data;

	static final class PointsData
	{
		Map<String, List<PointSource>> pointSources;
		List<Rank> ranks;
	}

	static final class Rank
	{
		String name;
		String displayName;
		int pointsRequired;
		int maintenancePerMonth;
		List<AdditionalRequirement> additionalRequirements;

		static final class AdditionalRequirement
		{
			String description;
			List<String> anyOf;
		}
	}

	static final class PointSource
	{
		String id;
		String name;
		String pointsDescription;
		String description;
		boolean repeatable;
		Integer points;
		Integer threshold;
		String icon;
		PointSourceMetadata metadata;

		String getPointsDisplay()
		{
			return pointsDescription == null ? "" : pointsDescription;
		}
	}

	static final class PointSourceMetadata
	{
		Integer itemId;
		String category;
		String source;
	}
}
