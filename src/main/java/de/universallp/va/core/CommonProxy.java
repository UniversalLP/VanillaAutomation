package de.universallp.va.core;

import de.universallp.va.VanillaAutomation;
import de.universallp.va.core.block.VABlocks;
import de.universallp.va.core.dispenser.DispenserTweaks;
import de.universallp.va.core.handler.ConfigHandler;
import de.universallp.va.core.handler.CrashReportHandler;
import de.universallp.va.core.item.VAItems;
import de.universallp.va.core.network.GuiHandler;
import de.universallp.va.core.network.PacketHandler;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

/**
 * Created by universallp on 19.03.2016 11:28.
 */
public class CommonProxy {


    public void preInit(FMLPreInitializationEvent e) {
        ConfigHandler.loadConfig(e.getSuggestedConfigurationFile());
        VAItems.init();
        VABlocks.init();
    }

    public void init(FMLInitializationEvent e) {
        NetworkRegistry.INSTANCE.registerGuiHandler(VanillaAutomation.instance, new GuiHandler());
        MinecraftForge.EVENT_BUS.register(new CrashReportHandler());


        VABlocks.register();
        VAItems.register();
        PacketHandler.register();
        DispenserTweaks.register();

        CrashReportHandler.readCrashes(e.getSide());
    }

    public void postInit(FMLPostInitializationEvent e) {
        ConfigHandler.loadPostInit();
    }

    /**
     * Shamelessly stolen from Botania
     * github.com/vazkii/Botania
     *
     * @param entity
     * @param reach
     */
    public void setReach(EntityLivingBase entity, float reach) {
        if (entity instanceof EntityPlayerMP)
            ((EntityPlayerMP) entity).interactionManager.setBlockReachDistance(reach);
    }


}
