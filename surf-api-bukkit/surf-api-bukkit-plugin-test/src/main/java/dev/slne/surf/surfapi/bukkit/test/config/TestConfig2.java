package dev.slne.surf.surfapi.bukkit.test.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Matches;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public class TestConfig2 {

  //  @Required
  @Comment("Config for various connections")
  @Setting("connection")
  public ConnectionConfig connectionConfig = new ConnectionConfig();

  @ConfigSerializable
  public static class ConnectionConfig {

    //    @Required
    @Comment("Config for database connection")
    @Setting("database")
    public DatabaseConfig databaseConfig = new DatabaseConfig();

    //    @Required
    @Comment("Config for redis connection")
    @Setting("redis")
    public RedisConfig redisConfig = new RedisConfig();

    @ConfigSerializable
    public static class DatabaseConfig {

      //      @Required
      @Comment("URL for database connection. Should be in the format of jdbc:<db_type>://<host>:<port>/<database>")
      @Matches("jdbc:[a-zA-Z]+://[a-zA-Z0-9.]+:[0-9]+/[a-zA-Z0-9_]+")
      @Setting("url")
      public String url = "jdbc:mariadb://127.0.0.1/surf_data";

      //      @Required
      @Comment("Username for database connection")
      @Setting("username")
      public String username = "root";

      //      @Required
      @Comment("Password for database connection")
      @Setting("password")
      public String password = "";
    }

    @ConfigSerializable
    public static class RedisConfig {

      //      @Required
      @Comment("Host for redis connection")
      @Matches("[a-zA-Z0-9.]+")
      @Setting("host")
      public String host = "127.0.0.1";

      //      @Required
      @Comment("Port for redis connection")
      @Matches("[0-9]+")
      @Setting("port")
      public int port = 6379;

      //      @Required
      @Comment("Username for redis connection")
      @Setting("username")
      public String username = "";

      //      @Required
      @Comment("Password for redis connection")
      @Setting("password")
      public String password = "";
    }
  }

}
