package de.universallp.va.core.handler;

import de.universallp.va.core.util.LogHelper;
import de.universallp.va.core.util.Utils;
import de.universallp.va.core.util.libs.LibLocalization;
import de.universallp.va.core.util.libs.LibNames;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * Created by universallp on 08.04.2016 20:08.
 *
 * Reads latest crash report, if it was caused by VA it'll suggest GitHub to report it
 */
public class CrashReportHandler {

    private static File latestCrash = null;

    public static void readCrashes(Side s) {
        if (ConfigHandler.READ_LOGS)
            LogHelper.logInfo("Reading crash logs... If you don't want VA to read crash-reports, disable it in the config");
        else {
            LogHelper.logInfo("VA is not reading crash-reports because it is disabled in the config");
            return;
        }

        String path;

        if (s == Side.CLIENT) {
            path = Minecraft.getMinecraft().mcDataDir.toString() + "/crash-reports/";
            LogHelper.logInfo("Reading crash-reports on client side from %s", path);
        } else {
            path = "./crash-reports";
            LogHelper.logInfo("Reading crash-reports on server side from %s", path);
        }

        File latest = getLatestFileFromDir(path);
        if (latest != null && latest.getName().contains("crash")) {
            try {
                Scanner scanner = new Scanner(latest);
                int lineNum = 0;
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    lineNum++;
                    if (line.contains(LibNames.CRASHREPORT) && !latest.getName().contains(ConfigHandler.LATEST_CRASH)) {
                        latestCrash = latest;
                        LogHelper.logInfo("Found latest crash likely related to VA: %s", latest.getName());
                        return;
                    }
                }
            } catch (FileNotFoundException e) {
                LogHelper.logException("Couldn't read crash-reports: %s", e, false);
            }
        }

    }

    private static File getLatestFileFromDir(String dirPath) {
        File dir = new File(dirPath);
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return null;
        }

        File lastModifiedFile = files[0];
        for (int i = 1; i < files.length; i++) {
            if (lastModifiedFile.lastModified() < files[i].lastModified()) {
                lastModifiedFile = files[i];
            }
        }
        return lastModifiedFile;
    }

    public static void onServerStart(FMLServerStartingEvent event) {
        if (latestCrash != null && event.getSide() == Side.SERVER) {
            LogHelper.logInfo("VA found a crash-report (%s) which is likely related to it.", latestCrash.getName());
            LogHelper.logInfo("If you want goto github.com/univrsal/VanillaAutomation and report it.");

            Utils.setConfigValue(ConfigHandler.config.getConfigFile(), "    S:latestCrash=", latestCrash.getName());
            ConfigHandler.LATEST_CRASH = latestCrash.getName();
        }
    }

    @SubscribeEvent
    public void onWorldJoined(EntityJoinWorldEvent e) {
        if (e != null && e.getEntity() instanceof EntityPlayer && e.getWorld().isRemote && latestCrash != null)
            if (!latestCrash.getName().equals(ConfigHandler.LATEST_CRASH)) {
                ClickEvent issues = new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/univrsal/VanillaAutomation/issues");
                Style s = new Style().setClickEvent(issues);
                ITextComponent msg = new TextComponentString(I18n.format(LibLocalization.MSG_CRASH1)).setStyle(s);

                Utils.setConfigValue(ConfigHandler.config.getConfigFile(), "    S:latestCrash=", latestCrash.getName());
                ConfigHandler.LATEST_CRASH = latestCrash.getName();
                e.getEntity().sendMessage(msg);
            }
    }
}
