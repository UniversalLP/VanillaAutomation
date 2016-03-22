package de.universallp.va.client.gui;

import de.universallp.va.core.network.PacketHandler;
import de.universallp.va.core.network.messages.MessageSyncPlacer;
import de.universallp.va.core.tile.TilePlacer;
import de.universallp.va.core.util.References;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiDispenser;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.EnumFacing;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by universallp on 20.03.2016 15:01.
 */
public class GuiPlacer extends GuiDispenser {

    private int reachDistance;
    private EnumFacing placeFace;

    private GuiButton btnUp;
    private GuiButton btnDown;
    private GuiButton btnFace;

    private TilePlacer placer;

    public GuiPlacer(InventoryPlayer playerInv, TilePlacer dispenserInv, int reachDistance, EnumFacing face) {
        super(playerInv, dispenserInv);

        this.reachDistance = reachDistance;
        this.placeFace     = face;
        placer = dispenserInv;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        if (mouseX > guiLeft + 135 && mouseX < guiLeft + 152)
            if (mouseY > guiTop + 38 && mouseY < guiTop + 48)
                drawHoveringText(Arrays.asList(I18n.format(References.GUI_DIST)), mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        fontRendererObj.drawString(String.valueOf(reachDistance),    (reachDistance > 9 ? 138 : 141), 39, 4210752);
        fontRendererObj.drawString(I18n.format(References.GUI_FACE), 9, 38, 4210752);
    }

    @Override
    public void initGui() {
        super.initGui();
        btnFace = new GuiButton(0, guiLeft + 8,   guiTop + 48, 50, 20, I18n.format(References.GUI_DIR + placeFace.getName()));
        btnUp   = new GuiButton(2, guiLeft + 134, guiTop + 15, 20, 20, "+");
        btnDown = new GuiButton(1, guiLeft + 134, guiTop + 50, 20, 20, "-");

        if (reachDistance < 2)
            btnDown.enabled = false;
        if (reachDistance > 15)
            btnUp.enabled = false;

        buttonList.add(btnDown);
        buttonList.add(btnUp);
        buttonList.add(btnFace);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            placeFace = References.getNext(placeFace);
            button.displayString = I18n.format(References.GUI_DIR + placeFace.getName());
            PacketHandler.INSTANCE.sendToServer(new MessageSyncPlacer(placer.getPos(), placeFace, (byte) reachDistance));
            placer.placeFace = placeFace;
        }

        if (button.id == 1) {
            if (reachDistance > 1) {
                reachDistance--;
                btnUp.enabled = true;
                if (reachDistance < 2)
                    button.enabled = false;
                placer.reachDistance = (byte) reachDistance;
                PacketHandler.INSTANCE.sendToServer(new MessageSyncPlacer(placer.getPos(), placeFace, (byte) reachDistance));
            }
        }

        if (button.id == 2) {
            if (reachDistance < 16) {
                reachDistance++;
                btnDown.enabled = true;
                if (reachDistance == 16)
                    button.enabled = false;
                placer.reachDistance = (byte) reachDistance;
                PacketHandler.INSTANCE.sendToServer(new MessageSyncPlacer(placer.getPos(), placeFace, (byte) reachDistance));
            }
        }

        super.actionPerformed(button);
    }
}
