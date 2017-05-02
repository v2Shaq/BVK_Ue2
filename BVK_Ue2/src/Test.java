
public class Test {

	public static void main(String[] args) {
		int x = 10;
		System.out.println(Integer.toBinaryString(x));
		int y = (x << 1) | 1;
		System.out.println(Integer.toBinaryString(x));
		char c = '1';
		System.out.println((int)c);
		long buf = 4314L
			;
		long temp = buf;
		System.out.println(temp==buf);
	}
}
