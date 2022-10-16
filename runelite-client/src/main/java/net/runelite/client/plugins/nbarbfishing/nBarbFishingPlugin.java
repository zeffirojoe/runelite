/*
 * Copyright (c) 2019 Hydrox6 <ikada@protonmail.ch>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.nbarbfishing;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;

import com.google.inject.Inject;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.extutils.ExtUtils;

import static net.runelite.api.AnimationID.*;
import static net.runelite.api.AnimationID.MAGIC_LUNAR_SHARED;

@PluginDescriptor(
		name = "nBarbFishing",
		description = "Barb Fishing helper",
		tags = {"barb", "fishing", "mattie"}
)
public class nBarbFishingPlugin extends Plugin
{

	//ExtUtils such as clicking and grabbing info
	@javax.inject.Inject
	private ExtUtils extUtils;
	@com.google.inject.Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	private Instant lastAnimating;
	private int lastAnimation = IDLE;
	private Instant lastInteracting;
	private Actor lastInteract;
	private Instant lastMoving;
	private WorldPoint lastPosition;
	private boolean notifyPosition = false;
	private boolean droppingInv = false;
	private int lastCombatCountdown = 0;

	@Override
	protected void startUp() throws Exception
	{
		// start up
	}

	@Override
	protected void shutDown() throws Exception
	{
		// shut down
	}

	private boolean checkMovementIdle(Duration waitDuration, Player local)
	{
		if (lastPosition == null)
		{
			lastPosition = local.getWorldLocation();
			return false;
		}

		WorldPoint position = local.getWorldLocation();

		if (lastPosition.equals(position))
		{
			if (notifyPosition
					&& local.getAnimation() == IDLE
					&& Instant.now().compareTo(lastMoving.plus(waitDuration)) >= 0)
			{
				notifyPosition = false;
				// Return true only if we weren't just breaking out of an animation
				return lastAnimation == IDLE;
			}
		}
		else
		{
			notifyPosition = true;
			lastPosition = position;
			lastMoving = Instant.now();
		}

		return false;
	}
	private boolean checkInteractionIdle(Duration waitDuration, Player local)
	{
		if (lastInteract == null)
		{
			return false;
		}

		final Actor interact = local.getInteracting();

		if (interact == null)
		{
			if (lastInteracting != null
					&& Instant.now().compareTo(lastInteracting.plus(waitDuration)) >= 0
					&& lastCombatCountdown == 0)
			{
				lastInteract = null;
				lastInteracting = null;

				// prevent animation notifications from firing too
				lastAnimation = IDLE;
				lastAnimating = null;

				return true;
			}
		}
		else
		{
			lastInteracting = Instant.now();
		}

		return false;
	}
	private boolean checkAnimationIdle(Duration waitDuration, Player local)
	{
		if (lastAnimation == IDLE)
		{
			return false;
		}

		final int animation = local.getAnimation();

		if (animation == IDLE)
		{
			if (lastAnimating != null && Instant.now().compareTo(lastAnimating.plus(waitDuration)) >= 0)
			{
				lastAnimation = IDLE;
				lastAnimating = null;

				// prevent interaction notifications from firing too
				lastInteract = null;
				lastInteracting = null;

				return true;
			}
		}
		else
		{
			lastAnimating = Instant.now();
		}

		return false;
	}
	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer != event.getActor())
		{
			return;
		}

		int graphic = localPlayer.getGraphic();
		int animation = localPlayer.getAnimation();
		switch (animation)
		{
			/* Woodcutting */
			case WOODCUTTING_BRONZE:
			case WOODCUTTING_IRON:
			case WOODCUTTING_STEEL:
			case WOODCUTTING_BLACK:
			case WOODCUTTING_MITHRIL:
			case WOODCUTTING_ADAMANT:
			case WOODCUTTING_RUNE:
			case WOODCUTTING_GILDED:
			case WOODCUTTING_DRAGON:
			case WOODCUTTING_DRAGON_OR:
			case WOODCUTTING_INFERNAL:
			case WOODCUTTING_3A_AXE:
			case WOODCUTTING_CRYSTAL:
			case WOODCUTTING_TRAILBLAZER:
				/* Cooking(Fire, Range) */
			case COOKING_FIRE:
			case COOKING_RANGE:
			case COOKING_WINE:
				/* Crafting(Gem Cutting, Glassblowing, Spinning, Weaving, Battlestaves, Pottery) */
			case GEM_CUTTING_OPAL:
			case GEM_CUTTING_JADE:
			case GEM_CUTTING_REDTOPAZ:
			case GEM_CUTTING_SAPPHIRE:
			case GEM_CUTTING_EMERALD:
			case GEM_CUTTING_RUBY:
			case GEM_CUTTING_DIAMOND:
			case GEM_CUTTING_AMETHYST:
			case CRAFTING_GLASSBLOWING:
			case CRAFTING_SPINNING:
			case CRAFTING_LOOM:
			case CRAFTING_BATTLESTAVES:
			case CRAFTING_LEATHER:
			case CRAFTING_POTTERS_WHEEL:
			case CRAFTING_POTTERY_OVEN:
				/* Fletching(Cutting, Stringing, Adding feathers and heads) */
			case FLETCHING_BOW_CUTTING:
			case FLETCHING_STRING_NORMAL_SHORTBOW:
			case FLETCHING_STRING_OAK_SHORTBOW:
			case FLETCHING_STRING_WILLOW_SHORTBOW:
			case FLETCHING_STRING_MAPLE_SHORTBOW:
			case FLETCHING_STRING_YEW_SHORTBOW:
			case FLETCHING_STRING_MAGIC_SHORTBOW:
			case FLETCHING_STRING_NORMAL_LONGBOW:
			case FLETCHING_STRING_OAK_LONGBOW:
			case FLETCHING_STRING_WILLOW_LONGBOW:
			case FLETCHING_STRING_MAPLE_LONGBOW:
			case FLETCHING_STRING_YEW_LONGBOW:
			case FLETCHING_STRING_MAGIC_LONGBOW:
			case FLETCHING_ATTACH_FEATHERS_TO_ARROWSHAFT:
			case FLETCHING_ATTACH_HEADS:
			case FLETCHING_ATTACH_BOLT_TIPS_TO_BRONZE_BOLT:
			case FLETCHING_ATTACH_BOLT_TIPS_TO_IRON_BROAD_BOLT:
			case FLETCHING_ATTACH_BOLT_TIPS_TO_BLURITE_BOLT:
			case FLETCHING_ATTACH_BOLT_TIPS_TO_STEEL_BOLT:
			case FLETCHING_ATTACH_BOLT_TIPS_TO_MITHRIL_BOLT:
			case FLETCHING_ATTACH_BOLT_TIPS_TO_ADAMANT_BOLT:
			case FLETCHING_ATTACH_BOLT_TIPS_TO_RUNE_BOLT:
			case FLETCHING_ATTACH_BOLT_TIPS_TO_DRAGON_BOLT:
				/* Smithing(Anvil, Furnace, Cannonballs */
			case SMITHING_ANVIL:
			case SMITHING_IMCANDO_HAMMER:
			case SMITHING_SMELTING:
			case SMITHING_CANNONBALL:
				/* Fishing */
			case FISHING_CRUSHING_INFERNAL_EELS:
			case FISHING_CUTTING_SACRED_EELS:
			case FISHING_BIG_NET:
			case FISHING_NET:
			case FISHING_POLE_CAST:
			case FISHING_CAGE:
			case FISHING_HARPOON:
			case FISHING_BARBTAIL_HARPOON:
			case FISHING_DRAGON_HARPOON:
			case FISHING_DRAGON_HARPOON_OR:
			case FISHING_INFERNAL_HARPOON:
			case FISHING_CRYSTAL_HARPOON:
			case FISHING_TRAILBLAZER_HARPOON:
			case FISHING_OILY_ROD:
			case FISHING_KARAMBWAN:
			case FISHING_BAREHAND:
			case FISHING_PEARL_ROD:
			case FISHING_PEARL_FLY_ROD:
			case FISHING_PEARL_BARBARIAN_ROD:
			case FISHING_PEARL_ROD_2:
			case FISHING_PEARL_FLY_ROD_2:
			case FISHING_PEARL_BARBARIAN_ROD_2:
			case FISHING_PEARL_OILY_ROD:
			case FISHING_BARBARIAN_ROD:
				/* Mining(Normal) */
			case MINING_BRONZE_PICKAXE:
			case MINING_IRON_PICKAXE:
			case MINING_STEEL_PICKAXE:
			case MINING_BLACK_PICKAXE:
			case MINING_MITHRIL_PICKAXE:
			case MINING_ADAMANT_PICKAXE:
			case MINING_RUNE_PICKAXE:
			case MINING_GILDED_PICKAXE:
			case MINING_DRAGON_PICKAXE:
			case MINING_DRAGON_PICKAXE_UPGRADED:
			case MINING_DRAGON_PICKAXE_OR:
			case MINING_DRAGON_PICKAXE_OR_TRAILBLAZER:
			case MINING_INFERNAL_PICKAXE:
			case MINING_3A_PICKAXE:
			case MINING_CRYSTAL_PICKAXE:
			case MINING_TRAILBLAZER_PICKAXE:
			case MINING_TRAILBLAZER_PICKAXE_2:
			case MINING_TRAILBLAZER_PICKAXE_3:
			case DENSE_ESSENCE_CHIPPING:
			case DENSE_ESSENCE_CHISELING:
				/* Mining(Motherlode) */
			case MINING_MOTHERLODE_BRONZE:
			case MINING_MOTHERLODE_IRON:
			case MINING_MOTHERLODE_STEEL:
			case MINING_MOTHERLODE_BLACK:
			case MINING_MOTHERLODE_MITHRIL:
			case MINING_MOTHERLODE_ADAMANT:
			case MINING_MOTHERLODE_RUNE:
			case MINING_MOTHERLODE_GILDED:
			case MINING_MOTHERLODE_DRAGON:
			case MINING_MOTHERLODE_DRAGON_UPGRADED:
			case MINING_MOTHERLODE_DRAGON_OR:
			case MINING_MOTHERLODE_DRAGON_OR_TRAILBLAZER:
			case MINING_MOTHERLODE_INFERNAL:
			case MINING_MOTHERLODE_3A:
			case MINING_MOTHERLODE_CRYSTAL:
			case MINING_MOTHERLODE_TRAILBLAZER:
				/* Herblore */
			case HERBLORE_PESTLE_AND_MORTAR:
			case HERBLORE_POTIONMAKING:
			case HERBLORE_MAKE_TAR:
				/* Magic */
			case MAGIC_CHARGING_ORBS:
			case MAGIC_LUNAR_PLANK_MAKE:
			case MAGIC_LUNAR_STRING_JEWELRY:
			case MAGIC_MAKE_TABLET:
			case MAGIC_ENCHANTING_JEWELRY:
			case MAGIC_ENCHANTING_AMULET_1:
			case MAGIC_ENCHANTING_AMULET_2:
			case MAGIC_ENCHANTING_AMULET_3:
			case MAGIC_ENCHANTING_BOLTS:
				/* Prayer */
			case USING_GILDED_ALTAR:
			case ECTOFUNTUS_FILL_SLIME_BUCKET:
			case ECTOFUNTUS_INSERT_BONES:
			case ECTOFUNTUS_GRIND_BONES:
			case ECTOFUNTUS_EMPTY_BIN:
				/* Farming */
			case FARMING_MIX_ULTRACOMPOST:
			case FARMING_HARVEST_BUSH:
			case FARMING_HARVEST_HERB:
			case FARMING_HARVEST_FRUIT_TREE:
			case FARMING_HARVEST_FLOWER:
			case FARMING_HARVEST_ALLOTMENT:
				/* Misc */
			case PISCARILIUS_CRANE_REPAIR:
			case HOME_MAKE_TABLET:
			case SAND_COLLECTION:
			case LOOKING_INTO:
				resetTimers();
				lastAnimation = animation;
				lastAnimating = Instant.now();
				break;
			case MAGIC_LUNAR_SHARED:
				if (graphic == GraphicID.BAKE_PIE)
				{
					resetTimers();
					lastAnimation = animation;
					lastAnimating = Instant.now();
					break;
				}
			case IDLE:
				lastAnimating = Instant.now();
				break;
			default:
				// On unknown animation simply assume the animation is invalid and dont throw notification
				lastAnimation = IDLE;
				lastAnimating = null;
		}
	}
	private void resetTimers()
	{
		final Player local = client.getLocalPlayer();

		// Reset animation idle timer
		lastAnimating = null;
		if (client.getGameState() == GameState.LOGIN_SCREEN || local == null || local.getAnimation() != lastAnimation)
		{
			lastAnimation = IDLE;
		}

		// Reset interaction idle timer
		lastInteracting = null;
		if (client.getGameState() == GameState.LOGIN_SCREEN || local == null || local.getInteracting() != lastInteract)
		{
			lastInteract = null;
		}
	}
	private void dropFish() {
		int returnedTime = extUtils.clickAllInvItems(new int[]{11330, 11328, 11332});
		extUtils.clickNPC(new int[]{1542}, returnedTime);
	}
	@Subscribe
	private void onGameTick(GameTick gameTick)
	{
		final Player local = client.getLocalPlayer();
		final Duration waitDuration = Duration.ofMillis(extUtils.getRandomIntBetweenRange(1250, 2000));

		if (checkMovementIdle(waitDuration, local) ||
			checkInteractionIdle(waitDuration, local) ||
			checkAnimationIdle(waitDuration, local)) {
			dropFish();
		}
	}
}
