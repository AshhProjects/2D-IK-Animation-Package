package uk.ac.soton.comp3200.model.figure.constraint;

import javafx.geometry.Point2D;
import uk.ac.soton.comp3200.model.math.Point;
import uk.ac.soton.comp3200.model.figure.model.StickFigureModel;

public class RelativeOffsetConstraint implements Constraint {
    private final int basePointIndex;
    private final int offsetPointIndex;
    private Point2D relativeOffset = null;

    public RelativeOffsetConstraint(int baseIndex, int offsetIndex) {
        this.basePointIndex = baseIndex;
        this.offsetPointIndex = offsetIndex;
    }

    @Override
    public void initialize(StickFigureModel model) {
        Point base = model.getPosition(basePointIndex);
        Point offset = model.getPosition(offsetPointIndex);
        if (base != null && offset != null) {
            this.relativeOffset = new Point2D(offset.x - base.x, offset.y - base.y);
        } else {
            System.err.println("Failed to initialize RelativeOffsetConstraint: points not found.");
            this.relativeOffset = Point2D.ZERO;
        }
    }

    @Override
    public boolean apply(StickFigureModel model, int recentlyMovedPointIndex) {
        if (relativeOffset == null) return false;

        Point base = model.getPosition(basePointIndex);
        Point offset = model.getPosition(offsetPointIndex);
        if (base == null || offset == null) return false;

        if (recentlyMovedPointIndex == basePointIndex || recentlyMovedPointIndex == -1) {
            double expectedX = base.x + relativeOffset.getX();
            double expectedY = base.y + relativeOffset.getY();

            if (Math.abs(offset.x - expectedX) > 1e-6 || Math.abs(offset.y - expectedY) > 1e-6) {
                offset.x = expectedX;
                offset.y = expectedY;
                return true;
            }
        }

        return false;
    }
}