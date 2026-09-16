package uk.ac.soton.comp3200.model.io;

import uk.ac.soton.comp3200.model.animation.KeyframeSegment;
import uk.ac.soton.comp3200.model.math.Point;
import uk.ac.soton.comp3200.model.figure.model.LimbAngles;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Data Transfer Object representing the complete state of an animation for saving/loading.
 */
public class AnimationSaveData {

    public SettingsData settings;
    public List<String> figures;
    // Figure IDs are keys, FigureSpecificData are values
    public Map<String, FigureSpecificData> figureData;

    /**
     * Inner class holding settings data.
     */
    public static class SettingsData {
        public int fps;
        public int currentFrame;
        public int maxFrames;
    }


    /**
     * Inner class holding data specific to a single figure.
     * Needs to be public and static to be accessible from SaveManager.
     */
    public static class FigureSpecificData {
        // Frame numbers are keys, points are values
        public Map<Integer, List<Point>> keyframes;
        // Frame numbers are keys, value is: <Limb names are keys, Angle data is value>
        public Map<Integer, Map<String, LimbAngles>> angles;

        public Set<KeyframeSegment> interpolationSegments;
    }
}