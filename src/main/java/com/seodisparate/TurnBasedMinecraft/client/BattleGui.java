package com.seodisparate.TurnBasedMinecraft.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.seodisparate.TurnBasedMinecraft.common.Battle;
import com.seodisparate.TurnBasedMinecraft.common.Combatant;
import com.seodisparate.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import com.seodisparate.TurnBasedMinecraft.common.networking.PacketBattleDecision;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class BattleGui extends GuiScreen
{
    public AtomicInteger timeRemaining;
    public long lastInstant;
    public long elapsedTime;
    private MenuState state;
    private boolean stateChanged;
    private String info;
    
    private enum MenuState
    {
        MAIN_MENU(0),
        ATTACK_TARGET(1),
        ITEM_ACTION(2),
        WAITING(3),
        SWITCH_ITEM(4),
        USE_ITEM(5);
        
        private int value;
        
        private MenuState(int value)
        {
            this.value = value;
        }
        
        public int getValue()
        {
            return value;
        }
        
        private static Map<Integer, MenuState> map;
        
        static
        {
            map = new HashMap<Integer, MenuState>();
            for(MenuState state : MenuState.values())
            {
                map.put(state.getValue(), state);
            }
        }
        
        public static MenuState valueOf(int value)
        {
            return map.get(value);
        }
    }
    
    private enum ButtonAction
    {
        ATTACK(0),
        DEFEND(1),
        ITEM(2),
        FLEE(3),
        ATTACK_TARGET(4),
        SWITCH_HELD_ITEM(5),
        DECIDE_USE_ITEM(6),
        CANCEL(7),
        DO_ITEM_SWITCH(8),
        DO_USE_ITEM(9);
        
        private int value;
        
        private ButtonAction(int value)
        {
            this.value = value;
        }
        
        public int getValue()
        {
            return value;
        }
        
        private static Map<Integer, ButtonAction> map;
        static
        {
            map = new HashMap<Integer, ButtonAction>();
            for(ButtonAction action : ButtonAction.values())
            {
                map.put(action.getValue(), action);
            }
        }
        
        public static ButtonAction valueOf(int value)
        {
            return map.get(value);
        }
    }
    
    public BattleGui()
    {
        timeRemaining = new AtomicInteger(TurnBasedMinecraftMod.getBattleDurationSeconds());
        lastInstant = System.nanoTime();
        elapsedTime = 0;
        state = MenuState.MAIN_MENU;
        stateChanged = true;
    }
    
    private void setState(MenuState state)
    {
        this.state = state;
        stateChanged = true;
    }
    
    public void turnBegin()
    {
        if(TurnBasedMinecraftMod.commonProxy.getLocalBattle() != null)
        {
            TurnBasedMinecraftMod.commonProxy.getLocalBattle().setState(Battle.State.ACTION);
        }
        setState(MenuState.WAITING);
    }
    
    public void turnEnd()
    {
        if(TurnBasedMinecraftMod.commonProxy.getLocalBattle() != null)
        {
            TurnBasedMinecraftMod.commonProxy.getLocalBattle().setState(Battle.State.DECISION);
        }
        timeRemaining.set(TurnBasedMinecraftMod.getBattleDurationSeconds());
        elapsedTime = 0;
        lastInstant = System.nanoTime();
        setState(MenuState.MAIN_MENU);
    }
    
    public void battleChanged()
    {
        stateChanged = true;
    }
    
    public void updateState()
    {
        if(!stateChanged)
        {
            return;
        }
        
        stateChanged = false;
        buttonList.clear();
        switch(state)
        {
        case MAIN_MENU:
            info = "What will you do?";
            buttonList.add(new GuiButton(ButtonAction.ATTACK.getValue(), width*3/7 - 25, height - 70, 50, 20, "Attack"));
            buttonList.add(new GuiButton(ButtonAction.DEFEND.getValue(), width*4/7 - 25, height - 70, 50, 20, "Defend"));
            buttonList.add(new GuiButton(ButtonAction.ITEM.getValue(), width*3/7 - 25, height - 50, 50, 20, "Item"));
            buttonList.add(new GuiButton(ButtonAction.FLEE.getValue(), width*4/7 - 25, height - 50, 50, 20, "Flee"));
            break;
        case ATTACK_TARGET:
            info = "Who will you attack?";
            int y = 30;
            for(Map.Entry<Integer, Combatant> e : TurnBasedMinecraftMod.commonProxy.getLocalBattle().getSideAEntrySet())
            {
                if(e.getValue().entity != null)
                {
                    buttonList.add(new EntitySelectionButton(ButtonAction.ATTACK_TARGET.getValue(), width/4 - 60, y, 120, 20, e.getValue().entity.getName(), e.getKey(), true));
                }
                else
                {
                    buttonList.add(new EntitySelectionButton(ButtonAction.ATTACK_TARGET.getValue(), width/4 - 60, y, 120, 20, "Unknown", e.getKey(), true));
                }
                y += 20;
            }
            y = 30;
            for(Map.Entry<Integer, Combatant> e : TurnBasedMinecraftMod.commonProxy.getLocalBattle().getSideBEntrySet())
            {
                if(e.getValue().entity != null)
                {
                    buttonList.add(new EntitySelectionButton(ButtonAction.ATTACK_TARGET.getValue(), width*3/4 - 60, y, 120, 20, e.getValue().entity.getName(), e.getKey(), false));
                }
                else
                {
                    buttonList.add(new EntitySelectionButton(ButtonAction.ATTACK_TARGET.getValue(), width*3/4 - 60, y, 120, 20, "Unknown", e.getKey(), false));
                }
                y += 20;
            }
            buttonList.add(new GuiButton(ButtonAction.CANCEL.getValue(), width/2 - 40, height - 120, 80, 20, "Cancel"));
            break;
        case ITEM_ACTION:
            info = "What will you do with an item?";
            buttonList.add(new GuiButton(ButtonAction.SWITCH_HELD_ITEM.getValue(), width*1/4 - 40, height - 120, 80, 20, "Switch Held"));
            buttonList.add(new GuiButton(ButtonAction.DECIDE_USE_ITEM.getValue(), width*2/4 - 40, height - 120, 80, 20, "Use"));
            buttonList.add(new GuiButton(ButtonAction.CANCEL.getValue(), width*3/4 - 40, height - 120, 80, 20, "Cancel"));
            break;
        case WAITING:
            info = "Waiting...";
            break;
        case SWITCH_ITEM:
            info = "To which item will you switch to?";
            for(int i = 0; i < 9; ++i)
            {
                buttonList.add(new ItemSelectionButton(ButtonAction.DO_ITEM_SWITCH.getValue(), width/2 - 88 + i * 20, height - 19, 16, 16, "", i));
            }
            buttonList.add(new GuiButton(ButtonAction.CANCEL.getValue(), width/2 - 40, height - 120, 80, 20, "Cancel"));
            break;
        case USE_ITEM:
            info = "Which item will you use?";
            for(int i = 0; i < 9; ++i)
            {
                buttonList.add(new ItemSelectionButton(ButtonAction.DO_USE_ITEM.getValue(), width/2 - 88 + i * 20, height - 19, 16, 16, "", i));
            }
            buttonList.add(new GuiButton(ButtonAction.CANCEL.getValue(), width/2 - 40, height - 120, 80, 20, "Cancel"));
            break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        if(TurnBasedMinecraftMod.commonProxy.getLocalBattle() == null)
        {
            drawHoveringText("Waiting...", width / 2 - 50, height / 2);
            return;
        }
        if(TurnBasedMinecraftMod.commonProxy.getLocalBattle().getState() == Battle.State.DECISION && timeRemaining.get() > 0)
        {
            long nextInstant = System.nanoTime();
            elapsedTime += nextInstant - lastInstant;
            lastInstant = nextInstant;
            while(elapsedTime > 1000000000)
            {
                elapsedTime -= 1000000000;
                timeRemaining.decrementAndGet();
            }
        }
        
        updateState();
        
        super.drawScreen(mouseX, mouseY, partialTicks);
        
        String timeRemainingString = "Time remaining: ";
        int timeRemainingInt = timeRemaining.get();
        if(timeRemainingInt > 8)
        {
            timeRemainingString += "\u00A7a";
        }
        else if(timeRemainingInt > 4)
        {
            timeRemainingString += "\u00A7e";
        }
        else
        {
            timeRemainingString += "\u00A7c";
        }
        timeRemainingString += Integer.toString(timeRemainingInt);
        int stringWidth = Minecraft.getMinecraft().fontRenderer.getStringWidth(timeRemainingString);
        Minecraft.getMinecraft().fontRenderer.drawString(timeRemainingString, width/2 - stringWidth/2, 5, 0xFFFFFFFF);
        stringWidth = Minecraft.getMinecraft().fontRenderer.getStringWidth(info);
        Minecraft.getMinecraft().fontRenderer.drawString(info, width/2 - stringWidth/2, 20, 0xFFFFFFFF);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException
    {
        switch(ButtonAction.valueOf(button.id))
        {
        case ATTACK:
            setState(MenuState.ATTACK_TARGET);
            break;
        case DEFEND:
            TurnBasedMinecraftMod.NWINSTANCE.sendToServer(new PacketBattleDecision(TurnBasedMinecraftMod.commonProxy.getLocalBattle().getId(), Battle.Decision.DEFEND, 0));
            setState(MenuState.WAITING);
            break;
        case ITEM:
            setState(MenuState.ITEM_ACTION);
            break;
        case FLEE:
            TurnBasedMinecraftMod.NWINSTANCE.sendToServer(new PacketBattleDecision(TurnBasedMinecraftMod.commonProxy.getLocalBattle().getId(), Battle.Decision.FLEE, 0));
            setState(MenuState.WAITING);
            break;
        case ATTACK_TARGET:
            if(button instanceof EntitySelectionButton)
            {
                TurnBasedMinecraftMod.NWINSTANCE.sendToServer(new PacketBattleDecision(TurnBasedMinecraftMod.commonProxy.getLocalBattle().getId(), Battle.Decision.ATTACK, ((EntitySelectionButton)button).entityID));
                setState(MenuState.WAITING);
            }
            else
            {
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
            if(button instanceof ItemSelectionButton)
            {
                TurnBasedMinecraftMod.NWINSTANCE.sendToServer(new PacketBattleDecision(TurnBasedMinecraftMod.commonProxy.getLocalBattle().getId(), Battle.Decision.SWITCH_ITEM, ((ItemSelectionButton)button).itemStackID));
                if(((ItemSelectionButton)button).itemStackID >= 0 && ((ItemSelectionButton)button).itemStackID < 9)
                {
                    Minecraft.getMinecraft().player.inventory.currentItem = ((ItemSelectionButton)button).itemStackID;
                }
                setState(MenuState.WAITING);
            }
            else
            {
                setState(MenuState.MAIN_MENU);
            }
            break;
        case DO_USE_ITEM:
            if(button instanceof ItemSelectionButton)
            {
                TurnBasedMinecraftMod.NWINSTANCE.sendToServer(new PacketBattleDecision(TurnBasedMinecraftMod.commonProxy.getLocalBattle().getId(), Battle.Decision.USE_ITEM, ((ItemSelectionButton)button).itemStackID));
                setState(MenuState.WAITING);
            }
            else
            {
                setState(MenuState.MAIN_MENU);
            }
            break;
        }
    }

    @Override
    public void initGui()
    {
        super.initGui();
    }

    @Override
    public void onGuiClosed()
    {
        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame()
    {
        return false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if(Minecraft.getMinecraft().player.isCreative())
        {
            super.keyTyped(typedChar, keyCode);
        }
    }
}
