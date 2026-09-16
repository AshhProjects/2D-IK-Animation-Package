package uk.ac.soton.comp3200.controller;

import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp3200.model.animation.InterpolationContext;
import uk.ac.soton.comp3200.model.animation.TimelineModel;
import uk.ac.soton.comp3200.event.AnimationUpdateListener;
import uk.ac.soton.comp3200.model.figure.manager.FigureManager;
import uk.ac.soton.comp3200.model.figure.model.IKChain;
import uk.ac.soton.comp3200.model.figure.model.LimbAngles;
import uk.ac.soton.comp3200.model.figure.model.StickFigureModel;
import uk.ac.soton.comp3200.model.io.SaveManager;
import uk.ac.soton.comp3200.model.io.export.ExportManager;
import uk.ac.soton.comp3200.model.math.Point;
import uk.ac.soton.comp3200.view.common.ToolMode;
import uk.ac.soton.comp3200.view.dialog.ProjectSettingsDialog;
import uk.ac.soton.comp3200.view.scene.MenuView;
import uk.ac.soton.comp3200.view.timeline.TimelineView;

import java.util.*;

/**
 * Main Controller for the application window (MenuView).
 * Handles menu actions, toolbar actions, save/load, export,
 * coordinates sub-controllers (Canvas, Timeline), and manages overall state updates.
 * Acts as the primary listener for the TimelineModel.
 */
public class MenuController implements AnimationUpdateListener {

    private static final Logger logger = LogManager.getLogger(MenuController.class);

    private MenuView view;

    private final TimelineModel timelineModel;
    private final FigureManager figureManager;
    private final SaveManager saveManager;
    private final ExportManager exportManager;

    private final CanvasController canvasController;
    private final TimelineController timelineController;

    private ToolMode currentToolMode = ToolMode.HAND;
    private boolean onionSkinEnabled = false;
    private int pastSkinsToShow = 2;
    private int futureSkinsToShow = 1;

    public boolean isOnionSkinEnabled() {
        return onionSkinEnabled;
    }

    public MenuController(TimelineModel timelineModel, FigureManager figureManager,
                          SaveManager saveManager, ExportManager exportManager) {
        this.timelineModel = timelineModel;
        this.figureManager = figureManager;
        this.saveManager = saveManager;
        this.exportManager = exportManager;

        this.canvasController = new CanvasController(figureManager, this);
        this.timelineController = new TimelineController();

        this.timelineModel.setListener(this);
    }

    public void setView(MenuView view) {
        this.view = view;
        wireSubControllers();
        Platform.runLater(this::initializeViewState);
    }

    private void wireSubControllers() {
        if (view == null) {
            logger.error("Cannot wire sub-controllers: View is null.");
            return;
        }
        TimelineView timelineView = view.getTimelineUI();
        Canvas canvas = view.getCanvas();

        if (canvas != null) {
            canvasController.setView(view);
            canvasController.setCanvas(canvas);
            logger.debug("CanvasController wired.");
        } else {
            logger.error("Cannot wire CanvasController: Canvas is null.");
        }

        if (timelineView != null) {
            timelineController.setModel(this.timelineModel);
            timelineController.setView(timelineView);
            timelineController.setMenuController(this);
            timelineView.setTimelineController(timelineController);
            logger.debug("TimelineController wired.");
        } else {
            logger.error("Cannot wire TimelineController: TimelineView is null.");
        }
    }

    public void initializeViewState() {
        logger.info("MenuController initializing View state...");
        if (view == null) {
            logger.error("Cannot initialize view state: View is null.");
            return;
        }

        if (figureManager.getAllFigures().isEmpty() && timelineModel.getManagedFigureIds().isEmpty()) {
            logger.info("MenuController: No figures found on initial load, adding default figure.");
            handleAddFigureClicked();
        } else {
            requestTimelineUIUpdate();
            requestCanvasRedraw();
        }

        view.updateTimelineHighlight(figureManager.getSelectedFigureId());
        view.updateToolModeSelection(this.currentToolMode);
        view.updateOnionSkinState(this.onionSkinEnabled, this.pastSkinsToShow, this.futureSkinsToShow);
        view.updateCursor(Cursor.DEFAULT);
        view.updateStatus("Ready.");
    }

