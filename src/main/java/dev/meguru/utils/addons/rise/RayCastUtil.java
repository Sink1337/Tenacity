package dev.meguru.utils.addons.rise;

import com.google.common.base.Predicates;
import dev.meguru.utils.addons.vector.Vector2f;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.*;

import java.util.List;

import static dev.meguru.utils.Utils.mc;

/**
 * @author Patrick
 * @since 11/17/2021
 */
public final class RayCastUtil {

    public static MovingObjectPosition rayCast(final Vector2f rotation, final double range) {
        return rayCast(rotation, range, 0);
    }

    public static MovingObjectPosition rayCast(Vector2f rotation, double range, float expand, boolean throughWall) {
        return rayCast(rotation, range, expand, mc.thePlayer, throughWall);
    }


    public static MovingObjectPosition rayCast(Vector2f rotation, double range, float expand, Entity entity, boolean throughWall) {
        float partialTicks = mc.timer.renderPartialTicks;
        if (entity != null && mc.theWorld != null) {
            MovingObjectPosition objectMouseOver = entity.rayTraceCustom(range, rotation.x, rotation.y);
            double d1 = range;
            Vec3 vec3 = entity.getPositionEyes(2.0f);
            if (objectMouseOver != null && objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && !throughWall) {
                d1 = objectMouseOver.hitVec.distanceTo(vec3);
            }
            Vec3 vec31 = mc.thePlayer.getVectorForRotation(rotation.y, rotation.x);
            Vec3 vec32 = vec3.addVector(vec31.xCoord * range, vec31.yCoord * range, vec31.zCoord * range);
            Entity pointedEntity = null;
            Vec3 vec33 = null;
            float f = 1.0f;
            List<Entity> list = mc.theWorld.getEntitiesInAABBexcluding(entity, entity.getEntityBoundingBox().addCoord(vec31.xCoord * range, vec31.yCoord * range, vec31.zCoord * range).expand(1.0, 1.0, 1.0), Predicates.and(EntitySelectors.NOT_SPECTATING, Entity::canBeCollidedWith));
            double d2 = d1;
            for (Entity entity1 : list) {
                double d3;
                float f1 = entity1.getCollisionBorderSize() + expand;
                AxisAlignedBB original = entity1.getEntityBoundingBox();
                original = original.offset(entity.posX - entity.prevPosX, entity.posY - entity.prevPosY, entity.posZ - entity.prevPosZ);
                AxisAlignedBB axisalignedbb = f1 >= 0.0f ? original.expand(f1, f1, f1).expand(-f1, -f1, -f1) : original.contract(f1, f1, f1).contract(-f1, -f1, -f1);
                MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);
                if (axisalignedbb.isVecInside(vec3)) {
                    if (!(d2 >= 0.0)) continue;
                    pointedEntity = entity1;
                    vec33 = movingobjectposition == null ? vec3 : movingobjectposition.hitVec;
                    d2 = 0.0;
                    continue;
                }
                if (movingobjectposition == null || !((d3 = vec3.distanceTo(movingobjectposition.hitVec)) < d2) && d2 != 0.0)
                    continue;
                pointedEntity = entity1;
                vec33 = movingobjectposition.hitVec;
                d2 = d3;
            }
            if (pointedEntity != null && (d2 < d1 || objectMouseOver == null)) {
                objectMouseOver = new MovingObjectPosition(pointedEntity, vec33);
            }
            return objectMouseOver;
        }
        return null;
    }

    public static boolean inView(final Entity entity) {
        int renderDistance = 16 * mc.gameSettings.renderDistanceChunks;

        if (entity.threadDistance > 100 || !(entity instanceof EntityPlayer)) {
            Vector2f rotations = RotationUtil.calculate(entity);

            if (Math.abs(MathHelper.wrapAngleTo180_float(rotations.x) - MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw)) > mc.gameSettings.fovSetting) {
                return false;
            }

            MovingObjectPosition movingObjectPosition = RayCastUtil.rayCast(rotations, renderDistance, 0.2f);
            return movingObjectPosition != null && movingObjectPosition.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY;
        } else {
            for (double yPercent = 1; yPercent >= -1; yPercent -= 0.5) {
                for (double xPercent = 1; xPercent >= -1; xPercent -= 1) {
                    for (double zPercent = 1; zPercent >= -1; zPercent -= 1) {
                        Vector2f subRotations = RotationUtil.calculate(entity.getCustomPositionVector().add(
                                (entity.getEntityBoundingBox().maxX - entity.getEntityBoundingBox().minX) * xPercent,
                                (entity.getEntityBoundingBox().maxY - entity.getEntityBoundingBox().minY) * yPercent,
                                (entity.getEntityBoundingBox().maxZ - entity.getEntityBoundingBox().minZ) * zPercent));

                        MovingObjectPosition movingObjectPosition = RayCastUtil.rayCast(subRotations, renderDistance, 0.2f);
                        if (movingObjectPosition != null && movingObjectPosition.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public static MovingObjectPosition rayCast(final Vector2f rotation, final double range, final float expand) {
        return rayCast(rotation, range, expand, mc.thePlayer);
    }

    public static MovingObjectPosition rayCast(final Vector2f rotation, final double range, final float expand, Entity entity) {
        final float partialTicks = mc.timer.renderPartialTicks;
        MovingObjectPosition objectMouseOver;

        if (entity != null && mc.theWorld != null) {
            objectMouseOver = entity.rayTraceCustom(range, rotation.x, rotation.y);
            double d1 = range;
            final Vec3 vec3 = entity.getPositionEyes(partialTicks);

            if (objectMouseOver != null) {
                d1 = objectMouseOver.hitVec.distanceTo(vec3);
            }

            final Vec3 vec31 = mc.thePlayer.getVectorForRotation(rotation.y, rotation.x);
            final Vec3 vec32 = vec3.addVector(vec31.xCoord * range, vec31.yCoord * range, vec31.zCoord * range);
            Entity pointedEntity = null;
            Vec3 vec33 = null;
            final float f = 1.0F;
            final List<Entity> list = mc.theWorld.getEntitiesInAABBexcluding(entity, entity.getEntityBoundingBox().addCoord(vec31.xCoord * range, vec31.yCoord * range, vec31.zCoord * range).expand(f, f, f), Predicates.and(EntitySelectors.NOT_SPECTATING, Entity::canBeCollidedWith));
            double d2 = d1;

            for (final Entity entity1 : list) {
                final float f1 = entity1.getCollisionBorderSize() + expand;
                final AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand(f1, f1, f1);
                final MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);

                if (axisalignedbb.isVecInside(vec3)) {
                    if (d2 >= 0.0D) {
                        pointedEntity = entity1;
                        vec33 = movingobjectposition == null ? vec3 : movingobjectposition.hitVec;
                        d2 = 0.0D;
                    }
                } else if (movingobjectposition != null) {
                    final double d3 = vec3.distanceTo(movingobjectposition.hitVec);

                    if (d3 < d2 || d2 == 0.0D) {
                        pointedEntity = entity1;
                        vec33 = movingobjectposition.hitVec;
                        d2 = d3;
                    }
                }
            }

            if (pointedEntity != null && (d2 < d1 || objectMouseOver == null)) {
                objectMouseOver = new MovingObjectPosition(pointedEntity, vec33);
            }

            return objectMouseOver;
        }

        return null;
    }

    public static boolean overBlock(final Vector2f rotation, final EnumFacing enumFacing, final BlockPos pos, final boolean strict) {
        final MovingObjectPosition movingObjectPosition = mc.thePlayer.rayTraceCustom(4.5f, rotation.x, rotation.y);

        if (movingObjectPosition == null) return false;

        final Vec3 hitVec = movingObjectPosition.hitVec;
        if (hitVec == null) return false;

        return movingObjectPosition.getBlockPos().equals(pos) && (!strict || movingObjectPosition.sideHit == enumFacing);
    }

    public static boolean overBlock(final EnumFacing enumFacing, final BlockPos pos, final boolean strict) {
        if (mc.objectMouseOver == null) {
            return false;
        }
        final MovingObjectPosition movingObjectPosition = mc.objectMouseOver;

        if (movingObjectPosition == null) return false;

        final Vec3 hitVec = movingObjectPosition.hitVec;
        if (hitVec == null) return false;

        return movingObjectPosition.getBlockPos().equals(pos) && (!strict || movingObjectPosition.sideHit == enumFacing);
    }

    public static Boolean overBlock(final Vector2f rotation, final BlockPos pos) {
        return overBlock(rotation, EnumFacing.UP, pos, false);
    }

    public static Boolean overBlock(final Vector2f rotation, final BlockPos pos, final EnumFacing enumFacing) {
        return overBlock(rotation, enumFacing, pos, true);
    }
}