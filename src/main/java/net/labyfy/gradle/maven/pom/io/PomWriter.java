package net.labyfy.gradle.maven.pom.io;

import net.labyfy.gradle.maven.pom.MavenDependency;
import net.labyfy.gradle.maven.pom.MavenPom;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class PomWriter {
    /**
     * Writes the given POM to the given file.
     *
     * @param pom    The POM to write
     * @param target The file to write to, parents are created as required
     * @throws IOException If an I/O error occurs while writing the POM
     */
    public static void write(MavenPom pom, Path target) throws IOException {
        Document document = PomIO.createXmlDocument();

        // Configure the main XML tag with standalone=true and version=1.0
        document.setXmlStandalone(true);
        document.setXmlVersion("1.0");

        // Set create the root tag and add the schemas
        Element root = document.createElement("project");
        root.setAttribute("xsi:schemaLocation",
                "http://maven.apache.org/xsd/maven-4.0.0.xsd http://maven.apache.org/xsd/maven-4.0.0.xsd");
        root.setAttribute("xmlns", "http://maven.apache.org/POM/4.0.0");
        root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        document.appendChild(root);

        // Create the model version tag with the value 4.0.0
        Element modelVersionElement = document.createElement("modelVersion");
        modelVersionElement.appendChild(document.createTextNode("4.0.0"));
        root.appendChild(modelVersionElement);

        // Create the groupId tag
        Element groupIdElement = document.createElement("groupId");
        groupIdElement.appendChild(document.createTextNode(pom.getGroupId()));
        root.appendChild(groupIdElement);

        // Create the artifactId tag
        Element artifactIdElement = document.createElement("artifactId");
        artifactIdElement.appendChild(document.createTextNode(pom.getArtifactId()));
        root.appendChild(artifactIdElement);

        // Create the version tag
        Element versionElement = document.createElement("version");
        versionElement.appendChild(document.createTextNode(pom.getVersion()));
        root.appendChild(versionElement);

        if (!pom.getDependencies().isEmpty()) {
            // Create the dependencies tag
            Element dependenciesElement = document.createElement("dependencies");
            for (MavenDependency dependency : pom.getDependencies()) {
                dependenciesElement.appendChild(createDependencyElement(
                        document,
                        dependency.getGroupId(),
                        dependency.getArtifactId(),
                        dependency.getVersion(),
                        dependency.getClassifier(),
                        dependency.getScope().getMavenName()
                ));
            }

            // Add the dependencies element to the root
            root.appendChild(dependenciesElement);
        }

        if (!Files.isDirectory(target.getParent())) {
            Files.createDirectories(target.getParent());
        }

        try (OutputStream stream = Files.newOutputStream(target)) {
            // Convert our document into a writeable source and prepare a result
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(stream);

            // Run a transformer over it to convert it to XML
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
            throw new IOException("Failed to create transformer", e);
        } catch (TransformerException e) {
            throw new IOException("Failed to transform document", e);
        }
    }

    /**
     * Creates a maven pom XML dependency element.
     *
     * @param document   The document to create the element on
     * @param group      The groupId of the dependency
     * @param name       The artifactId of the dependency
     * @param version    The version of the dependency
     * @param classifier The classifier of the dependency, or null if none
     * @param scope      The scope of the dependency
     * @return The created element
     */
    private static Element createDependencyElement(
            Document document, String group, String name, String version, String classifier, String scope) {
        Element dependencyElement = document.createElement("dependency");

        // Create the groupId tag
        Element groupIdElement = document.createElement("groupId");
        groupIdElement.appendChild(document.createTextNode(group));
        dependencyElement.appendChild(groupIdElement);

        // Create the artifactId tag
        Element artifactIdElement = document.createElement("artifactId");
        artifactIdElement.appendChild(document.createTextNode(name));
        dependencyElement.appendChild(artifactIdElement);

        if(version != null) {
            // Create the version tag
            Element versionElement = document.createElement("version");
            versionElement.appendChild(document.createTextNode(version));
            dependencyElement.appendChild(versionElement);
        }

        if (classifier != null) {
            // Create the classifier element
            Element classifierElement = document.createElement("classifier");
            classifierElement.appendChild(document.createTextNode(classifier));
            dependencyElement.appendChild(classifierElement);
        }

        // Create the scope element
        Element scopeElement = document.createElement("scope");
        scopeElement.appendChild(document.createTextNode(scope));
        dependencyElement.appendChild(scopeElement);

        return dependencyElement;
    }
}
