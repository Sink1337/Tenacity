package dev.meguru.module.impl.render;

import dev.meguru.Meguru;
import dev.meguru.event.impl.render.PreRenderEvent;
import dev.meguru.event.impl.render.Render2DEvent;
import dev.meguru.event.impl.render.Render3DEvent;
import dev.meguru.event.impl.render.ShaderEvent;
import dev.meguru.module.Category;
import dev.meguru.module.Module;
import dev.meguru.module.api.TargetManager;
import dev.meguru.module.impl.combat.KillAura;
import dev.meguru.module.impl.render.targethud.AutoDoxTargetHUD;
import dev.meguru.module.impl.render.targethud.RiseTargetHUD;
import dev.meguru.module.impl.render.targethud.TargetHUD;
import dev.meguru.module.settings.ParentAttribute;
import dev.meguru.module.settings.impl.BooleanSetting;
import dev.meguru.module.settings.impl.ModeSetting;
import dev.meguru.utils.animations.Animation;
import dev.meguru.utils.animations.Direction;
import dev.meguru.utils.animations.impl.DecelerateAnimation;
import dev.meguru.utils.objects.Dragging;
import dev.meguru.utils.objects.GradientColorWheel;
import dev.meguru.utils.objects.PlayerDox;
import dev.meguru.utils.render.ColorUtil;
import dev.meguru.utils.render.ESPUtil;
import dev.meguru.utils.render.RenderUtil;
import dev.meguru.utils.tuples.Pair;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.util.vector.Vector4f;

import java.awt.*;

public class TargetHUDMod extends Module {

    private final ModeSetting targetHud = new ModeSetting("Mode", "Tenacity", "Tenacity", "Old Tenacity", "Rise", "Exhibition", "Auto-Dox", "Akrien", "Astolfo", "Novoline", "Vape", "Moon");
    private final BooleanSetting trackTarget = new BooleanSetting("Track Target", false);
    private final ModeSetting trackingMode = new ModeSetting("Tracking Mode", "Middle", "Middle", "Top", "Left", "Right");
    private final BooleanSetting Animation = new BooleanSetting("Animation", true);

    public static boolean renderLayers = true;

    private final GradientColorWheel colorWheel = new GradientColorWheel();

    public TargetHUDMod() {
        super("TargetHUD", Category.RENDER, "Displays info about the KillAura target");
        trackingMode.addParent(trackTarget, ParentAttribute.BOOLEAN_CONDITION);
        addSettings(targetHud, trackTarget, trackingMode, Animation, colorWheel.createModeSetting("Color Mode", "Dark"), colorWheel.getColorSetting());
        TargetHUD.init();
    }

    private EntityLivingBase target;
    private final Dragging drag = Meguru.INSTANCE.createDrag(this, "targetHud", 300, 300);

    private final Animation openAnimation = new DecelerateAnimation(175, .5);

    private KillAura killAura;

    private Vector4f targetVector;

