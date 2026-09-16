package uk.ac.soton.comp3200.view.scene;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.ac.soton.comp3200.model.figure.model.PointData;
import uk.ac.soton.comp3200.model.figure.model.Segment;
import uk.ac.soton.comp3200.model.figure.model.StickFigureModel;
import uk.ac.soton.comp3200.model.math.Point;
import uk.ac.soton.comp3200.model.math.Utility;

import uk.ac.soton.comp3200.view.window.GameWindow;
import uk.ac.soton.comp3200.view.common.ToolMode;
import uk.ac.soton.comp3200.controller.CanvasController;
import uk.ac.soton.comp3200.controller.MenuController;
import uk.ac.soton.comp3200.controller.TimelineController;
import uk.ac.soton.comp3200.view.timeline.TimelineView;

import java.util.*;

public class MenuView extends BaseScene {

    private static final Logger logger = LogManager.getLogger(MenuView.class);

    private MenuController controller;

    private Canvas canvas;
    private TimelineView timeline;
    private ToggleGroup toolToggleGroup;
    private ToggleButton handToolButton;
    private ToggleButton moveToolButton;
    private CheckBox onionSkinCheckbox;
    private Spinner<Integer> pastSkinsSpinner;
    private Spinner<Integer> futureSkinsSpinner;
    private ContextMenu canvasContextMenu;
    private MenuItem deleteFigureMenuItem;
    private Label statusLabel;

    private boolean hideControls = false;

    private static final double LEFT_TOOLBAR_WIDTH = 120;
    private static final double FIXED_CANVAS_WIDTH = 800.0;
    private static final double FIXED_CANVAS_HEIGHT = 600.0;
    private static final Color DESELECTED_POINT_COLOR = Color.GRAY.deriveColor(0, 1.0, 1.0, 0.6);
    private static final Color DESELECTED_EFFECTOR_COLOR = Color.SLATEGRAY.deriveColor(0, 1.0, 1.0, 0.7);
    private static final Color DESELECTED_OUTLINE_COLOR = Color.DARKGRAY.deriveColor(0, 1.0, 1.0, 0.5);

    public MenuView(GameWindow gameWindow) {
        super(gameWindow);
        logger.info("Creating Menu View");
    }

    public void setController(MenuController controller) {
        this.controller = controller;
    }

    @Override
    public void build() {
        logger.info("Building MenuView UI");

        BorderPane mainPane = new BorderPane();
        this.root = mainPane;
        mainPane.setStyle("-fx-background-color: #333333;");

        timeline = new TimelineView(controller.getEngine());

        MenuBar menuBar = createMenuBar();
        VBox topVBox = new VBox(menuBar, timeline);
        mainPane.setTop(topVBox);

        VBox leftToolbar = buildLeftToolbar();
        mainPane.setLeft(leftToolbar);

        StackPane centeringPane = buildCenterCanvas();
        mainPane.setCenter(centeringPane);

        statusLabel = new Label("Ready.");
        statusLabel.setTextFill(Color.LIGHTGRAY);
        statusLabel.setPadding(new Insets(5));
        HBox statusBar = new HBox(statusLabel);
        statusBar.setStyle("-fx-background-color: #4f4f4f;");
        mainPane.setBottom(statusBar);

        CanvasController canvasController = controller.getCanvasController();
        TimelineController timelineController = controller.getTimelineController();

        if (canvasController != null && canvas != null) {
            canvasController.setView(this);
            canvasController.setCanvas(canvas);
            logger.debug("MenuView.build: CanvasController wired up.");
        } else {
            logger.error("MenuView.build: Failed to wire CanvasController (Controller or Canvas is null).");
        }

        if (timelineController != null && timeline != null) {
            timeline.setTimelineController(timelineController);
            logger.debug("MenuView.build: TimelineController wired up to TimelineView.");
        } else {
            logger.error("MenuView.build: Failed to wire TimelineController (Controller or View is null).");
        }

        setupCanvasContextMenu();

        Platform.runLater(() -> {
            if (controller != null) {
                logger.debug("MenuView.build: Triggering controller.initializeViewState().");
                controller.initializeViewState();
            } else {
                logger.error("MenuView.build: Cannot initialize view state, controller is null.");
            }
        });

        logger.info("MenuView build complete.");
    }

