package uk.ac.soton.comp3200.view.timeline;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.ac.soton.comp3200.model.animation.EasingType;
import uk.ac.soton.comp3200.model.animation.TimelineModel;
import uk.ac.soton.comp3200.controller.TimelineController;

import java.util.*;

public class TimelineView extends BorderPane {

    public static final Logger logger = LogManager.getLogger(TimelineView.class);

    public static final int FRAME_WIDTH = 22;
    public static final int FRAME_HEIGHT = 25;
    public static final int LAYER_LABEL_WIDTH = 120;
    public static final double FRAME_SPACING = 1.0;
    public static final double CELL_TOTAL_WIDTH = FRAME_WIDTH + FRAME_SPACING;
    private static final double FIXED_LAYER_AREA_HEIGHT = 130.0;

    private static final String NORMAL_LABEL_STYLE = "-fx-background-color: rgb(80, 80, 80);";
    private static final String SELECTED_LABEL_STYLE = "-fx-background-color: rgb(100, 120, 160); -fx-border-color: gold; -fx-border-width: 0 0 0 3;";
    private static final String NORMAL_FRAME_ROW_STYLE = "";
    private static final String SELECTED_FRAME_ROW_STYLE = "-fx-background-color: rgba(100, 120, 160, 0.2);";

    private final TimelineModel engine;
    private TimelineController timelineController;

    private final Map<String, HBox> figureFrameRows = new HashMap<>();
    private final Map<String, Label> figureLayerLabels = new HashMap<>();
    private final VBox layerLabelsColumn;
    private final VBox frameRowsColumn;
    private final HBox frameNumbersRow;
    private final StackPane scrollContentContainer;
    private final VBox rightSideContent;
    private final HBox scrollableContent;
    private final ScrollPane scrollPaneRef;

    private final Line playhead;
    private final Circle playheadHandle;

    private Label currentFrameLabel;
    private Button addKeyframeButton;
    private Button removeKeyframeButton;
    private Button playBtn;
    private Button stopBtn;

    private Rectangle framePreview;
    private Label framePreviewLabel;

    private String selectedLayerFigureId = null;
    private boolean needsRebuild = false;

    /**
     * Constructor. Sets up the layout and initial state.
     * @param engine The TimelineModel.
     */
    public TimelineView(TimelineModel engine) {
        this.engine = engine;

        HBox controls = createControlsBox();
        setTop(controls);

        layerLabelsColumn = new VBox(FRAME_SPACING);
        layerLabelsColumn.setPrefWidth(LAYER_LABEL_WIDTH);
        layerLabelsColumn.setMinWidth(LAYER_LABEL_WIDTH);
        layerLabelsColumn.setMaxWidth(LAYER_LABEL_WIDTH);
        layerLabelsColumn.setStyle("-fx-background-color: rgb(80, 80, 80);");

        Region labelHeaderSpacer = new Region();
        labelHeaderSpacer.setMinHeight(FRAME_HEIGHT);
        labelHeaderSpacer.setPrefHeight(FRAME_HEIGHT);
        layerLabelsColumn.getChildren().add(labelHeaderSpacer);

        frameNumbersRow = new HBox(FRAME_SPACING);
        frameNumbersRow.setStyle("-fx-background-color: rgb(80, 80, 80);");
        frameNumbersRow.setMinHeight(FRAME_HEIGHT);
        frameNumbersRow.setPrefHeight(FRAME_HEIGHT);
        frameNumbersRow.setAlignment(Pos.CENTER_LEFT);

        frameRowsColumn = new VBox(FRAME_SPACING);
        VBox.setVgrow(frameRowsColumn, Priority.ALWAYS);

        playhead = new Line(0, 0, 0, 0);
        playhead.setStroke(Color.RED);
        playhead.setStrokeWidth(2);
        playhead.setMouseTransparent(true);

        playheadHandle = new Circle(8);
        playheadHandle.setFill(Color.RED);

        scrollContentContainer = new StackPane();
        scrollContentContainer.getChildren().addAll(frameRowsColumn, playhead, playheadHandle);
        StackPane.setAlignment(frameRowsColumn, Pos.TOP_LEFT);
        StackPane.setAlignment(playhead, Pos.TOP_LEFT);
        StackPane.setAlignment(playheadHandle, Pos.TOP_LEFT);
        VBox.setVgrow(scrollContentContainer, Priority.ALWAYS);

        rightSideContent = new VBox(0);
        rightSideContent.getChildren().addAll(frameNumbersRow, scrollContentContainer);
        HBox.setHgrow(rightSideContent, Priority.ALWAYS);

        scrollableContent = new HBox(0);
        scrollableContent.getChildren().addAll(layerLabelsColumn, rightSideContent);
        scrollableContent.setAlignment(Pos.TOP_LEFT);

        scrollPaneRef = new ScrollPane();
        scrollPaneRef.setContent(scrollableContent);
        scrollPaneRef.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPaneRef.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPaneRef.setFitToHeight(true);
        scrollPaneRef.setFitToWidth(false);
        scrollPaneRef.setPrefHeight(FIXED_LAYER_AREA_HEIGHT + 20);
        scrollPaneRef.setMinHeight(FIXED_LAYER_AREA_HEIGHT + 20);
        scrollPaneRef.setMaxHeight(FIXED_LAYER_AREA_HEIGHT + 20);
        scrollPaneRef.setStyle("-fx-background: rgb(60, 60, 60); -fx-background-color: rgb(60, 60, 60);");
        ChangeListener<Number> scrollListener = (obs, oldVal, newVal) -> {
            if (timelineController != null && timelineController.isDraggingPlayhead()) {
                updateFramePreviewPosition();
            }
        };
        scrollPaneRef.hvalueProperty().addListener(scrollListener);

        setCenter(scrollPaneRef);

        Platform.runLater(() -> {
            adjustPlayheadHeight();
            updatePlayheadPosition();
        });

        setupFramePreview();
        setupPlayheadDragging();
        setupLayerAreaClickHandling();

        logger.info("TimelineView UI built with scrolling capabilities.");
    }

