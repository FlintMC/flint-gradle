package net.flintmc.gradle.patch.context;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import net.flintmc.gradle.patch.PatchContextual;
import net.flintmc.gradle.patch.PatchSingle;
import net.flintmc.gradle.patch.state.PatchMode;
import net.flintmc.gradle.util.Util;

public class LocalPatchContextProvider implements PatchContextProvider {

  private final PatchContextual patchContextual;

  public LocalPatchContextProvider(PatchContextual patchContextual) {
    this.patchContextual = patchContextual;
  }

  /** {@inheritDoc} */
  @Override
  public List<String> getData(PatchSingle patch) throws IOException {
    patch.setTargetFile(this.patchContextual.computeTargetFile(patch));

    if (!patch.getTargetFile().exists() || patch.isBinary()) {
      return null;
    }

    return this.readFile(patch.getTargetFile());
  }

  /** {@inheritDoc} */
  @Override
  public void setData(PatchSingle patch, List<String> data) throws IOException {
    this.backup(patch.getTargetFile());
    this.writeFile(patch, data);
  }

  /** {@inheritDoc} */
  @Override
  public void setFailed(PatchSingle patch, List<String> lines) throws IOException {
    if (lines.isEmpty()) {
      return;
    }

    try (PrintWriter printWriter =
        new PrintWriter(new FileOutputStream(patch.getTargetFile() + ".rej"))) {
      for (String line : lines) {
        printWriter.println(line);
      }
    }
  }

  /**
   * Reads all lines from a file.
   *
   * @param target The file to be read.
   * @return The lines from the file as a {@link List}. Whether the {@code List} is modifiable or
   *     not is implementation dependent and therefore not specified
   * @throws IOException If an I/O error has occurred.
   */
  private List<String> readFile(File target) throws IOException {
    return Files.readAllLines(target.toPath());
  }

  /**
   * Creates a backup file from the given file.
   *
   * @param target The file to be backed up.
   * @throws IOException If an I/O error has occurred.
   */
  private void backup(File target) throws IOException {
    if (target.exists()) {
      this.copyStreamsCloseAll(
          new FileOutputStream(new File(target.getParentFile(), target.getName() + ".backup~")),
          new FileInputStream(target));
    }
  }

  private void copyStreamsCloseAll(OutputStream writer, InputStream reader) throws IOException {
    byte[] buffer = new byte[4096];
    int index;
    while ((index = reader.read(buffer)) != -1) {
      writer.write(buffer, 0, index);
    }
    writer.close();
    reader.close();
  }

  private void writeFile(PatchSingle patch, List<String> lines) throws IOException {
    if (patch.getPatchMode() == PatchMode.DELETE) {
      patch.getTargetFile().delete();
      return;
    }

    patch.getTargetFile().getParentFile().mkdirs();
    if (patch.isBinary()) {
      if (patch.getHunks().length == 0) {
        patch.getTargetFile().delete();
      } else {
        byte[] content = Util.deserializeLines(patch.getHunks()[0].lines);
        this.copyStreamsCloseAll(
            new FileOutputStream(patch.getTargetFile()), new ByteArrayInputStream(content));
      }
    } else {
      if (lines.size() == 0) {
        return;
      }
      try (PrintWriter printWriter =
          new PrintWriter(
              new OutputStreamWriter(
                  new FileOutputStream(patch.getTargetFile()), Charset.defaultCharset()))) {
        for (String line : lines.subList(0, lines.size() - 1)) {
          printWriter.println(line);
        }
        printWriter.print(lines.get(lines.size() - 1));
        if (!patch.isNoEndingNewline()) {
          printWriter.println();
        }
      }
    }
  }
}
