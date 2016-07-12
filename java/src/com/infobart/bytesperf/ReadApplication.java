package com.infobart.bytesperf;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Base64;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.Map;

public class ReadApplication {

	public static Map<Integer, DoubleSummaryStatistics> stats = new HashMap<Integer, DoubleSummaryStatistics>();

	public static void readStream(Socket socket, boolean base64) throws Exception {
		DataInputStream input = new DataInputStream(socket.getInputStream());
		DataOutputStream output = new DataOutputStream(socket.getOutputStream());
		Base64.Decoder decoder = Base64.getDecoder();

		long startNano = System.nanoTime();
		int size = input.readInt();
		byte[] data = new byte[size];
		input.readFully(data);
//		System.out.println(size);
//		System.out.println((int)data[0]);
//		System.out.println((int)data[size-1]);
		if (base64) {
			data = decoder.decode(data);
		}
		output.writeByte(0);
		output.flush();
		long stopNano = System.nanoTime();
		long timeNs = stopNano - startNano;
		double timeMs = (double)timeNs / 1000.0 / 1000.0;
		//System.out.println("Read fully " + (int)data[0] + " to " + (int)data[size-1] + " in " + (stop-start));
//		System.out.println("Read " + size + " bytes in " + timeMs + " ms or " + timeNs + " "
//				+ "ns");

		DoubleSummaryStatistics currentStats = stats.get(size);
		if (currentStats == null) {
			currentStats = new DoubleSummaryStatistics();
			stats.put(size, currentStats);
		}
		currentStats.accept(timeMs);
		System.out.println("MS: " + timeMs + ". Average MS for size " + size + ": " + currentStats.getAverage());
	}

	public static void readChannel(Socket socket, boolean direct, boolean base64) throws Exception {
		DataInputStream input = new DataInputStream(socket.getInputStream());
		DataOutputStream output = new DataOutputStream(socket.getOutputStream());
		ReadableByteChannel readChannel = Channels.newChannel(socket.getInputStream());
		Base64.Decoder decoder = Base64.getDecoder();
		ByteBuffer buffer;
		int totalSize = 0;
		byte[] data;
//		int reads = 0;

		long startNano = System.nanoTime();
		int size = input.readInt();
		if (direct) {
			buffer = ByteBuffer.allocateDirect(size);
		} else {
			buffer = ByteBuffer.allocate(size);
		}
		while (totalSize < size) {
			totalSize += readChannel.read(buffer);
//			reads++;
		}

		buffer.flip();
		if (buffer.hasArray()) {
			data = buffer.array();
		} else {
			data = new byte[size];
			buffer.get(data);
		}
		if (base64) {
			data = decoder.decode(data);
		}
		output.writeByte(0);
		output.flush();
		long stopNano = System.nanoTime();
		long timeNs = stopNano - startNano;
		double timeMs = (double)timeNs / 1000.0 / 1000.0;
//		buffer.flip();
//		System.out.println((int)buffer.get(0));
//		System.out.println((int)buffer.get(size-1));

		DoubleSummaryStatistics currentStats = stats.get(size);
		if (currentStats == null) {
			currentStats = new DoubleSummaryStatistics();
			stats.put(size, currentStats);
		}
		currentStats.accept(timeMs);
		System.out.println("MS: " + timeMs + ". Average MS for size " + size + ": " + currentStats.getAverage());
//		System.out.println("Reads: " + reads);

	}

	public static void receive(Socket socket) {
		try {
//			readStream(socket, true);
			readChannel(socket, false, true);
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
