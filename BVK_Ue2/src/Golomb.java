import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Golomb extends JPanel {

	private static final String name = "Viet Tu Hoang";

	private static final long serialVersionUID = 1L;
	private static final int borderWidth = 5;
	private static final int maxWidth = 400;
	private static final int maxHeight = maxWidth;
	private static final String initialFilename = "lena_klein.png";
	private File openPath = new File(".");

	private static JFrame frame;
	private JLabel sliderLabel;
	private JSlider slider;

	private JLabel origEntropyLabel;
	private JLabel preProcessedEntropyLabel;
	private JLabel golombEntropyLabel;
	private JLabel sizeAndMSELabel;

	private ImageView srcView; // source image view
	private ImageView preProcessedView; // reconstructed image view
	private ImageView golombView;

	private int[] preProcessedError;
	private int mode = 0;

	private enum FileType {
		NORMAL, GOL
	}

	private enum DialogType {
		OPEN, SAVE
	}

	public Golomb() {
		super(new BorderLayout(borderWidth, borderWidth));

		setBorder(BorderFactory.createEmptyBorder(borderWidth, borderWidth, borderWidth, borderWidth));

		File input = new File(initialFilename);
		if (!input.canRead())
			input = fileDialog(DialogType.OPEN, FileType.NORMAL); // file not
		srcView = new ImageView();
		preProcessedView = new ImageView();
		golombView = new ImageView();

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

		JButton saveGomlomb = new JButton("Save Golomb Image");
		saveGomlomb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveGolombFile(fileDialog(DialogType.SAVE, FileType.GOL));
			}
		});

		JButton loadgolomb = new JButton("Open Golomb Image");
		loadgolomb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loadGolombFile(fileDialog(DialogType.OPEN, FileType.GOL));
			}
		});

		String[] options = { "Copy", "DPCM" };
		final JComboBox<String> jComboBox = new JComboBox<>(options);
		jComboBox.setSelectedIndex(mode);
		jComboBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				String selection = (String) jComboBox.getSelectedItem();
				switch (selection) {
				case "Copy":
					preProcessedView.setPixels(srcView.getPixels().clone());
					mode = 0;
					break;
				case "DPCM":
					dcpm();
					mode = 2;
				default:
					break;
				}
				updateMLabel(optimalM());
			}

		});
		sliderLabel = new JLabel();

		slider = new JSlider(1, 130, 1);
		slider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				sliderLabel.setText("M = " + slider.getValue());

			}
		});

		controls.add(loadSrc, c);
		controls.add(jComboBox, c);
		controls.add(sliderLabel, c);
		controls.add(slider, c);
		controls.add(saveGomlomb, c);
		controls.add(loadgolomb, c);

		JPanel images = new JPanel(new GridLayout(1, 3));
		images.add(srcView);
		images.add(preProcessedView);
		images.add(golombView);

		JPanel status = new JPanel(new GridBagLayout());
		origEntropyLabel = new JLabel(" ");
		preProcessedEntropyLabel = new JLabel(" ");
		golombEntropyLabel = new JLabel(" ");
		sizeAndMSELabel = new JLabel(" ");
		status.add(origEntropyLabel, c);
		status.add(preProcessedEntropyLabel, c);
		status.add(golombEntropyLabel, c);
		status.add(sizeAndMSELabel, c);

		add(controls, BorderLayout.NORTH);
		add(images, BorderLayout.CENTER);
		add(status, BorderLayout.SOUTH);

		loadSrcFile(input);
		grayScale(srcView);
		preProcessedView.setPixels(srcView.getPixels().clone());
		entropyForAllImageViews();
		int optimalM = optimalM();
		updateMLabel(optimalM);
		slider.setValue(optimalM);
	}

	private void entropyForAllImageViews() {
		setOrigEntropyLabel();
		setPreprocessedLabel();
		setGolombLabel();
	}

	private double mse() {
		int errorSumSquared = 0;
		int[] origPix = srcView.getPixels();
		int[] decodedPix = golombView.getPixels();
		for (int i = 0; i < origPix.length; i++) {
			int error = origPix[i] - decodedPix[i];
			errorSumSquared += error * error;
		}

		return 1.0 / origPix.length * errorSumSquared;

	}

	private void setGolombLabel() {
		golombEntropyLabel
				.setText("Entropy right: " + NumberFormat.getInstance().format(entropy(golombView.getPixels())));
	}

	private void setOrigEntropyLabel() {
		origEntropyLabel.setText("Entropy left: " + NumberFormat.getInstance().format(entropy(srcView.getPixels())));

	}

	private void dcpm() {
		int[] srcPixels = srcView.getPixels();
		int[] processedPixels = new int[srcPixels.length];
		preProcessedError = new int[srcPixels.length];
		int init = 128;
		processedPixels[0] = preProcessedError[0] = srcPixels[0] - init;// fehler
																		// =
																		// src-128

		if (processedPixels[0] > 255)
			processedPixels[0] = 255;
		else if (processedPixels[0] < 0)
			processedPixels[0] = 0;

		for (int i = 1; i < processedPixels.length; i++) {
			int currentPix = srcPixels[i] & 0xFF;
			int prevPix = srcPixels[i - 1] & 0xFF;
			int error = currentPix - prevPix;
			preProcessedError[i] = error;
			int value = error + init;

			if (value > 255)
				value = 255;
			else if (value < 0)
				value = 0;

			processedPixels[i] = (0xFF << 24) | (value << 16) | (value << 8) | value;
		}

		this.preProcessedView.setPixels(processedPixels);
		setPreprocessedLabel();

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
		int[] histogram = new int[256];
		for (int i : pixels) {
			histogram[i & 0xFF]++;
		}
		return histogram;
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

	private int optimalM() {
		return (int) ((int) getAvg(histogramOfImage(preProcessedView.getPixels())) * Math.log(2));
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
		if (fType == FileType.GOL)
			filter = new FileNameExtensionFilter("Golomb Images (*.gol)", "gol");
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
		golombView.resetToSize(srcView.getImgWidth(), srcView.getImgHeight());

		// displayMse();

		frame.pack();
	}

	private void loadGolombFile(File file) {
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

	private void saveGolombFile(File file) {
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
		frame = new JFrame("Golomb " + name);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JComponent newContentPane = new Golomb();
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

		int m = slider.getValue();
		out.write(preProcessedView.getImgWidth(), Short.SIZE);
		out.write(preProcessedView.getImgHeight(), Short.SIZE);
		out.write(mode, 8);
		out.write(m, 8);

		int b = (int) Math.ceil(Math.log10(m) / Math.log10(2));
		int threshold = (int) Math.pow(2.0, b) - m;

		if (mode == 0) {
			for (int i : preProcessedView.pixels) {
				int gray = i & 0xFF;
				int q = gray / m;
				int r = gray - q * m;

				for (int j = q; j > 0; j--) {
					out.write(1, 1);
				}
				out.write(0, 1);
				if (r < threshold) {
					out.write(r, b - 1);
				} else {
					out.write(r + threshold, b);
				}
			}

		} else if (mode == 2) {
			for (int i : preProcessedError) {
				int v = (i < 0) ? -(i * 2) - 1 : 2 * i;
				int q = v / m;
				int r = v - q * m;

				for (int j = q; j > 0; j--) {
					out.write(1, 1);
				}
				out.write(0, 1);
				if (r < threshold) {
					out.write(r, b - 1);
				} else {
					out.write(r + threshold, b);
				}
			}
		}
	}

	private void decodeImage(BitInputStream in) throws IOException {

		int width = in.read(16);
		int height = in.read(16);
		int mode = in.read(8);
		int m = in.read(8);

		int[] pix = new int[width * height];
		if (mode == 0) {
			decodecopy(pix, m, in);
		} else if (mode == 2) {
			decodeGolomb(pix, m, in);
		}
		setGolombLabel();
	}

	private void updateSizeAndMSELabel(int size) {
		sizeAndMSELabel.setText("Size: " + Math.round(size / 8 / 1024) + "kB " + String.format("MSE = %.1f", mse()));
	}

	private void decodeGolomb(int[] pix, int m, BitInputStream in) throws IOException {

		int b = (int) (Math.ceil(Math.log10(m) / Math.log10(2)));
		int threshold = ((int) Math.pow(2, b) - m);
		int[] error = new int[pix.length];
		int bits = 0;

		for (int i = 0; i < error.length; i++) {
			int q = 0;
			while (in.read(1) != 0) {
				q++;
				bits++;
			}
			bits++;
			int r = in.read(b - 1);
			bits += b - 1;
			if (r >= threshold) {
				r = r << 1;
				int v = in.read(1);
				r = r | v;
				r = r - threshold;
				bits++;
			}
			int val = q * m + r;
			if (val % 2 == 0) {
				val = (val / 2);
			} else {
				val = -(val + 1) / 2;
			}
			error[i] = val;
		}

		for (int i = 0; i < pix.length; i++) {
			int gray;
			if (i == 0) {
				gray = 128 + error[i];
				pix[i] = 0xFF << 24 | gray << 16 | gray << 8 | gray;
				continue;
			}
			gray = (pix[i - 1] & 0xFF) + error[i];
			pix[i] = 0xFF << 24 | gray << 16 | gray << 8 | gray;
		}

		golombView.setPixels(pix);
		updateSizeAndMSELabel(bits);
	}

	private void decodecopy(int[] pix, int m, BitInputStream in) throws IOException {
		int b = (int) (Math.ceil(Math.log10(m) / Math.log10(2)));
		int threshold = ((int) Math.pow(2, b) - m);
		int bits = 0;
		for (int i = 0; i < pix.length; i++) {
			int q = 0;
			while (in.read(1) == 1) {
				q++;
				bits++;
			}
			bits++;
			int r = in.read(b - 1);
			bits += b - 1;
			if (r >= threshold) {
				r = r << 1;
				int v = in.read(1);
				r = r | v;
				r = r - threshold;
				bits++;
			}
			int gray = q * m + r;
			pix[i] = 0xFF << 24 | gray << 16 | gray << 8 | gray;
		}
		golombView.setPixels(pix);
		updateSizeAndMSELabel(bits);
	}

	private void updateMLabel(int value) {
		sliderLabel.setText("M = " + optimalM());
	}
}
