package uk.ac.soton.comp3200.model.animation;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp3200.event.AnimationUpdateListener;
import uk.ac.soton.comp3200.model.figure.model.LimbAngles;
import uk.ac.soton.comp3200.model.math.Point;
import uk.ac.soton.comp3200.view.timeline.FrameType;

import java.util.*;
import java.util.stream.Collectors;

import static uk.ac.soton.comp3200.model.math.Utility.applyEasing;

public class TimelineModel {

    private static final Logger logger = LogManager.getLogger(TimelineModel.class);

    private static final int DEFAULT_FPS = 24;
    private int maxFrames = 120;

    private final Map<String, TreeMap<Integer, List<Point>>> figureKeyframes;
    private final Map<String, TreeMap<Integer, Map<String, LimbAngles>>> figureKeyframeAngles;
    private final Map<String, Map<Integer, List<Point>>> figureInterpolatedFrames;
    private final Map<String, Map<Integer, Map<String, LimbAngles>>> figureInterpolatedAngles;
    private final Map<String, Set<KeyframeSegment>> figureInterpolatedSegments;
    private final Set<String> managedFigureIds;

    private int fps = DEFAULT_FPS;
    private int currentFrame = 1;
    private boolean isPlaying = false;

    private Timeline playbackTimeline;

    private AnimationUpdateListener listener = null;

    public TimelineModel() {
        this.figureKeyframes = new HashMap<>();
        this.figureKeyframeAngles = new HashMap<>();
        this.figureInterpolatedFrames = new HashMap<>();
        this.figureInterpolatedAngles = new HashMap<>();
        this.figureInterpolatedSegments = new HashMap<>();
        this.managedFigureIds = new HashSet<>();
        setupPlaybackTimeline();
    }

    public int getMaxFrames() {
        return maxFrames;
    }

    public void setMaxFrames(int newMaxFrames) {
        newMaxFrames = Math.max(10, newMaxFrames);

        if (this.maxFrames != newMaxFrames) {
            logger.info("Engine Max Frames set to: {}", newMaxFrames);
            this.maxFrames = newMaxFrames;

            if (this.currentFrame > this.maxFrames) {
                goToFrame(this.maxFrames);
            } else {
                notifyStateChange();
            }
        }
    }

    public void setListener(AnimationUpdateListener listener) {
        this.listener = listener;
    }

    public int getFps() { return fps; }
    public int getCurrentFrame() { return currentFrame; }
    public boolean isPlaying() { return isPlaying; }
    public Set<String> getManagedFigureIds() { return Collections.unmodifiableSet(managedFigureIds); }

    public Map<String, TreeMap<Integer, List<Point>>> getAllFigureKeyframes() { return Collections.unmodifiableMap(figureKeyframes); }
    public Map<String, TreeMap<Integer, Map<String, LimbAngles>>> getAllFigureKeyframeAngles() { return Collections.unmodifiableMap(figureKeyframeAngles); }
    public Map<String, Set<KeyframeSegment>> getAllFigureSegments() { return Collections.unmodifiableMap(figureInterpolatedSegments); }

    public FrameType getFrameType(String figureId, int frame) {
        if (figureKeyframes.getOrDefault(figureId, new TreeMap<>()).containsKey(frame)) {
            return FrameType.KEYFRAME;
        } else if (figureInterpolatedFrames.getOrDefault(figureId, new HashMap<>()).containsKey(frame)) {
            return FrameType.INTERPOLATED;
        } else {
            return FrameType.EMPTY;
        }
    }
    private void advanceFrameForPlayback(int nextFrame) {
        this.currentFrame = nextFrame;
        logger.trace("Engine playback advanced to frame: {}", this.currentFrame);

        Map<String, List<Point>> stateForListener = new HashMap<>();
        InterpolationContext firstContextFound = null;
        for(String figId : managedFigureIds) {
            List<Point> points = getKeyframePoints(figId, this.currentFrame);
            InterpolationContext context;
            if (points == null) {
                points = getInterpolatedPoints(figId, this.currentFrame);
                if(points != null) {
                    context = getInterpolationContext(figId, this.currentFrame);
                    if (firstContextFound == null) firstContextFound = context;
                }
            }
            if (points != null) {
                stateForListener.put(figId, points);
            }
        }
        if (listener != null) {
            listener.onKeyframeSelected(stateForListener, firstContextFound);
        }

        notifyStateChange();
    }

