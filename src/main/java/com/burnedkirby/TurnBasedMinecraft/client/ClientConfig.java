package com.burnedkirby.TurnBasedMinecraft.client;

import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ClientConfig {
    public final ModConfigSpec.ConfigValue<List<? extends String>> battleMusicList;
    public final ModConfigSpec.ConfigValue<List<? extends String>> sillyMusicList;

    public final ModConfigSpec.IntValue sillyMusicThreshold;
    public final ModConfigSpec.BooleanValue volumeAffectedByMusicVolume;
    public final ModConfigSpec.DoubleValue musicVolume;

    public static final ClientConfig CLIENT;

    public static final ModConfigSpec CLIENT_SPEC;

    ClientConfig(ModConfigSpec.Builder builder) {
        builder.push("music");

        List<String> battleMusicList = new ArrayList<String>(8);
        battleMusicList.add("monster");
        battleMusicList.add("animal");
        battleMusicList.add("boss");
        battleMusicList.add("player");
        this.battleMusicList = builder
            .comment("What categories of mobs that play \"battle\" music")
            .translation(TurnBasedMinecraftMod.MODID + ".clientconfig.battle_music_list")
            .defineList("battleMusicList", battleMusicList, (v) -> v instanceof String);

        List<String> sillyMusicList = new ArrayList<String>(4);
        sillyMusicList.add("passive");
        this.sillyMusicList = builder
            .comment("What categories of mobs that play \"silly\" music")
            .translation(TurnBasedMinecraftMod.MODID + ".clientconfig.silly_music_list")
            .defineList("sillyMusicList", sillyMusicList, (v) -> true);

        this.sillyMusicThreshold = builder
            .comment("Minimum percentage of silly entities in battle to use silly music")
            .translation(TurnBasedMinecraftMod.MODID + ".clientconfig.silly_percentage")
            .defineInRange("sillyMusicThreshold", 40, 0, 100);

        this.volumeAffectedByMusicVolume = builder
            .comment("If \"true\", music volume will be affected by global Music volume setting")
            .translation(TurnBasedMinecraftMod.MODID + ".clientconfig.volume_affected_by_volume")
            .define("volumeAffectedByMusicVolume", true);

        this.musicVolume = builder
            .comment("Volume of battle/silly music as a percentage between 0.0 and 1.0")
            .translation(TurnBasedMinecraftMod.MODID + ".clientconfig.music_volume")
            .defineInRange("musicVolume", 0.8, 0.0, 1.0);

        builder.pop();
    }

    static {
        Pair<ClientConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT = pair.getKey();
        CLIENT_SPEC = pair.getValue();
    }
}
