/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have
 * questions.
 */

/*
 * @test
 * @modules jdk.incubator.vector
 * @run testng Byte256VectorTests
 *
 */

import jdk.incubator.vector.Shapes;
import jdk.incubator.vector.Vector;

import jdk.incubator.vector.ByteVector;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Test
public class Byte256VectorTests extends AbstractVectorTest {

    static final ByteVector.ByteSpecies<Shapes.S256Bit> SPECIES = (ByteVector.ByteSpecies<Shapes.S256Bit>)
                Vector.speciesInstance(Byte.class, Shapes.S_256_BIT);


    interface FUnOp {
        byte apply(byte a);
    }

    static void assertArraysEquals(byte[] a, byte[] r, FUnOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(f.apply(a[i]), r[i]);
            }
        } catch (AssertionError e) {
            Assert.assertEquals(f.apply(a[i]), r[i], "at index #" + i);
        }
    }

    static void assertArraysEquals(byte[] a, byte[] r, boolean[] mask, FUnOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(mask[i % SPECIES.length()] ? f.apply(a[i]) : a[i], r[i]);
            }
        } catch (AssertionError e) {
            Assert.assertEquals(mask[i % SPECIES.length()] ? f.apply(a[i]) : a[i], r[i], "at index #" + i);
        }
    }

    interface FBinOp {
        byte apply(byte a, byte b);
    }

    interface FBinMaskOp {
        byte apply(byte a, byte b, boolean m);

        static FBinMaskOp lift(FBinOp f) {
            return (a, b, m) -> m ? f.apply(a, b) : a;
        }
    }

    static void assertArraysEquals(byte[] a, byte[] b, byte[] r, FBinOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(f.apply(a[i], b[i]), r[i]);
            }
        } catch (AssertionError e) {
            Assert.assertEquals(f.apply(a[i], b[i]), r[i], "at index #" + i);
        }
    }

    static void assertArraysEquals(byte[] a, byte[] b, byte[] r, boolean[] mask, FBinOp f) {
        assertArraysEquals(a, b, r, mask, FBinMaskOp.lift(f));
    }

    static void assertArraysEquals(byte[] a, byte[] b, byte[] r, boolean[] mask, FBinMaskOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(f.apply(a[i], b[i], mask[i % SPECIES.length()]), r[i]);
            }
        } catch (AssertionError err) {
            Assert.assertEquals(f.apply(a[i], b[i], mask[i % SPECIES.length()]), r[i], "at index #" + i + ", a[i] = " + a[i] + ", b[i] = " + b[i] + ", mask = " + mask[i % SPECIES.length()]);
        }
    }
    
    static final List<IntFunction<byte[]>> BYTE_GENERATORS = List.of(
            withToString("byte[i * 5]", (int s) -> {
                return fill(s * 1000,
                            i -> (byte)(i * 5));
            }),
            withToString("byte[i + 1]", (int s) -> {
                return fill(s * 1000,
                            i -> (((byte)(i + 1) == 0) ? 1 : (byte)(i + 1)));
            })
    );

    // Create combinations of pairs
    // @@@ Might be sensitive to order e.g. div by 0
    static final List<List<IntFunction<byte[]>>> BYTE_GENERATOR_PAIRS =
        Stream.of(BYTE_GENERATORS.get(0)).
                flatMap(fa -> BYTE_GENERATORS.stream().skip(1).map(fb -> List.of(fa, fb))).
                collect(Collectors.toList());

    @DataProvider
    public Object[][] byteBinaryOpProvider() {
        return BYTE_GENERATOR_PAIRS.stream().map(List::toArray).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] byteBinaryOpMaskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
                flatMap(fm -> BYTE_GENERATOR_PAIRS.stream().map(lfa -> {
                    return Stream.concat(lfa.stream(), Stream.of(fm)).toArray();
                })).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] byteUnaryOpProvider() {
        return BYTE_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] byteUnaryOpMaskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
                flatMap(fm -> BYTE_GENERATORS.stream().map(fa -> {
                    return new Object[] {fa, fm};
                })).
                toArray(Object[][]::new);
    }

    static final List<IntFunction<byte[]>> BYTE_COMPARE_GENERATORS = List.of(
            withToString("byte[i]", (int s) -> {
                return fill(s * 1000,
                            i -> (byte)i);
            }),
            withToString("byte[i + 1]", (int s) -> {
                return fill(s * 1000,
                            i -> (byte)(i + 1));
            }),
            withToString("byte[i - 2]", (int s) -> {
                return fill(s * 1000,
                            i -> (byte)(i - 2));
            }),
            withToString("byte[zigZag(i)]", (int s) -> {
                return fill(s * 1000,
                            i -> i%3 == 0 ? (byte)i : (i%3 == 1 ? (byte)(i + 1) : (byte)(i - 2)));
            }),
            withToString("byte[cornerComparisonCase(i)]", (int s) -> {
                return fill(s * 1000,
                            i -> cornerComparisonCase(i));
            })
    );

    static final List<List<IntFunction<byte[]>>> BYTE_COMPARE_GENERATOR_PAIRS =
        BYTE_COMPARE_GENERATORS.stream().
                flatMap(fa -> BYTE_COMPARE_GENERATORS.stream().map(fb -> List.of(fa, fb))).
                collect(Collectors.toList());

    @DataProvider
    public Object[][] byteCompareOpProvider() {
        return BYTE_COMPARE_GENERATOR_PAIRS.stream().map(List::toArray).
                toArray(Object[][]::new);
    }

    interface ToByteF {
        byte apply(int i);
    }

    static byte[] fill(int s , ToByteF f) {
        return fill(new byte[s], f);
    }

    static byte[] fill(byte[] a, ToByteF f) {
        for (int i = 0; i < a.length; i++) {
            a[i] = f.apply(i);
        }
        return a;
    }
    
    static byte cornerComparisonCase(int i) {
        switch(i % 5) {
            case 0:
                return Byte.MAX_VALUE;
            case 1:
                return Byte.MIN_VALUE;
            case 2:
                return Byte.MIN_VALUE;
            case 3:
                return Byte.MAX_VALUE;
            default:
                return (byte)0;
        }
    }


    static byte add(byte a, byte b) {
        return (byte)(a + b);
    }

    @Test(dataProvider = "byteBinaryOpProvider", invocationCount = 10)
    static void addByte256VectorTests(IntFunction<byte[]> fa, IntFunction<byte[]> fb) {
        byte[] a = fa.apply(SPECIES.length());
        byte[] b = fb.apply(SPECIES.length());
        byte[] r = new byte[a.length];

        // Computation.
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            ByteVector<Shapes.S256Bit> av = SPECIES.fromArray(a, i);
            ByteVector<Shapes.S256Bit> bv = SPECIES.fromArray(b, i);
            av.add(bv).intoArray(r, i);
        }

        assertArraysEquals(a, b, r, Byte256VectorTests::add);
    }

    @Test(dataProvider = "byteBinaryOpMaskProvider", invocationCount = 10)
    static void addByte256VectorTests(IntFunction<byte[]> fa, IntFunction<byte[]> fb,
                                          IntFunction<boolean[]> fm) {
        byte[] a = fa.apply(SPECIES.length());
        byte[] b = fb.apply(SPECIES.length());
        byte[] r = new byte[a.length];
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Byte, Shapes.S256Bit> vmask = SPECIES.constantMask(mask);

        // Computation.
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            ByteVector<Shapes.S256Bit> av = SPECIES.fromArray(a, i);
            ByteVector<Shapes.S256Bit> bv = SPECIES.fromArray(b, i);
            av.add(bv, vmask).intoArray(r, i);
        }

        assertArraysEquals(a, b, r, mask, Byte256VectorTests::add);
    }

    static byte sub(byte a, byte b) {
        return (byte)(a - b);
    }

    @Test(dataProvider = "byteBinaryOpProvider", invocationCount = 10)
    static void subByte256VectorTests(IntFunction<byte[]> fa, IntFunction<byte[]> fb) {
        byte[] a = fa.apply(SPECIES.length());
        byte[] b = fb.apply(SPECIES.length());
        byte[] r = new byte[a.length];

        // Computation.
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            ByteVector<Shapes.S256Bit> av = SPECIES.fromArray(a, i);
            ByteVector<Shapes.S256Bit> bv = SPECIES.fromArray(b, i);
            av.sub(bv).intoArray(r, i);
        }

        assertArraysEquals(a, b, r, Byte256VectorTests::sub);
    }

    @Test(dataProvider = "byteBinaryOpMaskProvider", invocationCount = 10)
    static void subByte256VectorTests(IntFunction<byte[]> fa, IntFunction<byte[]> fb,
                                          IntFunction<boolean[]> fm) {
        byte[] a = fa.apply(SPECIES.length());
        byte[] b = fb.apply(SPECIES.length());
        byte[] r = new byte[a.length];
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Byte, Shapes.S256Bit> vmask = SPECIES.constantMask(mask);

        // Computation.
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            ByteVector<Shapes.S256Bit> av = SPECIES.fromArray(a, i);
            ByteVector<Shapes.S256Bit> bv = SPECIES.fromArray(b, i);
            av.sub(bv, vmask).intoArray(r, i);
        }

        assertArraysEquals(a, b, r, mask, Byte256VectorTests::sub);
    }

    static byte div(byte a, byte b) {
        return (byte)(a / b);
    }

    @Test(dataProvider = "byteBinaryOpProvider", invocationCount = 10)
    static void divByte256VectorTests(IntFunction<byte[]> fa, IntFunction<byte[]> fb) {
        byte[] a = fa.apply(SPECIES.length());
        byte[] b = fb.apply(SPECIES.length());
        byte[] r = new byte[a.length];

        // Computation.
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            ByteVector<Shapes.S256Bit> av = SPECIES.fromArray(a, i);
            ByteVector<Shapes.S256Bit> bv = SPECIES.fromArray(b, i);
            av.div(bv).intoArray(r, i);
        }

        assertArraysEquals(a, b, r, Byte256VectorTests::div);
    }

    @Test(dataProvider = "byteBinaryOpMaskProvider", invocationCount = 10)
    static void divByte256VectorTests(IntFunction<byte[]> fa, IntFunction<byte[]> fb,
                                          IntFunction<boolean[]> fm) {
        byte[] a = fa.apply(SPECIES.length());
        byte[] b = fb.apply(SPECIES.length());
        byte[] r = new byte[a.length];
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Byte, Shapes.S256Bit> vmask = SPECIES.constantMask(mask);

        // Computation.
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            ByteVector<Shapes.S256Bit> av = SPECIES.fromArray(a, i);
            ByteVector<Shapes.S256Bit> bv = SPECIES.fromArray(b, i);
            av.div(bv, vmask).intoArray(r, i);
        }

        assertArraysEquals(a, b, r, mask, Byte256VectorTests::div);
    }

    static byte mul(byte a, byte b) {
        return (byte)(a * b);
    }

    @Test(dataProvider = "byteBinaryOpProvider", invocationCount = 10)
    static void mulByte256VectorTests(IntFunction<byte[]> fa, IntFunction<byte[]> fb) {
        byte[] a = fa.apply(SPECIES.length());
        byte[] b = fb.apply(SPECIES.length());
        byte[] r = new byte[a.length];

        // Computation.
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            ByteVector<Shapes.S256Bit> av = SPECIES.fromArray(a, i);
            ByteVector<Shapes.S256Bit> bv = SPECIES.fromArray(b, i);
            av.mul(bv).intoArray(r, i);
        }

        assertArraysEquals(a, b, r, Byte256VectorTests::mul);
    }

    @Test(dataProvider = "byteBinaryOpMaskProvider", invocationCount = 10)
    static void mulByte256VectorTests(IntFunction<byte[]> fa, IntFunction<byte[]> fb,
                                          IntFunction<boolean[]> fm) {
        byte[] a = fa.apply(SPECIES.length());
        byte[] b = fb.apply(SPECIES.length());
        byte[] r = new byte[a.length];
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Byte, Shapes.S256Bit> vmask = SPECIES.constantMask(mask);

        // Computation.
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            ByteVector<Shapes.S256Bit> av = SPECIES.fromArray(a, i);
            ByteVector<Shapes.S256Bit> bv = SPECIES.fromArray(b, i);
            av.mul(bv, vmask).intoArray(r, i);
        }

        assertArraysEquals(a, b, r, mask, Byte256VectorTests::mul);
    }


    static byte and(byte a, byte b) {
        return (byte)(a & b);
    }



    @Test(dataProvider = "byteBinaryOpProvider", invocationCount = 10)
    static void andByte256VectorTests(IntFunction<byte[]> fa, IntFunction<byte[]> fb) {
        byte[] a = fa.apply(SPECIES.length());
        byte[] b = fb.apply(SPECIES.length());
        byte[] r = new byte[a.length];

        // Computation.
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            ByteVector<Shapes.S256Bit> av = SPECIES.fromArray(a, i);
            ByteVector<Shapes.S256Bit> bv = SPECIES.fromArray(b, i);
            av.and(bv).intoArray(r, i);
        }

        assertArraysEquals(a, b, r, Byte256VectorTests::and);
    }



    @Test(dataProvider = "byteBinaryOpMaskProvider", invocationCount = 10)
    static void andByte256VectorTests(IntFunction<byte[]> fa, IntFunction<byte[]> fb,
                                          IntFunction<boolean[]> fm) {
        byte[] a = fa.apply(SPECIES.length());
        byte[] b = fb.apply(SPECIES.length());
        byte[] r = new byte[a.length];
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Byte, Shapes.S256Bit> vmask = SPECIES.constantMask(mask);

        // Computation.
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            ByteVector<Shapes.S256Bit> av = SPECIES.fromArray(a, i);
            ByteVector<Shapes.S256Bit> bv = SPECIES.fromArray(b, i);
            av.and(bv, vmask).intoArray(r, i);
        }

        assertArraysEquals(a, b, r, mask, Byte256VectorTests::and);
    }



    static byte or(byte a, byte b) {
        return (byte)(a | b);
    }



    @Test(dataProvider = "byteBinaryOpProvider", invocationCount = 10)
    static void orByte256VectorTests(IntFunction<byte[]> fa, IntFunction<byte[]> fb) {
        byte[] a = fa.apply(SPECIES.length());
        byte[] b = fb.apply(SPECIES.length());
        byte[] r = new byte[a.length];

        // Computation.
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            ByteVector<Shapes.S256Bit> av = SPECIES.fromArray(a, i);
            ByteVector<Shapes.S256Bit> bv = SPECIES.fromArray(b, i);
            av.or(bv).intoArray(r, i);
        }

        assertArraysEquals(a, b, r, Byte256VectorTests::or);
    }



    @Test(dataProvider = "byteBinaryOpMaskProvider", invocationCount = 10)
    static void orByte256VectorTests(IntFunction<byte[]> fa, IntFunction<byte[]> fb,
                                          IntFunction<boolean[]> fm) {
        byte[] a = fa.apply(SPECIES.length());
        byte[] b = fb.apply(SPECIES.length());
        byte[] r = new byte[a.length];
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Byte, Shapes.S256Bit> vmask = SPECIES.constantMask(mask);

        // Computation.
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            ByteVector<Shapes.S256Bit> av = SPECIES.fromArray(a, i);
            ByteVector<Shapes.S256Bit> bv = SPECIES.fromArray(b, i);
            av.or(bv, vmask).intoArray(r, i);
        }

        assertArraysEquals(a, b, r, mask, Byte256VectorTests::or);
    }



    static byte xor(byte a, byte b) {
        return (byte)(a ^ b);
    }



    @Test(dataProvider = "byteBinaryOpProvider", invocationCount = 10)
    static void xorByte256VectorTests(IntFunction<byte[]> fa, IntFunction<byte[]> fb) {
        byte[] a = fa.apply(SPECIES.length());
        byte[] b = fb.apply(SPECIES.length());
        byte[] r = new byte[a.length];

        // Computation.
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            ByteVector<Shapes.S256Bit> av = SPECIES.fromArray(a, i);
            ByteVector<Shapes.S256Bit> bv = SPECIES.fromArray(b, i);
            av.xor(bv).intoArray(r, i);
        }

        assertArraysEquals(a, b, r, Byte256VectorTests::xor);
    }



    @Test(dataProvider = "byteBinaryOpMaskProvider", invocationCount = 10)
    static void xorByte256VectorTests(IntFunction<byte[]> fa, IntFunction<byte[]> fb,
                                          IntFunction<boolean[]> fm) {
        byte[] a = fa.apply(SPECIES.length());
        byte[] b = fb.apply(SPECIES.length());
        byte[] r = new byte[a.length];
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Byte, Shapes.S256Bit> vmask = SPECIES.constantMask(mask);

        // Computation.
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            ByteVector<Shapes.S256Bit> av = SPECIES.fromArray(a, i);
            ByteVector<Shapes.S256Bit> bv = SPECIES.fromArray(b, i);
            av.xor(bv, vmask).intoArray(r, i);
        }

        assertArraysEquals(a, b, r, mask, Byte256VectorTests::xor);
    }


    static byte max(byte a, byte b) {
        return (byte)(Math.max(a, b));
    }

    @Test(dataProvider = "byteBinaryOpProvider", invocationCount = 10)
    static void maxByte256VectorTests(IntFunction<byte[]> fa, IntFunction<byte[]> fb) {
        byte[] a = fa.apply(SPECIES.length());
        byte[] b = fb.apply(SPECIES.length());
        byte[] r = new byte[a.length];

        // Computation.
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            ByteVector<Shapes.S256Bit> av = SPECIES.fromArray(a, i);
            ByteVector<Shapes.S256Bit> bv = SPECIES.fromArray(b, i);
            av.max(bv).intoArray(r, i);
        }

        assertArraysEquals(a, b, r, Byte256VectorTests::max);
    }

    static byte min(byte a, byte b) {
        return (byte)(Math.min(a, b));
    }

    @Test(dataProvider = "byteBinaryOpProvider", invocationCount = 10)
    static void minByte256VectorTests(IntFunction<byte[]> fa, IntFunction<byte[]> fb) {
        byte[] a = fa.apply(SPECIES.length());
        byte[] b = fb.apply(SPECIES.length());
        byte[] r = new byte[a.length];

        // Computation.
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            ByteVector<Shapes.S256Bit> av = SPECIES.fromArray(a, i);
            ByteVector<Shapes.S256Bit> bv = SPECIES.fromArray(b, i);
            av.min(bv).intoArray(r, i);
        }

        assertArraysEquals(a, b, r, Byte256VectorTests::min);
    }

    @Test(dataProvider = "byteCompareOpProvider", invocationCount = 10)
    static void lessThanByte256VectorTests(IntFunction<byte[]> fa, IntFunction<byte[]> fb) {
        byte[] a = fa.apply(SPECIES.length());
        byte[] b = fb.apply(SPECIES.length());

        // Computation.
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            ByteVector<Shapes.S256Bit> av = SPECIES.fromArray(a, i);
            ByteVector<Shapes.S256Bit> bv = SPECIES.fromArray(b, i);
            Vector.Mask<Byte, Shapes.S256Bit> mv = av.lessThan(bv);

            // Check results as part of computation.
            for (int j = 0; j < SPECIES.length(); j++) {
                Assert.assertEquals(mv.getElement(j), a[i + j] < b[i + j]);
            }
        }
    }

    @Test(dataProvider = "byteCompareOpProvider", invocationCount = 10)
    static void greaterThanByte256VectorTests(IntFunction<byte[]> fa, IntFunction<byte[]> fb) {
        byte[] a = fa.apply(SPECIES.length());
        byte[] b = fb.apply(SPECIES.length());

        // Computation.
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            ByteVector<Shapes.S256Bit> av = SPECIES.fromArray(a, i);
            ByteVector<Shapes.S256Bit> bv = SPECIES.fromArray(b, i);
            Vector.Mask<Byte, Shapes.S256Bit> mv = av.greaterThan(bv);

            // Check results as part of computation.
            for (int j = 0; j < SPECIES.length(); j++) {
                Assert.assertEquals(mv.getElement(j), a[i + j] > b[i + j]);
            }
        }
    }

    @Test(dataProvider = "byteCompareOpProvider", invocationCount = 10)
    static void equalByte256VectorTests(IntFunction<byte[]> fa, IntFunction<byte[]> fb) {
        byte[] a = fa.apply(SPECIES.length());
        byte[] b = fb.apply(SPECIES.length());

        // Computation.
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            ByteVector<Shapes.S256Bit> av = SPECIES.fromArray(a, i);
            ByteVector<Shapes.S256Bit> bv = SPECIES.fromArray(b, i);
            Vector.Mask<Byte, Shapes.S256Bit> mv = av.equal(bv);

            // Check results as part of computation.
            for (int j = 0; j < SPECIES.length(); j++) {
                Assert.assertEquals(mv.getElement(j), a[i + j] == b[i + j]);
            }
        }
    }

    @Test(dataProvider = "byteCompareOpProvider", invocationCount = 10)
    static void notEqualByte256VectorTests(IntFunction<byte[]> fa, IntFunction<byte[]> fb) {
        byte[] a = fa.apply(SPECIES.length());
        byte[] b = fb.apply(SPECIES.length());

        // Computation.
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            ByteVector<Shapes.S256Bit> av = SPECIES.fromArray(a, i);
            ByteVector<Shapes.S256Bit> bv = SPECIES.fromArray(b, i);
            Vector.Mask<Byte, Shapes.S256Bit> mv = av.notEqual(bv);

            // Check results as part of computation.
            for (int j = 0; j < SPECIES.length(); j++) {
                Assert.assertEquals(mv.getElement(j), a[i + j] != b[i + j]);
            }
        }
    }

    @Test(dataProvider = "byteCompareOpProvider", invocationCount = 10)
    static void lessThanEqByte256VectorTests(IntFunction<byte[]> fa, IntFunction<byte[]> fb) {
        byte[] a = fa.apply(SPECIES.length());
        byte[] b = fb.apply(SPECIES.length());

        // Computation.
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            ByteVector<Shapes.S256Bit> av = SPECIES.fromArray(a, i);
            ByteVector<Shapes.S256Bit> bv = SPECIES.fromArray(b, i);
            Vector.Mask<Byte, Shapes.S256Bit> mv = av.lessThanEq(bv);

            // Check results as part of computation.
            for (int j = 0; j < SPECIES.length(); j++) {
                Assert.assertEquals(mv.getElement(j), a[i + j] <= b[i + j]);
            }
        }
    }

    @Test(dataProvider = "byteCompareOpProvider", invocationCount = 10)
    static void greaterThanEqByte256VectorTests(IntFunction<byte[]> fa, IntFunction<byte[]> fb) {
        byte[] a = fa.apply(SPECIES.length());
        byte[] b = fb.apply(SPECIES.length());

        // Computation.
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            ByteVector<Shapes.S256Bit> av = SPECIES.fromArray(a, i);
            ByteVector<Shapes.S256Bit> bv = SPECIES.fromArray(b, i);
            Vector.Mask<Byte, Shapes.S256Bit> mv = av.greaterThanEq(bv);

            // Check results as part of computation.
            for (int j = 0; j < SPECIES.length(); j++) {
                Assert.assertEquals(mv.getElement(j), a[i + j] >= b[i + j]);
            }
        }
    }

    static byte blend(byte a, byte b, boolean mask) {
        return mask ? b : a;
    }

    @Test(dataProvider = "byteBinaryOpMaskProvider", invocationCount = 10)
    static void blendByte256VectorTests(IntFunction<byte[]> fa, IntFunction<byte[]> fb,
                                          IntFunction<boolean[]> fm) {
        byte[] a = fa.apply(SPECIES.length());
        byte[] b = fb.apply(SPECIES.length());
        byte[] r = new byte[a.length];
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Byte, Shapes.S256Bit> vmask = SPECIES.constantMask(mask);

        // Computation.
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            ByteVector<Shapes.S256Bit> av = SPECIES.fromArray(a, i);
            ByteVector<Shapes.S256Bit> bv = SPECIES.fromArray(b, i);
            av.blend(bv, vmask).intoArray(r, i);
        }

        assertArraysEquals(a, b, r, mask, Byte256VectorTests::blend);
    }

    static byte neg(byte a) {
        return (byte)(-((byte)a));
    }

    @Test(dataProvider = "byteUnaryOpProvider", invocationCount = 10)
    static void negByte256VectorTests(IntFunction<byte[]> fa) {
        byte[] a = fa.apply(SPECIES.length());
        byte[] r = new byte[a.length];

        // Computation.
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            ByteVector<Shapes.S256Bit> av = SPECIES.fromArray(a, i);
            av.neg().intoArray(r, i);
        }

        assertArraysEquals(a, r, Byte256VectorTests::neg);
    }

    @Test(dataProvider = "byteUnaryOpMaskProvider")
    static void negMaskedByte256VectorTests(IntFunction<byte[]> fa,
                                                IntFunction<boolean[]> fm) {
        byte[] a = fa.apply(SPECIES.length());
        byte[] r = new byte[a.length];
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Byte, Shapes.S256Bit> vmask = SPECIES.constantMask(mask);

        // Computation.
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            ByteVector<Shapes.S256Bit> av = SPECIES.fromArray(a, i);
            av.neg(vmask).intoArray(r, i);
        }

        assertArraysEquals(a, r, mask, Byte256VectorTests::neg);
    }

    static byte abs(byte a) {
        return (byte)(Math.abs((byte)a));
    }

    @Test(dataProvider = "byteUnaryOpProvider", invocationCount = 10)
    static void absByte256VectorTests(IntFunction<byte[]> fa) {
        byte[] a = fa.apply(SPECIES.length());
        byte[] r = new byte[a.length];

        // Computation.
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            ByteVector<Shapes.S256Bit> av = SPECIES.fromArray(a, i);
            av.abs().intoArray(r, i);
        }

        assertArraysEquals(a, r, Byte256VectorTests::abs);
    }

    @Test(dataProvider = "byteUnaryOpMaskProvider")
    static void absMaskedByte256VectorTests(IntFunction<byte[]> fa,
                                                IntFunction<boolean[]> fm) {
        byte[] a = fa.apply(SPECIES.length());
        byte[] r = new byte[a.length];
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Byte, Shapes.S256Bit> vmask = SPECIES.constantMask(mask);

        // Computation.
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            ByteVector<Shapes.S256Bit> av = SPECIES.fromArray(a, i);
            av.abs(vmask).intoArray(r, i);
        }

        assertArraysEquals(a, r, mask, Byte256VectorTests::abs);
    }
}