    public void setTimelineController(TimelineController timelineController) {
        this.timelineController = timelineController;
        updateUIFromEngine();
        Platform.runLater(() -> {
            if (scrollPaneRef != null) scrollPaneRef.setHvalue(0.0);
        });
    }

    /** Main update entry point called by the controller or engine listener. */
    public void updateUIFromEngine() {
        if (timelineController == null || engine == null) {
            logger.warn("TimelineView: updateUIFromEngine aborted, controller or engine is null.");
            return;
        }
        logger.trace("TimelineView: Updating Timeline UI from Engine State...");

        int uiFrames = frameNumbersRow.getChildren().size();
        int modelFrames = engine.getMaxFrames();
        if (uiFrames != modelFrames) {
            needsRebuild = true;
            logger.info("TimelineView: Frame count mismatch (UI: {}, Engine: {}), scheduling rebuild.", uiFrames, modelFrames);
        }

        if (needsRebuild) {
            rebuildFrameRowsAndHeader();
            needsRebuild = false;
        }

        currentFrameLabel.setText("Frame: " + engine.getCurrentFrame());
        updateKeyframeButtonStates();
        updatePlaybackButtonStates(engine.isPlaying());

        syncLayersWithEngine();

        updateAllFrameDisplays();

        updatePlayheadPosition();

        highlightLayer(selectedLayerFigureId);

        logger.trace("TimelineView: UI Update from Engine State Complete.");
    }

