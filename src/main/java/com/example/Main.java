/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@SpringBootApplication
public class Main {

  @Value("${spring.datasource.url}")
  private String dbUrl;

  @Autowired
  private DataSource dataSource;

  public static void main(String[] args) throws Exception {
    SpringApplication.run(Main.class, args);
  }

  @RequestMapping("/addPoint/latitude/{latitude}/longitude/{longitude}/altitude/{altitude}")
  public String setPoint(@PathVariable(value = "latitude") double latitude,
                         @PathVariable(value = "longitude") double longitude,
                         @PathVariable(value = "altitude") double altitude) {

      try (Connection connection = dataSource.getConnection()) {
          Statement s = connection.createStatement();
          s.executeUpdate(
                  "CREATE TABLE IF NOT EXISTS points (id SERIAL, " +
                  "latitude double precision, " +
                  "longitude double precision, " +
                  "altitude double precision," +
                  "latEps double precision," +
                  "longEps double precision," +
                  "altEps double precision," +
                  "lastUpdate timestamp)");

          PreparedStatement ps = connection.prepareStatement(
                  "INSERT INTO points (latitude, longitude, altitude, latEps, longEps, altEps)" +
                  "VALUES (?, ?, ?, ?, ?, ?)",
                  Statement.RETURN_GENERATED_KEYS);
          ps.setDouble(1, latitude);
          ps.setDouble(2, longitude);
          ps.setDouble(3, altitude);
          ps.setDouble(4, 0);
          ps.setDouble(5, 0);
          ps.setDouble(6, 0);
          ps.executeUpdate();

          ResultSet rs = ps.getGeneratedKeys();

          if (rs.next()) {
              return "Id: " + rs.getInt(1);
          }

      } catch (Exception e) {
          return "Error" + e.getMessage();
      }

      return "Insert operation failed";
  }

  @RequestMapping("/getAllPoints")
  public Object getAllPoints() {
      try (Connection connection = dataSource.getConnection()) {
          Statement s = connection.createStatement();
          ResultSet rs = s.executeQuery("select * from points");
          List<Point> points = new ArrayList<>();

          while (rs.next()) {
              Point p = new Point();
              p.id = rs.getInt(1);
              p.latitude = rs.getDouble(2);
              p.longitude = rs.getDouble(3);
              p.altitude = rs.getDouble(4);
              p.latEps = rs.getDouble(5);
              p.longEps = rs.getDouble(6);
              p.altEps = rs.getDouble(7);
              p.timestamp = rs.getTimestamp(8);

              points.add(p);
          }

          return points;
      } catch (Exception e) {
          return "Error" + e.getMessage();
      }
  }

  @RequestMapping("/deletePoint/id/{id}")
  public String deletePoint(@PathVariable(value = "id") int id) {
      try (Connection connection = dataSource.getConnection()) {
          Statement s = connection.createStatement();
          s.execute("DELETE FROM points WHERE id = " + id);
      } catch (Exception e) {
          return "Error" + e.getMessage();
      }

      return "Success";
  }

  @RequestMapping("/setPointEps/id/{id}/latEps/{latEps}/longEps/{longEps}/altEps/{altEps}")
  public String setPointEps(@PathVariable(value = "id") int id,
                            @PathVariable(value = "latEps") double latEps,
                            @PathVariable(value = "longEps") double longEps,
                            @PathVariable(value = "altEps") double altEps) {

      try (Connection connection = dataSource.getConnection()) {

          PreparedStatement ps = connection.prepareStatement(
                  "UPDATE points " +
                  "SET latEps = ?, longEps = ?, altEps = ?, lastUpdate = now()" +
                  "WHERE id = ?;");
          ps.setDouble(1, latEps);
          ps.setDouble(2, longEps);
          ps.setDouble(3, altEps);
          ps.setInt(4, id);
          ps.executeUpdate();

      } catch (Exception e) {
          return "Error" + e.getMessage();
      }

      return "Success";
  }

  @RequestMapping("/")
  String index() {
    return "index";
  }

  @RequestMapping("/db")
  String db(Map<String, Object> model) {
    try (Connection connection = dataSource.getConnection()) {
      Statement stmt = connection.createStatement();
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ticks (tick timestamp)");
      stmt.executeUpdate("INSERT INTO ticks VALUES (now())");
      ResultSet rs = stmt.executeQuery("SELECT tick FROM ticks");

      ArrayList<String> output = new ArrayList<String>();
      while (rs.next()) {
        output.add("Read from DB: " + rs.getTimestamp("tick"));
      }

      model.put("records", output);
      return "db";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "error";
    }
  }

  @RequestMapping("/hello")
  public Hello hello() {
    return new Hello();
  }

  @Bean
  public DataSource dataSource() throws SQLException {
    if (dbUrl == null || dbUrl.isEmpty()) {
      return new HikariDataSource();
    } else {
      HikariConfig config = new HikariConfig();
      config.setJdbcUrl(dbUrl);
      return new HikariDataSource(config);
    }
  }

}
