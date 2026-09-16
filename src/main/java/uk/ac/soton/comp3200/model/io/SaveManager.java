package uk.ac.soton.comp3200.model.io;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp3200.model.animation.TimelineModel;
import uk.ac.soton.comp3200.model.figure.manager.FigureManager;
import uk.ac.soton.comp3200.model.figure.model.PointData;
import uk.ac.soton.comp3200.model.math.Point;
import uk.ac.soton.comp3200.model.animation.KeyframeSegment;
import uk.ac.soton.comp3200.model.figure.model.LimbAngles;
import uk.ac.soton.comp3200.model.figure.model.StickFigureModel;

import java.io.*;
import java.util.*;

public class SaveManager {

    private static final Logger logger = LogManager.getLogger(SaveManager.class);
    private static final String FILE_EXTENSION = ".stickanim";
    private static final String FILE_DESC = "Stick Figure Animation";

    private final Stage ownerStage;
    private final FigureManager figureManager;
    private final TimelineModel engine;
    private File currentSaveFile = null;

    private Timeline autoSaveTimeline = null;
    private Duration autoSaveInterval = Duration.seconds(300);

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    static {
        jsonMapper.enable(SerializationFeature.INDENT_OUTPUT);
        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public SaveManager(Stage ownerStage, FigureManager figureManager, TimelineModel engine) {
        this.ownerStage = ownerStage;
        this.figureManager = figureManager;
        this.engine = engine;
    }

    private File chooseFile(boolean open) {
        FileChooser fileChooser = new FileChooser();
        if (open) {
            fileChooser.setTitle("Open Animation");
        } else {
            fileChooser.setTitle("Save Animation As");
        }

        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(FILE_DESC, "*" + FILE_EXTENSION));
        if (currentSaveFile != null && !open) {
            fileChooser.setInitialDirectory(currentSaveFile.getParentFile());
            fileChooser.setInitialFileName(currentSaveFile.getName());
        } else if (!open) {
            fileChooser.setInitialFileName("Untitled" + FILE_EXTENSION);
        }

        if (open) {
            return fileChooser.showOpenDialog(ownerStage);
        } else {
            return fileChooser.showSaveDialog(ownerStage);
        }
    }

    public void startAutoSave(Duration interval) {
        stopAutoSave();
        this.autoSaveInterval = interval;
        if (autoSaveTimeline == null && currentSaveFile != null) {
            logger.info("Starting auto-save to current project file every {}", interval);
            autoSaveTimeline = new Timeline(
                    new KeyFrame(interval, event -> {
                        logger.debug("Auto-saving to {} ...", currentSaveFile.getName());
                        try {
                            writeStateToFileInternal(currentSaveFile);
                            logger.debug("Auto-save successful.");
                        } catch (Exception e) {
                            logger.error("Auto-save failed for {}: {}", currentSaveFile.getName(), e.getMessage());
                        }
                    })
            );
            autoSaveTimeline.setCycleCount(Timeline.INDEFINITE);
            autoSaveTimeline.play();
        }
    }

    public void stopAutoSave() {
        if (autoSaveTimeline != null) {
            autoSaveTimeline.stop();
            autoSaveTimeline = null;
            logger.info("Auto-save stopped.");
        }
    }

    public void saveAnimation() {
        if (currentSaveFile == null) {
            saveAnimationAs();
        }
        else saveToFile(currentSaveFile);
    }

    public void saveAnimationAs() {
        File file = chooseFile(false);
        if (file == null) return;

        String path = file.getAbsolutePath();
        if (!path.toLowerCase().endsWith(FILE_EXTENSION)) file = new File(path + FILE_EXTENSION);

        if (saveToFile(file)) {
            showInfo("Save Successful", "Animation saved to\n" + file.getName());
        }
    }

    /**
     * Attempts to write animation to the specified file and updates currentSaveFile on success
     */
    private boolean saveToFile(File file) {
        logger.info("Attempting save to: {}", file.getAbsolutePath());
        try {
            writeStateToFileInternal(file);
            this.currentSaveFile = file;
            logger.info("Save successful. Current file set to: {}", file.getName());
            startAutoSave(this.autoSaveInterval);
            return true;
        } catch (Exception e) {
            logger.error("Failed to save animation to file: {}", file.getAbsolutePath(), e);
            showError("Save Failed", "Could not write animation file:\n" + e.getMessage());
            return false;
        }
    }

