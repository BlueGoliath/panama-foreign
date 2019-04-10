/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @modules jdk.incubator.vector
 * @run testng CovarOverrideTest
 *
 */

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.Vector;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.testng.Assert.assertTrue;

public class CovarOverrideTest {

    static final Set<String> NON_COVARIENT_RETURNING_METHOD_NAMES_ON_VECTOR =
            Set.of("cast", "reinterpret", "reshape");

    @DataProvider
    public static Object[][] classesProvider() {
        return List.of(
                ByteVector.class,
                ShortVector.class,
                IntVector.class,
                FloatVector.class,
                DoubleVector.class).
                stream().
                map(c -> new Object[]{c}).
                toArray(Object[][]::new);
    }

    @Test(dataProvider = "classesProvider")
    public void testCovarientOverridesExist(Class<?> c) {
        Class<?> superClass = c.getSuperclass();

        Class<?> vectorClass = c;
        if (superClass == Vector.Species.class) {
            vectorClass = c.getDeclaringClass();
        }

        List<Method> notFound = new ArrayList<>();
        List<Method> notCovarientlyOverridden = new ArrayList<>();
        for (Method superMethod : getVectorReturningMethods(superClass)) {
            try {
                Method overrideMethod = c.getDeclaredMethod(superMethod.getName(), superMethod.getParameterTypes());
                if (vectorClass != overrideMethod.getReturnType()) {
                    notCovarientlyOverridden.add(overrideMethod);
                }
            }
            catch (NoSuchMethodException e) {
                notFound.add(superMethod);
            }
        }

        if (!notFound.isEmpty()) {
            System.out.println("  Methods not found on sub-type " + c.getName());
            notFound.forEach(m -> System.out.println("    " + m));
        }

        if (!notCovarientlyOverridden.isEmpty()) {
            System.out.println("  Methods not covariently overridden on sub-type " + c.getName());
            notCovarientlyOverridden.forEach(m -> System.out.println("    " + m));
        }

        assertTrue(notFound.isEmpty() && notCovarientlyOverridden.isEmpty());
    }

    static List<Method> getVectorReturningMethods(Class<?> c) {
        var filteredMethods = Stream.of(c.getDeclaredMethods()).
                filter(m -> Modifier.isPublic(m.getModifiers())).
                filter(m -> Vector.class == m.getReturnType());
        if (c == Vector.class) {
            filteredMethods = filteredMethods.
                    filter(m -> !NON_COVARIENT_RETURNING_METHOD_NAMES_ON_VECTOR.contains(m.getName()));
        }
        return filteredMethods.collect(toList());
    }
}
