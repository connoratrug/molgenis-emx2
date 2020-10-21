package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;

import java.io.StringWriter;
import java.util.List;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

public class TestCopy {

  static Database db;
  static Schema schema;

  @BeforeClass
  public static void setup() {
    db = TestDatabaseFactory.getTestDatabase();

    schema = db.dropCreateSchema(TestCopy.class.getSimpleName());
  }

  @Test
  public void test() {
    List<Row> rows =
        List.of(
            row("A", "a1", "B", List.of("b\"11\"", "b1,2")),
            row("A", "a2", "B", List.of("b21", "b22")));

    schema.create(table("test", column("A"), column("B").type(ColumnType.STRING_ARRAY)));

    // copyOut
    SqlTable table = (SqlTable) schema.getTable("test");
    table.insert(rows);
    StringWriter writer = new StringWriter();
    table.copyOut(writer);
    System.out.println("CopyOut");
    System.out.println(writer);

    // copyIn
    System.out.println("CopyIn");

    schema.create(table("test2", column("A")));
    table = (SqlTable) schema.getTable("test2");
    table.copyIn(rows);
    writer = new StringWriter();
    table.copyOut(writer);
    System.out.println(writer);
  }

  //  @Test
  //  public void test2() {
  //    schema.create(table("speed1", column("A")));
  //    schema.create(table("speed2", column("A")));
  //
  //    int count = 1000000;
  //    List<Row> rows = new ArrayList<>();
  //    for (int i = 0; i < count; i++) {
  //      rows.add(row("A", "a" + i));
  //    }
  //
  //    SqlTable t = (SqlTable) schema.getTable("speed1");
  //    StopWatch.start("speed1");
  //    t.insert(rows);
  //    StopWatch.print("normal", count);
  //
  //    t = (SqlTable) schema.getTable("speed2");
  //    StopWatch.start("speed2");
  //    t.copyIn(rows);
  //    StopWatch.print("copy", count);
  //  }
}