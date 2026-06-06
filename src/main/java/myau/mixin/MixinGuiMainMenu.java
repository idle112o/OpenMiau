package myau.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMainMenu.class)
public class MixinGuiMainMenu {
    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void drawCredits(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        String credits = "Credits: [ksyz, OpenMyau Project, idle]";
        mc.fontRendererObj.drawStringWithShadow(credits, 2.0F, 2.0F, 0xFFFFFFFF);
    }
}
