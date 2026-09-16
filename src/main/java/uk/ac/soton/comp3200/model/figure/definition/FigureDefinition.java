package uk.ac.soton.comp3200.model.figure.definition;

import java.util.ArrayList;
import java.util.List;

public class FigureDefinition {
    public String name = "DefaultFigure";
    public List<PointDefinition> pointDefs = new ArrayList<>();
    public List<uk.ac.soton.comp3200.model.figure.definition.SegmentDefinition> segmentDefs = new ArrayList<>();
    public List<IKChainDefinition> ikChainDefs = new ArrayList<>();
    public List<ConstraintDefinition> constraintDefs = new ArrayList<>();

    public FigureDefinition addPoint(PointDefinition pd) { pointDefs.add(pd); return this; }
    public void addSegment(uk.ac.soton.comp3200.model.figure.definition.SegmentDefinition sd) { segmentDefs.add(sd);
    }
    public void addIKChain(IKChainDefinition ikd) { ikChainDefs.add(ikd);
    }
    public void addConstraint(ConstraintDefinition cd) { constraintDefs.add(cd);
    }
}