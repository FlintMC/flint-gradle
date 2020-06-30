package net.labyfy.gradle.maven.pom.io;

import net.labyfy.gradle.maven.pom.MavenDependency;
import net.labyfy.gradle.maven.pom.MavenDependencyScope;
import net.labyfy.gradle.maven.pom.MavenPom;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class PomReader {
    /**
     * Reads a maven POM from the given path.
     *
     * @param source The path to read the POM from
     * @return The read POM
     * @throws IOException If an I/O exception occurs while reading the POM or if it can't be parsed
     */
    public static MavenPom read(Path source) throws IOException {
        try(InputStream stream = Files.newInputStream(source)) {
            return read(stream);
        }
    }

    /**
     * Reads a maven POM from the given stream.
     *
     * @param source The stream to read the POM from
     * @return The read POM
     * @throws IOException If an I/O exception occurs while reading the POM or if it can't be parsed
     */
    public static MavenPom read(InputStream source) throws IOException {
        Document document = PomIO.createXmlDocument(source);

        // Get the top level attributes
        Element root = document.getDocumentElement();
        MavenPom pom = new MavenPom(
                readSingleText(root, "groupId"),
                readSingleText(root, "artifactId"),
                readSingleText(root, "version")
        );

        NodeList nodes = root.getElementsByTagName("dependency");
        if(nodes != null && nodes.getLength() > 0) {
            // Dependencies found, iterate them
            for(int i = 0; i < nodes.getLength(); i++) {
                Node dependencyNode = nodes.item(i);

                if(dependencyNode.getNodeType() != Node.ELEMENT_NODE) {
                    // Dependencies are always nodes, if not the POM is malformed
                    throw new IllegalStateException("Expected dependency node to be an element");
                }

                // Construct the dependency
                Element dependencyElement = (Element) dependencyNode;
                MavenDependency dependency = new MavenDependency(
                        readSingleText(dependencyElement, "groupId"),
                        readSingleText(dependencyElement, "artifactId"),
                        readSingleText(dependencyElement, "version"),
                        readSingleText(dependencyElement, "classifier"),
                        readSingleText(dependencyElement, "type"),
                        stringToScope(readSingleText(dependencyElement, "scope")),
                        Boolean.parseBoolean(readSingleText(dependencyElement, "optional"))
                );

                // Add the constructed dependency to the final POM
                pom.addDependency(dependency);
            }
        }

        return pom;
    }

    /**
     * Reads the next tag content from the given tag from the given parent.
     *
     * @param parent The parent to find the next tag from
     * @param tagName The name of the next tag to find
     * @return The content of the tag, or {@code null} if no matching tag was found
     */
    private static String readSingleText(Element parent, String tagName) {
        NodeList elements = parent.getElementsByTagName(tagName);
        if(elements.getLength() < 1) {
            return null;
        }

        return elements.item(0).getTextContent();
    }

    /**
     * Converts a {@link String} to its matching {@link MavenDependencyScope}.
     *
     * @param scope The string representation of the scope, or {@code null} for the default scope
     * @return The parsed scope, will always be {@link MavenDependencyScope#COMPILE} if input is null
     * @throws IllegalArgumentException If the given scope does not match a maven dependency scope
     */
    private static MavenDependencyScope stringToScope(String scope) {
        if(scope == null) {
            return MavenDependencyScope.COMPILE;
        }

        for(MavenDependencyScope mavenScope : MavenDependencyScope.values()) {
            if(mavenScope.getMavenName().equals(scope)) {
                return mavenScope;
            }
        }

        throw new IllegalArgumentException("Invalid scope " + scope);
    }
}
