package dev.meguru.utils.player;

import com.google.common.base.Predicates;
import dev.meguru.event.impl.player.MotionEvent;
import dev.meguru.utils.Utils;
import dev.meguru.utils.addons.rise.MathConst;
import dev.meguru.utils.addons.vector.Rotation;
import dev.meguru.utils.addons.vector.Vector2f;
import dev.meguru.utils.misc.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.util.*;
import store.intent.intentguard.annotation.Exclude;
import store.intent.intentguard.annotation.Strategy;

import java.util.List;

import static dev.meguru.utils.addons.rise.RotationUtils.toRotation;

public class RotationUtils implements Utils {

    /*
     * Sets the player's head rotations to the given yaw and pitch (visual-only).
     */
    @Exclude(Strategy.NAME_REMAPPING)
    public static void setVisualRotations(float yaw, float pitch) {
        mc.thePlayer.rotationYawHead = mc.thePlayer.renderYawOffset = yaw;
        mc.thePlayer.rotationPitchHead = pitch;
    }

    public static void setVisualRotations(float[] rotations) {
        setVisualRotations(rotations[0], rotations[1]);
    }

    public static void setVisualRotations(MotionEvent e) {
        setVisualRotations(e.getYaw(), e.getPitch());
    }

    public static float clampRotation() {
        float rotationYaw = Minecraft.getMinecraft().thePlayer.rotationYaw;
        float n = 1.0f;
        if (Minecraft.getMinecraft().thePlayer.movementInput.moveForward < 0.0f) {
            rotationYaw += 180.0f;
            n = -0.5f;
        } else if (Minecraft.getMinecraft().thePlayer.movementInput.moveForward > 0.0f) {
            n = 0.5f;
        }
        if (Minecraft.getMinecraft().thePlayer.movementInput.moveStrafe > 0.0f) {
            rotationYaw -= 90.0f * n;
        }
        if (Minecraft.getMinecraft().thePlayer.movementInput.moveStrafe < 0.0f) {
            rotationYaw += 90.0f * n;
        }
        return rotationYaw * 0.017453292f;
    }

    public static float getSensitivityMultiplier() {
        float SENSITIVITY = Minecraft.getMinecraft().gameSettings.mouseSensitivity * 0.6F + 0.2F;
        return (SENSITIVITY * SENSITIVITY * SENSITIVITY * 8.0F) * 0.15F;
    }

    public static float smoothRotation(float from, float to, float speed) {
        float f = MathHelper.wrapAngleTo180_float(to - from);

        if (f > speed) {
            f = speed;
        }

        if (f < -speed) {
            f = -speed;
        }

        return from + f;
    }

    public static float[] getFacingRotations(ScaffoldUtils.BlockCache blockCache) {
        double d1 = blockCache.getPosition().getX() + 0.5D - mc.thePlayer.posX + blockCache.getFacing().getFrontOffsetX() / 2.0D;
        double d2 = blockCache.getPosition().getZ() + 0.5D - mc.thePlayer.posZ + blockCache.getFacing().getFrontOffsetZ() / 2.0D;
        double d3 = mc.thePlayer.posY + mc.thePlayer.getEyeHeight() - (blockCache.getPosition().getY());
        double d4 = MathHelper.sqrt_double(d1 * d1 + d2 * d2);
        float f1 = (float) (Math.atan2(d2, d1) * 180.0D / Math.PI) - 90.0F;
        float f2 = (float) (Math.atan2(d3, d4) * 180.0D / Math.PI);
        if (f1 < 0.0F) {
            f1 += 360.0F;
        }
        return new float[]{f1, f2};
    }

