package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.IgnoreForBinding;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationIgnoreForBindingTest extends AbstractInstrumentationTest {

    private static final String FOO = "FOO", BAR = "bar";

    @Test
    public void testIgnoreForBinding() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(Bar.class));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(), is(FOO));
    }

    public static class Foo {

        public String foo() {
            return null;
        }
    }

    public static class Bar {

        public static String bar() {
            return FOO;
        }

        @IgnoreForBinding
        public static String qux() {
            return BAR;
        }
    }
}
