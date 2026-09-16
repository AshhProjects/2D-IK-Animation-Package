package uk.ac.soton.comp3200.model.animation;


import java.util.Objects;

public class KeyframeSegment {
    public int startFrame;
    public int endFrame;
    public EasingType easingType;

    public KeyframeSegment() {}

    public KeyframeSegment(int start, int end, EasingType type) {
        if (type == null) throw new IllegalArgumentException("EasingType cannot be null");
        this.startFrame = start;
        this.endFrame = end;
        this.easingType = type;
    }

    public KeyframeSegment(int start, int end) {
        this(start, end, EasingType.LINEAR);
    }

    public boolean containsFrame(int frame) { return frame > startFrame && frame < endFrame; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeyframeSegment that = (KeyframeSegment) o;
        return startFrame == that.startFrame && endFrame == that.endFrame && easingType == that.easingType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startFrame, endFrame, easingType);
    }

    @Override
    public String toString() {
        return "Segment[" + startFrame + " -> " + endFrame + " (" + easingType.name() + ")]";
    }
}