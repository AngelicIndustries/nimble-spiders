package industries.angelic.nimblespiders.common;

import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import industries.angelic.nimblespiders.Tags;
import industries.angelic.nimblespiders.common.network.ClimberNetwork;

@Mod(
    modid = SpiderMod.MODID,
    name = "Nimble Spiders",
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "required-after:unimixins;",
    guiFactory = "industries.angelic.nimblespiders.client.ConfigGuiFactory",
    customProperties = { @Mod.CustomProperty(k = "license", v = "LGPL-2.1-or-later"),
        @Mod.CustomProperty(k = "issueTrackerUrl", v = "https://github.com/AngelicIndustries/nimble-spiders/issues"),
        @Mod.CustomProperty(k = "iconFile", v = "assets/nimble-spiders/icon.png"),
        @Mod.CustomProperty(k = "backgroundFile", v = "assets/nimble-spiders/background.png") })
public class SpiderMod {

    public static final String MODID = "nimble-spiders";
    @Mod.Instance(MODID)
    public static SpiderMod instance;
    @SidedProxy(
        clientSide = "industries.angelic.nimblespiders.client.ClientProxy",
        serverSide = "industries.angelic.nimblespiders.common.CommonProxy")
    public static CommonProxy proxy;
    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        Config.init(event.getModConfigurationDirectory());
        FMLCommonHandler.instance()
            .bus()
            .register(new Config());
        ClimberNetwork.init();
        proxy.preInit();
    }
}
