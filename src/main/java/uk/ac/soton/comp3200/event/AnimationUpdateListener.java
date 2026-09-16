package uk.ac.soton.comp3200.event;

import uk.ac.soton.comp3200.model.animation.InterpolationContext;
import uk.ac.soton.comp3200.model.figure.model.LimbAngles;
import uk.ac.soton.comp3200.model.math.Point;

import java.util.List;
import java.util.Map;

/**
 * Interface for the AnimationEngine to notify the UI about state changes
 * and to request information from the UI/Editor.
 */
public interface AnimationUpdateListener {

    /**
     * Called when a significant part of the timeline's state has changed
     * (e.g., current frame, keyframes added/removed, interpolation changed, figures added/removed, fps changed).
     * The UI should query the engine for the current state and update its display.
     */
    void onTimelineStateChanged();

    /**
     * Called specifically when the playback state (playing/stopped) changes.
     * @param isPlaying True if playback started, false if stopped.
     */
    void onPlaybackStateChanged(boolean isPlaying);

    /**
     * Called when the engine loads a specific frame's data (could be keyframe or interpolated).
     * The UI (e.g., MenuScene) should use this data to update the figure(s) display on the canvas.
     *
     * @param frameFigureStates The state data (point positions) for figures at the current frame.
     *                          Map key is figureId, value is the list of points.
     * @param context           Interpolation context if the frame is interpolated, otherwise null.
     *                          Contains start/end key info and easing factor.
     */
    void onKeyframeSelected(Map<String, List<Point>> frameFigureStates, InterpolationContext context);

    /**
     * Called by the engine when it needs the current pose (point positions)
     * of all figures managed by the editor (e.g., to add a new keyframe).
     *
     * @return A map where keys are figure IDs and values are lists of *copies*
     *         of the current points for each figure. Must be copies.
     */
    Map<String, List<Point>> requestFigureStates();

    /**
     * Called by the engine when it needs the current pose (limb angles)
     * of all figure IK chains managed by the editor (e.g., to add a new keyframe).
     *
     * @return A map where keys are figure IDs and values are maps (chain name -> LimbAngles)
     *         representing the current angles for each figure's IK chains.
     */
    Map<String, Map<String, LimbAngles>> requestFigureAngles();


    /**
     * Requests the UI/Editor to hide interactive controls (like draggable points).
     * Called when playback starts or during export.
     */
    void hideEditorControls();

    /**
     * Requests the UI/Editor to show interactive controls.
     * Called when playback stops or after export.
     */
    void showEditorControls();

}