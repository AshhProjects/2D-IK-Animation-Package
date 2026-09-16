package uk.ac.soton.comp3200.model.figure.definition;

import javafx.scene.paint.Color;
import uk.ac.soton.comp3200.model.figure.model.PointData;

public class PointDefinition {
    public String name;
    public double initialX, initialY;
    public Color color = Color.RED;
    public PointData.Shape shape = PointData.Shape.CIRCLE;
    public double size = 5.0;
    public boolean isDraggable = true;
    public boolean isVisible = true;
    public boolean isPoseControlPoint = false;

    public PointDefinition(String name, double initialX, double initialY) {
        this.name = name;
        this.initialX = initialX;
        this.initialY = initialY;
    }

    public PointDefinition color(Color color) { this.color = color; return this; }
    public PointDefinition shape(PointData.Shape shape) { this.shape = shape; return this; }
    public PointDefinition size(double size) { this.size = size; return this; }
    public PointDefinition draggable(boolean draggable) { this.isDraggable = draggable; return this; }
    public PointDefinition visible(boolean visible) { this.isVisible = visible; return this; }
    public PointDefinition poseControl(boolean isControl) { this.isPoseControlPoint = isControl; return this; }
}