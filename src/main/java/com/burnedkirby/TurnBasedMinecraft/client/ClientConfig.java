package com.burnedkirby.TurnBasedMinecraft.client;

import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class ClientConfig {
    public static final ClientConfig CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        Pair<ClientConfig, ForgeConfigSpec> pair =
            new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT = pair.getKey();
        CLIENT_SPEC = pair.getValue();
    }

    public final ForgeConfigSpec.ConfigValue<List<? extends String>> battleMusicList;
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> sillyMusicList;
    public final ForgeConfigSpec.DoubleValue sillyMusicThreshold;
    public final ForgeConfigSpec.BooleanValue volumeAffectedByMasterVolume;
    public final ForgeConfigSpec.BooleanValue volumeAffectedByMusicVolume;
    public final ForgeConfigSpec.DoubleValue musicVolume;

    ClientConfig(ForgeConfigSpec.Builder builder) {
        //builder.push("music");

        List<String> battleMusicList = new ArrayList<String>(8);
        battleMusicList.add("monster");
        battleMusicList.add("animal");
        battleMusicList.add("boss");
        battleMusicList.add("player");
        this.battleMusicList = builder.comment("What categories of mobs that play \"battle\" music")
            .translation(TurnBasedMinecraftMod.MODID + ".clientconfig.battle_music_list")
            .defineList("battleMusicList", battleMusicList, (v) -> v instanceof String);

        List<String> sillyMusicList = new ArrayList<String>(4);
        sillyMusicList.add("passive");
        this.sillyMusicList = builder.comment("What categories of mobs that play \"silly\" music")
            .translation(TurnBasedMinecraftMod.MODID + ".clientconfig.silly_music_list")
            .defineList("sillyMusicList", sillyMusicList, (v) -> true);

        this.sillyMusicThreshold =
            builder.comment("Minimum percentage of silly entities in battle to use silly music")
                .translation(TurnBasedMinecraftMod.MODID + ".clientconfig.silly_percentage")
                .defineInRange("sillyMusicThreshold", 0.4, 0.0, 1.0);

        this.volumeAffectedByMasterVolume = builder.comment(
                "If \"true\", music volume will be affected by global Master volume setting")
            .translation(TurnBasedMinecraftMod.MODID + ".clientconfig.volume_affected_by_master")
            .define("volumeAffectedByMasterVolume", true);

        this.volumeAffectedByMusicVolume = builder.comment(
                "If \"true\", music volume will be affected by global Music volume setting")
            .translation(TurnBasedMinecraftMod.MODID + ".clientconfig.volume_affected_by_volume")
            .define("volumeAffectedByMusicVolume", true);

        this.musicVolume =
            builder.comment("Volume of battle/silly music as a percentage between 0.0 and 1.0")
                .translation(TurnBasedMinecraftMod.MODID + ".clientconfig.music_volume")
                .defineInRange("musicVolume", 0.7, 0.0, 1.0);

        //builder.pop();
    }
}
