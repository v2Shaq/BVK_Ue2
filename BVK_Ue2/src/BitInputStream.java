import java.io.IOException;
import java.io.InputStream;

public class BitInputStream {
	private long buffer = 0;
	private int index = 0;
	private InputStream inputStream;

	public BitInputStream(InputStream in) {
		this.inputStream = in;
	}

	public int read(int bitNumber) throws IOException {
		if (bitNumber > index) {
			for (int i = index; i < bitNumber; i += 8) {
				long byteRead = inputStream.read();
				// System.out.println(Long.toBinaryString(byteRead));
				buffer = buffer | (byteRead << 64 - index - 8);
				// System.out.println(Long.toBinaryString(buffer));
				index += 8;
			}
		}
		// System.out.println(Long.toBinaryString(buffer));
		int value = (int) (buffer >>> (64 - bitNumber));
//		System.out.println(Integer.toBinaryString(value));
		// Integer.toBinaryString(value);
		buffer = buffer << bitNumber;
		// System.out.println(Long.toBinaryString(buffer));
		index -= bitNumber;
		return value;
	}

	public void close() throws IOException {
		inputStream.close();
	}

}
