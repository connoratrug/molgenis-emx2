package org.molgenis.beans;

import org.molgenis.Operator;
import org.molgenis.Query;
import org.molgenis.Where;

import java.util.Arrays;
import java.util.UUID;

public class WhereBean implements Where {

  private transient QueryBean query;
  private String[] path;
  private Operator op;
  private Object[] values;

  public WhereBean(Operator op) {
    this.op = op;
  }

  public WhereBean(QueryBean parent, String... path) {
    this.query = parent;
    this.path = path;
  }

  public WhereBean(Operator op, String... terms) {
    this.op = op;
    this.values = terms;
  }

  @Override
  public QueryBean eq(String... values) {
    this.values = values;
    this.op = Operator.EQ;
    return this.query;
  }

  @Override
  public QueryBean eq(Integer... values) {
    this.values = values;
    this.op = Operator.EQ;
    return this.query;
  }

  @Override
  public QueryBean eq(Double... values) {
    this.values = values;
    this.op = Operator.EQ;
    return this.query;
  }

  @Override
  public Query eq(UUID... values) {
    this.values = values;
    this.op = Operator.EQ;
    return this.query;
  }

  @Override
  public Operator getOperator() {
    return this.op;
  }

  @Override
  public String[] getPath() {
    return this.path;
  }

  @Override
  public Object[] getValues() {
    return this.values;
  }

  public String toString() {
    if (values != null)
      return String.format("%s %s %s", Arrays.toString(path), op, Arrays.toString(values));
    else return op.toString();
  }
}