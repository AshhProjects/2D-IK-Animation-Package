package uk.ac.soton.comp3200.controller;

import javafx.scene.input.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp3200.model.animation.EasingType;
import uk.ac.soton.comp3200.model.animation.TimelineModel;
import uk.ac.soton.comp3200.view.timeline.FrameCell;
import uk.ac.soton.comp3200.view.timeline.FrameType;
import uk.ac.soton.comp3200.view.timeline.TimelineView;

import java.util.Objects;

/**
 * Controller for the TimelineView. Handles user interactions with the timeline UI
 * and translates them into actions on the TimelineModel.
 */
public class TimelineController {

    private static final Logger logger = LogManager.getLogger(TimelineController.class);

    private TimelineModel model;
    private TimelineView view;

    private boolean isDraggingPlayhead = false;
    private static final DataFormat KEYFRAME_DRAG_FORMAT = new DataFormat("comp3200/keyframeDragInfo");

    private MenuController menuController;


    public void setModel(TimelineModel model) {
        this.model = model;
    }

    public void setView(TimelineView view) {
        this.view = view;
    }

    public void setMenuController(MenuController menuController) {
        this.menuController = menuController;
    }

    public void handleAddKeyframeClicked() {
        int frame = model.getCurrentFrame();
        logger.debug("TimelineController: Add Keyframe button clicked for frame {}", frame);
        for (String figId : model.getManagedFigureIds()) {
            if (model.getKeyframePoints(figId, frame) == null) {
                model.addKeyframeData(figId, frame);
            }
        }
    }

    public void handleRemoveKeyframeClicked() {
        int frame = model.getCurrentFrame();
        logger.debug("TimelineController: Remove Keyframe button clicked for frame {}", frame);
        for (String figId : model.getManagedFigureIds()) {
            if (model.getKeyframePoints(figId, frame) != null) {
                model.removeKeyframeData(figId, frame);
            }
        }
    }

    public void handlePlayClicked() {
        logger.debug("TimelineController: Play button clicked");
        model.play();
    }

    public void handleStopClicked() {
        logger.debug("TimelineController: Stop button clicked");
        model.stop();
    }

    public void handleGoToFirstFrameClicked() {
        logger.debug("TimelineController: Go to first frame button clicked");
        model.goToFrame(1);
    }

    public void handleFrameCellClicked(String figureId, int frameNumber, MouseButton button) {
        logger.trace("TimelineController: FrameCell clicked - Fig: {}, Frame: {}, Button: {}", figureId, frameNumber, button);
        if (button == MouseButton.PRIMARY) {
            handleLayerSelected(figureId);
            model.goToFrame(frameNumber);
        }
    }

    public void handleFrameCellAddKeyframe(String figureId, int frameNumber) {
        logger.debug("TimelineController: Request Add Keyframe - Fig: {}, Frame: {}", figureId, frameNumber);
        model.addKeyframeData(figureId, frameNumber);
    }

    public void handleFrameCellRemoveKeyframe(String figureId, int frameNumber) {
        logger.debug("TimelineController: Request Remove Keyframe - Fig: {}, Frame: {}", figureId, frameNumber);
        model.removeKeyframeData(figureId, frameNumber);
    }

    public void handleFrameCellCreateInterpolation(String figureId, int frameNumber, EasingType easingType) {
        logger.debug("TimelineController: Request Interpolation - Fig: {}, Frame: {}, Easing: {}", figureId, frameNumber, easingType);
        model.addInterpolationSegment(figureId, frameNumber, easingType);
    }

    public void handleFrameCellRemoveInterpolation(String figureId, int frameNumber) {
        logger.debug("TimelineController: Request Remove Interpolation - Fig: {}, Frame: {}", figureId, frameNumber);
        model.removeInterpolationSegment(figureId, frameNumber);
    }

