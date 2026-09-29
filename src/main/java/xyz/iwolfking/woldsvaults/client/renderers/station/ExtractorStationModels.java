package xyz.iwolfking.woldsvaults.client.renderers.station;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ForgeModelBakery;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.iwolfking.woldsvaults.WoldsVaults;

/** Registers every station BER sub-model as a special model so the block atlas stitches its texture and the bakery bakes it. */
@Mod.EventBusSubscriber(modid = WoldsVaults.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ExtractorStationModels {
    private ExtractorStationModels() {
    }

    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent event) {
        for (ExtractorStationRig rig : ExtractorStationRig.all()) {
            for (ExtractorStationRig.Part part : rig.parts) {
                ForgeModelBakery.addSpecialModel(part.model());
            }
        }
    }
}
