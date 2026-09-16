package uk.ac.soton.comp3200.model.figure.factory;

import uk.ac.soton.comp3200.model.figure.model.IKChain;
import uk.ac.soton.comp3200.model.figure.model.PointData;
import uk.ac.soton.comp3200.model.figure.model.Segment;
import uk.ac.soton.comp3200.model.figure.model.StickFigureModel;
import uk.ac.soton.comp3200.model.figure.constraint.Constraint;
import uk.ac.soton.comp3200.model.figure.constraint.FixedDistanceConstraint;
import uk.ac.soton.comp3200.model.figure.constraint.RelativeOffsetConstraint;
import uk.ac.soton.comp3200.model.figure.definition.ConstraintDefinition;
import uk.ac.soton.comp3200.model.figure.definition.FigureDefinition;
import uk.ac.soton.comp3200.model.figure.definition.IKChainDefinition;
import uk.ac.soton.comp3200.model.figure.definition.PointDefinition;
import uk.ac.soton.comp3200.model.figure.definition.SegmentDefinition;

import java.util.HashMap;
import java.util.Map;

public class StickFigureFactory {

    public static StickFigureModel build(FigureDefinition definition) {
        StickFigureModel model = new StickFigureModel();
        Map<String, Integer> nameToIndexMap = new HashMap<>();

        for (int i = 0; i < definition.pointDefs.size(); i++) {
            PointDefinition pd = definition.pointDefs.get(i);
            if (nameToIndexMap.containsKey(pd.name)) {
                System.err.println("Warning: Duplicate point name '" + pd.name + "'");
            }

            PointData pointData = new PointData(pd.initialX, pd.initialY, pd.name);
            pointData.isDraggable = pd.isDraggable;
            pointData.color = pd.color;
            pointData.shape = pd.shape;
            pointData.size = pd.size;
            pointData.isVisible = pd.isVisible;
            pointData.isPoseControlPoint = pd.isPoseControlPoint;
            model.points.add(pointData);
            nameToIndexMap.put(pd.name, i);
        }
        model.setNameToIndexMap(nameToIndexMap);

        for (SegmentDefinition sd : definition.segmentDefs) {
            Integer startIndex = nameToIndexMap.get(sd.startPointName);
            Integer endIndex = nameToIndexMap.get(sd.endPointName);

            if (startIndex == null || endIndex == null) {
                System.err.println("Error creating segment: Point name not found ('" + sd.startPointName + "' or '" + sd.endPointName + "')");
                continue;
            }

            Segment segment = new Segment(startIndex, endIndex, sd.color, sd.thickness, sd.isCircle);
            segment.isFilled = sd.isFilled;
            model.segments.add(segment);

            if (sd.fixedLength) {
                PointData startPD = model.getPointData(startIndex);
                PointData endPD = model.getPointData(endIndex);
                segment.calculateLength(startPD.position, endPD.position);
            }
        }

        for (IKChainDefinition ikd : definition.ikChainDefs) {
            Integer startIndex = nameToIndexMap.get(ikd.startJointName);
            Integer middleIndex = nameToIndexMap.get(ikd.middleJointName);
            Integer endIndex = nameToIndexMap.get(ikd.endPointName);
            Integer effectorIndex = nameToIndexMap.get(ikd.effectorName);

            if (startIndex == null || middleIndex == null || endIndex == null || effectorIndex == null) {
                System.err.println("Error creating IK chain '" + ikd.name + "': Point name not found.");
                continue;
            }

            IKChain ikChain = new IKChain(ikd.name, startIndex, middleIndex, endIndex, effectorIndex, ikd.preferPositiveAngle);
            model.ikChains.add(ikChain);
            model.ikMiddleJointIndices.add(middleIndex);

        }

        for (ConstraintDefinition cd : definition.constraintDefs) {
            Integer p1Index = nameToIndexMap.get(cd.point1Name);
            Integer p2Index = nameToIndexMap.get(cd.point2Name);

            if (p1Index == null || p2Index == null) {
                System.err.println("Error creating constraint: Point name not found ('" + cd.point1Name + "' or '" + cd.point2Name + "')");
                continue;
            }

            Constraint constraint = null;
            switch (cd.type) {
                case FIXED_DISTANCE:
                    constraint = new FixedDistanceConstraint(p1Index, p2Index);
                    break;
                case RELATIVE_OFFSET:
                    constraint = new RelativeOffsetConstraint(p1Index, p2Index);
                    break;
            }

            if (constraint != null) {
                model.constraints.add(constraint);
            }
        }

        model.initializeModel();

        return model;
    }
}