package com.burnedkirby.TurnBasedMinecraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;

import java.util.ArrayList;
import java.util.List;

public class ClientConfigGui extends net.minecraft.client.gui.screens.Screen {
    private final int widget_height = 20;
    private final int start_top_offset = 5;
    private final int widget_x_offset = 16;
    private boolean dirtyFlag;
    private boolean accepted;
    private StringWidget battleListEditBoxStringWidget = null;
    private EditBox battleListEditBox = null;
    private StringWidget sillyListEditBoxStringWidget = null;
    private EditBox sillyListEditBox = null;
    private StringWidget sillyMusicThresholdSliderStringWidget = null;
    private SliderPercentage sillyMusicThresholdSlider = null;
    private StringWidget affectedByMasterVolCheckboxStringWidget = null;
    private Checkbox affectedByMasterVolCheckbox = null;
    private StringWidget affectedByMusicVolCheckboxStringWidget = null;
    private Checkbox affectedByMusicVolCheckbox = null;
    private StringWidget volumeSliderStringWidget = null;
    private SliderPercentage volumeSlider = null;
    private Screen parentScreen = null;
    private ScrollBar scrollBar = null;
    private double scrollAmount = 0.0;

    public ClientConfigGui(ModContainer container, Screen parent) {
        super(Component.literal("TurnBasedMC Client Config"));

        dirtyFlag = true;

        accepted = false;

        this.parentScreen = parent;
    }

