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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeMap;

public final class DustYardBassDrumConversion
{
  private static final Logger LOG =
    LoggerFactory.getLogger(DustYardBassDrumConversion.class);

  private DustYardBassDrumConversion()
  {

  }

  public static DustYardBassDrum convertFLACs(
    final DustYardBassDrum input,
    final Path outputDirectory)
    throws IOException
  {
    final var bdOutput =
      new TreeMap<Integer,Path>();

    try {
      input.byVelocity().forEach((velocity, path) -> {
        try {
          convert(bdOutput, velocity, path, outputDirectory);
        } catch (final IOException e) {
          throw new UncheckedIOException(e);
        }
      });
    } catch (final UncheckedIOException e) {
      throw e.getCause();
    }

    return new DustYardBassDrum(bdOutput);
  }

  private static void convert(
    final TreeMap<Integer,Path> bdOutput,
    final Integer velocity,
    final Path path,
    final Path outputDirectory)
    throws IOException
  {
    final var outputFile =
      outputDirectory.resolve(String.format("%02d.wav", velocity));

    Files.createDirectories(outputDirectory);

    LOG.info("write {}", outputFile);

    try (var stream = DustYardFLACToMono16.readAs16Mono(path)) {
      AudioSystem.write(stream, AudioFileFormat.Type.WAVE, outputFile.toFile());
    } catch (final UnsupportedAudioFileException e) {
      throw new IOException(e);
    }

    bdOutput.put(velocity, outputFile);
  }
}
