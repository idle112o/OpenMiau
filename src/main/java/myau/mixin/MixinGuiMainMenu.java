package myau.mixin;

import myau.ui.MainMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMainMenu.class)
public class MixinGuiMainMenu {
    @Inject(method = "initGui", at = @At("HEAD"), cancellable = true)
    private void openCustomMainMenu(CallbackInfo ci) {
        Minecraft.getMinecraft().displayGuiScreen(new MainMenu());
        ci.cancel();
    }
}
