package industries.angelic.nimblespiders.integration;

import static industries.angelic.nimblespiders.integration.SpiderIntegration.check;
import static industries.angelic.nimblespiders.integration.SpiderIntegration.climb;

import net.minecraft.world.WorldServer;

import com.shinoow.abyssalcraft.common.entity.anti.EntityAntiSpider;

import industries.angelic.nimblespiders.common.entity.mob.SpiderClimber;
import industries.angelic.nimblespiders.common.entity.movement.AdvancedClimberPathNavigator;

/** Optional runtime checks, loaded only when AbyssalCraft is in the development runtime. */
public final class AbyssalCraftIntegration {

    static void run(WorldServer world) {
        EntityAntiSpider spider = new EntityAntiSpider(world);
        check(SpiderClimber.get(spider) != null, "AbyssalCraft anti-spider climber");
        check(spider.getNavigator() instanceof AdvancedClimberPathNavigator, "AbyssalCraft anti-spider navigator");
        climb(spider, "AbyssalCraft anti-spider");
    }
}
