package org.esa.beam.visat.processor.quasi;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.util.StringUtils;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;

public abstract class ProcessingNode implements ProductReader {

	private Product sourceProduct;
	private Product targetProduct;
	private Rectangle frameRect;
	private Object input;
	private Map frameDataMap;
	private AnalyticalFrameSizeCalculator fsc;

	public ProcessingNode() {
		frameDataMap = new HashMap(31);
	}

	public void close() throws IOException {
		frameDataMap.clear();
	}

	public Product getTargetProduct() {
		return targetProduct;
	}

	public Product getSourceProduct() {
		return sourceProduct;
	}

	public Object getInput() {
		return input;
	}

	public void readBandRasterData(final Band targetBand, final int x,
			final int y, final int w, final int h,
			final ProductData targetData, ProgressMonitor pm)
			throws IOException {
		readFrameData(targetBand, x, y, w, h, targetData);
	}

	public Product readProductNodes(Object input, ProductSubsetDef subsetDef)
			throws IOException {
		if (subsetDef != null)
			throw new IllegalArgumentException(
					"ProductSubsetDefs are not supported.");
		if (input == null) {
			sourceProduct = null;
		} else if (input instanceof Product) {
			sourceProduct = (Product) input;
		} else if (input instanceof Product[]) {
			sourceProduct = ((Product[]) input)[0];
		} else {
			throw new IllegalArgumentException("Unsupported input type.");
		}
		this.input = input;
		return createTargetProduct();
	}

	public ProductData getFrameData(final Band targetband) {
		if (frameRect == null)
			return null;
		ProductData frameData = (ProductData) frameDataMap.get(targetband);
		final int numElems = frameRect.width * frameRect.height;
		if (frameData == null || frameData.getNumElems() != numElems) {
			frameData = targetband.createCompatibleProductData(numElems);
			frameDataMap.put(targetband, frameData);
		}
		return frameData;
	}

	private Product createTargetProduct() {
		targetProduct = createTargetProductImpl();
		targetProduct.setProductReader(this);
		return targetProduct;
	}

	private void copyFrameData(final ProductData sourceData,
			final ProductData targetData, final int targetX, final int targetY,
			final int targetW, final int targetH) throws IOException {
		final Object sourceElems = sourceData.getElems();
		final Object targetElems = targetData.getElems();
		final int targetNumElems = targetData.getNumElems();
		if (sourceElems.getClass().equals(targetElems.getClass())) {
			if (frameRect.x == targetX && frameRect.y == targetY
					&& frameRect.width == targetW
					&& frameRect.height == targetH) {
				System.arraycopy(sourceElems, 0, targetElems, 0,
								targetNumElems);
			} else {
				final int offsetY = targetY - frameRect.y;
				int sourceIndex = frameRect.width * offsetY + targetX;
				int targetIndex = 0;
				for (int y = offsetY; y < offsetY + targetH; y++) {
					System.arraycopy(sourceElems, sourceIndex, targetElems,
							targetIndex, targetW);
					sourceIndex += frameRect.width;
					targetIndex += targetW;
				}
			}
		} else {
            System.out.println("ERROR: not supported !!!!!");
            throw new IOException("unsupported type conversion");
		}
	}

	private synchronized void readFrameData(final Band targetBand, final int x,
			final int y, final int w, final int h, final ProductData targetData)
			throws IOException {
		if (isNewFrame(x, y, w, h)) {
			setFrameRectangle(x, y, w, h);
			processFrame(x, y, w, h, ProgressMonitor.NULL);
		}
		final ProductData frameData = getFrameData(targetBand);
		copyFrameData(frameData, targetData, x, y, w, h);
	}

	private void setFrameRectangle(final int x, final int y, final int w,
			final int h) {
		frameRect = new Rectangle(x, y, w, h);
	}

	private boolean isNewFrame(final int x, final int y, final int w,
			final int h) {
		return frameRect == null || !frameRect.contains(x, y, w, h);
	}

	public void setFrameSizeCalculator(
			final AnalyticalFrameSizeCalculator frameCalc) {
		fsc = frameCalc;
		final Rectangle minRect = getMinFrameSize();
		fsc.addMinFrameSize(minRect.width, minRect.height);
	}

	public Rectangle getMinFrameSize() {
		return new Rectangle(1, 1);
	}

	public static void copyBandData(Band[] sourceBands, Product destProduct,
			Rectangle frameRect, ProgressMonitor pm) throws IOException {
		pm.beginTask("Copying band data...", sourceBands.length);
		try {
            for (Band sourceBand : sourceBands) {
                copyBandData(sourceBand, destProduct, frameRect,
                             SubProgressMonitor.create(pm, 1));
            }
		} finally {
			pm.done();
		}
	}

	public static void copyBandData(Band sourceBand, Product destProduct,
			Rectangle frameRect, ProgressMonitor pm) throws IOException {
		Band destBand = destProduct.getBand(sourceBand.getName());
		ProductData data = sourceBand
				.createCompatibleProductData(frameRect.width * frameRect.height);
		SubProgressMonitor subPm = new SubProgressMonitor(pm, 1);
		subPm.beginTask("Copying data of band '" + sourceBand.getName() + "'...",
				2);
		try {
			sourceBand.readRasterData(frameRect.x, frameRect.y,
					frameRect.width, frameRect.height, data, SubProgressMonitor
							.create(pm, 1));

			destBand.writeRasterData(frameRect.x, frameRect.y, frameRect.width,
					frameRect.height, data, SubProgressMonitor.create(pm, 1));
		} finally {
			subPm.done();
		}
	}

	static public void initWriter(ProductRef productRef, Product outputProduct,
			Logger logger) throws ProcessorException, IOException {
		if (productRef == null) {
			throw new ProcessorException("No output product in request");
		}
		File outputFile = new File(productRef.getFilePath());
		String outputFileFormat = productRef.getFileFormat();
		if (StringUtils.isNullOrEmpty(outputFileFormat)) {
			outputFileFormat = DimapProductConstants.DIMAP_FORMAT_NAME;
			logger.warning(ProcessorConstants.LOG_MSG_NO_OUTPUT_FORMAT);
			logger.warning(ProcessorConstants.LOG_MSG_USING + outputFileFormat);
		}
		ProductWriter outputWriter = ProductIO
				.getProductWriter(outputFileFormat);
		if (outputWriter == null) {
			throw new ProcessorException("Invalid output product format: "
					+ outputFileFormat);
		}
		outputProduct.setProductWriter(outputWriter);
		outputWriter.writeProductNodes(outputProduct, outputFile);
	}

	protected abstract Product createTargetProductImpl();

	protected abstract void processFrame(int x, int y, int w, int h,
			ProgressMonitor pm) throws IOException;

	/**
	 * ProductSubsetDef are not supported.
	 */
	public final ProductSubsetDef getSubsetDef() {
		return null;
	}

	public ProductReaderPlugIn getReaderPlugIn() {
		return null;
	}
}
