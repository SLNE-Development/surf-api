package dev.slne.surf.api.gen.generator.utils;

import com.squareup.javapoet.AnnotationSpec;
import java.util.List;

public final class Annotations {

  private static final AnnotationSpec SUPPRESS_WARNINGS = AnnotationSpec.builder(
          SuppressWarnings.class)
      .addMember("value", "$S", "unused")
      .addMember("value", "$S", "SpellCheckingInspection")
      .build();
  public static final Iterable<AnnotationSpec> CLASS_HEADER = List.of(
      SUPPRESS_WARNINGS
  );

  private Annotations() {
  }
}
