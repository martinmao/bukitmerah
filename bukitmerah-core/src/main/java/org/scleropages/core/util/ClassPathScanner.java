package org.scleropages.core.util; /**
 * 
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ClassUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * Utilities class used for scan classes from class path.
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class ClassPathScanner {

	public interface ScanListener {
		void onMatch(final MetadataReader metadataReader);
	}

	private static final ResourcePatternResolver RESOURCE_PATTERN_RESOLVER = new PathMatchingResourcePatternResolver();

	private static final MetadataReaderFactory METADATA_READER_FACTORY = new CachingMetadataReaderFactory(
			RESOURCE_PATTERN_RESOLVER);
	private static final PathMatcher PATH_MATCHER = new AntPathMatcher();

	private static final String DEFAULT_RESOURCE_PATTERN = "**/*.class";

	/**
	 * scan classes from base package
	 * 
	 * @param basePackage
	 *            scan from
	 * @param scanListener
	 *            scan result listener
	 * @param urlPattern
	 *            ant path file name path matcher.
	 * @throws IOException
	 */
	public static synchronized void scanClasses(String basePackage, ScanListener scanListener, String urlPattern)
			throws IOException {
		String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + resolveBasePackage(basePackage)
				+ "/" + DEFAULT_RESOURCE_PATTERN;
		Resource[] resources = RESOURCE_PATTERN_RESOLVER.getResources(packageSearchPath);
		for (Resource resource : resources) {
			if (resource.isReadable()) {
				try {
					MetadataReader metadataReader = METADATA_READER_FACTORY.getMetadataReader(resource);
					if (StringUtils.hasText(urlPattern)
							&& PATH_MATCHER.match(urlPattern, metadataReader.getResource().getURL().toString())) {
						scanListener.onMatch(metadataReader);
					} else
						scanListener.onMatch(metadataReader);
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
			}
		}
	}

	/**
	 * scan classes from base package
	 * 
	 * @param basePackage
	 *            scan from
	 * @param scanListener
	 *            scan result listener
	 * @param annotationOrSuperClass
	 *            can be annotation or interface or super class
	 * @param assignable
	 *            if true it will use class instance to check can be assignable
	 *            (<b>this will used class loader to load class from class file
	 *            first.)
	 * @throws IOException
	 */
	public static void scanClasses(String basePackage, final ScanListener scanListener,
			final Class<?> annotationOrSuperClass, final boolean assignable) throws IOException {
		final boolean isAnnotation = annotationOrSuperClass.isAnnotation();
		final boolean isInterface = annotationOrSuperClass.isInterface();
		scanClasses(basePackage, new ScanListener() {
			@Override
			public void onMatch(MetadataReader metadataReader) {
				if (isAnnotation
						&& metadataReader.getAnnotationMetadata().hasAnnotation(annotationOrSuperClass.getName())) {
					scanListener.onMatch(metadataReader);
				} else if (isInterface) {
					String[] interfaces = metadataReader.getClassMetadata().getInterfaceNames();
					for (String interface_ : interfaces) {
						if (annotationOrSuperClass.getName().equals(interface_))
							scanListener.onMatch(metadataReader);
					}
				} else if (annotationOrSuperClass.getName()
						.equals(metadataReader.getClassMetadata().getSuperClassName())) {
					scanListener.onMatch(metadataReader);
				} else if (assignable) {
					try {
						Class<?> clazz = ClassUtils.forName(metadataReader.getClassMetadata().getClassName(),
								this.getClass().getClassLoader());
						if (ClassUtils.isAssignable(annotationOrSuperClass, clazz)) {
							scanListener.onMatch(metadataReader);
						}
					} catch (ClassNotFoundException | LinkageError e) {

					}
				}
			}
		}, "");
	}

	private static String resolveBasePackage(String basePackage) {
		return ClassUtils.convertClassNameToResourcePath(basePackage);
	}
}
