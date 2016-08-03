package com.infobart.bytesperf;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.DoubleSummaryStatistics;
import java.util.Random;

import static com.infobart.bytesperf.ReadProtocolApplication.*;

public class WriteProtocolApplication {
	public final static int ITERATIONS = 50000;

	public final static String END = "e\n";

	public final static String COMMAND = "a";

	public final static String REF_STRING = "r15";

	public final static String SMALL_STRING = "hello";

	public static String LONG_STRING = "very long string ééééééééééééé";

	public static String EXTRA_LONG_STRING = "very long string \n ééééééééééééé";

	public final static String ESCAPE_CHAR = "\\";

	public final static int INTEGER = -789456123;

	public final static long LONG = 361231231231231231l;

	public final static double DOUBLE = 1.0 / 3.0;

	public final static byte[] SMALL_BYTES = new byte[64];

	public final static byte[] BYTES = new byte[1024 * 1024 * 64];

	public final static byte[] EXTRA_BYTES = new byte[1024 * 1024 * 256];

	public final static Random random = new Random();

	static {
		random.nextBytes(SMALL_BYTES);
		random.nextBytes(BYTES);
		random.nextBytes(EXTRA_BYTES);
		StringBuilder builder = new StringBuilder();

		for (int i = 0; i < 10; i++) {
			builder.append(LONG_STRING);
		}
		LONG_STRING = builder.toString();

		builder = new StringBuilder();

		for (int i = 0; i < 1000; i++) {
			builder.append(EXTRA_LONG_STRING);
		}
		EXTRA_LONG_STRING = builder.toString();
	}

	public static Tuple[] getArgs(int size) {
		Tuple[] args = new Tuple[size];
		if (size == 1) {
			args[0] = new Tuple(COMMAND_TYPE, COMMAND);
		} else if (size == 2) {
			args[0] = new Tuple(COMMAND_TYPE, COMMAND);
			args[1] = new Tuple(STRING_TYPE, REF_STRING);
		} else if (size == 3) {
			args[0] = new Tuple(COMMAND_TYPE, COMMAND);
			args[1] = new Tuple(COMMAND_TYPE, COMMAND);
			args[2] = new Tuple(STRING_TYPE, REF_STRING);
		} else if (size == 4) {
			args[0] = new Tuple(COMMAND_TYPE, COMMAND);
			args[1] = new Tuple(STRING_TYPE, SMALL_STRING);
			args[2] = new Tuple(INT_TYPE, INTEGER);
			args[3] = new Tuple(STRING_TYPE, SMALL_STRING);
		} else if (size == 5) {
			args[0] = new Tuple(COMMAND_TYPE, COMMAND);
			args[1] = new Tuple(COMMAND_TYPE, COMMAND);
			args[2] = new Tuple(STRING_TYPE, SMALL_STRING);
			args[3] = new Tuple(DOUBLE_TYPE, DOUBLE);
			args[4] = new Tuple(DOUBLE_TYPE, DOUBLE);
		} else if (size == 6) {
			args[0] = new Tuple(COMMAND_TYPE, COMMAND);
			args[1] = new Tuple(COMMAND_TYPE, COMMAND);
			args[2] = new Tuple(STRING_TYPE, SMALL_STRING);
			args[3] = new Tuple(STRING_TYPE, SMALL_STRING);
			args[4] = new Tuple(LONG_TYPE, LONG);
			args[5] = new Tuple(LONG_TYPE, LONG);
		} else if (size == 7) {
			args[0] = new Tuple(COMMAND_TYPE, COMMAND);
			args[1] = new Tuple(COMMAND_TYPE, COMMAND);
			args[2] = new Tuple(STRING_TYPE, EXTRA_LONG_STRING);
			args[3] = new Tuple(STRING_TYPE, SMALL_STRING);
			args[4] = new Tuple(STRING_TYPE, SMALL_STRING);
			args[5] = new Tuple(STRING_TYPE, SMALL_STRING);
			args[6] = new Tuple(STRING_TYPE, SMALL_STRING);
		} else if (size == 8) {
			args[0] = new Tuple(COMMAND_TYPE, COMMAND);
			args[1] = new Tuple(BYTE_TYPE, SMALL_BYTES);
			args[2] = new Tuple(STRING_TYPE, SMALL_STRING);
			args[3] = new Tuple(STRING_TYPE, SMALL_STRING);
			args[4] = new Tuple(STRING_TYPE, SMALL_STRING);
			args[5] = new Tuple(STRING_TYPE, SMALL_STRING);
			args[6] = new Tuple(STRING_TYPE, SMALL_STRING);
			args[7] = new Tuple(STRING_TYPE, SMALL_STRING);
		} else if (size == 9) {
			args[0] = new Tuple(COMMAND_TYPE, COMMAND);
			args[1] = new Tuple(BYTE_TYPE, BYTES);
			args[2] = new Tuple(STRING_TYPE, SMALL_STRING);
			args[3] = new Tuple(STRING_TYPE, SMALL_STRING);
			args[4] = new Tuple(STRING_TYPE, SMALL_STRING);
			args[5] = new Tuple(STRING_TYPE, SMALL_STRING);
			args[6] = new Tuple(STRING_TYPE, SMALL_STRING);
			args[7] = new Tuple(STRING_TYPE, SMALL_STRING);
			args[8] = new Tuple(STRING_TYPE, SMALL_STRING);
		} else if (size == 10) {
			args[0] = new Tuple(COMMAND_TYPE, COMMAND);
			args[1] = new Tuple(BYTE_TYPE, EXTRA_BYTES);
			args[2] = new Tuple(STRING_TYPE, SMALL_STRING);
			args[3] = new Tuple(STRING_TYPE, SMALL_STRING);
			args[4] = new Tuple(STRING_TYPE, SMALL_STRING);
			args[5] = new Tuple(STRING_TYPE, SMALL_STRING);
			args[6] = new Tuple(STRING_TYPE, SMALL_STRING);
			args[7] = new Tuple(STRING_TYPE, SMALL_STRING);
			args[8] = new Tuple(STRING_TYPE, SMALL_STRING);
			args[9] = new Tuple(STRING_TYPE, SMALL_STRING);
		}

		return args;
	}

