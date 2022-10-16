/*
 * Copyright (c) 2019-2020, ganom <https://github.com/Ganom>
 * All rights reserved.
 * Licensed under GPL3, see LICENSE for the full scope.
 */
package net.runelite.client.plugins.extutils;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Point;
import net.runelite.api.*;
import net.runelite.api.queries.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@PluginDescriptor(
		name = "ExtUtils",
		hidden = true
)
@Slf4j
@SuppressWarnings("unused")
@Singleton
public class ExtUtils extends Plugin
{
	@Inject
	private Client client;

	@Override
	protected void startUp()
	{

	}

	@Override
	protected void shutDown()
	{

	}

	public int[] stringToIntArray(String string)
	{
		return Arrays.stream(string.split(",")).map(String::trim).mapToInt(Integer::parseInt).toArray();
	}

	@Nullable
	public NPC findNearestNPC(int... ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new NPCQuery()
				.idEquals(ids)
				.result(client)
				.nearestTo(client.getLocalPlayer());
	}

	@Nullable
	public GameObject findNearestGameObject(int... ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new GameObjectQuery()
				.idEquals(ids)
				.result(client)
				.nearestTo(client.getLocalPlayer());
	}

	@Nullable
	public WallObject findNearestWallObject(int... ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new WallObjectQuery()
				.idEquals(ids)
				.result(client)
				.nearestTo(client.getLocalPlayer());
	}

	@Nullable
	public DecorativeObject findNearestDecorObject(int... ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new DecorativeObjectQuery()
				.idEquals(ids)
				.result(client)
				.nearestTo(client.getLocalPlayer());
	}

	@Nullable
	public GroundObject findNearestGroundObject(int... ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new GroundObjectQuery()
				.idEquals(ids)
				.result(client)
				.nearestTo(client.getLocalPlayer());
	}

	public List<GameObject> getGameObjects(int... ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return new ArrayList<>();
		}

