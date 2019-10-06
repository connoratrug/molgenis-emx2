package org.molgenis.emx2.sql;

import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.molgenis.emx2.utils.MolgenisException;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.sql.Constants.MG_USER_PREFIX;

public class UserAwareConnectionProvider extends DataSourceConnectionProvider {
  private String activeUser;

  public UserAwareConnectionProvider(DataSource source) {
    super(source);
  }

  @Override
  public Connection acquire() throws DataAccessException {
    try {
      Connection connection = super.acquire();
      if (activeUser != null) {
        DSL.using(connection, SQLDialect.POSTGRES_10)
            .execute("SET SESSION AUTHORIZATION {0}", name(MG_USER_PREFIX + activeUser));
      }
      return connection;
    } catch (DataAccessException dae) {
      throw new MolgenisException(
          "set active user failed",
          "set active user failed",
          "active user '" + activeUser + "' failed");
    }
  }

  @Override
  public void release(Connection connection) throws DataAccessException {
    try {
      DSL.using(connection, SQLDialect.POSTGRES_10).execute("RESET SESSION AUTHORIZATION");
    } catch (DataAccessException sqle) {
      throw new RuntimeException("release of connection failed ", sqle);
    }
    super.release(connection);
  }

  public String getActiveUser() {
    return activeUser;
  }

  public void setActiveUser(String activeUser) {
    this.activeUser = activeUser;
  }

  public void clearActiveUser() {
    this.activeUser = null;
  }
}
