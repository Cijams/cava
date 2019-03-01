/*
 * Copyright 2018 ConsenSys AG.
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class FileBackedFingerprintRepositoryTest {

  private SecureRandom secureRandom = new SecureRandom();

  private Bytes generateFingerprint() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    return Bytes.wrap(bytes);
  }

  @Test
  FileBackedFingerprintRepository testAddingNewFingerprint(@TempDirectory Path tempFolder) throws IOException {
    FileBackedFingerprintRepository repo = new FileBackedFingerprintRepository(tempFolder.resolve("repo"));
    Bytes fingerprint = generateFingerprint();
    repo.addFingerprint("foo", fingerprint);
    assertTrue(repo.contains("foo", fingerprint));
    assertEquals(
        "foo " + fingerprint.toHexString().substring(2).toLowerCase(),
        Files.readAllLines(tempFolder.resolve("repo")).get(0));
    return repo;
  }

  @Test
  void testUpdateFingerprint(@TempDirectory Path tempFolder) throws IOException {
    FileBackedFingerprintRepository repo = testAddingNewFingerprint(tempFolder);
    Bytes fingerprint = generateFingerprint();
    repo.addFingerprint("foo", fingerprint);
    assertTrue(repo.contains("foo", fingerprint));
    assertEquals(
        "foo " + fingerprint.toHexString().substring(2).toLowerCase(),
        Files.readAllLines(tempFolder.resolve("repo")).get(0));
  }

  @Test
  void testInvalidFingerprintAddedToFile(@TempDirectory Path tempFolder) throws IOException {
    FileBackedFingerprintRepository repo = new FileBackedFingerprintRepository(tempFolder.resolve("repo-bad2"));
    Bytes fingerprint = generateFingerprint();
    Files.write(
        tempFolder.resolve("repo-bad2"),
        ("bar " + fingerprint.slice(8).toHexString().substring(2) + "GGG").getBytes(UTF_8));
    assertThrows(TLSEnvironmentException.class, () -> repo.addFingerprint("foo", fingerprint));
  }
}
