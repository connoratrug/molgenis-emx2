package org.molgenis.bean;

import org.molgenis.Column;
import org.molgenis.DatabaseException;
import org.molgenis.Table;
import org.molgenis.Unique;

import static org.molgenis.Row.MOLGENISID;

import java.util.*;

public class TableBean implements Table {
  private String name;
  private String extend;
  protected Map<String, Column> columns = new LinkedHashMap<>();
  private Map<String, Unique> uniques = new LinkedHashMap<>();

  public TableBean(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Collection<Column> getColumns() {
    return Collections.unmodifiableCollection(columns.values());
  }

  @Override
  public Column getColumn(String name) {
    return columns.get(name);
  }

  @Override
  public Column addColumn(String name, Column.Type type) throws DatabaseException {
    Column c = new ColumnBean(this, name, type);
    columns.put(name, c);
    return c;
  }

  @Override
  public Column addRef(String name, Table otherTable) throws DatabaseException {
    Column c = new ColumnBean(this, name, otherTable);
    columns.put(name, c);
    return c;
  }

  @Override
  public Column addMref(String name, Table otherTable, String joinTable) throws DatabaseException {
    Column c = new ColumnBean(this, name, otherTable, joinTable);
    columns.put(name, c);
    return c;
  }

  @Override
  public void removeColumn(String name) throws DatabaseException {
    columns.remove(name);
  }

  @Override
  public Collection<Unique> getUniques() {
    return Collections.unmodifiableCollection(uniques.values());
  }

  @Override
  public Unique addUnique(String... names) throws DatabaseException {
    List<Column> cols = new ArrayList<>();
    for (String name : names) {
      Column c = getColumn(name);
      if (c == null) throw new DatabaseException("Unique unknown: " + names);
      cols.add(c);
    }
    String uniqueName = name + "_" + String.join("_", names) + "_UNIQUE";
    Unique u = new UniqueBean(this, cols);
    uniques.put(uniqueName, u);
    return u;
  }

  @Override
  public boolean isUnique(String... names) {
    try {
      getUniqueName(names);
      return true;
    } catch (DatabaseException e) {
      return false;
    }
  }

  public String getUniqueName(String... keys) throws DatabaseException {
    List<String> keyList = Arrays.asList(keys);
    for (Map.Entry<String, Unique> el : this.uniques.entrySet()) {
      if (el.getValue().getColumns().size() == keyList.size()
          && el.getValue().getColumnNames().containsAll(keyList)) {
        return el.getKey();
      }
    }
    throw new DatabaseException(
        "getUniqueName(" + keyList + ") failed: constraint unknown in table " + this.name);
  }

  @Override
  public void removeUnique(String... keys) throws DatabaseException {
    if (keys.length == 1 && MOLGENISID.equals(keys[0]))
      throw new DatabaseException(
          "You are not allowed to remove unique constraint on primary key column " + MOLGENISID);
    String uniqueName = getUniqueName(keys);
    uniques.remove(uniqueName);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("TABLE(").append(getName()).append("){");
    for (Column c : getColumns()) {
      builder.append("\n\t").append(c.toString());
    }
    for (Unique u : getUniques()) {
      builder.append("\n\t").append(u.toString());
    }
    builder.append("\n}");
    return builder.toString();
  }

  @Override
  public String getExtend() {
    return extend;
  }

  @Override
  public void setExtend(String extend) {
    this.extend = extend;
  }
}
