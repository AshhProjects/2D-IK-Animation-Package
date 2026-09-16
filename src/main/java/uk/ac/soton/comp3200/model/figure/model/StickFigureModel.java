package uk.ac.soton.comp3200.model.figure.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp3200.model.ik.FABRIKSolver;
import uk.ac.soton.comp3200.model.math.Point;
import uk.ac.soton.comp3200.model.figure.constraint.Constraint;
import uk.ac.soton.comp3200.model.math.Utility;

import java.util.*;

public class StickFigureModel {
    public String id;
    public List<PointData> points = new ArrayList<>();
    public List<Segment> segments = new ArrayList<>();
    public List<IKChain> ikChains = new ArrayList<>();
    public List<Constraint> constraints = new ArrayList<>();
    public Map<String, Integer> nameToIndexMap = new HashMap<>();

    /** Set of indices corresponding to points acting as middle joints in IK chains (e.g., elbows, knees). */
    public Set<Integer> ikMiddleJointIndices = new HashSet<>();

    public StickFigureModel(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("StickFigureModel must have a non-empty ID.");
        }
        this.id = id;
    }

    public StickFigureModel() {
        this.id = "figure_" + System.nanoTime();
    }

    public void setNameToIndexMap(Map<String, Integer> map) {
        this.nameToIndexMap = map;
    }

    public Integer getPointIndexByName(String name) {
        return nameToIndexMap.get(name);
    }

    public PointData getPointDataByName(String name) {
        Integer index = getPointIndexByName(name);
        return (index != null) ? getPointData(index) : null;
    }

    public PointData getPointData(int index) {
        if (index >= 0 && index < points.size()) {
            return points.get(index);
        }
        return null;
    }

    public Point getPosition(int index) {
        PointData pd = getPointData(index);
        return (pd != null) ? pd.position : null;
    }

    public Point getPositionByName(String name) {
        PointData pd = getPointDataByName(name);
        return (pd != null) ? pd.position : null;
    }

    /** Call after points/chains/constraints are added to calculate initial lengths/parameters */
    public void initializeModel() {
        calculateIKChainLengths();
        initializeConstraints();
    }

    public void calculateIKChainLengths() {
        for (IKChain chain : ikChains) {
            Point pStart = getPosition(chain.startJointIndex);
            Point pMid = getPosition(chain.middleJointIndex);
            Point pEnd = getPosition(chain.endPointIndex);

            if (pStart != null && pMid != null && pEnd != null) {
                chain.length1 = Utility.distance(pStart, pMid);
                chain.length2 = Utility.distance(pMid, pEnd);
            } else {
                System.err.println("Error calculating lengths for IK chain '" + chain.name + "' in figure '" + id + "': Points not found.");
                chain.length1 = 10;
                chain.length2 = 10;
            }
        }
    }

    public void initializeConstraints() {
        for (Constraint constraint : constraints) {
            constraint.initialize(this);
        }
    }

    /** Applies constraints iteratively until stable or max iterations */
    public void applyConstraintsIteratively(int maxIterations, int recentlyMovedPointIndex) {
        boolean passChanged;
        int initialMovedIndex = recentlyMovedPointIndex;

        for (int i = 0; i < maxIterations; i++) {
            passChanged = false;
            int currentMovedIndex = initialMovedIndex;
            for (Constraint constraint : constraints) {
                if (constraint.apply(this, currentMovedIndex)) {
                    passChanged = true;
                    currentMovedIndex = -1;
                }
            }
            initialMovedIndex = -1;

            if (!passChanged) break;
            if (i == maxIterations - 1) {
                logger.warn("Constraints for figure '{}' did not fully converge after {} iterations.", this.id, maxIterations);
            }
        }
    }

    /**
     * Checks if the given point index is used as a middle joint in any IK chain.
     * @param index The point index to check.
     * @return true if it's a middle joint, false otherwise.
     */
    public boolean isMiddleJoint(int index) {
        return ikMiddleJointIndices.contains(index);
    }

    /**
     * Find the IK chain that uses the given point index as its middle joint.
     * @param middleJointIndex The index of the middle joint.
     * @return The IKChain, or null if no chain uses this index as a middle joint.
     */
    public IKChain findChainByMiddleJoint(int middleJointIndex) {
        for (IKChain chain : ikChains) {
            if (chain.middleJointIndex == middleJointIndex) {
                return chain;
            }
        }
        return null;
    }

    /**
     * Creates a deep copy of the current point positions for storing in a keyframe.
     * @return A new list containing copies of all points.
     */
    public List<Point> getPointCopies() {
        List<Point> copies = new ArrayList<>(points.size());
        for (PointData pd : points) {
            if (pd != null && pd.position != null) {
                copies.add(new Point(pd.position.x, pd.position.y));
            } else {
                copies.add(new Point(0, 0));
                System.err.println("Warning: Null PointData or Position found in figure '" + id + "' during getPointCopies.");
            }
        }
        return copies;
    }

    /**
     * Updates the positions of this model's points from a list.
     * Assumes the list is in the correct order and size.
     * @param newPoints The list of new point positions.
     * @return true if update was successful (size matched), false otherwise.
     */
    public boolean setPointsFromList(List<Point> newPoints) {
        if (newPoints == null || newPoints.size() != points.size()) {
            System.err.println("Error setting points for figure '" + id + "': Point list size mismatch or null. Expected " + points.size() + ", got " + (newPoints == null ? "null" : newPoints.size()));
            return false;
        }
        for (int i = 0; i < points.size(); i++) {
            PointData pd = points.get(i);
            Point np = newPoints.get(i);
            if (pd != null && pd.position != null && np != null) {
                pd.position.x = np.x;
                pd.position.y = np.y;
            } else {
                System.err.println("Warning: Null PointData, Position, or new Point encountered during setPointsFromList for figure '" + id + "' at index " + i);
            }
        }
        return true;
    }

    private static final Logger logger = LogManager.getLogger(StickFigureModel.class);
    private static final int MAX_CONSTRAINT_ITERATIONS = 10;
    private static final double FABRIK_TOLERANCE = 0.1;
    private static final int FABRIK_MAX_ITERATIONS = 15;

    /** Applies constraints iteratively */
    public void applyConstraintsIteratively(int recentlyMovedPointIndex) {
        applyConstraintsIteratively(MAX_CONSTRAINT_ITERATIONS, recentlyMovedPointIndex);
    }

    /** Solves all IK chains */
    public void solveAllIK() {
        solveAllIK(null);
    }

    /** Solves all IK chains, optionally skipping one */
    public void solveAllIK(IKChain chainToSkip) {
        logger.trace("Solving all IK chains for figure '{}' (skipping: {})", this.id, chainToSkip != null ? chainToSkip.name : "None");
        for (IKChain chain : this.ikChains) {
            if (chain == chainToSkip) continue;
            Point hint = null;
            Point currentMidPos = this.getPosition(chain.middleJointIndex);
            if (currentMidPos != null) hint = new Point(currentMidPos.x, currentMidPos.y);
            solveSingleIK(chain, hint);
        }
    }

    /** Solves a single IK chain */
    private void solveSingleIK(IKChain chain, Point preferredMidPosHint) {
        if (chain == null) return;
        Point startPos = this.getPosition(chain.startJointIndex);
        Point midPos = this.getPosition(chain.middleJointIndex);
        Point endPos = this.getPosition(chain.endPointIndex);
        Point targetPos = this.getPosition(chain.effectorIndex);
        if (startPos == null || midPos == null || endPos == null || targetPos == null) return;

        if (chain.length1 <= 0 || chain.length2 <= 0) {
            this.calculateIKChainLengths();
            if (chain.length1 <= 0 || chain.length2 <= 0) return;
        }

        List<Point> fabrikPoints = new ArrayList<>(Arrays.asList(
                new Point(startPos.x, startPos.y),
                new Point(midPos.x, midPos.y),
                new Point(endPos.x, endPos.y)
        ));

        List<Double> fabrikLengths = Arrays.asList(chain.length1, chain.length2);
        Point fabrikTarget = new Point(targetPos.x, targetPos.y);
        boolean reached = FABRIKSolver.solve(fabrikPoints, fabrikLengths, fabrikTarget, FABRIK_TOLERANCE, FABRIK_MAX_ITERATIONS);
        Point solvedMidPos = fabrikPoints.get(1);
        Point solvedEndPos = fabrikPoints.get(2);

        midPos.x = solvedMidPos.x; midPos.y = solvedMidPos.y;
        endPos.x = solvedEndPos.x; endPos.y = solvedEndPos.y;

        if (!reached && Utility.distance(startPos, fabrikTarget) > chain.length1 + chain.length2 + 1e-4) {
            targetPos.x = solvedEndPos.x;
            targetPos.y = solvedEndPos.y;
            if (chain.endPointIndex != chain.effectorIndex) {
                Point actualEndPos = this.getPosition(chain.endPointIndex);
                if (actualEndPos != null) { actualEndPos.x = solvedEndPos.x; actualEndPos.y = solvedEndPos.y; }
            }
        }

        double currentDistStartEnd = Utility.distance(startPos, endPos);
        if (currentDistStartEnd <= chain.length1 + chain.length2 + 1e-4 &&
                currentDistStartEnd >= Math.abs(chain.length1 - chain.length2) - 1e-4) {
            double len1 = chain.length1; double len2 = chain.length2;
            double a = (len1 * len1 - len2 * len2 + currentDistStartEnd * currentDistStartEnd) / (2 * currentDistStartEnd);
            double hSq = len1 * len1 - a * a;
            double h_threshold = 1e-4;
            double finalMidX, finalMidY;

            if (hSq < h_threshold * h_threshold || preferredMidPosHint == null) {
                finalMidX = midPos.x;
                finalMidY = midPos.y;
            } else {
                double h = Math.sqrt(Math.max(0, hSq));
                double p2x = startPos.x + a * (endPos.x - startPos.x) / currentDistStartEnd;
                double p2y = startPos.y + a * (endPos.y - startPos.y) / currentDistStartEnd;
                double dx = endPos.x - startPos.x; double dy = endPos.y - startPos.y;
                double midX1, midY1, midX2, midY2;

                if (currentDistStartEnd > 1e-6) {
                    double perpDx = -dy * (h / currentDistStartEnd); double perpDy = dx * (h / currentDistStartEnd);
                    midX1 = p2x + perpDx; midY1 = p2y + perpDy;
                    midX2 = p2x - perpDx; midY2 = p2y - perpDy;
                } else { midX1 = startPos.x + len1; midY1 = startPos.y; midX2 = midX1; midY2 = midY1; }

                double distSq1 = Utility.distance(preferredMidPosHint, new Point(midX1, midY1)); distSq1 *= distSq1;
                double distSq2 = Utility.distance(preferredMidPosHint, new Point(midX2, midY2)); distSq2 *= distSq2;

                if (distSq1 <= distSq2) { finalMidX = midX1; finalMidY = midY1; }
                else { finalMidX = midX2; finalMidY = midY2; }
            }
            midPos.x = finalMidX; midPos.y = finalMidY;
            Utility.adjustPointDistance(endPos, midPos, len2);
        }
    }

    /** Calculates angles for a specific IK chain */
    public LimbAngles calculateAnglesForChain(IKChain chain) {
        Point pS = this.getPosition(chain.startJointIndex);
        Point pM = this.getPosition(chain.middleJointIndex);
        Point pE = this.getPosition(chain.endPointIndex);
        if (pS == null || pM == null || pE == null) return null;
        double angle1 = Math.atan2(pM.y - pS.y, pM.x - pS.x);
        double angleME = Math.atan2(pE.y - pM.y, pE.x - pM.x);
        double angle2 = angleME - angle1;
        while (angle2 <= -Math.PI) angle2 += 2 * Math.PI;
        while (angle2 > Math.PI) angle2 -= 2 * Math.PI;
        return new LimbAngles(angle1, angle2);
    }

    /** Reconstructs limb position from angles */
    public void reconstructLimbFromAngles(IKChain chain, LimbAngles angles) {
        Point pS = this.getPosition(chain.startJointIndex);
        Point pM = this.getPosition(chain.middleJointIndex);
        Point pE = this.getPosition(chain.endPointIndex);
        Point pEffector = this.getPosition(chain.effectorIndex);
        if (pS == null || pM == null || pE == null || angles == null || chain.length1 <= 0 || chain.length2 <= 0) return;
        pM.x = pS.x + chain.length1 * Math.cos(angles.angle1);
        pM.y = pS.y + chain.length1 * Math.sin(angles.angle1);
        double angleEndAbsolute = angles.angle1 + angles.angle2;
        pE.x = pM.x + chain.length2 * Math.cos(angleEndAbsolute);
        pE.y = pM.y + chain.length2 * Math.sin(angleEndAbsolute);
        if (chain.endPointIndex != chain.effectorIndex && pEffector != null) {
            pEffector.x = pE.x; pEffector.y = pE.y;
        }
    }

    /** Checks if any visible, draggable point is inside the given coordinates */
    public boolean anyPointInside(double x, double y) {
        for (PointData pd : points) {
            if (pd != null && pd.isVisible && pd.isDraggable && pd.shape != PointData.Shape.NONE && pd.isInside(x, y)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StickFigureModel that = (StickFigureModel) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}