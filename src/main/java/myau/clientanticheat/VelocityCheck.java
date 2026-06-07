package myau.clientanticheat;

import net.minecraft.entity.player.EntityPlayer;

import java.util.HashMap;
import java.util.Map;

public class VelocityCheck {
    private final Map<String, Integer> hurtTicks = new HashMap<>();
    private final Map<String, Integer> stillBuffer = new HashMap<>();

    public void check(EntityPlayer player, ClientAntiCheatContext context) {
        String name = player.getName();
        if (isExempt(player)) {
            reset(name);
            return;
        }

        if (player.hurtTime > 0 || player.hurtResistantTime > 10) {
            this.hurtTicks.put(name, 0);
        }

        if (!this.hurtTicks.containsKey(name)) {
            decay(name);
            return;
        }

        int ticks = this.hurtTicks.get(name) + 1;
        this.hurtTicks.put(name, ticks);
        if (ticks > 14) {
            reset(name);
            return;
        }

        double speed = Math.hypot(player.motionX, player.motionZ);
        boolean lowVelocity = ticks >= 2 && ticks <= 8 && speed < 0.025D && player.onGround && player.hurtTime == 0;
        int buffer = this.stillBuffer.getOrDefault(name, 0);
        if (lowVelocity) {
            buffer++;
            if (buffer >= 3) {
                context.receiveSignal(name, "Velocity");
                reset(name);
                return;
            }
        } else {
            buffer = Math.max(0, buffer - 1);
        }
        this.stillBuffer.put(name, buffer);
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
                || player.capabilities.disableDamage
                || player.isCollidedHorizontally;
    }

    private void decay(String name) {
        int buffer = this.stillBuffer.getOrDefault(name, 0);
        if (buffer > 0) this.stillBuffer.put(name, buffer - 1);
    }

    private void reset(String name) {
        this.hurtTicks.remove(name);
        this.stillBuffer.remove(name);
    }

    public void reset() {
        this.hurtTicks.clear();
        this.stillBuffer.clear();
    }
}
