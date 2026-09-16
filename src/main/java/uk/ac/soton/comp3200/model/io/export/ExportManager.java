package uk.ac.soton.comp3200.model.io.export;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.WritableImage;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp3200.model.animation.TimelineModel;
import uk.ac.soton.comp3200.event.AnimationUpdateListener;
import uk.ac.soton.comp3200.model.math.Point;

import uk.ac.soton.comp3200.model.io.export.format.GifSequenceWriter;
import uk.ac.soton.comp3200.model.io.export.format.MP4Encoder;

import uk.ac.soton.comp3200.view.dialog.ExportProgressDialog;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Handles exporting animations to different file formats using the AnimationEngine.
 */
public class ExportManager {
    private static final Logger logger = LogManager.getLogger(ExportManager.class);

    private Canvas canvas;
    private final TimelineModel engine;
    private AnimationUpdateListener animationUpdateListener;
    private final Stage stage;

    private volatile boolean isExporting = false;
    private List<BufferedImage> exportFrames;
    private int exportTargetFrame;
    private ExportProgressDialog exportProgressDialog;
    private File exportOutputFile;
    private String exportFormat;
    private int exportOriginalFrame;
    private boolean exportWasPlaying;

    /**
     * Creates a new export manager.
     * @param canvas The canvas containing the animation.
     * @param engine The core animation engine.
     * @param listener The listener (MenuScene) to notify for UI control changes.
     * @param stage The primary stage for dialogs.
     */
    public ExportManager(Canvas canvas, TimelineModel engine, AnimationUpdateListener listener, Stage stage) {
        this.canvas = canvas;
        this.engine = engine;
        this.animationUpdateListener = listener;
        this.stage = stage;
    }

