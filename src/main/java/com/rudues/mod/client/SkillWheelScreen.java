package com.rudues.mod.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.rudues.mod.ability.AbilityType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

/**
 * Pie menu opened by holding R. Click an ability, then press the key you want to cast it with.
 * The screen closes when R is released (unless you are in the middle of binding a key).
 */
public class SkillWheelScreen extends Screen {
    private static final int INNER = 36;
    private static final int OUTER = 110;
    private static final int SEGMENTS = AbilityType.values().length; // 3 -> 120 degrees each

    private int binding = -1;
    private int hovered = -1;
    private Component notice = null;

    public SkillWheelScreen() {
        super(Component.translatable("screen.rudues.skill_wheel"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (binding < 0) {
            long window = Minecraft.getInstance().getWindow().getWindow();
            if (!InputConstants.isKeyDown(window, ClientKeys.OPEN_WHEEL.getKey().getValue())) {
                onClose();
            }
        }
    }

    private double segStart(int i) {
        // segment 0 is centred straight up (-90 deg in screen space)
        return -90.0 - 180.0 / SEGMENTS + (360.0 / SEGMENTS) * i;
    }

    private int segmentAt(double mx, double my) {
        double dx = mx - width / 2.0;
        double dy = my - height / 2.0;
        if (Math.sqrt(dx * dx + dy * dy) < INNER) {
            return -1;
        }
        double deg = Math.toDegrees(Math.atan2(dy, dx));
        double size = 360.0 / SEGMENTS;
        for (int i = 0; i < SEGMENTS; i++) {
            double rel = ((deg - segStart(i)) % 360.0 + 360.0) % 360.0;
            if (rel < size) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0x66000000);
        int cx = width / 2;
        int cy = height / 2;
        hovered = binding >= 0 ? binding : segmentAt(mouseX, mouseY);
        double size = 360.0 / SEGMENTS;

        for (int i = 0; i < SEGMENTS; i++) {
            boolean active = i == hovered;
            boolean isBinding = i == binding;
            int color = isBinding ? 0xE0D060FF : active ? 0xD0A040E0 : 0xA03A1560;
            drawSector(g, cx, cy, segStart(i) + 1.5, segStart(i) + size - 1.5, INNER, active ? OUTER + 8 : OUTER, color);
        }

        for (int i = 0; i < SEGMENTS; i++) {
            AbilityType t = AbilityType.byIndex(i);
            double mid = Math.toRadians(segStart(i) + size / 2.0);
            int lx = cx + (int) (Math.cos(mid) * (INNER + OUTER) / 2.0);
            int ly = cy + (int) (Math.sin(mid) * (INNER + OUTER) / 2.0);
            g.drawCenteredString(font, t.displayName(), lx, ly - 9, 0xFFFFFF);
            Component keyText = Component.literal("[").append(AbilityBindings.keyName(AbilityBindings.get(i))).append("]");
            g.drawCenteredString(font, keyText, lx, ly + 2, 0xE8C0FF);
        }

        g.drawCenteredString(font, Component.literal("R"), cx, cy - 4, 0xFFFFFF);

        Component bottom;
        if (binding >= 0) {
            bottom = Component.translatable("gui.rudues.press_key", AbilityType.byIndex(binding).displayName());
        } else if (hovered >= 0) {
            bottom = AbilityType.byIndex(hovered).description();
        } else {
            bottom = Component.translatable("gui.rudues.click_to_set");
        }
        g.drawCenteredString(font, bottom, cx, cy + OUTER + 24, 0xFFFFFF);
        if (notice != null) {
            g.drawCenteredString(font, notice, cx, cy + OUTER + 38, 0xFF8080);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (binding < 0 && button == 0) {
            int seg = segmentAt(mx, my);
            if (seg >= 0) {
                binding = seg;
                notice = null;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding >= 0) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                binding = -1;
                notice = null;
                return true;
            }
            if (keyCode <= 0 || keyCode == ClientKeys.OPEN_WHEEL.getKey().getValue()) {
                notice = Component.translatable("gui.rudues.reserved");
                return true;
            }
            AbilityType type = AbilityType.byIndex(binding);
            AbilityBindings.set(binding, keyCode);
            ClientForgeEvents.castWasDown[binding] = true; // don't cast immediately from this same key press
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.translatable("message.rudues.bound",
                        type.displayName(), AbilityBindings.keyName(keyCode)), true);
            }
            binding = -1;
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static void drawSector(GuiGraphics g, int cx, int cy, double startDeg, double endDeg,
                                   int inner, int outer, int argb) {
        Matrix4f m = g.pose().last().pose();
        float a = (argb >>> 24 & 255) / 255f;
        float r = (argb >>> 16 & 255) / 255f;
        float gr = (argb >>> 8 & 255) / 255f;
        float b = (argb & 255) / 255f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        int steps = 28;
        for (int s = 0; s <= steps; s++) {
            double ang = Math.toRadians(startDeg + (endDeg - startDeg) * s / steps);
            float cos = (float) Math.cos(ang);
            float sin = (float) Math.sin(ang);
            buf.vertex(m, cx + cos * outer, cy + sin * outer, 0).color(r, gr, b, a).endVertex();
            buf.vertex(m, cx + cos * inner, cy + sin * inner, 0).color(r, gr, b, a).endVertex();
        }
        BufferUploader.drawWithShader(buf.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
