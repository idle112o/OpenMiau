package myau.module.modules.player;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.Priority;
import myau.events.LeftClickMouseEvent;
import myau.events.RightClickMouseEvent;
import myau.events.HitBlockEvent;
import myau.events.SwapItemEvent;
import myau.events.TickEvent;
import myau.mixin.IAccessorPlayerControllerMP;
import myau.module.Module;
import myau.util.PacketUtil;
import myau.util.TimerUtil;
import myau.property.properties.IntProperty;
import myau.property.properties.PercentProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.util.List;

public class AutoPot extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final TimerUtil timer = new TimerUtil();
    private int prevSlot = -1;

    public final PercentProperty health = new PercentProperty("health", 75);
    public final IntProperty delay = new IntProperty("delay", 500, 50, 5000);

    public AutoPot() {
        super("AutoPot", false);
    }

    public boolean isSwitching() {
        return this.prevSlot != -1;
    }

    private int findPotion() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack == null || !(stack.getItem() instanceof ItemPotion)) {
                continue;
            }

            ItemPotion potion = (ItemPotion) stack.getItem();
            List<PotionEffect> effects = potion.getEffects(stack);
            if (effects == null || effects.isEmpty() || !ItemPotion.isSplash(stack.getMetadata())) {
                continue;
            }

            PotionEffect effect = effects.get(0);
            int potionId = effect.getPotionID();
            if (!isGoodPotion(potionId)) {
                continue;
            }

            if ((potionId == Potion.regeneration.id || potionId == Potion.heal.id) && !shouldHealthPot()) {
                continue;
            }

            PotionEffect activeEffect = mc.thePlayer.getActivePotionEffect(Potion.potionTypes[potionId]);
            if (activeEffect != null && activeEffect.getDuration() != 0) {
                continue;
            }

            return i;
        }
        return -1;
    }

    private boolean shouldHealthPot() {
        return (mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount()) / mc.thePlayer.getMaxHealth()
                <= (float) this.health.getValue() / 100.0F;
    }

    private boolean isGoodPotion(int id) {
        return id == Potion.regeneration.id
                || id == Potion.heal.id
                || id == Potion.moveSpeed.id
                || id == Potion.fireResistance.id;
    }

    @EventTarget(Priority.HIGH)
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) {
            this.prevSlot = -1;
            return;
        }

        switch (event.getType()) {
            case PRE:
                if (mc.thePlayer == null
                        || !mc.thePlayer.onGround
                        || !this.timer.hasTimeElapsed(this.delay.getValue())
                        || Myau.moduleManager.getModule(Scaffold.class).isEnabled()) {
                    return;
                }

                int slot = this.findPotion();
                if (slot != -1) {
                    this.prevSlot = mc.thePlayer.inventory.currentItem;
                    mc.thePlayer.rotationPitch = 87.0F;
                    mc.thePlayer.inventory.currentItem = slot;
                    ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
                    PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                    this.timer.reset();
                }
                break;
            case POST:
                if (this.prevSlot != -1) {
                    mc.thePlayer.inventory.currentItem = this.prevSlot;
                    this.prevSlot = -1;
                }
                break;
            default:
                break;
        }
    }

    @EventTarget
    public void onLeftClick(LeftClickMouseEvent event) {
        if (this.isEnabled() && this.isSwitching()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled() && this.isSwitching()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onHitBlock(HitBlockEvent event) {
        if (this.isEnabled() && this.isSwitching()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onSwap(SwapItemEvent event) {
        if (this.isEnabled() && this.isSwitching()) {
            event.setCancelled(true);
        }
    }
}
