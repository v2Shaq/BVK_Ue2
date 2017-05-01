import java.io.IOException;
import java.io.OutputStream;

public class BitOutputStream  {

	private long buffer = 0L;
	private OutputStream outputStream;
	private int index = 0; 

	public BitOutputStream(OutputStream out) {
		outputStream = out;
	}

	public void write(int value, int bitNumber) throws IOException {
		
		buffer = value << bitNumber;

	}
	
	public void close() throws IOException {
		// TODO Auto-generated method stub
		outputStream.close();
	}

}
