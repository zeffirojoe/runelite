package net.runelite.client.plugins.joemining;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("JoeMiningConfig")

public interface JoeMiningConfig extends Config
{
	@ConfigItem(
			keyName = "mining",
			name = "Mining Active",
			description = "Mine ore",
			position = 0
	)
	default boolean getMiningActive()
	{
		return false;
	}

}