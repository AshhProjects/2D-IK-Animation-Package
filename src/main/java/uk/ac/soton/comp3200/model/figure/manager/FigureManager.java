package uk.ac.soton.comp3200.model.figure.manager;

import javafx.scene.paint.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp3200.model.figure.definition.*;
import uk.ac.soton.comp3200.model.figure.factory.StickFigureFactory;
import uk.ac.soton.comp3200.model.figure.model.IKChain;
import uk.ac.soton.comp3200.model.figure.model.LimbAngles;
import uk.ac.soton.comp3200.model.figure.model.PointData;
import uk.ac.soton.comp3200.model.figure.model.StickFigureModel;
import uk.ac.soton.comp3200.model.math.Point;
import uk.ac.soton.comp3200.model.math.Utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Manages the collection of StickFigureModels, selection state,
 * and provides methods for accessing and manipulating them.
 * This is part of the Model layer.
 */
public class FigureManager {
    private static final Logger logger = LogManager.getLogger(FigureManager.class);

    private final List<StickFigureModel> figures = new ArrayList<>();
    private final AtomicInteger figureCounter = new AtomicInteger(0);
    private final FigureDefinition defaultFigureDefinition;

    private String selectedFigureId = null;
    private int selectedPointIndex = -1;

    private static final int MAX_CONSTRAINT_ITERATIONS = 10;

    public FigureManager() {
        this.defaultFigureDefinition = createFigureDefinition();
    }

