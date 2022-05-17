package com.burnedkirby.TurnBasedMinecraft.client;

import com.burnedkirby.TurnBasedMinecraft.common.Battle;
import com.burnedkirby.TurnBasedMinecraft.common.Combatant;
import com.burnedkirby.TurnBasedMinecraft.common.Config;
import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import com.burnedkirby.TurnBasedMinecraft.common.networking.PacketBattleDecision;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
		super(new TextComponent("Battle Gui"));
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
		clearWidgets();
		switch (state) {
		case MAIN_MENU:
			info = "What will you do?";
			addRenderableWidget(new Button(width * 3 / 7 - 25, 40, 50, 20, new TextComponent("Attack"), (button) -> {
				buttonActionEvent(button, ButtonAction.ATTACK);
			}));
			addRenderableWidget(new Button(width * 4 / 7 - 25, 40, 50, 20, new TextComponent("Defend"), (button) -> {
				buttonActionEvent(button, ButtonAction.DEFEND);
			}));
			addRenderableWidget(new Button(width * 3 / 7 - 25, 60, 50, 20, new TextComponent("Item"), (button) -> {
				buttonActionEvent(button, ButtonAction.ITEM);
			}));
			addRenderableWidget(new Button(width * 4 / 7 - 25, 60, 50, 20, new TextComponent("Flee"), (button) -> {
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
						addRenderableWidget(new EntitySelectionButton(width / 4 - 60, y, 120, 20, e.getValue().entity.getName().getString(), e.getKey(), true, (button) -> {
							buttonActionEvent(button, ButtonAction.ATTACK_TARGET);
						}));
					} else {
						addRenderableWidget(new EntitySelectionButton(width / 4 - 60, y, 120, 20, "Unknown", e.getKey(), true, (button) -> {
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
						addRenderableWidget(new EntitySelectionButton(width * 3 / 4 - 60, y, 120, 20, e.getValue().entity.getName().getString(), e.getKey(), false, (button) -> {
							buttonActionEvent(button, ButtonAction.ATTACK_TARGET);
						}));
					} else {
						addRenderableWidget(new EntitySelectionButton(width * 3 / 4 - 60, y, 120, 20, "Unknown", e.getKey(), false, (button) -> {
							buttonActionEvent(button, ButtonAction.ATTACK_TARGET);
						}));
					}
					y += 20;
				}
			} catch (ConcurrentModificationException e) {
				// ignored
			}
			addRenderableWidget(new Button(width / 2 - 30, height - 120, 60, 20, new TextComponent("Cancel"), (button) -> {
				buttonActionEvent(button, ButtonAction.CANCEL);
			}));
			break;
		case ITEM_ACTION:
			info = "What will you do with an item?";
			addRenderableWidget(new Button(width * 1 / 4 - 40, height - 120, 80, 20, new TextComponent("Switch Held"), (button) -> {
				buttonActionEvent(button, ButtonAction.SWITCH_HELD_ITEM);
			}));
			addRenderableWidget(new Button(width * 2 / 4 - 40, height - 120, 80, 20, new TextComponent("Use"), (button) -> {
				buttonActionEvent(button, ButtonAction.DECIDE_USE_ITEM);
			}));
			addRenderableWidget(new Button(width * 3 / 4 - 40, height - 120, 80, 20, new TextComponent("Cancel"), (button) -> {
				buttonActionEvent(button, ButtonAction.CANCEL);
			}));
			break;
		case WAITING:
			info = "Waiting...";
			break;
		case SWITCH_ITEM:
			info = "To which item will you switch to?";
			for (int i = 0; i < 9; ++i) {
				addRenderableWidget(new ItemSelectionButton(width / 2 - 88 + i * 20, height - 19, 16, 16, "", i, (button) -> {
					buttonActionEvent(button, ButtonAction.DO_ITEM_SWITCH);
				}));
			}
			addRenderableWidget(new Button(width / 2 - 40, height - 120, 80, 20, new TextComponent("Cancel"), (button) -> {
				buttonActionEvent(button, ButtonAction.CANCEL);
			}));
			break;
		case USE_ITEM:
			info = "Which item will you use?";
			for (int i = 0; i < 9; ++i) {
				addRenderableWidget(new ItemSelectionButton(width / 2 - 88 + i * 20, height - 19, 16, 16, "", i, (button) -> {
					buttonActionEvent(button, ButtonAction.DO_USE_ITEM);
				}));
			}
			addRenderableWidget(new Button(width / 2 - 40, height - 120, 80, 20, new TextComponent("Cancel"), (button) -> {
				buttonActionEvent(button, ButtonAction.CANCEL);
			}));
			break;
		}
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
		if (TurnBasedMinecraftMod.proxy.getLocalBattle() == null) {
			// drawHoveringText("Waiting...", width / 2 - 50, height / 2);
			drawString(poseStack, "Waiting...", width / 2 - 50, height / 2, 0xFFFFFFFF);
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

		super.render(poseStack, mouseX, mouseY, partialTicks);

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
		int stringWidth = font.width(timeRemainingString);
		fill(poseStack, width / 2 - stringWidth / 2, 5, width / 2 + stringWidth / 2, 15, 0x70000000);
		drawString(poseStack, timeRemainingString, width / 2 - stringWidth / 2, 5, 0xFFFFFFFF);
		stringWidth = font.width(info);
		fill(poseStack, width / 2 - stringWidth / 2, 20, width / 2 + stringWidth / 2, 30, 0x70000000);
		drawString(poseStack, info, width / 2 - stringWidth / 2, 20, 0xFFFFFFFF);
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
					Minecraft.getInstance().player.getInventory().selected = ((ItemSelectionButton) button).getID();
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

	@Override
	public boolean keyReleased(int a, int b, int c) {
		if (getMinecraft().player.isCreative()) {
			return super.keyReleased(a, b, c);
		}
		return false; // TODO verify return value
	}

	public void setTimeRemaining(int remaining) {
		timeRemaining.set(remaining);
	}

	private void drawString(PoseStack poseStack, String string, int x, int y, int color) {
		font.draw(poseStack, string, x, y, color);
	}
}
