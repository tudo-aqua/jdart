package mocks.java.net;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class InputStream_Mock extends InputStream {
	Random r = new Random();

	@Override
	public int read() throws IOException {
		return r.nextInt();
	}

	public void close() {

	}
}
