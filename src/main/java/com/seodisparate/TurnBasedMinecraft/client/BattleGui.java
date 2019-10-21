package com.seodisparate.TurnBasedMinecraft.client;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.seodisparate.TurnBasedMinecraft.common.Battle;
import com.seodisparate.TurnBasedMinecraft.common.Combatant;
import com.seodisparate.TurnBasedMinecraft.common.Config;
import com.seodisparate.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import com.seodisparate.TurnBasedMinecraft.common.networking.PacketBattleDecision;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.StringTextComponent;

public class BattleGui extends Screen {
	private AtomicInteger timeRemaining;
	private long lastInstant;
	private long elapsedTime;
	private MenuState state;
	private boolean stateChanged;
	private String info;

	private enum MenuState {
		MAIN_MENU(0), ATTACK_TARGET(1), ITEM_ACTION(2), WAITING(3), SWITCH_ITEM(4), USE_ITEM(5);

		private int value;

		MenuState(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

		private static Map<Integer, MenuState> map;

		static {
			map = new HashMap<Integer, MenuState>();
			for (MenuState state : MenuState.values()) {
				map.put(state.getValue(), state);
			}
		}

		public static MenuState valueOf(int value) {
			return map.get(value);
		}
	}

	private enum ButtonAction {
		ATTACK(0), DEFEND(1), ITEM(2), FLEE(3), ATTACK_TARGET(4), SWITCH_HELD_ITEM(5), DECIDE_USE_ITEM(6), CANCEL(7),
		DO_ITEM_SWITCH(8), DO_USE_ITEM(9);

		private int value;

		ButtonAction(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

		private static Map<Integer, ButtonAction> map;
		static {
			map = new HashMap<Integer, ButtonAction>();
			for (ButtonAction action : ButtonAction.values()) {
				map.put(action.getValue(), action);
			}
		}

		public static ButtonAction valueOf(int value) {
			return map.get(value);
		}
	}

	public BattleGui() {
		super(new StringTextComponent("Battle Gui"));
		timeRemaining = new AtomicInteger((int) (Config.BATTLE_DECISION_DURATION_NANO_DEFAULT / 1000000000L));
		lastInstant = System.nanoTime();
		elapsedTime = 0;
		state = MenuState.MAIN_MENU;
		stateChanged = true;
	}

	private void setState(MenuState state) {
		this.state = state;
		stateChanged = true;
	}

	public void turnBegin() {
		if (TurnBasedMinecraftMod.proxy.getLocalBattle() != null) {
			TurnBasedMinecraftMod.proxy.getLocalBattle().setState(Battle.State.ACTION);
		}
		setState(MenuState.WAITING);
	}

	public void turnEnd() {
		if (TurnBasedMinecraftMod.proxy.getLocalBattle() != null) {
			TurnBasedMinecraftMod.proxy.getLocalBattle().setState(Battle.State.DECISION);
		}
		timeRemaining.set(TurnBasedMinecraftMod.proxy.getConfig().getDecisionDurationSeconds());
		elapsedTime = 0;
		lastInstant = System.nanoTime();
		setState(MenuState.MAIN_MENU);
	}

	public void battleChanged() {
		stateChanged = true;
	}

