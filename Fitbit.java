
package assignment10.fitbit;

import java.awt.Color;
import java.nio.charset.StandardCharsets;

import assignment9.Fitbit1;
import jssc.SerialPortException;
import sedgewick.StdDraw;
import studio4.SerialComm;

public class Fitbit {

	final private SerialComm port;

	public Fitbit(String portname) throws SerialPortException {
		port = new SerialComm(portname);
	}

	public static void main(String[] args) throws SerialPortException {
		Fitbit fitbit = new Fitbit("/dev/cu.usbserial-DN02B7P5");
		fitbit.run();
	}

	public void run() throws SerialPortException {

		int[] stepStore = new int[902];
		for (int i = 0; i < stepStore.length; i++) stepStore[i] = i;
		double[] steps = new double[902];
		int[] stepsAxis = new int[902];
		boolean threshold = true;

		int[] sleepStore = new int[47];
		for (int i = 0; i < sleepStore.length; i++) sleepStore[i] = i;
		int[] sleep = new int[47];
		int[] sleepAxis = new int[47];

		while (true) {
			if (port.available() == true) {
				if (port.readByte() == 0x40) {
					byte typeName = port.readByte();

					switch(typeName) {
					case 0x30:
						//						System.out.println("[Debugging String]");
						short debugByte = port.readByte();
						short debugByte2 = port.readByte();
						int debugLength = (debugByte << 8) | debugByte2;

						byte[] aArray = new byte[debugLength];

						for (int i = 0; i < debugLength; i++) {
							aArray[i] = port.readByte();
						}
						String s = new String(aArray, StandardCharsets.UTF_8);
						System.out.println(s);
						break;

					case 0x31:
						//						System.out.println("[Error String]");
						short errorByte = port.readByte();
						short errorByte2 = port.readByte();
						int errorLength = (errorByte << 8) | errorByte2;

						byte[] bArray = new byte[errorLength];

						for (int i = 0; i < errorLength; i++) {
							bArray[i] = port.readByte();
						}
						String s1 = new String(bArray, StandardCharsets.UTF_8);
						System.out.println(s1);
						break;

					case 0x32:
						//						System.out.println("[Converted Temperature]");
						int conByte = port.readByte();
						if (conByte < 0) conByte = conByte + 256;
						int conByte2 = port.readByte();
						if (conByte2 < 0) conByte2 = conByte2 + 256;
						int conByte3 = port.readByte();
						if (conByte3 < 0) conByte3 = conByte3 + 256;
						int conByte4 = port.readByte();
						if (conByte4 < 0) conByte4 = conByte4 + 256;
						int conLength = (conByte << 24) | (conByte2 << 16) | (conByte3 << 8) | conByte4;

						float conLengthFloat = Float.intBitsToFloat(conLength);
						System.out.println(conLengthFloat);

						//-----------------------------------------------------------------------
						String temperature = conLengthFloat + " degrees (C').";
						StdDraw.textLeft(0, -2, temperature);

						StdDraw.show();
						//-----------------------------------------------------------------------

						break;


					case 0x33:
						//						System.out.println("[Peaks of steps]");
						int stepByte = port.readByte();
						if (stepByte < 0) stepByte = stepByte + 256;
						int stepByte2 = port.readByte();
						if (stepByte2 < 0) stepByte2 = stepByte2 + 256;
						int stepLength = (stepByte << 8) | stepByte2;

						System.out.println(stepLength);

						//-----------------------------------------------------------------------
						String stepCount = "You walked " + stepLength + " steps.";
						StdDraw.textLeft(0, -1, stepCount);
						StdDraw.show();
						//-----------------------------------------------------------------------

						break;

					case 0x34:
						//						System.out.println("[Timer of sleep in ms]");
						int sleepByte = port.readByte();
						if (sleepByte < 0) sleepByte = sleepByte + 256;
						int sleepByte2 = port.readByte();
						if (sleepByte2 < 0) sleepByte2 = sleepByte2 + 256;
						int sleepByte3 = port.readByte();
						if (sleepByte3 < 0) sleepByte3 = sleepByte3 + 256;
						int sleepByte4 = port.readByte();
						if (sleepByte4 < 0) sleepByte4 = sleepByte4 + 256;
						int sleepLength = (sleepByte << 24) | (sleepByte2 << 16) | (sleepByte3 << 8) | sleepByte4;

						System.out.println(sleepLength);

						//-----------------------------------------------------------------------
						for (int i1 = 0; i1 < steps.length; i1++) {
							steps[i1] = 0;
							stepsAxis[i1] = 0;
						}
						
						for (int i2 = 0; i2 < sleep.length - 1; i2++) sleep[i2] = sleep[i2 + 1];
						sleep[sleep.length - 1] = sleepLength;
						
						for (int j = 1; j < sleepAxis.length - 1; j++) {
							if (sleep[j - 1] + sleep[j] + sleep[j + 1] != 0) sleepAxis[j]++;
						}

						StdDraw.clear();
						drawGraph();
						drawSleep();

						StdDraw.setPenColor(Color.BLUE);
						for (int i3 = 0; i3 < sleepStore.length - 1; i3++) {
							StdDraw.line(sleepStore[i3], sleep[i3] / 20000.0, sleepStore[i3 + 1], sleep[i3 + 1] / 20000.0);

							if (sleep[i3] > 0 && sleepAxis[i3] % 10 == 0) {
								StdDraw.setPenColor(Color.BLACK);
								StdDraw.text(sleepStore[i3], -0.25, String.valueOf(sleepAxis[i3]));
								StdDraw.setPenColor(Color.lightGray);
								StdDraw.line(sleepStore[i3], 0, sleepStore[i3], 2);
								StdDraw.setPenColor(Color.BLUE);
							}
						}
						StdDraw.setPenColor(Color.BLACK);



						String sleepTime = "You slept for " + (double)sleepLength/1000.0 + " seconds.";
						StdDraw.textLeft(0, -1.4, sleepTime);

						StdDraw.show();
						//-----------------------------------------------------------------------

						break;


					case 0x35:
						//						System.out.println("[Runtime in ms]");
						int timeByte = port.readByte();
						if (timeByte < 0) timeByte = timeByte + 256;
						int timeByte2 = port.readByte();
						if (timeByte2 < 0) timeByte2 = timeByte2 + 256;
						int timeByte3 = port.readByte();
						if (timeByte3 < 0) timeByte3 = timeByte3 + 256;
						int timeByte4 = port.readByte();
						if (timeByte4 < 0) timeByte4 = timeByte4 + 256;
						int timeLength = (timeByte << 24) | (timeByte2 << 16) | (timeByte3 << 8) | timeByte4;

						//						System.out.println(timeLength);
						break;

					case 0x36:
						//						System.out.println("[Raw accelerometer points]");
						int[] rawAccelByte = new int[4];
						for (int i = 0; i < rawAccelByte.length; i++) {
							rawAccelByte[i] = port.readByte();
							if (rawAccelByte[i] < 0) rawAccelByte[i] += 256;
						}
						int rawAccelLength = (rawAccelByte[0] << 24) | (rawAccelByte[1] << 16) | (rawAccelByte[2] << 8) | rawAccelByte[3];
						float rawAccelFloat = Float.intBitsToFloat(rawAccelLength);

						//-----------------------------------------------------------------------
						for (int i1 = 0; i1 < sleep.length; i1++) {
							sleep[i1] = 0;
							sleepAxis[i1] = 0;
						}
						for (int i2 = 0; i2 < steps.length - 1; i2++) steps[i2] = steps[i2 + 1];
						steps[steps.length - 1] = rawAccelFloat;
						for (int j = 1; j < stepsAxis.length - 1; j++) {
							if (steps[j - 1] + steps[j] + steps[j + 1] != 0) stepsAxis[j]++;
						}
						
						StdDraw.clear();
						drawGraph();
						drawStep();
						
						StdDraw.setPenColor(Color.RED);
						for (int i3 = 1; i3 < stepStore.length - 1; i3++) {
							StdDraw.line((stepStore[i3] - 1) / 20.0, steps[i3], (stepStore[i3 + 1] - 1) / 20.0, steps[i3 + 1]);
									
							if (steps[i3] < 0.7) threshold = true;
							if (threshold && steps[i3] > steps[i3 - 1] && steps[i3] > steps[i3 + 1] && steps[i3] > 1.2) {
								StdDraw.setPenRadius(StdDraw.getPenRadius() * 5);
								StdDraw.point((stepStore[i3] - 1) / 20.0, steps[i3]);
								StdDraw.setPenRadius(StdDraw.getPenRadius() / 5);
								threshold = false;
							}
							
							if (steps[i3] > 0 && stepsAxis[i3] % 200 == 0) {
								StdDraw.setPenColor(Color.BLACK);
								StdDraw.text(stepStore[i3] / 20.0, -0.25, String.valueOf(stepsAxis[i3] / 20));
								StdDraw.line(stepStore[i3] / 20.0, 0, stepStore[i3] / 20.0, 2);
								StdDraw.setPenColor(Color.RED);
							}
						}
						StdDraw.setPenColor(Color.BLACK);

						//-----------------------------------------------------------------------
						break;

					case 0x37: 
						int[] speedByte = new int[4];
						for (int i = 0; i < speedByte.length; i++) {
							speedByte[i] = port.readByte();
							if (speedByte[i] < 0) speedByte[i] += 256;
						}
						int speedLength = (speedByte[0] << 24) | (speedByte[1] << 16) | (speedByte[2] << 8) | speedByte[3];
						float speedByteFloat = Float.intBitsToFloat(speedLength);

						String speed = "(" + speedByteFloat + " steps/hr)";
						StdDraw.textLeft(0, -1.5, speed);

						StdDraw.show();

					default:
						
						break;
					}
				} else {};
			}
		}
	}