    public TimelineModel getEngine() { return timelineModel; }
    public FigureManager getFigureManager() { return figureManager; }
    public CanvasController getCanvasController() { return canvasController; }
    public TimelineController getTimelineController() { return timelineController; }

    public void handleNewAnimation() {
        logger.info("MenuController: New Animation requested.");
        figureManager.removeAllFigures();
        timelineModel.resetState();
        handleAddFigureClicked();
        currentToolMode = ToolMode.HAND;
        view.updateToolModeSelection(currentToolMode);
        view.updateTimelineHighlight(null);
        requestCanvasRedraw();
    }

    public void handleSaveAnimation() { saveManager.saveAnimation(); }
    public void handleSaveAnimationAs() { saveManager.saveAnimationAs(); }
    public void handleLoadAnimation() {
        logger.info("MenuController: Load Animation requested.");
        saveManager.loadAnimation();
        currentToolMode = ToolMode.HAND;
    }
    public void handleExit() {
        logger.info("MenuController: Exit requested.");
        saveManager.stopAutoSave();
        Platform.exit();
        System.exit(0);
    }

    public void handleExportPng() { exportManager.exportAsPng(); }
    public void handleExportPngSequence() { exportManager.exportAsPngSequence(); }
    public void handleExportGif() { exportManager.exportAsGif(); }
    public void handleExportMp4() { exportManager.exportAsMp4(); }

    public void handleToolModeChange(ToolMode newMode) {
        if (this.currentToolMode != newMode) {
            this.currentToolMode = newMode;
            logger.info("MenuController: Tool mode changed to {}", newMode);
            canvasController.setCurrentToolMode(newMode);
            if (view != null) {
                view.updateToolModeSelection(newMode);
                canvasController.updateViewCursor();
            }
        }
    }

    public void handleAddFigureClicked() {
        logger.info("MenuController: Add Figure clicked");
        StickFigureModel newFigure = figureManager.createAndAddFigure(null);
        if (newFigure != null) {
            timelineModel.addFigure(newFigure.id);
            figureManager.selectFigure(newFigure.id);
            if (view != null) {
                view.updateTimelineHighlight(newFigure.id);
            }
            requestCanvasRedraw();
        } else {
            logger.error("Failed to create new figure.");
        }
    }

    public void handleDeleteFigureClicked(String figureId) {
        logger.info("MenuController: Delete Figure requested for {}", figureId);
        if (view != null && view.showConfirmationDialog("Delete Figure?", "Delete figure '" + figureId + "' permanently?")) {
            figureManager.removeFigure(figureId);
            timelineModel.removeFigure(figureId);
            if (figureId.equals(figureManager.getSelectedFigureId())) {
                figureManager.deselectFigure();
                if (view != null) view.updateTimelineHighlight(null);
            }
            requestCanvasRedraw();
        }
    }

    public void handleDeleteFigureRequestFromTimeline(String figureId) {
        handleDeleteFigureClicked(figureId);
    }


    public void handleOnionSkinEnableChange(boolean enabled) {
        logger.debug("MenuController: Onion skin enable changed to {}", enabled);
        this.onionSkinEnabled = enabled;
        if (view != null) {
            view.updateOnionSkinState(this.onionSkinEnabled, this.pastSkinsToShow, this.futureSkinsToShow);
        }
        requestCanvasRedraw();
    }

    public void handlePastSkinsChange(int count) {
        logger.debug("MenuController: Past skins changed to {}", count);
        this.pastSkinsToShow = count;
        if (view != null && this.onionSkinEnabled) {
            view.updateOnionSkinState(true, this.pastSkinsToShow, this.futureSkinsToShow);
            requestCanvasRedraw();
        }
    }

    public void handleFutureSkinsChange(int count) {
        logger.debug("MenuController: Future skins changed to {}", count);
        this.futureSkinsToShow = count;
        if (view != null && this.onionSkinEnabled) {
            view.updateOnionSkinState(true, this.pastSkinsToShow, this.futureSkinsToShow);
            requestCanvasRedraw();
        }
    }

