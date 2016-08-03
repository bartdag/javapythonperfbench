package com.infobart.bytesperf;

import java.io.BufferedInputStream;
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

public class ReadProtocolApplication {
	public final static String END = "e";
	public static Map<Integer, DoubleSummaryStatistics> stats = new HashMap<Integer, DoubleSummaryStatistics>();

	public final static int BYTE_TYPE = 0;
	public final static int STRING_TYPE = 1;
	public final static int COMMAND_TYPE = 2;
	public final static int INT_TYPE = 3;
	public final static int LONG_TYPE = 4;
	public final static int DOUBLE_TYPE = 5;
//	public final static int ITERATIONS = 100;
	public final static int ITERATIONS = 50000;

	public final static String SBYTE_TYPE = "0";
	public final static String SSTRING_TYPE = "1";
	public final static String SCOMMAND_TYPE = "2";
	public final static String SINT_TYPE = "3";
	public final static String SLONG_TYPE = "4";
	public final static String SDOUBLE_TYPE = "5";


	public static void readLineByLine(Socket socket) {
		try {
			Charset utf8 = Charset.forName("UTF-8");
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(socket.getInputStream(), utf8));
			DataOutputStream output = new DataOutputStream(socket.getOutputStream());
			Base64.Decoder decoder = Base64.getDecoder();

			int generalSize = 0;

			for (int iter = 0; iter < ITERATIONS; iter++) {
				int size = 0;
				long startNano = System.nanoTime();
				List<Object> args = new ArrayList<Object>();

				String type = "";
				String value = "";
				while (true) {
					type = reader.readLine();
					if (type.equals(END)) {
						break;
					}
					size++;
					value = reader.readLine();
					if (!type.equals(SBYTE_TYPE)) {
						Object arg = null;
						if (type.equals(SINT_TYPE)) {
							arg = Integer.parseInt(value);
						} else if (type.equals(SLONG_TYPE)) {
							arg = Long.parseLong(value);
						} else if (type.equals(SDOUBLE_TYPE)) {
							arg = Double.parseDouble(value);
						} else {
							arg = StringUtil.unescape(value);
						}
						args.add(arg);
					} else {
						args.add(decoder.decode(value));
					}
				}
				generalSize = size;
				output.writeByte(0);
				output.flush();
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

			System.out.println("Average MS for size " + generalSize + ": " + stats.get(generalSize).getAverage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void readBlockByBlock(Socket socket) {
		try {
			Charset utf8 = Charset.forName("UTF-8");
			DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			DataOutputStream output = new DataOutputStream(socket.getOutputStream());

			int generalSize = 0;

			for (int iter = 0; iter < ITERATIONS; iter++) {

				long startNano = System.nanoTime();
				int size = input.readInt();
				generalSize = size;
				List<Object> args = new ArrayList<Object>();
				for (int i = 0; i < size; i++) {
					int type = input.readInt();
					int argSize = input.readInt();
					byte[] buffer = new byte[argSize];
					input.readFully(buffer);
					if (type != BYTE_TYPE) {
						String s = new String(buffer, utf8);
						Object arg = s;
						if (type == INT_TYPE) {
							arg = Integer.parseInt(s);
						} else if (type == LONG_TYPE) {
							arg = Long.parseLong(s);
						} else if (type == DOUBLE_TYPE) {
							arg = Double.parseDouble(s);
						}
						args.add(arg);
					} else {
						args.add(buffer);
					}
				}
				output.writeByte(0);
				output.flush();
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
			System.out.println("Average MS for size " + generalSize + ": " + stats.get(generalSize).getAverage());
		} catch (Exception e) {

		}

	}

	public static void readBlockByBlockTotal(Socket socket) {
		try {
			Charset utf8 = Charset.forName("UTF-8");
			DataInputStream socketInput = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			DataOutputStream output = new DataOutputStream(socket.getOutputStream());

			int generalSize = 0;

			for (int iter = 0; iter < ITERATIONS; iter++) {

				long startNano = System.nanoTime();
				int total = socketInput.readInt();
				byte[] tempBuffer = new byte[total];
				socketInput.readFully(tempBuffer);
				DataInputStream input = new DataInputStream(new ByteArrayInputStream(tempBuffer));
				int size = input.readInt();
				generalSize = size;
				List<Object> args = new ArrayList<Object>();
				for (int i = 0; i < size; i++) {
					int type = input.readInt();
					if (type == STRING_TYPE || type == COMMAND_TYPE || type == BYTE_TYPE) {
						int argSize = input.readInt();
						byte[] buffer = new byte[argSize];
						input.readFully(buffer);
						if (type != BYTE_TYPE) {
							String s = new String(buffer, utf8);
							args.add(s);
						} else {
							args.add(buffer);
						}
					} else if (type == INT_TYPE) {
						args.add(input.readInt());
					} else if (type == LONG_TYPE) {
						args.add(input.readLong());
					} else {
						args.add(input.readDouble());
					}
				}
				output.writeByte(0);
				output.flush();
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
			System.out.println("Average MS for size " + generalSize + ": " + stats.get(generalSize).getAverage());
		} catch (Exception e) {

		}

	}

	public static void readLengthThanArgs(Socket socket) {
		try {
			Charset utf8 = Charset.forName("UTF-8");
			DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			DataOutputStream output = new DataOutputStream(socket.getOutputStream());
			int generalSize = 0;

			for (int iter = 0; iter < ITERATIONS; iter++) {
				long startNano = System.nanoTime();
				int size = input.readInt();
				generalSize = size;
				List<Object> args = new ArrayList<Object>();
				byte[] meta = new byte[size * 8];
				input.readFully(meta);
				DataInputStream metaInput = new DataInputStream(new ByteArrayInputStream(meta));
				for (int i = 0; i < size; i++) {
					int type = metaInput.readInt();
					int argSize = metaInput.readInt();
					if (type == STRING_TYPE || type == COMMAND_TYPE || type == BYTE_TYPE) {
						byte[] buffer = new byte[argSize];
						input.readFully(buffer);
						if (type != BYTE_TYPE) {
							String s = new String(buffer, utf8);
							args.add(s);
						} else {
							args.add(buffer);
						}
					} else if (type == INT_TYPE) {
						args.add(input.readInt());
					} else if (type == LONG_TYPE) {
						args.add(input.readLong());
					} else {
						args.add(input.readDouble());
					}
				}
				output.writeByte(0);
				output.flush();
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

			System.out.println("Average MS for size " + generalSize + ": " + stats.get(generalSize).getAverage());
		} catch(Exception e){
			e.printStackTrace();
		}

	}

	public static void readBlockByBlockBytes(Socket socket, boolean typeAsByte) {
		try {
			Charset utf8 = Charset.forName("UTF-8");
			DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			DataOutputStream output = new DataOutputStream(socket.getOutputStream());
			int generalSize = 0;

			for (int iter = 0; iter < ITERATIONS; iter++) {
				long startNano = System.nanoTime();
				int size = input.readInt();
				generalSize = size;
				List<Object> args = new ArrayList<Object>();
				for (int i = 0; i < size; i++) {
					int type;
					if (typeAsByte) {
						type = input.readByte();
					} else {
						type = input.readInt();
					}
					if (type == STRING_TYPE || type == COMMAND_TYPE || type == BYTE_TYPE) {
						int argSize = input.readInt();
						byte[] buffer = new byte[argSize];
						input.readFully(buffer);
						if (type != BYTE_TYPE) {
							String s = new String(buffer, utf8);
							args.add(s);
						} else {
							args.add(buffer);
						}
					} else if (type == INT_TYPE) {
						args.add(input.readInt());
					} else if (type == LONG_TYPE) {
						args.add(input.readLong());
					} else {
						args.add(input.readDouble());
					}
				}
				output.writeByte(0);
				output.flush();
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

			System.out.println("Average MS for size " + generalSize + ": " + stats.get(generalSize).getAverage());
		} catch(Exception e){
			e.printStackTrace();
		}

	}

	public static void receive(Socket socket) {
		try {
//			readBlockByBlock(socket);
			readBlockByBlockBytes(socket, false);
//			readBlockByBlockBytes(socket, true);
//			readLineByLine(socket);
//			readBlockByBlockTotal(socket);
//			readLengthThanArgs(socket);
			socket.close();

		} catch (Exception e) {
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
