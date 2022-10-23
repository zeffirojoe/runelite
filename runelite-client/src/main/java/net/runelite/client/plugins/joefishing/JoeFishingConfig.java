package net.runelite.client.plugins.joefishing;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("JoeFishingConfig")

public interface JoeFishingConfig extends Config
{
	@ConfigItem(
			keyName = "fishing",
			name = "Fishing Active",
			description = "Fish lol",
			position = 0
	)
	default boolean getFishingActive()
	{
		return false;
	}

}