    private FigureDefinition createFigureDefinition() {
        FigureDefinition def = new FigureDefinition();
        def.name = "PivotStyleCentered";

        double limbThickness = 14.0;
        double bodyThickness = 14.0;

        double hipY = 0;
        double middleY = hipY - 40;
        double topY = hipY - 80;
        double headY = topY - 60;

        def.addPoint(new PointDefinition("HEAD_TOP", 0, headY)
                        .color(Color.RED).shape(PointData.Shape.CIRCLE).size(4).draggable(true).visible(true).poseControl(true))
                .addPoint(new PointDefinition("BODY_TOP", 0, topY)
                        .color(Color.RED).shape(PointData.Shape.CIRCLE).size(5).draggable(true).poseControl(true))
                .addPoint(new PointDefinition("BODY_MIDDLE", 0, middleY)
                        .color(Color.RED).shape(PointData.Shape.CIRCLE).size(5).draggable(true).poseControl(true))
                .addPoint(new PointDefinition("BODY_BOTTOM", 0, hipY)
                        .color(Color.ORANGE)
                        .shape(PointData.Shape.CIRCLE).size(5).draggable(true).poseControl(true))

                .addPoint(new PointDefinition("LEFT_ELBOW", -36, topY + 16)
                        .color(Color.RED).shape(PointData.Shape.CIRCLE).size(4).draggable(true).poseControl(false))
                .addPoint(new PointDefinition("LEFT_HAND", -72, topY + 32)
                        .color(Color.RED).shape(PointData.Shape.CIRCLE).size(4).draggable(false).poseControl(false))
                .addPoint(new PointDefinition("LEFT_HAND_EFFECTOR", -72, topY + 32)
                        .color(Color.BLUE).shape(PointData.Shape.SQUARE).size(5).draggable(true).visible(true).poseControl(true))
                .addPoint(new PointDefinition("RIGHT_ELBOW", 36, topY + 16)
                        .color(Color.RED).shape(PointData.Shape.CIRCLE).size(4).draggable(true).poseControl(false))
                .addPoint(new PointDefinition("RIGHT_HAND", 72, topY + 32)
                        .color(Color.RED).shape(PointData.Shape.CIRCLE).size(4).draggable(false).poseControl(false))
                .addPoint(new PointDefinition("RIGHT_HAND_EFFECTOR", 72, topY + 32)
                        .color(Color.BLUE).shape(PointData.Shape.SQUARE).size(5).draggable(true).visible(true).poseControl(true))

                .addPoint(new PointDefinition("LEFT_KNEE", -25, hipY + 45)
                        .color(Color.RED).shape(PointData.Shape.CIRCLE).size(4).draggable(true).poseControl(false))
                .addPoint(new PointDefinition("LEFT_FOOT", -40, hipY + 90)
                        .color(Color.RED).shape(PointData.Shape.CIRCLE).size(4).draggable(false).poseControl(false))
                .addPoint(new PointDefinition("LEFT_FOOT_EFFECTOR", -40, hipY + 90)
                        .color(Color.CORNFLOWERBLUE).shape(PointData.Shape.SQUARE).size(5).draggable(true).visible(true).poseControl(true))
                .addPoint(new PointDefinition("RIGHT_KNEE", 25, hipY + 45)
                        .color(Color.RED).shape(PointData.Shape.CIRCLE).size(4).draggable(true).poseControl(false))
                .addPoint(new PointDefinition("RIGHT_FOOT", 40, hipY + 90)
                        .color(Color.RED).shape(PointData.Shape.CIRCLE).size(4).draggable(false).poseControl(false))
                .addPoint(new PointDefinition("RIGHT_FOOT_EFFECTOR", 40, hipY + 90)
                        .color(Color.CORNFLOWERBLUE).shape(PointData.Shape.SQUARE).size(5).draggable(true).visible(true).poseControl(true));


        def.addSegment(new SegmentDefinition("HEAD_TOP", "BODY_TOP").color(Color.BLACK).thickness(1.0).circle(true).filled(true));
        def.addSegment(new SegmentDefinition("BODY_TOP", "BODY_MIDDLE").thickness(bodyThickness).color(Color.BLACK));
        def.addSegment(new SegmentDefinition("BODY_MIDDLE", "BODY_BOTTOM").thickness(bodyThickness).color(Color.BLACK));
        def.addSegment(new SegmentDefinition("BODY_TOP", "LEFT_ELBOW").thickness(limbThickness).color(Color.BLACK));
        def.addSegment(new SegmentDefinition("LEFT_ELBOW", "LEFT_HAND").thickness(limbThickness).color(Color.BLACK));
        def.addSegment(new SegmentDefinition("BODY_TOP", "RIGHT_ELBOW").thickness(limbThickness).color(Color.BLACK));
        def.addSegment(new SegmentDefinition("RIGHT_ELBOW", "RIGHT_HAND").thickness(limbThickness).color(Color.BLACK));
        def.addSegment(new SegmentDefinition("BODY_BOTTOM", "LEFT_KNEE").thickness(limbThickness).color(Color.BLACK));
        def.addSegment(new SegmentDefinition("LEFT_KNEE", "LEFT_FOOT").thickness(limbThickness).color(Color.BLACK));
        def.addSegment(new SegmentDefinition("BODY_BOTTOM", "RIGHT_KNEE").thickness(limbThickness).color(Color.BLACK));
        def.addSegment(new SegmentDefinition("RIGHT_KNEE", "RIGHT_FOOT").thickness(limbThickness).color(Color.BLACK));

        def.addIKChain(new IKChainDefinition("Left Arm", "BODY_TOP", "LEFT_ELBOW", "LEFT_HAND", "LEFT_HAND_EFFECTOR", true));
        def.addIKChain(new IKChainDefinition("Right Arm", "BODY_TOP", "RIGHT_ELBOW", "RIGHT_HAND", "RIGHT_HAND_EFFECTOR", true));
        def.addIKChain(new IKChainDefinition("Left Leg", "BODY_BOTTOM", "LEFT_KNEE", "LEFT_FOOT", "LEFT_FOOT_EFFECTOR", false));
        def.addIKChain(new IKChainDefinition("Right Leg", "BODY_BOTTOM", "RIGHT_KNEE", "RIGHT_FOOT", "RIGHT_FOOT_EFFECTOR", false));

        def.addConstraint(new ConstraintDefinition(ConstraintDefinition.ConstraintType.FIXED_DISTANCE, "BODY_TOP", "HEAD_TOP"));
        def.addConstraint(new ConstraintDefinition(ConstraintDefinition.ConstraintType.FIXED_DISTANCE, "BODY_TOP", "BODY_MIDDLE"));
        def.addConstraint(new ConstraintDefinition(ConstraintDefinition.ConstraintType.FIXED_DISTANCE, "BODY_MIDDLE", "BODY_BOTTOM"));

        return def;
    }

