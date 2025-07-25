package dev.meguru.module.impl.combat;

import de.florianmichael.viamcp.fixes.AttackOrder;
import dev.meguru.Meguru;
import dev.meguru.commands.impl.FriendCommand;
import dev.meguru.event.impl.game.TickEvent;
import dev.meguru.event.impl.player.AttackEvent;
import dev.meguru.event.impl.player.KeepSprintEvent;
import dev.meguru.event.impl.player.MotionEvent;
import dev.meguru.event.impl.render.Render3DEvent;
import dev.meguru.module.Category;
import dev.meguru.module.Module;
import dev.meguru.module.api.TargetManager;
import dev.meguru.module.impl.player.Scaffold;
import dev.meguru.module.impl.render.HUDMod;
import dev.meguru.module.settings.impl.*;
import dev.meguru.utils.BlinkUtils;
import dev.meguru.utils.addons.rise.MovementFix;
import dev.meguru.utils.addons.rise.component.RotationComponent;
import dev.meguru.utils.addons.vector.Vector2f;
import dev.meguru.utils.animations.Animation;
import dev.meguru.utils.animations.Direction;
import dev.meguru.utils.animations.impl.DecelerateAnimation;
import dev.meguru.utils.misc.MathUtils;
import dev.meguru.utils.player.InventoryUtils;
import dev.meguru.utils.player.RotationUtils;
import dev.meguru.utils.render.RenderUtil;
import dev.meguru.utils.server.PacketUtils;
import dev.meguru.utils.time.TimerUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class KillAura extends Module {

    public static boolean attacking;
    public static boolean blocking;
    public static boolean wasBlocking;
    private float yaw = 0;
    private int cps;
    public static final List<EntityLivingBase> targets = new ArrayList<>();
    private final TimerUtil attackTimer = new TimerUtil();
    private final TimerUtil switchTimer = new TimerUtil();
    private int currentTargetIndex = 0;
    private int blocksmcTick;

    private final ModeSetting mode = new ModeSetting("Mode", "Single", "Single", "Switch", "Multi");

    private final NumberSetting switchDelay = new NumberSetting("Switch Delay", 50, 500, 0, 25);
    private final NumberSetting maxTargetAmount = new NumberSetting("Max Target Amount", 3, 50, 2, 1);

    private final NumberSetting minCPS = new NumberSetting("Min CPS", 10, 20, 1, 1);
    private final NumberSetting maxCPS = new NumberSetting("Max CPS", 10, 20, 1, 1);
    private final NumberSetting reach = new NumberSetting("Reach", 4, 6, 3, 0.1);

    private final BooleanSetting autoblock = new BooleanSetting("Autoblock", false);
    private final ModeSetting autoblockMode = new ModeSetting("Autoblock Mode", "Watchdog", "Fake", "Verus", "Watchdog", "Interact", "BlocksMC A", "BlocksMC B");

    private final BooleanSetting rotations = new BooleanSetting("Rotations", true);
    private final ModeSetting rotationMode = new ModeSetting("Rotation Mode", "Vanilla", "Vanilla", "Smooth", "HVH", "BlocksMC");
    private final NumberSetting rotationSpeed = new NumberSetting("Rotation speed", 5, 10, 2, 0.1);
    public static final ModeSetting movementFix = new ModeSetting("Movement fix Mode", "Traditional", "Off", "Normal", "Traditional", "Backwards Sprint");

    private final ModeSetting sortMode = new ModeSetting("Sort Mode", "Range", "Range", "Hurt Time", "Health", "Armor");

    private final MultipleBoolSetting addons = new MultipleBoolSetting("Addons",
            new BooleanSetting("Keep Sprint", true),
            new BooleanSetting("Through Walls", true),
            new BooleanSetting("Allow Scaffold", false),
            new BooleanSetting("Movement Fix", false),
            new BooleanSetting("Ray Cast", false));

    private final MultipleBoolSetting auraESP = new MultipleBoolSetting("Target ESP",
            new BooleanSetting("Circle", true),
            new BooleanSetting("Tracer", false),
            new BooleanSetting("Box", false),
            new BooleanSetting("Custom Color", false));
    private final ColorSetting customColor = new ColorSetting("Custom Color", Color.WHITE);
    private EntityLivingBase auraESPTarget;

    public KillAura() {
        super("KillAura", Category.COMBAT, "Automatically attacks players");
        autoblockMode.addParent(autoblock, a -> autoblock.isEnabled());
        rotationMode.addParent(rotations, r -> rotations.isEnabled());
        rotationSpeed.addParent(rotations, r -> rotations.isEnabled());
        movementFix.addParent(addons, r -> r.isEnabled("Movement Fix"));
        switchDelay.addParent(mode, m -> mode.is("Switch"));
        maxTargetAmount.addParent(mode, m -> mode.is("Multi"));
        customColor.addParent(auraESP, r -> r.isEnabled("Custom Color"));
        this.addSettings(mode, maxTargetAmount, switchDelay, minCPS, maxCPS, reach, autoblock, autoblockMode,
                rotations, rotationMode, rotationSpeed, movementFix, sortMode, addons, auraESP, customColor);
    }

    @Override
    public void onEnable() {
        blocksmcTick = 0;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        TargetManager.target = null;
        targets.clear();
        BlinkUtils.stopBlink();
        blocking = false;
        attacking = false;
        if (wasBlocking) {
            PacketUtils.sendPacketNoEvent(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        }
        wasBlocking = false;
        currentTargetIndex = 0;
        blocksmcTick = 0;
        super.onDisable();
    }

    @Override
    public void onTickEvent(TickEvent event) {
        if (autoblock.isEnabled() && autoblockMode.is("BlocksMC B")) {
            return;
        }
        if (event.isPost()) {
            if (TargetManager.target != null) {
                if (attackTimer.hasTimeElapsed(cps, true)) {
                    final int maxValue = (int) ((minCPS.getMaxValue() - maxCPS.getValue()) * 20);
                    final int minValue = (int) ((minCPS.getMaxValue() - minCPS.getValue()) * 20);
                    cps = MathUtils.getRandomInRange(minValue, maxValue);
                    AttackEvent attackEvent = new AttackEvent(TargetManager.target);
                    Meguru.INSTANCE.getEventProtocol().handleEvent(attackEvent);
                    attack();
                }
            }
        }
    }

    @Override
    public void onMotionEvent(MotionEvent event) {
        this.setSuffix(mode.getMode());
        final double minRotationSpeed = rotationSpeed.getValue();
        final double maxRotationSpeed = rotationSpeed.getValue();
        final float rotationSpeed = (float) MathUtils.getRandom(minRotationSpeed, maxRotationSpeed);
        if (minCPS.getValue() > maxCPS.getValue()) {
            minCPS.setValue(minCPS.getValue() - 1);
        }

        sortTargets();

        if (autoblock.isEnabled() && InventoryUtils.isHoldingSword() && autoblockMode.is("BlocksMC B") && !targets.isEmpty() && !Meguru.INSTANCE.isEnabled(Scaffold.class)) {
            if (event.isPre()) {
                TargetManager.target = targets.get(0);

                if (rotations.isEnabled()) {
                    float[] rotations = RotationUtils.getRotationsNeeded(TargetManager.target);
                    RotationComponent.setRotations(new Vector2f(rotations[0], rotations[1]), rotationSpeed, MovementFix.values()[movementFix.modes.indexOf(movementFix.getMode())]);
                }

                if (mc.thePlayer.getDistanceToEntity(TargetManager.target) > reach.getValue()) {
                    TargetManager.target = null;
                    blocksmcTick = 0;
                    return;
                }

                if (blocksmcTick == 0) {
                    PacketUtils.sendPacketNoEvent(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                    wasBlocking = false;
                    blocksmcTick++;
                } else if (blocksmcTick == 1) {
                    if (attackTimer.hasTimeElapsed(cps, true)) {
                        cps = (int) MathUtils.getRandomInRange(minCPS.getValue(), maxCPS.getValue());
                        attackEntity(TargetManager.target);
                    }
                    blocksmcTick++;
                } else if (blocksmcTick == 2) {
                    blocksmcTick++;
                } else if (blocksmcTick == 3) {
                    if (attackTimer.hasTimeElapsed(cps, true)) {
                        cps = (int) MathUtils.getRandomInRange(minCPS.getValue(), maxCPS.getValue());
                        attackEntity(TargetManager.target);
                    }
                    PacketUtils.sendPacketNoEvent(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                    wasBlocking = true;
                    blocksmcTick = 0;
                }
            }
            return;
        }


        if (event.isPre()) {
            attacking = !targets.isEmpty() && (addons.getSetting("Allow Scaffold").isEnabled() || !Meguru.INSTANCE.isEnabled(Scaffold.class));
            blocking = autoblock.isEnabled() && attacking && InventoryUtils.isHoldingSword();

            if (attacking) {
                if (mode.is("Switch")) {
                    if (switchTimer.hasTimeElapsed(switchDelay.getValue().longValue(), true)) {
                        currentTargetIndex++;
                        if (currentTargetIndex >= targets.size()) {
                            currentTargetIndex = 0;
                        }
                    }
                    if (!targets.isEmpty() && currentTargetIndex < targets.size()) {
                        TargetManager.target = targets.get(currentTargetIndex);
                    } else {
                        TargetManager.target = null;
                        currentTargetIndex = 0;
                    }
                } else {
                    if (!targets.isEmpty()) {
                        TargetManager.target = targets.get(0);
                    } else {
                        TargetManager.target = null;
                    }
                }


                if (TargetManager.target != null) {
                    if (rotations.isEnabled()) {
                        float[] rotations = new float[]{0, 0};
                        switch (rotationMode.getMode()) {
                            case "Vanilla":
                                rotations = RotationUtils.getRotationsNeeded(TargetManager.target);
                                break;
                            case "Smooth":
                                rotations = RotationUtils.getSmoothRotations(TargetManager.target);
                                break;
                            case "HVH":
                                rotations = dev.meguru.utils.addons.rise.RotationUtils.getHVHRotation(TargetManager.target);
                                break;
                            case "BlocksMC":
                                rotations = RotationUtils.getRotationsNeeded(TargetManager.target);
                                float mouseSensitivity = mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
                                double multiplier = (mouseSensitivity * mouseSensitivity * mouseSensitivity * 8.0f) * 0.15D;
                                rotations[0] = (float) (Math.round(rotations[0] / multiplier) * multiplier);
                                rotations[1] = (float) (Math.round(rotations[1] / multiplier) * multiplier);
                                break;
                        }
                        yaw = event.getYaw();
                        RotationComponent.setRotations(new Vector2f(rotations[0], rotations[1]), rotationSpeed, MovementFix.values()[movementFix.modes.indexOf(movementFix.getMode())]);
                    }

                    if (!RotationComponent.isRotationg || mc.thePlayer.getDistanceToEntity(TargetManager.target) > reach.getValue() || (addons.getSetting("Ray Cast").isEnabled() && !RotationUtils.isMouseOver(event.getYaw(), event.getPitch(), TargetManager.target, reach.getValue().floatValue())))
                        return;

                }
            } else {
                TargetManager.target = null;
                BlinkUtils.stopBlink();
                switchTimer.reset();
                currentTargetIndex = 0;
            }
        }

        if (blocking) {
            switch (autoblockMode.getMode()) {
                case "Watchdog":
                    if (event.isPre() && wasBlocking && mc.thePlayer.ticksExisted % 4 == 0) {
                        PacketUtils.sendPacketNoEvent(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                        wasBlocking = false;
                    }
                    if (event.isPost() && !wasBlocking) {
                        PacketUtils.sendPacketNoEvent(new C08PacketPlayerBlockPlacement(BlockPos.ORIGIN, 255, mc.thePlayer.getHeldItem(), 255, 255, 255));
                        wasBlocking = true;
                    }
                    break;
                case "Verus":
                    if (event.isPre()) {
                        if (wasBlocking) {
                            PacketUtils.sendPacketNoEvent(new C07PacketPlayerDigging(C07PacketPlayerDigging.
                                    Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                        }
                        if (mc.thePlayer.getHeldItem() != null) {
                            PacketUtils.sendPacketNoEvent(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                            wasBlocking = true;
                        }
                    }
                    break;
                case "BlocksMC A":
                    if (event.isPre() && wasBlocking) {
                        PacketUtils.sendPacketNoEvent(new C09PacketHeldItemChange((mc.thePlayer.inventory.currentItem + 1) % 9));
                        PacketUtils.sendPacketNoEvent(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                    }
                    if (event.isPost()) {
                        PacketUtils.sendPacketNoEvent(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                        wasBlocking = true;
                    }
                    break;
                case "Fake":
                    break;
            }
        } else if (wasBlocking) {
            PacketUtils.sendPacketNoEvent(new C07PacketPlayerDigging(C07PacketPlayerDigging.
                    Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
            wasBlocking = false;
        }
    }

    private void sortTargets() {
        targets.clear();
        if (mc.theWorld == null) return;

        for (Entity entity : mc.theWorld.getLoadedEntityList()) {
            if (entity instanceof EntityLivingBase && mc.thePlayer != null && mc.thePlayer != entity) {
                EntityLivingBase entityLivingBase = (EntityLivingBase) entity;
                if (entityLivingBase != null && mc.thePlayer.getDistanceToEntity(entity) <= reach.getValue() && isValid(entity) && !FriendCommand.isFriend(entityLivingBase.getName())) {
                    targets.add(entityLivingBase);
                }
            }
        }
        if (!targets.isEmpty()) {
            switch (sortMode.getMode()) {
                case "Range":
                    targets.sort(Comparator.comparingDouble(mc.thePlayer::getDistanceToEntity));
                    break;
                case "Hurt Time":
                    targets.sort(Comparator.comparingInt(EntityLivingBase::getHurtTime));
                    break;
                case "Health":
                    targets.sort(Comparator.comparingDouble(EntityLivingBase::getHealth));
                    break;
                case "Armor":
                    targets.sort(Comparator.comparingInt(EntityLivingBase::getTotalArmorValue));
                    break;
            }
        }
    }

    public boolean isValid(Entity entity) {
        if (mc.thePlayer == null || entity == null) return false;
        if (!addons.isEnabled("Through Walls") && !mc.thePlayer.canEntityBeSeen(entity)) return false;
        else return TargetManager.checkEntity(entity);
    }

    @Override
    public void onKeepSprintEvent(KeepSprintEvent event) {
        if (addons.getSetting("Keep Sprint").isEnabled()) {
            event.cancel();
        }
    }



    private final Animation auraESPAnim = new DecelerateAnimation(300, 1);

    @Override
    public void onRender3DEvent(Render3DEvent event) {
        auraESPAnim.setDirection(TargetManager.target != null ? Direction.FORWARDS : Direction.BACKWARDS);
        if (TargetManager.target != null) {
            auraESPTarget = TargetManager.target;
        }

        if (auraESPAnim.finished(Direction.BACKWARDS)) {
            auraESPTarget = null;
        }

        Color color = HUDMod.getClientColors().getFirst();

        if (auraESP.isEnabled("Custom Color")) {
            color = customColor.getColor();
        }

        if (auraESPTarget != null) {
            if (auraESP.getSetting("Box").isEnabled()) {
                RenderUtil.renderBoundingBox(auraESPTarget, color, auraESPAnim.getOutput().floatValue());
            }
            if (auraESP.getSetting("Circle").isEnabled()) {
                RenderUtil.drawCircle(auraESPTarget, event.getTicks(), .75f, color.getRGB(), auraESPAnim.getOutput().floatValue());
            }

            if (auraESP.getSetting("Tracer").isEnabled()) {
                RenderUtil.drawTracerLine(auraESPTarget, 4f, Color.BLACK, auraESPAnim.getOutput().floatValue());
                RenderUtil.drawTracerLine(auraESPTarget, 2.5f, color, auraESPAnim.getOutput().floatValue());
            }
        }
    }

    private void attack() {
        if (TargetManager.target != null) {
            if (autoblockMode.is("Interact")) {
                if (wasBlocking) {
                    unblock();
                }
            }
            attackEntity(TargetManager.target);
            if (autoblockMode.is("Interact")) {
                hypblock(true);
            }
        }
    }

    private void attackEntity(final Entity target) {
        AttackOrder.sendFixedAttack(mc.thePlayer, target);
        attackTimer.reset();
    }

    private void hypblock(boolean interact) {
        if (wasBlocking)
            return;
        EntityLivingBase targetEntity = TargetManager.target;
        //BlinkUtils.stopBlink();
        if (interact)
            PacketUtils.sendPacket(new C02PacketUseEntity(targetEntity, C02PacketUseEntity.Action.INTERACT));
        PacketUtils.sendBlocking(true, false);
        //BlinkUtils.startBlink();
        wasBlocking = true;
    }

    private void unblock() {
        if (!this.wasBlocking)
            return;
        if (!mc.gameSettings.keyBindUseItem.isKeyDown()) {
            PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        } else if (canAutoBlock()) {
            mc.gameSettings.keyBindUseItem.pressed = false;
        }
        this.wasBlocking = false;
    }

    private boolean canAutoBlock() {
        return (TargetManager.target != null && mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof net.minecraft.item.ItemSword && mc.thePlayer.getDistanceToEntity(TargetManager.target) < this.reach.getValue().doubleValue());
    }
}