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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

public final class DustYardSplashConversion
{
  private static final Logger LOG =
    LoggerFactory.getLogger(DustYardSplashConversion.class);

  private DustYardSplashConversion()
  {

  }

  public static DustYardSplash convertFLACs(
    final DustYardSplash input,
    final Path outputDirectory)
    throws IOException
  {
    final SortedMap<String, SortedMap<Integer, Path>> output = new TreeMap<>();

    final var inputFiles = input.files();
    for (final var kind : inputFiles.keySet()) {
      final var byVelocity = inputFiles.get(kind);
      for (final var velocity : byVelocity.keySet()) {
        final var file = byVelocity.get(velocity);
        convert(output, kind, velocity, file, outputDirectory);
      }
    }

    return new DustYardSplash(output);
  }

  private static void convert(
    final SortedMap<String, SortedMap<Integer, Path>> output,
    final String kind,
    final Integer velocity,
    final Path path,
    final Path outputDirectory)
    throws IOException
  {
    final var outputFile =
      outputDirectory.resolve(
        String.format("%s_%02d.wav", kind.toUpperCase(Locale.ROOT), velocity)
      );

    Files.createDirectories(outputDirectory);

    LOG.info("write {}", outputFile);

    try (var stream = DustYardFLACToMono16.readAs16Mono(path)) {
      AudioSystem.write(stream, AudioFileFormat.Type.WAVE, outputFile.toFile());
    } catch (final UnsupportedAudioFileException e) {
      throw new IOException(e);
    }

    final var outputVel =
      output.computeIfAbsent(kind, ignored -> new TreeMap<>());

    outputVel.put(velocity, outputFile);
  }
}
