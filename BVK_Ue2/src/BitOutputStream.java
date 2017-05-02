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

//		// buffer = value << bitNumber;
//		char[] bits = (Integer.toBinaryString(value)).toCharArray();
//		for (int dif = bitNumber - bits.length; dif > 0; dif--) {
//			buffer = buffer << 1;
//		}
//		for (char c : bits) {
//			if (c == '1') {
//				buffer = (buffer << 1) | 1;
//			} else {
//				buffer = buffer << 1;
//			}
//		}
//
//		index += bitNumber;
//		if (index >= 8) {
//			
//		}
		
		long temp = value;
		temp = temp << (64-len-bitNumber);
		buf = buf | temp;
		
		len = len + bitNumber;
        
        while(len >= 8){
        	
        	temp = buf;
        	byte toWrite = (byte)(temp >> 56);
			output.write(toWrite);
		
        }
		

	}

	public void close() throws IOException {
		// TODO Auto-generated method stub
		outputStream.close();
	}

}
