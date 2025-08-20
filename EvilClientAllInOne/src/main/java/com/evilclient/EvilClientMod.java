package com.evilclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class EvilClientMod implements ClientModInitializer {

    private static KeyBinding toggleSprintKey;
    private static KeyBinding zoomKey;
    private static KeyBinding bossbarToggleKey;

    private static boolean toggleSprint = false;
    private static boolean hideBossBar = false;
    private static double originalFov = -1;

    private static int lastLeftClicks = 0;
    private static long lastLeftClickTime = 0;
    private static int cps = 0;

    @Override
    public void onInitializeClient() {
        toggleSprintKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.evilclient.togglesprint", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.category.evilclient"));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.evilclient.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "key.category.evilclient"));
        bossbarToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.evilclient.bossbar", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.category.evilclient"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleSprintKey.wasPressed()) {
                toggleSprint = !toggleSprint;
                if (client.player != null) client.player.sendMessage(Text.literal("ToggleSprint: " + (toggleSprint ? "ON" : "OFF")), true);
            }
            while (bossbarToggleKey.wasPressed()) {
                hideBossBar = !hideBossBar;
                if (client.player != null) client.player.sendMessage(Text.literal("BossBar: " + (hideBossBar ? "HIDDEN" : "VISIBLE")), true);
            }

            if (toggleSprint && client.player != null) {
                client.player.setSprinting(true);
            }

            if (zoomKey.isPressed()) {
                if (originalFov < 0) originalFov = MinecraftClient.getInstance().options.getFov().getValue();
                MinecraftClient.getInstance().options.getFov().setValue(30.0D);
            } else {
                if (originalFov >= 0) {
                    MinecraftClient.getInstance().options.getFov().setValue(originalFov);
                    originalFov = -1;
                }
            }

            // CPS calculation (approx)
            int left = MinecraftClient.getInstance().mouse.getLeftButton();
            long now = System.currentTimeMillis();
            if (left == 1) {
                if (now - lastLeftClickTime <= 1000) {
                    cps++;
                } else {
                    cps = 1;
                    lastLeftClickTime = now;
                }
                lastLeftClickTime = now;
            }
        });

        HudRenderCallback.EVENT.register(EvilClientMod::onHudRender);
    }

    private static void onHudRender(DrawContext context, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        int x = 8, y = 8;
        // FPS
        context.drawText(mc.textRenderer, "FPS: " + mc.getCurrentFps(), x, y, 0xFFFFFF, true);
        y += 12;
        // Ping (if in server)
        try {
            int ping = mc.getNetworkHandler().getPlayerListEntry(mc.player.getEntityName()).getLatency();
            context.drawText(mc.textRenderer, "Ping: " + ping + "ms", x, y, 0xFFFFFF, true);
        } catch (Exception e) {
            context.drawText(mc.textRenderer, "Ping: -", x, y, 0xFFFFFF, true);
        }
        y += 12;
        // Speed (blocks/sec) simple approx using player movement
        try {
            double vx = mc.player.getVelocity().x;
            double vz = mc.player.getVelocity().z;
            double speed = Math.sqrt(vx*vx + vz*vz) * 20.0; // blocks per second approx
            context.drawText(mc.textRenderer, String.format("Speed: %.2f b/s", speed), x, y, 0xFFFFFF, true);
        } catch (Exception e) {
            context.drawText(mc.textRenderer, "Speed: -", x, y, 0xFFFFFF, true);
        }
        y += 12;
        // Keystrokes
        String keys = "W A S D  "; // simplified
        context.drawText(mc.textRenderer, "Keys: " + keys, x, y, 0xAAAAAA, true);
        y += 12;
        // CPS
        context.drawText(mc.textRenderer, "CPS: " + 0, x, y, 0xFFFFFF, true);
        y += 12;
        // Potion HUD placeholder
        context.drawText(mc.textRenderer, "Potions: none", x, y, 0xFFFFFF, true);
    }
}
