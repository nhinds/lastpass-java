package com.nhinds.lastpass.impl.dto.reader;

import java.lang.reflect.Field;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.common.base.CaseFormat;
import com.nhinds.lastpass.LastPassException;

/**
 * Simple DOM parser which reads a {@link Document} into a dto annotated with a
 * limited subset of the JAXB API, without using a JAXB implementation (which
 * generally do not work on android)
 * <p>
 * Only field-annotated DTOs are supported. Non-default namespaces are not
 * supported.
 * <p>
 * Supported annotations:
 * <ul>
 * <li>{@link XmlRootElement}
 * <li>{@link XmlType}
 * <li>{@link XmlAttribute}
 * <li>{@link XmlElement}
 * </ul>
 * <p>
 * Supported field types:
 * <ul>
 * <li>Anything annotated with {@link XmlType}
 * <li>String
 * <li>int/Integer
 * <li>boolean/Boolean
 * </ul>
 */
public class DtoReader {
	/** Marker value which means the parameter was not specified */
	private static final String DEFAULT_ANNOTATION_VALUE = "##default";

	/**
	 * Read the Document into a DTO of the given type
	 * 
	 * @param document
	 *            The document to read
	 * @param clazz
	 *            The type of DTO to read the document into
	 * @return The populated DTO, or null if the document is not of the given
	 *         type
	 */
	public <T> T read(Document document, Class<T> clazz) {
		XmlRootElement rootElement = clazz.getAnnotation(XmlRootElement.class);
		if (rootElement == null)
			throw new IllegalArgumentException(clazz + " is not a valid XML root element");
		try {
			Element documentElement = document.getDocumentElement();

			if (!documentElement.getNodeName().equals(getName(rootElement, clazz)))
				return null;

			return readDtoElement(documentElement, clazz);
		} catch (Exception e) {
			throw new LastPassException(e);
		}
	}

	/**
	 * Read a single attribute from the given element and coerce it into the
	 * given type
	 * 
	 * @param element
	 *            The element to read the attribute from
	 * @param type
	 *            The type to interpret the value from (must be Integer,
	 *            Boolean, String or a primitive thereof)
	 * @param name
	 *            The attribute name
	 * @return The attribute value as the given type, or null if no such
	 *         attribute exists on the given element
	 */
	private <T> T readAttribute(Element element, Class<T> type, String name) {
		if (!element.hasAttribute(name))
			return null;

		return coerce(element.getAttribute(name), type);
	}

	/**
	 * Read a direct child element from the given element and coerce it into the
	 * given type
	 * 
	 * @param element
	 *            The parent element to read the child element from
	 * @param clazz
	 *            The type to interpret the value from (must be Integer,
	 *            Boolean, String or a primitive thereof, or a type annotated
	 *            with {@link XmlType})
	 * @param name
	 *            The name of the child element to read
	 * @return The element content as the given type, or null if no such child
	 *         element exists in the given parent element
	 */
	private <T> T readElement(Element element, Class<T> clazz, String name) throws InstantiationException, IllegalAccessException {
		Element child = getChildElementByTagName(element, name);
		if (child == null)
			return null;

		if (clazz.getAnnotation(XmlType.class) != null)
			return readDtoElement(child, clazz);
		return coerce(child.getTextContent(), clazz);
	}

	/**
	 * Read the given element's contents as a DTO
	 * 
	 * @param element
	 *            The element to read as a DTO
	 * @param clazz
	 *            The DTO class (should be annotated with {@link XmlType} or
	 *            {@link XmlRootElement}
	 * @return The constructed and populated DTO
	 */
	private <T> T readDtoElement(Element element, Class<T> clazz) throws InstantiationException, IllegalAccessException {
		T dto = clazz.newInstance();
		for (Field f : clazz.getDeclaredFields()) {
			f.setAccessible(true);
			final Object value;
			XmlAttribute attributeAnnotation = f.getAnnotation(XmlAttribute.class);
			XmlElement elementAnnotation = f.getAnnotation(XmlElement.class);
			Class<?> fieldType = f.getType();
			if (attributeAnnotation != null) {
				value = readAttribute(element, fieldType, getName(attributeAnnotation, f));
			} else if (elementAnnotation != null) {
				value = readElement(element, fieldType, getName(elementAnnotation, f));
			} else {
				continue;
			}

			if (value != null)
				f.set(dto, value);
		}
		return dto;
	}

	@SuppressWarnings("unchecked")
	private <T> T coerce(String value, Class<T> type) {
		if (type == String.class) {
			return (T) value;
		} else if (type == Integer.class || type == int.class) {
			return (T) Integer.valueOf(value);
		} else if (type == Boolean.class || type == boolean.class) {
			return (T) Boolean.valueOf(value);
		} else {
			throw new UnsupportedOperationException("Unsupported type: " + type);
		}
	}

	private static String getName(XmlAttribute annotation, Field field) {
		return getDefault(annotation.name(), field.getName());
	}

	private static String getName(XmlElement annotation, Field field) {
		return getDefault(annotation.name(), field.getName());
	}

	private static String getName(XmlRootElement annotation, Class<?> clazz) {
		return getDefault(annotation.name(), CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, clazz.getSimpleName()));
	}

	private static String getDefault(String value, String defaultValue) {
		if (DEFAULT_ANNOTATION_VALUE.equals(value)) {
			return defaultValue;
		}
		return value;
	}

	private static Element getChildElementByTagName(Element node, String name) {
		Node child = node.getFirstChild();
		while (child != null) {
			if (child.getNodeType() == Node.ELEMENT_NODE && name.equals(child.getNodeName()))
				return (Element) child;
			child = child.getNextSibling();
		}
		return null;
	}
}
