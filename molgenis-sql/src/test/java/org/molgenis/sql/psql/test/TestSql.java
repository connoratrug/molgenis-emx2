package org.molgenis.sql.psql.test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.jooq.ForeignKey;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.sql.*;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.molgenis.sql.SqlRow.MOLGENISID;
import static org.molgenis.sql.SqlType.*;

public class TestSql {

  private static SqlDatabaseImpl db = null;
  private static HikariDataSource dataSource = null;

  @BeforeClass
  public static void setUp() {
    Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    rootLogger.setLevel(Level.INFO);

    String userName = "molgenis";
    String password = "molgenis";
    String url = "jdbc:postgresql:molgenis";

    try {
      dataSource = new HikariDataSource();
      dataSource.setJdbcUrl(url);
      dataSource.setUsername(userName);
      dataSource.setPassword(password);

      Connection conn = dataSource.getConnection();

      // internal magic
      DSLContext context = DSL.using(conn, SQLDialect.POSTGRES_10);

      // delete all foreign key constaints
      for (Table t : context.meta().getTables()) {
        for (ForeignKey k : (List<ForeignKey>) t.getReferences()) {
          context.alterTable(t).dropConstraint(k.getName()).execute();
        }
      }
      // delete all tables
      for (Table t : context.meta().getTables()) {
        context.dropTable(t).execute();
      }

      conn.close();

      db = new SqlDatabaseImpl(dataSource);
    }

    // For the sake of this test, let's keep exception handling simple
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testBatch() throws SqlDatabaseException {
    SqlTable t = db.createTable("TestBatch");
    t.addColumn("test", STRING);
    t.addColumn("testint", INT);

    long startTime = System.currentTimeMillis();

    int size = 1000;

    List<SqlRow> rows = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      SqlRow r = new SqlRow();
      r.setString("test", "test" + i);
      r.setInt("testint", i);
      rows.add(r);
    }
    long endTime = System.currentTimeMillis();
    System.out.println(
        "Generated " + size + " test record in " + (endTime - startTime) + " milliseconds");

    startTime = System.currentTimeMillis();
    t.insert(rows.subList(0, 100));
    endTime = System.currentTimeMillis();
    System.out.println("First batch insert " + (endTime - startTime) + " milliseconds");

    startTime = System.currentTimeMillis();
    t.insert(rows.subList(100, 200));
    endTime = System.currentTimeMillis();
    System.out.println("Second batch insert " + (endTime - startTime) + " milliseconds");

    startTime = System.currentTimeMillis();
    t.insert(rows.subList(200, 300));
    endTime = System.currentTimeMillis();
    System.out.println("Third batch insert " + (endTime - startTime) + " milliseconds");

    startTime = System.currentTimeMillis();
    for (SqlRow r : rows) {
      r.setString("test", r.getString("test") + "_updated");
    }
    t.update(rows);
    endTime = System.currentTimeMillis();
    System.out.println("Batch update " + (endTime - startTime) + " milliseconds");

    startTime = System.currentTimeMillis();
    for (SqlRow r : db.query("TestBatch").retrieve()) {
      System.out.println(r);
    }
    endTime = System.currentTimeMillis();
    System.out.println("Retrieve " + (endTime - startTime) + " milliseconds");
  }

