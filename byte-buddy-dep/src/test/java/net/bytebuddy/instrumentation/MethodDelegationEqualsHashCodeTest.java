package net.bytebuddy.instrumentation;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.when;

public class MethodDelegationEqualsHashCodeTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testStaticMethodDelegation() throws Exception {
        assertThat(MethodDelegation.to(Foo.class).hashCode(), is(MethodDelegation.to(Foo.class).hashCode()));
        assertThat(MethodDelegation.to(Foo.class), is(MethodDelegation.to(Foo.class)));
        assertThat(MethodDelegation.to(Foo.class).hashCode(), not(is(MethodDelegation.to(Bar.class).hashCode())));
        assertThat(MethodDelegation.to(Foo.class), not(is(MethodDelegation.to(Bar.class))));
    }

    @Test
    public void testStaticInstanceDelegation() throws Exception {
        assertThat(MethodDelegation.to(new Foo()).hashCode(), is(MethodDelegation.to(new Foo()).hashCode()));
        assertThat(MethodDelegation.to(new Foo()), is(MethodDelegation.to(new Foo())));
        assertThat(MethodDelegation.to(new Foo()).hashCode(), not(is(MethodDelegation.to(new Bar()).hashCode())));
        assertThat(MethodDelegation.to(new Foo()), not(is(MethodDelegation.to(new Bar()))));
    }

    @Test
    public void testStaticInstanceDelegationWithFieldName() throws Exception {
        assertThat(MethodDelegation.to(new Foo(), FOO).hashCode(), is(MethodDelegation.to(new Foo(), FOO).hashCode()));
        assertThat(MethodDelegation.to(new Foo(), FOO), is(MethodDelegation.to(new Foo(), FOO)));
        assertThat(MethodDelegation.to(new Foo(), FOO).hashCode(), not(is(MethodDelegation.to(new Foo(), BAR).hashCode())));
        assertThat(MethodDelegation.to(new Foo()), not(is(MethodDelegation.to(new Foo(), BAR))));
    }

    @Test
    public void testInstanceFieldDelegation() throws Exception {
        assertThat(MethodDelegation.toInstanceField(Foo.class, FOO).hashCode(), is(MethodDelegation.toInstanceField(Foo.class, FOO).hashCode()));
        assertThat(MethodDelegation.toInstanceField(Foo.class, FOO), is(MethodDelegation.toInstanceField(Foo.class, FOO)));
        assertThat(MethodDelegation.toInstanceField(Foo.class, FOO).hashCode(), not(is(MethodDelegation.toInstanceField(Bar.class, FOO).hashCode())));
        assertThat(MethodDelegation.toInstanceField(Foo.class, FOO), not(is(MethodDelegation.toInstanceField(Bar.class, FOO))));
        assertThat(MethodDelegation.toInstanceField(Foo.class, FOO).hashCode(), not(is(MethodDelegation.toInstanceField(Foo.class, BAR).hashCode())));
        assertThat(MethodDelegation.toInstanceField(Foo.class, FOO), not(is(MethodDelegation.toInstanceField(Foo.class, BAR))));
    }

    @Test
    public void testConstructorDelegation() throws Exception {
        assertThat(MethodDelegation.toConstructor(Foo.class).hashCode(), is(MethodDelegation.toConstructor(Foo.class).hashCode()));
        assertThat(MethodDelegation.toConstructor(Foo.class), is(MethodDelegation.toConstructor(Foo.class)));
        assertThat(MethodDelegation.toConstructor(Foo.class).hashCode(), not(is(MethodDelegation.toConstructor(Bar.class).hashCode())));
        assertThat(MethodDelegation.toConstructor(Foo.class), not(is(MethodDelegation.toConstructor(Bar.class))));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodDelegation.class).refine(new ObjectPropertyAssertion.Refinement<List<?>>() {
            @Override
            public void apply(List<?> mock) {
                when(mock.size()).thenReturn(1);
            }
        }).apply();
        ObjectPropertyAssertion.of(MethodDelegation.Appender.class).apply();
        ObjectPropertyAssertion.of(MethodDelegation.InstrumentationDelegate.ForStaticField.class).apply();
        ObjectPropertyAssertion.of(MethodDelegation.InstrumentationDelegate.ForInstanceField.class).apply();
        ObjectPropertyAssertion.of(MethodDelegation.InstrumentationDelegate.ForConstruction.class).apply();
    }

    public static class Foo {

        public static void foo() {
            /* empty */
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null && obj.getClass() == getClass();
        }

        @Override
        public int hashCode() {
            return 31;
        }
    }

    public static class Bar {

        public static void bar() {
            /* empty */
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null && obj.getClass() == getClass();
        }

        @Override
        public int hashCode() {
            return 27;
        }
    }
}