    /** Rebuilds frame numbers and all layer rows when maxFrames changes. */
    private void rebuildFrameRowsAndHeader() {
        if (engine == null) return;
        int maxFrames = engine.getMaxFrames();
        logger.debug("TimelineView: Rebuilding frame rows and header for {} frames.", maxFrames);

        double totalWidth = maxFrames * CELL_TOTAL_WIDTH;

        frameNumbersRow.getChildren().clear();
        for (int i = 1; i <= maxFrames; i++) {
            Label frameNumber = new Label(String.valueOf(i));
            frameNumber.setPrefWidth(FRAME_WIDTH);
            frameNumber.setMinWidth(FRAME_WIDTH);
            frameNumber.setMaxWidth(FRAME_WIDTH);
            frameNumber.setPrefHeight(FRAME_HEIGHT);
            frameNumber.setAlignment(Pos.CENTER);
            frameNumber.setTextFill(Color.WHITE);
            frameNumber.setPadding(new Insets(2));
            frameNumber.setStyle(i % 5 == 0 ? "-fx-background-color: rgb(100, 100, 100);" : "");
            Tooltip.install(frameNumber, new Tooltip("Frame " + i));
            frameNumbersRow.getChildren().add(frameNumber);
        }
        frameNumbersRow.setPrefWidth(totalWidth);
        logger.trace("TimelineView: Frame numbers row rebuilt. Total width: {}", totalWidth);

        List<String> figuresToRebuild = new ArrayList<>(figureFrameRows.keySet());
        for (String figId : figuresToRebuild) {
            HBox frameRow = figureFrameRows.get(figId);
            if (frameRow != null) {
                frameRow.getChildren().clear();
                for (int i = 1; i <= maxFrames; i++) {
                    FrameCell frameCell = new FrameCell(i, figId, this, this.timelineController);
                    setupFrameCellInteractions(frameCell);
                    frameRow.getChildren().add(frameCell);
                }
                frameRow.setPrefWidth(totalWidth);
                logger.trace("TimelineView: Rebuilt frame row for figure '{}'.", figId);
            }
        }

        adjustPlayheadHeight();
        updatePlayheadPosition();
    }

    /** Adds/Removes layer UI to match figures in the engine. */
    private void syncLayersWithEngine() {
        if (engine == null) return;
        Set<String> engineFigureIds = engine.getManagedFigureIds();
        Set<String> uiFigureIds = new HashSet<>(figureFrameRows.keySet());

        Set<String> toRemove = new HashSet<>(uiFigureIds);
        toRemove.removeAll(engineFigureIds);
        if (!toRemove.isEmpty()) {
            logger.debug("TimelineView: Removing layers for figures: {}", toRemove);
            toRemove.forEach(this::removeLayerUI);
        }

        Set<String> toAdd = new HashSet<>(engineFigureIds);
        toAdd.removeAll(uiFigureIds);
        if (!toAdd.isEmpty()) {
            logger.debug("TimelineView: Adding layers for figures: {}", toAdd);
            List<String> sortedToAdd = new ArrayList<>(toAdd);
            sortedToAdd.sort(Comparator.comparingInt(id -> {
                try {
                    return Integer.parseInt(id.replaceAll("\\D+", ""));
                } catch (NumberFormatException e) {
                    return Integer.MAX_VALUE;
                }
            }));
            sortedToAdd.forEach(this::addLayerUI);
        }

        if (!toAdd.isEmpty() || !toRemove.isEmpty()) {
            adjustPlayheadHeight();
        }
    }

    /** Adjusts the playhead line's height based on the frameRowsColumn height. */
    private void adjustPlayheadHeight() {
        Platform.runLater(()-> {
            double frameRowsHeight = frameRowsColumn.getBoundsInParent().getHeight();
            if (frameRowsHeight <= 0) {
                frameRowsHeight = FIXED_LAYER_AREA_HEIGHT;
                logger.trace("TimelineView: Using fallback height for playhead.");
            }

            playhead.setStartY(0);
            playhead.setEndY(frameRowsHeight + 5);
            playheadHandle.setTranslateY(-playheadHandle.getRadius());
            logger.trace("TimelineView: Adjusted playhead height to: {}", playhead.getEndY());
        });
    }

    /** Updates the playhead's horizontal position based on the current frame. */
    public void updatePlayheadPosition() {
        if (timelineController != null && timelineController.isDraggingPlayhead()) return;
        if (engine == null) return;

        double frameCenterX = (engine.getCurrentFrame() - 1) * CELL_TOTAL_WIDTH + (FRAME_WIDTH / 2.0);
        double finalXPosition = Math.round(frameCenterX);

        playhead.setTranslateX(finalXPosition);
        playheadHandle.setTranslateX(finalXPosition);

        logger.trace("TimelineView: Playhead positioned at X={} for frame {}", finalXPosition, engine.getCurrentFrame());
        ensurePlayheadVisible();
    }

    /** Temporarily moves the visual playhead during drag. */
    public void movePlayheadTemporarilyDuringDrag(double xPositionInFrameArea) {
        double clampedX = Math.max(0, Math.min(frameRowsColumn.getWidth() - 1, xPositionInFrameArea));
        double finalXPosition = Math.round(clampedX);
        playhead.setTranslateX(finalXPosition);
        playheadHandle.setTranslateX(finalXPosition);
    }