    public void onDirty() {
        clearWidgets();

        // Initialize GUI elements.
        int widget_width = this.width / 2 - widget_x_offset * 2;
        int top_offset = start_top_offset;

        if (scrollBar == null) {
            scrollBar = new ScrollBar(this.width - widget_x_offset, 0, widget_x_offset, this.height - widget_height);
        } else {
            scrollBar.setPosition(this.width - widget_x_offset, 0);
            scrollBar.setSize(widget_x_offset, this.height - widget_height);
        }
        addRenderableWidget(scrollBar);

        if (battleListEditBoxStringWidget == null) {
            battleListEditBoxStringWidget = new StringWidget(this.width / 2 - widget_width + widget_x_offset,
                                                             top_offset - (int)scrollBar.scrollAmount(),
                    widget_width, widget_height, Component.literal("Battle Music Categories"),
                    font);
        } else {
            battleListEditBoxStringWidget.setPosition(this.width / 2 - widget_width + widget_x_offset,
                                                      top_offset - (int)scrollBar.scrollAmount());
            battleListEditBoxStringWidget.setSize(widget_width, widget_height);
        }
        addRenderableWidget(battleListEditBoxStringWidget);
        if (battleListEditBox == null) {
            battleListEditBox =
                    new EditBox(font, this.width / 2 + widget_x_offset, top_offset - (int)scrollBar.scrollAmount(), widget_width,
                            widget_height, Component.literal("Battle Music Categories Edit Box"));
        } else {
            battleListEditBox.setPosition(this.width / 2 + widget_x_offset, top_offset - (int)scrollBar.scrollAmount());
            battleListEditBox.setSize(widget_width, widget_height);
        }
        String tempString = "";
        for (String category : ClientConfig.CLIENT.battleMusicList.get()) {
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

        if (sillyListEditBoxStringWidget == null) {
            sillyListEditBoxStringWidget =
                    new StringWidget(this.width / 2 - widget_width + widget_x_offset,
                                     top_offset - (int)scrollBar.scrollAmount(),
                            widget_width, widget_height, Component.literal("Silly Music Categories"),
                            font);
        } else {
            sillyListEditBoxStringWidget.setPosition(this.width / 2 - widget_width + widget_x_offset,
                                                     top_offset - (int)scrollBar.scrollAmount());
            sillyListEditBoxStringWidget.setSize(widget_width, widget_height);
        }
        addRenderableWidget(sillyListEditBoxStringWidget);
        if (sillyListEditBox == null) {
            sillyListEditBox =
                    new EditBox(font, this.width / 2 + widget_x_offset,
                            top_offset - (int)scrollBar.scrollAmount(), widget_width,
                            widget_height, Component.literal("Silly Music Categories Edit Box"));
        } else {
            sillyListEditBox.setPosition(this.width / 2 + widget_x_offset,
                                         top_offset - (int)scrollBar.scrollAmount());
            sillyListEditBox.setSize(widget_width, widget_height);
        }
        tempString = "";
        for (String category : ClientConfig.CLIENT.sillyMusicList.get()) {
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

        if (sillyMusicThresholdSliderStringWidget == null) {
            sillyMusicThresholdSliderStringWidget =
                    new StringWidget(this.width / 2 - widget_width + widget_x_offset,
                                     top_offset - (int)scrollBar.scrollAmount(),
                            widget_width, widget_height, Component.literal("Silly Music Threshold"), font);
            sillyMusicThresholdSliderStringWidget.setTooltip(Tooltip.create(
                    Component.literal("Ratio of minimum of silly mobs in battle to play silly music")));
        } else {
            sillyMusicThresholdSliderStringWidget.setPosition(this.width / 2 - widget_width + widget_x_offset,
                                                              top_offset - (int)scrollBar.scrollAmount());
            sillyMusicThresholdSliderStringWidget.setSize(widget_width, widget_height);
        }
        addRenderableWidget(sillyMusicThresholdSliderStringWidget);
        if (sillyMusicThresholdSlider == null) {
            sillyMusicThresholdSlider =
                    new SliderPercentage(this.width / 2 + widget_x_offset, top_offset - (int)scrollBar.scrollAmount(), widget_width,
                            widget_height, Component.literal("Silly Music Threshold: " +
                            String.format("%.1f%%", ClientConfig.CLIENT.sillyMusicThreshold.get() * 100.0)),
                            ClientConfig.CLIENT.sillyMusicThreshold.get(), "Silly Music Threshold: ");
        } else {
            sillyMusicThresholdSlider.setPosition(this.width / 2 + widget_x_offset,
                                                  top_offset - (int)scrollBar.scrollAmount());
            sillyMusicThresholdSlider.setSize(widget_width, widget_height);
        }
        addRenderableWidget(sillyMusicThresholdSlider);

        top_offset += widget_height;

        if (affectedByMasterVolCheckboxStringWidget == null) {
            affectedByMasterVolCheckboxStringWidget =
                    new StringWidget(this.width / 2 - widget_width + widget_x_offset,
                                     top_offset - (int)scrollBar.scrollAmount(),
                            widget_width, widget_height, Component.literal("Affected by Master Vol."),
                            font);
            affectedByMasterVolCheckboxStringWidget.setTooltip(Tooltip.create(
                    Component.literal("If enabled, volume is affected by global master volume.")));
        } else {
            affectedByMasterVolCheckboxStringWidget.setPosition(this.width / 2 - widget_width + widget_x_offset,
                                                                top_offset - (int)scrollBar.scrollAmount());
            affectedByMasterVolCheckboxStringWidget.setSize(widget_width, widget_height);
        }
        addRenderableWidget(affectedByMasterVolCheckboxStringWidget);
        if (affectedByMasterVolCheckbox == null) {
            affectedByMasterVolCheckbox = Checkbox.builder(Component.literal(""), font)
                    .pos(this.width / 2 + widget_x_offset, top_offset - (int)scrollBar.scrollAmount()).build();
        } else {
            affectedByMasterVolCheckbox.setPosition(this.width / 2 + widget_x_offset,
                    top_offset - (int)scrollBar.scrollAmount());
        }
        if ((ClientConfig.CLIENT.volumeAffectedByMasterVolume.get() &&
                !affectedByMasterVolCheckbox.selected()) ||
                (!ClientConfig.CLIENT.volumeAffectedByMasterVolume.get() &&
                        affectedByMasterVolCheckbox.selected())) {
            affectedByMasterVolCheckbox.onPress(new MouseButtonInfo(0, 0));
        }
        addRenderableWidget(affectedByMasterVolCheckbox);

        top_offset += widget_height;

        if (affectedByMusicVolCheckboxStringWidget == null) {
            affectedByMusicVolCheckboxStringWidget =
                    new StringWidget(this.width / 2 - widget_width + widget_x_offset,
                                     top_offset - (int)scrollBar.scrollAmount(),
                            widget_width, widget_height, Component.literal("Affected by Music Vol."), font);
            affectedByMusicVolCheckboxStringWidget.setTooltip(Tooltip.create(
                    Component.literal("If enabled, volume is affected by global music volume.")));
        } else {
            affectedByMusicVolCheckboxStringWidget.setPosition(this.width / 2 - widget_width + widget_x_offset,
                                                               top_offset - (int)scrollBar.scrollAmount());
            affectedByMusicVolCheckboxStringWidget.setSize(widget_width, widget_height);
        }
        addRenderableWidget(affectedByMusicVolCheckboxStringWidget);
        if (affectedByMusicVolCheckbox == null) {
            affectedByMusicVolCheckbox = Checkbox.builder(Component.literal(""), font)
                    .pos(this.width / 2 + widget_x_offset, top_offset - (int)scrollBar.scrollAmount()).build();
        } else {
            affectedByMusicVolCheckbox.setPosition(this.width / 2 + widget_x_offset,
                    top_offset - (int)scrollBar.scrollAmount());
        }
        if ((ClientConfig.CLIENT.volumeAffectedByMusicVolume.get() &&
                !affectedByMusicVolCheckbox.selected()) ||
                (!ClientConfig.CLIENT.volumeAffectedByMusicVolume.get() &&
                        affectedByMusicVolCheckbox.selected())) {
            affectedByMusicVolCheckbox.onPress(new MouseButtonInfo(0, 0));
        }
        addRenderableWidget(affectedByMusicVolCheckbox);

        top_offset += widget_height;

        if (volumeSliderStringWidget == null) {
            volumeSliderStringWidget =
                    new StringWidget(this.width / 2 - widget_width + widget_x_offset, top_offset - (int)scrollBar.scrollAmount(),
                            widget_width, widget_height, Component.literal("Music Volume"), font);
            volumeSliderStringWidget.setTooltip(
                    Tooltip.create(Component.literal("Volume of battle/silly music")));
        } else {
            volumeSliderStringWidget.setPosition(this.width / 2 - widget_width + widget_x_offset, top_offset - (int)scrollBar.scrollAmount());
            volumeSliderStringWidget.setSize(widget_width, widget_height);
        }
        addRenderableWidget(volumeSliderStringWidget);
        if (volumeSlider == null) {
            volumeSlider =
                    new SliderPercentage(this.width / 2 + widget_x_offset, top_offset - (int)scrollBar.scrollAmount(),
                            widget_width, widget_height,
                            Component.literal(
                            "Volume: " + String.format("%.1f%%", ClientConfig.CLIENT.musicVolume.get() * 100.0)),
                            ClientConfig.CLIENT.musicVolume.get(), "Volume: ");
        } else {
            volumeSlider.setPosition(this.width / 2 + widget_x_offset, top_offset - (int)scrollBar.scrollAmount());
            volumeSlider.setSize(widget_width, widget_height);
        }
        addRenderableWidget(volumeSlider);

        addRenderableWidget(Button.builder(Component.literal("Cancel"),
                        (b) -> Minecraft.getInstance().setScreen(this.parentScreen))
                .bounds(this.width / 2 - widget_width + widget_x_offset,
                        this.height - widget_height, widget_width, widget_height).build());
        addRenderableWidget(Button.builder(Component.literal("Accept"), (b) -> {
            accepted = true;
        }).bounds(this.width / 2 + widget_x_offset, this.height - widget_height, widget_width,
                widget_height).build());

        top_offset += widget_height;
        // TODO set just to "top_offset"
        scrollBar.setContentHeight(top_offset * 4);

        dirtyFlag = false;
    }

    private void doAccepted() {
        String temp = battleListEditBox.getValue();
        {
            List<String> battleList = new ArrayList<String>();
            for (String category : temp.split(",")) {
                battleList.add(category.strip());
            }
            ClientConfig.CLIENT.battleMusicList.set(battleList);
        }

        temp = sillyListEditBox.getValue();
        {
            List<String> sillyList = new ArrayList<String>();
            for (String category : temp.split(",")) {
                sillyList.add(category.strip());
            }
            ClientConfig.CLIENT.sillyMusicList.set(sillyList);
        }

        ClientConfig.CLIENT.sillyMusicThreshold.set(sillyMusicThresholdSlider.percentage);

        ClientConfig.CLIENT.volumeAffectedByMasterVolume.set(affectedByMasterVolCheckbox.selected());

        ClientConfig.CLIENT.volumeAffectedByMusicVolume.set(affectedByMusicVolCheckbox.selected());

        ClientConfig.CLIENT.musicVolume.set(volumeSlider.percentage);

        ClientConfig.CLIENT_SPEC.save();
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (accepted) {
            doAccepted();
            Minecraft.getInstance().setScreen(this.parentScreen);
            return;
        }
        if (dirtyFlag) {
            onDirty();
        } else if (scrollBar != null && scrollAmount != scrollBar.scrollAmount()) {
            scrollAmount = scrollBar.scrollAmount();

            int widget_width = this.width / 2 - widget_x_offset * 2;
            int top_offset = start_top_offset;

            battleListEditBoxStringWidget.setPosition(this.width / 2 - widget_width + widget_x_offset,
                                                      top_offset - (int)scrollBar.scrollAmount());
            battleListEditBox.setPosition(this.width / 2 + widget_x_offset,
                                          top_offset - (int)scrollBar.scrollAmount());

            top_offset += widget_height;

            sillyListEditBoxStringWidget.setPosition(this.width / 2 - widget_width + widget_x_offset,
                                                     top_offset - (int)scrollBar.scrollAmount());
            sillyListEditBox.setPosition(this.width / 2 + widget_x_offset, top_offset - (int)scrollBar.scrollAmount());

            top_offset += widget_height;

            sillyMusicThresholdSliderStringWidget.setPosition(this.width / 2 - widget_width + widget_x_offset,
                                                              top_offset - (int)scrollBar.scrollAmount());
            sillyMusicThresholdSlider.setPosition(this.width / 2 + widget_x_offset,
                                                  top_offset - (int)scrollBar.scrollAmount());

            top_offset += widget_height;

            affectedByMasterVolCheckboxStringWidget.setPosition(this.width / 2 - widget_width + widget_x_offset,
                                                                top_offset - (int)scrollBar.scrollAmount());
            affectedByMasterVolCheckbox.setPosition(this.width / 2 + widget_x_offset,
                                                    top_offset - (int)scrollBar.scrollAmount());

            top_offset += widget_height;

            affectedByMusicVolCheckboxStringWidget.setPosition(this.width / 2 - widget_width + widget_x_offset,
                                                                top_offset - (int)scrollBar.scrollAmount());
            affectedByMusicVolCheckbox.setPosition(this.width / 2 + widget_x_offset,
                                                    top_offset - (int)scrollBar.scrollAmount());

            top_offset += widget_height;

            volumeSliderStringWidget.setPosition(this.width / 2 - widget_width + widget_x_offset,
                                                 top_offset - (int)scrollBar.scrollAmount());
            volumeSlider.setPosition(this.width / 2 + widget_x_offset,
                                     top_offset - (int)scrollBar.scrollAmount());
        }

        super.extractRenderState(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void resize(int pWidth, int pHeight) {
        dirtyFlag = true;
        super.resize(pWidth, pHeight);
    }

    private static class SliderPercentage extends AbstractSliderButton {
        private final String messagePrefix;
        private double percentage;

        public SliderPercentage(int x, int y, int width, int height, Component message, double percentage, String messagePrefix) {
            super(x, y, width, height, message, percentage);
            this.percentage = percentage;
            this.messagePrefix = messagePrefix;
        }

        @Override
        protected void updateMessage() {
            setMessage(
                    Component.literal(messagePrefix + String.format("%.1f%%", percentage * 100.0)));
        }

        @Override
        protected void applyValue() {
            percentage = value;
        }
    }
    private static class ScrollBar extends AbstractScrollArea {
        int contentHeight = 128;
        double scrollRate = 8;

        public ScrollBar(int x, int y, int width, int height) {
            super(x, y, width, height, Component.literal("Client Config Scroll Bar"), AbstractScrollArea.defaultSettings(3));
        }

        public void setContentHeight(int contentHeight) {
            this.contentHeight = contentHeight;
        }

        @Override
        protected int contentHeight() {
            return contentHeight;
        }

        @Override
        protected double scrollRate() {
            return scrollRate;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphicsExtractor, int x, int y, float a) {
            extractRenderState(guiGraphicsExtractor, x, y, a);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            narrationElementOutput.add(NarratedElementType.TITLE, "TBMM Client Config Vertical Scroll Bar");
            narrationElementOutput.add(NarratedElementType.USAGE, "Scroll Vertically With Mouse Wheel");
        }
    }
}