    public static float[] getRotations(BlockPos blockPos) {
        return getRotations(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5, mc.thePlayer.posX, mc.thePlayer.posY + (double)mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
    }

    public static float[] getRotations(double rotX, double rotY, double rotZ, double startX, double startY, double startZ) {
        double x = rotX - startX;
        double y = rotY - startY;
        double z = rotZ - startZ;
        double dist = MathHelper.sqrt_double(x * x + z * z);
        float yaw = (float) (Math.atan2(z, x) * 180.0 / Math.PI) - 90.0F;
        float pitch = (float) (-(Math.atan2(y, dist) * 180.0 / Math.PI));
        return new float[]{yaw, pitch};
    }

    public static float[] getRotations(BlockPos blockPos, EnumFacing enumFacing) {
        double d = (double) blockPos.getX() + 0.5 - mc.thePlayer.posX + (double) enumFacing.getFrontOffsetX() * 0.25;
        double d2 = (double) blockPos.getZ() + 0.5 - mc.thePlayer.posZ + (double) enumFacing.getFrontOffsetZ() * 0.25;
        double d3 = mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight() - blockPos.getY() - (double) enumFacing.getFrontOffsetY() * 0.25;
        double d4 = MathHelper.sqrt_double(d * d + d2 * d2);
        float f = (float) (Math.atan2(d2, d) * 180.0 / Math.PI) - 90.0f;
        float f2 = (float) (Math.atan2(d3, d4) * 180.0 / Math.PI);
        return new float[]{MathHelper.wrapAngleTo180_float(f), f2};
    }

    public static float[] getRotationsNeeded(final Entity entity) {
        if (entity == null) {
            return null;
        }
        Minecraft mc = Minecraft.getMinecraft();
        final double xSize = entity.posX - mc.thePlayer.posX;
        final double ySize = entity.posY + entity.getEyeHeight() / 2 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        final double zSize = entity.posZ - mc.thePlayer.posZ;
        final double theta = MathHelper.sqrt_double(xSize * xSize + zSize * zSize);
        final float yaw = (float) (Math.atan2(zSize, xSize) * 180 / Math.PI) - 90;
        final float pitch = (float) (-(Math.atan2(ySize, theta) * 180 / Math.PI));
        return new float[]{(mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw)) % 360, (mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - mc.thePlayer.rotationPitch)) % 360.0f};
    }

    public static float[] getRotationsDiff(Entity entity) {
        if (entity == null) {
            return null;
        }
        float[] rotations = getRotationsNeeded(entity);
        return new float[]{Math.abs(mc.thePlayer.rotationYaw - rotations[0]), Math.abs(mc.thePlayer.rotationPitch - rotations[1])};
    }

    public static float[] getFacingRotations2(final int paramInt1, final double d, final int paramInt3) {
        final EntitySnowball localEntityPig = new EntitySnowball(Minecraft.getMinecraft().theWorld);
        localEntityPig.posX = paramInt1 + 0.5;
        localEntityPig.posY = d + 0.5;
        localEntityPig.posZ = paramInt3 + 0.5;
        return getRotationsNeeded(localEntityPig);
    }

    public static float getEnumRotations(EnumFacing facing) {
        float yaw = 0;
        if (facing == EnumFacing.NORTH) {
            yaw = 0;
        }
        if (facing == EnumFacing.EAST) {
            yaw = 90;
        }
        if (facing == EnumFacing.WEST) {
            yaw = -90;
        }
        if (facing == EnumFacing.SOUTH) {
            yaw = 180;
        }
        return yaw;
    }

    public static float getYaw(Vec3 to) {
        float x = (float) (to.xCoord - mc.thePlayer.posX);
        float z = (float) (to.zCoord - mc.thePlayer.posZ);
        float var1 = (float) (StrictMath.atan2(z, x) * 180.0D / StrictMath.PI) - 90.0F;
        float rotationYaw = mc.thePlayer.rotationYaw;
        return rotationYaw + MathHelper.wrapAngleTo180_float(var1 - rotationYaw);
    }

    public static Vec3 getVecRotations(float yaw, float pitch) {
        double d = Math.cos(Math.toRadians(-yaw) - Math.PI);
        double d1 = Math.sin(Math.toRadians(-yaw) - Math.PI);
        double d2 = -Math.cos(Math.toRadians(-pitch));
        double d3 = Math.sin(Math.toRadians(-pitch));
        return new Vec3(d1 * d2, d3, d * d2);
    }

    public static Vec3 getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float)Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float)Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3((double)(f1 * f2), (double)f3, (double)(f * f2));
    }


    public static float[] getRotations(double posX, double posY, double posZ) {
        double x = posX - mc.thePlayer.posX, z = posZ - mc.thePlayer.posZ, y = posY - (mc.thePlayer.getEyeHeight() + mc.thePlayer.posY);
        double d3 = MathHelper.sqrt_double(x * x + z * z);
        float yaw = (float) (MathHelper.atan2(z, x) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) (-(MathHelper.atan2(y, d3) * 180.0D / Math.PI));
        return new float[]{yaw, pitch};
    }

    public static float[] getSmoothRotations(EntityLivingBase entity) {
        float f1 = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float fac = f1 * f1 * f1 * 256.0F;

        double x = entity.posX - mc.thePlayer.posX;
        double z = entity.posZ - entity.posZ;
        double y = entity.posY + entity.getEyeHeight()
                - (mc.thePlayer.getEntityBoundingBox().minY
                + (mc.thePlayer.getEntityBoundingBox().maxY
                - mc.thePlayer.getEntityBoundingBox().minY));

        double d3 = MathHelper.sqrt_double(x * x + z * z);
        float yaw = (float) (MathHelper.atan2(z, x) * 180.0 / Math.PI) - 90.0F;
        float pitch = (float) (-(MathHelper.atan2(y, d3) * 180.0 / Math.PI));
        yaw = smoothRotation(mc.thePlayer.prevRotationYawHead, yaw, fac * MathUtils.getRandomFloat(0.9F, 1));
        pitch = smoothRotation(mc.thePlayer.prevRotationPitchHead, pitch, fac * MathUtils.getRandomFloat(0.7F, 1));

        return new float[]{yaw, pitch};
    }

    public static boolean isMouseOver(final float yaw, final float pitch, final Entity target, final float range) {
        final float partialTicks = mc.timer.renderPartialTicks;
        final Entity entity = mc.getRenderViewEntity();
        MovingObjectPosition objectMouseOver;
        Entity mcPointedEntity = null;

        if (entity != null && mc.theWorld != null) {

            mc.mcProfiler.startSection("pick");
            final double d0 = mc.playerController.getBlockReachDistance();
            objectMouseOver = entity.rayTrace(d0, partialTicks);
            double d1 = d0;
            final Vec3 vec3 = entity.getPositionEyes(partialTicks);
            final boolean flag = d0 > (double) range;

            if (objectMouseOver != null) {
                d1 = objectMouseOver.hitVec.distanceTo(vec3);
            }

            final Vec3 vec31 = getVectorForRotation(pitch, yaw);
            final Vec3 vec32 = vec3.addVector(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0);
            Entity pointedEntity = null;
            Vec3 vec33 = null;
            final float f = 1.0F;
            final List<Entity> list = mc.theWorld.getEntitiesInAABBexcluding(entity, entity.getEntityBoundingBox().addCoord(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0).expand(f, f, f), Predicates.and(EntitySelectors.NOT_SPECTATING, Entity::canBeCollidedWith));
            double d2 = d1;

            for (final Entity entity1 : list) {
                final float f1 = entity1.getCollisionBorderSize();
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

            if (pointedEntity != null && flag && vec3.distanceTo(vec33) > (double) range) {
                pointedEntity = null;
                objectMouseOver = new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, vec33, null, new BlockPos(vec33));
            }

            if (pointedEntity != null && (d2 < d1 || objectMouseOver == null)) {
                if (pointedEntity instanceof EntityLivingBase || pointedEntity instanceof EntityItemFrame) {
                    mcPointedEntity = pointedEntity;
                }
            }

            mc.mcProfiler.endSection();

            return mcPointedEntity == target;
        }

        return false;
    }

    public static Vector2f calculate(final dev.meguru.utils.addons.vector.Vector3d position, final EnumFacing enumFacing) {
        double x = position.getX() + 0.5D;
        double y = position.getY() + 0.5D;
        double z = position.getZ() + 0.5D;

        x += (double) enumFacing.getDirectionVec().getX() * 0.5D;
        y += (double) enumFacing.getDirectionVec().getY() * 0.5D;
        z += (double) enumFacing.getDirectionVec().getZ() * 0.5D;
        return calculate(new dev.meguru.utils.addons.vector.Vector3d(x, y, z));
    }

    public static Vector2f calculate(final dev.meguru.utils.addons.vector.Vector3d to) {
        return calculate(mc.thePlayer.getCustomPositionVector().add(mc.thePlayer.motionX, mc.thePlayer.getEyeHeight() + mc.thePlayer.motionY, mc.thePlayer.motionZ), to);
    }

    public static Vector2f calculate(final dev.meguru.utils.addons.vector.Vector3d from, final dev.meguru.utils.addons.vector.Vector3d to) {
        final dev.meguru.utils.addons.vector.Vector3d diff = to.subtract(from);
        final double distance = Math.hypot(diff.getX(), diff.getZ());
        final float yaw = (float) (MathHelper.atan2(diff.getZ(), diff.getX()) * MathConst.TO_DEGREES) - 90.0F;
        final float pitch = (float) (-(MathHelper.atan2(diff.getY(), distance) * MathConst.TO_DEGREES));
        return new Vector2f(yaw, pitch);
    }

    public static Rotation rotationToFace(BlockPos targetPos, EnumFacing targetFace, Vec3 helpVector) {
        AxisAlignedBB bb = mc.theWorld.getBlockState(targetPos).getBlock().getCollisionBoundingBox(mc.theWorld, targetPos, mc.theWorld.getBlockState(targetPos));
        double height = bb.maxY - bb.minY;
        double xWidth = bb.maxX - bb.minX;
        double zWidth = bb.maxZ - bb.minZ;
        Vec3 hitVec = new Vec3(bb.minX, bb.minY, bb.minZ).add(new Vec3(xWidth / 2f, height / 2f, zWidth / 2f));
        Vec3i faceVec = targetFace.getDirectionVec();
        Vec3 directionVec = new Vec3(faceVec.getX() * (xWidth / 2f), faceVec.getY() * (height / 2f), faceVec.getZ() * (zWidth / 2f));
        hitVec = hitVec.add(directionVec);
        double max = 0.4;
        double fixX = 0.0;
        double fixZ = 0.0;
        double fixY = 0.0;
        if (helpVector != null) {
            if (directionVec.getX() == 0) {
                fixX += Math.min(-xWidth / 2f * max, Math.max(xWidth / 2f * max, helpVector.getX() - hitVec.getX()));
            }
            if (directionVec.getY() == 0) {
                fixY += Math.min(-height / 2f * max, Math.max(height / 2f * max, helpVector.getY() - hitVec.getY()));
            }
            if (directionVec.getZ() == 0) {
                fixZ += Math.min(-zWidth / 2f * max, Math.max(zWidth / 2f * max, helpVector.getZ() - hitVec.getZ()));
            }
        }
        hitVec = hitVec.add(new Vec3(fixX, fixY, fixZ));
        return toRotation(hitVec);
    }

    public static float getMovementYaw() {
        float yaw = 0.0f;
        double moveForward = mc.thePlayer.moveForward;
        double moveStrafe = mc.thePlayer.moveStrafing;
        if (moveForward == 0.0) {
            if (moveStrafe == 0.0) {
                yaw = 180.0f;
            } else if (moveStrafe > 0.0) {
                yaw = 90.0f;
            } else if (moveStrafe < 0.0) {
                yaw = -90.0f;
            }
        } else if (moveForward > 0.0) {
            if (moveStrafe == 0.0) {
                yaw = 180.0f;
            } else if (moveStrafe > 0.0) {
                yaw = 135.0f;
            } else if (moveStrafe < 0.0) {
                yaw = -135.0f;
            }
        } else if (moveForward < 0.0) {
            if (moveStrafe == 0.0) {
                yaw = 0.0f;
            } else if (moveStrafe > 0.0) {
                yaw = 45.0f;
            } else if (moveStrafe < 0.0) {
                yaw = -45.0f;
            }
        }
        return (MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) + yaw % 360 + 360) % 360;
    }
}