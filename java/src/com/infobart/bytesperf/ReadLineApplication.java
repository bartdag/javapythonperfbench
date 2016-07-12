package com.infobart.bytesperf;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.Buffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReadLineApplication {
	public final static String END = "e";
	public static Map<Integer, DoubleSummaryStatistics> stats = new HashMap<Integer, DoubleSummaryStatistics>();

	public final static int STRING_TYPE = 0;
	public final static int BYTE_TYPE = 1;
	public final static int ITERATIONS = 100;

	public static void readByLine(Socket socket) {
		try {
			Charset utf8 = Charset.forName("UTF-8");
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(socket.getInputStream(), utf8));
			DataOutputStream output = new DataOutputStream(socket.getOutputStream());
			Base64.Decoder decoder = Base64.getDecoder();

			long startNano = System.nanoTime();
			int count = 0;
			String current = "";
			byte[] bytes;
			while (!current.equals(END)) {
				current = reader.readLine();
				if (count == 2) {
					current = StringUtil.unescape(current);
				} else if (count == 5) {
					 bytes = decoder.decode(current.getBytes());
				}
				count += 1;
			}
			output.writeByte(0);
			output.flush();
			long stopNano = System.nanoTime();
			long timeNs = stopNano - startNano;
			double timeMs = (double)timeNs / 1000.0 / 1000.0;

			DoubleSummaryStatistics currentStats = stats.get(count);
			if (currentStats == null) {
				currentStats = new DoubleSummaryStatistics();
				stats.put(count, currentStats);
			}
			currentStats.accept(timeMs);
			if (currentStats.getCount() == ITERATIONS) {
				System.out.println("MS: " + timeMs + ". Average MS for size " + count + ": " + currentStats.getAverage
						());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void readBlockByLine(Socket socket) {
		try {
			Charset utf8 = Charset.forName("UTF-8");
			DataInputStream input = new DataInputStream(socket.getInputStream());
			DataOutputStream output = new DataOutputStream(socket.getOutputStream());
			Base64.Decoder decoder = Base64.getDecoder();

			long startNano = System.nanoTime();
			int count = 0;
			String current = "";
			int size = input.readInt();
			byte[] buffer = new byte[size];
			input.readFully(buffer);
			BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buffer)));

			byte[] bytes;
			while (!current.equals(END)) {
				current = reader.readLine();
				if (count == 2) {
					current = StringUtil.unescape(current);
				} else if (count == 5) {
					bytes = decoder.decode(current.getBytes());
				}
				count += 1;
			}

			output.writeByte(0);
			output.flush();
			long stopNano = System.nanoTime();
			long timeNs = stopNano - startNano;
			double timeMs = (double)timeNs / 1000.0 / 1000.0;

			DoubleSummaryStatistics currentStats = stats.get(count);
			if (currentStats == null) {
				currentStats = new DoubleSummaryStatistics();
				stats.put(count, currentStats);
			}
			currentStats.accept(timeMs);
			if (currentStats.getCount() == ITERATIONS) {
				System.out.println("MS: " + timeMs + ". Average MS for size " + count + ": " + currentStats.getAverage
						());
			}
		} catch (Exception e) {

		}

	}

	public static void readBlockByBlock(Socket socket) {
		try {
			Charset utf8 = Charset.forName("UTF-8");
			DataInputStream input = new DataInputStream(socket.getInputStream());
			DataOutputStream output = new DataOutputStream(socket.getOutputStream());

			long startNano = System.nanoTime();
			int size = input.readInt();
			List<Object> args = new ArrayList<Object>();
			for (int i = 0; i < size; i++) {
				int argSize = input.readInt();
				int type = input.readInt();
				byte[] buffer = new byte[argSize];
				input.readFully(buffer);
				if (type == STRING_TYPE) {
					args.add(new String(buffer, utf8));
				} else {
					args.add(buffer);
				}
			}
			output.writeByte(0);
			output.flush();
			long stopNano = System.nanoTime();
			long timeNs = stopNano - startNano;
			double timeMs = (double)timeNs / 1000.0 / 1000.0;

			DoubleSummaryStatistics currentStats = stats.get(size);
			if (currentStats == null) {
				currentStats = new DoubleSummaryStatistics();
				stats.put(size, currentStats);
			}
			currentStats.accept(timeMs);
			if (currentStats.getCount() == ITERATIONS) {
				System.out.println("MS: " + timeMs + ". Average MS for size " + size + ": " + currentStats.getAverage());
			}
		} catch (Exception e) {

		}

	}

	public static void receive(Socket socket) {
		try {
//			readByLine(socket);
//			readBlockByLine(socket);
			readBlockByBlock(socket);
			socket.close();

		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void processSocket(final Socket socket) {
		new Thread(new Runnable() {
			@Override public void run() {
				receive(socket);
			}
		}).start();
	}

	public static void listen() {
		try {
			ServerSocket sSocket = new ServerSocket(10000);
			while (true) {
				Socket socket = sSocket.accept();
				processSocket(socket);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		listen();
	}

}
