package myau.clientanticheat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;

import java.util.HashMap;
import java.util.Map;

public class SprintCheck {
    private final Map<String, Integer> buffer = new HashMap<>();
    private final Map<String, Integer> omniBuffer = new HashMap<>();

    public void check(EntityPlayer player, ClientAntiCheatContext context) {
        ItemStack heldItem = player.getHeldItem();
        boolean blocking = heldItem != null && heldItem.getItem() instanceof ItemSword && player.isBlocking();
        boolean suspicious = blocking && player.isSprinting() && player.motionY == 0.0D;
        String name = player.getName();
        int vl = this.buffer.getOrDefault(name, 0);
        if (suspicious) {
            vl++;
            if (vl > 10) {
                context.receiveSignal(name, "Sprint");
                vl = 0;
            }
        } else {
            vl = Math.max(0, vl - 1);
        }
        this.buffer.put(name, vl);

        int omniVl = this.omniBuffer.getOrDefault(name, 0);
        boolean omniSprint = player.isSprinting()
                && (player.moveForward < 0.0F || player.moveForward == 0.0F && player.moveStrafing != 0.0F)
                && player.hurtTime == 0;
        if (omniSprint) {
            omniVl++;
            if (omniVl > 6) {
                context.receiveSignal(name, "OmniSprint");
                omniVl = 0;
            }
        } else {
            omniVl = Math.max(0, omniVl - 1);
        }
        this.omniBuffer.put(name, omniVl);
    }

    public void reset() {
        this.buffer.clear();
        this.omniBuffer.clear();
    }
}