    /**
     * Exports the current frame as a PNG image.
     */
    public void exportAsPng() {
        logger.info("Exporting current frame as PNG");

        final boolean wasPlaying = engine.isPlaying();
        if (wasPlaying) {
            engine.stop();
        }

        logger.debug("Hiding editor controls for PNG export.");
        animationUpdateListener.hideEditorControls();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PNG Image");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Images", "*.png"));
        File file = fileChooser.showSaveDialog(stage);

        if (file == null) {
            logger.info("PNG export cancelled.");
            logger.debug("Restoring editor controls after PNG cancel.");
            animationUpdateListener.showEditorControls();
            if (wasPlaying) {
                engine.play();
            }
            return;
        }

        String path = file.getAbsolutePath();
        if (!path.toLowerCase().endsWith(".png")) {
            file = new File(path + ".png");
        }

        try {
            File finalFile = file;
            Platform.runLater(() -> {
                try {

                    logger.debug("ExportManager (PNG): Taking snapshot. Canvas is null? {}", (this.canvas == null));
                    if (this.canvas == null) {
                        logger.error("Cannot take PNG snapshot: Canvas is null!");
                        cleanupExport(null, exportOriginalFrame, exportWasPlaying);
                        return;
                    }
                    WritableImage writableImage = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
                    SnapshotParameters params = new SnapshotParameters();
                    canvas.snapshot(params, writableImage);

                    BufferedImage bufferedImage = SwingFXUtils.fromFXImage(writableImage, null);
                    if (bufferedImage != null) {
                        ImageIO.write(bufferedImage, "png", finalFile);
                        logger.info("PNG export completed: {}", finalFile.getAbsolutePath());
                    } else {
                        logger.error("PNG export failed: Snapshot conversion resulted in null.");
                    }
                } catch (IOException e) {
                    logger.error("Failed to write PNG file: {}", e.getMessage(), e);
                } finally {
                    logger.debug("Restoring editor controls after PNG export.");
                    animationUpdateListener.showEditorControls();
                    if (wasPlaying) {
                        engine.play();
                    }
                }
            });
        } catch (Exception e) {
            logger.error("Failed to initiate PNG export snapshot: {}", e.getMessage(), e);
            logger.debug("Restoring editor controls after PNG initiation error.");
            animationUpdateListener.showEditorControls();
            if (wasPlaying) {
                engine.play();
            }
        }
    }

    private void startSequenceExport(File outputFile, String format, String dialogTitle) {
        exportOriginalFrame = engine.getCurrentFrame();
        exportWasPlaying = engine.isPlaying();

        if (exportWasPlaying) {
            engine.stop();
        }
        logger.debug("Hiding editor controls for {} export.", format.toUpperCase());
        animationUpdateListener.hideEditorControls();

        isExporting = true;
        exportFrames = new ArrayList<>();
        this.exportOutputFile = outputFile;
        this.exportFormat = format;

        int firstFrame = 1;
        int lastFrame = 1;
        Map<String, TreeMap<Integer, List<Point>>> allKeyframes = engine.getAllFigureKeyframes();

        int maxKeyframeFound = 0;
        int minKeyframeFound = Integer.MAX_VALUE;
        boolean hasKeyframes = false;
        for (TreeMap<Integer, ?> figureFrames : allKeyframes.values()) {
            if (figureFrames != null && !figureFrames.isEmpty()) {
                hasKeyframes = true;
                maxKeyframeFound = Math.max(maxKeyframeFound, figureFrames.lastKey());
                minKeyframeFound = Math.min(minKeyframeFound, figureFrames.firstKey());
            }
        }

        if (hasKeyframes) {
            firstFrame = (minKeyframeFound == Integer.MAX_VALUE) ? 1 : minKeyframeFound;
            lastFrame = maxKeyframeFound;
        }
        if (lastFrame < firstFrame) lastFrame = firstFrame;

        exportProgressDialog = new ExportProgressDialog(stage, dialogTitle, firstFrame, lastFrame, this::cancelExport);
        exportProgressDialog.show();

        exportTargetFrame = firstFrame;
        logger.info("Starting export sequence from frame {} to {} for format {}", firstFrame, lastFrame, format);
        triggerFrameCapture(firstFrame, lastFrame);
    }

    private void triggerFrameCapture(int currentFrame, int lastFrame) {
        if (!isExporting || exportProgressDialog == null || !exportProgressDialog.isShowing()) {
            logger.warn("triggerFrameCapture called but export not active or dialog closed.");
            handleExportCompletion(false);
            return;
        }
        if (currentFrame > lastFrame) {
            logger.info("Reached end of frame range. Finishing export.");
            handleExportCompletion(true);
            return;
        }

        logger.debug("Triggering capture for frame: {}", currentFrame);
        exportTargetFrame = currentFrame;
        exportProgressDialog.updateProgress(currentFrame, "Processing frame " + currentFrame);

        engine.goToFrame(currentFrame);

        Platform.runLater(this::performSnapshot);
    }

    private void performSnapshot() {
        if (!isExporting || exportProgressDialog == null || !exportProgressDialog.isShowing()) {
            logger.warn("performSnapshot executed but export cancelled or dialog closed for frame {}", exportTargetFrame);
            if(isExporting) {
                isExporting = false;
                handleExportCompletion(false);
            }
            return;
        }

        logger.debug("ExportManager (Sequence): Performing snapshot for frame {}. Canvas is null? {}", exportTargetFrame, (this.canvas == null));
        if (this.canvas == null) {
            logger.error("Cannot perform snapshot for frame {}: Canvas is null!", exportTargetFrame);
            isExporting = false;
            handleExportCompletion(false);
            return;
        }
        WritableImage writableImage = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
        SnapshotParameters params = new SnapshotParameters();
        canvas.snapshot(params, writableImage);

        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(writableImage, null);
        if (bufferedImage != null) {
            exportFrames.add(bufferedImage);
            logger.trace("Snapshot successful, added frame {} to list (size: {})", exportTargetFrame, exportFrames.size());
        } else {
            logger.error("Snapshot conversion failed for frame {}.", exportTargetFrame);
        }

        int nextFrame = exportTargetFrame + 1;
        int lastFrame = exportProgressDialog.getMaxProgress();
        triggerFrameCapture(nextFrame, lastFrame);
    }

    public void exportAsPngSequence() {
        logger.info("Exporting animation as PNG Sequence");
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Directory for PNG Sequence");
        File selectedDirectory = directoryChooser.showDialog(stage);
        if (selectedDirectory == null || !selectedDirectory.isDirectory()) {
            logger.info("PNG Sequence export cancelled or invalid directory.");
            return;
        }
        startSequenceExport(selectedDirectory, "png_sequence", "Exporting PNG Sequence");
    }

    public void exportAsGif() {
        logger.info("Exporting animation as GIF");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save GIF Animation");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("GIF Animations", "*.gif"));
        File file = fileChooser.showSaveDialog(stage);
        if (file == null) {
            logger.info("GIF export cancelled");
            return;
        }
        if (!file.getName().toLowerCase().endsWith(".gif")) {
            file = new File(file.getAbsolutePath() + ".gif");
        }
        startSequenceExport(file, "gif", "Exporting GIF");
    }

    public void exportAsMp4() {
        logger.info("Exporting animation as MP4");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save MP4 Video");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("MP4 Videos", "*.mp4"));
        File file = fileChooser.showSaveDialog(stage);
        if (file == null) {
            logger.info("MP4 export cancelled");
            return;
        }
        if (!file.getName().toLowerCase().endsWith(".mp4")) {
            file = new File(file.getAbsolutePath() + ".mp4");
        }
        startSequenceExport(file, "mp4", "Exporting MP4");
    }


    private void handleExportCompletion(boolean captureSuccess) {
        if (!isExporting && exportProgressDialog == null) {
            logger.debug("handleExportCompletion called but export not active and dialog is null. Ignoring.");
            return;
        }
        if (!isExporting) {
            logger.info("handleExportCompletion called but export already marked as not running (isExporting=false). Triggering cleanup.");
            Platform.runLater(() -> cleanupExport(exportProgressDialog, exportOriginalFrame, exportWasPlaying));
            return;
        }

        if (!captureSuccess) {
            logger.info("Capture phase incomplete or cancelled.");
            Platform.runLater(() -> {
                if (exportProgressDialog != null && exportProgressDialog.isShowing()) {
                    exportProgressDialog.setError("Export Cancelled");
                }
                isExporting = false;
                cleanupExport(exportProgressDialog, exportOriginalFrame, exportWasPlaying);
            });
            return;
        }

        if (exportFrames == null || exportFrames.isEmpty()) {
            logger.warn("Export finished but no frames were captured.");
            Platform.runLater(() -> {
                if (exportProgressDialog != null && exportProgressDialog.isShowing()) {
                    exportProgressDialog.setError("Export Failed: No frames captured.");
                }
                isExporting = false;
                cleanupExport(exportProgressDialog, exportOriginalFrame, exportWasPlaying);
            });
            return;
        }

        if (exportProgressDialog != null && exportProgressDialog.isShowing()) {
            exportProgressDialog.updateProgress(exportProgressDialog.getMaxProgress(), "Saving " + exportFormat.toUpperCase() + " file...");
        }

        new Thread(() -> {
            boolean saveSuccess = false;
            String finalMessage = "Export failed during save.";
            try {
                logger.info("Starting background save for format: {}", exportFormat);
                switch (exportFormat) {
                    case "gif":
                        saveAsGif(exportOutputFile);
                        finalMessage = "GIF Export completed successfully!";
                        saveSuccess = true;
                        break;
                    case "mp4":
                        saveAsMp4(exportOutputFile);
                        finalMessage = "MP4 Export completed successfully!";
                        saveSuccess = true;
                        break;
                    case "png_sequence":
                        saveAsPngSequence(exportOutputFile);
                        finalMessage = "PNG Sequence Export completed successfully!";
                        saveSuccess = true;
                        break;
                    default:
                        logger.error("Unknown export format encountered during save: {}", exportFormat);
                        finalMessage = "Export Failed: Unknown format.";
                        break;
                }
                if (saveSuccess) logger.info("Save successful for: {}", exportOutputFile.getAbsolutePath());

            } catch (Exception e) {
                logger.error("Failed to save exported file '{}': {}", exportFormat, e.getMessage(), e);
                finalMessage = "Export failed: " + e.getMessage();
                saveSuccess = false;
            } finally {
                boolean finalSaveSuccess = saveSuccess;
                String finalUiMessage = finalMessage;
                Platform.runLater(() -> {
                    if (exportProgressDialog != null && exportProgressDialog.isShowing()) {
                        if (finalSaveSuccess) {
                            exportProgressDialog.setComplete(finalUiMessage);
                        } else {
                            exportProgressDialog.setError(finalUiMessage);
                        }
                    }
                    isExporting = false;
                    cleanupExport(exportProgressDialog, exportOriginalFrame, exportWasPlaying);
                });
            }
        }).start();
    }

    private void saveAsPngSequence(File outputDirectory) throws IOException {
        if (exportFrames == null || exportFrames.isEmpty())
            throw new IOException("No frames to save for PNG sequence.");
        if (!outputDirectory.isDirectory()) throw new IOException("Output path is not a directory: " + outputDirectory);

        int frameCount = exportFrames.size();
        int padding = Math.max(3, String.valueOf(frameCount).length());
        logger.info("Saving {} frames as PNG sequence to {}", frameCount, outputDirectory.getAbsolutePath());

        for (int i = 0; i < frameCount; i++) {
            if (!isExporting) {
                logger.warn("PNG sequence save cancelled during write loop.");
                throw new IOException("Export cancelled during PNG save.");
            }
            BufferedImage frameImage = exportFrames.get(i);
            String filename = String.format("frame_%0" + padding + "d.png", i + 1);
            File outputFile = new File(outputDirectory, filename);
            if (!ImageIO.write(frameImage, "png", outputFile)) {
                logger.error("ImageIO failed to write PNG file: {}", outputFile.getAbsolutePath());
                throw new IOException("Failed to write PNG frame: " + filename);
            }
            final int progressVal = exportProgressDialog != null ? exportProgressDialog.minProgress + i + 1 : 0;
            final String msg = "Saving " + filename;
            if (exportProgressDialog != null) {
                Platform.runLater(() -> {
                    if (this.exportProgressDialog != null && this.exportProgressDialog.isShowing()) {
                        this.exportProgressDialog.updateProgress(progressVal, msg);
                    }
                });
            }
        }
        logger.info("Finished saving PNG sequence.");
    }

    private void cleanupExport(ExportProgressDialog progressDialog, int originalFrame, boolean wasPlaying) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> cleanupExport(progressDialog, originalFrame, wasPlaying));
            return;
        }
        logger.debug("Cleaning up export state. Original Frame: {}, Was Playing: {}", originalFrame, wasPlaying);

        if (isExporting) {
            isExporting = false;
            exportFrames = null;
            exportOutputFile = null;
            exportFormat = null;
        }

        this.exportProgressDialog = null;

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.closeAfterDelay(1500);
        }

        logger.debug("Restoring engine to frame {}", originalFrame);
        engine.goToFrame(originalFrame);

        if (wasPlaying) {
            logger.debug("Resuming engine playback.");
            engine.play();
        } else {
            logger.debug("Showing editor controls after export cleanup.");
            animationUpdateListener.showEditorControls();
        }
        logger.debug("Export cleanup complete on FX thread.");
    }

    private void saveAsGif(File outputFile) throws IOException {
        if (exportFrames == null || exportFrames.isEmpty()) throw new IOException("No frames to export");
        int delayMs = 1000 / engine.getFps();
        ImageOutputStream output = new FileImageOutputStream(outputFile);
        GifSequenceWriter writer = null;
        try {
            int imageType = exportFrames.get(0).getType();
            if (imageType == 0) {
                logger.warn("Detected custom/unknown BufferedImage type {}, defaulting to TYPE_INT_ARGB for GIF.", imageType);
                imageType = BufferedImage.TYPE_INT_ARGB;
            }

            writer = new GifSequenceWriter(output, imageType, delayMs, true);
            for (BufferedImage frame : exportFrames) {
                if (!isExporting) throw new IOException("Export cancelled during GIF save.");
                writer.writeToSequence(frame);
            }
        } finally {
            if (writer != null) writer.close();
            output.close();
        }
    }

    private void saveAsMp4(File outputFile) throws IOException {
        if (exportFrames == null || exportFrames.isEmpty()) {
            throw new IOException("No frames captured to export as MP4");
        }
        MP4Encoder mp4Encoder = null;
        try {
            int width = exportFrames.get(0).getWidth();
            int height = exportFrames.get(0).getHeight();
            mp4Encoder = new MP4Encoder(outputFile.getAbsolutePath(), width, height, engine.getFps());

            for (BufferedImage frame : exportFrames) {
                if (!isExporting) throw new IOException("Export cancelled during MP4 frame processing.");
                if (frame != null) mp4Encoder.addFrame(frame);
                else logger.warn("Encountered null BufferedImage in exportFrames list. Skipping.");
            }

            if (!isExporting) throw new IOException("Export cancelled before finalizing MP4.");

            mp4Encoder.finish();

        } catch (Exception e) {
            logger.error("MP4 encoding process failed: {}", e.getMessage(), e);
            if (mp4Encoder != null) {
                try { mp4Encoder.finish(); } catch (Exception ignored) {}
            }
            if (e instanceof IOException) throw (IOException) e;
            else throw new IOException("Failed to create MP4 file: " + e.getMessage(), e);
        }
    }

    public void cancelExport() {
        if (isExporting) {
            logger.info("Cancel export requested.");
            isExporting = false;

            Platform.runLater(() -> {
                if (exportProgressDialog != null && exportProgressDialog.isShowing()) {
                    exportProgressDialog.setError("Cancelling...");
                }
            });
        } else {
            logger.debug("Cancel export requested, but not currently exporting.");
        }
    }

    public void setCanvas(Canvas canvas) {
        logger.debug("ExportManager: Canvas set. Canvas is null? {}", (canvas == null));
        this.canvas = canvas;
    }

    public void setListener(AnimationUpdateListener listener) {
        logger.debug("ExportManager: Listener set.");
        this.animationUpdateListener = listener;
    }

}