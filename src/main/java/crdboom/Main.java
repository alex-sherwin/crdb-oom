package crdboom;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

public class Main {

  public static void main(String[] args) throws Exception {

    final DataSource ds = dataSource();

    for (int i = 0; i < 1000; i++) {

      try (final Connection db = ds.getConnection()) {

        final PreparedStatement ps = db.prepareStatement("SELECT st1.* FROM sometable1 st1 INNER JOIN sometable2 st2 ON st1.id = st2.sometable1_id");

        ResultSet res = ps.executeQuery();

        while (res.next()) {
          final UUID id = res.getObject("id", UUID.class);

          System.out.println("map.id=" + id);

        }

        final PreparedStatement ps2 = db.prepareStatement("SELECT * FROM sometable2");
        res = ps2.executeQuery();

        while (res.next()) {
          final UUID id = res.getObject("id", UUID.class);
          final Array dnsServers = res.getArray("dnsServers");
          System.out.println("st2.id=" + id + ", array=" + dnsServers);
        }
      }

    }

  }


  private static DataSource dataSource() {

    final String jdbcUrl = "jdbc:postgresql://127.0.0.1:26267/test1?sslmode=disable";

    HikariConfig poolConfig = new HikariConfig();
    poolConfig.setPoolName("rdbms-pool");
    poolConfig.setJdbcUrl(jdbcUrl);
    poolConfig.setUsername("test1");
    poolConfig.setPassword("");
    poolConfig.setMinimumIdle(2);
    poolConfig.setMaximumPoolSize(10);
    poolConfig.setReadOnly(false);
    poolConfig.addDataSourceProperty("url", jdbcUrl);
    poolConfig.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");

    return new HikariDataSource(poolConfig);
  }
}
