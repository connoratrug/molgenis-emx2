package org.molgenis.emx2;

import org.molgenis.emx2.query.Operator;

import java.io.Serializable;

public interface Where {
  Query eq(Serializable... values);

  Query contains(Serializable... values);

  Query search(String terms);

  // below move to implementation?

  Operator getOperator();

  Serializable[] getValues();

  String[] getPath();
}