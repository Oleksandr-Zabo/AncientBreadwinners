package com.example.ancientbreadwinners;

import java.io.Serializable;

public class Tool implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;

    private ToolTypes type;
    private float speedX;

    public Tool(ToolTypes type, float speedX) {
        this.type = type;
        this.speedX = speedX;
    }

    public ToolTypes getType() {
        return type;
    }

    public void setType(ToolTypes type) {
        this.type = type;
    }

    public void setSpeedX(float speedX) {
        this.speedX = speedX;
    }

    public float getSpeedX() {
        return speedX;
    }

    public double speedCoeff() {
        return type == null ? 1.0 : type.speedCoeff();
    }

    @Override
    public String toString() {
        return "Tool{type=" + type + ", speedX=" + speedX + '}';
    }

    @Override
    public Tool clone() {
        try {
            return (Tool) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