    /** Updates the visual state (Empty, Keyframe, Interpolated) of all frame cells. */
    private void updateAllFrameDisplays() {
        if (engine == null) return;
        logger.trace("TimelineView: Updating all frame cell displays.");
        int maxFrames = engine.getMaxFrames();

        for (Map.Entry<String, HBox> entry : figureFrameRows.entrySet()) {
            String figId = entry.getKey();
            HBox row = entry.getValue();
            if (row != null) {
                if (row.getChildren().size() != maxFrames) {
                    logger.warn("TimelineView: Mismatch detected in frame cell count for '{}' during update. Forcing rebuild flag.", figId);
                    needsRebuild = true;
                    continue;
                }
                for (Node node : row.getChildren()) {
                    if (node instanceof FrameCell) {
                        FrameCell cell = (FrameCell) node;
                        FrameType type = engine.getFrameType(figId, cell.getFrameNumber());
                        cell.setFrameType(type);
                    }
                }
            }
        }
    }

    /** Updates Add/Remove Keyframe button states. */
    private void updateKeyframeButtonStates() {
        if (engine == null || addKeyframeButton == null || removeKeyframeButton == null) return;
        boolean canAddAny = false;
        boolean canRemoveAny = false;
        int currentFrame = engine.getCurrentFrame();
        Set<String> figures = engine.getManagedFigureIds();

        if (!figures.isEmpty()) {
            String targetFigureId = selectedLayerFigureId != null ? selectedLayerFigureId : figures.iterator().next();
            boolean figureHasKeyframe = (engine.getKeyframePoints(targetFigureId, currentFrame) != null);
            if (figureHasKeyframe) {
                canRemoveAny = true;
            } else {
                canAddAny = true;
            }
        }

        addKeyframeButton.setDisable(figures.isEmpty() || !canAddAny);
        removeKeyframeButton.setDisable(figures.isEmpty() || !canRemoveAny);
    }

    /** Updates Play/Stop button states. */
    public void updatePlaybackButtonStates(boolean isPlaying) {
        if (playBtn != null) playBtn.setDisable(isPlaying);
        if (stopBtn != null) stopBtn.setDisable(!isPlaying);
        logger.trace("TimelineView: Playback buttons updated (isPlaying: {})", isPlaying);
    }

    /** Visually highlights the selected layer row and label. */
    public void highlightLayer(String figureId) {
        if (!Objects.equals(figureId, selectedLayerFigureId)) {
            selectedLayerFigureId = figureId;
            logger.debug("TimelineView: Highlighting layer set to: {}", selectedLayerFigureId);
            updateKeyframeButtonStates();
        }

        for (Map.Entry<String, Label> entry : figureLayerLabels.entrySet()) {
            String currentFigureId = entry.getKey();
            Label layerLabel = entry.getValue();
            HBox frameRow = figureFrameRows.get(currentFigureId);
            if (layerLabel == null || frameRow == null) continue;
            boolean isSelected = currentFigureId.equals(this.selectedLayerFigureId);
            layerLabel.setStyle(isSelected ? SELECTED_LABEL_STYLE : NORMAL_LABEL_STYLE);
            frameRow.setStyle(isSelected ? SELECTED_FRAME_ROW_STYLE : NORMAL_FRAME_ROW_STYLE);
        }
    }

