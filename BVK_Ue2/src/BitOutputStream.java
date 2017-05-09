import java.io.IOException;
import java.io.OutputStream;

public class BitOutputStream {

	private long buffer = 0L;
	private OutputStream outputStream;
	private int index = 0;

	public BitOutputStream(OutputStream out) {
		outputStream = out;
	}

	public void write(int value, int bitNumber) throws IOException {

		if (bitNumber > Long.SIZE - index) {
			throw new IllegalArgumentException("Zahl zu groﬂ");
		}

		long valueInLong = value;
		// System.out.println(Long.toBinaryString(valueInLong));
		buffer = buffer | valueInLong << 64 - index - bitNumber;
		// System.out.println(Long.toBinaryString(buffer));
		index += bitNumber;

		while (index >= 8) {
			int toWrite = (int) (buffer >>> 56);
//			System.out.println("TOWRITE" + Integer.toBinaryString(toWrite));
			outputStream.write(toWrite);
			index -= 8;
			buffer = buffer << 8;
		}
	}

	public void close() throws IOException {
		int toWrite = (int) (buffer >>> 56);
		// System.out.println(Long.toBinaryString(buffer));
		// System.out.println(Integer.toBinaryString(toWrite));
		outputStream.write(toWrite);
		outputStream.close();
	}

	/*
	 * // // buffer = value << bitNumber; // char[] bits =
	 * (Integer.toBinaryString(value)).toCharArray(); // for (int dif =
	 * bitNumber - bits.length; dif > 0; dif--) { // buffer = buffer << 1; // }
	 * // for (char c : bits) { // if (c == '1') { // buffer = (buffer << 1) |
	 * 1; // } else { // buffer = buffer << 1; // } // } // // index +=
	 * bitNumber; // if (index >= 8) { // // }
	 * 
	 * // long temp = value; // temp = temp << (64-len-bitNumber); // buf = buf
	 * | temp; // // len = len + bitNumber; // // while(len >= 8){ // // temp =
	 * buf; // byte toWrite = (byte)(temp >> 56); // output.write(toWrite); //
	 * // }
	 */

}
