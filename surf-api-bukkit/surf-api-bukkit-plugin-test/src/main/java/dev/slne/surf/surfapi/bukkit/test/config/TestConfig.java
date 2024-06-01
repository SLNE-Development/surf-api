package dev.slne.surf.surfapi.bukkit.test.config;

import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class TestConfig {

  public LinkedList<String> testLinkedList = new LinkedList<>(List.of("test1", "test2", "test3"));
  public LinkedList<TestObject> testObjectLinkedList = new LinkedList<>(
      List.of(new TestObject(1, "test1"), new TestObject(2, "test2"), new TestObject(3, "test3")));

  @ConfigSerializable
  public static class TestObject {

    public String testString;
    public int testInt;

    @SuppressWarnings("unused") // for config serialization
    protected TestObject() {
    }

    public TestObject(int testInt, String testString) {
      this.testInt = testInt;
      this.testString = testString;
    }

    @Override
    public String toString() {
      return ToStringBuilder.reflectionToString(this);
    }
  }
}
