package uk.ac.soton.comp3200.model.io.export.format;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Encodes a sequence of frames into an MP4 video file using JavaCV (FFmpeg).
 */
public class MP4Encoder {
    private static final Logger logger = LogManager.getLogger(MP4Encoder.class);

    private final String outputPath;

    private FFmpegFrameRecorder recorder = null;
    private Java2DFrameConverter converter;
    private boolean initialized;
    private boolean finished = false;

    /**
     * Creates a new MP4 encoder using FFmpeg via JavaCV.
     *
     * @param outputPath The path to the output MP4 file.
     * @param width      The width of the video.
     * @param height     The height of the video.
     * @param frameRate  The frame rate of the video.
     * @throws FrameRecorder.Exception If recorder initialization fails.
     */
    public MP4Encoder(String outputPath, int width, int height, int frameRate) throws FrameRecorder.Exception {
        this.outputPath = outputPath;
        int width1 = (width % 2 == 0) ? width : width + 1;
        int height1 = (height % 2 == 0) ? height : height + 1;

        logger.info("Initializing MP4 encoder for file: {}", outputPath);
        logger.info("Resolution: {}x{}, Frame rate: {}", width1, height1, frameRate);

        try {
            recorder = new FFmpegFrameRecorder(outputPath, width1, height1, 0);

            recorder.setFormat("mp4");
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);

            recorder.setVideoQuality(18);
            recorder.setVideoOption("preset", "medium");
            recorder.setFrameRate(frameRate);
            recorder.setGopSize(frameRate * 2);

            recorder.start();
            logger.info("FFmpegFrameRecorder started successfully.");

            converter = new Java2DFrameConverter();
            initialized = true;

        } catch (FrameRecorder.Exception e) {
            logger.error("Failed to initialize FFmpegFrameRecorder", e);
            if (recorder != null) {
                try {
                    recorder.release();
                } catch (FrameRecorder.Exception ex) {
                    logger.error("Error releasing recorder after init failure", ex);
                }
            }
            throw e;
        }
    }

    /**
     * Adds a frame (as a BufferedImage) to the MP4 video.
     * Forces conversion to TYPE_3BYTE_BGR format before recording.
     *
     * @param frame The frame to add.
     * @throws IOException           If conversion or recording fails.
     * @throws IllegalStateException If encoder is not initialized or already finished.
     */
    public void addFrame(BufferedImage frame) throws IOException {
        if (!initialized) {
            throw new IllegalStateException("MP4 encoder not initialized.");
        }
        if (finished) {
            throw new IllegalStateException("MP4 encoder already finished.");
        }
        if (frame == null) {
            logger.warn("Attempted to add a null frame. Skipping.");
            return;
        }

        try {
            BufferedImage convertedBgrFrame;

            if (frame.getType() == BufferedImage.TYPE_3BYTE_BGR) {
                convertedBgrFrame = frame;
            } else {
                convertedBgrFrame = new BufferedImage(
                        frame.getWidth(),
                        frame.getHeight(),
                        BufferedImage.TYPE_3BYTE_BGR);

                Graphics2D g2d = convertedBgrFrame.createGraphics();
                g2d.drawImage(frame, 0, 0, null);
                g2d.dispose();
                logger.trace("Converted frame from type {} to TYPE_3BYTE_BGR", frame.getType());
            }

            Frame convertedFrame = converter.convert(convertedBgrFrame);
            if (convertedFrame == null) {
                logger.warn("Frame conversion returned null after BGR conversion. Skipping frame.");
                return;
            }

            recorder.record(convertedFrame);
            logger.trace("Recorded frame successfully (as BGR).");

        } catch (FrameRecorder.Exception e) {
            logger.error("Error recording frame", e);
            throw new IOException("Failed to record frame to MP4 file.", e);
        } catch (Exception e) {
            logger.error("Unexpected error during frame conversion/drawing", e);
            throw new IOException("Failed processing frame before recording.", e);
        }
    }

    /**
     * Finishes encoding, closes the recorder, and releases resources.
     * This method MUST be called to finalize the video file.
     *
     * @throws IOException If stopping or releasing the recorder fails.
     */
    public void finish() throws IOException {
        if (!initialized || finished) {
            logger.warn("finish() called on MP4 encoder that is not initialized or already finished.");
            return;
        }

        logger.info("Finishing MP4 encoding for: {}", outputPath);
        finished = true;
        initialized = false;

        try {
            if (recorder != null) {
                recorder.stop();
                recorder.release();
                logger.info("FFmpegFrameRecorder stopped and released.");
            }
            if (converter != null) {
                converter = null;
            }
        } catch (FrameRecorder.Exception e) {
            logger.error("Error stopping or releasing FFmpegFrameRecorder", e);
            throw new IOException("Failed to finalize MP4 file.", e);
        } finally {
            recorder = null;
            converter = null;
        }
    }
}