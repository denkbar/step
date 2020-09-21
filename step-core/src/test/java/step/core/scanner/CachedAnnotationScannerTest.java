package step.core.scanner;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import org.junit.Test;

import step.core.scanner.AnnotationScannerTest.TestClass;

public class CachedAnnotationScannerTest {

	@Test
	public void test() {
		Class<?> class1 = CachedAnnotationScanner.getClassesWithAnnotation(TestAnnotation.class).stream().findFirst().get();
		assertEquals(TestClass.class, class1);
	}

	@Test
	public void test2() {
		Class<?> class1 = CachedAnnotationScanner
				.getClassesWithAnnotation(TestAnnotation.class, this.getClass().getClassLoader()).stream().findFirst()
				.get();
		assertEquals(TestClass.class, class1);
	}

	@Test
	public void test3() {
		Class<?> class1 = CachedAnnotationScanner
				.getClassesWithAnnotation("step", TestAnnotation.class, this.getClass().getClassLoader()).stream()
				.findFirst().get();
		assertEquals(TestClass.class, class1);
	}

	@Test
	public void testMethod1() {
		Method method1 = CachedAnnotationScanner.getMethodsWithAnnotation(TestAnnotation.class).stream().findFirst().get();
		assertEquals("testMethod", method1.getName());
	}

	@Test
	public void testMethod2() {
		Method method1 = CachedAnnotationScanner
				.getMethodsWithAnnotation(TestAnnotation.class, this.getClass().getClassLoader()).stream().findFirst()
				.get();
		assertEquals("testMethod", method1.getName());
	}

	@Test
	public void testMethod3() {
		Method method1 = CachedAnnotationScanner
				.getMethodsWithAnnotation("step", TestAnnotation.class, this.getClass().getClassLoader()).stream()
				.findFirst().get();
		assertEquals("testMethod", method1.getName());
	}
	
	@Test
	public void testClearCache() {
		Method method1 = CachedAnnotationScanner.getMethodsWithAnnotation(TestAnnotation.class).stream().findFirst().get();
		assertEquals("testMethod", method1.getName());
		CachedAnnotationScanner.clearCache();
		method1 = CachedAnnotationScanner.getMethodsWithAnnotation(TestAnnotation.class).stream().findFirst().get();
		assertEquals("testMethod", method1.getName());
	}
}
