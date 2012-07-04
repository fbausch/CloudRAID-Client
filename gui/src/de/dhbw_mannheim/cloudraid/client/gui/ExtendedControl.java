package de.dhbw_mannheim.cloudraid.client.gui;

/*
 * @author Simone Bordet
 * @copyright The source code of the following class (ExtendedControl) was
 * published at http://bordet.blogspot.com/2007/
 * 01/utf-8-handling-for-resourcebundle-and.html. The source code was
 * published under the Creative Commons Attribution-NoDerivs 2.5
 * License. For more information please refer to:
 * http://creativecommons.org/licenses/by-nd/2.5/.
 */

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

/**
 * JDK 6's <a href=
 * "http://download.oracle.com/javase/6/docs/api/java/util/ResourceBundle.Control.html"
 * > ResourceBundle.Control</a> subclass that allows loading of bundles in XML
 * format. The bundles are searched first as Java classes, then as properties
 * files (these two methods are the standard search mechanism of
 * ResourceBundle), then as XML properties files. The filename extension of the
 * XML properties files is assumed to be *.properties.xml
 */
public class ExtendedControl extends Control {
	/**
	 * ResourceBundle that loads definitions from an XML properties file.
	 */
	public static class PropertyXMLResourceBundle extends ResourceBundle {
		private final Properties properties = new Properties();

		@Override
		public Enumeration<String> getKeys() {
			final Enumeration<Object> keys = this.properties.keys();
			return new Enumeration<String>() {
				@Override
				public boolean hasMoreElements() {
					return keys.hasMoreElements();
				}

				@Override
				public String nextElement() {
					return (String) keys.nextElement();
				}
			};
		}

		@Override
		protected Object handleGetObject(String key) {
			return this.properties.getProperty(key);
		}

		public void load(InputStream stream) throws IOException {
			this.properties.loadFromXML(stream);
		}
	}

	private static final String FORMAT_XML_SUFFIX = "properties.xml";
	private static final String FORMAT_XML = "java."
			+ ExtendedControl.FORMAT_XML_SUFFIX;
	private static final List<String> FORMATS;

	static {
		List<String> formats = new ArrayList<String>(Control.FORMAT_DEFAULT);
		formats.add(ExtendedControl.FORMAT_XML);
		FORMATS = Collections.unmodifiableList(formats);
	}

	@Override
	public List<String> getFormats(String baseName) {
		return ExtendedControl.FORMATS;
	}

	private InputStream getResourceInputStream(final URL resourceURL,
			boolean reload) throws IOException {
		if (!reload) {
			return resourceURL.openStream();
		}

		try {
			// This permission has already been checked by
			// ClassLoader.getResource(String), which will return null
			// in case the code has not enough privileges.
			return AccessController
					.doPrivileged(new PrivilegedExceptionAction<InputStream>() {
						@Override
						public InputStream run() throws IOException {
							URLConnection connection = resourceURL
									.openConnection();
							connection.setUseCaches(false);
							return connection.getInputStream();
						}
					});
		} catch (PrivilegedActionException x) {
			throw (IOException) x.getCause();
		}
	}

	@Override
	public ResourceBundle newBundle(String baseName, Locale locale,
			String format, ClassLoader loader, boolean reload)
			throws IllegalAccessException, InstantiationException, IOException {
		if (!ExtendedControl.FORMAT_XML.equals(format)) {
			return super.newBundle(baseName, locale, format, loader, reload);
		}

		String bundleName = toBundleName(baseName, locale);
		String resourceName = toResourceName(bundleName,
				ExtendedControl.FORMAT_XML_SUFFIX);
		final URL resourceURL = loader.getResource(resourceName);
		if (resourceURL == null) {
			return null;
		}

		InputStream stream = getResourceInputStream(resourceURL, reload);

		try {
			PropertyXMLResourceBundle result = new PropertyXMLResourceBundle();
			result.load(stream);
			return result;
		} finally {
			stream.close();
		}
	}
}