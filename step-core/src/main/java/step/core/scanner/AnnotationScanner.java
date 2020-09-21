package step.core.scanner;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

public class AnnotationScanner implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(AnnotationScanner.class);

	private final ScanResult scanResult;
	private final ClassLoader classLoader;

	private AnnotationScanner(ScanResult scanResult, ClassLoader classLoader) {
		this.scanResult = scanResult;
		this.classLoader = classLoader;
	}

	/**
	 * @return an instance of {@link AnnotationScanner} scanning all classes of the
	 *         context class loader
	 */
	public static AnnotationScanner forAllClassesFromContextClassLoader() {
		return forAllClassesFromClassLoader(null, Thread.currentThread().getContextClassLoader());
	}

	/**
	 * @param classloader the {@link ClassLoader} (including parents) to be scanned
	 * @return an instance of {@link AnnotationScanner} scanning all classes of the
	 *         provided class loader
	 */
	public static AnnotationScanner forAllClassesFromClassLoader(ClassLoader classloader) {
		return forAllClassesFromClassLoader(null, classloader);
	}

	/**
	 * @param packagePrefix the specific package to be scanned
	 * @param classloader   the {@link ClassLoader} (including parents) to be
	 *                      scanned
	 * @return an instance of {@link AnnotationScanner} scanning all classes of the
	 *         provided class loader
	 */
	public static AnnotationScanner forAllClassesFromClassLoader(String packagePrefix, ClassLoader classloader) {
		ClassGraph classGraph = new ClassGraph();
		if (packagePrefix != null) {
			classGraph.whitelistPackages(packagePrefix);
		}
		classGraph.overrideClassLoaders(classloader);
		classGraph.enableClassInfo().enableAnnotationInfo().enableMethodInfo();

		return scan(classGraph, classloader);
	}

	/**
	 * @param jar the specific jar file to be scanned
	 * @return an instance of {@link AnnotationScanner} scanning all classes of the
	 *         provided jar file
	 */
	public static AnnotationScanner forSpecificJar(File jar) {
		URLClassLoader urlClassLoader;
		try {
			urlClassLoader = new URLClassLoader(new URL[] { jar.toURI().toURL() });
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		return forSpecificJarFromURLClassLoader(urlClassLoader);
	}

	/**
	 * Scans the jar files of a specific {@link URLClassLoader}
	 * 
	 * @param classloader the specific {@link ClassLoader} to scan the {@link URL}s
	 *                    of
	 * @return an instance of {@link AnnotationScanner} scanning all classes of the
	 *         provided {@link URLClassLoader} (parent excluded)
	 */
	public static AnnotationScanner forSpecificJarFromURLClassLoader(URLClassLoader classloader) {
		List<String> jars = Arrays.asList(classloader.getURLs()).stream().map(url -> url.getPath())
				.collect(Collectors.toList());

		ClassGraph classGraph = new ClassGraph().overrideClasspath(jars).enableClassInfo().enableAnnotationInfo()
				.enableMethodInfo();

		return scan(classGraph, classloader);
	}

	private static AnnotationScanner scan(ClassGraph classGraph, ClassLoader classLoaderForResultClassesAndMethods) {
		long t1 = System.currentTimeMillis();
		logger.info("Scanning classpath...");
		ScanResult scanResult = classGraph.scan();
		logger.info("Scanned classpath in " + (System.currentTimeMillis() - t1) + "ms");
		AnnotationScanner annotationScanner = new AnnotationScanner(scanResult, classLoaderForResultClassesAndMethods);
		return annotationScanner;
	}

	/**
	 * Get all classes annotated by the provided {@link Annotation}
	 * 
	 * @param annotationClass
	 * @return the {@link Set} of classes annotated by the provided
	 *         {@link Annotation}
	 */
	public Set<Class<?>> getClassesWithAnnotation(Class<? extends Annotation> annotationClass) {
		ClassInfoList classInfos = scanResult.getClassesWithAnnotation(annotationClass.getName());
		return loadClassesFromClassInfoList(classLoader, classInfos);
	}

	/**
	 * Get all methods annotated by the provided {@link Annotation}
	 * 
	 * @param annotationClass
	 * @return the {@link Set} of methods annotated by the provided
	 *         {@link Annotation}
	 */
	public Set<Method> getMethodsWithAnnotation(Class<? extends Annotation> annotationClass) {
		Set<Method> result = new HashSet<>();
		ClassInfoList classInfos = scanResult.getClassesWithMethodAnnotation(annotationClass.getName());
		Set<Class<?>> classesFromClassInfoList = loadClassesFromClassInfoList(classLoader, classInfos);
		classesFromClassInfoList.forEach(c -> {
			Method[] methods = c.getMethods();
			for (Method method : methods) {
				if (isAnnotationPresent(annotationClass, method)) {
					result.add(method);
				}
			}
		});
		return result;
	}

	/**
	 * Alternative implementation of {@link Class#isAnnotationPresent(Class)} which
	 * doesn't rely on class equality but class names. The class loaders of the
	 * annotationClass and the method provided as argument might be different
	 * 
	 * @param annotationClass
	 * @param method
	 * @return
	 */
	private static boolean isAnnotationPresent(Class<? extends Annotation> annotationClass, Method method) {
		return Arrays.asList(method.getAnnotations()).stream()
				.filter(an -> an.annotationType().getName().equals(annotationClass.getName())).findAny().isPresent();
	}

	private static Set<Class<?>> loadClassesFromClassInfoList(ClassLoader classloader, ClassInfoList classInfos) {
		return classInfos.getNames().stream().map(Classes.loadWith(classloader)).collect(Collectors.toSet());
	}

	@Override
	public void close() {
		scanResult.close();
	}
}