    private VBox buildLeftToolbar() {
        VBox leftToolbar = new VBox();
        leftToolbar.setPadding(new Insets(15));
        leftToolbar.setSpacing(10);
        leftToolbar.setPrefWidth(LEFT_TOOLBAR_WIDTH);
        leftToolbar.setStyle("-fx-background-color: #4f4f4f;");
        leftToolbar.setAlignment(Pos.TOP_CENTER);

        toolToggleGroup = new ToggleGroup();
        handToolButton = new ToggleButton("Hand");
        handToolButton.setToggleGroup(toolToggleGroup);
        handToolButton.setSelected(true);
        handToolButton.setMaxWidth(Double.MAX_VALUE);
        handToolButton.setOnAction(e -> { if(controller != null) controller.handleToolModeChange(ToolMode.HAND); });

        moveToolButton = new ToggleButton("Move");
        moveToolButton.setToggleGroup(toolToggleGroup);
        moveToolButton.setMaxWidth(Double.MAX_VALUE);
        moveToolButton.setOnAction(e -> { if(controller != null) controller.handleToolModeChange(ToolMode.MOVE); });

        Button addFigureButton = new Button("Add Figure");
        addFigureButton.setMaxWidth(Double.MAX_VALUE);
        addFigureButton.setOnAction(e -> { if(controller != null) controller.handleAddFigureClicked(); });

        Label onionSkinLabel = new Label("Onion Skinning");
        onionSkinLabel.setTextFill(Color.WHITE);
        onionSkinLabel.setMaxWidth(Double.MAX_VALUE);
        onionSkinLabel.setAlignment(Pos.CENTER);

        onionSkinCheckbox = new CheckBox("Enable");
        onionSkinCheckbox.setTextFill(Color.WHITE);
        onionSkinCheckbox.setContentDisplay(ContentDisplay.RIGHT);
        onionSkinCheckbox.setOnAction(e -> { if(controller != null) controller.handleOnionSkinEnableChange(onionSkinCheckbox.isSelected()); });

        HBox checkBoxContainer = new HBox(onionSkinCheckbox);
        checkBoxContainer.setAlignment(Pos.CENTER);

        Label pastFramesLabel = new Label("Past Frames:");
        pastFramesLabel.setTextFill(Color.LIGHTGRAY);
        pastFramesLabel.setMaxWidth(Double.MAX_VALUE);
        pastFramesLabel.setAlignment(Pos.CENTER);

        pastSkinsSpinner = new Spinner<>(0, 10, 2);
        pastSkinsSpinner.setMaxWidth(Double.MAX_VALUE);
        pastSkinsSpinner.valueProperty().addListener((obs, oldVal, newVal) -> { if(controller != null) controller.handlePastSkinsChange(newVal); });

        VBox pastBox = new VBox(3, pastFramesLabel, pastSkinsSpinner);
        pastBox.setAlignment(Pos.CENTER);

        Label futureFramesLabel = new Label("Future Frames:");
        futureFramesLabel.setTextFill(Color.LIGHTGRAY);
        futureFramesLabel.setMaxWidth(Double.MAX_VALUE);
        futureFramesLabel.setAlignment(Pos.CENTER);

        futureSkinsSpinner = new Spinner<>(0, 10, 1);
        futureSkinsSpinner.setMaxWidth(Double.MAX_VALUE);
        futureSkinsSpinner.valueProperty().addListener((obs, oldVal, newVal) -> { if(controller != null) controller.handleFutureSkinsChange(newVal); });

        VBox futureBox = new VBox(3, futureFramesLabel, futureSkinsSpinner);
        futureBox.setAlignment(Pos.CENTER);

        leftToolbar.getChildren().addAll(
                handToolButton,
                moveToolButton,
                new Separator(),
                addFigureButton,
                new Separator(),
                onionSkinLabel,
                checkBoxContainer,
                pastBox,
                futureBox
        );

        return leftToolbar;
    }

