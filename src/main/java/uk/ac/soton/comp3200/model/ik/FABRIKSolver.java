package uk.ac.soton.comp3200.model.ik;

import uk.ac.soton.comp3200.model.math.Point;
import uk.ac.soton.comp3200.model.math.Utility;

import java.util.List;

public class FABRIKSolver {

    /**
     * Solves IK for a kinematic chain using the FABRIK algorithm.
     * IMPORTANT: The base point (chainPoints[0]) is considered FIXED during the solve.
     *
     * @param chainPoints    A list of Point objects representing the current joint positions (mutable copies).
     *                       The first point (index 0) is the base and will be reset to its initial position.
     * @param segmentLengths A list of doubles representing the length of each segment. segmentLengths[i] is the length between chainPoints[i] and chainPoints[i+1].
     * @param target         The target Point the end effector (last point in chainPoints) should reach.
     * @param tolerance      The acceptable distance between the end effector and the target to stop iterating.
     * @param maxIterations  The maximum number of iterations to perform.
     * @return true if the target was considered reached within tolerance, false otherwise.
     */
    public static boolean solve(List<Point> chainPoints, List<Double> segmentLengths, Point target, double tolerance, int maxIterations) {

        if (chainPoints == null || chainPoints.isEmpty() || segmentLengths == null || target == null) {
            System.err.println("FABRIK Error: Invalid input.");
            return false;
        }
        if (chainPoints.size() != segmentLengths.size() + 1) {
            System.err.println("FABRIK Error: Mismatch between points (" + chainPoints.size() + ") and segments (" + segmentLengths.size() + ").");
            return false;
        }

        int numPoints = chainPoints.size();
        int lastIndex = numPoints - 1;
        double totalLength = segmentLengths.stream().mapToDouble(Double::doubleValue).sum();

        Point basePosition = new Point(chainPoints.get(0).x, chainPoints.get(0).y);
        double distBaseToTarget = Utility.distance(basePosition, target);

        // --- Check Reachability ---
        if (distBaseToTarget > totalLength) {
            // Target is out of reach - Stretch the chain towards the target
            Point currentPoint = basePosition;
            chainPoints.get(0).x = basePosition.x;
            chainPoints.get(0).y = basePosition.y;

            for (int i = 0; i < lastIndex; i++) {
                double dx = target.x - currentPoint.x;
                double dy = target.y - currentPoint.y;
                double currentDist = Math.sqrt(dx * dx + dy * dy);
                if (currentDist < 1e-6) currentDist = 1e-6;

                double lambda = segmentLengths.get(i) / currentDist;
                Point nextPoint = chainPoints.get(i + 1);
                nextPoint.x = currentPoint.x + lambda * dx;
                nextPoint.y = currentPoint.y + lambda * dy;
                currentPoint = nextPoint;
            }

            return false;
        }

        // --- Target is Reachable - Iterate ---
        int iterations = 0;
        double distEndToTarget = Utility.distance(chainPoints.get(lastIndex), target);

        while (distEndToTarget > tolerance && iterations < maxIterations) {
            iterations++;

            // --- Backwards Pass ---
            chainPoints.get(lastIndex).x = target.x;
            chainPoints.get(lastIndex).y = target.y;

            for (int i = lastIndex - 1; i >= 0; i--) {
                repositionPoint(chainPoints.get(i + 1), chainPoints.get(i), segmentLengths.get(i));
            }

            // --- Forwards Pass ---
            chainPoints.get(0).x = basePosition.x;
            chainPoints.get(0).y = basePosition.y;

            for (int i = 0; i < lastIndex; i++) {
                repositionPoint(chainPoints.get(i), chainPoints.get(i + 1), segmentLengths.get(i));
            }

            distEndToTarget = Utility.distance(chainPoints.get(lastIndex), target);
        }

        return distEndToTarget <= tolerance;
    }

    /**
     * Repositions the `moving` point so that it lies `desiredLength` away from `fixed` along the line connecting them.
     */
    private static void repositionPoint(Point fixed, Point moving, double desiredLength) {
        double dist = Utility.distance(fixed, moving);
        if (dist < 1e-9) dist = 1e-9;

        double lambda = desiredLength / dist;
        moving.x = fixed.x + (moving.x - fixed.x) * lambda;
        moving.y = fixed.y + (moving.y - fixed.y) * lambda;
    }
}
