package thunder.hack.gui.hud.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;
import thunder.hack.ThunderHack;
import thunder.hack.core.impl.ModuleManager;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.gui.hud.HudElement;
import thunder.hack.modules.client.HudEditor;
import thunder.hack.modules.render.NameTags;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.render.Render2DEngine;

import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Radar extends HudElement {
    public Radar() {
        super("Radar", 100, 100);
    }

    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Rect);
    private final Setting<ColorMode> colorMode = new Setting<>("ColorMode", ColorMode.Sync);
    private final Setting<Integer> size = new Setting<>("Size", 80, 20, 300);
    private final Setting<ColorSetting> color2 = new Setting<>("Color", new ColorSetting(0xFF101010));
    private final Setting<ColorSetting> color3 = new Setting<>("PlayerColor", new ColorSetting(0xC59B9B9B));

    private final Setting<Component> c1 = new Setting<>("Component1", Component.Name, v -> mode.is(Mode.Text));
    private final Setting<Component> c2 = new Setting<>("Component2", Component.Hp, v -> mode.is(Mode.Text));
    private final Setting<Component> c3 = new Setting<>("Component3", Component.Ping, v -> mode.is(Mode.Text));
    private final Setting<Component> c4 = new Setting<>("Component4", Component.None, v -> mode.is(Mode.Text));
    private final Setting<Component> c5 = new Setting<>("Component5", Component.None, v -> mode.is(Mode.Text));

    private final Setting<Formatting> c12 = new Setting<>("Color1", Formatting.WHITE, v -> mode.is(Mode.Text));
    private final Setting<Formatting> c22 = new Setting<>("Color2", Formatting.WHITE, v -> mode.is(Mode.Text));
    private final Setting<Formatting> c32 = new Setting<>("Color3", Formatting.WHITE, v -> mode.is(Mode.Text));
    private final Setting<Formatting> c42 = new Setting<>("Color4", Formatting.WHITE, v -> mode.is(Mode.Text));

    public void onRender2D(DrawContext context) {
        super.onRender2D(context);

        if (mode.getValue() == Mode.Rect) {
            Render2DEngine.drawHudBase(context.getMatrices(), getPosX(), getPosY(), size.getValue(), size.getValue(), HudEditor.hudRound.getValue());

            if (HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry)) {
                Render2DEngine.verticalGradient(context.getMatrices(), getPosX(), getPosY() + (size.getValue() / 2F - 2), getPosX() + size.getValue(), getPosY() + (size.getValue() / 2F), new Color(0x0000000, true), new Color(0x7B000000, true));
                Render2DEngine.verticalGradient(context.getMatrices(), getPosX(), getPosY() + (size.getValue() / 2F), getPosX() + size.getValue(), getPosY() + (size.getValue() / 2F + 2), new Color(0x7B000000, true), new Color(0x0000000, true));
                Render2DEngine.horizontalGradient(context.getMatrices(), getPosX() + (size.getValue() / 2F - 2), getPosY() - 1, getPosX() + (size.getValue() / 2F), getPosY() + size.getValue() - 1, new Color(0x0000000, true), new Color(0x7B000000, true));
                Render2DEngine.horizontalGradient(context.getMatrices(), getPosX() + (size.getValue() / 2F), getPosY() - 1, getPosX() + (size.getValue() / 2F + 2), getPosY() + size.getValue() - 1, new Color(0x7B000000, true), new Color(0x0000000, true));
            } else {
                Render2DEngine.draw2DGradientRect(context.getMatrices(),
                        (float) (getPosX() + (size.getValue() / 2F - 0.5)),
                        (float) (getPosY() + 3.5),
                        (float) (getPosX() + (size.getValue() / 2F + 0.2)),
                        (float) ((getPosY() + size.getValue()) - 3.5),
                        color2.getValue().getColorObject(), color2.getValue().getColorObject(), color2.getValue().getColorObject(), color2.getValue().getColorObject()
                );

                Render2DEngine.draw2DGradientRect(
                        context.getMatrices(),
                        getPosX() + 3.5f,
                        getPosY() + (size.getValue() / 2F - 0.2f),
                        (getPosX() + size.getValue()) - 3.5f,
                        getPosY() + (size.getValue() / 2F + 0.5f),
                        color2.getValue().getColorObject(), color2.getValue().getColorObject(), color2.getValue().getColorObject(), color2.getValue().getColorObject()
                );
            }

            for (PlayerEntity entityPlayer : ThunderHack.asyncManager.getAsyncPlayers()) {
                if (entityPlayer == mc.player)
                    continue;

                float posX = (float) (entityPlayer.prevX + (entityPlayer.prevX - entityPlayer.getX()) * mc.getTickDelta() - mc.player.getX()) * 2;
                float posZ = (float) (entityPlayer.prevZ + (entityPlayer.prevZ - entityPlayer.getZ()) * mc.getTickDelta() - mc.player.getZ()) * 2;
                float cos = (float) Math.cos(mc.player.getYaw(mc.getTickDelta()) * 0.017453292);
                float sin = (float) Math.sin(mc.player.getYaw(mc.getTickDelta()) * 0.017453292);
                float rotY = -(posZ * cos - posX * sin);
                float rotX = -(posX * cos + posZ * sin);
                if (rotY > size.getValue() / 2F - 6) {
                    rotY = size.getValue() / 2F - 6;
                } else if (rotY < -(size.getValue() / 2F - 8)) {
                    rotY = -(size.getValue() / 2F - 8);
                }
                if (rotX > size.getValue() / 2F - 5) {
                    rotX = size.getValue() / 2F - 5;
                } else if (rotX < -(size.getValue() / 2F - 5)) {
                    rotX = -(size.getValue() / 2F - 5);
                }

                Render2DEngine.drawRound(context.getMatrices(), (getPosX() + size.getValue() / 2F + rotX) - 2, (getPosY() + size.getValue() / 2F + rotY) - 2, 4, 4, 2f, color3.getValue().getColorObject());
            }
        }

        if (mode.getValue() == Mode.Text) {
            float offset_y = 0;
            for (PlayerEntity entityPlayer : ThunderHack.asyncManager.getAsyncPlayers()) {
                if (entityPlayer == mc.player)
                    continue;

                String str = String.format("%s %s %s %s %s", getText(c1, entityPlayer), getText(c2, entityPlayer), getText(c3, entityPlayer), getText(c4, entityPlayer), getText(c5, entityPlayer));
                if (colorMode.getValue() == ColorMode.Sync) {
                    FontRenderers.sf_bold.drawString(context.getMatrices(), str, getPosX(), getPosY() + offset_y, HudEditor.getColor((int) (offset_y * 2f)).getRGB());
                } else {
                    FontRenderers.sf_bold.drawString(context.getMatrices(), str, getPosX(), getPosY() + offset_y, color2.getValue().getColor());
                }
                offset_y += FontRenderers.sf_bold.getFontHeight(str);
            }
        }

        setBounds(getPosX(), getPosY(), size.getValue(), size.getValue());
    }

    private enum Mode {
        Rect, Text
    }

    public enum ColorMode {
        Sync, Custom
    }

    public String getText(Setting<Component> c, PlayerEntity player) {
        switch (c.getValue()) {
            default -> {
                return "";
            }
            case Hp -> {
                int health = (int) Math.ceil(player.getHealth() + player.getAbsorptionAmount());
                return ModuleManager.nameTags.getHealthColor(health) + health + Formatting.RESET;
            }
            case Name -> {
                return c12.getValue() + player.getName().getString() + Formatting.RESET;
            }
            case Ping -> {
                return c22.getValue()+ (NameTags.getEntityPing(player) + "ms") + Formatting.RESET;
            }
            case Distance -> {
                return c32.getValue() + (((int) Math.ceil(mc.player.distanceTo(player))) + "m") + Formatting.RESET;
            }
            case TotemPops -> {
                return c42.getValue() + (ThunderHack.combatManager.getPops(player) + "") + Formatting.RESET;
            }
        }
    }

    public enum Component {
        Name, Hp, Distance, Ping, TotemPops, None
    }
}