    public StickFigureModel createAndAddFigure(String figureId) {
        StickFigureModel newFigure;
        if (figureId == null) {
            String newId = "figure_" + figureCounter.getAndIncrement();
            logger.info("FigureManager: Creating new figure: {}", newId);

            newFigure = StickFigureFactory.build(defaultFigureDefinition);
            newFigure.id = newId;

            for (PointData pd : newFigure.points) {
                if (pd != null && pd.position != null) {
                    pd.position.x += 400;
                    pd.position.y += 300;
                }
            }
            newFigure.initializeModel();
            newFigure.solveAllIK();
        } else {
            if (figures.stream().anyMatch(f -> f.id.equals(figureId))) {
                logger.warn("Figure with ID '{}' already exists.", figureId);
                return getFigureById(figureId);
            }
            logger.info("FigureManager: Creating figure with specific ID: {}", figureId);
            if (defaultFigureDefinition == null) {
                logger.error("Cannot create figure '{}': Default Figure Definition is null.", figureId);
                return null;
            }
            newFigure = StickFigureFactory.build(defaultFigureDefinition);
            newFigure.id = figureId;

            try {
                String numberPart = figureId.substring("figure_".length());
                int idNum = Integer.parseInt(numberPart);
                figureCounter.updateAndGet(current -> Math.max(current, idNum + 1));
                logger.trace("Updated figure counter based on loaded ID '{}' to {}", figureId, figureCounter.get());
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                logger.warn("Could not parse number from loaded figure ID '{}' for counter update.", figureId);
            }

        }

        figures.add(newFigure);
        logger.debug("FigureManager: Added figure {}. Total figures: {}", figureId, figures.size());
        return newFigure;
    }

    public void removeFigure(String figureId) {
        logger.info("FigureManager: Removing figure: {}", figureId);
        boolean removed = figures.removeIf(f -> f.id.equals(figureId));
        if (removed) {
            if (figureId.equals(selectedFigureId)) {
                deselectFigure();
            }
            logger.debug("FigureManager: Removed figure {}. Total figures: {}", figureId, figures.size());
        }
    }

    public void removeAllFigures() {
        logger.info("FigureManager: Removing all figures.");
        figures.clear();
        figureCounter.set(0);
        deselectFigure();
    }

    public void selectFigure(String figureId) {
        if (figureId == null) {
            deselectFigure();
            return;
        }
        if (!figureId.equals(selectedFigureId)) {
            StickFigureModel figure = getFigureById(figureId);
            if (figure != null) {
                logger.debug("FigureManager: Selecting figure {}", figureId);
                selectedFigureId = figureId;
                selectedPointIndex = -1;
            } else {
                logger.warn("FigureManager: Cannot select non-existent figure {}", figureId);
                deselectFigure();
            }
        }
    }

    public void selectPoint(String figureId, int pointIndex) {
        if (figureId == null || !figureId.equals(selectedFigureId)) {
            selectFigure(figureId);
        }
        if (figureId != null && figureId.equals(selectedFigureId)) {
            StickFigureModel fig = getSelectedFigure();
            if (fig != null && pointIndex >= 0 && pointIndex < fig.points.size()) {
                if (selectedPointIndex != pointIndex) {
                    logger.debug("FigureManager: Selecting point {} on figure {}", pointIndex, figureId);
                    selectedPointIndex = pointIndex;
                }
            } else {
                logger.warn("FigureManager: Invalid point index {} for figure {}", pointIndex, figureId);
                selectedPointIndex = -1;
            }
        } else {
            if (selectedPointIndex != -1) {
                selectedPointIndex = -1;
            }
        }
    }

    public void deselectFigure() {
        if (selectedFigureId != null) {
            logger.debug("FigureManager: Deselecting figure.");
            selectedFigureId = null;
            selectedPointIndex = -1;
        }
    }

    public List<StickFigureModel> getAllFigures() {
        return Collections.unmodifiableList(figures);
    }

    public List<String> getAllFigureIds() {
        return figures.stream().map(f -> f.id).collect(Collectors.toList());
    }


    public StickFigureModel getFigureById(String figureId) {
        return figures.stream().filter(f -> f.id.equals(figureId)).findFirst().orElse(null);
    }

    public StickFigureModel getSelectedFigure() {
        return getFigureById(selectedFigureId);
    }

    public String getSelectedFigureId() {
        return selectedFigureId;
    }

    public int getSelectedPointIndex() {
        return selectedPointIndex;
    }

