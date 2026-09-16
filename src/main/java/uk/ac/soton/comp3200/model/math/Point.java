package uk.ac.soton.comp3200.model.math;

/**
 * Represents a point in 2D space
 */
public class Point {
    public double x;
    public double y;

    /**
     * Creates a new point
     *
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Creates a new default point
     */
    public Point() {
        this.x = 0.0;
        this.y = 0.0;
    }

    /**
     * Linear interpolation between two points
     *
     * @param other the target point
     * @param t interpolation factor (0.0 to 1.0)
     * @return a new point at the interpolated position
     */
    public Point interpolate(Point other, double t) {
        double newX = this.x + t * (other.x - this.x);
        double newY = this.y + t * (other.y - this.y);
        return new Point(newX, newY);
    }
}