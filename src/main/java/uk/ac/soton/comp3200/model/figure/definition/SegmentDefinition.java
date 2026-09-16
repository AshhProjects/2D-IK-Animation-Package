package uk.ac.soton.comp3200.model.figure.definition;

import javafx.scene.paint.Color;

public class SegmentDefinition {
    public String startPointName;
    public String endPointName;
    public Color color = Color.BLACK;
    public double thickness = 10.0;
    public boolean isCircle = false;
    public boolean fixedLength = false;
    public boolean isFilled = false;

    public SegmentDefinition(String start, String end) {
        this.startPointName = start;
        this.endPointName = end;
    }

    public SegmentDefinition color(Color color) { this.color = color; return this; }
    public SegmentDefinition thickness(double thickness) { this.thickness = thickness; return this; }
    public SegmentDefinition circle(boolean isCircle) { this.isCircle = isCircle; return this; }
    public SegmentDefinition filled(boolean isFilled) { this.isFilled = isFilled; return this; }
}