
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

public class Arith extends JPanel {

	private static final String name = "Viet Tu Hoang";

	private static final long serialVersionUID = 1L;
	private static final int borderWidth = 5;
	private static final int maxWidth = 400;
	private static final int maxHeight = maxWidth;
	private static final String initialFilename = "rhino_part.png";
	private File openPath = new File(".");

	private static JFrame frame;
	private JLabel sliderLabel;
	private JSlider slider;

	private JLabel origEntropyLabel;
	private JLabel preProcessedEntropyLabel;
	private JLabel DecodedEntropyLabel;
	private JLabel sizeAndMSELabel;

	private ImageView srcView; // source image view
	private ImageView preProcessedView; // reconstructed image view
	private ImageView arithView;

	private enum FileType {
		NORMAL, ARI
	}

	private enum DialogType {
		OPEN, SAVE
	}

	private final static String fileConstant = "Arith";
	private final static String fileExtension = "ari";

	public Arith() {
		super(new BorderLayout(borderWidth, borderWidth));

		setBorder(BorderFactory.createEmptyBorder(borderWidth, borderWidth, borderWidth, borderWidth));

		File input = new File(initialFilename);
		if (!input.canRead())
			input = fileDialog(DialogType.OPEN, FileType.NORMAL); // file not
		srcView = new ImageView();
		preProcessedView = new ImageView();
		arithView = new ImageView();

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

		JButton saveImage = new JButton("Save " + fileConstant + " Image");
		saveImage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveImageFile(fileDialog(DialogType.SAVE, FileType.ARI));
			}
		});

		JButton loadImage = new JButton("Open " + fileConstant + " Image");
		loadImage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loadImageFile(fileDialog(DialogType.OPEN, FileType.ARI));
			}
		});

		sliderLabel = new JLabel();
		slider = new JSlider(0, 255, 1);
		slider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				int threshold = slider.getValue();
				sliderLabel.setText("T = " + threshold);
				binarisePreProcessedView(threshold);
			}
		});

		controls.add(loadSrc, c);
		controls.add(sliderLabel, c);
		controls.add(slider, c);
		controls.add(saveImage, c);
		controls.add(loadImage, c);

		JPanel images = new JPanel(new GridLayout(1, 3));
		images.add(srcView);
		images.add(preProcessedView);
		images.add(arithView);

		JPanel status = new JPanel(new GridBagLayout());
		origEntropyLabel = new JLabel(" ");
		preProcessedEntropyLabel = new JLabel(" ");
		DecodedEntropyLabel = new JLabel(" ");
		sizeAndMSELabel = new JLabel(" ");
		status.add(origEntropyLabel, c);
		status.add(preProcessedEntropyLabel, c);
		status.add(DecodedEntropyLabel, c);
		status.add(sizeAndMSELabel, c);

		add(controls, BorderLayout.NORTH);
		add(images, BorderLayout.CENTER);
		add(status, BorderLayout.SOUTH);

		loadSrcFile(input);
		grayScale(srcView);
		binarisePreProcessedView(slider.getValue());
		entropyForAllImageViews();
	}

	private int[] binarise(int[] srcPix, int threshold) {
		int[] binarised = new int[srcPix.length];
		int max = 255;
		for (int i = 0; i < binarised.length; i++) {
			binarised[i] = (srcPix[i] & 0xFF) < threshold ? 0 : (0xFF << 24) | (max << 16) | (max << 8) | max;
		}
		return binarised;
	}

	private void entropyForAllImageViews() {
		setOrigEntropyLabel();
		setPreprocessedLabel();
		setDecodedLabel();
	}

	private double mse() {
		int errorSumSquared = 0;
		int[] origPix = srcView.getPixels();
		int[] decodedPix = arithView.getPixels();
		for (int i = 0; i < origPix.length; i++) {
			int error = origPix[i] - decodedPix[i];
			errorSumSquared += error * error;
		}

		return 1.0 / origPix.length * errorSumSquared;

	}

	private void setDecodedLabel() {
		DecodedEntropyLabel
				.setText("Entropy right: " + NumberFormat.getInstance().format(entropy(arithView.getPixels())));
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
		if (fType == FileType.ARI)
			filter = new FileNameExtensionFilter(fileConstant + " Images (*." + fileExtension + ")", fileExtension);
		else
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
		arithView.resetToSize(srcView.getImgWidth(), srcView.getImgHeight());

		// displayMse();

		frame.pack();
	}

	private void loadImageFile(File file) {
		if (file == null)
			return;

		try {
			BitInputStream in = new BitInputStream(new FileInputStream(file));
			decodeImage(in);
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		preProcessedView.applyChanges();
		preProcessedView.setMaxSize(new Dimension(maxWidth, maxHeight));

		// displayMse();

		frame.pack();
	}

	private void saveImageFile(File file) {
		if (file == null)
			return;

		try {
			BitOutputStream out = new BitOutputStream(new FileOutputStream(file));
			encodeImage(out);
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void createAndShowGUI() {
		// create and setup the window
		frame = new JFrame(fileConstant + " " + name);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JComponent newContentPane = new Arith();
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

	private void encodeImage(BitOutputStream out) throws IOException {
		double probability = getProbability(preProcessedView.getPixels());
		int valueForFileFormat = ((int) probability & 0x7FFF);
		out.write(preProcessedView.getImgWidth(), 16);
		out.write(preProcessedView.getImgHeight(), 16);

		out.write(valueForFileFormat, 16);

		double lowerA, lowerB, upperA, upperB, x0, x1;
		lowerA = lowerB = x0 = 0.0;
		upperA = upperB = x1 = 1.0;

		for (int pix : preProcessedView.getPixels()) {
			// aktualisiere lowA,lowB
			// Grenzen checken
			// lowerA = 0.0;
			// upperA = 1.0;
			if (pix == 0) {
				upperA = upperA * probability;
			} else {
				lowerA = probability;
			}

			// skalierung
			while (true) {
				if (lowerA >= 0 && upperA <= 0.5) {
					upperA *= 2;
					upperB *= 2;
				} else if (lowerA >= 0.5 && upperA <= 1.0) {
					lowerA = 2 * (lowerA - 0.5);
					upperA = 2 * (upperA - 0.5);
					lowerB = 2 * (lowerB - 0.5);
					upperB = 2 * (upperB - 0.5);
				} else if (lowerA >= 0.25 && upperA <= 0.75) {
					lowerA = 2 * (lowerA - 0.25);
					upperA = 2 * (upperA - 0.25);
					lowerB = 2 * (lowerB - 0.25);
					upperB = 2 * (upperB - 0.25);
				} else {
					break;
				}
			}

			while (true) {
				double bHalfed = upperB / 2;
				if (upperA <= upperB && lowerA >= bHalfed) {
					out.write(1, 1);
					// aktualisiere B
					lowerB += bHalfed;
				} else if (upperA <= bHalfed) {
					out.write(0, 1);
					// aktualisiere b
					upperB -= bHalfed;
				} else {
					break;
				}
			}
		}

		double midOfB = upperB / 2;
		if (midOfB <= upperA && midOfB >= lowerA) {
			out.write(1, 1);
		}
	}

	private void decodeImage(BitInputStream in) throws IOException {
		final int white = 255;
		final int black = 0;
		int width = in.read(16);
		int height = in.read(16);
		double p0 = (double) (in.read(16) / 0x7FFF);

		double lowerA, lowerB, upperA, upperB, x0, x1;
		lowerA = lowerB = x0 = 0.0;
		upperA = upperB = x1 = 1.0;
		int index = 0;
		int[] decoded = new int[width * height];
		while (index < decoded.length) {
			while (true) {

				if (lowerB >= lowerA * p0 && upperB >= upperA * p0) {
					decoded[index] = (0xFF << 24) | (white << 16) | (white << 8) | white;
					index++;
					// a aktualisieren
					break;
				} else if (lowerB <= upperA && upperB <= upperA) {// ??
					decoded[index] = (0xFF << 24) | (black << 16) | (black << 8) | black;
					index++;
					// a aktualisieren
					break;
				}

				int v = in.read(1);
				// b aktualisiseren
			}

			// skalierung
			while (true) {
				// double bHalfed = upperB / 2;
				if (lowerA >= 0 && upperA <= 0.5) {
					upperA *= 2;
					upperB *= 2;
				} else if (lowerA >= 0.5 && upperA <= 1.0) {
					lowerA = 2 * (lowerA - 0.5);
					upperA = 2 * (upperA - 0.5);
					lowerB = 2 * (lowerB - 0.5);
					upperB = 2 * (upperB - 0.5);
				} else if (lowerA >= 0.25 && upperA <= 0.75) {
					lowerA = 2 * (lowerA - 0.25);
					upperA = 2 * (upperA - 0.25);
					lowerB = 2 * (lowerB - 0.25);
					upperB = 2 * (upperB - 0.25);
				} else {
					break;
				}
			}
		}

	}

	private void updateSizeAndMSELabel(int size) {
		sizeAndMSELabel.setText("Size: " + Math.round(size / 8 / 1024) + "kB " + String.format("MSE = %.1f", mse()));
	}

	private void binarisePreProcessedView(int threshold) {
		preProcessedView.setPixels(binarise(srcView.getPixels(), threshold));
	}
}
