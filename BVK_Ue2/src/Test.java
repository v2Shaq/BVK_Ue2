import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class Test {

	public static void main(String[] args) {
		// int x = 10;
		// System.out.println(Integer.toBinaryString(x));
		// int y = (x << 1) | 1;
		// System.out.println(Integer.toBinaryString(x));
		// char c = '1';
		// System.out.println((int) c);
		// long buf = Long.MAX_VALUE - 1;
		// long temp = buf;
		// System.out.println(Long.toBinaryString(buf));
		// System.out.println(Integer.toBinaryString((int) buf));
		System.out.println("1110101110010101010010101111100100100101010101010101010".length()+ " ");
		System.out.println(Integer.MAX_VALUE);
		File f = new File("test.bvkdat");
		try {
			BitOutputStream bos = new BitOutputStream(new FileOutputStream(f));
			// System.out.println("here2");
			bos.write(6, 8);
			bos.write(23451, 20);
			bos.write(123513431, 30);
			bos.write(-3, 4);
			bos.close();
		} catch (Exception e1) {
			// TODO Auto-generated catch block

			e1.printStackTrace();
		}
		try {
			BitInputStream bis = new BitInputStream(new FileInputStream(f));
			 System.out.println("here");
			 int x2 = bis.read(8);
			 System.out.println(x2);
			 x2 = bis.read(20);
			 System.out.println(x2);
			 x2 = bis.read(30);
			 System.out.println(x2);
			 x2 = bis.read(4);
			 System.out.println(x2);
			// int x = bis.read(32);
			// int x2 = bis.read(20);
			// System.out.println(Integer.toBinaryString(x));
			// System.out.println(Integer.toBinaryString(x2));
			bis.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
