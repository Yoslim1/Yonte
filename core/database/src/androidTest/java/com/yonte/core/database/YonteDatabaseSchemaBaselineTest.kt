package com.yonte.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves that Room can load the committed v1 schema asset to create a historical database.
 * The manual FTS5 table is intentionally outside Room's schema and is not asserted here.
 */
@RunWith(AndroidJUnit4::class)
class YonteDatabaseSchemaBaselineTest {

    @get:Rule
    val migrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        YonteDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @After
    fun deleteTestDatabase() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(TEST_DATABASE_NAME)
    }

    @Test
    fun committedVersion1SchemaCanCreateAndValidateDatabase() {
        migrationTestHelper.createDatabase(TEST_DATABASE_NAME, VERSION_1).apply {
            execSQL(
                """
                INSERT INTO notes (
                    id,
                    title,
                    body,
                    normalizedText,
                    isPinned,
                    isArchived,
                    isTrashed,
                    createdAt,
                    updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(
                    "schema-v1-note",
                    "Schema baseline",
                    "v1 data",
                    "schema baseline v1 data",
                    0,
                    0,
                    0,
                    1L,
                    1L,
                ),
            )
            close()
        }

        val database = migrationTestHelper.runMigrationsAndValidate(
            TEST_DATABASE_NAME,
            VERSION_1,
            true,
        )
        try {
            database.query("SELECT id FROM notes").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("schema-v1-note", cursor.getString(0))
            }
        } finally {
            database.close()
        }
    }

    private companion object {
        const val TEST_DATABASE_NAME = "yonte-schema-baseline-test"
        const val VERSION_1 = 1
    }
}
