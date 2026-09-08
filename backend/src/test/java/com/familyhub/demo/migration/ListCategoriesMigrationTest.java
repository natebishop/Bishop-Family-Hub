package com.familyhub.demo.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListCategoriesMigrationTest {

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @BeforeAll
    static void startContainer() {
        postgres.start();
    }

    @AfterAll
    static void stopContainer() {
        postgres.stop();
    }

    // ---------------------------------------------------------------------------
    // Helper: build a Flyway instance targeting a specific schema and migration target
    // ---------------------------------------------------------------------------
    private Flyway flyway(String schema, String target) {
        return Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .schemas(schema)
                .defaultSchema(schema)
                .createSchemas(true)
                .target(target)
                .load();
    }

    private Connection connect(String schema) throws SQLException {
        Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        conn.createStatement().execute("SET search_path TO \"" + schema + "\"");
        return conn;
    }

    // ---------------------------------------------------------------------------
    // Test 1: clean schema → latest — assert all V17 artifacts exist
    // ---------------------------------------------------------------------------
    @Test
    void cleanSchema_migratesThrough_V17() throws SQLException {
        String schema = "clean_" + UUID.randomUUID().toString().replace("-", "");
        flyway(schema, "latest").migrate();

        try (Connection conn = connect(schema)) {

            // table exists
            ResultSet tableRs = conn.getMetaData().getTables(null, schema, "list_category_catalog_scope", null);
            assertThat(tableRs.next()).as("list_category_catalog_scope table should exist").isTrue();

            // required columns: id, family_id, kind
            ResultSet colRs = conn.getMetaData().getColumns(null, schema, "list_category_catalog_scope", null);
            Set<String> cols = new HashSet<>();
            while (colRs.next()) cols.add(colRs.getString("COLUMN_NAME"));
            assertThat(cols).contains("id", "family_id", "kind");

            // normalized unique index exists
            boolean indexFound = false;
            ResultSet idxRs = conn.getMetaData().getIndexInfo(null, schema, "list_category", false, false);
            while (idxRs.next()) {
                String idxName = idxRs.getString("INDEX_NAME");
                if ("uk_list_category_family_kind_normalized_name".equals(idxName)) {
                    indexFound = true;
                    break;
                }
            }
            assertThat(indexFound).as("normalized unique index should exist").isTrue();

            // seeded column still exists on list_category
            ResultSet seededRs = conn.getMetaData().getColumns(null, schema, "list_category", "seeded");
            assertThat(seededRs.next()).as("list_category.seeded column should still exist").isTrue();

            // composite uk_list_category_id_family_kind still exists (backs composite FK)
            boolean compositeUkFound = false;
            ResultSet compositeIdxRs = conn.getMetaData().getIndexInfo(null, schema, "list_category", false, false);
            while (compositeIdxRs.next()) {
                String idxName = compositeIdxRs.getString("INDEX_NAME");
                if ("uk_list_category_id_family_kind".equals(idxName)) {
                    compositeUkFound = true;
                    break;
                }
            }
            assertThat(compositeUkFound).as("uk_list_category_id_family_kind should still exist").isTrue();
        }
    }

    // ---------------------------------------------------------------------------
    // Test 2: upgrade from V16 data → V17 — rows unchanged, exactly 3 scope rows per family,
    //         and normalized unique index enforces case/whitespace uniqueness.
    // ---------------------------------------------------------------------------
    @Test
    void v16Data_upgradesTo_V17_cleanly() throws SQLException {
        String schema = "upgrade_" + UUID.randomUUID().toString().replace("-", "");

        // Migrate to V16 first
        flyway(schema, "16").migrate();

        // Insert representative V16 fixture data
        String familyId = UUID.randomUUID().toString();
        String groceryListId = UUID.randomUUID().toString();
        String todoListId = UUID.randomUUID().toString();
        String generalListId = UUID.randomUUID().toString();
        String groceryCatId = UUID.randomUUID().toString();
        String todoCatId = UUID.randomUUID().toString();

        try (Connection conn = connect(schema)) {
            conn.setAutoCommit(false);

            // family
            conn.createStatement().execute(
                    "INSERT INTO family (id, name, username, password_hash) VALUES " +
                    "('" + familyId + "', 'Test Family', 'testfamily', 'hash')");

            // list_categories (V12 only allows GROCERY, TODO)
            conn.createStatement().execute(
                    "INSERT INTO list_category (id, family_id, kind, name, seeded, sort_order) VALUES " +
                    "('" + groceryCatId + "', '" + familyId + "', 'GROCERY', 'Produce', true, 0)");
            conn.createStatement().execute(
                    "INSERT INTO list_category (id, family_id, kind, name, seeded, sort_order) VALUES " +
                    "('" + todoCatId + "', '" + familyId + "', 'TODO', 'Urgent', true, 0)");

            // shared_lists (GROCERY, TODO, GENERAL)
            conn.createStatement().execute(
                    "INSERT INTO shared_list (id, family_id, name, kind, category_display_mode) VALUES " +
                    "('" + groceryListId + "', '" + familyId + "', 'Groceries', 'GROCERY', 'GROUPED')");
            conn.createStatement().execute(
                    "INSERT INTO shared_list (id, family_id, name, kind, category_display_mode) VALUES " +
                    "('" + todoListId + "', '" + familyId + "', 'Todo', 'TODO', 'GROUPED')");
            conn.createStatement().execute(
                    "INSERT INTO shared_list (id, family_id, name, kind, category_display_mode) VALUES " +
                    "('" + generalListId + "', '" + familyId + "', 'General', 'GENERAL', 'FLAT')");

            // shared_list_items: grocery item has category, general item must have null category
            conn.createStatement().execute(
                    "INSERT INTO shared_list_item (list_id, family_id, list_kind, category_id, text) VALUES " +
                    "('" + groceryListId + "', '" + familyId + "', 'GROCERY', '" + groceryCatId + "', 'Milk')");
            conn.createStatement().execute(
                    "INSERT INTO shared_list_item (list_id, family_id, list_kind, category_id, text) VALUES " +
                    "('" + todoListId + "', '" + familyId + "', 'TODO', '" + todoCatId + "', 'Buy milk')");
            conn.createStatement().execute(
                    "INSERT INTO shared_list_item (list_id, family_id, list_kind, category_id, text) VALUES " +
                    "('" + generalListId + "', '" + familyId + "', 'GENERAL', NULL, 'A note')");

            conn.commit();
        }

        // Upgrade to latest (V17)
        flyway(schema, "latest").migrate();

        try (Connection conn = connect(schema)) {
            conn.setAutoCommit(false);

            // list_category rows unchanged
            ResultSet catRs = conn.createStatement().executeQuery(
                    "SELECT count(*) FROM list_category WHERE family_id = '" + familyId + "'");
            catRs.next();
            assertThat(catRs.getLong(1)).as("list_category row count should be unchanged").isEqualTo(2);

            // exactly 3 scope rows for this family (GROCERY, TODO, GENERAL)
            ResultSet scopeRs = conn.createStatement().executeQuery(
                    "SELECT count(*) FROM list_category_catalog_scope WHERE family_id = '" + familyId + "'");
            scopeRs.next();
            assertThat(scopeRs.getLong(1))
                    .as("exactly 3 scope rows should exist (one per kind)")
                    .isEqualTo(3);

            // all three kinds present
            ResultSet kindsRs = conn.createStatement().executeQuery(
                    "SELECT kind FROM list_category_catalog_scope " +
                    "WHERE family_id = '" + familyId + "' ORDER BY kind");
            Set<String> kinds = new HashSet<>();
            while (kindsRs.next()) kinds.add(kindsRs.getString(1));
            assertThat(kinds).containsExactlyInAnyOrder("GROCERY", "TODO", "GENERAL");

            // Insert a Documents category (GENERAL kind — allowed after V17 widens the kind check)
            String documentsCatId = UUID.randomUUID().toString();
            conn.createStatement().execute(
                    "INSERT INTO list_category (id, family_id, kind, name, seeded, sort_order) VALUES " +
                    "('" + documentsCatId + "', '" + familyId + "', 'GENERAL', 'Documents', false, 0)");
            conn.commit();

            // Exact duplicate 'documents' (lowercase) should violate normalized unique index
            assertThatThrownBy(() ->
                conn.createStatement().execute(
                    "INSERT INTO list_category (id, family_id, kind, name, seeded, sort_order) VALUES " +
                    "(gen_random_uuid(), '" + familyId + "', 'GENERAL', 'documents', false, 1)")
            ).as("duplicate 'documents' should violate normalized unique index")
             .isInstanceOf(SQLException.class);

            // Re-establish clean connection state after the exception
            conn.rollback();

            // Whitespace/case variant '  DOCUMENTS  ' should also violate the normalized unique index
            assertThatThrownBy(() ->
                conn.createStatement().execute(
                    "INSERT INTO list_category (id, family_id, kind, name, seeded, sort_order) VALUES " +
                    "(gen_random_uuid(), '" + familyId + "', 'GENERAL', '  DOCUMENTS  ', false, 2)")
            ).as("whitespace/case variant should violate normalized unique index")
             .isInstanceOf(SQLException.class);

            conn.rollback();
        }
    }

    // ---------------------------------------------------------------------------
    // Test 3: V17 guard aborts the migration when a pre-existing database already holds
    //         rows that collide under the new normalized lower(btrim(name)) key.
    //         V16's case-sensitive uniqueness lets such rows coexist; V17 must refuse
    //         to swap the constraint and instead RAISE EXCEPTION ('... duplicates exist').
    // ---------------------------------------------------------------------------
    @Test
    void v17_aborts_whenPreExistingNormalizedDuplicatesExist() throws SQLException {
        String schema = "guard_" + UUID.randomUUID().toString().replace("-", "");

        // Migrate to V16 only — its uniqueness is case/whitespace-sensitive.
        flyway(schema, "16").migrate();

        String familyId = UUID.randomUUID().toString();

        try (Connection conn = connect(schema)) {
            conn.setAutoCommit(false);

            conn.createStatement().execute(
                    "INSERT INTO family (id, name, username, password_hash) VALUES " +
                    "('" + familyId + "', 'Dup Family', 'dupfamily', 'hash')");

            // Two GROCERY categories that are DISTINCT strings ('Produce' vs 'produce')
            // but collide under lower(btrim(name)). V16's uk_list_category_family_kind_name
            // is case-sensitive, so both inserts succeed here.
            conn.createStatement().execute(
                    "INSERT INTO list_category (id, family_id, kind, name, seeded, sort_order) VALUES " +
                    "(gen_random_uuid(), '" + familyId + "', 'GROCERY', 'Produce', true, 0)");
            conn.createStatement().execute(
                    "INSERT INTO list_category (id, family_id, kind, name, seeded, sort_order) VALUES " +
                    "(gen_random_uuid(), '" + familyId + "', 'GROCERY', 'produce', true, 1)");

            conn.commit();

            // Both rows really are present under V16.
            ResultSet countRs = conn.createStatement().executeQuery(
                    "SELECT count(*) FROM list_category WHERE family_id = '" + familyId + "'");
            countRs.next();
            assertThat(countRs.getLong(1))
                    .as("V16 case-sensitive uniqueness should allow 'Produce' and 'produce' to coexist")
                    .isEqualTo(2);
        }

        // Migrating to latest must abort: the V17 DO-block guard raises before the
        // constraint swap. Flyway wraps the PostgreSQL error, so walk the message text.
        assertThatThrownBy(() -> flyway(schema, "latest").migrate())
                .as("V17 must abort the migration when pre-existing normalized duplicates exist")
                .hasStackTraceContaining("duplicates exist");

        // The migration aborted inside V17's transaction, so the scope table was never created.
        try (Connection conn = connect(schema)) {
            ResultSet tableRs = conn.getMetaData().getTables(
                    null, schema, "list_category_catalog_scope", null);
            assertThat(tableRs.next())
                    .as("aborted V17 migration must not have created list_category_catalog_scope")
                    .isFalse();
        }
    }
}
