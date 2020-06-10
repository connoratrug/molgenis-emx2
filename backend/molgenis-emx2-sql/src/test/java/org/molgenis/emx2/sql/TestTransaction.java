package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;

import java.sql.SQLException;

import static org.junit.Assert.assertNull;
import static org.molgenis.emx2.TableMetadata.table;

public class TestTransaction {
  private static Database db;

  @BeforeClass
  public static void setUp() throws SQLException {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testTransaction() {
    Schema s = db.dropCreateSchema("testTransaction");

    // as long as from same db instance you can use resources in multiple Tx
    try {
      db.tx(
          db -> {
            s.create(table("a"));
            s.create(table("b"));
            throw new RuntimeException("transaction stopped to check if it rolled back");
          });
    } catch (Exception e) {
      System.out.println(e);
      assertNull(s.getTable("a"));
    }
  }
}
