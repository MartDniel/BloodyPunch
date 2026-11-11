package com.bloodypunch;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.bloodypunch.event.BareHandHarvestHandler;
import com.bloodypunch.event.IncomingDamageHandler;
import com.bloodypunch.event.MiningHandler;
import com.bloodypunch.event.PunchHandler;
import com.bloodypunch.network.BloodyPunchNetwork;
import com.bloodypunch.registry.ModEffects;
import com.bloodypunch.server.ServerBloodinessHandler;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(BloodyPunch.MODID)
public class BloodyPunch {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "bloodypunch";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public BloodyPunch(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register our mod's content to the mod event bus
        ModEffects.register(modEventBus);

        // Register our network payloads (mod bus event).
        modEventBus.addListener(BloodyPunchNetwork::register);

        // Rebuild resolved config values whenever the config loads/reloads.
        modEventBus.addListener(Config::onLoad);
        modEventBus.addListener(Config::onReload);

        // Register our gameplay listeners on the GAME event bus (not the mod bus).
        // PunchHandler's @SubscribeEvent methods are static, so we register the class.
        NeoForge.EVENT_BUS.register(PunchHandler.class);
        NeoForge.EVENT_BUS.register(IncomingDamageHandler.class);
        NeoForge.EVENT_BUS.register(MiningHandler.class);
        NeoForge.EVENT_BUS.register(BareHandHarvestHandler.class);
        NeoForge.EVENT_BUS.register(ServerBloodinessHandler.class);

        // Register our config under config/bloodypunch/bloodypunch-common.toml
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC, "bloodypunch/bloodypunch-common.toml");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("BloodyPunch common setup complete");
    }
}
