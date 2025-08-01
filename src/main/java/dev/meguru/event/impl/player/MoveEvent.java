package dev.meguru.event.impl.player;

import dev.meguru.event.Event;
import dev.meguru.utils.player.MovementUtils;
import store.intent.intentguard.annotation.Exclude;
import store.intent.intentguard.annotation.Strategy;

public class MoveEvent extends Event {

    private double x, y, z;
    private double friction;
    private float strafe;
    private float forward;

    public MoveEvent(double x, double y, double z, float strafe, float forward) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.strafe = strafe;
        this.forward = forward;
        this.friction = 0.91;
    }

    @Exclude(Strategy.NAME_REMAPPING)
    public double getX() {
        return x;
    }

    @Exclude(Strategy.NAME_REMAPPING)
    public void setX(double x) {
        this.x = x;
    }

    @Exclude(Strategy.NAME_REMAPPING)
    public double getY() {
        return y;
    }

    @Exclude(Strategy.NAME_REMAPPING)
    public void setY(double y) {
        this.y = y;
    }

    @Exclude(Strategy.NAME_REMAPPING)
    public double getZ() {
        return z;
    }

    @Exclude(Strategy.NAME_REMAPPING)
    public void setZ(double z) {
        this.z = z;
    }

    @Exclude(Strategy.NAME_REMAPPING)
    public void setSpeed(double speed) {
        MovementUtils.setSpeed(this, speed);
    }

    @Exclude(Strategy.NAME_REMAPPING)
    public double getFriction() {
        return friction;
    }

    @Exclude(Strategy.NAME_REMAPPING)
    public void setFriction(double friction) {
        this.friction = friction;
    }

    @Exclude(Strategy.NAME_REMAPPING)
    public float getStrafe() {
        return strafe;
    }

    @Exclude(Strategy.NAME_REMAPPING)
    public void setStrafe(float strafe) {
        this.strafe = strafe;
    }

    @Exclude(Strategy.NAME_REMAPPING)
    public float getForward() {
        return forward;
    }

    @Exclude(Strategy.NAME_REMAPPING)
    public void setForward(float forward) {
        this.forward = forward;
    }
}