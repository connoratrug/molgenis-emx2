package org.molgenis.emx2.io.emx1;

import org.molgenis.emx2.*;
import org.molgenis.emx2.io.stores.RowStore;
import org.molgenis.emx2.utils.MolgenisException;

import java.util.ArrayList;
import java.util.List;

import static org.molgenis.emx2.ColumnType.*;

public class ConvertEmx1ToSchema {

  private ConvertEmx1ToSchema() {
    // hide constructor
  }

  public static SchemaMetadata convert(RowStore store, String packagePrefix) {
    SchemaMetadata schema = new SchemaMetadata();

    List<Entity> entities = loadTables(store, schema);
    List<Attribute> attributes = loadColumns(store, packagePrefix, schema);
    loadInheritanceRelations(packagePrefix, schema, entities);
    loadRefRelationships(packagePrefix, schema, entities, attributes);

    return schema;
  }

  private static void loadRefRelationships(
      String packagePrefix,
      SchemaMetadata schema,
      List<Entity> entities,
      List<Attribute> attributes) {
    // update refEntity
    for (Attribute attribute : attributes) {
      if (attribute.getDataType().contains("ref")
          || attribute.getDataType().contains("categorical")) {

        TableMetadata table =
            schema.getTableMetadata(getTableName(attribute.getEntity(), packagePrefix));

        if (attribute.getRefEntity() == null) {
          throw new MolgenisException(
              "missing_refentity",
              "Refentity is missing for attribute '",
              "Adding reference '"
                  + attribute.getEntity()
                  + "'.'"
                  + attribute.getName()
                  + "' failed. RefEntity was missing");
        }
        table
            .getColumn(attribute.getName())
            .setReference(getTableName(attribute.getRefEntity(), packagePrefix), null);
      }
    }
  }

  private static void loadInheritanceRelations(
      String packagePrefix, SchemaMetadata schema, List<Entity> entities) {
    int line = 2; // line 1 is header
    try {
      for (Entity entity : entities) {
        if (entity.getExtends() != null) {
          schema
              .getTableMetadata(entity.getName())
              .inherits(getTableName(entity.getExtends(), packagePrefix));
        }
        line++;
      }
    } catch (MolgenisException me) {
      throw new MolgenisException(
          me.getType(), me.getTitle(), me.getDetail() + ". See 'entities' line " + line, me);
    }
  }

  private static List<Attribute> loadColumns(
      RowStore store, String packagePrefix, SchemaMetadata schema) {
    int line = 2; // line 1 is header
    List<Attribute> attributes = new ArrayList<>();
    try {
      if (store.containsTable("attributes")) {
        for (Row row : store.read("attributes")) {
          attributes.add(new Attribute(row));
          line++;
        }
      }

      line = 2; // line 1 is header
      for (Attribute attribute : attributes) {

        // create the table
        TableMetadata table =
            schema.createTable(getTableName(attribute.getEntity(), packagePrefix));

        // create the attribute
        ColumnType type = getColumnType(attribute.getDataType());
        Column column = table.addColumn(attribute.getName(), type);
        column.setNullable(attribute.getNillable());
        column.setPrimaryKey(attribute.getIdAttribute());
        table.addColumn(column);

        line++;
      }
    } catch (MolgenisException me) {
      throw new MolgenisException(
          me.getType(), me.getTitle(), me.getDetail() + ". See 'attributes' line " + line, me);
    }
    return attributes;
  }

  private static List<Entity> loadTables(RowStore store, SchemaMetadata schema) {
    List<Entity> entities = new ArrayList<>();
    int line = 2; // line 1 is header

    try {
      if (store.containsTable("entities")) {
        for (Row row : store.read("entities")) {
          entities.add(new Entity(row));
          line++;
        }
      }

      line = 2; // line 1 is header
      for (Entity entity : entities) {
        schema.createTable(entity.getName());
        line++;
      }
    } catch (MolgenisException me) {
      throw new MolgenisException(
          me.getType(), me.getTitle(), me.getDetail() + ". See 'entities' line " + line, me);
    }
    return entities;
  }

  private static String getTableName(String fullName, String packagePrefix) {
    return fullName.replaceFirst(packagePrefix, "");
  }

  public static ColumnType getColumnType(String dataType) {
    switch (dataType) {
      case "compound": // todo
      case "string":
      case "email":
      case "enum": // todo
      case "file": // todo
      case "hyperlink":
      case "one_to_many":
        return STRING; // todo
      case "text":
      case "html":
        return TEXT;
      case "int":
      case "long":
        return INT;
      case "decimal":
        return DECIMAL;
      case "bool":
        return BOOL;
      case "date":
        return DATE;
      case "datetime":
        return DATETIME;
      case "xref":
      case "categorical":
        return REF;
      case "mref":
      case "categorical_mref":
        return REF_ARRAY; // todo: or should we use mref? but that is only in case of two sided
        // references ATM
      default:
        return STRING;
    }
  }
}