package org.molgenis.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.*;
import org.molgenis.Row;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.molgenis.Type.STRING;

public class TestTransaction {
  private static Database db;

  @BeforeClass
  public static void setUp() throws MolgenisException, SQLException {
    db = DatabaseFactory.getDatabase("molgenis", "molgenis");
  }

  @Test
  public void testCommit() throws MolgenisException {

    Schema schema = db.createSchema("testCommit"); // not transactional in jooq :-(

    db.transaction(
        db -> {
          Schema s = db.getSchema("testCommit");
          Table t = s.createTable("testCommit");
          t.addColumn("ColA", STRING);
          t.addUnique("ColA");
          t.insert(new Row().setString("ColA", "test"));
          t.insert(new Row().setString("ColA", "test2"));
        });
    db.clearCache();
    assertEquals(2, db.getSchema("testCommit").getTable("testCommit").retrieve().size());
  }

  @Test(expected = MolgenisException.class)
  public void testRollBack() throws MolgenisException {
    db.transaction(
        db -> {
          Schema s = db.createSchema("testRollBack");
          Table t = s.createTable("testRollBack");
          t.addColumn("ColA", STRING);
          t.addUnique("ColA");

          org.molgenis.Row r = new Row().setString("ColA", "test");
          t.insert(r);
          t.insert(r);
        });
  }
}