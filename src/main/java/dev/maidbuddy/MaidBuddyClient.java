package dev.maidbuddy;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Einstiegspunkt der Mod.
 *
 * Der Begleiter ist KEINE Minecraft-Entity. Er existiert nur als Position im Speicher
 * dieses Clients und wird direkt in die Welt gezeichnet. Dadurch:
 * - sieht ihn kein anderer Spieler,
 * - wird nichts an den Server gesendet,
 * - kann er nicht angreifen, nicht angegriffen werden und keine Items aufheben.
 */
public final class MaidBuddyClient implements ClientModInitializer {
    public static final String MOD_ID = "maidbuddy";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final BuddyState STATE = new BuddyState();

    private static KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.maidbuddy.toggle",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_B,
                KeyMapping.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(MaidBuddyClient::onClientTick);
        LevelRenderEvents.COLLECT_SUBMITS.register(BuddyRenderer::submit);

        LOGGER.info("[Maid Buddy] Client-only companion initialised.");
    }

    private static void onClientTick(Minecraft client) {
        while (toggleKey.consumeClick()) {
            STATE.visible = !STATE.visible;
            if (client.player != null) {
                client.player.displayClientMessage(
                        Component.translatable(STATE.visible ? "maidbuddy.shown" : "maidbuddy.hidden"), true);
            }
        }
        STATE.tick(client);
    }
}
