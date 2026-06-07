package myau.clientanticheat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;

import java.util.HashMap;
import java.util.Map;

public class MotionCheck {
    private final Map<String, Integer> speedBuffer = new HashMap<>();
    private final Map<String, Integer> airStallBuffer = new HashMap<>();
    private final Map<String, Integer> airTicks = new HashMap<>();

    public void check(EntityPlayer player, ClientAntiCheatContext context) {
        String name = player.getName();
        if (isExempt(player)) {
            reset(name);
            return;
        }

        double speed = Math.hypot(player.motionX, player.motionZ);
        double max = getMaxExpectedSpeed(player);
        int speedVl = this.speedBuffer.getOrDefault(name, 0);
        if (speed > max && player.hurtTime == 0 && player.hurtResistantTime < 5 && !player.isCollidedHorizontally) {
            speedVl++;
            if (speedVl >= 5) {
                context.receiveSignal(name, "Motion");
                speedVl = 0;
            }
        } else {
            speedVl = Math.max(0, speedVl - 1);
        }
        this.speedBuffer.put(name, speedVl);

        int offGroundTicks = player.onGround ? 0 : this.airTicks.getOrDefault(name, 0) + 1;
        this.airTicks.put(name, offGroundTicks);

        int airVl = this.airStallBuffer.getOrDefault(name, 0);
        boolean airStall = !player.onGround
                && offGroundTicks >= 6
                && Math.abs(player.motionY) < 0.003D
                && speed < 0.02D
                && player.fallDistance > 0.0F;
        if (airStall) {
            airVl++;
            if (airVl >= 4) {
                context.receiveSignal(name, "Motion");
                airVl = 0;
            }
        } else {
            airVl = Math.max(0, airVl - 1);
        }
        this.airStallBuffer.put(name, airVl);
    }

    private double getMaxExpectedSpeed(EntityPlayer player) {
        double max = player.onGround ? 0.42D : 0.62D;
        if (player.isSprinting()) max += 0.08D;
        if (player.isPotionActive(Potion.moveSpeed)) {
            int amplifier = player.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1;
            max += amplifier * 0.08D;
        }
        if (player.isPotionActive(Potion.jump)) max += 0.04D;
        return max;
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
        this.speedBuffer.remove(name);
        this.airStallBuffer.remove(name);
        this.airTicks.remove(name);
    }

    public void reset() {
        this.speedBuffer.clear();
        this.airStallBuffer.clear();
        this.airTicks.clear();
    }
}
