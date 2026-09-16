package uk.ac.soton.comp3200.model.io.export.format;

import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.*;
import java.awt.image.*;
import java.io.*;
import java.util.Iterator;

/**
 * Creates a GIF animation sequence using the standard Java ImageIO libraries.
 * Based on code by Elliot Kroo: https://stackoverflow.com/questions/16649620/is-there-a-way-to-create-one-gif-image-from-multiple-images-in-java/16649681
 * Original work licensed under the Creative Commons Attribution 3.0 Unported License (http://creativecommons.org/licenses/by/3.0/).
 * Code adapted and slightly refactored for integration into this project.
 */
public class GifSequenceWriter {
    protected ImageWriter gifWriter;
    protected ImageWriteParam imageWriteParam;
    protected IIOMetadata imageMetaData;

    /**
     * Creates a new GifSequenceWriter
     *
     * @param outputStream the ImageOutputStream to be written to
     * @param imageType one of the imageTypes specified in BufferedImage
     * @param timeBetweenFramesMS the time between frames in milliseconds
     * @param loopContinuously whether the gif should loop repeatedly
     * @throws IIOException if no gif ImageWriters are found
     */
    public GifSequenceWriter(
            ImageOutputStream outputStream,
            int imageType,
            int timeBetweenFramesMS,
            boolean loopContinuously) throws IOException {

        gifWriter = getWriter();
        imageWriteParam = gifWriter.getDefaultWriteParam();

        ImageTypeSpecifier imageTypeSpecifier =
                ImageTypeSpecifier.createFromBufferedImageType(imageType);

        imageMetaData = gifWriter.getDefaultImageMetadata(imageTypeSpecifier, imageWriteParam);

        configureGifMetadata(imageMetaData, timeBetweenFramesMS, loopContinuously);

        gifWriter.setOutput(outputStream);
        gifWriter.prepareWriteSequence(null);
    }

    /**
     * Retrieves an ImageWriter for GIF format
     */
    private static ImageWriter getWriter() throws IIOException {
        for (Iterator<ImageWriter> it = ImageIO.getImageWritersBySuffix("gif"); it.hasNext(); ) {
            return it.next();
        }
        throw new IIOException("No GIF Image Writers Found");
    }

    /**
     * Configures the GIF metadata for frame delay and looping
     */
    private static void configureGifMetadata(
            IIOMetadata metadata,
            int timeBetweenFramesMS,
            boolean loopContinuously) throws IIOInvalidTreeException {

        String metaFormatName = metadata.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metaFormatName);

        IIOMetadataNode gce = findOrCreateNode(root, "GraphicControlExtension");
        gce.setAttribute("disposalMethod", "none");
        gce.setAttribute("userInputFlag", "FALSE");
        gce.setAttribute("transparentColorFlag", "FALSE");
        gce.setAttribute("delayTime", Integer.toString(timeBetweenFramesMS / 10)); // In 1/100 sec
        gce.setAttribute("transparentColorIndex", "0");

        if (loopContinuously) {
            IIOMetadataNode aes = findOrCreateNode(root, "ApplicationExtensions");
            IIOMetadataNode ae = new IIOMetadataNode("ApplicationExtension");
            ae.setAttribute("applicationID", "NETSCAPE");
            ae.setAttribute("authenticationCode", "2.0");

            byte[] loopBytes = new byte[] { 1, 0, 0 };
            ae.setUserObject(loopBytes);
            aes.appendChild(ae);
        }

        metadata.setFromTree(metaFormatName, root);
    }

    /**
     * Finds a named node in the tree, or creates it if it doesn't exist
     */
    private static IIOMetadataNode findOrCreateNode(IIOMetadataNode rootNode, String nodeName) {
        for (int i = 0; i < rootNode.getLength(); i++) {
            if (rootNode.item(i).getNodeName().equalsIgnoreCase(nodeName)) {
                return (IIOMetadataNode) rootNode.item(i);
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(nodeName);
        rootNode.appendChild(node);
        return node;
    }

    /**
     * Adds a frame to the GIF sequence
     */
    public void writeToSequence(RenderedImage img) throws IOException {
        gifWriter.writeToSequence(new IIOImage(img, null, imageMetaData), imageWriteParam);
    }

    /**
     * Closes the GIF writer
     */
    public void close() throws IOException {
        gifWriter.endWriteSequence();
    }
}