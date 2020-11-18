package org.molgenis.emx2;

import java.util.*;
import org.molgenis.emx2.utils.TableSort;

public class SchemaMetadata {

  protected Map<String, TableMetadata> tables = new LinkedHashMap<>();
  protected Map<String, String> settings = new LinkedHashMap<>();
  private String name;
  // optional
  private Database database;

  public SchemaMetadata() {}

  public SchemaMetadata(String name) {
    if (name == null || name.isEmpty())
      throw new MolgenisException("Create schema failed: Schema name was null or empty");
    this.name = name;
  }

  public SchemaMetadata(SchemaMetadata schema) {
    this.name = schema.getName();
    this.settings = schema.getSettings();
  }

  public SchemaMetadata(Database db, SchemaMetadata schema) {
    this(schema);
    this.database = db;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Set<String> getTableNames() {
    return this.tables.keySet();
  }

  public TableMetadata create(TableMetadata table) {
    if (tables.get(table.getTableName()) != null)
      throw new MolgenisException(
          "Create table failed: Table with name '"
              + table.getTableName()
              + "'already exists in schema '"
              + getName()
              + "'");
    this.tables.put(table.getTableName(), table);
    table.setSchema(this);
    return table;
  }

  public void create(TableMetadata... tables) {
    for (TableMetadata table : tables) {
      this.create(table);
    }
  }

  public TableMetadata getTableMetadata(String name) {
    return tables.get(name);
  }

  public void drop(String tableId) {
    tables.remove(tableId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (TableMetadata t : tables.values()) {
      sb.append(t);
    }
    return sb.toString();
  }

  public List<TableMetadata> getTables() {
    List<TableMetadata> result = new ArrayList<>();
    for (String tableName : getTableNames()) {
      result.add(getTableMetadata(tableName));
    }
    TableSort.sortTableByDependency(result);
    return result;
  }

  public Map<String, String> getSettings() {
    return this.settings;
  }

  public SchemaMetadata setSettings(Map<String, String> settings) {
    if (settings == null) return this;
    this.settings = settings;
    return this;
  }

  public Database getDatabase() {
    return database;
  }

  public void setDatabase(Database database) {
    this.database = database;
  }
}