	public static String[] encodeUtf8Lines(Tuple[] args, Base64.Encoder encoder) {
		String[] lines = new String[(args.length * 2) + 1];
		for (int i = 0; i < args.length; i++) {
			Tuple tuple = args[i];
			lines[i * 2] = String.valueOf(tuple.getType()) + "\n";
			if (tuple.getType() != BYTE_TYPE) {
				if (tuple.getType() == STRING_TYPE) {
					lines[(i * 2) + 1] = StringUtil.escape((String)tuple.getValue()) + "\n";
				} else {
					lines[(i * 2) + 1] = String.valueOf(tuple.getValue()) + "\n";
				}

			} else {
				lines[(i * 2) + 1] = encoder.encodeToString((byte[])tuple.getValue()) + "\n";
			}
		}

		lines[(args.length * 2)] = END;
		return lines;
	}

	public static void encodeByteTuple(Tuple[] args, Charset utf8, DataOutputStream writer) throws Exception {
		for (int i = 0; i < args.length; i++) {
			Tuple tuple = args[i];
			int type = tuple.getType();
			if (type == COMMAND_TYPE || type == STRING_TYPE) {
				byte[] value = ((String)tuple.getValue()).getBytes(utf8);
				writer.writeInt(type);
				writer.writeInt(value.length);
				writer.write(value);
			} else if (type == INT_TYPE){
				writer.writeInt(type);
				writer.writeInt((int)tuple.getValue());
			} else if (type == LONG_TYPE) {
				writer.writeInt(type);
				writer.writeLong((long)tuple.getValue());
			} else if (type == DOUBLE_TYPE) {
				writer.writeInt(type);
				writer.writeDouble((double)tuple.getValue());
			} else if (type == BYTE_TYPE) {
				byte[] value = (byte[])tuple.getValue();
				writer.writeInt(type);
				writer.writeInt(value.length);
				writer.write(value);
			}
		}
	}


	public static void writeLineByLine(Socket socket, int size) throws Exception {
		Charset utf8 = Charset.forName("UTF-8");
		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(socket.getOutputStream(), utf8));
		DataInputStream input = new DataInputStream(socket.getInputStream());
		Base64.Encoder encoder = Base64.getEncoder();

		for (int i = 0; i < ITERATIONS; i++) {
			long startNano = System.nanoTime();
			Tuple[] args = getArgs(size);
			String[] lines = encodeUtf8Lines(args, encoder);
			for (String line: lines) {
				writer.write(line);
			}
			// In general, writer.flush at the end is faster than writer.flush, but that depends on the test...
			writer.flush();

			input.readByte();

			long stopNano = System.nanoTime();
			long timeNs = stopNano - startNano;
			double timeMs = (double) timeNs / 1000.0 / 1000.0;

			DoubleSummaryStatistics currentStats = stats.get(size);
			if (currentStats == null) {
				currentStats = new DoubleSummaryStatistics();
				stats.put(size, currentStats);
			}
			currentStats.accept(timeMs);
		}

		System.out.println("Average MS for size " + size + ": " + stats.get(size).getAverage());

	}

	public static void writeOptimized(Socket socket, int size) throws Exception {
		Charset utf8 = Charset.forName("UTF-8");
		DataOutputStream writer = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		DataInputStream input = new DataInputStream(socket.getInputStream());

		for (int i = 0; i < ITERATIONS; i++) {
			long startNano = System.nanoTime();
			Tuple[] args = getArgs(size);
			writer.writeInt(args.length);
			encodeByteTuple(args, utf8, writer);
			// In general, writer.flush at the end is faster than writer.flush, but that depends on the test...
			writer.flush();

			input.readByte();

			long stopNano = System.nanoTime();
			long timeNs = stopNano - startNano;
			double timeMs = (double) timeNs / 1000.0 / 1000.0;

			DoubleSummaryStatistics currentStats = stats.get(size);
			if (currentStats == null) {
				currentStats = new DoubleSummaryStatistics();
				stats.put(size, currentStats);
			}
			currentStats.accept(timeMs);
		}

		System.out.println("Average MS for size " + size + ": " + stats.get(size).getAverage());

	}

	public static void processSocket(Socket socket, int size) {
		try {
//			writeLineByLine(socket, size);
			writeOptimized(socket, size);
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				socket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void send() {
		for (int i = 0; i < 11; i++) {
			try {
				Socket socket = new Socket(InetAddress.getByName("127.0.0.1"), 10000);
				processSocket(socket, i);
				Thread.currentThread().sleep(250);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		send();
	}

	public static class Tuple {
		private int type;
		private Object value;

		public Tuple(int type, Object value) {
			this.type = type;
			this.value = value;
		}

		public int getType() {
			return type;
		}

		public Object getValue() {
			return value;
		}

		public boolean sendLength() {
			return type == STRING_TYPE || type == BYTE_TYPE || type == COMMAND_TYPE;
		}
	}

}