    public List<Point> getKeyframePoints(String figureId, int frame) {
        return figureKeyframes.getOrDefault(figureId, new TreeMap<>()).get(frame);
    }

    public List<Point> getInterpolatedPoints(String figureId, int frame) {
        return figureInterpolatedFrames.getOrDefault(figureId, new HashMap<>()).get(frame);
    }

    public Map<String, LimbAngles> getInterpolatedAngles(String figureId, int frame) {
        return figureInterpolatedAngles.getOrDefault(figureId, new HashMap<>()).get(frame);
    }

    public InterpolationContext getInterpolationContext(String figureId, int frame) {
        KeyframeSegment activeSegment = findSegmentContainingFrame(figureId, frame);
        if (activeSegment == null) return null;

        List<Point> startPoints = getKeyframePoints(figureId, activeSegment.startFrame);
        List<Point> endPoints = getKeyframePoints(figureId, activeSegment.endFrame);

        if (startPoints == null || endPoints == null) {
            logger.warn("Missing keyframe data while creating context for segment {}", activeSegment);
            return null;
        }

        int startKey = activeSegment.startFrame;

        int endKey = activeSegment.endFrame;

        float t = (float) (frame - activeSegment.startFrame) / (activeSegment.endFrame - activeSegment.startFrame);
        return new InterpolationContext(startKey, endKey, t, activeSegment.easingType);
    }

    public void addFigure(String figureId) {
        if (managedFigureIds.add(figureId)) {
            logger.debug("Engine managing new figure: {}", figureId);
            figureKeyframes.computeIfAbsent(figureId, k -> new TreeMap<>());
            figureKeyframeAngles.computeIfAbsent(figureId, k -> new TreeMap<>());
            figureInterpolatedFrames.computeIfAbsent(figureId, k -> new HashMap<>());
            figureInterpolatedAngles.computeIfAbsent(figureId, k -> new HashMap<>());
            figureInterpolatedSegments.computeIfAbsent(figureId, k -> new HashSet<>());
            notifyStateChange();
        } else {
            logger.warn("Engine already managing figure: {}", figureId);
        }
    }

    public void removeFigure(String figureId) {
        if (managedFigureIds.remove(figureId)) {
            logger.debug("Engine removing figure: {}", figureId);
            figureKeyframes.remove(figureId);
            figureKeyframeAngles.remove(figureId);
            figureInterpolatedFrames.remove(figureId);
            figureInterpolatedAngles.remove(figureId);
            figureInterpolatedSegments.remove(figureId);
            notifyStateChange();
        } else {
            logger.warn("Engine cannot remove figure, not found: {}", figureId);
        }
    }

    public void setFps(int newFps) {
        newFps = Math.max(1, Math.min(60, newFps));
        if (this.fps != newFps) {
            this.fps = newFps;
            logger.debug("Engine FPS set to: {}", this.fps);
            updatePlaybackSpeed();
            notifyStateChange();
        }
    }

    public void goToFrame(int frame) {
        frame = Math.max(1, Math.min(this.maxFrames, frame));
        if (this.currentFrame != frame) {
            this.currentFrame = frame;
            logger.debug("Engine current frame set to: {} (Max: {})", this.currentFrame, this.maxFrames);

            Map<String, List<Point>> stateForListener = new HashMap<>();
            InterpolationContext firstContextFound = null;
            for(String figId : managedFigureIds) {
                List<Point> points = getKeyframePoints(figId, frame);
                InterpolationContext context;
                if (points == null) {
                    points = getInterpolatedPoints(figId, frame);
                    if(points != null) {
                        context = getInterpolationContext(figId, frame);
                        if (firstContextFound == null) firstContextFound = context;
                    }
                }
                if (points != null) {
                    stateForListener.put(figId, points);
                }
            }

            if (listener != null) {
                listener.onKeyframeSelected(stateForListener, firstContextFound);
            }

            notifyStateChange();
        }
    }