    @Override
    public void onTimelineStateChanged() {
        logger.debug("MenuController: Received onTimelineStateChanged from Engine.");
        Platform.runLater(() -> {
            logger.debug("MenuController: Executing UI updates via Platform.runLater for onTimelineStateChanged.");
            requestTimelineUIUpdate();
            requestCanvasRedraw();
            if (view != null) {
                view.updateTimelineHighlight(figureManager.getSelectedFigureId());
            }
        });
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        logger.debug("MenuController: Received onPlaybackStateChanged from Engine: {}", isPlaying);
        if (view != null) {
            Platform.runLater(() -> {
                if(canvasController != null) canvasController.updateViewCursor();

                TimelineView timelineView = view.getTimelineUI();
                if (timelineView != null) {
                    timelineView.updatePlaybackButtonStates(isPlaying);
                }
            });
        }
    }

    public void handleEditProjectSettings() {
        logger.info("MenuController: Edit -> Project Settings requested.");

        if (view == null || view.getStage() == null) {
            logger.error("Cannot show Project Settings: View or Stage is null.");
            return;
        }
        if (timelineModel == null) {
            logger.error("Cannot show Project Settings: TimelineModel is null.");
            return;
        }

        ProjectSettingsDialog dialog = new ProjectSettingsDialog(
                view.getStage(),
                timelineModel.getFps(),
                timelineModel.getMaxFrames()
        );

        Optional<Pair<Integer, Integer>> result = dialog.showAndWait();

        result.ifPresent(values -> {
            int newFps = values.getKey();
            int newMaxFrames = values.getValue();

            boolean fpsValid = (newFps >= 1 && newFps <= 60);
            boolean maxFramesValid = (newMaxFrames >= 10);

            if (fpsValid && maxFramesValid) {
                boolean fpsChanged = (timelineModel.getFps() != newFps);
                boolean maxFramesChanged = (timelineModel.getMaxFrames() != newMaxFrames);

                if (fpsChanged) {
                    timelineModel.setFps(newFps);
                    logger.info("Project Settings: FPS updated to {}", newFps);
                }
                if (maxFramesChanged) {
                    timelineModel.setMaxFrames(newMaxFrames);
                    logger.info("Project Settings: Max Frames updated to {}", newMaxFrames);
                }

                if (fpsChanged || maxFramesChanged) {
                    if (view != null) view.updateStatus("Project settings updated.");
                } else {
                    if (view != null) view.updateStatus("Project settings unchanged.");
                }

            } else {
                String errorMsg = "Invalid settings:\n";
                if (!fpsValid) errorMsg += "- FPS must be between 1 and 60.\n";
                if (!maxFramesValid) errorMsg += "- Max Frames must be 10 or greater.\n";
                logger.warn("Invalid project settings entered: FPS={}, MaxFrames={}", newFps, newMaxFrames);
                if (view != null) view.showErrorDialog("Invalid Settings", errorMsg);
            }
        });
    }

    @Override
    public void onKeyframeSelected(Map<String, List<Point>> frameFigureStates, InterpolationContext context) {
        logger.trace("MenuController: Received onKeyframeSelected from Engine for frame {}", timelineModel.getCurrentFrame());
        if (frameFigureStates != null) {
            boolean needsRedraw = false;
            for (Map.Entry<String, List<Point>> entry : frameFigureStates.entrySet()) {
                String figureId = entry.getKey();
                List<Point> poseData = entry.getValue();
                StickFigureModel figure = figureManager.getFigureById(figureId);
                if (figure != null) {
                    boolean applied = figure.setPointsFromList(poseData);
                    if (applied) {
                        if (context != null) {
                            Map<String, LimbAngles> figureFrameAngles = timelineModel.getInterpolatedAngles(figureId, timelineModel.getCurrentFrame());
                            if (figureFrameAngles != null) {
                                for (IKChain chain : figure.ikChains) {
                                    LimbAngles angles = figureFrameAngles.get(chain.name);
                                    if (angles != null) {
                                        figure.reconstructLimbFromAngles(chain, angles);
                                    }
                                }
                            }
                            figure.applyConstraintsIteratively(-1);
                        } else {
                            figure.initializeConstraints();
                            figure.calculateIKChainLengths();
                            figure.applyConstraintsIteratively(-1);
                            figure.solveAllIK();
                        }
                        needsRedraw = true;
                    }
                } else {
                    logger.warn("MenuController: Figure '{}' not found in FigureManager during onKeyframeSelected.", figureId);
                }
            }
            if (needsRedraw) {
                requestCanvasRedraw();
            }
        } else {
            requestCanvasRedraw();
        }
    }

