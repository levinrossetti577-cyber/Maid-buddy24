package dev.maidbuddy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

/**
 * Rein clientseitiger Zustand des Begleiters: Position, Geschwindigkeit, Blickrichtung
 * und Animationswerte. Wird 20x pro Sekunde (pro Tick) aktualisiert; beim Zeichnen
 * wird zwischen dem letzten und aktuellen Tick interpoliert, damit es bei jeder
 * Bildrate flüssig aussieht.
 */
public final class BuddyState {
    // ---- Einstellungen (in Blöcken) ----
    /** Abstand hinter dem Spieler. */
    static final double BEHIND = 1.2;
    /** Seitlicher Abstand (positiv = rechts vom Spieler). */
    static final double SIDE = 0.9;
    /** Höhe der Füsse des Begleiters über den Füssen des Spielers. */
    static final double HOVER_HEIGHT = 0.75;
    /** Ab dieser Entfernung wird teleportiert. */
    static final double TELEPORT_DISTANCE = 16.0;
    /** Ab dieser Entfernung holt er schneller auf. */
    static final double CATCH_UP_DISTANCE = 5.0;

    public boolean visible = true;
    boolean initialized = false;
    private ClientLevel lastLevel;

    Vec3 prevPos = Vec3.ZERO;
    Vec3 pos = Vec3.ZERO;
    private Vec3 velocity = Vec3.ZERO;

    private float anchorYaw;          // geglättete Spieler-Blickrichtung (Grad)
    float prevBodyYaw, bodyYaw;       // Radiant, 0 = Blick nach +Z
    float prevHeadYaw, headYaw;       // relativ zum Körper
    float prevHeadPitch, headPitch;
    float prevWalk, walk;             // 0..1 Bewegungsstärke
    float prevPhase, phase;           // Lauf-/Schwebe-Phase
    int ticks;
    int packedLight = 0xF000F0;

    void tick(Minecraft client) {
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        if (player == null || level == null) {
            initialized = false;
            lastLevel = null;
            return;
        }
        if (level != lastLevel) {           // Dimensionswechsel / neue Welt
            initialized = false;
            lastLevel = level;
        }
        ticks++;

        // Ankerrichtung folgt der Spielerrichtung nur weich, damit der Begleiter
        // beim schnellen Umsehen nicht hektisch um den Spieler kreist.
        float playerYaw = player.getYRot();
        if (!initialized) anchorYaw = playerYaw;
        anchorYaw += Mth.wrapDegrees(playerYaw - anchorYaw) * 0.08f;

        Vec3 target = findTarget(level, player);

        prevPos = pos;
        Vec3 delta = target.subtract(pos);
        double dist = delta.length();

        if (!initialized || dist > TELEPORT_DISTANCE) {
            // Teleport in die Nähe des Spielers
            pos = target;
            prevPos = target;
            velocity = Vec3.ZERO;
            initialized = true;
            float yaw = (float) Mth.atan2(player.getX() - pos.x, player.getZ() - pos.z);
            prevBodyYaw = bodyYaw = yaw;
        } else {
            // Gedämpfte Feder: weiche, leicht schwebende Bewegung
            velocity = velocity.scale(0.62).add(delta.scale(0.085));
            if (dist > CATCH_UP_DISTANCE) {
                velocity = velocity.add(delta.normalize().scale((dist - CATCH_UP_DISTANCE) * 0.25));
            }
            double max = 3.0;
            if (velocity.lengthSqr() > max * max) velocity = velocity.normalize().scale(max);
            pos = pos.add(velocity);
        }

        // ---- Blickrichtung: Körper und Kopf schauen zum Spieler ----
        prevBodyYaw = bodyYaw;
        prevHeadYaw = headYaw;
        prevHeadPitch = headPitch;

        Vec3 eye = player.getEyePosition();
        Vec3 head = pos.add(0, 0.8, 0);
        double dx = eye.x - head.x, dz = eye.z - head.z, dy = eye.y - head.y;
        double horiz = Math.sqrt(dx * dx + dz * dz);

        double hSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float wantedYaw;
        if (hSpeed > 0.12) {
            wantedYaw = (float) Mth.atan2(velocity.x, velocity.z);   // in Bewegungsrichtung
        } else {
            wantedYaw = (float) Mth.atan2(dx, dz);                   // zum Spieler
        }
        bodyYaw = bodyYaw + wrapRad(wantedYaw - bodyYaw) * 0.18f;

        float lookYaw = (float) Mth.atan2(dx, dz);
        float relHead = Mth.clamp(wrapRad(lookYaw - bodyYaw), -1.1f, 1.1f);
        headYaw += (relHead - headYaw) * 0.3f;
        float pitch = (float) Mth.clamp(Math.atan2(dy, Math.max(horiz, 0.01)), -0.8, 0.8);
        headPitch += (pitch - headPitch) * 0.3f;

        // ---- Animation ----
        prevWalk = walk;
        prevPhase = phase;
        walk += ((float) Math.min(hSpeed * 3.0, 1.0) - walk) * 0.2f;
        phase += 0.12f + (float) hSpeed * 1.6f;

        // ---- Licht an der Position (mit leichtem Eigenleuchten) ----
        BlockPos bp = BlockPos.containing(pos.x, pos.y + 0.5, pos.z);
        int block = Math.max(level.getBrightness(LightLayer.BLOCK, bp), 5);
        int sky = level.getBrightness(LightLayer.SKY, bp);
        packedLight = (block << 4) | (sky << 20);
    }

    private Vec3 findTarget(ClientLevel level, LocalPlayer player) {
        double rad = Math.toRadians(anchorYaw);
        Vec3 forward = new Vec3(-Math.sin(rad), 0, Math.cos(rad));
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        Vec3 base = player.position().add(0, HOVER_HEIGHT, 0);

        Vec3 a = base.add(forward.scale(-BEHIND)).add(right.scale(SIDE));
        if (isFree(level, a)) return a;
        Vec3 b = base.add(forward.scale(-BEHIND)).add(right.scale(-SIDE));
        if (isFree(level, b)) return b;
        Vec3 c = base.add(forward.scale(-BEHIND * 0.6));
        if (isFree(level, c)) return c;
        // Enge Räume: direkt über/neben dem Kopf des Spielers
        return player.position().add(right.scale(0.45)).add(0, player.getBbHeight() * 0.55, 0);
    }

    private static boolean isFree(ClientLevel level, Vec3 feet) {
        for (double y : new double[]{0.05, 0.5, 0.95}) {
            BlockPos bp = BlockPos.containing(feet.x, feet.y + y, feet.z);
            if (!level.getBlockState(bp).getCollisionShape(level, bp).isEmpty()) return false;
        }
        return true;
    }

    static float wrapRad(float a) {
        a = a % (float) (Math.PI * 2);
        if (a >= Math.PI) a -= (float) (Math.PI * 2);
        if (a < -Math.PI) a += (float) (Math.PI * 2);
        return a;
    }

    static float lerpRad(float pt, float a, float b) {
        return a + wrapRad(b - a) * pt;
    }
}
