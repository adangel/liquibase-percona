package liquibase.ext.percona;

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import liquibase.statement.SqlStatement;
import liquibase.statement.core.CommentStatement;
import liquibase.statement.core.RawSqlStatement;

public class PerconaRawSQLChangeTest extends AbstractPerconaChangeTest<PerconaRawSQLChange> {

    public PerconaRawSQLChangeTest() {
        super(PerconaRawSQLChange.class);
    }

    @Override
    protected void setupChange(PerconaRawSQLChange change) {
        alterText = "ADD COLUMN address VARCHAR(255) NULL";
        change.setSql("alter table person " + alterText);
    }

    @Test
    public void testGetTargetTableName() {
        PerconaRawSQLChange change = getChange();
        Assertions.assertEquals("person", change.getTargetTableName());
    }

    @Test
    public void testGenerateAlterStatement() {
        PerconaRawSQLChange change = getChange();
        Assertions.assertEquals(alterText, change.generateAlterStatement(getDatabase()));
    }

    @Test
    public void testTargetTableNameAndAlterStatementKeepCase() {
        PerconaRawSQLChange change = getChange();
        change.setSql("altEr tAble pErSoN " + alterText);
        Assertions.assertEquals("pErSoN", change.getTargetTableName());
        Assertions.assertEquals(alterText, change.generateAlterStatement(getDatabase()));
    }

    @Test
    public void testTargetTableNameAndAlterStatementWithSpaces() {
        PerconaRawSQLChange change = getChange();
        change.setSql("  altEr   tAble   pErSoN   " + alterText + "  ");
        Assertions.assertEquals("pErSoN", change.getTargetTableName());
        Assertions.assertEquals(alterText, change.generateAlterStatement(getDatabase()));
    }

    @Test
    public void testTargetTableNameAndAlterStatementWithEscapes() {
        PerconaRawSQLChange change = getChange();
        String alterTextEscaped = "ADD COLUMN `address` VARCHAR(255) NULL";
        change.setSql("altEr tAble `pErSoN` " + alterTextEscaped);
        Assertions.assertEquals("pErSoN", change.getTargetTableName());
        Assertions.assertEquals(alterTextEscaped, change.generateAlterStatement(getDatabase()));

        change.setSql("altEr tAble `my pErSoN table` " + alterTextEscaped);
        Assertions.assertNull(change.getTargetTableName());
        Assertions.assertNull(change.generateAlterStatement(getDatabase()));
    }

    @Test
    public void testTargetTableNameAndAlterStatementForInsert() {
        PerconaRawSQLChange change = getChange();
        change.setSql("insert into person (name) values ('Bob')");
        Assertions.assertNull(change.getTargetTableName());
        Assertions.assertNull(change.generateAlterStatement(getDatabase()));
    }

    @Test
    public void testGetTargetDatabaseName() {
        PerconaRawSQLChange change = getChange();
        Assertions.assertNull(change.getTargetDatabaseName());
    }

    @Test
    public void testWithoutPercona() {
        PTOnlineSchemaChangeStatement.available = false;
        SqlStatement[] statements = generateStatements();
        Assertions.assertEquals(1, statements.length);
        Assertions.assertEquals(RawSqlStatement.class, statements[0].getClass());
    }

    @Test
    public void testWithoutPerconaAndFail() {
        System.setProperty(Configuration.FAIL_IF_NO_PT, "true");
        PTOnlineSchemaChangeStatement.available = false;

        Assertions.assertThrows(RuntimeException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                generateStatements();
            }
        });
    }

    @Test
    public void testReal() {
        assertPerconaChange(alterText);
    }

    @Test
    public void testUpdateSQL() {
        enableLogging();

        SqlStatement[] statements = generateStatements();
        Assertions.assertEquals(3, statements.length);
        Assertions.assertEquals(CommentStatement.class, statements[0].getClass());
        Assertions.assertEquals("pt-online-schema-change "
                        + "--alter-foreign-keys-method=auto "
                        + "--nocheck-unique-key-change "
                        + "--alter=\"" + alterText + "\" "
                        + "--password=*** --execute "
                        + "h=localhost,P=3306,u=user,D=testdb,t=person",
                ((CommentStatement)statements[0]).getText());
        Assertions.assertEquals(CommentStatement.class, statements[1].getClass());
        Assertions.assertEquals(RawSqlStatement.class, statements[2].getClass());
    }

    @Test
    public void testUpdateSQLNoAlterSqlDryMode() {
        enableLogging();
        System.setProperty(Configuration.NO_ALTER_SQL_DRY_MODE, "true");

        SqlStatement[] statements = generateStatements();
        Assertions.assertEquals(1, statements.length);
        Assertions.assertEquals(CommentStatement.class, statements[0].getClass());
        Assertions.assertEquals("pt-online-schema-change "
                        + "--alter-foreign-keys-method=auto "
                        + "--nocheck-unique-key-change "
                        + "--alter=\"" + alterText + "\" "
                        + "--password=*** --execute "
                        + "h=localhost,P=3306,u=user,D=testdb,t=person",
                ((CommentStatement)statements[0]).getText());
    }

    @Test
    public void testSkipRawSQLChange() {
        System.setProperty(Configuration.SKIP_CHANGES, "sql");
        SqlStatement[] statements = generateStatements();
        Assertions.assertEquals(1, statements.length);
        Assertions.assertEquals(RawSqlStatement.class, statements[0].getClass());
    }
}
