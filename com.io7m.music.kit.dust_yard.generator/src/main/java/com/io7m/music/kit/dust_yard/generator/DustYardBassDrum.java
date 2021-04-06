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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class DustYardBassDrum
{
  private static final Pattern BD_PATTERN =
    Pattern.compile(
      "([0-9]+)__quartertone__bd22x16-mlp-o-v([0-9]+)\\.flac");

  private final SortedMap<Integer, Path> byVelocity;

  public DustYardBassDrum(
    final SortedMap<Integer, Path> inSnare)
  {
    this.byVelocity = Objects.requireNonNull(inSnare, "snare");
  }

  public static DustYardBassDrum open(
    final Path directory)
    throws IOException
  {
    final var byVelocity = new TreeMap<Integer,Path>();

    try (var pathStream = Files.list(directory)) {
      final var files =
        pathStream
          .map(Path::toAbsolutePath)
          .sorted()
          .collect(Collectors.toList());

      for (final var file : files) {
        final var fileName = file.getFileName().toString();
        final var matcher = BD_PATTERN.matcher(fileName);
        if (matcher.matches()) {
          final var velocity =
            matcher.group(2);
          final var velocityNumber =
            Integer.valueOf(Integer.valueOf(velocity).intValue() - 1);

          byVelocity.put(velocityNumber, file);
        }
      }

      return new DustYardBassDrum(byVelocity);
    }
  }

  public SortedMap<Integer, Path> byVelocity()
  {
    return this.byVelocity;
  }
}
