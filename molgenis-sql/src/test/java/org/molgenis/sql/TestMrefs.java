package org.molgenis.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.*;
import org.molgenis.utils.StopWatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.molgenis.Type.*;

public class TestMrefs {

  static Database db;

  @BeforeClass
  public static void setup() throws MolgenisException {
    db = DatabaseFactory.getDatabase("molgenis", "molgenis");
  }

  @Test
  public void testUUID() throws MolgenisException {
    executeTest(
        UUID,
        new java.util.UUID[] {
          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce4"),
          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce5"),
          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce6")
        });
  }

  @Test
  public void testString() throws MolgenisException {
    executeTest(STRING, new String[] {"aap", "noot", "mies"});
  }

  @Test
  public void testInt() throws MolgenisException {
    executeTest(INT, new Integer[] {5, 6});
  }

  @Test
  public void testDate() throws MolgenisException {
    executeTest(DATE, new String[] {"2013-01-01", "2013-01-02", "2013-01-03"});
  }

  @Test
  public void testDateTime() throws MolgenisException {
    executeTest(
        DATETIME,
        new String[] {"2013-01-01T18:00:00", "2013-01-01T18:00:01", "2013-01-01T18:00:02"});
  }

  @Test
  public void testDecimal() throws MolgenisException {
    executeTest(DECIMAL, new Double[] {5.0, 6.0, 7.0});
  }

  @Test
  public void testText() throws MolgenisException {
    executeTest(
        TEXT,
        new String[] {
          "This is a hello world", "This is a hello back to you", "This is a hello some more"
        });
  }

  private void executeTest(Type type, Object[] values) throws MolgenisException {
    StopWatch.start("executeTest");

    Schema s = db.createSchema("TestMrefs" + type.toString().toUpperCase());

    Table a = s.createTable("A");
    String keyOfA = "AKey";
    a.addColumn(keyOfA, type);
    a.addUnique(keyOfA);

    Table b = s.createTable("B");
    String keyOfB = "BKey";
    b.addColumn(keyOfB, STRING);
    b.addUnique(keyOfB);

    StopWatch.print("schema created");

    List<Row> aRows = new ArrayList<>();
    for (Object value : values) {
      Row aRow = new Row().set(keyOfA, value);
      a.insert(aRow);
      aRows.add(aRow);
    }

    // add two sided many-to-many
    String refName = type + "refToA";
    String refReverseName = type + "refToB";
    String joinTableName = "AB";
    b.addMref(refName, "A", keyOfA, refReverseName, keyOfB, joinTableName);

    Row bRow = new Row().set(keyOfB, keyOfB + "1").set(refName, Arrays.copyOfRange(values, 1, 3));
    b.insert(bRow);

    StopWatch.print("data inserted");

    // and update
    bRow.set(refName, Arrays.copyOfRange(values, 0, 2));
    b.update(bRow);

    StopWatch.print("data updated");

    b.delete(bRow);
    for (Row aRow : aRows) {
      a.delete(aRow);
    }

    StopWatch.print("data deleted");
  }
}