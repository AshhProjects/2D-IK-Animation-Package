package uk.ac.soton.comp3200.model.math;

import uk.ac.soton.comp3200.model.animation.EasingType;

public class Utility {

    /** calculates the distance between two points. */
    public static double distance(Point p1, Point p2) {
        if (p1 == null || p2 == null) return 0;
        double dx = p1.x - p2.x;
        double dy = p1.y - p2.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     Adjusts pointToMove so its distance from anchorPoint equals the specified distance.
     This is for any points that are NOT end effectors.
     **/
    public static void adjustPointDistance(Point pointToMove, Point anchorPoint, double targetDistance) {
        if (pointToMove == null || anchorPoint == null || targetDistance < 0) return;

        double dx = pointToMove.x - anchorPoint.x;
        double dy = pointToMove.y - anchorPoint.y;
        double currentDist = distance(pointToMove, anchorPoint);

        if (currentDist < 0) {
            pointToMove.x = anchorPoint.x;
            pointToMove.y = anchorPoint.y + targetDistance;
            return;
        }

        double scale = targetDistance / currentDist;
        pointToMove.x = anchorPoint.x + dx * scale;
        pointToMove.y = anchorPoint.y + dy * scale;
    }

    /**
     * applies the specified easing function to the linear time factor t.
     * @param t Linear time factor (0.0 to 1.0)
     * @param type The type of easing to apply.
     * @return The eased time factor (usually 0.0 to 1.0).
     */
    public static float applyEasing(float t, EasingType type) {
        switch (type) {
            case EASE_IN_QUAD:
                return t * t;
            case EASE_OUT_QUAD:
                return t * (2f - t); // starts fast, slows down: -t^2 + 2t
            case EASE_IN_OUT_QUAD:
                if (t < 0.5f) {
                    return 2f * t * t; // Ease In part
                } else {
                    return -1f + (4f - 2f * t) * t; // Ease Out part
                }
            case LINEAR:
            default:
                return t;
        }
    }
}