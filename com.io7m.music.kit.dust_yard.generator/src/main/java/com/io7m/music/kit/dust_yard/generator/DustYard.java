/*
 * Copyright © 2021 Mark Raynsford <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.music.kit.dust_yard.generator;

import com.io7m.jnoisetype.writer.api.NTBuilderProviderType;
import com.io7m.jnoisetype.writer.api.NTWriterProviderType;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.ServiceLoader;

public final class DustYard
{
  private final Path sourceDirectory;
  private final Path temporaryDirectory;
  private final Path targetFile;

  private DustYard(
    final Path inSourceDirectory,
    final Path inTemporaryDirectory,
    final Path inTargetFile)
  {
    this.sourceDirectory =
      Objects.requireNonNull(inSourceDirectory, "sourceDirectory");
    this.temporaryDirectory =
      Objects.requireNonNull(inTemporaryDirectory, "temporaryDirectory");
    this.targetFile =
      Objects.requireNonNull(inTargetFile, "targetFile");
  }

  public static void main(
    final String[] args)
    throws Exception
  {
    final var sourceDirectory =
      Paths.get(args[0]);
    final var temporaryDirectory =
      Paths.get(args[1]);
    final var targetFile =
      Paths.get(args[2]);

    final var dustYard =
      new DustYard(sourceDirectory, temporaryDirectory, targetFile);

    dustYard.execute();
  }

  public void execute()
    throws Exception
  {
    final var builders =
      ServiceLoader.load(NTBuilderProviderType.class)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No builder service available"));

    final var writers =
      ServiceLoader.load(NTWriterProviderType.class)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No writer service available"));

    final var snare =
      DustYardSnare.open(
        this.sourceDirectory.resolve("8833__quartertone__snaredrum-14x08inchtama-veryhighpitch-multisampled")
      );
    final var snareConverted =
      DustYardSnareConversion.convertFLACs(
        snare,
        this.temporaryDirectory.resolve("snare")
      );

    final var dustYardFont =
      DustYardFont.of(builders, writers, snareConverted);

    dustYardFont.write(this.targetFile);
  }
}