    public PointData getSelectedPointData() {
        StickFigureModel fig = getSelectedFigure();
        if (fig != null && selectedPointIndex >= 0 && selectedPointIndex < fig.points.size()) {
            return fig.points.get(selectedPointIndex);
        }
        return null;
    }

    public AtomicInteger getFigureCounter() {
        return figureCounter;
    }

    public void translateFigure(String figureId, double deltaX, double deltaY) {
        StickFigureModel figure = getFigureById(figureId);
        if (figure != null) {
            logger.trace("FigureManager: Translating figure '{}' by ({}, {})", figureId, deltaX, deltaY);
            for (PointData pd : figure.points) {
                if (pd != null && pd.position != null) {
                    pd.position.x += deltaX;
                    pd.position.y += deltaY;
                }
            }
        }
    }

    public void moveSelectedPoint(double targetX, double targetY, boolean isMiddleJointDrag, boolean isBodyBottomDrag, double deltaX, double deltaY) {
        StickFigureModel figure = getSelectedFigure();
        PointData point = getSelectedPointData();
        int pointIndex = getSelectedPointIndex();

        if (figure == null || point == null || pointIndex < 0) return;

        logger.trace("FigureManager: Moving point {} on figure {}", point.name, figure.id);

        point.position.x = targetX;
        point.position.y = targetY;

        IKChain affectedChain = null;
        if (isMiddleJointDrag) {
            affectedChain = figure.findChainByMiddleJoint(pointIndex);
            if (affectedChain != null) {
                Point startPos = figure.getPosition(affectedChain.startJointIndex);
                Point midPos = point.position;
                Point endPos = figure.getPosition(affectedChain.endPointIndex);
                Point effectorPos = figure.getPosition(affectedChain.effectorIndex);

                if (startPos != null && midPos != null && endPos != null && affectedChain.length1 > 0 && affectedChain.length2 > 0) {
                    Point targetMidPos = new Point(targetX, targetY);
                    Utility.adjustPointDistance(targetMidPos, startPos, affectedChain.length1);
                    midPos.x = targetMidPos.x;
                    midPos.y = targetMidPos.y;

                    Utility.adjustPointDistance(endPos, midPos, affectedChain.length2);
                    if (affectedChain.endPointIndex != affectedChain.effectorIndex && effectorPos != null) {
                        effectorPos.x = endPos.x;
                        effectorPos.y = endPos.y;
                    }
                }
            }
        } else if (isBodyBottomDrag) {
            Point pMid = figure.getPositionByName("BODY_MIDDLE");
            Point pTop = figure.getPositionByName("BODY_TOP");
            Point pHead = figure.getPositionByName("HEAD_TOP");
            if (pMid != null) {
                pMid.x += deltaX;
                pMid.y += deltaY;
            }
            if (pTop != null) {
                pTop.x += deltaX;
                pTop.y += deltaY;
            }
            if (pHead != null) {
                pHead.x += deltaX;
                pHead.y += deltaY;
            }
        }

        figure.applyConstraintsIteratively(MAX_CONSTRAINT_ITERATIONS, pointIndex);

        figure.solveAllIK(affectedChain);
    }

    public void finalizeFigureManipulation(String figureId) {
        StickFigureModel figure = getFigureById(figureId);
        if (figure != null) {
            logger.debug("FigureManager: Finalizing manipulation for figure {}", figureId);
            figure.applyConstraintsIteratively(MAX_CONSTRAINT_ITERATIONS, -1);
            figure.solveAllIK();
        }
    }

    public Map<String, List<Point>> getAllFigureStates() {
        Map<String, List<Point>> allStates = new HashMap<>();
        for (StickFigureModel figure : figures) {
            allStates.put(figure.id, figure.getPointCopies());
        }
        return allStates;
    }

    public Map<String, Map<String, LimbAngles>> getAllFigureAngles() {
        Map<String, Map<String, LimbAngles>> allAngles = new HashMap<>();
        for (StickFigureModel figure : figures) {
            Map<String, LimbAngles> figureAngles = new HashMap<>();
            for (IKChain chain : figure.ikChains) {
                LimbAngles angles = figure.calculateAnglesForChain(chain);
                if (angles != null) {
                    figureAngles.put(chain.name, angles);
                }
            }
            if (!figureAngles.isEmpty()) {
                allAngles.put(figure.id, figureAngles);
            }
        }
        return allAngles;
    }

}