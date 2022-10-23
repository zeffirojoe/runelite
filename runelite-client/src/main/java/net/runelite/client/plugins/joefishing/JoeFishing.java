package net.runelite.client.plugins.joefishing;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.extutils.ExtUtils;

import javax.inject.Inject;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static net.runelite.api.AnimationID.*;


@PluginDescriptor(
		name = "Joe Fishing",
		description = "Joe's Fishing Plugin",
		enabledByDefault = false,
		tags = {"weed", "smoke", "cheat", "fishing"}
)
@Slf4j
public class JoeFishing extends Plugin
{
	// Injects our config
	@Inject
	private JoeFishingConfig config;

	//Inject Client to use
	@Inject
	private Client client;

	//Inject Client to use
	@Inject
	private ClientThread clientThread;

	//ExtUtils such as clicking and grabbing info
	@Inject
	private ExtUtils extUtils;

	//Notifier
	@Inject
	private Notifier notifier;

	// Provides the config
	@Provides
	JoeFishingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(JoeFishingConfig.class);
	}

	private static final int LOGOUT_WARNING_MILLIS = (4 * 60 + 40) * 1000; // 4 minutes and 40 seconds
	private static final int COMBAT_WARNING_MILLIS = 19 * 60 * 1000; // 19 minutes
	private static final int LOGOUT_WARNING_CLIENT_TICKS = LOGOUT_WARNING_MILLIS / Constants.CLIENT_TICK_LENGTH;
	private static final int COMBAT_WARNING_CLIENT_TICKS = COMBAT_WARNING_MILLIS / Constants.CLIENT_TICK_LENGTH;

	private static final int HIGHEST_MONSTER_ATTACK_SPEED = 8; // Except Scarab Mage, but they are with other monsters
	private static final Duration SIX_HOUR_LOGOUT_WARNING_AFTER_DURATION = Duration.ofMinutes(340);
	private static final String FISHING_SPOT = "Fishing spot";


	private Instant lastAnimating;
	private int lastAnimation = IDLE;
	private Instant lastInteracting;
	private Actor lastInteract;
	private Instant lastMoving;
	private WorldPoint lastPosition;
	private boolean notifyPosition = false;
	private boolean notifyIdleLogout = true;
	private int lastCombatCountdown = 0;
	private Instant sixHourWarningTime;
	private boolean ready;

	@Override
	protected void startUp() throws Exception
	{
		// can't tell when 6hr will be if enabled while already logged in
		sixHourWarningTime = null;
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
				/* Fishing(Normal) */
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
				/* Fishing(Motherlode) */
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

	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		final Actor source = event.getSource();
		if (source != client.getLocalPlayer())
		{
			return;
		}

		final Actor target = event.getTarget();

		// Reset last interact
		if (target != null)
		{
			lastInteract = null;
		}
		else
		{
			lastInteracting = Instant.now();
		}

		final boolean isNpc = target instanceof NPC;

		// If this is not NPC, do not process as we are not interested in other entities
		if (!isNpc)
		{
			return;
		}

		final NPC npc = (NPC) target;
		final NPCComposition npcComposition = npc.getComposition();
		final List<String> npcMenuActions = Arrays.asList(npcComposition.getActions());

		boolean lastInteractWasCombat;
		if (npcMenuActions.contains("Attack"))
		{
			// Player is most likely in combat with attack-able NPC
			resetTimers();
			lastInteract = target;
			lastInteracting = Instant.now();
			lastInteractWasCombat = true;
		}
		else if (target.getName() != null && target.getName().contains(FISHING_SPOT))
		{
			// Player is fishing
			resetTimers();
			lastInteract = target;
			lastInteracting = Instant.now();
			lastInteractWasCombat = false;
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		lastInteracting = null;

		GameState state = gameStateChanged.getGameState();

		switch (state)
		{
			case LOGIN_SCREEN:
				resetTimers();
				break;
			case LOGGING_IN:
			case HOPPING:
			case CONNECTION_LOST:
				ready = true;
				break;
			case LOGGED_IN:
				if (ready)
				{
					sixHourWarningTime = Instant.now().plus(SIX_HOUR_LOGOUT_WARNING_AFTER_DURATION);
					ready = false;
					resetTimers();
				}

				break;
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (event.getActor() != client.getLocalPlayer())
		{
			return;
		}

		final Hitsplat hitsplat = event.getHitsplat();

		if (hitsplat.getHitsplatType() == HitsplatID.DAMAGE_ME
				|| hitsplat.getHitsplatType() == HitsplatID.BLOCK_ME)
		{
			lastCombatCountdown = HIGHEST_MONSTER_ATTACK_SPEED;
		}
	}

	@Subscribe
	public void onGraphicChanged(GraphicChanged event)
	{
		Actor actor = event.getActor();

		if (actor != client.getLocalPlayer())
		{
			return;
		}

		if (actor.getGraphic() == GraphicID.SPLASH)
		{
			lastCombatCountdown = HIGHEST_MONSTER_ATTACK_SPEED;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) throws InterruptedException
	{
		final Player local = client.getLocalPlayer();
		final Duration waitDuration = Duration.ofMillis(extUtils.getRandomIntBetweenRange(1250, 2000));
		lastCombatCountdown = Math.max(lastCombatCountdown - 1, 0);

		if (client.getGameState() != GameState.LOGGED_IN
				|| local == null)
		{
			resetTimers();
			return;
		}

		if (client.getWidget(WidgetInfo.LEVEL_UP_LEVEL) != null)
		{
			fishing();
		}

		if (checkIdleLogout())
		{
			fishing();
		}
		if (checkAnimationIdle(waitDuration, local))
		{
			fishing();
		}
		if (checkMovementIdle(waitDuration, local))
		{
			fishing();
		}
		if (checkInteractionIdle(waitDuration, local))
		{
			fishing();
		}
	}

	private void fishing()
	{
		int returnedTime = 0;
		if (checkInvFull())
		{
			returnedTime = extUtils.clickAllInvItems(new int[]{ItemID.RAW_ANCHOVIES, ItemID.RAW_SHRIMPS});
		}
		extUtils.clickNPC(new int[]{1530}, returnedTime);
	}

	private boolean checkInvFull()
	{
		final ItemContainer itemContainer = client.getItemContainer(InventoryID.INVENTORY);

		if (itemContainer == null)
			return false;

		final Item[] items = itemContainer.getItems();

		return items.length >= 28;
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

	private boolean checkIdleLogout()
	{
		// Check clientside AFK first, because this is required for the server to disconnect you for being first
		int idleClientTicks = client.getKeyboardIdleTicks();
		if (client.getMouseIdleTicks() < idleClientTicks)
		{
			idleClientTicks = client.getMouseIdleTicks();
		}

		if (idleClientTicks < LOGOUT_WARNING_CLIENT_TICKS)
		{
			notifyIdleLogout = true;
			return false;
		}

		// If we are not receiving hitsplats then we can be afk kicked
		if (lastCombatCountdown <= 0)
		{
			boolean warn = notifyIdleLogout;
			notifyIdleLogout = false;
			return warn;
		}

		// We are in combat, so now we have to check for the timer that knocks you out of combat
		// I think there are other conditions that I don't know about, because during testing I just didn't
		// get removed from combat sometimes.
		final long lastInteractionAgo = System.currentTimeMillis() - client.getMouseLastPressedMillis();
		if (lastInteractionAgo < COMBAT_WARNING_MILLIS || client.getKeyboardIdleTicks() < COMBAT_WARNING_CLIENT_TICKS)
		{
			notifyIdleLogout = true;
			return false;
		}

		boolean warn = notifyIdleLogout;
		notifyIdleLogout = false;
		return warn;
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

	private void clickInvItem(int[] ITEM_IDS)
	{
		List<WidgetItem> items = extUtils.getItems(ITEM_IDS);
		if (items == null)
			return;

		WidgetItem coin_pouch = items.get(0);
		if (coin_pouch == null)
			return;

		Rectangle coin_pouch_rect = coin_pouch.getCanvasBounds();
		if (coin_pouch_rect == null)
			return;

		(new Thread(() -> {
			try
			{
				Thread.sleep(extUtils.getRandomIntBetweenRange(142, 420));
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			extUtils.click(coin_pouch_rect);
		})).start();
	}

	private void clickNPC(int[] NPC_IDS)
	{
		NPC pickpocket_npc = extUtils.findNearestNPC(NPC_IDS); //Change this to Ardy Knights
		if (pickpocket_npc == null)
			return;

		Shape npc_shape = pickpocket_npc.getConvexHull();
		if (npc_shape == null)
			return;

		Rectangle npc_rect = npc_shape.getBounds();
		if (npc_rect == null)
			return;

		(new Thread(() -> {
			try
			{
				Thread.sleep(extUtils.getRandomIntBetweenRange(142, 523));
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			extUtils.click(npc_rect);
		})).start();
	}

	private void clickWallObject(int[] WALL_IDS)
	{
		WallObject near_wall_obj = extUtils.findNearestWallObject(WALL_IDS);
		if (near_wall_obj == null)
			return;

		Shape npc_shape = near_wall_obj.getConvexHull();
		if (npc_shape == null)
			return;

		Rectangle npc_rect = npc_shape.getBounds();
		if (npc_rect == null)
			return;

		(new Thread(() -> {
			try
			{
				Thread.sleep(extUtils.getRandomIntBetweenRange(142, 523));
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			extUtils.click(npc_rect);
		})).start();
	}
}