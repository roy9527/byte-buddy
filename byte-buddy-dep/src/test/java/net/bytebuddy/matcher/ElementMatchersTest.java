package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.ByteCodeElement;
import net.bytebuddy.instrumentation.ModifierReviewable;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.PrecompiledTypeClassLoader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class ElementMatchersTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final String SINGLE_DEFAULT_METHOD = "net.bytebuddy.test.precompiled.SingleDefaultMethodInterface";

    @Rule
    public MethodRule java8Rule = new JavaVersionRule(8);

    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        classLoader = new PrecompiledTypeClassLoader(getClass().getClassLoader());
    }

    @Test
    public void testIs() throws Exception {
        Object value = new Object();
        assertThat(ElementMatchers.is(value).matches(value), is(true));
        assertThat(ElementMatchers.is(value).matches(new Object()), is(false));
        assertThat(ElementMatchers.is((Object) null).matches(null), is(true));
        assertThat(ElementMatchers.is((Object) null).matches(new Object()), is(false));
    }

    @Test
    public void testIsType() throws Exception {
        assertThat(ElementMatchers.is(Object.class).matches(new TypeDescription.ForLoadedType(Object.class)), is(true));
        assertThat(ElementMatchers.is(String.class).matches(new TypeDescription.ForLoadedType(Object.class)),
                is(false));
    }

    @Test
    public void testIsMethodOrConstructor() throws Exception {
        assertThat(ElementMatchers.is(Object.class.getDeclaredMethod("toString"))
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("toString"))), is(true));
        assertThat(ElementMatchers.is(Object.class.getDeclaredMethod("toString"))
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("hashCode"))), is(false));
        assertThat(ElementMatchers.is(Object.class.getDeclaredConstructor())
                .matches(new MethodDescription.ForLoadedConstructor(Object.class.getDeclaredConstructor())), is(true));
        assertThat(ElementMatchers.is(Object.class.getDeclaredConstructor())
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("hashCode"))), is(false));
    }

    @Test
    public void testIsAnnotation() throws Exception {
        AnnotationDescription annotationDescription = new TypeDescription.ForLoadedType(IsAnnotatedWith.class)
                .getDeclaredAnnotations().ofType(IsAnnotatedWithAnnotation.class);
        assertThat(ElementMatchers.is(IsAnnotatedWith.class.getAnnotation(IsAnnotatedWithAnnotation.class))
                .matches(annotationDescription), is(true));
        assertThat(ElementMatchers.is(Other.class.getAnnotation(OtherAnnotation.class)).matches(annotationDescription),
                is(false));
    }

    @Test
    public void testNot() throws Exception {
        Object value = new Object();
        @SuppressWarnings("unchecked")
        ElementMatcher<Object> elementMatcher = mock(ElementMatcher.class);
        when(elementMatcher.matches(value)).thenReturn(true);
        assertThat(ElementMatchers.not(elementMatcher).matches(value), is(false));
        verify(elementMatcher).matches(value);
        Object otherValue = new Object();
        assertThat(ElementMatchers.not(elementMatcher).matches(otherValue), is(true));
        verify(elementMatcher).matches(otherValue);
        verifyNoMoreInteractions(elementMatcher);
    }

    @Test
    public void testAny() throws Exception {
        assertThat(ElementMatchers.any().matches(new Object()), is(true));
    }

    @Test
    public void testAnyType() throws Exception {
        assertThat(ElementMatchers.anyOf(Object.class).matches(new TypeDescription.ForLoadedType(Object.class)),
                is(true));
        assertThat(ElementMatchers.anyOf(String.class, Object.class)
                .matches(new TypeDescription.ForLoadedType(Object.class)), is(true));
        assertThat(ElementMatchers.anyOf(String.class).matches(new TypeDescription.ForLoadedType(Object.class)),
                is(false));
    }

    @Test
    public void testAnyMethodOrConstructor() throws Exception {
        Method toString = Object.class.getDeclaredMethod("toString"), hashCode = Object.class
                .getDeclaredMethod("hashCode");
        assertThat(ElementMatchers.anyOf(toString)
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("toString"))), is(true));
        assertThat(ElementMatchers.anyOf(toString, hashCode)
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("toString"))), is(true));
        assertThat(ElementMatchers.anyOf(toString)
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("hashCode"))), is(false));
        assertThat(ElementMatchers.anyOf(Object.class.getDeclaredConstructor())
                .matches(new MethodDescription.ForLoadedConstructor(Object.class.getDeclaredConstructor())), is(true));
        assertThat(ElementMatchers
                .anyOf(Object.class.getDeclaredConstructor(), String.class.getDeclaredConstructor(String.class))
                .matches(new MethodDescription.ForLoadedConstructor(Object.class.getDeclaredConstructor())), is(true));
        assertThat(ElementMatchers.anyOf(Object.class.getDeclaredConstructor())
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("hashCode"))), is(false));
    }

    @Test
    public void testAnyAnnotation() throws Exception {
        AnnotationDescription annotationDescription = new TypeDescription.ForLoadedType(IsAnnotatedWith.class)
                .getDeclaredAnnotations().ofType(IsAnnotatedWithAnnotation.class);
        assertThat(ElementMatchers.anyOf(IsAnnotatedWith.class.getAnnotation(IsAnnotatedWithAnnotation.class))
                .matches(annotationDescription), is(true));
        assertThat(ElementMatchers.anyOf(IsAnnotatedWith.class.getAnnotation(IsAnnotatedWithAnnotation.class),
                Other.class.getAnnotation(OtherAnnotation.class)).matches(annotationDescription), is(true));
        assertThat(
                ElementMatchers.anyOf(Other.class.getAnnotation(OtherAnnotation.class)).matches(annotationDescription),
                is(false));
    }

    @Test
    public void testNone() throws Exception {
        assertThat(ElementMatchers.none().matches(new Object()), is(false));
    }

    @Test
    public void testNoneType() throws Exception {
        assertThat(ElementMatchers.noneOf(Object.class).matches(new TypeDescription.ForLoadedType(Object.class)),
                is(false));
        assertThat(ElementMatchers.noneOf(String.class, Object.class)
                .matches(new TypeDescription.ForLoadedType(Object.class)), is(false));
        assertThat(ElementMatchers.noneOf(String.class).matches(new TypeDescription.ForLoadedType(Object.class)),
                is(true));
    }

    @Test
    public void testNoneMethodOrConstructor() throws Exception {
        Method toString = Object.class.getDeclaredMethod("toString"), hashCode = Object.class
                .getDeclaredMethod("hashCode");
        assertThat(ElementMatchers.noneOf(toString)
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("toString"))), is(false));
        assertThat(ElementMatchers.noneOf(toString, hashCode)
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("toString"))), is(false));
        assertThat(ElementMatchers.noneOf(toString)
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("hashCode"))), is(true));
        assertThat(ElementMatchers.noneOf(Object.class.getDeclaredConstructor())
                .matches(new MethodDescription.ForLoadedConstructor(Object.class.getDeclaredConstructor())), is(false));
        assertThat(ElementMatchers
                .noneOf(Object.class.getDeclaredConstructor(), String.class.getDeclaredConstructor(String.class))
                .matches(new MethodDescription.ForLoadedConstructor(Object.class.getDeclaredConstructor())), is(false));
        assertThat(ElementMatchers.noneOf(Object.class.getDeclaredConstructor())
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("hashCode"))), is(true));
    }

    @Test
    public void testNoneAnnotation() throws Exception {
        AnnotationDescription annotationDescription = new TypeDescription.ForLoadedType(IsAnnotatedWith.class)
                .getDeclaredAnnotations().ofType(IsAnnotatedWithAnnotation.class);
        assertThat(ElementMatchers.noneOf(IsAnnotatedWith.class.getAnnotation(IsAnnotatedWithAnnotation.class))
                .matches(annotationDescription), is(false));
        assertThat(ElementMatchers.noneOf(IsAnnotatedWith.class.getAnnotation(IsAnnotatedWithAnnotation.class),
                Other.class.getAnnotation(OtherAnnotation.class)).matches(annotationDescription), is(false));
        assertThat(
                ElementMatchers.noneOf(Other.class.getAnnotation(OtherAnnotation.class)).matches(annotationDescription),
                is(true));
    }

    @Test
    public void testAnyOf() throws Exception {
        Object value = new Object(), otherValue = new Object();
        assertThat(ElementMatchers.anyOf(value, otherValue).matches(value), is(true));
        assertThat(ElementMatchers.anyOf(value, otherValue).matches(otherValue), is(true));
        assertThat(ElementMatchers.anyOf(value, otherValue).matches(new Object()), is(false));
    }

    @Test
    public void testNoneOf() throws Exception {
        Object value = new Object(), otherValue = new Object();
        assertThat(ElementMatchers.noneOf(value, otherValue).matches(value), is(false));
        assertThat(ElementMatchers.noneOf(value, otherValue).matches(otherValue), is(false));
        assertThat(ElementMatchers.noneOf(value, otherValue).matches(new Object()), is(true));
    }

    @Test
    public void testNamed() throws Exception {
        ByteCodeElement byteCodeElement = mock(ByteCodeElement.class);
        when(byteCodeElement.getSourceCodeName()).thenReturn(FOO);
        assertThat(ElementMatchers.named(FOO).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.named(FOO.toUpperCase()).matches(byteCodeElement), is(false));
        assertThat(ElementMatchers.named(BAR).matches(byteCodeElement), is(false));
    }

    @Test
    public void testNamedIgnoreCase() throws Exception {
        ByteCodeElement byteCodeElement = mock(ByteCodeElement.class);
        when(byteCodeElement.getSourceCodeName()).thenReturn(FOO);
        assertThat(ElementMatchers.namedIgnoreCase(FOO).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.namedIgnoreCase(FOO.toUpperCase()).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.namedIgnoreCase(BAR).matches(byteCodeElement), is(false));
    }

    @Test
    public void testNameStartsWith() throws Exception {
        ByteCodeElement byteCodeElement = mock(ByteCodeElement.class);
        when(byteCodeElement.getSourceCodeName()).thenReturn(FOO);
        assertThat(ElementMatchers.nameStartsWith(FOO.substring(0, 2)).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.nameStartsWith(FOO.substring(0, 2).toUpperCase()).matches(byteCodeElement),
                is(false));
        assertThat(ElementMatchers.nameStartsWith(BAR).matches(byteCodeElement), is(false));
    }

    @Test
    public void testNameStartsWithIgnoreCase() throws Exception {
        ByteCodeElement byteCodeElement = mock(ByteCodeElement.class);
        when(byteCodeElement.getSourceCodeName()).thenReturn(FOO);
        assertThat(ElementMatchers.nameStartsWithIgnoreCase(FOO.substring(0, 2)).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.nameStartsWithIgnoreCase(FOO.substring(0, 2).toUpperCase()).matches(byteCodeElement),
                is(true));
        assertThat(ElementMatchers.nameStartsWithIgnoreCase(BAR).matches(byteCodeElement), is(false));
    }

    @Test
    public void testNameEndsWith() throws Exception {
        ByteCodeElement byteCodeElement = mock(ByteCodeElement.class);
        when(byteCodeElement.getSourceCodeName()).thenReturn(FOO);
        assertThat(ElementMatchers.nameEndsWith(FOO.substring(1)).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.nameEndsWith(FOO.substring(1).toUpperCase()).matches(byteCodeElement), is(false));
        assertThat(ElementMatchers.nameEndsWith(BAR).matches(byteCodeElement), is(false));
    }

    @Test
    public void testNameEndsWithIgnoreCase() throws Exception {
        ByteCodeElement byteCodeElement = mock(ByteCodeElement.class);
        when(byteCodeElement.getSourceCodeName()).thenReturn(FOO);
        assertThat(ElementMatchers.nameEndsWithIgnoreCase(FOO.substring(1)).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.nameEndsWithIgnoreCase(FOO.substring(1).toUpperCase()).matches(byteCodeElement),
                is(true));
        assertThat(ElementMatchers.nameEndsWithIgnoreCase(BAR).matches(byteCodeElement), is(false));
    }

    @Test
    public void testNameContains() throws Exception {
        ByteCodeElement byteCodeElement = mock(ByteCodeElement.class);
        when(byteCodeElement.getSourceCodeName()).thenReturn(FOO);
        assertThat(ElementMatchers.nameContains(FOO.substring(1, 2)).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.nameContains(FOO.substring(1, 2).toUpperCase()).matches(byteCodeElement), is(false));
        assertThat(ElementMatchers.nameContains(BAR).matches(byteCodeElement), is(false));
    }

    @Test
    public void testNameContainsIgnoreCase() throws Exception {
        ByteCodeElement byteCodeElement = mock(ByteCodeElement.class);
        when(byteCodeElement.getSourceCodeName()).thenReturn(FOO);
        assertThat(ElementMatchers.nameContainsIgnoreCase(FOO.substring(1, 2)).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.nameContainsIgnoreCase(FOO.substring(1, 2).toUpperCase()).matches(byteCodeElement),
                is(true));
        assertThat(ElementMatchers.nameContainsIgnoreCase(BAR).matches(byteCodeElement), is(false));
    }

    @Test
    public void testNameMatches() throws Exception {
        ByteCodeElement byteCodeElement = mock(ByteCodeElement.class);
        when(byteCodeElement.getSourceCodeName()).thenReturn(FOO);
        assertThat(ElementMatchers.nameMatches("^" + FOO + "$").matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.nameMatches(FOO.toUpperCase()).matches(byteCodeElement), is(false));
        assertThat(ElementMatchers.nameMatches(BAR).matches(byteCodeElement), is(false));
    }

    @Test
    public void testHasDescriptor() throws Exception {
        ByteCodeElement byteCodeElement = mock(ByteCodeElement.class);
        when(byteCodeElement.getDescriptor()).thenReturn(FOO);
        assertThat(ElementMatchers.hasDescriptor(FOO).matches(byteCodeElement), is(true));
        assertThat(ElementMatchers.hasDescriptor(FOO.toUpperCase()).matches(byteCodeElement), is(false));
        assertThat(ElementMatchers.hasDescriptor(BAR).matches(byteCodeElement), is(false));
    }

    @Test
    public void testIsDeclaredBy() throws Exception {
        assertThat(ElementMatchers.isDeclaredBy(IsDeclaredBy.class)
                .matches(new TypeDescription.ForLoadedType(IsDeclaredBy.Inner.class)), is(true));
        assertThat(ElementMatchers.isDeclaredBy(IsDeclaredBy.class).matches(mock(ByteCodeElement.class)), is(false));
        assertThat(ElementMatchers.isDeclaredBy(Object.class).matches(mock(ByteCodeElement.class)), is(false));
    }

    @Test
    public void testIsVisibleTo() throws Exception {
        assertThat(
                ElementMatchers.isVisibleTo(Object.class).matches(new TypeDescription.ForLoadedType(IsVisibleTo.class)),
                is(true));
        assertThat(ElementMatchers.isVisibleTo(Object.class)
                .matches(new TypeDescription.ForLoadedType(IsNotVisibleTo.class)), is(false));
    }

    @Test
    public void testIsAnnotatedWith() throws Exception {
        assertThat(ElementMatchers.isAnnotatedWith(IsAnnotatedWithAnnotation.class)
                .matches(new TypeDescription.ForLoadedType(IsAnnotatedWith.class)), is(true));
        assertThat(ElementMatchers.isAnnotatedWith(IsAnnotatedWithAnnotation.class)
                .matches(new TypeDescription.ForLoadedType(Object.class)), is(false));
    }

    @Test
    public void testIsPublic() throws Exception {
        ModifierReviewable modifierReviewable = mock(ModifierReviewable.class);
        when(modifierReviewable.getModifiers()).thenReturn(Opcodes.ACC_PUBLIC);
        assertThat(ElementMatchers.isPublic().matches(modifierReviewable), is(true));
        assertThat(ElementMatchers.isPublic().matches(mock(ModifierReviewable.class)), is(false));
    }

    @Test
    public void testIsProtected() throws Exception {
        ModifierReviewable modifierReviewable = mock(ModifierReviewable.class);
        when(modifierReviewable.getModifiers()).thenReturn(Opcodes.ACC_PROTECTED);
        assertThat(ElementMatchers.isProtected().matches(modifierReviewable), is(true));
        assertThat(ElementMatchers.isProtected().matches(mock(ModifierReviewable.class)), is(false));
    }

    @Test
    public void testIsPackagePrivate() throws Exception {
        ModifierReviewable modifierReviewable = mock(ModifierReviewable.class);
        when(modifierReviewable.getModifiers())
                .thenReturn(Opcodes.ACC_PUBLIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED);
        assertThat(ElementMatchers.isPackagePrivate().matches(mock(ModifierReviewable.class)), is(true));
        assertThat(ElementMatchers.isPackagePrivate().matches(modifierReviewable), is(false));
    }

    @Test
    public void testIsPrivate() throws Exception {
        ModifierReviewable modifierReviewable = mock(ModifierReviewable.class);
        when(modifierReviewable.getModifiers()).thenReturn(Opcodes.ACC_PRIVATE);
        assertThat(ElementMatchers.isPrivate().matches(modifierReviewable), is(true));
        assertThat(ElementMatchers.isPrivate().matches(mock(ModifierReviewable.class)), is(false));
    }

    @Test
    public void testIsFinal() throws Exception {
        ModifierReviewable modifierReviewable = mock(ModifierReviewable.class);
        when(modifierReviewable.getModifiers()).thenReturn(Opcodes.ACC_FINAL);
        assertThat(ElementMatchers.isFinal().matches(modifierReviewable), is(true));
        assertThat(ElementMatchers.isFinal().matches(mock(ModifierReviewable.class)), is(false));
    }

    @Test
    public void testIsStatic() throws Exception {
        ModifierReviewable modifierReviewable = mock(ModifierReviewable.class);
        when(modifierReviewable.getModifiers()).thenReturn(Opcodes.ACC_STATIC);
        assertThat(ElementMatchers.isStatic().matches(modifierReviewable), is(true));
        assertThat(ElementMatchers.isStatic().matches(mock(ModifierReviewable.class)), is(false));
    }

    @Test
    public void testIsSynthetic() throws Exception {
        ModifierReviewable modifierReviewable = mock(ModifierReviewable.class);
        when(modifierReviewable.getModifiers()).thenReturn(Opcodes.ACC_SYNTHETIC);
        assertThat(ElementMatchers.isSynthetic().matches(modifierReviewable), is(true));
        assertThat(ElementMatchers.isSynthetic().matches(mock(ModifierReviewable.class)), is(false));
    }

    @Test
    public void testIsSynchronized() throws Exception {
        MethodDescription methodDescription = mock(MethodDescription.class);
        when(methodDescription.getModifiers()).thenReturn(Opcodes.ACC_SYNCHRONIZED);
        assertThat(ElementMatchers.isSynchronized().matches(methodDescription), is(true));
        assertThat(ElementMatchers.isSynchronized().matches(mock(MethodDescription.class)), is(false));
    }

    @Test
    public void testIsNative() throws Exception {
        MethodDescription methodDescription = mock(MethodDescription.class);
        when(methodDescription.getModifiers()).thenReturn(Opcodes.ACC_NATIVE);
        assertThat(ElementMatchers.isNative().matches(methodDescription), is(true));
        assertThat(ElementMatchers.isNative().matches(mock(MethodDescription.class)), is(false));
    }

    @Test
    public void testIsStrict() throws Exception {
        MethodDescription methodDescription = mock(MethodDescription.class);
        when(methodDescription.getModifiers()).thenReturn(Opcodes.ACC_STRICT);
        assertThat(ElementMatchers.isStrict().matches(methodDescription), is(true));
        assertThat(ElementMatchers.isStrict().matches(mock(MethodDescription.class)), is(false));
    }

    @Test
    public void testIsVarArgs() throws Exception {
        MethodDescription modifierReviewable = mock(MethodDescription.class);
        when(modifierReviewable.getModifiers()).thenReturn(Opcodes.ACC_VARARGS);
        assertThat(ElementMatchers.isVarArgs().matches(modifierReviewable), is(true));
        assertThat(ElementMatchers.isVarArgs().matches(mock(MethodDescription.class)), is(false));
    }

    @Test
    public void testIsBridge() throws Exception {
        MethodDescription modifierReviewable = mock(MethodDescription.class);
        when(modifierReviewable.getModifiers()).thenReturn(Opcodes.ACC_BRIDGE);
        assertThat(ElementMatchers.isBridge().matches(modifierReviewable), is(true));
        assertThat(ElementMatchers.isBridge().matches(mock(MethodDescription.class)), is(false));
    }

    @Test
    public void testIsMethod() throws Exception {
        assertThat(ElementMatchers.is(IsEqual.class.getDeclaredMethod(FOO))
                .matches(new MethodDescription.ForLoadedMethod(IsEqual.class.getDeclaredMethod(FOO))), is(true));
        assertThat(ElementMatchers.is(IsEqual.class.getDeclaredMethod(FOO)).matches(mock(MethodDescription.class)),
                is(false));
        assertThat(ElementMatchers.is(IsEqual.class.getDeclaredConstructor())
                .matches(new MethodDescription.ForLoadedConstructor(IsEqual.class.getDeclaredConstructor())), is(true));
        assertThat(ElementMatchers.is(IsEqual.class.getDeclaredConstructor()).matches(mock(MethodDescription.class)),
                is(false));
    }

    @Test
    public void testReturns() throws Exception {
        assertThat(ElementMatchers.returns(void.class)
                .matches(new MethodDescription.ForLoadedMethod(Returns.class.getDeclaredMethod(FOO))), is(true));
        assertThat(ElementMatchers.returns(void.class)
                .matches(new MethodDescription.ForLoadedMethod(Returns.class.getDeclaredMethod(BAR))), is(false));
        assertThat(ElementMatchers.returns(String.class)
                .matches(new MethodDescription.ForLoadedMethod(Returns.class.getDeclaredMethod(BAR))), is(true));
        assertThat(ElementMatchers.returns(String.class)
                .matches(new MethodDescription.ForLoadedMethod(Returns.class.getDeclaredMethod(FOO))), is(false));
    }

    @Test
    public void testTakesArguments() throws Exception {
        assertThat(ElementMatchers.takesArguments(Void.class).matches(
                        new MethodDescription.ForLoadedMethod(TakesArguments.class.getDeclaredMethod(FOO, Void.class))),
                is(true));
        assertThat(ElementMatchers.takesArguments(Void.class, Object.class).matches(
                        new MethodDescription.ForLoadedMethod(TakesArguments.class.getDeclaredMethod(FOO, Void.class))),
                is(false));
        assertThat(ElementMatchers.takesArguments(String.class, int.class).matches(
                new MethodDescription.ForLoadedMethod(
                        TakesArguments.class.getDeclaredMethod(BAR, String.class, int.class))), is(true));
        assertThat(ElementMatchers.takesArguments(String.class, Integer.class).matches(
                new MethodDescription.ForLoadedMethod(
                        TakesArguments.class.getDeclaredMethod(BAR, String.class, int.class))), is(false));
    }

    @Test
    public void testTakesArgumentsLength() throws Exception {
        assertThat(ElementMatchers.takesArguments(1).matches(
                        new MethodDescription.ForLoadedMethod(TakesArguments.class.getDeclaredMethod(FOO, Void.class))),
                is(true));
        assertThat(ElementMatchers.takesArguments(2).matches(
                        new MethodDescription.ForLoadedMethod(TakesArguments.class.getDeclaredMethod(FOO, Void.class))),
                is(false));
        assertThat(ElementMatchers.takesArguments(2).matches(new MethodDescription.ForLoadedMethod(
                TakesArguments.class.getDeclaredMethod(BAR, String.class, int.class))), is(true));
        assertThat(ElementMatchers.takesArguments(3).matches(new MethodDescription.ForLoadedMethod(
                TakesArguments.class.getDeclaredMethod(BAR, String.class, int.class))), is(false));
    }

    @Test
    public void testCanThrow() throws Exception {
        assertThat(ElementMatchers.canThrow(IOException.class)
                .matches(new MethodDescription.ForLoadedMethod(CanThrow.class.getDeclaredMethod(FOO))), is(true));
        assertThat(ElementMatchers.canThrow(SQLException.class)
                .matches(new MethodDescription.ForLoadedMethod(CanThrow.class.getDeclaredMethod(FOO))), is(false));
        assertThat(ElementMatchers.canThrow(Error.class)
                .matches(new MethodDescription.ForLoadedMethod(CanThrow.class.getDeclaredMethod(FOO))), is(true));
        assertThat(ElementMatchers.canThrow(RuntimeException.class)
                .matches(new MethodDescription.ForLoadedMethod(CanThrow.class.getDeclaredMethod(FOO))), is(true));
        assertThat(ElementMatchers.canThrow(IOException.class)
                .matches(new MethodDescription.ForLoadedMethod(CanThrow.class.getDeclaredMethod(BAR))), is(false));
        assertThat(ElementMatchers.canThrow(SQLException.class)
                .matches(new MethodDescription.ForLoadedMethod(CanThrow.class.getDeclaredMethod(BAR))), is(false));
        assertThat(ElementMatchers.canThrow(Error.class)
                .matches(new MethodDescription.ForLoadedMethod(CanThrow.class.getDeclaredMethod(BAR))), is(true));
        assertThat(ElementMatchers.canThrow(RuntimeException.class)
                .matches(new MethodDescription.ForLoadedMethod(CanThrow.class.getDeclaredMethod(BAR))), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("unchecked")
    public void testCanThrowValidates() throws Exception {
        ElementMatchers.canThrow((Class) Object.class);
    }

    @Test
    public void testSortIsMethod() throws Exception {
        assertThat(ElementMatchers.isMethod()
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("toString"))), is(true));
        assertThat(ElementMatchers.isMethod()
                .matches(new MethodDescription.ForLoadedConstructor(Object.class.getDeclaredConstructor())), is(false));
        assertThat(ElementMatchers.isMethod()
                .matches(MethodDescription.Latent.typeInitializerOf(mock(TypeDescription.class))), is(false));
    }

    @Test
    public void testSortIsConstructor() throws Exception {
        assertThat(ElementMatchers.isConstructor()
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("toString"))), is(false));
        assertThat(ElementMatchers.isConstructor()
                .matches(new MethodDescription.ForLoadedConstructor(Object.class.getDeclaredConstructor())), is(true));
        assertThat(ElementMatchers.isConstructor()
                .matches(MethodDescription.Latent.typeInitializerOf(mock(TypeDescription.class))), is(false));
    }

    @Test
    @JavaVersionRule.Enforce
    public void testIsDefaultMethod() throws Exception {
        assertThat(ElementMatchers.isDefaultMethod().matches(new MethodDescription.ForLoadedMethod(
                classLoader.loadClass(SINGLE_DEFAULT_METHOD).getDeclaredMethod(FOO))), is(true));
        assertThat(ElementMatchers.isDefaultMethod()
                .matches(new MethodDescription.ForLoadedMethod(Runnable.class.getDeclaredMethod("run"))), is(false));
    }

    @Test
    public void testSortIsTypeInitializer() throws Exception {
        assertThat(ElementMatchers.isTypeInitializer()
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("toString"))), is(false));
        assertThat(ElementMatchers.isTypeInitializer()
                .matches(new MethodDescription.ForLoadedConstructor(Object.class.getDeclaredConstructor())), is(false));
        assertThat(ElementMatchers.isTypeInitializer()
                .matches(MethodDescription.Latent.typeInitializerOf(mock(TypeDescription.class))), is(true));
    }

    @Test
    public void testSortIsVisibilityBridge() throws Exception {
        assertThat(ElementMatchers.isVisibilityBridge()
                        .matches(
                                new MethodDescription.ForLoadedMethod(IsVisibilityBridge.class.getDeclaredMethod(FOO))),
                is(true));
        assertThat(ElementMatchers.isVisibilityBridge()
                        .matches(new MethodDescription.ForLoadedMethod(
                                IsBridge.class.getDeclaredMethod(FOO, Object.class))),
                is(false));
        assertThat(ElementMatchers.isVisibilityBridge()
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("toString"))), is(false));
    }

    @Test
    public void testSortIsBridge() throws Exception {
        assertThat(ElementMatchers.isBridge()
                        .matches(
                                new MethodDescription.ForLoadedMethod(IsVisibilityBridge.class.getDeclaredMethod(FOO))),
                is(true));
        assertThat(ElementMatchers.isBridge()
                        .matches(new MethodDescription.ForLoadedMethod(
                                IsBridge.class.getDeclaredMethod(FOO, Object.class))),
                is(true));
        assertThat(ElementMatchers.isBridge()
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("toString"))), is(false));
    }

    @Test
    public void testIsOverridable() throws Exception {
        assertThat(ElementMatchers.isOverridable()
                        .matches(new MethodDescription.ForLoadedMethod(IsOverridable.class.getDeclaredMethod("baz"))),
                is(true));
        assertThat(ElementMatchers.isOverridable()
                        .matches(new MethodDescription.ForLoadedMethod(IsOverridable.class.getDeclaredMethod("foo"))),
                is(false));
        assertThat(ElementMatchers.isOverridable()
                        .matches(new MethodDescription.ForLoadedMethod(IsOverridable.class.getDeclaredMethod("bar"))),
                is(false));
        assertThat(ElementMatchers.isOverridable()
                        .matches(new MethodDescription.ForLoadedMethod(IsOverridable.class.getDeclaredMethod("qux"))),
                is(false));
        assertThat(ElementMatchers.isOverridable()
                        .matches(new MethodDescription.ForLoadedConstructor(
                                IsOverridable.class.getDeclaredConstructor())),
                is(false));
    }

    @Test
    public void testIsDefaultFinalizer() throws Exception {
        assertThat(ElementMatchers.isDefaultFinalizer()
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("finalize"))), is(true));
        assertThat(ElementMatchers.isDefaultFinalizer()
                        .matches(new MethodDescription.ForLoadedMethod(
                                ObjectMethods.class.getDeclaredMethod("finalize"))),
                is(false));
        assertThat(ElementMatchers.isDefaultFinalizer()
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("toString"))), is(false));
    }

    @Test
    public void testIsFinalizer() throws Exception {
        assertThat(ElementMatchers.isFinalizer()
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("finalize"))), is(true));
        assertThat(ElementMatchers.isFinalizer()
                        .matches(new MethodDescription.ForLoadedMethod(
                                ObjectMethods.class.getDeclaredMethod("finalize"))),
                is(true));
        assertThat(ElementMatchers.isFinalizer()
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("toString"))), is(false));
    }

    @Test
    public void testIsHashCode() throws Exception {
        assertThat(ElementMatchers.isHashCode()
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("hashCode"))), is(true));
        assertThat(ElementMatchers.isHashCode()
                        .matches(new MethodDescription.ForLoadedMethod(
                                ObjectMethods.class.getDeclaredMethod("hashCode"))),
                is(true));
        assertThat(ElementMatchers.isHashCode()
                .matches(new MethodDescription.ForLoadedMethod(Runnable.class.getDeclaredMethod("run"))), is(false));
    }

    @Test
    public void testIsEquals() throws Exception {
        assertThat(ElementMatchers.isEquals()
                        .matches(new MethodDescription.ForLoadedMethod(
                                Object.class.getDeclaredMethod("equals", Object.class))),
                is(true));
        assertThat(ElementMatchers.isEquals().matches(
                        new MethodDescription.ForLoadedMethod(
                                ObjectMethods.class.getDeclaredMethod("equals", Object.class))),
                is(true));
        assertThat(ElementMatchers.isEquals()
                .matches(new MethodDescription.ForLoadedMethod(Runnable.class.getDeclaredMethod("run"))), is(false));
    }

    @Test
    public void testIsClone() throws Exception {
        assertThat(ElementMatchers.isClone()
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("clone"))), is(true));
        assertThat(ElementMatchers.isClone()
                        .matches(new MethodDescription.ForLoadedMethod(ObjectMethods.class.getDeclaredMethod("clone"))),
                is(true));
        assertThat(ElementMatchers.isClone()
                .matches(new MethodDescription.ForLoadedMethod(Runnable.class.getDeclaredMethod("run"))), is(false));
    }

    @Test
    public void testIsToString() throws Exception {
        assertThat(ElementMatchers.isToString()
                .matches(new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("toString"))), is(true));
        assertThat(ElementMatchers.isToString()
                        .matches(new MethodDescription.ForLoadedMethod(
                                ObjectMethods.class.getDeclaredMethod("toString"))),
                is(true));
        assertThat(ElementMatchers.isToString()
                .matches(new MethodDescription.ForLoadedMethod(Runnable.class.getDeclaredMethod("run"))), is(false));
    }

    @Test
    public void testIsDefaultConstructor() throws Exception {
        assertThat(ElementMatchers.isDefaultConstructor()
                .matches(new MethodDescription.ForLoadedConstructor(Object.class.getDeclaredConstructor())), is(true));
        assertThat(ElementMatchers.isDefaultConstructor()
                        .matches(new MethodDescription.ForLoadedConstructor(
                                String.class.getDeclaredConstructor(String.class))),
                is(false));
        assertThat(ElementMatchers.isDefaultConstructor()
                .matches(new MethodDescription.ForLoadedMethod(Runnable.class.getDeclaredMethod("run"))), is(false));
    }

    @Test
    public void testIsGetter() throws Exception {
        assertThat(ElementMatchers.isGetter()
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("getFoo"))), is(false));
        assertThat(ElementMatchers.isGetter()
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("isQux"))), is(true));
        assertThat(ElementMatchers.isGetter()
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("getQux"))), is(true));
        assertThat(ElementMatchers.isGetter()
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("isBar"))), is(true));
        assertThat(ElementMatchers.isGetter()
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("getBar"))), is(true));
        assertThat(ElementMatchers.isGetter()
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("isBaz"))), is(false));
        assertThat(ElementMatchers.isGetter()
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("getBaz"))), is(true));
        assertThat(ElementMatchers.isGetter()
                        .matches(new MethodDescription.ForLoadedMethod(
                                Getters.class.getDeclaredMethod("getBaz", Void.class))),
                is(false));
        assertThat(ElementMatchers.isGetter(String.class)
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("getBaz"))), is(true));
        assertThat(ElementMatchers.isGetter(Void.class)
                .matches(new MethodDescription.ForLoadedMethod(Getters.class.getDeclaredMethod("getBaz"))), is(false));
    }

    @Test
    public void testIsSetter() throws Exception {
        assertThat(ElementMatchers.isSetter()
                .matches(new MethodDescription.ForLoadedMethod(Setters.class.getDeclaredMethod("setFoo"))), is(false));
        assertThat(ElementMatchers.isSetter().matches(
                        new MethodDescription.ForLoadedMethod(
                                Setters.class.getDeclaredMethod("setBar", boolean.class))),
                is(true));
        assertThat(ElementMatchers.isSetter().matches(
                        new MethodDescription.ForLoadedMethod(
                                Setters.class.getDeclaredMethod("setQux", Boolean.class))),
                is(true));
        assertThat(ElementMatchers.isSetter().matches(
                        new MethodDescription.ForLoadedMethod(Setters.class.getDeclaredMethod("setBaz", String.class))),
                is(true));
        assertThat(ElementMatchers.isSetter().matches(new MethodDescription.ForLoadedMethod(
                Setters.class.getDeclaredMethod("setBaz", String.class, Void.class))), is(false));
        assertThat(ElementMatchers.isSetter(String.class).matches(
                        new MethodDescription.ForLoadedMethod(Setters.class.getDeclaredMethod("setBaz", String.class))),
                is(true));
        assertThat(ElementMatchers.isSetter(Void.class).matches(
                        new MethodDescription.ForLoadedMethod(Setters.class.getDeclaredMethod("setBaz", String.class))),
                is(false));
    }

    @Test
    public void testisSpecializationOf() throws Exception {
        MethodDescription methodDescription = new MethodDescription.ForLoadedMethod(
                IsSpecialization.class.getDeclaredMethod(FOO, Number.class));
        assertThat(ElementMatchers.isSpecializationOf(methodDescription).matches(
                        new MethodDescription.ForLoadedMethod(
                                IsSpecialization.class.getDeclaredMethod(FOO, Number.class))),
                is(true));
        assertThat(ElementMatchers.isSpecializationOf(methodDescription).matches(
                        new MethodDescription.ForLoadedMethod(
                                IsSpecialization.class.getDeclaredMethod(FOO, Integer.class))),
                is(true));
        assertThat(ElementMatchers.isSpecializationOf(methodDescription).matches(
                        new MethodDescription.ForLoadedMethod(
                                IsSpecialization.class.getDeclaredMethod(FOO, String.class))),
                is(false));
        assertThat(ElementMatchers.isSpecializationOf(methodDescription).matches(
                        new MethodDescription.ForLoadedMethod(
                                IsSpecialization.class.getDeclaredMethod(BAR, Integer.class))),
                is(false));
    }

    @Test
    public void testIsSubOrSuperType() throws Exception {
        assertThat(ElementMatchers.isSubTypeOf(String.class).matches(new TypeDescription.ForLoadedType(Object.class)),
                is(false));
        assertThat(ElementMatchers.isSubTypeOf(Object.class).matches(new TypeDescription.ForLoadedType(String.class)),
                is(true));
        assertThat(ElementMatchers.isSubTypeOf(Serializable.class)
                .matches(new TypeDescription.ForLoadedType(String.class)), is(true));
        assertThat(ElementMatchers.isSuperTypeOf(Object.class).matches(new TypeDescription.ForLoadedType(String.class)),
                is(false));
        assertThat(ElementMatchers.isSuperTypeOf(String.class).matches(new TypeDescription.ForLoadedType(Object.class)),
                is(true));
        assertThat(ElementMatchers.isSuperTypeOf(String.class)
                .matches(new TypeDescription.ForLoadedType(Serializable.class)), is(true));
    }

    @Test
    public void testIsAnnotatedInheritedWith() throws Exception {
        assertThat(ElementMatchers.inheritsAnnotation(OtherAnnotation.class)
                .matches(new TypeDescription.ForLoadedType(OtherInherited.class)), is(true));
        assertThat(ElementMatchers.isAnnotatedWith(OtherAnnotation.class)
                .matches(new TypeDescription.ForLoadedType(OtherInherited.class)), is(false));
    }

    @Test
    public void testDeclaresField() throws Exception {
        assertThat(ElementMatchers.declaresField(ElementMatchers.isAnnotatedWith(OtherAnnotation.class))
                .matches(new TypeDescription.ForLoadedType(DeclaresFieldOrMethod.class)), is(true));
        assertThat(ElementMatchers.declaresField(ElementMatchers.isAnnotatedWith(OtherAnnotation.class))
                .matches(new TypeDescription.ForLoadedType(Object.class)), is(false));
        assertThat(ElementMatchers.declaresMethod(ElementMatchers.isAnnotatedWith(OtherAnnotation.class))
                .matches(new TypeDescription.ForLoadedType(DeclaresFieldOrMethod.class)), is(true));
        assertThat(ElementMatchers.declaresMethod(ElementMatchers.isAnnotatedWith(OtherAnnotation.class))
                .matches(new TypeDescription.ForLoadedType(Object.class)), is(false));
    }

    @Test
    public void testIsBootstrapClassLoader() throws Exception {
        assertThat(ElementMatchers.isBootstrapClassLoader().matches(null), is(true));
        assertThat(ElementMatchers.isBootstrapClassLoader().matches(mock(ClassLoader.class)), is(false));
    }

    @Test
    public void testIsSystemClassLoader() throws Exception {
        assertThat(ElementMatchers.isSystemClassLoader().matches(ClassLoader.getSystemClassLoader()), is(true));
        assertThat(ElementMatchers.isSystemClassLoader().matches(null), is(false));
        assertThat(ElementMatchers.isSystemClassLoader().matches(ClassLoader.getSystemClassLoader().getParent()), is(false));
        assertThat(ElementMatchers.isSystemClassLoader().matches(mock(ClassLoader.class)), is(false));
    }

    @Test
    public void testIsExtensionClassLoader() throws Exception {
        assertThat(ElementMatchers.isExtensionClassLoader().matches(ClassLoader.getSystemClassLoader().getParent()), is(true));
        assertThat(ElementMatchers.isExtensionClassLoader().matches(ClassLoader.getSystemClassLoader()), is(false));
        assertThat(ElementMatchers.isExtensionClassLoader().matches(null), is(false));
        assertThat(ElementMatchers.isExtensionClassLoader().matches(mock(ClassLoader.class)), is(false));
    }

    @Test
    public void testIsChildOf() throws Exception {
        ClassLoader parent = new URLClassLoader(new URL[0], null);
        assertThat(ElementMatchers.isChildOf(parent).matches(new URLClassLoader(new URL[0], parent)), is(true));
        assertThat(ElementMatchers.isChildOf(parent).matches(new URLClassLoader(new URL[0], null)), is(false));
        assertThat(ElementMatchers.isChildOf(parent).matches(null), is(false));
        assertThat(ElementMatchers.isChildOf(null).matches(mock(ClassLoader.class)), is(true));
    }

    @Test
    public void testIsParentOf() throws Exception {
        ClassLoader parent = new URLClassLoader(new URL[0], null);
        assertThat(ElementMatchers.isParentOf(new URLClassLoader(new URL[0], parent)).matches(parent), is(true));
        assertThat(ElementMatchers.isParentOf(new URLClassLoader(new URL[0], null)).matches(parent), is(false));
        assertThat(ElementMatchers.isParentOf(null).matches(new URLClassLoader(new URL[0], null)), is(false));
        assertThat(ElementMatchers.isParentOf(null).matches(null), is(true));
        assertThat(ElementMatchers.isParentOf(mock(ClassLoader.class)).matches(null), is(true));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testConstructorIsHidden() throws Exception {
        assertThat(Modifier.isPrivate(ElementMatchers.class.getDeclaredConstructor().getModifiers()), is(true));
        Constructor<?> constructor = ElementMatchers.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            fail();
        } catch (InvocationTargetException e) {
            throw (UnsupportedOperationException) e.getCause();
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    private static @interface IsAnnotatedWithAnnotation {

    }

    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface OtherAnnotation {
    }

    private static class IsDeclaredBy {

        static class Inner {
            /* empty */
        }
    }

    public static class IsVisibleTo {
        /* empty */
    }

    private static class IsNotVisibleTo {
        /* empty */
    }

    @IsAnnotatedWithAnnotation
    private static class IsAnnotatedWith {

    }

    private static abstract class IsEqual {

        abstract void foo();
    }

    private static abstract class Returns {

        abstract void foo();

        abstract String bar();
    }

    private static abstract class TakesArguments {

        abstract void foo(Void a);

        abstract void bar(String a, int b);
    }

    private static abstract class CanThrow {

        protected abstract void foo() throws IOException;

        protected abstract void bar();
    }

    static class VisibilityBridgeBase {

        public void foo() {
            /* empty */
        }
    }

    public abstract static class IsVisibilityBridge extends VisibilityBridgeBase {
        /* empty */
    }

    public static class BridgeBase<T> {

        public void foo(T arg) {
            /* empty */
        }
    }

    public static class IsBridge extends BridgeBase<Void> {

        @Override
        public void foo(Void arg) {
            /* empty */
        }
    }

    private static class ObjectMethods {

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        @Override
        public String toString() {
            return super.toString();
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
        }
    }

    public static class IsOverridable {

        public static void bar() {
            /* empty */
        }

        private void foo() {
            /* empty */
        }

        public final void qux() {
            /* empty */
        }

        public void baz() {
            /* empty */
        }
    }

    public static class Getters {

        public void getFoo() {
            /* empty */
        }

        public Boolean isBar() {
            return null;
        }

        public boolean isQux() {
            return false;
        }

        public Boolean getBar() {
            return null;
        }

        public boolean getQux() {
            return false;
        }

        public String isBaz() {
            return null;
        }

        public String getBaz() {
            return null;
        }

        public String getBaz(Void argument) {
            return null;
        }
    }

    public static class Setters {

        public void setFoo() {
            /* empty */
        }

        public void setBar(boolean argument) {
            /* empty */
        }

        public void setQux(Boolean argument) {
            /* empty */
        }

        public void setBaz(String argument) {
            /* empty */
        }

        public void setBaz(String argument, Void argument2) {
            /* empty */
        }
    }

    @OtherAnnotation
    public static class Other {
    }

    public static class OtherInherited extends Other {
    }

    public static class IsSpecialization {

        public Number foo(Number argument) {
            return null;
        }

        public Number foo(Integer argument) {
            return null;
        }

        public Number foo(String argument) {
            return null;
        }

        public Integer bar(Integer argument) {
            return null;
        }
    }

    public static class DeclaresFieldOrMethod {

        @OtherAnnotation
        Void field;

        @OtherAnnotation
        void method() {

        }
    }
}