    public void addKeyframeData(String figureId, int frame, List<Point> points, Map<String, LimbAngles> angles) {
        if (!managedFigureIds.contains(figureId)) {
            logger.warn("Cannot add keyframe, figure '{}' not managed by engine.", figureId);
            return;
        }
        logger.debug("Engine adding keyframe for {} at frame {}", figureId, frame);
        TreeMap<Integer, List<Point>> keyframes = figureKeyframes.computeIfAbsent(figureId, k -> new TreeMap<>());
        TreeMap<Integer, Map<String, LimbAngles>> keyframeAngles = figureKeyframeAngles.computeIfAbsent(figureId, k -> new TreeMap<>());
        Map<Integer, List<Point>> interpolated = figureInterpolatedFrames.computeIfAbsent(figureId, k -> new HashMap<>());
        Map<Integer, Map<String, LimbAngles>> interpolatedAngles = figureInterpolatedAngles.computeIfAbsent(figureId, k -> new HashMap<>());

        interpolated.remove(frame);
        interpolatedAngles.remove(frame);
        keyframes.put(frame, points);
        if (angles != null && !angles.isEmpty()) {
            keyframeAngles.put(frame, angles);
        } else {
            keyframeAngles.remove(frame);
        }

        updateInterpolationAround(figureId, frame);
        notifyStateChange();
    }
    public void addKeyframeData(String figureId, int frame) {

        if (listener == null) {
            logger.error("Cannot add keyframe for '{}' at frame {}: No listener to provide current state.", figureId, frame);
            return;
        }

        Map<String, List<Point>> currentPoints = listener.requestFigureStates();
        Map<String, Map<String, LimbAngles>> currentAngles = listener.requestFigureAngles();

        List<Point> figureState = currentPoints.get(figureId);
        Map<String, LimbAngles> figureAngles = currentAngles.get(figureId);

        if (figureState == null) {
            logger.error("Listener did not provide point state for figure '{}'. Cannot add keyframe.", figureId);
            return;
        }

        addKeyframeData(figureId, frame, figureState, figureAngles);
    }

    public void removeKeyframeData(String figureId, int frame) {
        if (!managedFigureIds.contains(figureId)) return;
        logger.debug("Engine removing keyframe for {} at frame {}", figureId, frame);
        TreeMap<Integer, List<Point>> keyframes = figureKeyframes.get(figureId);
        TreeMap<Integer, Map<String, LimbAngles>> keyframeAngles = figureKeyframeAngles.get(figureId);

        boolean removed = keyframes != null && keyframes.remove(frame) != null;
        if (keyframeAngles != null) {
            keyframeAngles.remove(frame);
        }

        if(removed) {
            updateInterpolationAround(figureId, frame);
            notifyStateChange();
        }
    }

