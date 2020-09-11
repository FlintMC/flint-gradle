package net.labyfy.gradle.minecraft.yggdrasil;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.labyfy.gradle.json.JsonConverter;
import net.labyfy.gradle.util.Util;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Utility class for accessing minecraft logins.
 */
public class YggdrasilAuthenticator {
    private static final String BASE_URL = "https://authserver.mojang.com/";

    private final HttpClient httpClient;

    private final Path accessTokenPath;
    private final Path playerNamePath;
    private final Path uuidPath;
    private final UUID clientToken;

    /**
     * Constructs a new {@link YggdrasilAuthenticator} with the given cache dir.
     *
     * @param httpClient The HTTP client to use for authentication requests
     * @param cacheDir   The directory to store the auth cache in
     * @throws IOException If the reading/generation of the client token fails
     */
    public YggdrasilAuthenticator(HttpClient httpClient, Path cacheDir) throws IOException {
        this.httpClient = httpClient;

        if(!Files.isDirectory(cacheDir)) {
            // The cache dir is required for the next operations, create it
            Files.createDirectories(cacheDir);
        }

        this.accessTokenPath = cacheDir.resolve("access-token");
        this.playerNamePath = cacheDir.resolve("player-name");
        this.uuidPath = cacheDir.resolve("uuid");
        this.clientToken = getClientToken(cacheDir.resolve("client-token"));
    }

    /**
     * Checks if the authentication needs to be refreshed.
     *
     * @return {@code true} if the authentication needs to be refreshed, {@code false} otherwise
     * @throws IOException If an I/O error occurs while checking
     */
    public boolean requiresReAuth() throws IOException {
        if (!Files.exists(accessTokenPath)) {
            return true;
        }

        // Get the token from the cache
        String token = getCachedToken();

        // And validate it
        return !validateAccessToken(token);
    }

    /**
     * Tries to refresh the cached access token.
     *
     * @return {@code true} if the refresh was successful, {@code false} otherwise
     * @throws IOException If an I/O error occurs while refreshing
     */
    public boolean refreshToken() throws IOException {
        if (!Files.exists(accessTokenPath)) {
            return false;
        }

        // Construct the payload data
        Map<String, String> payloadData = new HashMap<>();
        payloadData.put("accessToken", getCachedToken());
        payloadData.put("clientToken", simplifyUUID(clientToken));

        // Convert payload to Json
        String payload = JsonConverter.OBJECT_MAPPER.writeValueAsString(payloadData);

        // Form and execute the post request
        HttpPost request = new HttpPost(BASE_URL + "refresh");
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(payload));

