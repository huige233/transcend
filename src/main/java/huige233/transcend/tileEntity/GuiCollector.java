package huige233.transcend.tileEntity;

import huige233.transcend.util.Reference;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class GuiCollector extends GuiContainer {
    private static final ResourceLocation TEXTURES = new ResourceLocation(Reference.MOD_ID + ":textures/gui/bedrock_collector.png");
    private ResourceLocation BACKGROUND_TEX;

    public GuiCollector(InventoryPlayer player, TileEntityCollerctor tileentity)
    {
        super(new ContainerCollector(player, tileentity));
        this.setBackgroundTexture(TEXTURES);
    }

    protected void setBackgroundTexture(ResourceLocation location) {
        this.BACKGROUND_TEX = location;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String s = I18n.format("container.bedrock_collector", new Object[0]);
        //float scaled_progress = scaleF((float)((TileNeutronCollector)this.machineTile).getProgress(), 7111.0F, 100.0F);
        //String progress = "Progress: " + MathHelper.round(scaled_progress, 10.0F) + "%";
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 6, 4210752);
        //this.fontRenderer.drawString(progress, this.xSize / 2 - this.fontRenderer.getStringWidth(progress) / 2, 60, 4210752);
        this.fontRenderer.drawString(I18n.format("container.inventory", new Object[0]), 8, this.ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURES);
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
    }
}