		return new GameObjectQuery()
				.idEquals(ids)
				.result(client)
				.list;
	}

	public List<WallObject> getWallObjects(int... ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return new ArrayList<>();
		}

		return new WallObjectQuery()
				.idEquals(ids)
				.result(client)
				.list;
	}

	public List<DecorativeObject> getDecorObjects(int... ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return new ArrayList<>();
		}

		return new DecorativeObjectQuery()
				.idEquals(ids)
				.result(client)
				.list;
	}

	public List<GroundObject> getGroundObjects(int... ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return new ArrayList<>();
		}

		return new GroundObjectQuery()
				.idEquals(ids)
				.result(client)
				.list;
	}

	@Nullable
	public TileObject findNearestObject(int... ids)
	{
		GameObject gameObject = findNearestGameObject(ids);

		if (gameObject != null)
		{
			return gameObject;
		}

		WallObject wallObject = findNearestWallObject(ids);

		if (wallObject != null)
		{
			return wallObject;
		}
		DecorativeObject decorativeObject = findNearestDecorObject(ids);

		if (decorativeObject != null)
		{
			return decorativeObject;
		}

		return findNearestGroundObject(ids);
	}

	public boolean checkInvFull()
	{
		final ItemContainer itemContainer = client.getItemContainer(InventoryID.INVENTORY);

		if (itemContainer == null)
			return false;

		final Item[] items = itemContainer.getItems();

		return items.length >= 28;
	}

	public List<WidgetItem> getItems(int... itemIDs)
	{
		assert client.isClientThread();

		return new InventoryWidgetItemQuery()
				.idEquals(itemIDs)
				.result(client)
				.list;
	}

	public List<Widget> getEquippedItems(int[] itemIds)
	{
		assert client.isClientThread();

		Widget equipmentWidget = client.getWidget(WidgetInfo.EQUIPMENT);

		List<Integer> equippedIds = new ArrayList<>();

		for (int i : itemIds)
		{
			equippedIds.add(i);
		}

		List<Widget> equipped = new ArrayList<>();

		if (equipmentWidget.getStaticChildren() != null)
		{
			for (Widget widgets : equipmentWidget.getStaticChildren())
			{
				for (Widget items : widgets.getDynamicChildren())
				{
					if (equippedIds.contains(items.getItemId()))
					{
						equipped.add(items);
					}
				}
			}
		}

		return equipped;
	}

	public int getTabHotkey(Tab tab)
	{
		assert client.isClientThread();

		final int var = client.getVarbitValue(client.getVarps(), tab.getVarbit());
		final int offset = 111;

		switch (var)
		{
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
			case 8:
			case 9:
			case 10:
			case 11:
			case 12:
				return var + offset;
			case 13:
				return 27;
			default:
				return -1;
		}
	}

	public WidgetInfo getSpellWidgetInfo(String spell)
	{
		assert client.isClientThread();
		return Spells.getWidget(spell);
	}

	public WidgetInfo getPrayerWidgetInfo(String spell)
	{
		assert client.isClientThread();
		return PrayerMap.getWidget(spell);
	}

	public Widget getSpellWidget(String spell)
	{
		assert client.isClientThread();
		return client.getWidget(Spells.getWidget(spell));
	}

	public Widget getPrayerWidget(String spell)
	{
		assert client.isClientThread();
		return client.getWidget(PrayerMap.getWidget(spell));
	}
	/*
	Series of Functions that click specific things in the world.
	 */

	//Clicks all items that matche the ID in user INV
	public int clickAllInvItems(int[] ITEM_IDS)
	{
		assert client.isClientThread();

		Widget invWidget = client.getWidget(WidgetInfo.INVENTORY.getPackedId());

		Widget[] childrenWidgets = invWidget.getChildren();

		int timer = 142;

		for(Widget child : childrenWidgets){
			for(int itemId : ITEM_IDS){
				if(itemId == child.getItemId()){
					int finalTimer = timer;
					(new Thread(() -> {
						try
						{
							Thread.sleep(getRandomIntBetweenRange(finalTimer, finalTimer +278));
						}
						catch (InterruptedException e)
						{
							e.printStackTrace();
						}
						click(child.getBounds());
					})).start();
					timer += 420;
				}
			}
		}
		return timer;
	}

	//Clicks the closest npc after x time.
	public void clickNPC(int[] NPC_IDS, int time)
	{
		assert client.isClientThread();

		NPC pickpocket_npc = findNearestNPC(NPC_IDS); //Change this to Ardy Knights
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
				Thread.sleep(getRandomIntBetweenRange(time, time+278));
				click(npc_rect);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		})).start();
	}

	//Clicks the closest IP that matches the id's given
	public void clickNPC(int[] NPC_IDS)
	{
		assert client.isClientThread();

		NPC pickpocket_npc = findNearestNPC(NPC_IDS); //Change this to Ardy Knights
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
				Thread.sleep(getRandomIntBetweenRange(142, 523));
				click(npc_rect);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		})).start();
	}

	//clicks the closest wall object that matches the IDS given
	private void clickWallObject(int[] WALL_IDS)
	{
		assert client.isClientThread();

		WallObject near_wall_obj = findNearestWallObject(WALL_IDS); //Change this to Ardy Knights
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
				Thread.sleep(getRandomIntBetweenRange(142, 523));
				click(npc_rect);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		})).start();
	}

	@Nullable
	public boolean findAndClickNearestObj(int[] object_ids)
	{
		if (object_ids.length < 1)
		{
			return false;
		}

		Player player = client.getLocalPlayer();
		if (client.getLocalPlayer() == null)
		{
			return false;
		}

		int smallest_dist = Integer.MAX_VALUE;
		TileObject object_to_click = null;
		for (int id : object_ids) {
			TileObject tmp = findNearestObject(id);
			if (tmp == null)
			{
				continue;
			}

			int dist = player.getLocalLocation().distanceTo(tmp.getLocalLocation());
			if (dist < smallest_dist)
			{
				object_to_click = tmp;
				smallest_dist = dist;
			}
		}

		if(object_to_click == null || smallest_dist > 4000 )
			return false;

		clickTileObject(object_to_click);

		return true;
	}
	public void clickTileObject(TileObject TILE_OBJ)
	{
		assert client.isClientThread();

		Shape tile_obj_shape = TILE_OBJ.getClickbox();
		if (tile_obj_shape == null)
			return;

		Rectangle tile_obj_rect = tile_obj_shape.getBounds();
		if (tile_obj_rect == null)
			return;

		(new Thread(() ->
		{
			try
			{
				Thread.sleep(getRandomIntBetweenRange(142, 523));
				click(tile_obj_rect);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		})).start();
	}

	/**
	 * This method must be called on a new
	 * thread, if you try to call it on
	 * {@link net.runelite.client.callback.ClientThread}
	 * it will result in a crash/desynced thread.
	 */
	public void typeString(String string)
	{
		assert !client.isClientThread();

		for (char c : string.toCharArray())
		{
			pressKey(c);
		}
	}

	public void pressKey(char key)
	{
		keyEvent(401, key);
		keyEvent(402, key);
		keyEvent(400, key);
	}

	private void keyEvent(int id, char key)
	{
		KeyEvent e = new KeyEvent(
				client.getCanvas(), id, System.currentTimeMillis(),
				0, KeyEvent.VK_UNDEFINED, key
		);

		client.getCanvas().dispatchEvent(e);
	}

	/**
	 * This method must be called on a new
	 * thread, if you try to call it on
	 * {@link net.runelite.client.callback.ClientThread}
	 * it will result in a crash/desynced thread.
	 */
	public void click(Rectangle rectangle)
	{
		assert !client.isClientThread();
		Point point = getClickPoint(rectangle);
		try
		{
			click(point);
		}
		catch (Exception ignored)
		{
			//Just return it. Should never get hit anyway tbh.
		}
	}

	public void click(Point p) throws InterruptedException
	{
		assert !client.isClientThread();

		if (client.isStretchedEnabled())
		{
			final Dimension stretched = client.getStretchedDimensions();
			final Dimension real = client.getRealDimensions();
			final double width = (stretched.width / real.getWidth());
			final double height = (stretched.height / real.getHeight());
			final Point point = new Point((int) (p.getX() * width), (int) (p.getY() * height));
			mouseEvent(503, point);
			Thread.sleep(getRandomIntBetweenRange(50, 200));
			mouseEvent(501, point);
			mouseEvent(502, point);
			mouseEvent(500, point);
			return;
		}
		mouseEvent(503, p);
		Thread.sleep(getRandomIntBetweenRange(50, 200));
		mouseEvent(501, p);
		mouseEvent(502, p);
		mouseEvent(500, p);
	}

	public Point getClickPoint(@NotNull Rectangle rect)
	{
		final int x = (int) (rect.getX() + getRandomIntBetweenRange((int) rect.getWidth() / 6 * -1, (int) rect.getWidth() / 6) + rect.getWidth() / 2);
		final int y = (int) (rect.getY() + getRandomIntBetweenRange((int) rect.getHeight() / 6 * -1, (int) rect.getHeight() / 6) + rect.getHeight() / 2);

		return new Point(x, y);
	}

	public int getRandomIntBetweenRange(int min, int max)
	{
		return (int) ((Math.random() * ((max - min) + 1)) + min);
	}

	private void mouseEvent(int id, @NotNull Point point)
	{
		MouseEvent e = new MouseEvent(
				client.getCanvas(), id,
				System.currentTimeMillis(),
				0, point.getX(), point.getY(),
				1, false, 1
		);

		client.getCanvas().dispatchEvent(e);
	}
}
