package uk.ac.soton.comp3200.model.figure.constraint;

import uk.ac.soton.comp3200.model.math.Point;
import uk.ac.soton.comp3200.model.figure.model.StickFigureModel;
import uk.ac.soton.comp3200.model.math.Utility;

public class FixedDistanceConstraint implements Constraint {
    private final int point1Index;
    private final int point2Index;
    private double targetDistance = -1;

    public FixedDistanceConstraint(int p1Index, int p2Index) {
        this.point1Index = p1Index;
        this.point2Index = p2Index;
    }

    @Override
    public void initialize(StickFigureModel model) {
        Point p1 = model.getPosition(point1Index);
        Point p2 = model.getPosition(point2Index);
        if (p1 != null && p2 != null) {
            this.targetDistance = Utility.distance(p1, p2);
        } else {
            System.err.println("Failed to initialize FixedDistanceConstraint: points not found.");
            this.targetDistance = 0;
        }
    }

    @Override
    public boolean apply(StickFigureModel model, int recentlyMovedPointIndex) {
        if (targetDistance < 0) return false;

        Point p1 = model.getPosition(point1Index);
        Point p2 = model.getPosition(point2Index);
        if (p1 == null || p2 == null) return false;

        double currentDistance = Utility.distance(p1, p2);
        if (Math.abs(currentDistance - targetDistance) < 1e-6) {
            return false;
        }

        Point pointToMove;
        Point anchorPoint;

        if (recentlyMovedPointIndex == point1Index) {
            pointToMove = p2;
            anchorPoint = p1;
        } else if (recentlyMovedPointIndex == point2Index) {
            pointToMove = p1;
            anchorPoint = p2;
        } else {
            pointToMove = p2;
            anchorPoint = p1;
        }

        Utility.adjustPointDistance(pointToMove, anchorPoint, targetDistance);
        return true;
    }
}