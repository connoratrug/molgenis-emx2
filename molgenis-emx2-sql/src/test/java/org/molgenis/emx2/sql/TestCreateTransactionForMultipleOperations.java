package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.MolgenisException;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

public class TestCreateTransactionForMultipleOperations {
  private static Database db;

  @BeforeClass
  public static void setUp() throws SQLException {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testCommit() {

    db.tx(
        db -> {
          Schema schema = db.createSchema("testCommit");
          Table testTable =
              schema.create(table("testCommit").addColumn(column("ColA")).addUnique("ColA"));
          testTable.insert(new Row().setString("ColA", "test"));
          testTable.insert(new Row().setString("ColA", "DependencyOrderOutsideTransactionFails"));
        });
    db.clearCache();
    assertEquals(2, db.getSchema("testCommit").getTable("testCommit").getRows().size());
  }

  @Test(expected = MolgenisException.class)
  public void testRollBack() {
    db.tx(
        db -> {
          Schema schema = db.createSchema("testRollBack");
          Table testTable =
              schema.create(table("testRollBack").addColumn(column("ColA")).addUnique("ColA"));
          Row r = new Row().setString("ColA", "test");
          testTable.insert(r);
          testTable.insert(r);
        });
  }
}