    private StackPane buildCenterCanvas() {
        StackPane centeringPane = new StackPane();
        centeringPane.setStyle("-fx-background-color: #cccccc;");
        Pane canvasContainer = new Pane();
        canvasContainer.setStyle("-fx-border-color: black; -fx-border-width: 1;");
        canvasContainer.setPrefSize(FIXED_CANVAS_WIDTH, FIXED_CANVAS_HEIGHT);
        canvasContainer.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        canvasContainer.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        canvas = new Canvas();
        canvas.widthProperty().bind(canvasContainer.widthProperty());
        canvas.heightProperty().bind(canvasContainer.heightProperty());
        canvasContainer.getChildren().add(canvas);
        centeringPane.getChildren().add(canvasContainer);
        StackPane.setAlignment(canvasContainer, Pos.CENTER);
        return centeringPane;
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");

        MenuItem newItem = new MenuItem("New");
        newItem.setOnAction(event -> { if(controller != null) controller.handleNewAnimation(); });
        MenuItem saveItem = new MenuItem("Save");
        saveItem.setOnAction(event -> { if(controller != null) controller.handleSaveAnimation(); });
        MenuItem saveAsItem = new MenuItem("Save As...");
        saveAsItem.setOnAction(event -> { if(controller != null) controller.handleSaveAnimationAs(); });
        MenuItem loadItem = new MenuItem("Open...");
        loadItem.setOnAction(event -> { if(controller != null) controller.handleLoadAnimation(); });
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(event -> { if(controller != null) controller.handleExit(); });

        Menu exportMenu = new Menu("Export");
        Menu exportPngMenu = new Menu("PNG");
        MenuItem exportPngSingle = new MenuItem("Current Frame...");
        exportPngSingle.setOnAction(event -> { if(controller != null) controller.handleExportPng(); });
        MenuItem exportPngSeq = new MenuItem("Image Sequence...");
        exportPngSeq.setOnAction(event -> { if(controller != null) controller.handleExportPngSequence(); });
        exportPngMenu.getItems().addAll(exportPngSingle, exportPngSeq);
        MenuItem exportGif = new MenuItem("GIF Animation...");
        exportGif.setOnAction(event -> { if(controller != null) controller.handleExportGif(); });
        MenuItem exportMp4 = new MenuItem("MP4 Video...");
        exportMp4.setOnAction(event -> { if(controller != null) controller.handleExportMp4(); });
        exportMenu.getItems().addAll(exportPngMenu, exportGif, exportMp4);

        fileMenu.getItems().addAll(newItem,loadItem, saveItem, saveAsItem , new SeparatorMenuItem(), exportMenu, new SeparatorMenuItem(), exitItem);

        Menu editMenu = new Menu("Edit");
        MenuItem settingsItem = new MenuItem("Project Settings...");
        settingsItem.setOnAction(event -> { if(controller != null) controller.handleEditProjectSettings(); });
        editMenu.getItems().addAll(settingsItem);
        menuBar.getMenus().addAll(fileMenu, editMenu);

        return menuBar;
    }

    private void setupCanvasContextMenu() {
        canvasContextMenu = new ContextMenu();
        deleteFigureMenuItem = new MenuItem("Delete Figure");
        canvasContextMenu.getItems().add(deleteFigureMenuItem);
    }

