package org.molgenis.emx2.io;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.emx2b.Emx3;
import org.molgenis.emx2.io.rowstore.TableStoreForXlsxFile;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.utils.StopWatch;

import java.io.File;
import java.nio.file.Path;

public class TestCohortCatalogue {

  static Database database;
  static Schema schema;

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(TestCohortCatalogue.class.getSimpleName());
  }

  @Test
  public void importTest() {
    StopWatch.print("begin");

    ClassLoader classLoader = getClass().getClassLoader();
    Path file = new File(classLoader.getResource("cohort_catalogue.xlsx").getFile()).toPath();

    TableStoreForXlsxFile store = new TableStoreForXlsxFile(file);

    SchemaMetadata cohortSchema = Emx3.readSchema(store.readTable("molgenis"));

    System.out.println(cohortSchema);

    StopWatch.print("schema loaded, now creating tables");

    database.tx(
        db -> {
          schema.merge(cohortSchema);

          StopWatch.print("creation of tables complete, now starting import data");

          for (String tableName : schema.getTableNames()) {
            if (store.containsTable(tableName))
              schema.getTable(tableName).update(store.readTable(tableName)); // actually upsert
          }

          StopWatch.print("import of data complete");
        });
  }
}