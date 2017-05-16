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

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

//import RLE.DialogType;
//import RLE.FileType;

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
	private int sliderValue;

	private JLabel origEntropyLabel;
	private JLabel preProcessedEntropyLabel;

	private ImageView srcView; // source image view
	private ImageView preProcessedView; // reconstructed image view
	private ImageView golombView;

	private int[] preProcessedError;

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
		jComboBox.setSelectedIndex(0);
		jComboBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				String selection = (String) jComboBox.getSelectedItem();
				switch (selection) {
				case "Copy":
					preProcessedView.setPixels(srcView.getPixels().clone());
					break;
				case "DPCM":
					dcpm();
				default:
					break;
				}
			}

		});

		sliderLabel = new JLabel("M = 1");
		slider = new JSlider(1, 130, 1);
		slider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				sliderValue = slider.getValue();
				sliderLabel.setText("M = " + sliderValue);

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
		status.add(origEntropyLabel, c);
		status.add(preProcessedEntropyLabel, c);

		add(controls, BorderLayout.NORTH);
		add(images, BorderLayout.CENTER);
		add(status, BorderLayout.SOUTH);

		loadSrcFile(input);
		grayScale(srcView);
		preProcessedView.setPixels(srcView.getPixels().clone());
		entropyForAllImageViews();
	}

	private void entropyForAllImageViews() {
		origEntropyLabel.setText("Entropy: " + entropy(srcView.getPixels()));
		setPreprocessedLabel();
	}

	private void dcpm() {
		int[] srcPixels = srcView.getPixels();
		int[] processedPixels = new int[srcPixels.length];
		preProcessedError = new int[srcPixels.length];
		int init = 128;
		processedPixels[0] = preProcessedError[0] = srcPixels[0] - init;

		if (processedPixels[0] > 255)
			processedPixels[0] = 255;
		else if (processedPixels[0] < 0)
			processedPixels[0] = 0;

		for (int i = 1; i < processedPixels.length; i++) {
			int currentPix = (srcPixels[i] >> 16) & 0xFF;
			int prevPix = (srcPixels[i - 1] >> 16) & 0xFF;
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
		NumberFormat.getInstance().setMaximumFractionDigits(3);
		preProcessedEntropyLabel
				.setText("Entropy: " + NumberFormat.getInstance().format(entropy(preProcessedView.getPixels())));

	}

	private double entropy(int[] pixels) {
		int count = 0;
		int[] histogram = new int[256];
		for (int i : pixels) {
			histogram[i & 0xFF]++;
		}
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
			DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
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

	private void encodeImage(DataOutputStream out) throws IOException {

	}

	private void decodeImage(BitInputStream in) throws IOException {

		int width = in.read(16);
		int height = in.read(16);
		int mode = in.read(8);
		int m = in.read(8);

		int[] decodedImage = new int[width * height];
		int b = (int) (Math.ceil(Math.log10(m) / Math.log10(2)));
		int threshold = ((int) Math.pow(2, b) - m);
		for (int i = 0; i < decodedImage.length; i++) {
			System.out.println(i);
			int q = 0;
			while (in.read(1) == 1) {
				q++;
				// System.out.println("while");
			}
			int r = in.read(b - 1);
			if (r >= threshold) {
				r = r << 1;
				int v = in.read(1);
				r = r | v;
			}
			int gray = q * m + r;
			decodedImage[i] = 0xFF << 24 | gray << 16 | gray << 8 | gray;
		}
		golombView.setPixels(decodedImage);
	}

	private void decodeGolomb(int width, int height, BitInputStream in) {

	}

	private void decodecopy(int width, int height, BitInputStream in) {
		int[] decodedImage = new int[width * height];

	}

	private void createGeomDistr() {

		Arrays.sort(preProcessedError);

	}
}
