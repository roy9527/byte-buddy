package net.bytebuddy.instrumentation;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class LoadedTypeInitializerForStaticFieldTest {

    private static final String FOO = "foo";

    @Test
    public void testAccessibleField() throws Exception {
        Object object = new Object();
        LoadedTypeInitializer loadedTypeInitializer = LoadedTypeInitializer.ForStaticField.accessible(FOO, object);
        assertThat(loadedTypeInitializer.isAlive(), is(true));
        loadedTypeInitializer.onLoad(Foo.class);
        assertThat(Foo.foo, is(object));
    }

    @Test
    public void testNonAccessibleField() throws Exception {
        Object object = new Object();
        LoadedTypeInitializer loadedTypeInitializer = LoadedTypeInitializer.ForStaticField.nonAccessible(FOO, object);
        assertThat(loadedTypeInitializer.isAlive(), is(true));
        loadedTypeInitializer.onLoad(Bar.class);
        assertThat(Bar.foo, is(object));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonAccessibleFieldThrowsException() throws Exception {
        Object object = new Object();
        LoadedTypeInitializer loadedTypeInitializer = LoadedTypeInitializer.ForStaticField.accessible(FOO, object);
        assertThat(loadedTypeInitializer.isAlive(), is(true));
        loadedTypeInitializer.onLoad(Bar.class);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(LoadedTypeInitializer.ForStaticField.class).apply();
    }

    @SuppressWarnings("unused")
    public static class Foo {

        public static Object foo;
    }

    @SuppressWarnings("unused")
    public static class Bar {

        private static Object foo;
    }
}
