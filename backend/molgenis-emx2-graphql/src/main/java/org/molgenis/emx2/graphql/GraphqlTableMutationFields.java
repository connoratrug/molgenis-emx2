package org.molgenis.emx2.graphql;

import graphql.Scalars;
import graphql.schema.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.TypeUtils;

import java.util.List;
import java.util.Map;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.Status.SUCCESS;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.typeForMutationResult;

class GraphqlTableMutationFields {

  private enum MutationType {
    INSERT,
    UPDATE,
    DELETE
  }

  private GraphqlTableMutationFields() {
    // hide
  }

  public static GraphQLFieldDefinition insertMutation(Schema schema) {
    GraphQLFieldDefinition.Builder fieldBuilder =
        GraphQLFieldDefinition.newFieldDefinition()
            .name("insert")
            .type(typeForMutationResult)
            .dataFetcher(fetcher(schema, MutationType.INSERT));

    for (String tableName : schema.getTableNames()) {
      Table table = schema.getTable(tableName);
      fieldBuilder.argument(
          GraphQLArgument.newArgument()
              .name(tableName)
              .type(GraphQLList.list(rowInputType(table))));
    }
    return fieldBuilder.build();
  }

  public static GraphQLFieldDefinition updateMutation(Schema schema) {
    GraphQLFieldDefinition.Builder fieldBuilder =
        GraphQLFieldDefinition.newFieldDefinition()
            .name("update")
            .type(typeForMutationResult)
            .dataFetcher(fetcher(schema, MutationType.UPDATE));

    for (String tableName : schema.getTableNames()) {
      Table table = schema.getTable(tableName);
      fieldBuilder.argument(
          GraphQLArgument.newArgument()
              .name(tableName)
              // reuse same input as insert
              .type(GraphQLList.list(GraphQLTypeReference.typeRef(table.getName() + "Input"))));
    }
    return fieldBuilder.build();
  }

  public static GraphQLFieldDefinition deleteMutation(Schema schema) {
    GraphQLFieldDefinition.Builder fieldBuilder =
        GraphQLFieldDefinition.newFieldDefinition()
            .name("delete")
            .type(typeForMutationResult)
            .dataFetcher(fetcher(schema, MutationType.DELETE));

    for (String tableName : schema.getTableNames()) {
      // if no pkey is provided, you cannot delete rows
      if (schema.getMetadata().getTableMetadata(tableName).getPrimaryKey() != null
          && schema.getMetadata().getTableMetadata(tableName).getPrimaryKey().length > 0) {
        fieldBuilder.argument(
            GraphQLArgument.newArgument()
                .name(tableName)
                // reuse same input as insert
                .type(GraphQLList.list(GraphQLTypeReference.typeRef(tableName + "Input"))));
      }
    }
    return fieldBuilder.build();
  }

  private static DataFetcher fetcher(Schema schema, MutationType mutationType) {
    return dataFetchingEnvironment -> {
      StringBuilder result = new StringBuilder();
      boolean any = false;
      for (String tableName : schema.getTableNames()) {
        List<Map<String, Object>> rowsAslistOfMaps = dataFetchingEnvironment.getArgument(tableName);
        if (rowsAslistOfMaps != null) {
          Table table = schema.getTable(tableName);
          int count = 0;
          switch (mutationType) {
            case UPDATE:
              count = table.update(GraphqlApiFactory.convertToRows(rowsAslistOfMaps));
              result.append("updated " + count + " records to " + tableName + "\n");
              break;
            case INSERT:
              count = table.insert(GraphqlApiFactory.convertToRows(rowsAslistOfMaps));
              result.append("inserted " + count + " records to " + tableName + "\n");
              break;
            case DELETE:
              count = table.delete(GraphqlApiFactory.convertToRows(rowsAslistOfMaps));
              result.append("delete " + count + " records from " + tableName + "\n");
              break;
          }
          any = true;
        }
      }
      if (!any) throw new MolgenisException("Error with save", "no data provided");
      return new GraphqlApiMutationResult(SUCCESS, result.toString());
    };
  }

  private static GraphQLInputObjectType rowInputType(Table table) {
    GraphQLInputObjectType.Builder inputBuilder =
        GraphQLInputObjectType.newInputObject().name(table.getName() + "Input");
    for (Column col : table.getMetadata().getColumns()) {
      ColumnType columnType = TypeUtils.getPrimitiveColumnType(col);
      GraphQLInputType type = getGraphQLInputType(columnType);
      // if (col.isPrimaryKey() || !col.isNullable() && !REFBACK.equals(columnType)) {
      // type = GraphQLNonNull.nonNull(type);
      // }
      inputBuilder.field(
          GraphQLInputObjectField.newInputObjectField().name(col.getName()).type(type));
    }
    return inputBuilder.build();
  }

  private static GraphQLInputType getGraphQLInputType(ColumnType columnType) {
    switch (columnType) {
      case BOOL:
        return Scalars.GraphQLBoolean;
      case INT:
        return Scalars.GraphQLInt;
      case DECIMAL:
        return Scalars.GraphQLFloat;
      case UUID:
      case STRING:
      case TEXT:
      case DATE:
      case DATETIME:
        return Scalars.GraphQLString;
      case BOOL_ARRAY:
        return GraphQLList.list(Scalars.GraphQLBoolean);
      case INT_ARRAY:
        return GraphQLList.list(Scalars.GraphQLInt);
      case DECIMAL_ARRAY:
        return GraphQLList.list(Scalars.GraphQLFloat);
      case STRING_ARRAY:
      case TEXT_ARRAY:
      case DATE_ARRAY:
      case DATETIME_ARRAY:
      case UUID_ARRAY:
        return GraphQLList.list(Scalars.GraphQLString);
      default:
        throw new MolgenisException(
            "Internal error", "Type " + columnType + " not expected at this place");
    }
  }
}