package dev.slne.surf.surfapi.bukkit.example;

import java.net.URL;
import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault.DefaultDouble;
import space.arim.dazzleconf.annote.ConfDefault.DefaultInteger;
import space.arim.dazzleconf.annote.ConfDefault.DefaultObject;
import space.arim.dazzleconf.annote.ConfDefault.DefaultString;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.ConfSerialisers;
import space.arim.dazzleconf.annote.IntegerRange;
import space.arim.dazzleconf.annote.NumericRange;
import space.arim.dazzleconf.serialiser.URLValueSerialiser;

@ConfSerialisers(URLValueSerialiser.class) // use other serialisers than the default ones
public interface ExampleConfig {

  int LAST_VERSION = 1;

  @ConfComments({
      "This is the version of the config file.",
      "Do not change this value."
  })
  @ConfKey("_version")
  @DefaultInteger(LAST_VERSION)
  int version();


  @ConfComments("This is a message.")
  @ConfKey("messages.example") // This is the path to the config value.
  @DefaultString("Hello, world!")
    // This is the default value.
  String message();

  @ConfComments("This is a bounded integer.")
  @IntegerRange(min = 0, max = 10)
  int boundedInt();

  @ConfComments("This is a bounded double.")
  @NumericRange(min = 0, max = 10.5)
  @DefaultDouble(5.5)
  double boundedDouble();

  @ConfComments("This is an enum.")
  @ConfKey("example_enum")
  @DefaultString("FIRST")
  ExampleEnum exampleEnum();

  @ConfComments("This is a random default enum.")
  @ConfKey("random_enum")
  @DefaultObject("dev.slne.surf.surfapi.bukkit.example.ExampleConfig$ExampleEnum.random")
  ExampleEnum randomEnum();

  @DefaultString("https://google.com")
  URL url();

  @ConfComments("This is a nested config.")
  @ConfKey("nested")
  NestedConfig nested();

  enum ExampleEnum {
    FIRST,
    SECOND,
    THIRD;

    public static ExampleEnum random() {
      return values()[(int) (Math.random() * values().length)];
    }
  }

  interface NestedConfig {

    @ConfComments("This is a nested message.")
    @ConfKey("nested.message")
    @DefaultString("Hello, world!")
    String message();
  }
}
