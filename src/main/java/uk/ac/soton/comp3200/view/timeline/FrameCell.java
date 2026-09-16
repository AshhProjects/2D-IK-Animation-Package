package uk.ac.soton.comp3200.view.timeline;

import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import uk.ac.soton.comp3200.model.animation.EasingType;
import uk.ac.soton.comp3200.controller.TimelineController;

/**
 * Custom UI class for frame cells in the timeline.
 * Interacts with TimelineView for state reading and TimelineController for actions.
 */
public class FrameCell extends StackPane {
    private final int frameNumber;
    private final String figureId;
    private final Rectangle background;
    private final Circle keyframeIndicator;
    private FrameType frameType = FrameType.EMPTY;

    private final TimelineView timelineView;
    private final TimelineController timelineController;

    public FrameCell(int frameNumber, String figureId, TimelineView timelineView, TimelineController timelineController) {
        this.frameNumber = frameNumber;
        this.figureId = figureId;
        this.timelineView = timelineView;
        this.timelineController = timelineController;
        this.setPrefSize(TimelineView.FRAME_WIDTH, TimelineView.FRAME_HEIGHT);

        background = new Rectangle(TimelineView.FRAME_WIDTH, TimelineView.FRAME_HEIGHT);
        keyframeIndicator = new Circle(TimelineView.FRAME_WIDTH / 4.5);
        keyframeIndicator.setVisible(false);
        StackPane.setAlignment(keyframeIndicator, javafx.geometry.Pos.CENTER);

        getChildren().addAll(background, keyframeIndicator);

        updateVisualState();
        updateCursor();

        setupMouseHandling();
        setupContextMenu();
    }

    public String getFigureId() { return figureId; }
    public int getFrameNumber() { return frameNumber; }

    private void setupMouseHandling() {
        setOnMouseClicked(e -> {
            if (timelineController != null) {
                timelineController.handleFrameCellClicked(figureId, frameNumber, e.getButton());
            }
            e.consume();
        });

        setOnMouseEntered(e -> setHoverHighlight(true, false));
        setOnMouseExited(e -> setHoverHighlight(false, false));
    }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem addKeyframeItem = new MenuItem("Insert Keyframe on This Layer");
        addKeyframeItem.setOnAction(e -> {
            if (timelineController != null) {
                timelineController.handleFrameCellAddKeyframe(figureId, frameNumber);
            }
        });

        MenuItem removeKeyframeItem = new MenuItem("Remove Keyframe from This Layer");
        removeKeyframeItem.setOnAction(e -> {
            if (timelineController != null) {
                timelineController.handleFrameCellRemoveKeyframe(figureId, frameNumber);
            }
        });

        Menu createInterpolationMenu = new Menu("Create Interpolation...");
        MenuItem createLinearItem = new MenuItem("Simple Linear");
        createLinearItem.setOnAction(e -> {
            if (timelineController != null) {
                timelineController.handleFrameCellCreateInterpolation(figureId, frameNumber, EasingType.LINEAR);
            }
        });
        MenuItem createEaseInItem = new MenuItem("Ease In");
        createEaseInItem.setOnAction(e -> {
            if (timelineController != null) {
                timelineController.handleFrameCellCreateInterpolation(figureId, frameNumber, EasingType.EASE_IN_QUAD);
            }
        });
        MenuItem createEaseOutItem = new MenuItem("Ease Out");
        createEaseOutItem.setOnAction(e -> {
            if (timelineController != null) {
                timelineController.handleFrameCellCreateInterpolation(figureId, frameNumber, EasingType.EASE_OUT_QUAD);
            }
        });
        MenuItem createEaseInOutItem = new MenuItem("Ease In + Ease Out");
        createEaseInOutItem.setOnAction(e -> {
            if (timelineController != null) {
                timelineController.handleFrameCellCreateInterpolation(figureId, frameNumber, EasingType.EASE_IN_OUT_QUAD);
            }
        });
        createInterpolationMenu.getItems().addAll(createLinearItem, createEaseInItem, createEaseOutItem, createEaseInOutItem);

        MenuItem removeInterpolationItem = new MenuItem("Remove Interpolation from This Layer");
        removeInterpolationItem.setOnAction(e -> {
            if (timelineController != null) {
                timelineController.handleFrameCellRemoveInterpolation(figureId, frameNumber);
            }
        });

        contextMenu.getItems().addAll(
                addKeyframeItem, removeKeyframeItem, new SeparatorMenuItem(),
                createInterpolationMenu, removeInterpolationItem
        );

        setOnContextMenuRequested(e -> {
            boolean isKeyframe = timelineView.isKeyframe(figureId, frameNumber);
            Integer prevKey = timelineView.findPreviousKeyframe(figureId, frameNumber);
            Integer nextKey = timelineView.findNextKeyframe(figureId, frameNumber);
            boolean inSegment = timelineView.isFrameInInterpolatedSegment(figureId, frameNumber);

            addKeyframeItem.setDisable(isKeyframe);
            removeKeyframeItem.setDisable(!isKeyframe);
            createInterpolationMenu.setDisable(isKeyframe || prevKey == null || nextKey == null);
            removeInterpolationItem.setDisable(!inSegment && !isKeyframe);
            removeInterpolationItem.setDisable(!inSegment);


            contextMenu.show(this, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }

    public void setFrameType(FrameType type) {
        if (this.frameType != type) {
            this.frameType = type;
            updateVisualState();
            updateCursor();
            if (getOpacity() != 1.0) {
                setOpacity(1.0);
            }
        }
    }

    public void setHoverHighlight(boolean isHovering, boolean isValidDropTarget) {
        if (getOpacity() < 1.0 && !isValidDropTarget) return;

        if (isValidDropTarget) {
            background.setFill(Color.LIGHTGREEN);
        } else if (isHovering) {
            switch (frameType) {
                case KEYFRAME:     background.setFill(Color.rgb(90, 110, 165)); break;
                case INTERPOLATED: background.setFill(Color.rgb(70, 90, 130));  break;
                case EMPTY:        background.setFill(Color.rgb(70, 70, 70));   break;
            }
        } else {
            if (getOpacity() == 1.0) {
                updateVisualState();
            }
        }
    }

    private void updateVisualState() {
        switch (frameType) {
            case KEYFRAME:
                keyframeIndicator.setVisible(true);
                keyframeIndicator.setFill(Color.WHITE);
                background.setFill(Color.rgb(80, 100, 155));
                break;
            case INTERPOLATED:
                keyframeIndicator.setVisible(true);
                keyframeIndicator.setFill(Color.LIGHTGRAY);
                background.setFill(Color.rgb(60, 80, 120));
                break;
            case EMPTY:
            default:
                keyframeIndicator.setVisible(false);
                background.setFill(Color.rgb(50, 50, 50));
                break;
        }
    }

    private void updateCursor() {
        if (frameType == FrameType.KEYFRAME) {
            setCursor(Cursor.HAND);
        } else {
            setCursor(Cursor.DEFAULT);
        }
    }
}