	public void updateState() {
		if (!stateChanged) {
			return;
		}

		stateChanged = false;
		buttons.clear();
		children.clear();
		switch (state) {
		case MAIN_MENU:
			info = "What will you do?";
			addButton(new Button(width * 3 / 7 - 25, 40, 50, 20, "Attack", (button) -> {
				buttonActionEvent(button, ButtonAction.ATTACK);
			}));
			addButton(new Button(width * 4 / 7 - 25, 40, 50, 20, "Defend", (button) -> {
				buttonActionEvent(button, ButtonAction.DEFEND);
			}));
			addButton(new Button(width * 3 / 7 - 25, 60, 50, 20, "Item", (button) -> {
				buttonActionEvent(button, ButtonAction.ITEM);
			}));
			addButton(new Button(width * 4 / 7 - 25, 60, 50, 20, "Flee", (button) -> {
				buttonActionEvent(button, ButtonAction.FLEE);
			}));
			break;
		case ATTACK_TARGET:
			info = "Who will you attack?";
			int y = 30;
			try {
				for (Map.Entry<Integer, Combatant> e : TurnBasedMinecraftMod.proxy.getLocalBattle()
						.getSideAEntrySet()) {
					if (e.getValue().entity != null) {
						addButton(new EntitySelectionButton(width / 4 - 60, y, 120, 20, e.getValue().entity.getName().getString(), e.getKey(), true, (button) -> {
							buttonActionEvent(button, ButtonAction.ATTACK_TARGET);
						}));
					} else {
						addButton(new EntitySelectionButton(width / 4 - 60, y, 120, 20, "Unknown", e.getKey(), true, (button) -> {
							buttonActionEvent(button, ButtonAction.ATTACK_TARGET);
						}));
					}
					y += 20;
				}
			} catch (ConcurrentModificationException e) {
				// ignored
			}
			y = 30;
			try {
				for (Map.Entry<Integer, Combatant> e : TurnBasedMinecraftMod.proxy.getLocalBattle()
						.getSideBEntrySet()) {
					if (e.getValue().entity != null) {
						addButton(new EntitySelectionButton(width * 3 / 4 - 60, y, 120, 20, e.getValue().entity.getName().getString(), e.getKey(), false, (button) -> {
							buttonActionEvent(button, ButtonAction.ATTACK_TARGET);
						}));
					} else {
						addButton(new EntitySelectionButton(width * 3 / 4 - 60, y, 120, 20, "Unknown", e.getKey(), false, (button) -> {
							buttonActionEvent(button, ButtonAction.ATTACK_TARGET);
						}));
					}
					y += 20;
				}
			} catch (ConcurrentModificationException e) {
				// ignored
			}
			addButton(new Button(width / 2 - 30, height - 120, 60, 20, "Cancel", (button) -> {
				buttonActionEvent(button, ButtonAction.CANCEL);
			}));
			break;
		case ITEM_ACTION:
			info = "What will you do with an item?";
			addButton(new Button(width * 1 / 4 - 40, height - 120, 80, 20, "Switch Held", (button) -> {
				buttonActionEvent(button, ButtonAction.SWITCH_HELD_ITEM);
			}));
			addButton(new Button(width * 2 / 4 - 40, height - 120, 80, 20, "Use", (button) -> {
				buttonActionEvent(button, ButtonAction.DECIDE_USE_ITEM);
			}));
			addButton(new Button(width * 3 / 4 - 40, height - 120, 80, 20, "Cancel", (button) -> {
				buttonActionEvent(button, ButtonAction.CANCEL);
			}));
			break;
		case WAITING:
			info = "Waiting...";
			break;
		case SWITCH_ITEM:
			info = "To which item will you switch to?";
			for (int i = 0; i < 9; ++i) {
				addButton(new ItemSelectionButton(width / 2 - 88 + i * 20, height - 19, 16, 16, "", i, (button) -> {
					buttonActionEvent(button, ButtonAction.DO_ITEM_SWITCH);
				}));
			}
			addButton(new Button(width / 2 - 40, height - 120, 80, 20, "Cancel", (button) -> {
				buttonActionEvent(button, ButtonAction.CANCEL);
			}));
			break;
		case USE_ITEM:
			info = "Which item will you use?";
			for (int i = 0; i < 9; ++i) {
				addButton(new ItemSelectionButton(width / 2 - 88 + i * 20, height - 19, 16, 16, "", i, (button) -> {
					buttonActionEvent(button, ButtonAction.DO_USE_ITEM);
				}));
			}
			addButton(new Button(width / 2 - 40, height - 120, 80, 20, "Cancel", (button) -> {
				buttonActionEvent(button, ButtonAction.CANCEL);
			}));
			break;
		}
	}