    @Override
    public Map<String, List<Point>> requestFigureStates() {
        logger.trace("MenuController: Engine requested current figure states.");
        return figureManager.getAllFigureStates();
    }

    @Override
    public Map<String, Map<String, LimbAngles>> requestFigureAngles() {
        logger.trace("MenuController: Engine requested current figure angles.");
        return figureManager.getAllFigureAngles();
    }

    @Override
    public void hideEditorControls() {
        logger.trace("MenuController: Engine requested hide controls.");
        if (view != null) {
            Platform.runLater(() -> view.setControlsVisibility(false));
        }
    }

    @Override
    public void showEditorControls() {
        logger.trace("MenuController: Engine requested show controls.");
        if (view != null) {
            Platform.runLater(() -> view.setControlsVisibility(true));
        }
    }

    public void handleFigureSelection(String figureId) {
        logger.debug("MenuController: Figure selection changed to: {}", figureId);
        figureManager.selectFigure(figureId);
        if (view != null) {
            view.updateTimelineHighlight(figureId);
        }
        requestCanvasRedraw();
    }

    public void handleTimelineBackgroundClick() {
        logger.debug("MenuController: Timeline background click processed.");
        if (figureManager.getSelectedFigureId() != null) {
            handleFigureSelection(null);
        }
    }

    private void requestTimelineUIUpdate() {
        if (view != null) {
            TimelineView timelineView = view.getTimelineUI();
            if (timelineView != null) {
                timelineView.updateUIFromEngine();
            } else {
                logger.warn("MenuController: Cannot request timeline update, TimelineView is null.");
            }
        }
    }

    public void requestCanvasRedraw() {
        if (view != null) {
            List<StickFigureModel> figures = figureManager.getAllFigures();

            List<MenuView.FrameData> pastData = Collections.emptyList();
            List<MenuView.FrameData> futureData = Collections.emptyList();
            if (this.onionSkinEnabled) {
                pastData = getOnionSkinFrameData(true, this.pastSkinsToShow);
                futureData = getOnionSkinFrameData(false, this.futureSkinsToShow);
            }

            view.redrawCanvas(figures, pastData, futureData);
        }
    }

    public String getSelectedFigureId() {
        return figureManager.getSelectedFigureId();
    }

    private List<MenuView.FrameData> getOnionSkinFrameData(boolean past, int count) {
        List<MenuView.FrameData> frameDataList = new ArrayList<>();
        if (count <= 0) return frameDataList;

        int currentFrame = timelineModel.getCurrentFrame();
        List<Integer> frameNumbers;
        if(past) {
            frameNumbers = timelineModel.findNearestPastKeyframeNumbers(currentFrame, count);
        } else {
            frameNumbers = timelineModel.findNearestFutureKeyframeNumbers(currentFrame, count);
        }

        for (int frameNum : frameNumbers) {
            Map<String, List<Point>> poses = new HashMap<>();
            for (String figId : figureManager.getAllFigureIds()) {
                List<Point> pose = timelineModel.getKeyframePoints(figId, frameNum);
                if (pose != null) {
                    poses.put(figId, pose);
                }
            }
            if (!poses.isEmpty()) {
                frameDataList.add(new MenuView.FrameData(frameNum, poses));
            }
        }
        return frameDataList;
    }

    public void setCanvasForExportManager(Canvas canvas) {
        logger.debug("MenuController: Setting canvas for ExportManager. Canvas is null? {}", (canvas == null));
        if (this.exportManager != null) {
            this.exportManager.setCanvas(canvas);
            this.exportManager.setListener(this);
        } else {
            logger.error("MenuController cannot set canvas for ExportManager: ExportManager is null.");
        }
    }
}