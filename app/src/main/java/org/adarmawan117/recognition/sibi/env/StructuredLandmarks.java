package org.adarmawan117.recognition.sibi.env;

public class StructuredLandmarks {
    private final double x;
    private final double y;

    public StructuredLandmarks(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
