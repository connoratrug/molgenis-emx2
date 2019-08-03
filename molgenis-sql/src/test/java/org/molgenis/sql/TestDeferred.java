package org.molgenis.sql;

import org.junit.Test;
import org.molgenis.*;
import org.molgenis.Row;
import org.molgenis.utils.StopWatch;

import java.util.UUID;

import static junit.framework.TestCase.fail;

public class TestDeferred {
  Database database = DatabaseFactory.getDatabase("molgenis", "molgenis");

  public TestDeferred() throws MolgenisException {}

  @Test
  public void test1() throws MolgenisException {

    StopWatch.start("test1");

    database.transaction(
        db -> {
          Schema s = db.createSchema("TestDeffered");

          Table subject = s.createTable("Subject");

          Table sample = s.createTable("Sample");
          sample.addRef("subject", "Subject");

          StopWatch.print("schema created");

          org.molgenis.Row sub1 = new Row();
          org.molgenis.Row sam1 = new Row().setUuid("subject", sub1.getMolgenisid());

          sample.insert(sam1);
          subject.insert(sub1);

          StopWatch.print("data added (in wrong dependency order, how cool is that??)");
        });
    StopWatch.print("transaction committed)");
  }

  @Test(expected = MolgenisException.class)
  public void test2() throws MolgenisException {
    // without transaction
    {
      Schema s = database.createSchema("TestDeffered2");

      Table subject = s.createTable("Subject");

      Table sample = s.createTable("Sample");
      sample.addRef("subject", "Subject");

      org.molgenis.Row sub1 = new Row();
      org.molgenis.Row sam1 = new Row().setUuid("subject", sub1.getMolgenisid());

      sample.insert(sam1);
      subject.insert(sub1);
    }
  }

  @Test
  public void test3() throws MolgenisException {
    StopWatch.start("test1");

    try {
      database.transaction(
          db -> {
            Schema s = db.createSchema("TestDeffered3");

            Table subject = s.createTable("Subject");

            Table sample = s.createTable("Sample");
            sample.addRef("subject", "Subject");

            StopWatch.print("schema created");

            org.molgenis.Row sub1 = new Row();
            org.molgenis.Row sam1 = new Row().setUuid("subject", UUID.randomUUID());
            org.molgenis.Row sam2 = new Row().setUuid("subject", UUID.randomUUID());

            sample.insert(sam1, sam2);
            subject.insert(sub1);

            StopWatch.print("data added");
          });
      StopWatch.print("transaction committed)");
      fail("should have failed on wrong fkey");
    } catch (MolgenisException e) {
      StopWatch.print("errored correctly " + e.getCause().getMessage());
    }
  }
}