package uk.ac.soton.comp3200.model.animation;


public final class InterpolationContext {
    public final int startKey;
    public final int endKey;
    public final float t;
    public final EasingType easingType;

    public InterpolationContext(int startKey, int endKey, float t, EasingType easingType) {
        this.startKey = startKey;
        this.endKey = endKey;
        this.easingType = easingType;
        this.t = t;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InterpolationContext that = (InterpolationContext) o;
        return Float.compare(that.t, t) == 0 &&
                java.util.Objects.equals(startKey, that.startKey) &&
                java.util.Objects.equals(endKey, that.endKey) &&
                java.util.Objects.equals(easingType, that.easingType);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(startKey, endKey, t);
    }

    @Override
    public String toString() {
        return "InterpolationContext{" +
                "startKey=" + startKey +
                ", endKey=" + endKey +
                ", easingType=" + easingType +
                ", t=" + t +
                '}';
    }
}