    public void moveKeyframeData(String figureId, int sourceFrame, int targetFrame) {
        if (!managedFigureIds.contains(figureId) || sourceFrame == targetFrame) return;
        TreeMap<Integer, List<Point>> keyframes = figureKeyframes.get(figureId);
        TreeMap<Integer, Map<String, LimbAngles>> keyframeAngles = figureKeyframeAngles.get(figureId);
        if (keyframes == null) return;

        if (keyframes.containsKey(targetFrame) || getInterpolatedPoints(figureId, targetFrame) != null) {
            logger.warn("Cannot move keyframe for '{}' to {}: Target frame not empty.", figureId, targetFrame);
            return;
        }

        List<Point> points = keyframes.remove(sourceFrame);
        Map<String, LimbAngles> angles = (keyframeAngles != null) ? keyframeAngles.remove(sourceFrame) : null;

        if (points != null) {
            logger.debug("Engine moving keyframe for {} from {} to {}", figureId, sourceFrame, targetFrame);
            figureInterpolatedFrames.computeIfAbsent(figureId, k -> new HashMap<>()).remove(sourceFrame);
            figureInterpolatedAngles.computeIfAbsent(figureId, k -> new HashMap<>()).remove(sourceFrame);

            keyframes.put(targetFrame, points);
            if (angles != null) {
                keyframeAngles.put(targetFrame, angles);
            }

            updateInterpolationAround(figureId, sourceFrame);
            updateInterpolationAround(figureId, targetFrame);
            notifyStateChange();

            if(this.currentFrame == sourceFrame) {
                this.currentFrame = targetFrame;
            }

        } else {
            logger.warn("Cannot move keyframe for '{}' from {}: No keyframe data found at source.", figureId, sourceFrame);
        }
    }

    public void addInterpolationSegment(String figureId, int frame, EasingType easingType) {
        if (!managedFigureIds.contains(figureId)) return;
        Integer prevKey = findPreviousKeyframe(figureId, frame);
        Integer nextKey = findNextKeyframe(figureId, frame);

        if (prevKey != null && nextKey != null) {
            KeyframeSegment newSegment = new KeyframeSegment(prevKey, nextKey, easingType);
            Set<KeyframeSegment> segments = figureInterpolatedSegments.computeIfAbsent(figureId, k -> new HashSet<>());

            segments.removeIf(s -> s.startFrame == newSegment.startFrame && s.endFrame == newSegment.endFrame);

            if (segments.add(newSegment)) {
                logger.debug("Engine added segment {} for {}", newSegment, figureId);
            } else {
                logger.debug("Engine segment {} already exists for {}. Refreshing.", newSegment, figureId);
            }
            performInterpolation(figureId, newSegment);
            notifyStateChange();
        } else {
            logger.warn("Cannot add segment for figure '{}' at frame {}: No surrounding keyframes.", figureId, frame);
        }
    }

    public void removeInterpolationSegment(String figureId, int frame) {
        if (!managedFigureIds.contains(figureId)) return;
        Set<KeyframeSegment> segments = figureInterpolatedSegments.get(figureId);
        if (segments == null) return;

        KeyframeSegment segmentToRemove = findSegmentContainingFrame(figureId, frame);

        if (segmentToRemove != null && segments.remove(segmentToRemove)) {
            logger.debug("Engine removing segment {} for {}", segmentToRemove, figureId);
            clearInterpolatedData(figureId, segmentToRemove);
            notifyStateChange();
        } else {
            logger.warn("Engine couldn't find/remove segment containing frame {} for {}", frame, figureId);
        }
    }

