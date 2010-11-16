// Defines the size and location of user interface for QAA

package org.esa.beam.visat.processor.quasi;

import org.esa.beam.util.Guardian;

import java.awt.Rectangle;

public class AnalyticalFrameSizeCalculator {

    private int sceneWidth;
    private int sceneHeight;
    private int maxHeight;

    public AnalyticalFrameSizeCalculator(final int width, final int height) {
        sceneWidth = width;
        sceneHeight = height;
        if (height < 240) {
            maxHeight = 1;
        } else {
            maxHeight = 16;
        }
    }

    public void addMinFrameSize(final int width, final int height) {
        Guardian.assertWithinRange("width", width, 0, sceneWidth);
        Guardian.assertWithinRange("height", height, 0, sceneHeight);

        if (maxHeight % height != 0 && (maxHeight * height <= sceneHeight)) {
            maxHeight *= height;
        }
    }

    public Rectangle getMaxFrameSize() {
        return new Rectangle(sceneWidth, maxHeight);
    }

    public int getFrameCount() {
        int frameCount = sceneHeight / maxHeight;
        if (sceneHeight % maxHeight != 0) {
            frameCount++;
        }
        return frameCount;
    }

    public Rectangle getFrameRect(final int frameNumber) {
        Guardian.assertWithinRange("frameNumber", frameNumber, 0, getFrameCount() - 1);

        final int frameHeight;
        if ((frameNumber + 1) * maxHeight > sceneHeight) {
            frameHeight = sceneHeight % maxHeight;
        } else {
            frameHeight = maxHeight;
        }
        return new Rectangle(0, frameNumber * maxHeight, sceneWidth, frameHeight);
    }
}