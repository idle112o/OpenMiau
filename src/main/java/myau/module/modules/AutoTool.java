package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.util.ItemUtil;
import myau.util.KeyBindUtil;
import myau.util.PacketUtil;
import myau.util.TeamUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class AutoTool extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int serverSlot = -1;
    private int spoofedToolSlot = -1;
    private int tickDelayCounter = 0;
    public final IntProperty switchDelay = new IntProperty("delay", 0, 0, 5);
    public final BooleanProperty switchBack = new BooleanProperty("switch-back", true);
    public final BooleanProperty sneakOnly = new BooleanProperty("sneak-only", true);

    public AutoTool() {
        super("AutoTool", false);
    }

    public boolean isKillAura() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (!killAura.isEnabled()) return false;
        return TeamUtil.isEntityLoaded(killAura.getTarget()) && killAura.isAttackAllowed();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) {
            return;
        }
        if (mc.thePlayer == null || mc.theWorld == null) {
            this.resetSilentSlot(false);
            return;
        }
        if (this.shouldSpoofTool()) {
            if (this.tickDelayCounter >= this.switchDelay.getValue()) {
                Block block = mc.theWorld.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock();
                int slot = this.findBestHotbarTool(block);
                if (slot != -1 && slot != mc.thePlayer.inventory.currentItem) {
                    this.selectToolSilently(slot);
                }
            }
            this.tickDelayCounter++;
        } else {
            this.resetSilentSlot(this.switchBack.getValue());
            this.tickDelayCounter = 0;
        }
    }

    private boolean shouldSpoofTool() {
        return mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK
                && mc.gameSettings.keyBindAttack.isKeyDown()
                && !mc.thePlayer.isUsingItem()
                && !this.isKillAura()
                && (!this.sneakOnly.getValue() || KeyBindUtil.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()))
                && mc.theWorld.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock()
                        .getBlockHardness(mc.theWorld, mc.objectMouseOver.getBlockPos()) != 0.0F;
    }

    private int findBestHotbarTool(Block block) {
        int currentSlot = mc.thePlayer.inventory.currentItem;
        ItemStack currentStack = mc.thePlayer.inventory.getStackInSlot(currentSlot);
        float bestStrength = currentStack == null ? 1.0F : currentStack.getStrVsBlock(block);
        int bestSlot = currentSlot;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            if (stack == null) {
                continue;
            }
            float strength = stack.getStrVsBlock(block);
            if (strength > bestStrength) {
                bestStrength = strength;
                bestSlot = slot;
            }
        }
        return bestSlot == currentSlot ? -1 : bestSlot;
    }

    private void selectToolSilently(int slot) {
        if (this.serverSlot == -1) {
            this.serverSlot = mc.thePlayer.inventory.currentItem;
        }
        if (this.spoofedToolSlot != slot) {
            PacketUtil.sendPacket(new C09PacketHeldItemChange(slot));
            this.spoofedToolSlot = slot;
        }
    }

    private void resetSilentSlot(boolean sendSwitchBack) {
        if (this.serverSlot != -1 && sendSwitchBack) {
            PacketUtil.sendPacket(new C09PacketHeldItemChange(this.serverSlot));
        }
        this.serverSlot = -1;
        this.spoofedToolSlot = -1;
    }

    @Override
    public void onDisabled() {
        this.resetSilentSlot(true);
        this.tickDelayCounter = 0;
    }
}