  @Test
  public void testTypes() throws SqlDatabaseException {

    // generate TypeTest table, with columns for each type
    SqlTable t = db.createTable("TypeTest");
    for (SqlType type : SqlType.values()) {
      if (REF.equals(type)) {
        t.addRef("Test_" + type.toString().toLowerCase() + "_nillable", t).setNullable(true);
      } else if (MREF.equals(type)) {
        // cannot set nullable
      } else {
        t.addColumn("Test_" + type.toString().toLowerCase(), type);
        t.addColumn("Test_" + type.toString().toLowerCase() + "_nillable", type).setNullable(true);
      }
    }

    // retrieve this table from metadataa
    SqlTable t2 = db.getTable("TypeTest");
    System.out.println(t2);

    // check nullable ok
    SqlRow row = new SqlRow();
    row.setUuid("Test_uuid", java.util.UUID.randomUUID());
    row.setString("Test_string", "test");
    row.setBool("Test_bool", true);
    row.setInt("Test_int", 1);
    row.setDecimal("Test_decimal", 1.1);
    row.setText("Test_text", "testtext");
    row.setDate("Test_date", LocalDate.of(2018, 12, 13));
    row.setDateTime("Test_datetime", OffsetDateTime.of(2018, 12, 13, 12, 40, 0, 0, ZoneOffset.UTC));
    t2.insert(row);

    // check not null expects exception
    row = new SqlRow();
    row.setUuid("Test_uuid_nillable", java.util.UUID.randomUUID());
    row.setString("Test_string_nillable", "test");
    row.setBool("Test_bool_nillable", true);
    row.setInt("Test_int_nillable", 1);
    row.setDecimal("Test_decimal_nillable", 1.1);
    row.setText("Test_text_nillable", "testtext");
    row.setDate("Test_date_nillable", LocalDate.of(2018, 12, 13));
    row.setDateTime(
        "Test_datetime_nillable", OffsetDateTime.of(2018, 12, 13, 12, 40, 0, 0, ZoneOffset.UTC));
    try {
      t2.insert(row);
      fail(); // should not reach this one
    } catch (SqlDatabaseException e) {
      System.out.println("as expected, caught exceptoin: " + e.getMessage());
    }

    // check query and test getters
    List<SqlRow> result = db.query("TypeTest").retrieve();
    for (SqlRow res : result) {
      System.out.println(res);
      res.setRowID(java.util.UUID.randomUUID());
      assert (res.getDate("Test_date") instanceof LocalDate);
      assert (res.getDateTime("Test_datetime") instanceof OffsetDateTime);
      assert (res.getString("Test_string") instanceof String);
      assert (res.getInt("Test_int") instanceof Integer);
      assert (res.getDecimal("Test_decimal") instanceof Double);
      assert (res.getText("Test_text") instanceof String);
      assert (res.getBool("Test_bool") instanceof Boolean);
      assert (res.getUuid("Test_uuid") instanceof java.util.UUID);

      t2.insert(res);
    }

    System.out.println("testing TypeTest query");
    for (SqlRow r : db.query("TypeTest").retrieve()) {
      System.out.println(r);
    }
  }

  @Test
  public void testCreate() throws SqlDatabaseException {

    long startTime = System.currentTimeMillis();

    // create a fromTable
    SqlTable t = db.createTable("Person");
    t.addColumn("First Name", STRING);
    t.addRef("Father", t).setNullable(true);
    t.addColumn("Last Name", STRING);
    t.addUnique("First Name", "Last Name");
    long endTime = System.currentTimeMillis();

    System.out.println(
        "Created fromTable: \n" + t.toString() + " in " + (endTime - startTime) + " milliseconds");

    // reinitialise database to see if it can recreate from background
    db = new SqlDatabaseImpl(dataSource);
    assertEquals(10, db.getTables().size());

    // insert
    startTime = System.currentTimeMillis();
    SqlTable t2 = db.getTable("Person");
    List<SqlRow> rows = new ArrayList<>();
    int count = 1000;
    for (int i = 0; i < count; i++) {
      rows.add(new SqlRow().setString("Last Name", "Duck" + i).setString("First Name", "Donald"));
    }
    t.insert(rows);
    endTime = System.currentTimeMillis();
    long total = (endTime - startTime);
    System.out.println(
        "Insert of "
            + count
            + " records took "
            + total
            + " milliseconds (that is "
            + (1000 * count / total)
            + " rows/sec)");

    // query
    startTime = System.currentTimeMillis();
    SqlQuery q = db.query("Person");
    for (SqlRow row : q.retrieve()) {
      // System.out.println("Query result: " + row);
    }
    endTime = System.currentTimeMillis();
    System.out.println("Query took " + (endTime - startTime) + " milliseconds");
    System.out.println("Query contents " + q);

    // delete
    startTime = System.currentTimeMillis();
    t.delete(rows);
    endTime = System.currentTimeMillis();
    total = (endTime - startTime);
    System.out.println(
        "Delete took " + total + " milliseconds (that is " + (1000 * count / total) + " rows/sec)");

    assertEquals(0, db.query("Person").retrieve().size());

    assertEquals(2, t.getUniques().size());
    try {
      t.removeUnique(MOLGENISID);
      fail("you shouldn't be allowed to remove primary key unique constraint");
    } catch (Exception e) {
      // good stuff
    }
    t.removeUnique("Last Name", "First Name");
    assertEquals(1, t.getUniques().size());

    assertEquals(4, t.getColumns().size());
    try {
      t.removeColumn(MOLGENISID);
      fail("you shouldn't be allowed to remove primary key column");
    } catch (Exception e) {
      // good stuff
    }
    t.removeColumn("Father");
    assertEquals(3, t.getColumns().size());

    // drop a fromTable
    db.dropTable(t.getName());
    assertEquals(null, db.getTable("Person"));
    // make sure nothing was left behind in backend
    db = new SqlDatabaseImpl(dataSource);
    assertEquals(null, db.getTable("Person"));
  }

