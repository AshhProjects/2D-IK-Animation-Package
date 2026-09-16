package uk.ac.soton.comp3200.model.figure.model;

import javafx.scene.paint.Color;
import uk.ac.soton.comp3200.model.math.Point;

public class PointData {
    public Point position;
    public String name;
    public Color color = Color.RED;
    public Shape shape = Shape.CIRCLE;
    public double size = 5.0;
    public boolean isDraggable = true;
    public boolean isVisible = true;
    public boolean isPoseControlPoint = false;

    public enum Shape { CIRCLE, SQUARE, NONE }

    public PointData(double x, double y, String name) {
        this.position = new Point(x, y);
        this.name = name;
    }

    public PointData(double x, double y, String name, boolean isDraggable, Color color, Shape shape, double size, boolean isPoseControl) {
        this(x,y, name);
        this.isDraggable = isDraggable;
        this.color = color;
        this.shape = shape;
        this.size = size;
        this.isPoseControlPoint = isPoseControl;
    }

    public boolean isInside(double eventX, double eventY) {
        if (!isVisible || !isDraggable) return false;
        return eventX >= position.x - size && eventX <= position.x + size &&
                eventY >= position.y - size && eventY <= position.y + size;
    }

    public PointData copy() {
        PointData newPd = new PointData(this.position.x, this.position.y, this.name);
        newPd.color = this.color;
        newPd.shape = this.shape;
        newPd.size = this.size;
        newPd.isDraggable = this.isDraggable;
        newPd.isVisible = this.isVisible;
        newPd.isPoseControlPoint = this.isPoseControlPoint;
        return newPd;
    }
}