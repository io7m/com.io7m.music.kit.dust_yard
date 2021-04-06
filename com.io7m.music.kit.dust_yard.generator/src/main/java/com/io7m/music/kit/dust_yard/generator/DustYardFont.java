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

import com.io7m.jnoisetype.api.NTBankIndex;
import com.io7m.jnoisetype.api.NTGenerators;
import com.io7m.jnoisetype.api.NTGenericAmount;
import com.io7m.jnoisetype.api.NTInfo;
import com.io7m.jnoisetype.api.NTLongString;
import com.io7m.jnoisetype.api.NTPitch;
import com.io7m.jnoisetype.api.NTShortString;
import com.io7m.jnoisetype.api.NTTransforms;
import com.io7m.jnoisetype.api.NTVersion;
import com.io7m.jnoisetype.writer.api.NTBuilderProviderType;
import com.io7m.jnoisetype.writer.api.NTBuilderType;
import com.io7m.jnoisetype.writer.api.NTInstrumentBuilderType;
import com.io7m.jnoisetype.writer.api.NTSampleBuilderType;
import com.io7m.jnoisetype.writer.api.NTWriteException;
import com.io7m.jnoisetype.writer.api.NTWriterProviderType;
import com.io7m.jsamplebuffer.api.SampleBufferType;
import com.io7m.jsamplebuffer.vanilla.SampleBufferDouble;
import com.io7m.jsamplebuffer.xmedia.SampleBufferXMedia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public final class DustYardFont
{
  private static final Logger LOG =
    LoggerFactory.getLogger(DustYardFont.class);

  private static final int CYMBAL_ROOT = 60;
  private static final int SPLASH_ROOT = 84;

  private final NTBuilderProviderType builders;
  private final NTWriterProviderType writers;
  private final DustYardSnare snare;
  private final DustYardBassDrum bassDrum;
  private final DustYardChinaHiHat cym;
  private final DustYardSplash splashConverted;

  public DustYardFont(
    final NTBuilderProviderType inBuilders,
    final NTWriterProviderType inWriters,
    final DustYardSnare inSnare,
    final DustYardBassDrum inBassDrum,
    final DustYardChinaHiHat inCym,
    final DustYardSplash inSplashConverted)
  {
    this.builders =
      Objects.requireNonNull(inBuilders, "builders");
    this.writers =
      Objects.requireNonNull(inWriters, "writers");
    this.snare =
      Objects.requireNonNull(inSnare, "snare");
    this.bassDrum =
      Objects.requireNonNull(inBassDrum, "bd");
    this.cym =
      Objects.requireNonNull(inCym, "cym");
    this.splashConverted =
      Objects.requireNonNull(inSplashConverted, "splashConverted");
  }

  public static DustYardFont of(
    final NTBuilderProviderType builders,
    final NTWriterProviderType writers,
    final DustYardSnare snare,
    final DustYardBassDrum bd,
    final DustYardChinaHiHat cym,
    final DustYardSplash splashConverted)
  {
    return new DustYardFont(
      builders,
      writers,
      snare,
      bd,
      cym,
      splashConverted
    );
  }

  /**
   * Add all of the bass drum samples.
   */

  private static void addBassDrumSampleDefinitions(
    final List<NTSampleBuilderType> bdSamples,
    final NTInstrumentBuilderType sfInstrument)
  {
    final var velocityRegionSize = 128 / bdSamples.size();

    var velocityLow = 0;
    var velocityHigh = velocityLow + velocityRegionSize;

    for (final var bdSample : bdSamples) {
      final var zone = sfInstrument.addZone();
      zone.addKeyRangeGenerator(24, 24);
      zone.addGenerator(
        NTGenerators.findForName("sampleModes").orElseThrow(),
        NTGenericAmount.of(0));
      zone.addVelocityRangeGenerator(velocityLow, velocityHigh);
      zone.addSampleGenerator(bdSample);
      zone.addGenerator(
        NTGenerators.findForName("decayVolEnv").orElseThrow(),
        NTGenericAmount.of(702)
      );
      zone.addGenerator(
        NTGenerators.findForName("sustainVolEnv").orElseThrow(),
        NTGenericAmount.of(1440)
      );

      velocityLow = velocityHigh + 1;
      velocityHigh = Math.min(127, velocityHigh + velocityRegionSize);
    }
  }

  /**
   * Add all of the china hihat samples.
   */

  private static void addChinaHiHatSampleDefinitions(
    final List<NTSampleBuilderType> cymSamples,
    final NTInstrumentBuilderType sfInstrument)
  {
    var index = 0;
    for (final var cymSample : cymSamples) {
      final var zone = sfInstrument.addZone();
      zone.addKeyRangeGenerator(CYMBAL_ROOT + index, CYMBAL_ROOT + index);
      zone.addGenerator(
        NTGenerators.findForName("sampleModes").orElseThrow(),
        NTGenericAmount.of(0));
      zone.addSampleGenerator(cymSample);
      ++index;
    }
  }

  /**
   * Add all of the snare samples.
   */

  private static void addSnareSampleDefinitions(
    final SortedMap<Integer, List<NTSampleBuilderType>> snareSamples,
    final NTInstrumentBuilderType sfInstrument)
  {
    for (final var snareEntry : snareSamples.entrySet()) {
      final var rootNote =
        snareEntry.getKey().intValue();
      final var samples =
        snareEntry.getValue();

      final var velocityRegionSize = 128 / samples.size();
      var velocityLow = 0;
      var velocityHigh = velocityLow + velocityRegionSize;

      for (final var sample : samples) {
        final var zone = sfInstrument.addZone();
        zone.addKeyRangeGenerator(rootNote, rootNote);
        zone.addGenerator(
          NTGenerators.findForName("sampleModes").orElseThrow(),
          NTGenericAmount.of(0));
        zone.addVelocityRangeGenerator(velocityLow, velocityHigh);
        zone.addSampleGenerator(sample);

        velocityLow = velocityHigh + 1;
        velocityHigh = Math.min(127, velocityHigh + velocityRegionSize);
      }
    }
  }

  /**
   * Add all of the splash samples.
   */

  private static void addSplashSampleDefinitions(
    final SortedMap<Integer, List<NTSampleBuilderType>> splashSamples,
    final NTInstrumentBuilderType sfInstrument)
  {
    for (final var splashEntry : splashSamples.entrySet()) {
      final var rootNote =
        splashEntry.getKey().intValue();
      final var samples =
        splashEntry.getValue();

      final var velocityRegionSize = 128 / samples.size();
      var velocityLow = 0;
      var velocityHigh = velocityLow + velocityRegionSize;

      for (final var sample : samples) {
        final var zone = sfInstrument.addZone();
        zone.addKeyRangeGenerator(rootNote, rootNote);
        zone.addGenerator(
          NTGenerators.findForName("sampleModes").orElseThrow(),
          NTGenericAmount.of(0));
        zone.addVelocityRangeGenerator(velocityLow, velocityHigh);
        zone.addSampleGenerator(sample);

        velocityLow = velocityHigh + 1;
        velocityHigh = Math.min(127, velocityHigh + velocityRegionSize);
      }
    }
  }

  private static List<NTSampleBuilderType> addSnareSpecific(
    final NTBuilderType builder,
    final int rootNote,
    final DustYardSnareTautnessKind tautnessKind,
    final DustYardSnareStrikeKind strikeKind,
    final DustYardSnareStrikeFiles strikeFiles)
    throws IOException
  {
    final var samples = new ArrayList<NTSampleBuilderType>();
    for (final var entry : strikeFiles.filesByVelocity().entrySet()) {
      final var velocity = entry.getKey();
      final var file = entry.getValue();

      final var sampleName =
        String.format(
          "%s_%s_%02d",
          tautnessKind.shortName(),
          strikeKind.shortName(),
          velocity
        );

      final var sample =
        builder.addSample(sampleName);

      final SampleBufferType sampleBuffer;
      try (var stream = AudioSystem.getAudioInputStream(file.toFile())) {
        sampleBuffer = SampleBufferXMedia.sampleBufferOfStream(
          stream, DustYardFont::buffers
        );
      } catch (final UnsupportedAudioFileException e) {
        throw new IOException(e);
      }

      sample.setSampleRate((int) sampleBuffer.sampleRate());
      sample.setPitchCorrection(0);
      sample.setSampleCount(sampleBuffer.samples());
      sample.setOriginalPitch(NTPitch.of(rootNote));
      sample.setLoopStart(0L);
      sample.setLoopEnd(sampleBuffer.samples() - 1L);
      sample.setDataWriter(ch -> copySampleToChannel(
        sampleBuffer,
        sampleName,
        ch));
      samples.add(sample);
    }
    return List.copyOf(samples);
  }

  private static SampleBufferType buffers(
    final int channels,
    final long frames,
    final double sampleRate)
  {
    return SampleBufferDouble.createWithHeapBuffer(
      channels,
      frames,
      sampleRate
    );
  }

  private static String textResource(
    final String name)
    throws IOException
  {
    final var path =
      String.format("/com/io7m/music/kit/dust_yard/generator/%s", name);
    try (var stream = DustYardFont.class.getResourceAsStream(path)) {
      return new String(stream.readAllBytes(), US_ASCII);
    }
  }

  private static void copySampleToChannel(
    final SampleBufferType source,
    final String sampleName,
    final SeekableByteChannel channel)
    throws IOException
  {
    LOG.debug("copying: {}", sampleName);

    final var buffer =
      ByteBuffer.allocate(Math.toIntExact(source.samples() * 2L))
        .order(LITTLE_ENDIAN);

    for (var index = 0L; index < source.frames(); ++index) {
      final var frame_d = source.frameGetExact(index);
      final var frame_s = frame_d * 32767.0;
      final var frame_i = (short) frame_s;
      buffer.putShort(frame_i);
    }

    buffer.flip();
    final var wrote = channel.write(buffer);
    if (wrote != buffer.capacity()) {
      throw new IOException(
        new StringBuilder(32)
          .append("Wrote too few bytes (wrote ")
          .append(wrote)
          .append(" expected ")
          .append(buffer.capacity())
          .append(")")
          .toString()
      );
    }
  }

  public void write(
    final Path fileOutput)
    throws IOException
  {
    final var builder = this.builders.createBuilder();
    builder.setInfo(
      NTInfo.builder()
        .setName(NTShortString.of("Dust Yard"))
        .setVersion(NTVersion.of(2, 11))
        .setProduct(NTShortString.of("com.io7m.music.kit.dust_yard"))
        .setEngineers(NTShortString.of("Mark Raynsford <audio@io7m.com>"))
        .setCopyright(NTShortString.of("(c) 2021 Mark Raynsford <audio@io7m.com>"))
        .setCreationDate(NTShortString.of(OffsetDateTime.now().toString()))
        .setComment(NTLongString.of(textResource("comment.txt")))
        .build()
    );

    final var snareSamples =
      this.addSnare(builder);
    final var bdSamples =
      this.addBassDrum(builder);
    final var cymSamples =
      this.addChinaHiHat(builder);
    final var splashSamples =
      this.addSplash(builder);

    final var sfInstrument =
      builder.addInstrument("dustYard");
    final var preset =
      builder.addPreset(
        NTBankIndex.of(128),
        sfInstrument.name().value()
      );

    final var presetZoneGlobal =
      preset.addZone()
        .addKeyRangeGenerator(0, 127)
        .addInstrumentGenerator(sfInstrument);

    final var instrumentZoneGlobal =
      sfInstrument.addZone();

    /*
     * Allow for controlling the pitch via the pitch wheel.
     */

    instrumentZoneGlobal.addModulator(
      526,
      NTGenerators.findForName("coarseTune")
        .orElseThrow(() -> new IllegalStateException("Missing generator")),
      (short) 12,
      512,
      NTTransforms.find(0)
    );

    /*
     * Apply an inverse attenuation value based on the key velocity. This
     * means that the velocity curve is not linear, with notes at 0 velocity
     * still being audible.
     */

    instrumentZoneGlobal.addModulator(
      258,
      NTGenerators.findForName("initialAttenuation")
        .orElseThrow(() -> new IllegalStateException("Missing generator")),
      (short) -128,
      0,
      NTTransforms.find(0)
    );

    addSnareSampleDefinitions(snareSamples, sfInstrument);
    addBassDrumSampleDefinitions(bdSamples, sfInstrument);
    addChinaHiHatSampleDefinitions(cymSamples, sfInstrument);
    addSplashSampleDefinitions(splashSamples, sfInstrument);
    this.serialize(fileOutput, builder);
  }

  private SortedMap<Integer, List<NTSampleBuilderType>> addSplash(
    final NTBuilderType builder)
    throws IOException
  {
    final SortedMap<Integer, List<NTSampleBuilderType>> samples =
      new TreeMap<>();
    final var splashFiles =
      this.splashConverted.files();

    var index = 0;
    for (final var kind : splashFiles.keySet()) {
      final var byVelocity = splashFiles.get(kind);
      for (final var velocity : byVelocity.keySet()) {
        final var file = byVelocity.get(velocity);

        final var sampleName =
          String.format("SP_%s_%02d", kind.toUpperCase(Locale.ROOT), velocity);
        final var sample =
          builder.addSample(sampleName);

        final SampleBufferType sampleBuffer;
        try (var stream = AudioSystem.getAudioInputStream(file.toFile())) {
          sampleBuffer =
            SampleBufferXMedia.sampleBufferOfStream(
              stream, DustYardFont::buffers);
        } catch (final UnsupportedAudioFileException e) {
          throw new IOException(e);
        }

        sample.setSampleRate((int) sampleBuffer.sampleRate());
        sample.setPitchCorrection(0);
        sample.setSampleCount(sampleBuffer.samples());
        sample.setOriginalPitch(NTPitch.of(SPLASH_ROOT + index));
        sample.setLoopStart(0L);
        sample.setLoopEnd(sampleBuffer.samples() - 1L);
        sample.setDataWriter(ch -> copySampleToChannel(
          sampleBuffer,
          sampleName,
          ch));

        final var velocities =
          samples.computeIfAbsent(
            Integer.valueOf(SPLASH_ROOT + index),
            ignored -> new ArrayList<>()
          );

        velocities.add(sample);
      }
      ++index;
    }

    return samples;
  }

  private List<NTSampleBuilderType> addChinaHiHat(
    final NTBuilderType builder)
    throws IOException
  {
    final List<NTSampleBuilderType> samples = new ArrayList<>();
    final var entries = this.cym.byKind().entrySet();

    var index = 0;
    for (final var entry : entries) {
      final var file = entry.getValue();
      final var kind = entry.getKey();

      final var sampleName =
        String.format("CHH_%s", kind.toUpperCase(Locale.ROOT));
      final var sample =
        builder.addSample(sampleName);

      final SampleBufferType sampleBuffer;
      try (var stream = AudioSystem.getAudioInputStream(file.toFile())) {
        sampleBuffer =
          SampleBufferXMedia.sampleBufferOfStream(
            stream, DustYardFont::buffers);
      } catch (final UnsupportedAudioFileException e) {
        throw new IOException(e);
      }

      sample.setSampleRate((int) sampleBuffer.sampleRate());
      sample.setPitchCorrection(0);
      sample.setSampleCount(sampleBuffer.samples());
      sample.setOriginalPitch(NTPitch.of(CYMBAL_ROOT + index));
      sample.setLoopStart(0L);
      sample.setLoopEnd(sampleBuffer.samples() - 1L);
      sample.setDataWriter(ch -> copySampleToChannel(
        sampleBuffer,
        sampleName,
        ch));
      samples.add(sample);
      ++index;
    }

    return List.copyOf(samples);
  }

  private List<NTSampleBuilderType> addBassDrum(
    final NTBuilderType builder)
    throws IOException
  {
    final List<NTSampleBuilderType> samples =
      new ArrayList<>();

    for (final var entry : this.bassDrum.byVelocity().entrySet()) {
      final var velocity = entry.getKey();
      final var file = entry.getValue();

      final var sampleName =
        String.format("BD_%02d", velocity);
      final var sample =
        builder.addSample(sampleName);

      final SampleBufferType sampleBuffer;
      try (var stream = AudioSystem.getAudioInputStream(file.toFile())) {
        sampleBuffer =
          SampleBufferXMedia.sampleBufferOfStream(
            stream, DustYardFont::buffers);
      } catch (final UnsupportedAudioFileException e) {
        throw new IOException(e);
      }

      sample.setSampleRate((int) sampleBuffer.sampleRate());
      sample.setPitchCorrection(0);
      sample.setSampleCount(sampleBuffer.samples());
      sample.setOriginalPitch(NTPitch.of(24));
      sample.setLoopStart(0L);
      sample.setLoopEnd(sampleBuffer.samples() - 1L);
      sample.setDataWriter(ch -> copySampleToChannel(
        sampleBuffer,
        sampleName,
        ch));
      samples.add(sample);
    }
    return List.copyOf(samples);
  }

  private void serialize(
    final Path fileOutput,
    final NTBuilderType builder)
    throws IOException
  {
    final var description = builder.build();
    try (var channel =
           FileChannel.open(fileOutput, CREATE, TRUNCATE_EXISTING, WRITE)) {
      final var writer =
        this.writers.createForChannel(fileOutput.toUri(), description, channel);
      writer.write();
    } catch (final NTWriteException e) {
      throw new IOException(e);
    }
  }

  private SortedMap<Integer, List<NTSampleBuilderType>> addSnare(
    final NTBuilderType builder)
    throws IOException
  {
    final AtomicInteger rootNote =
      new AtomicInteger(36);
    final SortedMap<Integer, List<NTSampleBuilderType>> samples =
      new TreeMap<>();

    try {
      this.snare.snare().forEach((tautnessKind, tautnessFiles) -> {
        tautnessFiles.filesByKind().forEach((strikeKind, strikeFiles) -> {
          try {
            final var rootNoteNow = rootNote.get();
            final var sampleList =
              addSnareSpecific(
                builder,
                rootNoteNow,
                tautnessKind,
                strikeKind,
                strikeFiles
              );

            samples.put(Integer.valueOf(rootNoteNow), sampleList);
            rootNote.incrementAndGet();
          } catch (final IOException e) {
            throw new UncheckedIOException(e);
          }
        });
      });
    } catch (final UncheckedIOException e) {
      throw e.getCause();
    }
    return samples;
  }
}
