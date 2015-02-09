package com.linkedin.pinot.index.reader;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.linkedin.pinot.core.index.reader.impl.FixedBitWidthRowColDataFileReader;
import com.linkedin.pinot.core.util.CustomBitSet;

@Test
public class TestFixedBitWidthRowColDataFileReader {
	private static Logger LOG = Logger
			.getLogger(TestFixedBitWidthRowColDataFileReader.class);
	boolean debug = false;

	@Test
	public void testReadIntFromByteBuffer() {
		int maxBits = 1;
		while (maxBits < 32) {
			System.out.println("START MAX BITS:" + maxBits);
			int numElements = 100;
			CustomBitSet customBitSet = CustomBitSet.withBitLength(numElements
					* maxBits);
			int max = (int) Math.pow(2, maxBits);
			Random r = new Random();
			int[] values = new int[numElements];
			for (int i = 0; i < numElements; i++) {
				int value = r.nextInt(max);
				values[i] = value;
				System.out.println(value);
				for (int j = maxBits - 1; j >= 0; j--) {
					if ((value & (1 << j)) != 0) {
						System.out.print("1");
						customBitSet.setBit(i * maxBits + (maxBits - j - 1));
					} else {
						System.out.print("0");
					}
				}
				System.out.println();
			}
			customBitSet.print();
			int bitPos = 0;
			for (int i = 0; i < numElements; i++) {
				bitPos = i * maxBits;
				int readInt = customBitSet.readInt(bitPos, bitPos + maxBits);
				if (readInt != values[i]) {
					readInt = customBitSet.readInt(bitPos, bitPos + maxBits);
				}
				System.out.println(i + "  Expected:" + values[i] + " Actual:"
						+ readInt);
				Assert.assertEquals(readInt, values[i]);
			}
			System.out.println("END MAX BITS:" + maxBits);
			maxBits = maxBits + 1;

		}
	}

	/**
	 * Tests only positive numbers
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSingleColUnsigned() throws Exception {
		int[] maxBitArray = new int[] { 4 };

		for (int maxBits : maxBitArray) {
			String fileName = "test" + maxBits + "FixedBitWidthSingleCol";
			File file = new File(fileName);
			try {
				System.out.println("START MAX BITS:" + maxBits);
				int numElements = 100;
				CustomBitSet bitset = CustomBitSet.withBitLength(numElements
						* maxBits);
				int max = (int) Math.pow(2, maxBits);
				Random r = new Random();
				int[] values = new int[numElements];
				for (int i = 0; i < numElements; i++) {
					int value = r.nextInt(max);
					values[i] = value;
					for (int j = maxBits - 1; j >= 0; j--) {
						if ((value & (1 << j)) != 0) {
							bitset.setBit(i * maxBits + (maxBits - j - 1));
						}
					}
				}
				byte[] byteArray = bitset.toByteArray();

				FileOutputStream fos = new FileOutputStream(file);
				fos.write(byteArray);
				fos.close();
				FixedBitWidthRowColDataFileReader reader;
				reader = FixedBitWidthRowColDataFileReader.forMmap(file,
						numElements, 1, new int[] { maxBits });
				for (int i = 0; i < numElements; i++) {
					int readInt = reader.getInt(i, 0);
					System.out.println(i + "  Expected:" + values[i]
							+ " Actual:" + readInt);
					Assert.assertEquals(readInt, values[i]);
				}
				System.out.println("END MAX BITS:" + maxBits);
			} finally {
				file.delete();

			}

		}
	}

	/**
	 * Tests both positive and negative numbers
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSingleColSigned() throws Exception {
		int[] maxBitArray = new int[] { 4 };

		for (int maxBits : maxBitArray) {
			String fileName = "test" + maxBits + "FixedBitWidthSingleCol";
			File file = new File(fileName);
			try {
				System.out.println("START MAX BITS:" + maxBits);
				int numElements = 100;
				int requiredBits = maxBits + 1;
				CustomBitSet bitset = CustomBitSet.withBitLength(numElements
						* requiredBits);
				int max = (int) Math.pow(2, maxBits);
				Random r = new Random();
				int[] values = new int[numElements];
				int offset = max - 1;
				for (int i = 0; i < numElements; i++) {
					int value = r.nextInt(max);
					if (Math.random() > .5) {
						value = -1 * value;
					}
					values[i] = value;
					int offsetValue = offset + value;
					for (int j = requiredBits - 1; j >= 0; j--) {
						if ((offsetValue & (1 << j)) != 0) {
							bitset.setBit(i * requiredBits
									+ (requiredBits - j - 1));
						}
					}
				}
				byte[] byteArray = bitset.toByteArray();

				FileOutputStream fos = new FileOutputStream(file);
				fos.write(byteArray);
				fos.close();
				FixedBitWidthRowColDataFileReader reader;
				reader = FixedBitWidthRowColDataFileReader.forMmap(file,
						numElements, 1, new int[] { maxBits },
						new boolean[] { true });
				for (int i = 0; i < numElements; i++) {
					int readInt = reader.getInt(i, 0);
					System.out.println(i + "  Expected:" + values[i]
							+ " Actual:" + readInt);
					Assert.assertEquals(readInt, values[i]);
				}
				System.out.println("END MAX BITS:" + maxBits);
			} finally {
				file.delete();

			}

		}
	}
}
