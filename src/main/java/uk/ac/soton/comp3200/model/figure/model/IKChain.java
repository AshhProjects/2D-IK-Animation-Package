package uk.ac.soton.comp3200.model.figure.model;

public class IKChain {
    public String name;
    public int startJointIndex;
    public int middleJointIndex;
    public int endPointIndex;
    public int effectorIndex;

    public double length1 = -1;
    public double length2 = -1;
    public boolean preferPositiveAngle;

    public IKChain(String name, int start, int middle, int end, int effector, boolean preferPositiveAngle) {
        this.name = name;
        this.startJointIndex = start;
        this.middleJointIndex = middle;
        this.endPointIndex = end;
        this.effectorIndex = effector;
        this.preferPositiveAngle = preferPositiveAngle;
    }
}