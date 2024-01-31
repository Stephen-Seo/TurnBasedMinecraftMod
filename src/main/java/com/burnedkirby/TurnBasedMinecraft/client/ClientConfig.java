package com.burnedkirby.TurnBasedMinecraft.client;

import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class ClientConfig {
    public static final ClientConfig CLIENT;
    public static final ModConfigSpec CLIENT_SPEC;

    static {
        Pair<ClientConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT = pair.getKey();
        CLIENT_SPEC = pair.getValue();
    }

    public final ModConfigSpec.ConfigValue<List<? extends String>> battleMusicList;
    public final ModConfigSpec.ConfigValue<List<? extends String>> sillyMusicList;
    public final ModConfigSpec.IntValue sillyMusicThreshold;
    public final ModConfigSpec.BooleanValue volumeAffectedByMusicVolume;
    public final ModConfigSpec.DoubleValue musicVolume;

    ClientConfig(ModConfigSpec.Builder builder) {
        //builder.push("music");

        List<String> battleMusicList = new ArrayList<String>(8);
        battleMusicList.add("monster");
        battleMusicList.add("animal");
        battleMusicList.add("boss");
        battleMusicList.add("player");
        this.battleMusicList = builder.comment("What categories of mobs that play \"battle\" music").translation(TurnBasedMinecraftMod.MODID + ".clientconfig.battle_music_list").defineList("battleMusicList", battleMusicList, (v) -> v instanceof String);

        List<String> sillyMusicList = new ArrayList<String>(4);
        sillyMusicList.add("passive");
        this.sillyMusicList = builder.comment("What categories of mobs that play \"silly\" music").translation(TurnBasedMinecraftMod.MODID + ".clientconfig.silly_music_list").defineList("sillyMusicList", sillyMusicList, (v) -> true);

        this.sillyMusicThreshold = builder.comment("Minimum percentage of silly entities in battle to use silly music").translation(TurnBasedMinecraftMod.MODID + ".clientconfig.silly_percentage").defineInRange("sillyMusicThreshold", 40, 0, 100);

        this.volumeAffectedByMusicVolume = builder.comment("If \"true\", music volume will be affected by global Music volume setting").translation(TurnBasedMinecraftMod.MODID + ".clientconfig.volume_affected_by_volume").define("volumeAffectedByMusicVolume", true);

        this.musicVolume = builder.comment("Volume of battle/silly music as a percentage between 0.0 and 1.0").translation(TurnBasedMinecraftMod.MODID + ".clientconfig.music_volume").defineInRange("musicVolume", 0.7, 0.0, 1.0);

        //builder.pop();
    }

    public static class CliConfGui extends Screen {
        private final int widget_height = 20;
        private boolean dirtyFlag;
        private boolean accepted;
        private EditBox battleListEditBox = null;
        private EditBox sillyListEditBox = null;
        private Slider100 sillyMusicThresholdSlider = null;
        private Checkbox affectedByMusicVolCheckbox = null;
        private SliderDouble volumeSlider = null;

        public CliConfGui() {
            super(Component.literal("TurnBasedMC Client Config"));

            dirtyFlag = true;

            accepted = false;
        }

        public void onDirty() {
            clearWidgets();

            // Initialize GUI elements.
            int widget_x_offset = 5;
            int widget_width = this.width / 2 - widget_x_offset * 2;
            int top_offset = 5;

            addRenderableWidget(new StringWidget(this.width / 2 - widget_width + widget_x_offset, top_offset, widget_width, widget_height, Component.literal("Battle Music Categories"), font));
            if (battleListEditBox == null) {
                battleListEditBox = new EditBox(font, this.width / 2 + widget_x_offset, top_offset, widget_width, widget_height, Component.literal("Battle Music Categories Edit Box"));
            } else {
                battleListEditBox.setPosition(this.width / 2 + widget_x_offset, top_offset);
                battleListEditBox.setSize(widget_width, widget_height);
            }
            String tempString = "";
            for (String category : CLIENT.battleMusicList.get()) {
                if (tempString.isEmpty()) {
                    tempString = category;
                } else {
                    tempString += "," + category;
                }
            }
            battleListEditBox.setMaxLength(128);
            battleListEditBox.setValue(tempString);
            addRenderableWidget(battleListEditBox);

            top_offset += widget_height;

            addRenderableWidget(new StringWidget(this.width / 2 - widget_width + widget_x_offset, top_offset, widget_width, widget_height, Component.literal("Silly Music Categories"), font));
            if (sillyListEditBox == null) {
                sillyListEditBox = new EditBox(font, this.width / 2 + widget_x_offset, top_offset, widget_width, widget_height, Component.literal("Silly Music Categories Edit Box"));
            } else {
                sillyListEditBox.setPosition(this.width / 2 + widget_x_offset, top_offset);
                sillyListEditBox.setSize(widget_width, widget_height);
            }
            tempString = "";
            for (String category : CLIENT.sillyMusicList.get()) {
                if (tempString.isEmpty()) {
                    tempString = category;
                } else {
                    tempString += "," + category;
                }
            }
            sillyListEditBox.setMaxLength(128);
            sillyListEditBox.setValue(tempString);
            addRenderableWidget(sillyListEditBox);

            top_offset += widget_height;

            addRenderableWidget(new StringWidget(this.width / 2 - widget_width + widget_x_offset, top_offset, widget_width, widget_height, Component.literal("Silly Music Threshold").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0XFFFFFFFF)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("0-100 percentage minimum count of silly mobs to play silly music")))), font));
            if (sillyMusicThresholdSlider == null) {
                sillyMusicThresholdSlider = new Slider100(this.width / 2 + widget_x_offset, top_offset, widget_width, widget_height, Component.literal("Silly Music Threshold: " + CLIENT.sillyMusicThreshold.get()), CLIENT.sillyMusicThreshold.get(), "Silly Music Threshold: ");
            } else {
                sillyMusicThresholdSlider.setPosition(this.width / 2 + widget_x_offset, top_offset);
                sillyMusicThresholdSlider.setSize(widget_width, widget_height);
            }
            addRenderableWidget(sillyMusicThresholdSlider);

            top_offset += widget_height;

            addRenderableWidget(new StringWidget(this.width / 2 - widget_width + widget_x_offset, top_offset, widget_width, widget_height, Component.literal("Affected by Music Vol.").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0XFFFFFFFF)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("If enabled, volume is affected by global music volume.")))), font));
            if (affectedByMusicVolCheckbox == null) {
                affectedByMusicVolCheckbox = Checkbox.builder(Component.literal(""), font).pos(this.width / 2 + widget_x_offset, top_offset).build();
            } else {
                affectedByMusicVolCheckbox.setPosition(this.width / 2 + widget_x_offset, top_offset);
            }
            if ((CLIENT.volumeAffectedByMusicVolume.get() && !affectedByMusicVolCheckbox.selected()) || (!CLIENT.volumeAffectedByMusicVolume.get() && affectedByMusicVolCheckbox.selected())) {
                affectedByMusicVolCheckbox.onPress();
            }
            addRenderableWidget(affectedByMusicVolCheckbox);

            top_offset += widget_height;

            addRenderableWidget(new StringWidget(this.width / 2 - widget_width + widget_x_offset, top_offset, widget_width, widget_height, Component.literal("Music Volume").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0XFFFFFFFF)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Volume of battle/silly music")))), font));
            if (volumeSlider == null) {
                volumeSlider = new SliderDouble(this.width / 2 + widget_x_offset, top_offset, widget_width, widget_height, Component.literal("Volume: " + String.format("%.2f", CLIENT.musicVolume.get())), CLIENT.musicVolume.get(), "Volume: ");
            } else {
                volumeSlider.setPosition(this.width / 2 + widget_x_offset, top_offset);
                volumeSlider.setSize(widget_width, widget_height);
            }
            addRenderableWidget(volumeSlider);

            addRenderableWidget(Button.builder(Component.literal("Cancel"), (b) -> Minecraft.getInstance().setScreen(null)).bounds(this.width / 2 - widget_width + widget_x_offset, this.height - widget_height, widget_width, widget_height).build());
            addRenderableWidget(Button.builder(Component.literal("Accept"), (b) -> {
                accepted = true;
            }).bounds(this.width / 2 + widget_x_offset, this.height - widget_height, widget_width, widget_height).build());

            dirtyFlag = false;
        }

        private void doAccepted() {
            String temp = battleListEditBox.getValue();
            {
                List<String> battleList = new ArrayList<String>();
                for (String category : temp.split(",")) {
                    battleList.add(category.strip());
                }
                CLIENT.battleMusicList.set(battleList);
            }

            temp = sillyListEditBox.getValue();
            {
                List<String> sillyList = new ArrayList<String>();
                for (String category : temp.split(",")) {
                    sillyList.add(category.strip());
                }
                CLIENT.sillyMusicList.set(sillyList);
            }

            CLIENT.sillyMusicThreshold.set(sillyMusicThresholdSlider.percentageInt);

            CLIENT.volumeAffectedByMusicVolume.set(affectedByMusicVolCheckbox.selected());

            CLIENT.musicVolume.set(volumeSlider.percentage);
        }

        @Override
        public boolean isPauseScreen() {
            return true;
        }

        @Override
        public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            if (accepted) {
                doAccepted();
                Minecraft.getInstance().setScreen(null);
                return;
            }
            if (dirtyFlag) {
                onDirty();
            }
            super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        }

        @Override
        public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
            dirtyFlag = true;
            super.resize(pMinecraft, pWidth, pHeight);
        }

        private static class Slider100 extends AbstractSliderButton {
            private int percentageInt;
            private String messagePrefix;

            public Slider100(int x, int y, int width, int height, Component message, int percentage, String messagePrefix) {
                super(x, y, width, height, message, percentage / 100.0);
                this.percentageInt = percentage;
                this.messagePrefix = messagePrefix;
            }

            @Override
            protected void updateMessage() {
                setMessage(Component.literal(messagePrefix + (int) (value * 100.0)));
            }

            @Override
            protected void applyValue() {
                percentageInt = (int) (value * 100.0);
            }
        }

        private static class SliderDouble extends AbstractSliderButton {
            private final String messagePrefix;
            private double percentage;

            public SliderDouble(int x, int y, int width, int height, Component message, double percentage, String messagePrefix) {
                super(x, y, width, height, message, percentage);
                this.percentage = percentage;
                this.messagePrefix = messagePrefix;
            }

            @Override
            protected void updateMessage() {
                setMessage(Component.literal(messagePrefix + String.format("%.2f", percentage)));
            }

            @Override
            protected void applyValue() {
                percentage = value;
            }
        }
    }
}
