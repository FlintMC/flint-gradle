package net.flintmc.gradle.maven.pom.io;

import net.flintmc.gradle.FlintGradleException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

public final class PomIO {
  private static final DocumentBuilder DOCUMENT_BUILDER;

  static {
    try {
      DOCUMENT_BUILDER = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new FlintGradleException("Failed to create document builder", e);
    }
  }

  public static Document createXmlDocument() {
    return DOCUMENT_BUILDER.newDocument();
  }

  public static Document createXmlDocument(InputStream stream) throws IOException {
    try {
      return DOCUMENT_BUILDER.parse(stream);
    } catch (SAXException e) {
      throw new IOException("Failed to parse XML", e);
    }
  }
}
