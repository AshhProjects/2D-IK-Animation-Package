package uk.ac.soton.comp3200.model.figure.definition;

public class ConstraintDefinition {
    public enum ConstraintType {
        FIXED_DISTANCE,
        RELATIVE_OFFSET
    }

    public ConstraintType type;
    public String point1Name;
    public String point2Name;

    public ConstraintDefinition(ConstraintType type, String p1, String p2) {
        this.type = type;
        this.point1Name = p1;
        this.point2Name = p2;
    }
}