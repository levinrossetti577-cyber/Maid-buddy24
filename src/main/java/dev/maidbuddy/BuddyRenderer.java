package dev.maidbuddy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Zeichnet den Begleiter als kleines 3D-Pixelmodell mit dem mitgelieferten
 * 64x64-Skin (Slim-Arm-Layout). Die Geometrie wird über den SubmitNodeCollector
 * eingereiht, damit sie zusammen mit Entities gezeichnet wird (Tiefe, Licht, Nebel
 * stimmen) – in Ego- und Third-Person-Ansicht.
 */
public final class BuddyRenderer {
    static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(MaidBuddyClient.MOD_ID, "textures/entity/maid_buddy.png");

    /** Modellgrösse: 32 Pixel hoch * SCALE / 16 = ca. 0.84 Blöcke. */
    static final float SCALE = 0.42f;
    private static final float TEX = 64f;

    private BuddyRenderer() {}

    static void submit(LevelRenderContext context) {
        BuddyState s = MaidBuddyClient.STATE;
        Minecraft mc = Minecraft.getInstance();
        if (!s.visible || !s.initialized || mc.player == null || mc.level == null) return;

        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float time = s.ticks + pt;

        Vec3 p = s.prevPos.lerp(s.pos, pt);
        float bob = Mth.sin(time * 0.1f) * 0.06f;

        Vec3 cam = context.levelState().cameraRenderState.pos;
        double dx = p.x - cam.x;
        double dy = p.y + bob - cam.y;
        double dz = p.z - cam.z;
        double distSq = dx * dx + dy * dy + dz * dz;
        // Zu nah an der Kamera (würde ins Bild clippen) oder zu weit weg -> nicht zeichnen
        if (distSq < 0.4 * 0.4 || distSq > 96 * 96) return;

        Pose pose = new Pose(
                BuddyState.lerpRad(pt, s.prevBodyYaw, s.bodyYaw),
                Mth.lerp(pt, s.prevHeadYaw, s.headYaw),
                Mth.lerp(pt, s.prevHeadPitch, s.headPitch),
                Mth.lerp(pt, s.prevWalk, s.walk),
                Mth.lerp(pt, s.prevPhase, s.phase),
                time,
                s.packedLight);

        PoseStack poseStack = context.poseStack();
        if (poseStack == null) poseStack = new PoseStack();

        RenderType type = RenderTypes.entityCutoutNoCull(TEXTURE);

        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);
        context.submitNodeCollector().submitCustomGeometry(poseStack, type,
                (matrices, consumer) -> drawModel(matrices, consumer, pose));
        poseStack.popPose();
    }

    /** Unveränderliche Animationswerte für ein Frame. */
    private record Pose(float bodyYaw, float headYaw, float headPitch,
                        float walk, float phase, float time, int light) {}

    private static void drawModel(PoseStack.Pose matrices, VertexConsumer vc, Pose a) {
        float lean = 0.25f * a.walk();
        Matrix4f root = new Matrix4f(matrices.pose())
                .rotateY(a.bodyYaw())
                .rotateX(lean)
                .scale(SCALE / 16f);
        Matrix3f nRoot = new Matrix3f(matrices.normal())
                .rotateY(a.bodyYaw())
                .rotateX(lean);

        float idle = Mth.sin(a.time() * 0.08f);
        float swing = Mth.sin(a.phase()) * 0.7f * a.walk();

        // Arme: Laufschwung + leichtes Wippen im Leerlauf, etwas abgespreizt
        float rArmX = swing + idle * 0.08f;
        float lArmX = -swing - idle * 0.08f;
        float rArmZ = -0.12f - 0.05f * idle;
        float lArmZ = 0.12f + 0.05f * idle;

        // Beine baumeln beim Schweben leicht nach hinten
        float dangle = 0.18f + 0.06f * Mth.sin(a.time() * 0.07f + 1f);
        float rLegX = -swing * 0.8f + dangle;
        float lLegX = swing * 0.8f + dangle * 0.7f;

        // Kopf (Pivot am Hals)
        part(root, nRoot, vc, a.light(), 0, 24, 0, a.headYaw(), -a.headPitch(), 0,
                -4, 24, -4, 4, 32, 4, 0, 0, 8, 8, 8, 0f, 32, 0, 0.5f);
        // Körper
        part(root, nRoot, vc, a.light(), 0, 0, 0, 0, 0, 0,
                -4, 12, -2, 4, 24, 2, 16, 16, 8, 12, 4, 0f, 16, 32, 0.25f);
        // Rechter Arm (Slim, Charakter-rechts = -X)
        part(root, nRoot, vc, a.light(), -5, 22, 0, 0, rArmX, rArmZ,
                -7, 12, -2, -4, 24, 2, 40, 16, 3, 12, 4, 0f, 40, 32, 0.25f);
        // Linker Arm
        part(root, nRoot, vc, a.light(), 5, 22, 0, 0, lArmX, lArmZ,
                4, 12, -2, 7, 24, 2, 32, 48, 3, 12, 4, 0f, 48, 48, 0.25f);
        // Rechtes Bein
        part(root, nRoot, vc, a.light(), -2, 12, 0, 0, rLegX, 0,
                -4, 0, -2, 0, 12, 2, 0, 16, 4, 12, 4, 0f, 0, 32, 0.25f);
        // Linkes Bein
        part(root, nRoot, vc, a.light(), 2, 12, 0, 0, lLegX, 0,
                0, 0, -2, 4, 12, 2, 16, 48, 4, 12, 4, 0f, 0, 48, 0.25f);
    }

    /**
     * Zeichnet einen Körperteil (Basisschicht + Überzug-Schicht) mit Rotation um einen Pivot.
     * Koordinaten in Skin-Pixeln, Ursprung an den Füssen, Vorderseite zeigt nach +Z.
     */
    private static void part(Matrix4f root, Matrix3f nRoot, VertexConsumer vc, int light,
                             float px, float py, float pz, float rotY, float rotX, float rotZ,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             int u, int v, int w, int h, int d, float inflate,
                             int ou, int ov, float overlayInflate) {
        Matrix4f m = new Matrix4f(root)
                .translate(px, py, pz)
                .rotateY(rotY).rotateX(rotX).rotateZ(rotZ)
                .translate(-px, -py, -pz);
        Matrix3f n = new Matrix3f(nRoot).rotateY(rotY).rotateX(rotX).rotateZ(rotZ);

        cube(m, n, vc, light, x0, y0, z0, x1, y1, z1, u, v, w, h, d, inflate);
        cube(m, n, vc, light, x0, y0, z0, x1, y1, z1, ou, ov, w, h, d, overlayInflate);
    }

    private static void cube(Matrix4f m, Matrix3f n, VertexConsumer vc, int light,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             int u, int v, int w, int h, int d, float g) {
        x0 -= g; y0 -= g; z0 -= g;
        x1 += g; y1 += g; z1 += g;

        // Vorderseite (+Z)
        face(m, n, vc, light, 0, 0, 1,
                x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1,
                u + d, v + d, u + d + w, v + d + h);
        // Rückseite (-Z)
        face(m, n, vc, light, 0, 0, -1,
                x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0,
                u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h);
        // Rechte Seite des Charakters (-X)
        face(m, n, vc, light, -1, 0, 0,
                x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1,
                u, v + d, u + d, v + d + h);
        // Linke Seite des Charakters (+X)
        face(m, n, vc, light, 1, 0, 0,
                x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0,
                u + d + w, v + d, u + 2 * d + w, v + d + h);
        // Oben (+Y)
        face(m, n, vc, light, 0, 1, 0,
                x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0,
                u + d, v, u + d + w, v + d);
        // Unten (-Y)
        face(m, n, vc, light, 0, -1, 0,
                x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1,
                u + d + w, v, u + d + 2 * w, v + d);
    }

    /** Ein Viereck: Ecken oben-links, unten-links, unten-rechts, oben-rechts. */
    private static void face(Matrix4f m, Matrix3f n, VertexConsumer vc, int light,
                             float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float uL, float vT, float uR, float vB) {
        Vector3f normal = n.transform(nx, ny, nz, new Vector3f()).normalize();
        vertex(m, vc, light, normal, ax, ay, az, uL, vT);
        vertex(m, vc, light, normal, bx, by, bz, uL, vB);
        vertex(m, vc, light, normal, cx, cy, cz, uR, vB);
        vertex(m, vc, light, normal, dx, dy, dz, uR, vT);
    }

    private static void vertex(Matrix4f m, VertexConsumer vc, int light, Vector3f normal,
                               float x, float y, float z, float u, float v) {
        Vector3f p = m.transformPosition(x, y, z, new Vector3f());
        vc.addVertex(p.x, p.y, p.z)
                .setColor(0xFFFFFFFF)
                .setUv(u / TEX, v / TEX)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(normal.x, normal.y, normal.z);
    }
}