    private void performInterpolation(String figureId, KeyframeSegment segment) {
        int startFrame = segment.startFrame;
        int endFrame = segment.endFrame;
        EasingType easingType = segment.easingType;

        TreeMap<Integer, List<Point>> keyframes = figureKeyframes.getOrDefault(figureId, new TreeMap<>());
        TreeMap<Integer, Map<String, LimbAngles>> keyframeAngles = figureKeyframeAngles.getOrDefault(figureId, new TreeMap<>());
        Map<Integer, List<Point>> interpolatedPoints = figureInterpolatedFrames.computeIfAbsent(figureId, k -> new HashMap<>());
        Map<Integer, Map<String, LimbAngles>> interpolatedAngles = figureInterpolatedAngles.computeIfAbsent(figureId, k -> new HashMap<>());

        List<Point> startPoints = keyframes.get(startFrame);
        List<Point> endPoints = keyframes.get(endFrame);
        Map<String, LimbAngles> startAnglesMap = keyframeAngles.get(startFrame);
        Map<String, LimbAngles> endAnglesMap = keyframeAngles.get(endFrame);

        boolean pointsValid = (startPoints != null && endPoints != null && startPoints.size() == endPoints.size());

        if (!pointsValid || endFrame <= startFrame + 1) {
            logger.warn("Cannot interpolate figure '{}' segment {}: Invalid keyframes.", figureId, segment);
            return;
        }
        logger.trace("Engine calculating interpolation for figure '{}' segment: {}", figureId, segment);

        for (int frame = startFrame + 1; frame < endFrame; frame++) {
            if (keyframes.containsKey(frame)) {
                interpolatedPoints.remove(frame);
                interpolatedAngles.remove(frame);
                continue;
            }

            float linear_t = (float) (frame - startFrame) / (endFrame - startFrame);
            float eased_t = applyEasing(linear_t, easingType);

            List<Point> interpPose = new ArrayList<>(startPoints.size());
            for (int i = 0; i < startPoints.size(); i++) {
                Point start = startPoints.get(i); Point end = endPoints.get(i);
                if (start != null && end != null) interpPose.add(start.interpolate(end, eased_t));
                else interpPose.add(start != null ? new Point(start.x, start.y) : (end != null ? new Point(end.x, end.y) : new Point(0,0)));
            }
            interpolatedPoints.put(frame, interpPose);

            Map<String, LimbAngles> interpAngles = new HashMap<>();
            if (startAnglesMap != null && endAnglesMap != null) {
                Set<String> commonChains = new HashSet<>(startAnglesMap.keySet());
                commonChains.retainAll(endAnglesMap.keySet());
                for (String chainName : commonChains) {
                    LimbAngles startLimb = startAnglesMap.get(chainName);
                    LimbAngles endLimb = endAnglesMap.get(chainName);
                    if(startLimb != null && endLimb != null) {
                        interpAngles.put(chainName, startLimb.interpolate(endLimb, eased_t));
                    }
                }
            }
            if (!interpAngles.isEmpty()) {
                interpolatedAngles.put(frame, interpAngles);
            } else {
                interpolatedAngles.remove(frame);
            }
        }
        logger.trace("Finished interpolation calculation for segment {}", segment);
    }

    private void updateInterpolationAround(String figureId, int frameNumber) {
        Set<KeyframeSegment> segments = figureInterpolatedSegments.get(figureId);
        if (segments == null) return;

        Set<KeyframeSegment> segmentsToRemove = new HashSet<>();
        Set<KeyframeSegment> segmentsToRecalculate = new HashSet<>();

        for (KeyframeSegment segment : segments) {
            if (segment.startFrame == frameNumber || segment.endFrame == frameNumber) {
                segmentsToRemove.add(segment);
            } else if (segment.containsFrame(frameNumber)) {
                segmentsToRemove.add(segment);
            }
        }

        Integer prevKey = findPreviousKeyframe(figureId, frameNumber);
        Integer nextKey = findNextKeyframe(figureId, frameNumber);
        if (prevKey != null && nextKey != null) {
            boolean exists = segments.stream()
                    .anyMatch(s -> s.startFrame == prevKey && s.endFrame == nextKey && !segmentsToRemove.contains(s));
            if (exists) {
                segments.stream()
                        .filter(s -> s.startFrame == prevKey && s.endFrame == nextKey)
                        .findFirst()
                        .ifPresent(segmentsToRecalculate::add);
            }
        }

        if (!segmentsToRemove.isEmpty()) {
            logger.debug("Engine removing {} segments around frame {} for {}", segmentsToRemove.size(), frameNumber, figureId);
            segmentsToRemove.forEach(seg -> clearInterpolatedData(figureId, seg));
            segments.removeAll(segmentsToRemove);
        }
        if (!segmentsToRecalculate.isEmpty()) {
            logger.debug("Engine recalculating {} segments around frame {} for {}", segmentsToRecalculate.size(), frameNumber, figureId);
            segmentsToRecalculate.forEach(seg -> performInterpolation(figureId, seg));
        }
    }

