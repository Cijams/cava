/*
 * Copyright 2018, ConsenSys Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.net.tls;

import static net.consensys.cava.crypto.Hash.sha2_256;
import static net.consensys.cava.net.tls.SecurityTestUtils.DUMMY_FINGERPRINT;
import static net.consensys.cava.net.tls.SecurityTestUtils.startServer;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.cava.junit.VertxExtension;
import net.consensys.cava.junit.VertxInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.netty.util.internal.StringUtil;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.SelfSignedCertificate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
@ExtendWith(VertxExtension.class)
class ClientRecordTest {

  private static String caValidFingerprint;
  private static HttpServer caValidServer;
  private static String fooFingerprint;
  private static HttpServer fooServer;
  private static String barFingerprint;
  private static HttpServer barServer;
  private static String foobarFingerprint;
  private static HttpServer foobarServer;

  private Path knownServersFile;
  private HttpClient client;

  @BeforeAll
  static void startServers(@TempDirectory Path tempDir, @VertxInstance Vertx vertx) throws Exception {
    SelfSignedCertificate caSignedCert = SelfSignedCertificate.create("localhost");
    SecurityTestUtils.configureJDKTrustStore(tempDir, caSignedCert);
    caValidFingerprint = StringUtil
        .toHexStringPadded(sha2_256(SecurityTestUtils.loadPEM(Paths.get(caSignedCert.keyCertOptions().getCertPath()))));
    caValidServer = vertx
        .createHttpServer(new HttpServerOptions().setSsl(true).setPemKeyCertOptions(caSignedCert.keyCertOptions()))
        .requestHandler(context -> context.response().end("OK"));
    startServer(caValidServer);

    SelfSignedCertificate fooCert = SelfSignedCertificate.create("foo.com");
    fooFingerprint = StringUtil
        .toHexStringPadded(sha2_256(SecurityTestUtils.loadPEM(Paths.get(fooCert.keyCertOptions().getCertPath()))));
    fooServer = vertx
        .createHttpServer(new HttpServerOptions().setSsl(true).setPemKeyCertOptions(fooCert.keyCertOptions()))
        .requestHandler(context -> context.response().end("OK"));
    startServer(fooServer);

    SelfSignedCertificate barCert = SelfSignedCertificate.create("bar.com");
    barFingerprint = StringUtil
        .toHexStringPadded(sha2_256(SecurityTestUtils.loadPEM(Paths.get(barCert.keyCertOptions().getCertPath()))));
    barServer = vertx
        .createHttpServer(new HttpServerOptions().setSsl(true).setPemKeyCertOptions(barCert.keyCertOptions()))
        .requestHandler(context -> context.response().end("OK"));
    startServer(barServer);

    SelfSignedCertificate foobarCert = SelfSignedCertificate.create("foobar.com");
    foobarFingerprint = StringUtil
        .toHexStringPadded(sha2_256(SecurityTestUtils.loadPEM(Paths.get(foobarCert.keyCertOptions().getCertPath()))));
    foobarServer = vertx
        .createHttpServer(new HttpServerOptions().setSsl(true).setPemKeyCertOptions(foobarCert.keyCertOptions()))
        .requestHandler(context -> context.response().end("OK"));
    startServer(foobarServer);
  }

  @BeforeEach
  void setupClient(@TempDirectory Path tempDir, @VertxInstance Vertx vertx) throws Exception {
    knownServersFile = tempDir.resolve("known-hosts.txt");
    Files.write(
        knownServersFile,
        Arrays.asList("#First line", "localhost:" + foobarServer.actualPort() + " " + DUMMY_FINGERPRINT));

    HttpClientOptions options = new HttpClientOptions();
    options
        .setSsl(true)
        .setTrustOptions(VertxTrustOptions.recordServerFingerprints(knownServersFile, false))
        .setConnectTimeout(1500)
        .setReuseAddress(true)
        .setReusePort(true);
    client = vertx.createHttpClient(options);
  }

  @AfterEach
  void cleanupClient() {
    client.close();
  }

  @AfterAll
  static void stopServers() {
    caValidServer.close();
    fooServer.close();
    barServer.close();
    foobarServer.close();
    System.clearProperty("javax.net.ssl.trustStore");
    System.clearProperty("javax.net.ssl.trustStorePassword");
  }

  @Test
  void shouldNotValidateUsingCertificate() throws Exception {
    CompletableFuture<Integer> statusCode = new CompletableFuture<>();
    client
        .post(
            caValidServer.actualPort(),
            "localhost",
            "/sample",
            response -> statusCode.complete(response.statusCode()))
        .exceptionHandler(statusCode::completeExceptionally)
        .end();
    assertEquals((Integer) 200, statusCode.join());

    List<String> knownServers = Files.readAllLines(knownServersFile);
    assertEquals(3, knownServers.size(), "Host was verified using CA");
    assertEquals("#First line", knownServers.get(0));
    assertEquals("localhost:" + foobarServer.actualPort() + " " + DUMMY_FINGERPRINT, knownServers.get(1));
    assertEquals("localhost:" + caValidServer.actualPort() + " " + caValidFingerprint, knownServers.get(2));
  }

  @Test
  void shouldRecordMultipleHosts() throws Exception {
    CompletableFuture<Integer> statusCode = new CompletableFuture<>();
    client
        .post(fooServer.actualPort(), "localhost", "/sample", response -> statusCode.complete(response.statusCode()))
        .exceptionHandler(statusCode::completeExceptionally)
        .end();
    assertEquals((Integer) 200, statusCode.join());

    List<String> knownServers = Files.readAllLines(knownServersFile);
    assertEquals(3, knownServers.size(), String.join("\n", knownServers));
    assertEquals("#First line", knownServers.get(0));
    assertEquals("localhost:" + foobarServer.actualPort() + " " + DUMMY_FINGERPRINT, knownServers.get(1));
    assertEquals("localhost:" + fooServer.actualPort() + " " + fooFingerprint, knownServers.get(2));

    CompletableFuture<Integer> secondStatusCode = new CompletableFuture<>();
    client
        .post(
            barServer.actualPort(),
            "localhost",
            "/sample",
            response -> secondStatusCode.complete(response.statusCode()))
        .exceptionHandler(secondStatusCode::completeExceptionally)
        .end();
    assertEquals((Integer) 200, secondStatusCode.join());

    knownServers = Files.readAllLines(knownServersFile);
    assertEquals(4, knownServers.size(), String.join("\n", knownServers));
    assertEquals("#First line", knownServers.get(0));
    assertEquals("localhost:" + foobarServer.actualPort() + " " + DUMMY_FINGERPRINT, knownServers.get(1));
    assertEquals("localhost:" + fooServer.actualPort() + " " + fooFingerprint, knownServers.get(2));
    assertEquals("localhost:" + barServer.actualPort() + " " + barFingerprint, knownServers.get(3));
  }

  @Test
  void shouldReplaceFingerprint() throws Exception {
    CompletableFuture<Integer> statusCode = new CompletableFuture<>();
    client
        .post(fooServer.actualPort(), "localhost", "/sample", response -> statusCode.complete(response.statusCode()))
        .exceptionHandler(statusCode::completeExceptionally)
        .end();
    assertEquals((Integer) 200, statusCode.join());

    List<String> knownServers = Files.readAllLines(knownServersFile);
    assertEquals(3, knownServers.size(), String.join("\n", knownServers));
    assertEquals("#First line", knownServers.get(0));
    assertEquals("localhost:" + foobarServer.actualPort() + " " + DUMMY_FINGERPRINT, knownServers.get(1));
    assertEquals("localhost:" + fooServer.actualPort() + " " + fooFingerprint, knownServers.get(2));

    CompletableFuture<Integer> secondStatusCode = new CompletableFuture<>();
    client
        .post(
            foobarServer.actualPort(),
            "localhost",
            "/sample",
            response -> secondStatusCode.complete(response.statusCode()))
        .exceptionHandler(secondStatusCode::completeExceptionally)
        .end();
    assertEquals((Integer) 200, secondStatusCode.join());

    knownServers = Files.readAllLines(knownServersFile);
    assertEquals(3, knownServers.size(), String.join("\n", knownServers));
    assertEquals("#First line", knownServers.get(0));
    assertEquals("localhost:" + foobarServer.actualPort() + " " + foobarFingerprint, knownServers.get(1));
    assertEquals("localhost:" + fooServer.actualPort() + " " + fooFingerprint, knownServers.get(2));
  }
}