	@Override
	public void render(int mouseX, int mouseY, float partialTicks) {
		if (TurnBasedMinecraftMod.proxy.getLocalBattle() == null) {
			// drawHoveringText("Waiting...", width / 2 - 50, height / 2);
			getMinecraft().fontRenderer.drawString("Waiting...", width / 2 - 50, height / 2, 0xFFFFFFFF);
			return;
		}
		if (TurnBasedMinecraftMod.proxy.getLocalBattle().getState() == Battle.State.DECISION
				&& timeRemaining.get() > 0) {
			long nextInstant = System.nanoTime();
			elapsedTime += nextInstant - lastInstant;
			lastInstant = nextInstant;
			while (elapsedTime > 1000000000) {
				elapsedTime -= 1000000000;
				timeRemaining.decrementAndGet();
			}
		}

		updateState();

		super.render(mouseX, mouseY, partialTicks);

		String timeRemainingString = "Time remaining: ";
		int timeRemainingInt = timeRemaining.get();
		if (timeRemainingInt > 8) {
			timeRemainingString += "\u00A7a";
		} else if (timeRemainingInt > 4) {
			timeRemainingString += "\u00A7e";
		} else {
			timeRemainingString += "\u00A7c";
		}
		timeRemainingString += Integer.toString(timeRemainingInt);
		int stringWidth = getMinecraft().fontRenderer.getStringWidth(timeRemainingString);
		fill(width / 2 - stringWidth / 2, 5, width / 2 + stringWidth / 2, 15, 0x70000000);
		getMinecraft().fontRenderer.drawString(timeRemainingString, width / 2 - stringWidth / 2, 5, 0xFFFFFFFF);
		stringWidth = getMinecraft().fontRenderer.getStringWidth(info);
		fill(width / 2 - stringWidth / 2, 20, width / 2 + stringWidth / 2, 30, 0x70000000);
		getMinecraft().fontRenderer.drawString(info, width / 2 - stringWidth / 2, 20, 0xFFFFFFFF);
	}

	protected void buttonActionEvent(Button button, ButtonAction action) {
		switch (action) {
		case ATTACK:
			setState(MenuState.ATTACK_TARGET);
			break;
		case DEFEND:
			TurnBasedMinecraftMod.getHandler().sendToServer(new PacketBattleDecision(
					TurnBasedMinecraftMod.proxy.getLocalBattle().getId(), Battle.Decision.DEFEND, 0));
			setState(MenuState.WAITING);
			break;
		case ITEM:
			setState(MenuState.ITEM_ACTION);
			break;
		case FLEE:
			TurnBasedMinecraftMod.getHandler().sendToServer(new PacketBattleDecision(
					TurnBasedMinecraftMod.proxy.getLocalBattle().getId(), Battle.Decision.FLEE, 0));
			setState(MenuState.WAITING);
			break;
		case ATTACK_TARGET:
			if (button instanceof EntitySelectionButton) {
				TurnBasedMinecraftMod.getHandler()
						.sendToServer(new PacketBattleDecision(TurnBasedMinecraftMod.proxy.getLocalBattle().getId(),
								Battle.Decision.ATTACK, ((EntitySelectionButton) button).getID()));
				setState(MenuState.WAITING);
			} else {
				setState(MenuState.MAIN_MENU);
			}
			break;
		case SWITCH_HELD_ITEM:
			setState(MenuState.SWITCH_ITEM);
			break;
		case DECIDE_USE_ITEM:
			setState(MenuState.USE_ITEM);
			break;
		case CANCEL:
			setState(MenuState.MAIN_MENU);
			break;
		case DO_ITEM_SWITCH:
			if (button instanceof ItemSelectionButton) {
				TurnBasedMinecraftMod.getHandler()
						.sendToServer(new PacketBattleDecision(TurnBasedMinecraftMod.proxy.getLocalBattle().getId(),
								Battle.Decision.SWITCH_ITEM, ((ItemSelectionButton) button).getID()));
				if (((ItemSelectionButton) button).getID() >= 0 && ((ItemSelectionButton) button).getID() < 9) {
					Minecraft.getInstance().player.inventory.currentItem = ((ItemSelectionButton) button).getID();
				}
				setState(MenuState.WAITING);
			} else {
				setState(MenuState.MAIN_MENU);
			}
			break;
		case DO_USE_ITEM:
			if (button instanceof ItemSelectionButton) {
				TurnBasedMinecraftMod.getHandler()
						.sendToServer(new PacketBattleDecision(TurnBasedMinecraftMod.proxy.getLocalBattle().getId(),
								Battle.Decision.USE_ITEM, ((ItemSelectionButton) button).getID()));
				setState(MenuState.WAITING);
			} else {
				setState(MenuState.MAIN_MENU);
			}
			break;
		}
	}

	@Override
	protected void init() {
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean keyPressed(int a, int b, int c) {
		if (getMinecraft().player.isCreative()) {
			return super.keyPressed(a, b, c);
		}
		return false; // TODO verify return value
	}

	public void setTimeRemaining(int remaining) {
		timeRemaining.set(remaining);
	}
}