    public void handlePlayheadDragStarted(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            isDraggingPlayhead = true;
            if (model.isPlaying()) {
                model.stop();

            }
            if (view != null) {
                view.setPlayheadDraggingVisualState(true);
                view.showFramePreview(model.getCurrentFrame());
            }
            logger.trace("TimelineController: Playhead drag started");
        }
    }

    /**
     * Handles the continuous dragging of the playhead.
     * Called by TimelineView with the calculated horizontal position within the frame area.
     *
     * @param xPositionInFrameArea The mouse's horizontal coordinate relative to the start of the frameRowsColumn in TimelineView.
     */
    public void handlePlayheadDragged(double xPositionInFrameArea) {
        if (!isDraggingPlayhead || model == null || view == null) return;

        int frame = view.calculateFrameFromPosition(xPositionInFrameArea);

        if (frame != model.getCurrentFrame()) {
            model.goToFrame(frame);
            logger.trace("TimelineController: Playhead dragged, model goToFrame({})", frame);
        }

        view.movePlayheadTemporarilyDuringDrag(xPositionInFrameArea);

        view.updateFramePreviewText(frame);
    }

    public void handlePlayheadDragEnded() {
        if (isDraggingPlayhead) {
            isDraggingPlayhead = false;
            if (view != null) {
                view.setPlayheadDraggingVisualState(false);
                view.hideFramePreview();
            }
            logger.trace("TimelineController: Playhead drag ended at frame {}", model.getCurrentFrame());
        }
    }

    public boolean isDraggingPlayhead() {
        return isDraggingPlayhead;
    }

    public void handleLayerSelected(String figureId) {
        logger.debug("TimelineController: Layer selected - Fig: {}", figureId);
        if (!Objects.equals(figureId, menuController.getSelectedFigureId())) {
            menuController.handleFigureSelection(figureId);
        }
        if (view != null) view.highlightLayer(figureId);
        else logger.warn("TimelineController: View is null in handleLayerSelected");
    }

    public void handleLayerContextMenuDeleteFigure(String figureId) {
        logger.debug("TimelineController: Layer context menu delete - Fig: {}", figureId);
        menuController.handleDeleteFigureRequestFromTimeline(figureId);
    }

    public void handleTimelineBackgroundClicked() {
        logger.trace("TimelineController: Timeline background clicked.");
        menuController.handleTimelineBackgroundClick();
    }

    public void handleKeyframeDragDetected(FrameCell cell, MouseEvent event) {
        if (model.getFrameType(cell.getFigureId(), cell.getFrameNumber()) == FrameType.KEYFRAME
                && event.getButton() == MouseButton.PRIMARY)
        {
            logger.trace("TimelineController: Drag detected on keyframe - Fig: {}, Frame: {}", cell.getFigureId(), cell.getFrameNumber());
            Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            String dragInfo = cell.getFigureId() + ":" + cell.getFrameNumber();
            content.put(KEYFRAME_DRAG_FORMAT, dragInfo);
            content.putString(dragInfo);
            db.setContent(content);
            view.setCellDragState(cell, true);
        }
    }

    public void handleKeyframeDragOver(FrameCell cell, DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean canDrop = false;
        if (db.hasContent(KEYFRAME_DRAG_FORMAT)) {
            String dragInfo = (String) db.getContent(KEYFRAME_DRAG_FORMAT);
            if (dragInfo != null && dragInfo.contains(":")) {
                String[] parts = dragInfo.split(":", 2);
                String sourceFigureId = parts[0];
                int sourceFrame = -1; try { sourceFrame = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}

                if (sourceFrame != -1 && sourceFigureId.equals(cell.getFigureId()) &&
                        model.getFrameType(cell.getFigureId(), cell.getFrameNumber()) == FrameType.EMPTY &&
                        sourceFrame != cell.getFrameNumber())
                {
                    canDrop = true;
                }
            }
        }

        if (canDrop) {
            event.acceptTransferModes(TransferMode.MOVE);
            view.setCellDropTargetState(cell, true);
        } else {
            view.setCellDropTargetState(cell, false);
        }
        logger.trace("TimelineController: Drag over - Fig: {}, Frame: {}, CanDrop: {}", cell.getFigureId(), cell.getFrameNumber(), canDrop);

    }

    public void handleKeyframeDragExited(FrameCell cell, DragEvent event) {
        logger.trace("TimelineController: Drag exited - Fig: {}, Frame: {}", cell.getFigureId(), cell.getFrameNumber());
        view.setCellDropTargetState(cell, false);
    }

    public void handleKeyframeDragDropped(FrameCell cell, DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasContent(KEYFRAME_DRAG_FORMAT)) {
            String dragInfo = (String) db.getContent(KEYFRAME_DRAG_FORMAT);
            if (dragInfo != null && dragInfo.contains(":")) {
                String[] parts = dragInfo.split(":", 2);
                String sourceFigureId = parts[0];
                int sourceFrame = -1; try { sourceFrame = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}

                if (sourceFrame != -1 && sourceFigureId.equals(cell.getFigureId()) &&
                        model.getFrameType(cell.getFigureId(), cell.getFrameNumber()) == FrameType.EMPTY &&
                        sourceFrame != cell.getFrameNumber())
                {
                    logger.debug("TimelineController: Keyframe dropped - Fig: {}, From: {}, To: {}", sourceFigureId, sourceFrame, cell.getFrameNumber());
                    model.moveKeyframeData(sourceFigureId, sourceFrame, cell.getFrameNumber());
                    success = true;
                }
            }
        }
        event.setDropCompleted(success);
        logger.trace("TimelineController: Drag dropped - Success: {}", success);
    }

    public void handleKeyframeDragDone(FrameCell cell, DragEvent event) {
        logger.trace("TimelineController: Drag done - Fig: {}, Frame: {}", cell.getFigureId(), cell.getFrameNumber());
        view.setCellDragState(cell, false);
    }
}