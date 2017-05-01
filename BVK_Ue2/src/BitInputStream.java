import java.io.IOException;
import java.io.InputStream;

public class BitInputStream  {
	private long buffer = 0;
	private int index = 0; 
	private InputStream inputStream;

	public BitInputStream(InputStream in) {
		// TODO Auto-generated constructor stub
	}
	public int read(int bitNumber) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public void close() throws IOException {
		// TODO Auto-generated method stub
		try{
			inputStream.close();
		}catch (IOException e) {
		}
		
	}


}
