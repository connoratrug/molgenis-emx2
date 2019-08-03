package org.molgenis.sql;

import org.jooq.DSLContext;
import org.molgenis.Column;
import org.molgenis.MolgenisException;
import org.molgenis.Table;

import static org.jooq.impl.DSL.*;
import static org.molgenis.Row.MOLGENISID;
import static org.molgenis.Type.MREF;
import static org.molgenis.sql.MetadataUtils.saveColumnMetadata;

/**
 * Creates a join table.
 *
 * <p>Also refTable.refColumn[] and refBackTable.refBackColumn[] columns are created
 *
 * <p>Finally, triggers take inserts/updates on refColumn[] or refBackColumn[] and put content in
 * joinTable
 *
 * <p>Note: we replace all relevant values in joinTable to allow also for changing of the refered to
 * column. This might get slow on large MREF lists (to be measured).
 */
public class MrefSqlColumn extends SqlColumn {
  private DSLContext jooq;

  public MrefSqlColumn(
      SqlTable sqlTable,
      String name,
      String refTable,
      String refColumn,
      String reverseName,
      String reverseRefColumn,
      String joinTableName) {
    super(sqlTable, name, MREF, refTable, refColumn, reverseName, reverseRefColumn, joinTableName);
    this.jooq = sqlTable.getJooq();
  }

  @Override
  public MrefSqlColumn createColumn() throws MolgenisException {

    // create nullable array columns compatible with the refs
    SqlTable otherTable = (SqlTable) getTable().getSchema().getTable(getRefTable());
    Column otherColumn = otherTable.getColumn(getRefColumn());

    jooq.alterTable(name(getTable().getSchemaName(), getTable().getName()))
        .add(field(name(getName()), SqlTypeUtils.jooqTypeOf(otherColumn).getArrayDataType()))
        .execute();
    jooq.alterTable(name(getTable().getSchemaName(), otherTable.getName()))
        .add(field(name(getReverseName()), SqlTypeUtils.jooqTypeOf(otherColumn).getArrayDataType()))
        .execute();

    // create the joinTable
    Table table = getTable().getSchema().createTable(getJoinTable());
    table.addRef(getName(), getRefTable(), getRefColumn());
    table.addRef(getReverseName(), getTable().getName(), getReverseRefColumn());

    // add the reverse column to the other table
    MrefSqlColumn reverseColumn =
        new MrefSqlColumn(
            otherTable,
            getReverseName(),
            getTable().getName(),
            getReverseRefColumn(),
            getName(),
            getRefColumn(),
            getJoinTable());
    otherTable.addMrefReverse(reverseColumn);

    // create triggers both ways
    createTriggers(jooq, table, this, reverseColumn);
    createTriggers(jooq, table, reverseColumn, this);

    // save metadata
    saveColumnMetadata(this);
    saveColumnMetadata(reverseColumn);

    return this;
  }

  private static void createTriggers(
      DSLContext jooq, Table joinTable, Column column, Column reverseColumn)
      throws MolgenisException {

    // jooq parameters
    String insertOrUpdateTrigger =
        column.getTable().getName() + "_" + column.getName() + "_UPSERT_TRIGGER";
    Column targetColumn = reverseColumn.getTable().getColumn(column.getRefColumn());

    // insert and update trigger:
    // first delete references to previous instance of this
    // and then update with current set
    jooq.execute(
        "CREATE FUNCTION {0}() RETURNS trigger AS"
            + "\n$BODY$"
            + "\nDECLARE"
            + "\n\t item {1};"
            + "\nBEGIN"
            // DELETE FROM jointable WHERE getReverseRefColumn = reverseRefColumn
            + "\n\tDELETE FROM {2} WHERE {3} = OLD.{4};"
            // foreach new.refColumn
            + "\n\tFOREACH item IN ARRAY NEW.{5}"
            + "\n\tLOOP"
            // INSERT INTO jointable(column,getReverseRefColumn) VALUES (item, NEW.reverseRefColumn)
            + "\n\t\tINSERT INTO {2} ({6},{3},{5}) VALUES (md5(random()::text || clock_timestamp()::text)::uuid, NEW.{4},item);"
            + "\n\tEND LOOP;"
            // NEW.column = NULL
            + "\n\tNEW.{5} = NULL;"
            + "\n\tRETURN NEW;"
            + "\nEND;"
            + "\n$BODY$ LANGUAGE plpgsql;",
        name(column.getTable().getSchemaName(), insertOrUpdateTrigger),
        keyword(SqlTypeUtils.getPsqlType(targetColumn)),
        table(name(joinTable.getSchema().getName(), joinTable.getName())),
        field(name(reverseColumn.getName())),
        field(name(reverseColumn.getRefColumn())),
        field(name(column.getName())),
        name(MOLGENISID));

    jooq.execute(
        "CREATE TRIGGER {0} "
            + "\n\tAFTER INSERT OR UPDATE OF {1} ON {2}"
            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {3}()",
        name(insertOrUpdateTrigger),
        name(column.getName()),
        name(column.getTable().getSchemaName(), column.getTable().getName()),
        name(column.getTable().getSchemaName(), insertOrUpdateTrigger));

    // delete trigger: delete all references to 'self'
    String deleteTrigger = column.getTable().getName() + "_" + column.getName() + "_DELETE_TRIGGER";
    jooq.execute(
        "CREATE FUNCTION {0}() RETURNS trigger AS"
            + "\n$BODY$"
            + "\nBEGIN"
            // DELETE FROM jointable WHERE getReverseRefColumn = reverseRefColumn
            + "\n\tDELETE FROM {2} WHERE {3} = OLD.{4};"
            + "\n\tRETURN OLD;"
            + "\nEND;"
            + "\n$BODY$ LANGUAGE plpgsql;",
        name(column.getTable().getSchemaName(), deleteTrigger),
        keyword(SqlTypeUtils.getPsqlType(targetColumn)),
        table(name(joinTable.getSchema().getName(), joinTable.getName())),
        field(name(reverseColumn.getName())),
        field(name(reverseColumn.getRefColumn())),
        field(name(column.getName())),
        name(MOLGENISID));

    jooq.execute(
        "CREATE TRIGGER {0} "
            + "\n\tAFTER DELETE ON {1}"
            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {2}()",
        name(deleteTrigger),
        name(column.getTable().getSchemaName(), column.getTable().getName()),
        name(column.getTable().getSchemaName(), deleteTrigger));

    // TODO: delete trigger
  }
}