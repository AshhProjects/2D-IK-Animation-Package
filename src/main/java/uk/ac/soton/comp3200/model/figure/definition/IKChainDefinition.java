package uk.ac.soton.comp3200.model.figure.definition;

public class IKChainDefinition {
    public String name;
    public String startJointName;
    public String middleJointName;
    public String endPointName;
    public String effectorName;
    public boolean preferPositiveAngle;

    public IKChainDefinition(String name, String start, String middle, String end, String effector, boolean positiveAngle) {
        this.name = name;
        this.startJointName = start;
        this.middleJointName = middle;
        this.endPointName = end;
        this.effectorName = effector;
        this.preferPositiveAngle = positiveAngle;
    }
}