    private void clearInterpolatedData(String figureId, KeyframeSegment segment) {
        Map<Integer, List<Point>> interpolated = figureInterpolatedFrames.get(figureId);
        Map<Integer, Map<String, LimbAngles>> interpolatedAngles = figureInterpolatedAngles.get(figureId);
        if (interpolated != null || interpolatedAngles != null) {
            for (int i = segment.startFrame + 1; i < segment.endFrame; i++) {
                if (interpolated != null) interpolated.remove(i);
                if (interpolatedAngles != null) interpolatedAngles.remove(i);
            }
        }
    }

    public Integer findPreviousKeyframe(String figureId, int frameNumber) {
        TreeMap<Integer, ?> keyframes = figureKeyframes.get(figureId);
        return (keyframes != null) ? keyframes.lowerKey(frameNumber) : null;
    }

    public Integer findNextKeyframe(String figureId, int frameNumber) {
        TreeMap<Integer, ?> keyframes = figureKeyframes.get(figureId);
        return (keyframes != null) ? keyframes.higherKey(frameNumber) : null;
    }

    public KeyframeSegment findSegmentContainingFrame(String figureId, int frame) {
        Set<KeyframeSegment> segments = figureInterpolatedSegments.get(figureId);
        if (segments != null) {
            for (KeyframeSegment segment : segments) {
                if (segment.containsFrame(frame)) {
                    return segment;
                }
            }
        }
        return null;
    }

    public List<Integer> findNearestPastKeyframeNumbers(int currentFrame, int count) {
        if (count <= 0 || figureKeyframes.isEmpty()) return Collections.emptyList();
        TreeSet<Integer> pastKeys = new TreeSet<>(Comparator.reverseOrder());
        figureKeyframes.values().forEach(map -> pastKeys.addAll(map.headMap(currentFrame).keySet()));
        return pastKeys.stream().limit(count).sorted().collect(Collectors.toList());
    }

    public List<Integer> findNearestFutureKeyframeNumbers(int currentFrame, int count) {
        if (count <= 0 || figureKeyframes.isEmpty()) return Collections.emptyList();
        TreeSet<Integer> futureKeys = new TreeSet<>();
        figureKeyframes.values().forEach(map -> futureKeys.addAll(map.tailMap(currentFrame, false).keySet()));
        return futureKeys.stream().limit(count).collect(Collectors.toList());
    }

    private void setupPlaybackTimeline() {
        playbackTimeline = new Timeline();
        updatePlaybackSpeed();
        playbackTimeline.setCycleCount(Timeline.INDEFINITE);
    }

    private void updatePlaybackSpeed() {
        if (playbackTimeline != null) {
            boolean wasRunning = playbackTimeline.getStatus() == Timeline.Status.RUNNING;
            playbackTimeline.stop();
            playbackTimeline.getKeyFrames().clear();
            playbackTimeline.getKeyFrames().add(
                    new KeyFrame(Duration.seconds(1.0 / fps), e -> playNextFrame())
            );
            if (wasRunning) {
                playbackTimeline.play();
            }
        }
    }

    public void play() {
        if (!isPlaying) {
            isPlaying = true;
            logger.info("Engine starting playback at {} FPS", fps);
            if (listener != null) listener.hideEditorControls();
            updatePlaybackSpeed();
            playbackTimeline.play();
            notifyPlaybackChange();
        }
    }

    public void stop() {
        if (isPlaying) {
            isPlaying = false;
            logger.info("Engine stopping playback");
            playbackTimeline.stop();
            if (listener != null) listener.showEditorControls();
            notifyPlaybackChange();
        }
    }