	public static void drawGraph() {
		StdDraw.enableDoubleBuffering();
		StdDraw.setXscale(-7, 50);
		StdDraw.setYscale(-2.5, 3);

		java.awt.Font font = StdDraw.getFont();

		StdDraw.setFont(new java.awt.Font(font.getName(), font.getStyle(), font.getSize() * 2));
		StdDraw.text(22.5, 2.5, "132 FITBIT");
		StdDraw.setFont(font);

		StdDraw.setPenRadius(StdDraw.getPenRadius() * 2);
		StdDraw.line(0, 0, 45, 0);
		StdDraw.line(0, 0, 0, 2);
		StdDraw.setPenRadius(StdDraw.getPenRadius() / 2);
		
		StdDraw.setPenColor(Color.gray);
		StdDraw.text(21.5, -0.5, "time (ms)");
		StdDraw.setPenColor(Color.BLACK);
		StdDraw.textRight(-1, -0.25, "0");

	}

	public static void drawStep() {
		for (double d = 0.5; d <= 1.5; d += 0.5) {
			StdDraw.text(-5, 1, "accel values (g)", 90);
			StdDraw.textRight(-1, d, String.valueOf(d));
			StdDraw.setPenColor(Color.lightGray);
			StdDraw.line(0, d, 45, d);
			StdDraw.setPenColor(Color.BLACK);
		}
	}

	public static void drawSleep() {
		for (double d = 0.5; d <= 1.5; d += 0.5) {
			StdDraw.setPenColor(Color.gray);
			StdDraw.text(-5, 1, "sleep time (ms)", 90);
			StdDraw.setPenColor(Color.BLACK);
			StdDraw.textRight(-1, d, String.valueOf((int)(d * 20)));
			StdDraw.setPenColor(Color.lightGray);
			StdDraw.line(0.1, d, 45, d);
			StdDraw.setPenColor(Color.BLACK);
		}
	}

}