package com.nhinds.lastpass.impl.dto.reader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.Test;
import org.w3c.dom.Document;

public class DtoReaderTest {
	private final DtoReader reader = new DtoReader();

	@Test(expected = IllegalArgumentException.class)
	public void invalidDtoThrowsException() {
		this.reader.read(toDocument("<otherEl/>"), InvalidDto.class);
	}

	@Test
	public void wrongElementNameReturnsNull() {
		assertNull(this.reader.read(toDocument("<otherEl/>"), ValidDto.class));
	}

	@Test
	public void emptyElement() {
		ValidDto dto = this.reader.read(toDocument("<root-el/>"), ValidDto.class);
		assertNotNull(dto);
		assertNull(dto.noName);
		assertNull(dto.named);
		assertNull(dto.noNameElement);
		assertNull(dto.namedElement);
	}

	@Test
	public void populatedAttributesWithStrings() {
		ValidDto dto = this.reader.read(toDocument("<root-el noName='val1' override='val2'/>"), ValidDto.class);
		assertNotNull(dto);
		assertEquals("val1", dto.noName);
		assertEquals("val2", dto.named);
		assertNull(dto.noNameElement);
		assertNull(dto.namedElement);
	}

	@Test
	public void populatedElementsWithStrings() {
		ValidDto dto = this.reader.read(
				toDocument("<root-el><noNameElement>val3</noNameElement><override-el>val4</override-el></root-el>"), ValidDto.class);
		assertNotNull(dto);
		assertNull(dto.noName);
		assertNull(dto.named);
		assertEquals("val3", dto.noNameElement);
		assertEquals("val4", dto.namedElement);
	}

	@Test
	public void populatedElementsAndAttributesWithStrings() {
		ValidDto dto = this.reader.read(toDocument("<root-el noName='val1' override='val2'>"
				+ "<noNameElement>val3</noNameElement><override-el>val4</override-el>" + "</root-el>"), ValidDto.class);
		assertNotNull(dto);
		assertEquals("val1", dto.noName);
		assertEquals("val2", dto.named);
		assertEquals("val3", dto.noNameElement);
		assertEquals("val4", dto.namedElement);
	}

	@Test
	public void unpopulatedPrimitives() {
		Primitives dto = this.reader.read(toDocument("<primitives/>"), Primitives.class);
		assertNotNull(dto);
		assertEquals(0, dto.intAttr);
		assertNull(dto.integerAttr);
		assertFalse(dto.boolAttr);
		assertNull(dto.booleanAttr);
		assertEquals(0, dto.intElement);
		assertNull(dto.integerElement);
		assertFalse(dto.boolElement);
		assertNull(dto.booleanElement);
	}

	@Test
	public void populatedPrimitives() {
		Primitives dto = this.reader.read(toDocument("<primitives intAttr='1' integerAttr='2' boolAttr='true' booleanAttr='false'>"
				+ "<intElement>3</intElement><integerElement>4</integerElement>"
				+ "<boolElement>true</boolElement><booleanElement>false</booleanElement>" + "</primitives>"), Primitives.class);
		assertNotNull(dto);
		assertEquals(1, dto.intAttr);
		assertEquals(Integer.valueOf(2), dto.integerAttr);
		assertTrue(dto.boolAttr);
		assertFalse(dto.booleanAttr);
		assertEquals(3, dto.intElement);
		assertEquals(Integer.valueOf(4), dto.integerElement);
		assertTrue(dto.boolElement);
		assertFalse(dto.booleanElement);
	}

	@Test
	public void innerElement() {
		Outer dto = this.reader.read(toDocument("<outer><innerEl value='value'/></outer>"), Outer.class);
		assertNotNull(dto);
		assertNotNull(dto.innerEl);
		assertEquals("value", dto.innerEl.value);
	}

	private static Document toDocument(String xml) {
		try {
			Transformer identityTransform = TransformerFactory.newInstance().newTransformer();
			DOMResult result = new DOMResult();
			identityTransform.transform(new StreamSource(new StringReader(xml)), result);
			return (Document) result.getNode();
		} catch (TransformerException e) {
			throw new RuntimeException(e);
		}
	}

	public static class InvalidDto {

	}

	@XmlRootElement(name = "root-el")
	public static class ValidDto {
		@XmlAttribute
		private String noName;

		@XmlAttribute(name = "override")
		private String named;

		@XmlElement
		private String noNameElement;

		@XmlElement(name = "override-el")
		private String namedElement;
	}

	@XmlRootElement
	public static class Primitives {
		@XmlAttribute
		private int intAttr;

		@XmlElement
		private int intElement;

		@XmlAttribute
		private Integer integerAttr;

		@XmlElement
		private Integer integerElement;

		@XmlAttribute
		private boolean boolAttr;

		@XmlElement
		private boolean boolElement;

		@XmlAttribute
		private Boolean booleanAttr;

		@XmlElement
		private Boolean booleanElement;
	}

	@XmlRootElement
	public static class Outer {
		@XmlElement
		private Inner innerEl;

		@XmlType
		public static class Inner {
			@XmlAttribute
			private String value;
		}
	}

}
