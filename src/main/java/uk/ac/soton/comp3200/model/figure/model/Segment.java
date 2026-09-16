package uk.ac.soton.comp3200.model.figure.model;

import javafx.scene.paint.Color;
import uk.ac.soton.comp3200.model.math.Point;

public class Segment {
    public int startIndex;
    public int endIndex;
    public Color color = Color.BLACK;
    public double thickness = 3.0;
    public boolean isCircle = false;
    public double length = -1;
    public boolean isFilled = false;

    public Segment(int startIndex, int endIndex) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    public Segment(int startIndex, int endIndex, Color color, double thickness, boolean isCircle) {
        this(startIndex, endIndex);
        this.color = color;
        this.thickness = thickness;
        this.isCircle = isCircle;
    }

    public void calculateLength(Point startPos, Point endPos) {
        if (startPos != null && endPos != null) {
            double dx = startPos.x - endPos.x;
            double dy = startPos.y - endPos.y;
            this.length = Math.sqrt(dx * dx + dy * dy);
        } else {
            this.length = 0;
        }
    }
}