    /**
     * Core logic to write current state to file as JSON.
     */
    private void writeStateToFileInternal(File targetFile) throws IOException {
        logger.debug("Writing state to file: {}", targetFile.getName());
        AnimationSaveData saveData = new AnimationSaveData();

        saveData.settings = new AnimationSaveData.SettingsData();
        saveData.settings.fps = engine.getFps();
        saveData.settings.currentFrame = engine.getCurrentFrame();
        saveData.settings.maxFrames = engine.getMaxFrames();

        saveData.figures = figureManager.getAllFigureIds();

        saveData.figureData = new HashMap<>();
        for (String figureId : saveData.figures) {
            AnimationSaveData.FigureSpecificData figureInfo = new AnimationSaveData.FigureSpecificData();
            figureInfo.keyframes = engine.getAllFigureKeyframes().get(figureId);
            figureInfo.angles = engine.getAllFigureKeyframeAngles().get(figureId);
            figureInfo.interpolationSegments = engine.getAllFigureSegments().get(figureId);
            saveData.figureData.put(figureId, figureInfo);
        }

        jsonMapper.writeValue(targetFile, saveData);
        logger.debug("JSON write complete for: {}", targetFile.getName());
    }

    public void loadAnimation() {
        File file = chooseFile(true);
        if (file == null) return;
        logger.info("Loading animation from: {}", file.getAbsolutePath());

        try {
            AnimationSaveData loadedData = jsonMapper.readValue(file, AnimationSaveData.class);

            int loadedFps = (loadedData.settings != null) ? loadedData.settings.fps : 24;
            int loadedCurrentFrame = (loadedData.settings != null) ? loadedData.settings.currentFrame : 1;
            int loadedMaxFrames = (loadedData.settings != null && loadedData.settings.maxFrames > 0)
                    ? loadedData.settings.maxFrames : 120;

            List<String> loadedFigureIds;
            if (loadedData.figures != null) {
                loadedFigureIds = loadedData.figures;
            } else {
                loadedFigureIds = new ArrayList<>();
            }

            Map<String, TreeMap<Integer, List<Point>>> loadedFigureKeyframes = new HashMap<>();
            Map<String, TreeMap<Integer, Map<String, LimbAngles>>> loadedFigureAngles = new HashMap<>();
            Map<String, Set<KeyframeSegment>> loadedFigureSegments = new HashMap<>();

            if (loadedData.figureData != null) {
                for (String figureId : loadedFigureIds) {
                    AnimationSaveData.FigureSpecificData figureInfo = loadedData.figureData.get(figureId);

                    if (figureInfo == null) {
                        logger.warn("Figure ID '{}' listed but data not found in JSON.", figureId);
                        continue;
                    }

                    TreeMap<Integer, List<Point>> keyframesMap = new TreeMap<>(figureInfo.keyframes);
                    loadedFigureKeyframes.put(figureId, keyframesMap);

                    TreeMap<Integer, Map<String, LimbAngles>> anglesMap = new TreeMap<>(figureInfo.angles);
                    loadedFigureAngles.put(figureId, anglesMap);

                    loadedFigureSegments.put(figureId, figureInfo.interpolationSegments);

                }
            }

            stopAutoSave();
            applyLoadedState(loadedFps, loadedCurrentFrame, loadedMaxFrames, loadedFigureIds,
                    loadedFigureKeyframes, loadedFigureAngles, loadedFigureSegments);

            this.currentSaveFile = file;
            logger.info("Animation loaded successfully via JSON file. Current file set.");
            showInfo("Load Successful", "Loaded animation from\n" + file.getName());
            startAutoSave(this.autoSaveInterval);

        } catch (IOException e) {
            logger.error("Failed to load or parse animation file: {}", file.getAbsolutePath(), e);
            showError("Load Failed", "Could not read or parse animation file:\n" + e.getMessage());
            this.currentSaveFile = null;
        } catch (Exception e) {
            logger.error("Unexpected error during animation loading: {}", file.getAbsolutePath(), e);
            showError("Load Error", "An unexpected error occurred during loading:\n" + e.getMessage());
            this.currentSaveFile = null;
        }
    }