        // Execute the request and retrieve the status
        HttpResponse response = httpClient.execute(request);
        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode == 200) {
            // Refresh has been successful
            ObjectNode rootNode;

            try (InputStream stream = request.getEntity().getContent()) {
                // Read the value into a node
                rootNode = JsonConverter.OBJECT_MAPPER.readValue(stream, ObjectNode.class);
            }

            // Extract the required values from the Json structure
            String accessToken = rootNode.get("accessToken").requireNonNull().asText();

            // Checks whether the selectedProfile field is located in the node
            if(rootNode.has("selectedProfile")) {
                UUID playerUUID = readUUIDFromString(
                        rootNode.get("selectedProfile")
                                .get("id")
                                .requireNonNull()
                                .asText()
                );
                String playerName = rootNode.get("selectedProfile").get("name").requireNonNull().asText();

                // Save selected profile values
                saveUUID(uuidPath, playerUUID);
                saveString(playerNamePath, playerName);
            }

            // Save access token
            saveString(accessTokenPath, accessToken);

            return true;
        } else {
            // Refresh failed
            return false;
        }
    }

    /**
     * Authenticates against the Yggdrasil server from Mojang with the given username
     * and password.
     *
     * @param username The username to authenticate with
     * @param password The password to authenticate with
     * @throws YggdrasilAuthenticationException If authentication fails or an error occurs while authenticating
     */
    public void authenticate(String username, String password) throws YggdrasilAuthenticationException {
        // Construct the agent data to request the minecraft login
        Map<String, Object> agentData = new HashMap<>();
        agentData.put("name", "Minecraft");
        agentData.put("version", 1);

        // Construct the post payload data
        Map<String, Object> payloadData = new HashMap<>();
        payloadData.put("agent", agentData);
        payloadData.put("username", username);
        payloadData.put("password", password);
        payloadData.put("clientToken", simplifyUUID(clientToken));

        ObjectNode responseNode;
        try {
            // Convert the payload to Json
            String payload = JsonConverter.OBJECT_MAPPER.writeValueAsString(payloadData);

            // Send the web request for authentication
            HttpPost request = new HttpPost(BASE_URL + "authenticate");
            request.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));
            HttpResponse response = httpClient.execute(request);

            // Extract the response status
            int statusCode = response.getStatusLine().getStatusCode();
            String statusMessage = response.getStatusLine().getReasonPhrase();

            try (InputStream stream = response.getEntity().getContent()) {
                if (stream == null) {
                    // If the server did not send a response, it is probably a server error
                    throw new YggdrasilAuthenticationException("Server responded with " + statusCode + " (" +
                            statusMessage + ") and no content");
                }

                // Read the response as Json
                responseNode = JsonConverter.OBJECT_MAPPER.readValue(stream, ObjectNode.class);
            }
        } catch (JsonParseException | JsonMappingException e) {
            throw new YggdrasilAuthenticationException("Failed to parse Json response from server", e);
        } catch (JsonProcessingException e) {
            throw new YggdrasilAuthenticationException("Unexpected error while constructing payload", e);
        } catch (IOException e) {
            throw new YggdrasilAuthenticationException("Failed to contact Mojang authentication server", e);
        }

        if (responseNode.has("error")) {
            // Failed request
            throw new YggdrasilAuthenticationException(responseNode.get("errorMessage").requireNonNull().asText());
        } else {
            if (!responseNode.has("selectedProfile")) {
                // If the selectedProfile entry is missing from the Json, the account is very likely
                // one without a valid license
                throw new YggdrasilAuthenticationException(
                        "The selected account has no minecraft account associated with it");
            }

            // Retrieve important data from the response
            String accessToken = responseNode.get("accessToken").requireNonNull().asText();
            UUID playerUUID = readUUIDFromString(
                    responseNode.get("selectedProfile").get("id").requireNonNull().asText());
            String playerName = responseNode.get("selectedProfile").get("name").requireNonNull().asText();

            try {
                // Save all values so they can be accessed later
                saveString(accessTokenPath, accessToken);
                saveUUID(uuidPath, playerUUID);
                saveString(playerNamePath, playerName);
            } catch (IOException e) {
                throw new YggdrasilAuthenticationException("Failed to save authentication data", e);
            }
        }
    }

    /**
     * Reads the auth token currently cached.
     *
     * @return The currently cached auth token
     * @throws IOException If an I/O error occurs while reading the auth token
     */
    public String getCachedToken() throws IOException {
        return readString(this.accessTokenPath);
    }

    /**
     * Reads the auth token and decodes the Yggdrasil token from it.
     *
     * @return The currently cached Yggdrasil token
     * @throws IOException If an I/O error occurs while reading the Yggdrasil token
     */
    public String getCachedYggdrasilToken() throws IOException {
        String[] tokenParts = getCachedToken().split("\\.");
        if(tokenParts.length < 2) {
            throw new IOException("Cached token does not contain Yggdrasil token part");
        }

        // Read the json from the second auth token part
        ObjectNode yggdrasilNode = JsonConverter.OBJECT_MAPPER.readValue(
                Base64.getDecoder().decode(tokenParts[1]), ObjectNode.class);
        return yggdrasilNode.get("yggt").requireNonNull().asText();
    }

    /**
     * e
     * Reads the UUID currently cached.
     *
     * @return The currently cached UUID
     * @throws IOException If an I/O error occurs while reading the UUID
     */
    public UUID getCachedUUID() throws IOException {
        return readUUID(this.uuidPath);
    }

    /**
     * Reads the player name currently cached.
     *
     * @return The currently cached player name
     * @throws IOException If an I/O error occurs while reading the player name
     */
    public String getCachedPlayerName() throws IOException {
        return readString(this.playerNamePath);
    }

    /**
     * Checks if the given access token is a valid one.
     *
     * @param token The token to check
     */
    private boolean validateAccessToken(String token) throws IOException {
        // Construct the payload data
        Map<String, String> payloadData = new HashMap<>();
        payloadData.put("accessToken", token);
        payloadData.put("clientToken", simplifyUUID(clientToken));

        // Convert payload to Json
        String payload = JsonConverter.OBJECT_MAPPER.writeValueAsString(payloadData);

        // Form and execute the post request
        HttpPost request = new HttpPost(BASE_URL + "validate");
        request.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));

        HttpResponse response = httpClient.execute(request);
        int statusCode = response.getStatusLine().getStatusCode();
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * Retrieves the client token for this machine. This will be generated on first run
     * and then cached.
     *
     * @param clientTokenPath The path to obtain the token from or save it to
     * @return The generated or read client token
     * @throws IOException If generation or reading of the client token fails
     */
    private UUID getClientToken(Path clientTokenPath) throws IOException {
        if (Files.exists(clientTokenPath)) {
            // There is a cached client token
            UUID clientToken = readUUID(clientTokenPath);
            if (clientToken != null) {
                // The UUID was valid, return it
                return clientToken;
            }
            // The UUID could not be read, regenerate it
        }

        // Generate a new client token
        UUID clientToken = UUID.randomUUID();
        saveUUID(clientTokenPath, clientToken);
        return clientToken;
    }

    /**
     * Reads a {@link UUID} from the given path.
     *
     * @param path The path to read the UUID from
     * @return The read {@link UUID}, or {@code null} if the {@link UUID} file was corrupt
     * @throws IOException If an I/O error occurs while reading from the file
     */
    private UUID readUUID(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            byte[] mostSignificantBits = new byte[8];
            byte[] leastSignificantBits = new byte[8];

            // Read the 128 bits, if we read 8 bytes each time, construct a UUID from them
            if (in.read(mostSignificantBits) == 8 && in.read(leastSignificantBits) == 8) {
                return new UUID(
                        Util.longFromByteArray(mostSignificantBits),
                        Util.longFromByteArray(leastSignificantBits)
                );
            } else {
                // Corrupted file, for our case it is ok to just return null
                return null;
            }
        }
    }

    /**
     * Saves a {@link UUID} to the given path.
     *
     * @param path The path to save the UUID to
     * @param uuid The {@link UUID} to save
     * @throws IOException If an I/O error occurs while saving the UUID
     */
    private void saveUUID(Path path, UUID uuid) throws IOException {
        try (OutputStream out = Files.newOutputStream(path)) {
            long leastSignificantBits = uuid.getLeastSignificantBits();
            long mostSignificantBits = uuid.getMostSignificantBits();
            out.write(Util.longToByteArray(mostSignificantBits));
            out.write(Util.longToByteArray(leastSignificantBits));
        }
    }

    /**
     * Converts the UUID to a string and removes all dashes from it.
     *
     * @param toSimplify The UUID to simplify
     * @return The simplified UUID
     */
    private String simplifyUUID(UUID toSimplify) {
        return toSimplify.toString().replace("-", "");
    }

    /**
     * Converts a string to a UUID, adding in the dashes if required.
     *
     * @param uuid The string representation of the UUID
     * @return The created UUID
     */
    private UUID readUUIDFromString(String uuid) {
        if (uuid.contains("-")) {
            return UUID.fromString(uuid);
        } else {
            return UUID.fromString(uuid.replaceAll("(.{8})(.{4})(.{4})(.{4})(.+)", "$1-$2-$3-$4-$5"));
        }
    }

    /**
     * Reads a file as a string.
     *
     * @param path The path to the file to read
     * @return The content of the file as a string
     * @throws IOException If an I/O error occurs while reading the file
     */
    private String readString(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    /**
     * Writes a string to a file.
     *
     * @param path    The path to the file to write to
     * @param content The content to write to the file
     * @throws IOException If an I/O error occurs while writing to the file
     */
    private void saveString(Path path, String content) throws IOException {
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }
}