    @Override
    public void onRender3DEvent(Render3DEvent event) {
        if (trackTarget.isEnabled() && target != null) {
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (entity instanceof EntityLivingBase) {
                    EntityLivingBase entityLivingBase = (EntityLivingBase) entity;
                    if (target.equals(entityLivingBase)) {
                        targetVector = ESPUtil.getEntityPositionsOn2D(entity);
                    }
                }
            }

        }
    }

    @Override
    public void onPreRenderEvent(PreRenderEvent event) {
        TargetHUD currentTargetHUD = TargetHUD.get(targetHud.getMode());
        drag.setWidth(currentTargetHUD.getWidth());
        drag.setHeight(currentTargetHUD.getHeight());

        if (!Animation.isEnabled() && !openAnimation.isDone()) {
            openAnimation.timerUtil.lastMS = System.currentTimeMillis() - openAnimation.getDuration();
        }


        if (killAura == null) {
            killAura = (KillAura) Meguru.INSTANCE.getModuleCollection().get(KillAura.class);
        }

        AutoDoxTargetHUD autoDoxTargetHud = TargetHUD.get(AutoDoxTargetHUD.class);

        if (!(mc.currentScreen instanceof GuiChat)) {
            if (!killAura.isEnabled()) {
                openAnimation.setDirection(Direction.BACKWARDS);
                if (openAnimation.finished(Direction.BACKWARDS)) {
                    autoDoxTargetHud.doxMap.clear();
                }
            }

            if (target == null && TargetManager.target != null) {
                target = TargetManager.target;
                openAnimation.setDirection(Direction.FORWARDS);

                if (!autoDoxTargetHud.doxMap.containsKey(target)) {
                    autoDoxTargetHud.doxMap.put(target, new PlayerDox(target));
                }

            } else if (TargetManager.target == null || target != TargetManager.target) {
                openAnimation.setDirection(Direction.BACKWARDS);
            }

            if (openAnimation.finished(Direction.BACKWARDS)) {
                TargetHUD.get(RiseTargetHUD.class).particles.clear();
                target = null;
            }
        } else {
            openAnimation.setDirection(Direction.FORWARDS);
            target = mc.thePlayer;
            if (!autoDoxTargetHud.doxMap.containsKey(target)) {
                autoDoxTargetHud.doxMap.put(target, autoDoxTargetHud.thePlayerDox);
            }
        }


        if (target != null) {
            colorWheel.setColorsForMode("Dark", ColorUtil.brighter(new Color(30, 30, 30), .65f));
            colorWheel.setColors();
            currentTargetHUD.setColorWheel(colorWheel);
        }

    }

    @Override
    public void onRender2DEvent(Render2DEvent e) {
        this.setSuffix(targetHud.getMode());
        boolean tracking = trackTarget.isEnabled() && targetVector != null && target != mc.thePlayer;

        TargetHUD currentTargetHUD = TargetHUD.get(targetHud.getMode());

        if (target != null) {
            float trackScale = 1;
            float x = drag.getX(), y = drag.getY();
            if (tracking) {
                float newWidth = (targetVector.getZ() - targetVector.getX()) * 1.4f;
                trackScale = Math.min(1, newWidth / currentTargetHUD.getWidth());

                Pair<Float, Float> coords = getTrackedCoords();
                x = coords.getFirst();
                y = coords.getSecond();
            }

            float scaleOutput;
            if (Animation.isEnabled()) {
                scaleOutput = (float) (.5 + openAnimation.getOutput().floatValue());
            } else {
                scaleOutput = (float) openAnimation.getOutput().floatValue();
                if (openAnimation.getDirection().forwards()) {
                    scaleOutput = (float) openAnimation.getEndPoint();
                } else {
                    scaleOutput = 0.0f;
                }
                scaleOutput = 1.0f;
            }

            RenderUtil.scaleStart(x + drag.getWidth() / 2f, y + drag.getHeight() / 2f, scaleOutput * trackScale);

            float alpha;
            if (Animation.isEnabled()) {
                alpha = Math.min(1, openAnimation.getOutput().floatValue() * 2);
            } else {
                alpha = 1f;
            }

            currentTargetHUD.render(x, y, alpha, target);

            RenderUtil.scaleEnd();
        }
    }


    @Override
    public void onShaderEvent(ShaderEvent e) {
        float x = drag.getX(), y = drag.getY();
        float trackScale = 1;
        TargetHUD currentTargetHUD = TargetHUD.get(targetHud.getMode());
        if (trackTarget.isEnabled() && targetVector != null && target != mc.thePlayer) {
            Pair<Float, Float> coords = getTrackedCoords();
            x = coords.getFirst();
            y = coords.getSecond();

            float newWidth = (targetVector.getZ() - targetVector.getX()) * 1.4f;
            trackScale = Math.min(1, newWidth / currentTargetHUD.getWidth());
        }


        if (target != null) {

            boolean glow = e.getBloomOptions().getSetting("TargetHud").isEnabled();

            float scaleOutput;
            if (Animation.isEnabled()) {
                scaleOutput = (float) (.5 + openAnimation.getOutput().floatValue());
            } else {
                scaleOutput = 1.0f;
            }

            RenderUtil.scaleStart(x + drag.getWidth() / 2f, y + drag.getHeight() / 2f, scaleOutput * trackScale);

            float alpha;
            if (Animation.isEnabled()) {
                alpha = Math.min(1, openAnimation.getOutput().floatValue() * 2);
            } else {
                alpha = 1f;
            }

            currentTargetHUD.renderEffects(x, y, alpha, glow);

            RenderUtil.scaleEnd();
        }
    }


    @Override
    public void onEnable() {
        super.onEnable();

        target = null;
    }


    private Pair<Float, Float> getTrackedCoords() {
        ScaledResolution sr = new ScaledResolution(mc);
        float width = drag.getWidth(), height = drag.getHeight();
        float x = targetVector.getX(), y = targetVector.getY();
        float entityWidth = (targetVector.getZ() - targetVector.getX());
        float entityHeight = (targetVector.getW() - targetVector.getY());
        float middleX = x + entityWidth / 2f - width / 2f;
        float middleY = y + entityHeight / 2f - height / 2f;
        switch (trackingMode.getMode()) {
            case "Middle":
                return Pair.of(middleX, middleY);
            case "Top":
                return Pair.of(middleX, y - (height / 2f + height / 4f));
            case "Left":
                return Pair.of(x - (width / 2f + width / 4f), middleY);
            default:
                return Pair.of(x + entityWidth - (width / 4f), middleY);
        }
    }
}