  @Test
  public void testQuery() throws SqlDatabaseException {

    long startTime = System.currentTimeMillis();

    SqlTable part = db.createTable("Part");
    part.addColumn("name", STRING);
    part.addColumn("weight", INT);
    part.addUnique("name");

    SqlRow part1 = new SqlRow().setString("name", "forms").setInt("weight", 100);
    SqlRow part2 = new SqlRow().setString("name", "login").setInt("weight", 50);
    part.insert(part1);
    part.insert(part2);

    SqlTable component = db.createTable("Component");
    component.addColumn("name", STRING);
    component.addUnique("name");
    component.addMref("parts", part, "partOfComponent");

    SqlRow component1 = new SqlRow().setString("name", "explorer").setMref("parts", part1, part2);
    SqlRow component2 = new SqlRow().setString("name", "navigator").setMref("parts", part2);
    component.insert(component1);
    component.insert(component2);

    SqlTable product = db.createTable("Product");
    product.addColumn("name", STRING);
    product.addUnique("name");
    product.addMref("components", component, "partOfProduct");

    SqlRow product1 =
        new SqlRow().setString("name", "molgenis").setMref("components", component1, component2);
    product.insert(product1);

    long endTime = System.currentTimeMillis();
    System.out.println("Creation took " + (endTime - startTime) + " milliseconds");

    // now getQuery to show product.name and parts.name linked by path Assembly.product,part

    // needed:
    // join+columns paths, potentially multiple paths. We only support outer join over relationships
    // if names are not unique, require explicit select naming
    // complex nested where clauses
    // sortby clauses
    // later: group by.

    //        SqlQueryImpl q1 = new PsqlQueryBack(db);
    // db        q1.select("Product").columns("name").as("productName");
    //        q1.mref("ProductComponent").columns("name").as("componentName");
    //        q1.mref("ComponentPart").columns("name").as("partName");
    //        //q1.where("productName").eq("molgenis");
    //
    //        System.out.println(q1);

    try {
      db.query("pietje");
      fail("exception handling from(pietje) failed");
    } catch (Exception e) {
      System.out.println("Succesfully caught exception: " + e);
    }

    try {
      db.query("Product").as("p").join("Comp", "p", "components");
      fail("should fail because faulty table");
    } catch (Exception e) {
      System.out.println("Succesfully caught exception: " + e);
    }

    try {
      db.query("Product").as("p").join("Component", "p2", "components");
      fail("should fail because faulty toTabel");
    } catch (Exception e) {
      System.out.println("Succesfully caught exception: " + e);
    }

    try {
      db.query("Product").as("p").join("Component", "p2", "components");
      fail("should fail because faulty on although it is an mref");
    } catch (Exception e) {
      System.out.println("Succesfully caught exception: " + e);
    }

    try {
      db.query("Product").as("p").join("Component", "p", "comps");
      fail("should fail because faulty on");
    } catch (Exception e) {
      System.out.println("Succesfully caught exception: " + e);
    }

    try {
      db.query("Product").as("p").select("wrongname").as("productName");
      fail("should fail because faulty 'select'");
    } catch (Exception e) {
      System.out.println("Succesfully caught exception: " + e);
    }

    startTime = System.currentTimeMillis();
    SqlQuery q2 = db.query("Product").as("p").select("name").as("productName");
    q2.join("Component", "p", "components").as("c").select("name").as("componentName");
    q2.join("Part", "c", "parts").as("pt").select("name").as("partName").select("weight");
    q2.eq("pt", "weight", 50).eq("c", "name", "explorer", "navigator");
    for (SqlRow row : q2.retrieve()) {
      System.out.println(row);
    }
    endTime = System.currentTimeMillis();
    System.out.println("Query took " + (endTime - startTime) + " milliseconds");
    System.out.println("Query contents: " + q2);

    // again reversing the 'on' columns
    SqlQuery q3 = db.query("Product").as("p").select("name").as("productName");
    q3.join("Component", "p", "partOfProduct").as("c").select("name").as("componentName");
    q3.join("Part", "c", "partOfComponent").as("pt").select("name").as("partName").select("weight");
    q3.eq("pt", "weight", 50).eq("c", "name", "explorer", "navigator");
    for (SqlRow row : q3.retrieve()) {
      System.out.println(row);
    }

    // test delete
    product.delete(product1);
    assertEquals(0, db.query("Product").retrieve().size());
  }

  @AfterClass
  public static void close() {
    db.close();
  }
}