
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Ue04 extends JPanel {

	private static final String name = "Viet Tu Hoang";

	private static final long serialVersionUID = 1L;
	private static final int borderWidth = 5;
	private static final int maxWidth = 400;
	private static final int maxHeight = maxWidth;
	private static final String initialFilename = "C:/Users/s0547648/Pictures/test2.jpg";
	private File openPath = new File(".");

	private static JFrame frame;
	private JLabel cascadeSliderLabel;
	private JSlider cascadeSlider;

	private JLabel origEntropyLabel;
	private JLabel preProcessedEntropyLabel;
	private JLabel DecodedEntropyLabel;
	private JLabel sizeAndMSELabel;

	private ImageView srcView; // source image view
	private ImageView preProcessedView; // reconstructed image view
	private ImageView decodedView;

	private JSlider resolutionSlider;

	private enum FileType {
		NORMAL, ARI
	}

	private enum DialogType {
		OPEN, SAVE
	}

	private final static String fileConstant = "";
	private final static String fileExtension = "ari";

	public Ue04() {
		super(new BorderLayout(borderWidth, borderWidth));

		setBorder(BorderFactory.createEmptyBorder(borderWidth, borderWidth, borderWidth, borderWidth));

		File input = new File(initialFilename);
		if (!input.canRead())
			input = fileDialog(DialogType.OPEN, FileType.NORMAL); // file not
		srcView = new ImageView();
		preProcessedView = new ImageView();
		decodedView = new ImageView();

		NumberFormat.getInstance().setMaximumFractionDigits(3);

		JPanel controls = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0, 5, 0, 5);

		JButton loadSrc = new JButton("Open Source Image");
		loadSrc.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loadSrcFile(fileDialog(DialogType.OPEN, FileType.NORMAL));
			}
		});

		JButton loadImage = new JButton("Open " + fileConstant + " Image");
		loadImage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loadImageFile(fileDialog(DialogType.OPEN, FileType.ARI));
			}
		});

		cascadeSliderLabel = new JLabel();
		final JLabel cascadeSliderLabelNumber = new JLabel();
		JLabel resolutionSliderLabel = new JLabel();
		final JLabel resolutionSliderLabelNumber = new JLabel();

		cascadeSlider = new JSlider(0, 5, 0);
		resolutionSlider = new JSlider(0, 100, 0);

		cascadeSliderLabel.setText("Kaskaden:");
		cascadeSliderLabelNumber.setText("" + cascadeSlider.getValue());
		resolutionSliderLabel.setText("Auflösungsstufe:");
		resolutionSliderLabelNumber.setText("" + resolutionSlider.getValue());

		cascadeSlider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				cascadeSliderLabelNumber.setText("" + cascadeSlider.getValue());
			}
		});

		resolutionSlider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				resolutionSliderLabelNumber.setText("" + resolutionSlider.getValue());
			}
		});

		controls.add(loadSrc, c);
		controls.add(cascadeSliderLabel, c);
		controls.add(cascadeSlider, c);
		controls.add(cascadeSliderLabelNumber, c);
		controls.add(resolutionSliderLabel, c);
		controls.add(resolutionSlider, c);
		controls.add(resolutionSliderLabelNumber, c);

		JPanel images = new JPanel(new GridLayout(1, 3));
		images.add(srcView);
		images.add(preProcessedView);
		images.add(decodedView);

		JPanel status = new JPanel(new GridBagLayout());

		add(controls, BorderLayout.NORTH);
		add(images, BorderLayout.CENTER);
		add(status, BorderLayout.SOUTH);

		loadSrcFile(input);
		grayScale(srcView);
		// binarisePreProcessedView(slider.getValue());
		// entropyForAllImageViews();
		encode();
		decode();
	}

	private class Cascade {

		double[] lowLowPass;
		double[] lowHighPass;
		double[] highLowPass;
		double[] highPass;

		int depth;// ?

		public Cascade(int length, int depth) {
			lowLowPass = lowHighPass = highLowPass = highPass = new double[length / 4];
			this.depth = depth;
		}

		private void transform(double[] srcPixels) {
			double[] lowPass, highPass;
			lowPass = highPass = new double[lowLowPass.length * 2];

			for (int i = 1; i < lowPass.length; i++) {
				double value = 6 * srcPixels[i * 2] + 2 * srcPixels[i * 2 + 1] + 2 * srcPixels[i * 2 - 1]
						+ (-1 * srcPixels[i * 2 - 2]) + (-1 * srcPixels[i * 2 + 2]);
				lowPass[i] = value;
			}

			// Randbehandlung lp
			lowPass[0] = 6 * srcPixels[0 * 2] + 2 * (2 * srcPixels[0 * 2 + 1]) + 2 * (-(srcPixels[0 * 2 + 2]));
			lowPass[lowPass.length - 1] = 6 * srcPixels[srcPixels.length - 2] + 2 * (srcPixels[srcPixels.length - 3])
					+ 2 * (srcPixels[srcPixels.length - 1]) + 2 * (-(srcPixels[srcPixels.length - 4]));

			for (int i = 0; i < highPass.length-1; i++) {
				highPass[i] = 2 * srcPixels[i * 2 + 1] - srcPixels[i * 2] - srcPixels[i * 2 + 3];
			}
			
			//Randbehandlung hp 
			highPass[highPass.length-1] = 2 * srcPixels[highPass.length-1 * 2 + 1] - srcPixels[highPass.length-1 * 2];
			
			
		}

	}

	private void decode() {

	}

	private void encode() {

	}

	// private int[] binarise(int[] srcPix, int threshold) {
	// int[] binarised = new int[srcPix.length];
	// int max = 255;
	// for (int i = 0; i < binarised.length; i++) {
	// binarised[i] = (srcPix[i] & 0xFF) < threshold ? 0 : (0xFF << 24) | (max
	// << 16) | (max << 8) | max;
	// }
	// return binarised;
	// }

	private void entropyForAllImageViews() {
		setOrigEntropyLabel();
		setPreprocessedLabel();
		setDecodedLabel();
	}

	private double mse() {
		int errorSumSquared = 0;
		int[] origPix = srcView.getPixels();
		int[] decodedPix = decodedView.getPixels();
		for (int i = 0; i < origPix.length; i++) {
			int error = origPix[i] - decodedPix[i];
			errorSumSquared += error * error;
		}

		return 1.0 / origPix.length * errorSumSquared;

	}

	private void setDecodedLabel() {
		DecodedEntropyLabel
				.setText("Entropy right: " + NumberFormat.getInstance().format(entropy(decodedView.getPixels())));
	}

	private void setOrigEntropyLabel() {
		origEntropyLabel.setText("Entropy left: " + NumberFormat.getInstance().format(entropy(srcView.getPixels())));

	}

	private void setPreprocessedLabel() {
		preProcessedEntropyLabel
				.setText("Entropy middle: " + NumberFormat.getInstance().format(entropy(preProcessedView.getPixels())));

	}

	private double entropy(int[] pixels) {
		int count = 0;
		int[] histogram = histogramOfImage(pixels);

		for (int i = 0; i < histogram.length; i++) {
			count += histogram[i];
		}
		if (count == 0) {
			count = 1;
		}
		double entropy = 0.0;
		// int meanSquaredError = 0;
		for (int i = 0; i < histogram.length; i++) {
			if (histogram[i] != 0) {
				double propability = (double) histogram[i] / count;
				// meanSquaredError = meanSquaredError + (histogram[i] * ((i) *
				// (i)));
				entropy -= propability * (Math.log(propability) / Math.log(2));
			}
		}
		return entropy;
	}

	private int[] histogramOfImage(int[] pixels) {
		int[] histogram = new int[255];
		for (int i : pixels) {
			histogram[i & 0xFF]++;
		}
		return histogram;
	}

	private double getProbability(int[] binarisedImage) {
		int numberOfBlackPixels = 0;
		for (int i = 0; i < binarisedImage.length; i++) {
			if ((i & 0xFF) == 0) {
				numberOfBlackPixels++;
			}
		}
		return ((double) numberOfBlackPixels) / binarisedImage.length;
	}

	private int getAvg(int[] histogram) {
		int avg = 0;
		int sum1 = 0;
		int sum2 = 0;
		for (int i = 0; i < histogram.length; i++) {
			sum1 = sum1 + i * histogram[i];
			sum2 = sum2 + histogram[i];
		}
		avg = sum1 / sum2;
		return (int) avg;
	}

	private void grayScale(ImageView image) {

		int pixels[] = image.getPixels();
		// loop over all pixels
		for (int i = 0; i < pixels.length; i++) {

			int argb = pixels[i];
			int r = (argb >> 16) & 0xFF;
			int g = (argb >> 8) & 0xFF;
			int b = argb & 0xFF;

			int gray = (r + 2 * g + b) / 4;
			if (gray > 255)
				gray = 255;
			else if (gray < 0)
				gray = 0;
			pixels[i] = (0xFF << 24) | (gray << 16) | (gray << 8) | gray;
		}
		image.applyChanges();
	}

	private File fileDialog(DialogType dType, FileType fType) {
		JFileChooser chooser = new JFileChooser() {
			private static final long serialVersionUID = 1L;

			@Override
			public void approveSelection() {
				File file = getSelectedFile();
				if (file.exists() && getDialogType() == SAVE_DIALOG) {
					int result = JOptionPane.showConfirmDialog(this, "Overwrite existing file?", "Existing file",
							JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
					switch (result) {
					case JOptionPane.YES_OPTION:
						super.approveSelection();
						return;
					default:
						return;
					}
				}
				super.approveSelection();
			}
		};

		FileNameExtensionFilter filter;
		filter = new FileNameExtensionFilter("Images (*.jpg, *.png, *.gif)", "jpg", "png", "gif");
		chooser.setFileFilter(filter);
		chooser.setCurrentDirectory(openPath);

		int ret;
		if (dType == DialogType.OPEN)
			ret = chooser.showOpenDialog(this);
		else
			ret = chooser.showSaveDialog(this);

		if (ret == JFileChooser.APPROVE_OPTION) {
			openPath = chooser.getSelectedFile().getParentFile();
			return chooser.getSelectedFile();
		}

		return null;
	}

	private void loadSrcFile(File file) {
		if (file == null)
			return;

		srcView.loadImage(file);
		srcView.setMaxSize(new Dimension(maxWidth, maxHeight));

		// create empty destination images
		preProcessedView.resetToSize(srcView.getImgWidth(), srcView.getImgHeight());
		decodedView.resetToSize(srcView.getImgWidth(), srcView.getImgHeight());

		// displayMse();

		frame.pack();
	}

	private void loadImageFile(File file) {
		if (file == null)
			return;

		try {
			BitInputStream in = new BitInputStream(new FileInputStream(file));
			// decodeImage(in);
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		preProcessedView.applyChanges();
		preProcessedView.setMaxSize(new Dimension(maxWidth, maxHeight));

		// displayMse();

		frame.pack();
	}

	// private void saveImageFile(File file) {
	// if (file == null)
	// return;
	//
	// try {
	// BitOutputStream out = new BitOutputStream(new FileOutputStream(file));
	// encodeImage(out);
	// out.close();
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }

	private static void createAndShowGUI() {
		// create and setup the window
		frame = new JFrame(fileConstant + " " + name);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JComponent newContentPane = new Ue04();
		newContentPane.setOpaque(true); // content panes must be opaque
		frame.setContentPane(newContentPane);

		// display the window.
		frame.pack();
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Dimension screenSize = toolkit.getScreenSize();
		frame.setLocation((screenSize.width - frame.getWidth()) / 2, (screenSize.height - frame.getHeight()) / 2);
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}

	private void updateSizeAndMSELabel(int size) {
		sizeAndMSELabel.setText("Size: " + Math.round(size / 8 / 1024) + "kB " + String.format("MSE = %.1f", mse()));
	}

	// private void binarisePreProcessedView(int threshold) {
	// preProcessedView.setPixels(binarise(srcView.getPixels(), threshold));
	// }
}