    private void playNextFrame() {
        if (!isPlaying) return;

        int minKeyframeFound = Integer.MAX_VALUE;
        int maxKeyframeFound = 0;
        for (TreeMap<Integer, ?> figureFrames : figureKeyframes.values()) {
            if (figureFrames != null && !figureFrames.isEmpty()) {
                int figMax = figureFrames.lastKey();
                int figMin = figureFrames.firstKey();
                if (figMax > maxKeyframeFound) {
                    maxKeyframeFound = figMax;
                }
                if (figMin < minKeyframeFound) {
                    minKeyframeFound = figMin;
                }
            }
        }

        int firstFrame = (minKeyframeFound != Integer.MAX_VALUE && minKeyframeFound > 0) ? minKeyframeFound : 1;

        int actualLastDataFrame = maxKeyframeFound > 0 ? maxKeyframeFound : 1;
        int playbackEndPoint = Math.min(actualLastDataFrame, this.maxFrames);

        if (playbackEndPoint < firstFrame) {
            logger.warn("Playback range invalid (Start: {}, End: {} based on MaxFrames: {}). Stopping playback.",
                    firstFrame, playbackEndPoint, this.maxFrames);
            stop();
            return;
        }

        int nextFrame = currentFrame + 1;

        if (nextFrame > playbackEndPoint) {
            if (playbackEndPoint > firstFrame) {
                nextFrame = firstFrame;
            } else {
                stop();
                return;
            }
        }

        if (nextFrame > this.maxFrames) {
            logger.warn("Calculated nextFrame ({}) exceeded maxFrames ({}), stopping playback.", nextFrame, this.maxFrames);
            stop();
            return;
        }

        advanceFrameForPlayback(nextFrame);
    }

    public void resetState() {
        logger.info("Engine resetting state.");
        stop();
        figureKeyframes.clear();
        figureKeyframeAngles.clear();
        figureInterpolatedFrames.clear();
        figureInterpolatedAngles.clear();
        figureInterpolatedSegments.clear();
        managedFigureIds.clear();
        currentFrame = 1;
        fps = DEFAULT_FPS;
        maxFrames = 120;
        updatePlaybackSpeed();
        notifyStateChange();
    }

    public void loadState(int newFps, int newCurrentFrame, int newMaxFrames,
                          List<String> figureIds,
                          Map<String, TreeMap<Integer, List<Point>>> loadedFigureKeyframes,
                          Map<String, TreeMap<Integer, Map<String, LimbAngles>>> loadedFigureAngles,
                          Map<String, Set<KeyframeSegment>> loadedFigureSegments) {
        logger.info("Engine loading state...");
        stop();

        figureKeyframes.clear();
        figureKeyframeAngles.clear();
        figureInterpolatedFrames.clear();
        figureInterpolatedAngles.clear();
        figureInterpolatedSegments.clear();
        managedFigureIds.clear();

        this.fps = Math.max(1, Math.min(60, newFps));
        this.maxFrames = Math.max(10, newMaxFrames);
        managedFigureIds.addAll(figureIds);

        loadedFigureKeyframes.forEach((figId, map) -> figureKeyframes.computeIfAbsent(figId, k->new TreeMap<>()).putAll(map));
        loadedFigureAngles.forEach((figId, map) -> figureKeyframeAngles.computeIfAbsent(figId, k->new TreeMap<>()).putAll(map));
        loadedFigureSegments.forEach((figId, set) -> figureInterpolatedSegments.computeIfAbsent(figId, k->new HashSet<>()).addAll(set));

        updatePlaybackSpeed();

        logger.info("Recalculating interpolation for loaded state...");
        for(String figId : managedFigureIds) {
            Set<KeyframeSegment> segments = figureInterpolatedSegments.get(figId);
            if (segments != null) {
                for(KeyframeSegment segment : segments) {
                    performInterpolation(figId, segment);
                }
            }
        }

        int targetFrame = Math.max(1, Math.min(this.maxFrames, newCurrentFrame));
        goToFrame(targetFrame);

        logger.info("Engine state loaded (FPS: {}, MaxFrames: {}).", this.fps, this.maxFrames);
    }

    private void notifyStateChange() {
        logger.debug("TimelineModel: Notifying state change. Listener is null? {}", (listener == null));
        if (listener != null) {
            listener.onTimelineStateChanged();
        }
    }

    private void notifyPlaybackChange() {
        if (listener != null) {
            listener.onPlaybackStateChanged(isPlaying);
        }
    }
}