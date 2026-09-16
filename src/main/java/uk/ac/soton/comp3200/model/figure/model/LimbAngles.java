package uk.ac.soton.comp3200.model.figure.model;

public class LimbAngles {
    public double angle1;
    public double angle2;

    public LimbAngles() {
    }

    public LimbAngles(double a1, double a2) { this.angle1 = a1; this.angle2 = a2; }

    public LimbAngles interpolate(LimbAngles other, double t) {
        double interpAngle1 = interpolateAngleShortestPath(this.angle1, other.angle1, t);
        double interpAngle2 = interpolateAngleShortestPath(this.angle2, other.angle2, t);
        return new LimbAngles(interpAngle1, interpAngle2);
    }

    public static double interpolateAngleShortestPath(double startAngleRad, double endAngleRad, double t) {
        double delta = endAngleRad - startAngleRad;
        delta = delta - Math.floor(delta / (2 * Math.PI)) * (2 * Math.PI);
        if (delta > Math.PI) {
            delta -= 2 * Math.PI;
        } else if (delta <= -Math.PI) {
            delta += 2 * Math.PI;
        }
        return startAngleRad + delta * t;
    }

    @Override public String toString() { return String.format("Angles[%.1f, %.1f]", Math.toDegrees(angle1), Math.toDegrees(angle2)); }
}
