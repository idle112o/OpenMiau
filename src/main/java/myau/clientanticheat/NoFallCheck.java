package myau.clientanticheat;

import net.minecraft.entity.player.EntityPlayer;

import java.util.HashMap;
import java.util.Map;

public class NoFallCheck {
    private final Map<String, Float> maxFallDistance = new HashMap<>();
    private final Map<String, Integer> landingBuffer = new HashMap<>();

    public void check(EntityPlayer player, ClientAntiCheatContext context) {
        String name = player.getName();
        if (isExempt(player)) {
            reset(name);
            return;
        }

        if (player.fallDistance > 3.2F && !player.onGround) {
            float max = Math.max(this.maxFallDistance.getOrDefault(name, 0.0F), player.fallDistance);
            this.maxFallDistance.put(name, max);
            return;
        }

        float trackedFall = this.maxFallDistance.getOrDefault(name, 0.0F);
        if (trackedFall > 3.2F && player.onGround) {
            int buffer = this.landingBuffer.getOrDefault(name, 0);
            boolean noDamage = player.hurtTime == 0 && player.hurtResistantTime <= 10;
            if (noDamage) {
                buffer++;
                if (buffer >= 2) {
                    context.receiveSignal(name, "NoFall");
                    reset(name);
                    return;
                }
            } else {
                reset(name);
                return;
            }
            this.landingBuffer.put(name, buffer);
        } else if (trackedFall == 0.0F) {
            this.landingBuffer.remove(name);
        }
    }

    private boolean isExempt(EntityPlayer player) {
        return player == null
                || player.isDead
                || player.ticksExisted < 20
                || player.isInWater()
                || player.isInLava()
                || player.isOnLadder()
                || player.isRiding()
                || player.capabilities.isFlying
                || player.capabilities.disableDamage;
    }

    private void reset(String name) {
        this.maxFallDistance.remove(name);
        this.landingBuffer.remove(name);
    }

    public void reset() {
        this.maxFallDistance.clear();
        this.landingBuffer.clear();
    }
}