    /**
     * Applies the loaded state to the MenuScene and AnimationTimeline
     */
    private void applyLoadedState(int fps, int currentFrame, int maxFrames,
                                  List<String> loadedFigureIds,
                                  Map<String, TreeMap<Integer, List<Point>>> loadedFigureKeyframes,
                                  Map<String, TreeMap<Integer, Map<String, LimbAngles>>> loadedFigureAngles,
                                  Map<String, Set<KeyframeSegment>> loadedFigureSegments) {


        Platform.runLater(() -> {
            logger.info("Applying loaded state (using closest keyframe for initial pose)...");

            figureManager.removeAllFigures();
            engine.resetState();

            if (loadedFigureIds.isEmpty()) {
                logger.warn("Loaded file contains no figure IDs.");
            } else {
                logger.info("Loading {} figures by ID.", loadedFigureIds.size());

                int maxLoadedIdNum = -1;

                for (String id : loadedFigureIds) {
                    StickFigureModel figure = figureManager.createAndAddFigure(id);
                    if (figure == null) continue;

                    TreeMap<Integer, List<Point>> keyframesForFigure = loadedFigureKeyframes.get(id);
                    List<Point> initialPoseToApply = null;
                    int appliedPoseFrame = -1;

                    if (keyframesForFigure != null && !keyframesForFigure.isEmpty()) {
                        Integer floorKey = keyframesForFigure.floorKey(currentFrame);
                        Integer ceilingKey = keyframesForFigure.ceilingKey(currentFrame);
                        Integer closestFrameNum = null;

                        if (floorKey == null && ceilingKey == null) {
                            logger.warn("Could not find floor or ceiling key for figure {} despite keyframes existing.", id);
                        } else if (floorKey == null) {
                            closestFrameNum = ceilingKey;
                        } else if (ceilingKey == null) {
                            closestFrameNum = floorKey;
                        } else {
                            int distToFloor = currentFrame - floorKey;
                            int distToCeiling = ceilingKey - currentFrame;
                            closestFrameNum = (distToFloor <= distToCeiling) ? floorKey : ceilingKey;
                        }

                        if (closestFrameNum != null) {
                            initialPoseToApply = keyframesForFigure.get(closestFrameNum);
                            appliedPoseFrame = closestFrameNum;
                        }
                    }

                    if (initialPoseToApply != null) {
                        logger.debug("Pre-setting pose for figure {} from closest keyframe {}", id, appliedPoseFrame);
                        figure.setPointsFromList(initialPoseToApply);
                    } else {
                        logger.debug("No keyframes found for figure {}. Applying default offset.", id);
                        double defaultOffsetX = 400.0;
                        double defaultOffsetY = 300.0;
                        for (PointData pd : figure.points) {
                            if (pd != null && pd.position != null) {
                                pd.position.x += defaultOffsetX;
                                pd.position.y += defaultOffsetY;
                            }
                        }
                    }

                    figure.initializeModel();
                    figure.solveAllIK();

                    try {
                        int num = Integer.parseInt(id.substring("figure_".length()));
                        if (num > maxLoadedIdNum) maxLoadedIdNum = num;
                    } catch (Exception ignored) {}

                }

                figureManager.getFigureCounter().set(maxLoadedIdNum + 1);
                logger.debug("Figure counter set to {} after loading figures.", figureManager.getFigureCounter().get());

                logger.info("Loading full animation state data into AnimationEngine...");
                engine.loadState(fps, currentFrame, maxFrames, loadedFigureIds,
                        loadedFigureKeyframes, loadedFigureAngles, loadedFigureSegments);
            }

            logger.info("Loaded state application initiated.");
        });
    }

    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.initOwner(ownerStage);
            alert.showAndWait();
        });
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.initOwner(ownerStage);
            alert.showAndWait();
        });
    }
}