    /** Creates and adds UI elements for a new layer. */
    private void addLayerUI(String figureId) {
        if (figureLayerLabels.containsKey(figureId) || engine == null) return;
        logger.debug("TimelineView: addLayerUI for figure ID: {}", figureId);

        Label layerLabel = new Label(figureId);
        layerLabel.setTextFill(Color.WHITE);
        layerLabel.setPrefWidth(LAYER_LABEL_WIDTH);
        layerLabel.setMinWidth(LAYER_LABEL_WIDTH);
        layerLabel.setMaxWidth(LAYER_LABEL_WIDTH);
        layerLabel.setMinHeight(FRAME_HEIGHT);
        layerLabel.setPrefHeight(FRAME_HEIGHT);
        layerLabel.setPadding(new Insets(5));
        layerLabel.setAlignment(Pos.CENTER_LEFT);
        layerLabel.setStyle(NORMAL_LABEL_STYLE);
        Tooltip.install(layerLabel, new Tooltip("Layer: " + figureId));
        layerLabel.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && timelineController != null) {
                timelineController.handleLayerSelected(figureId);
                event.consume();
            }
        });
        ContextMenu layerContextMenu = createLayerContextMenu(figureId);
        layerLabel.setContextMenu(layerContextMenu);
        layerLabel.setOnContextMenuRequested(event -> {
            layerContextMenu.show(layerLabel, event.getScreenX(), event.getScreenY());
            event.consume();
        });

        HBox frameRow = new HBox(FRAME_SPACING);
        frameRow.setStyle(NORMAL_FRAME_ROW_STYLE);
        frameRow.setAlignment(Pos.CENTER_LEFT);
        int maxFrames = engine.getMaxFrames();
        double totalWidth = maxFrames * CELL_TOTAL_WIDTH;
        frameRow.setPrefWidth(totalWidth);

        for (int i = 1; i <= maxFrames; i++) {
            FrameCell frameCell = new FrameCell(i, figureId, this, this.timelineController);
            setupFrameCellInteractions(frameCell);
            frameRow.getChildren().add(frameCell);
        }
        frameRow.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getTarget() == frameRow && timelineController != null) {
                timelineController.handleLayerSelected(figureId);
                event.consume();
            }
        });
        frameRow.setOnContextMenuRequested(event -> {
            if (event.getTarget() == frameRow) {
                layerContextMenu.show(frameRow, event.getScreenX(), event.getScreenY());
                event.consume();
            }
        });

        layerLabelsColumn.getChildren().add(layerLabel);
        frameRowsColumn.getChildren().add(frameRow);

        figureLayerLabels.put(figureId, layerLabel);
        figureFrameRows.put(figureId, frameRow);
        logger.debug("TimelineView: Added layer UI for '{}' with {} frames.", figureId, maxFrames);
    }

    /** Removes UI elements for a specific layer. */
    private void removeLayerUI(String figureId) {
        logger.debug("TimelineView: Removing layer UI for figure '{}'.", figureId);
        HBox frameRow = figureFrameRows.remove(figureId);
        Label layerLabel = figureLayerLabels.remove(figureId);
        if (frameRow != null) frameRowsColumn.getChildren().remove(frameRow);
        if (layerLabel != null) layerLabelsColumn.getChildren().remove(layerLabel);
        if (figureId.equals(selectedLayerFigureId)) {
            selectedLayerFigureId = null;
            updateKeyframeButtonStates();
        }
    }

    /** Sets up interactions for a single FrameCell. */
    private void setupFrameCellInteractions(FrameCell cell) {
        cell.setOnMouseClicked(e -> {
            if (timelineController != null) {
                timelineController.handleFrameCellClicked(cell.getFigureId(), cell.getFrameNumber(), e.getButton());
            }
            e.consume();
        });
        setupContextMenuHandling(cell);
        setupDragAndDropHandling(cell);
    }

    /** Creates the context menu for a FrameCell. */
    private void setupContextMenuHandling(FrameCell cell) {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem addKeyframeItem = new MenuItem("Insert Keyframe Here");
        addKeyframeItem.setOnAction(e -> {
            if(timelineController != null) timelineController.handleFrameCellAddKeyframe(cell.getFigureId(), cell.getFrameNumber());
        });

        MenuItem removeKeyframeItem = new MenuItem("Remove Keyframe Here");
        removeKeyframeItem.setOnAction(e -> {
            if(timelineController != null) timelineController.handleFrameCellRemoveKeyframe(cell.getFigureId(), cell.getFrameNumber());
        });

        Menu createInterpolationMenu = new Menu("Create Interpolation...");
        for (EasingType type : EasingType.values()) {
            MenuItem easeItem = new MenuItem(type.toString().replace("_", " ").toLowerCase());
            easeItem.setOnAction(e -> {
                if (timelineController != null)
                    timelineController.handleFrameCellCreateInterpolation(cell.getFigureId(), cell.getFrameNumber(), type);
            });
            createInterpolationMenu.getItems().add(easeItem);
        }

        MenuItem removeInterpolationItem = new MenuItem("Remove Interpolation Here");
        removeInterpolationItem.setOnAction(e -> {
            if(timelineController != null) timelineController.handleFrameCellRemoveInterpolation(cell.getFigureId(), cell.getFrameNumber());
        });

        contextMenu.getItems().addAll(
                addKeyframeItem, removeKeyframeItem, new SeparatorMenuItem(),
                createInterpolationMenu, removeInterpolationItem
        );

        cell.setOnContextMenuRequested(e -> {
            if (engine == null || timelineController == null) return;
            String figureId = cell.getFigureId();
            int frameNumber = cell.getFrameNumber();
            boolean isKeyframe = isKeyframe(figureId, frameNumber);
            Integer prevKey = findPreviousKeyframe(figureId, frameNumber);
            Integer nextKey = findNextKeyframe(figureId, frameNumber);
            boolean inSegment = isFrameInInterpolatedSegment(figureId, frameNumber);

            addKeyframeItem.setDisable(isKeyframe);
            removeKeyframeItem.setDisable(!isKeyframe);
            createInterpolationMenu.setDisable(isKeyframe || prevKey == null || nextKey == null);
            removeInterpolationItem.setDisable(!inSegment);
            contextMenu.show(cell, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }

    /** Creates the context menu for a layer label/row. */
    private ContextMenu createLayerContextMenu(String figureId) {
        ContextMenu menu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Delete Figure '" + figureId + "'");
        deleteItem.setOnAction(e -> {
            if (timelineController != null)
                timelineController.handleLayerContextMenuDeleteFigure(figureId);
        });
        menu.getItems().add(deleteItem);
        return menu;
    }

    /** Creates the top controls HBox. */
    private HBox createControlsBox() {
        addKeyframeButton = new Button("Add Keyframe");
        addKeyframeButton.setOnAction(e -> { if(timelineController != null) timelineController.handleAddKeyframeClicked(); });
        Tooltip.install(addKeyframeButton, new Tooltip("Add a keyframe at the current frame for the selected layer"));


        removeKeyframeButton = new Button("Remove Keyframe");
        removeKeyframeButton.setOnAction(e -> { if(timelineController != null) timelineController.handleRemoveKeyframeClicked(); });
        Tooltip.install(removeKeyframeButton, new Tooltip("Remove the keyframe at the current frame for the selected layer"));


        playBtn = new Button("▶");
        playBtn.setOnAction(e -> { if(timelineController != null) timelineController.handlePlayClicked(); });
        Tooltip.install(playBtn, new Tooltip("Play Animation"));


        stopBtn = new Button("■");
        stopBtn.setOnAction(e -> { if(timelineController != null) timelineController.handleStopClicked(); });
        stopBtn.setDisable(true);
        Tooltip.install(stopBtn, new Tooltip("Stop Animation"));


        Button firstBtn = new Button("⏮");
        firstBtn.setOnAction(e -> { if(timelineController != null) timelineController.handleGoToFirstFrameClicked(); });
        Tooltip.install(firstBtn, new Tooltip("Go to First Frame"));


        currentFrameLabel = new Label("Frame: 1");
        currentFrameLabel.setTextFill(Color.WHITE);
        currentFrameLabel.setPadding(new Insets(0, 5, 0, 5));

        HBox controls = new HBox(10,
                addKeyframeButton, removeKeyframeButton,
                new Separator(Orientation.VERTICAL),
                firstBtn, playBtn, stopBtn,
                new Separator(Orientation.VERTICAL),
                currentFrameLabel
        );
        controls.setPadding(new Insets(8));
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setStyle("-fx-background-color: rgb(40, 40, 40);");
        return controls;
    }

    /** Sets up the frame preview tooltip elements. */
    private void setupFramePreview() {
        framePreview = new Rectangle(60, 25);
        framePreview.setFill(Color.rgb(30, 30, 30, 0.85));
        framePreview.setStroke(Color.LIGHTGRAY);
        framePreview.setArcWidth(8); framePreview.setArcHeight(8);
        framePreview.setMouseTransparent(true);
        framePreview.setVisible(false);

        framePreviewLabel = new Label("Frame: 1");
        framePreviewLabel.setTextFill(Color.WHITE);
        framePreviewLabel.setMouseTransparent(true);
        framePreviewLabel.setVisible(false);
        framePreviewLabel.setStyle("-fx-font-size: 10px;");

        this.getChildren().addAll(framePreview, framePreviewLabel);
    }

    /** Shows the frame preview tooltip. */
    public void showFramePreview(int frame) {
        if (!framePreview.isVisible()) {
            updateFramePreviewPosition();
            framePreviewLabel.setText("Frame: " + frame);
            framePreview.setVisible(true);
            framePreviewLabel.setVisible(true);
            logger.trace("TimelineView: Showing frame preview for frame {}", frame);
        }
    }

    /** Hides the frame preview tooltip. */
    public void hideFramePreview() {
        if (framePreview.isVisible()) {
            framePreview.setVisible(false);
            framePreviewLabel.setVisible(false);
            logger.trace("TimelineView: Hiding frame preview.");
        }
    }

    /** Updates the text in the frame preview. */
    public void updateFramePreviewText(int frame) {
        if (framePreviewLabel.isVisible()) {
            framePreviewLabel.setText("Frame: " + frame);
        }
    }

    /** Updates the position of the frame preview relative to the visible playhead. */
    private void updateFramePreviewPosition() {
        if (!framePreview.isVisible() || playhead == null || scrollPaneRef == null || rightSideContent == null) return;

        double playheadXInContent = playhead.getTranslateX();

        double scrollPaneWidth = scrollPaneRef.getViewportBounds().getWidth() - LAYER_LABEL_WIDTH;
        double contentWidth = rightSideContent.getBoundsInLocal().getWidth();
        double scrollMax = Math.max(0, contentWidth - scrollPaneWidth);
        double scrollOffset = scrollPaneRef.getHvalue() * scrollMax;

        double playheadVisibleX = LAYER_LABEL_WIDTH + playheadXInContent - scrollOffset;

        double previewX = playheadVisibleX - framePreview.getWidth() / 2.0;
        double previewY = scrollPaneRef.getLayoutY() - framePreview.getHeight() - 5;

        double maxX = this.getWidth() - framePreview.getWidth();
        previewX = Math.max(LAYER_LABEL_WIDTH, Math.min(maxX, previewX));
        previewY = Math.max(0, previewY);

        framePreview.setTranslateX(previewX);
        framePreview.setTranslateY(previewY);
        framePreviewLabel.setTranslateX(previewX + 5);
        double labelHeight = framePreviewLabel.prefHeight(-1);
        framePreviewLabel.setTranslateY(previewY + (framePreview.getHeight() - labelHeight) / 2.0);
    }

    /** Scrolls the timeline horizontally to ensure the playhead is visible. */
    public void ensurePlayheadVisible() {
        if (scrollPaneRef == null || playhead == null || engine == null || frameRowsColumn.getWidth() <= 0) return;

        Platform.runLater(() -> {
            Bounds viewportBounds = scrollPaneRef.getViewportBounds();
            if (viewportBounds == null || viewportBounds.getWidth() <= 0) return;

            double viewportWidth = viewportBounds.getWidth();
            double playheadX = playhead.getTranslateX();
            double contentWidth = frameRowsColumn.getBoundsInLocal().getWidth();

            if (contentWidth <= viewportWidth) return;

            double hValue = scrollPaneRef.getHvalue();
            double visibleMinX = hValue * (contentWidth - viewportWidth);
            double visibleMaxX = visibleMinX + viewportWidth;
            double buffer = 50.0;

            double targetHValue = hValue;
            if (playheadX < visibleMinX + buffer) {
                targetHValue = (playheadX - buffer) / (contentWidth - viewportWidth);
            } else if (playheadX > visibleMaxX - buffer) {
                targetHValue = (playheadX - viewportWidth + buffer) / (contentWidth - viewportWidth);
            }

            targetHValue = Math.max(0.0, Math.min(1.0, targetHValue));
            if (Math.abs(targetHValue - hValue) > 1e-6 && scrollPaneRef.getScene() != null) {
                scrollPaneRef.setHvalue(targetHValue);
                logger.trace("TimelineView: Scrolled to HValue={} to keep playhead visible.", targetHValue);
            }
        });
    }

    /** Sets up dragging behavior for the playhead handle. */
    private void setupPlayheadDragging() {
        playheadHandle.setOnMousePressed(event -> {
            if (timelineController != null) {
                logger.trace("Playhead drag START detected on handle");
                timelineController.handlePlayheadDragStarted(event);
            }
            event.consume();
        });

        EventHandler<MouseEvent> dragHandler = event -> {
            if (timelineController != null && timelineController.isDraggingPlayhead()) {
                Point2D mouseLocal;
                try {
                    mouseLocal = frameRowsColumn.sceneToLocal(event.getSceneX(), event.getSceneY());
                } catch (Exception e) {
                    logger.error("TimelineView: Error during sceneToLocal in drag handler.", e); return;
                }

                if (mouseLocal != null) {
                    timelineController.handlePlayheadDragged(mouseLocal.getX());
                    updateFramePreviewPosition();
                }
            }
            event.consume();
        };

        playheadHandle.setOnMouseDragged(dragHandler);
        scrollContentContainer.setOnMouseDragged(dragHandler);

        EventHandler<MouseEvent> releaseHandler = event -> {
            if (timelineController != null && timelineController.isDraggingPlayhead()) {
                logger.trace("Playhead drag END detected");
                timelineController.handlePlayheadDragEnded();
            }
        };

        Platform.runLater(() -> {
            Scene scene = getScene();
            if (scene != null) {
                scene.addEventFilter(MouseEvent.MOUSE_RELEASED, releaseHandler);
            } else {
                this.addEventFilter(MouseEvent.MOUSE_RELEASED, releaseHandler);
                logger.warn("TimelineView: Scene was null when adding global mouse release handler.");
            }
        });
    }

    /** Sets up click handling for background areas and the frame number row. */
    private void setupLayerAreaClickHandling() {
        frameNumbersRow.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && timelineController != null && engine != null) {
                int frame = calculateFrameFromPosition(e.getX());
                logger.trace("TimelineView: Frame number row clicked at frame {}", frame);
                engine.goToFrame(frame);
                e.consume();
            }
        });

        scrollableContent.setOnMouseClicked(e -> {
            Node target = (Node) e.getTarget();
            if (e.getButton() == MouseButton.PRIMARY && timelineController != null &&
                    (target == scrollableContent || target == layerLabelsColumn || target == rightSideContent || target == scrollContentContainer) )
            {
                logger.trace("TimelineView: Click on scrollable background area.");
                timelineController.handleTimelineBackgroundClicked();
                e.consume();
            }
        });
    }

    /** Calculates frame number from horizontal position within frameRowsColumn. */
    public int calculateFrameFromPosition(double xPositionInFrameArea) {
        if (engine == null) return 1;
        int frameIndex = (int) Math.floor((xPositionInFrameArea + (CELL_TOTAL_WIDTH / 2.0)) / CELL_TOTAL_WIDTH);
        return Math.max(1, Math.min(engine.getMaxFrames(), frameIndex + 1));
    }

    public boolean isKeyframe(String f, int n) { return engine != null && engine.getFrameType(f, n) == FrameType.KEYFRAME; }
    public boolean isFrameInInterpolatedSegment(String f, int n) { return engine != null && engine.findSegmentContainingFrame(f, n) != null; }
    public Integer findPreviousKeyframe(String f, int n) { return engine != null ? engine.findPreviousKeyframe(f, n) : null; }
    public Integer findNextKeyframe(String f, int n) { return engine != null ? engine.findNextKeyframe(f, n) : null; }

    public void setupDragAndDropHandling(FrameCell cell) {
        cell.setOnDragDetected(event -> {
            if (timelineController != null) timelineController.handleKeyframeDragDetected(cell, event);
            event.consume();
        });
        cell.setOnDragOver(event -> {
            if (timelineController != null) timelineController.handleKeyframeDragOver(cell, event);
            event.consume();
        });
        cell.setOnDragExited(event -> {
            if (timelineController != null) timelineController.handleKeyframeDragExited(cell, event);
            event.consume();
        });
        cell.setOnDragDropped(event -> {
            if (timelineController != null) timelineController.handleKeyframeDragDropped(cell, event);
            event.consume();
        });
        cell.setOnDragDone(event -> {
            if (timelineController != null) timelineController.handleKeyframeDragDone(cell, event);
            event.consume();
        });
    }

    public void setPlayheadDraggingVisualState(boolean i) { playheadHandle.setFill(i ? Color.ORANGE : Color.RED); playheadHandle.setRadius(i ? 10 : 8); }
    public void setCellDragState(FrameCell c, boolean i) { c.setOpacity(i ? 0.6 : 1.0); }
    public void setCellDropTargetState(FrameCell c, boolean i) { c.setHoverHighlight(true, i); }
}