    public void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(gameWindow.getStage());
        alert.showAndWait();
    }

    /** Called by Controller to trigger a redraw of the canvas */
    public void redrawCanvas(List<StickFigureModel> figuresToDraw,
                             List<FrameData> pastFrames, List<FrameData> futureFrames) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> redrawCanvas(figuresToDraw, pastFrames, futureFrames));
            return;
        }
        logger.trace("View Redrawing Canvas...");

        final Color pastSkinColorTint = Color.RED.deriveColor(0, 1.0, 1.0, 0.5);
        final Color futureSkinColorTint = Color.GREEN.deriveColor(0, 1.0, 1.0, 0.5);

        if (canvas == null) return;
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (controller.isOnionSkinEnabled()) {
            if (pastFrames != null) {
                gc.setGlobalAlpha(0.18);
                for (FrameData frameData : pastFrames) {
                    drawKeyframeState(gc, frameData.figurePoses, pastSkinColorTint);
                }
            }
            if (futureFrames != null) {
                gc.setGlobalAlpha(0.15);
                for (FrameData frameData : futureFrames) {
                    drawKeyframeState(gc, frameData.figurePoses, futureSkinColorTint);
                }
            }
            gc.setGlobalAlpha(1.0);
        }

        if (figuresToDraw != null) {
            for (StickFigureModel figure : figuresToDraw) {
                drawSingleFigureState(gc, figure, null, 1.0, null, true);
            }
        }
    }

    public void setControlsVisibility(boolean visible) {
        this.hideControls = !visible;
        if (controller != null) controller.requestCanvasRedraw();
    }
    public void updateOnionSkinState(boolean enabled, int pastCount, int futureCount) {
        onionSkinCheckbox.setSelected(enabled);
        pastSkinsSpinner.getValueFactory().setValue(pastCount);
        futureSkinsSpinner.getValueFactory().setValue(futureCount);
        pastSkinsSpinner.setDisable(!enabled);
        futureSkinsSpinner.setDisable(!enabled);
    }
    public void updateTimelineHighlight(String figureId) {
        if (timeline != null) {
            timeline.highlightLayer(figureId);
        }
    }
    public void updateCursor(Cursor cursor) {
        if (scene != null) {
            scene.setCursor(cursor);
        }
    }
    public void updateToolModeSelection(ToolMode mode) {
        if (toolToggleGroup != null) {
            ToggleButton btnToSelect = (mode == ToolMode.HAND) ? handToolButton : moveToolButton;
            if (!btnToSelect.isSelected()) {
                toolToggleGroup.selectToggle(btnToSelect);
            }
        }
    }
    public void showCanvasContextMenu(ContextMenuEvent event, boolean deleteEnabled, String figureName) {
        if (canvasContextMenu != null && canvas != null) {
            deleteFigureMenuItem.setText("Delete Figure '" + (figureName != null ? figureName : "") + "'");
            deleteFigureMenuItem.setDisable(!deleteEnabled);
            canvasContextMenu.show(canvas, event.getScreenX(), event.getScreenY());
        }
    }
    public MenuItem getDeleteFigureMenuItem() {
        return deleteFigureMenuItem;
    }
    public void hideCanvasContextMenu() {
        if (canvasContextMenu != null && canvasContextMenu.isShowing()) {
            canvasContextMenu.hide();
        }
    }
    public boolean showConfirmationDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(gameWindow.getStage());
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    public void updateStatus(String text) {
        if (statusLabel != null) {
            statusLabel.setText(text);
        }
    }

    public TimelineView getTimelineUI() {
        return timeline;
    }
    public Stage getStage() {
        return gameWindow.getStage();
    }

    public Canvas getCanvas() {
        return canvas;
    }

    private void drawKeyframeState(GraphicsContext gc, Map<String, List<Point>> figurePoses, Color colorHint) {
        if (figurePoses == null || controller == null) return;

        for (Map.Entry<String, List<Point>> entry : figurePoses.entrySet()) {
            String figId = entry.getKey();
            List<Point> pose = entry.getValue();
            StickFigureModel figure = controller.getFigureManager().getFigureById(figId);
            if (figure != null && pose != null) {
                drawSingleFigureState(gc, figure, pose, gc.getGlobalAlpha(), colorHint, false);
            }
        }
    }

    private void drawSingleFigureState(GraphicsContext gc, StickFigureModel figure, List<Point> posePoints, double alpha, Color colorHint, boolean isMainFigure) {
        if (figure == null || controller == null) return;

        boolean isSelected = isMainFigure && figure.id.equals(controller.getSelectedFigureId());
        int currentSelectedPointIndex = isSelected ? controller.getFigureManager().getSelectedPointIndex() : -1;

        gc.setLineCap(StrokeLineCap.ROUND);

        for (Segment segment : figure.segments) {
            Point start = getPointFromPose(figure, posePoints, segment.startIndex);
            Point end = getPointFromPose(figure, posePoints, segment.endIndex);
            if (start == null || end == null) continue;

            Color baseSegmentColor = segment.color;
            Color finalSegmentColor;

            if (!isMainFigure && colorHint != null) {
                finalSegmentColor = colorHint.deriveColor(0, 1.0, 1.0, 1.0);
            } else if (!isMainFigure) {
                finalSegmentColor = baseSegmentColor.deriveColor(0, 1.0, 1.0, 1.0);
            } else {
                finalSegmentColor = isSelected ? baseSegmentColor : baseSegmentColor.darker().desaturate();
            }

            gc.setGlobalAlpha(alpha);

            if (segment.isCircle) {
                double centerX = (start.x + end.x) / 2; double centerY = (start.y + end.y) / 2;
                double radius = Utility.distance(start, end) / 2.0;
                if (segment.isFilled) {
                    gc.setFill(finalSegmentColor);
                    gc.fillOval(centerX - radius, centerY - radius, 2 * radius, 2 * radius);
                } else {
                    gc.setStroke(finalSegmentColor);
                    gc.setLineWidth(segment.thickness);
                    gc.strokeOval(centerX - radius, centerY - radius, 2 * radius, 2 * radius);
                }
            } else {
                gc.setStroke(finalSegmentColor);
                gc.setLineWidth(segment.thickness);
                gc.strokeLine(start.x, start.y, end.x, end.y);
            }
            gc.setGlobalAlpha(1.0);
        }

        if (isMainFigure && !this.hideControls) {
            gc.setGlobalAlpha(1.0);
            for (int i = 0; i < figure.points.size(); ++i) {
                PointData pd = figure.points.get(i);
                Point currentPos = getPointFromPose(figure, posePoints, i);
                if (pd == null || currentPos == null || !pd.isVisible || pd.shape == PointData.Shape.NONE) continue;

                Color pointFillColor; Color pointOutlineColor;
                if (isSelected) {
                    pointFillColor = pd.color; pointOutlineColor = pd.color.darker();
                } else {
                    pointFillColor = pd.isPoseControlPoint ? DESELECTED_EFFECTOR_COLOR : DESELECTED_POINT_COLOR;
                    pointOutlineColor = DESELECTED_OUTLINE_COLOR;
                }

                gc.setFill(pointFillColor);
                double x = currentPos.x; double y = currentPos.y; double size = pd.size;

                if (isSelected && i == currentSelectedPointIndex) {
                    gc.setStroke(Color.GOLD); gc.setLineWidth(2);
                } else {
                    gc.setStroke(pointOutlineColor); gc.setLineWidth(1);
                }
                gc.setLineCap(StrokeLineCap.BUTT);

                switch (pd.shape) {
                    case CIRCLE:
                        gc.fillOval(x - size, y - size, size * 2, size * 2);
                        gc.strokeOval(x - size, y - size, size * 2, size * 2);
                        break;
                    case SQUARE:
                        gc.fillRect(x - size, y - size, size * 2, size * 2);
                        gc.strokeRect(x - size, y - size, size * 2, size * 2);
                        break;
                    case NONE: break;
                }
            }
            gc.setLineCap(StrokeLineCap.ROUND);
        }
    }

    private Point getPointFromPose(StickFigureModel figure, List<Point> posePoints, int index) {
        if (posePoints != null) {
            if (index >= 0 && index < posePoints.size()) {
                return posePoints.get(index);
            }
        } else if (figure != null) {
            return figure.getPosition(index);
        }

        logger.warn("Could not get point position for figure {}, index {}", figure != null ? figure.id : "null", index);
        return null;
    }

    public static class FrameData {
        public final int frameNumber;
        public final Map<String, List<Point>> figurePoses;

        public FrameData(int frameNumber, Map<String, List<Point>> figurePoses) {
            this.frameNumber = frameNumber;
            this.figurePoses = figurePoses != null ? new HashMap<>(figurePoses) : Collections.emptyMap();
        }
    }
}