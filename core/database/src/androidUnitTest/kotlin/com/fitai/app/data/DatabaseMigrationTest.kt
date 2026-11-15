package com.fitai.app.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.fitai.app.db.FitAiDatabase
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Upgrades a real version-1 database (the `1.db` snapshot SQLDelight generated from the schema before
 * migrations existed, i.e. what installed builds have) and checks that rows survive, the new columns and
 * tables work, and the result matches a fresh install of the current schema.
 */
class DatabaseMigrationTest {

    private val drivers = mutableListOf<SqlDriver>()
    private val files = mutableListOf<File>()

    @AfterTest
    fun tearDown() {
        drivers.forEach { it.close() }
        files.forEach { it.delete() }
    }

    @Test
    fun currentSchemaIsVersionTwo() {
        assertEquals(2L, FitAiDatabase.Schema.version)
    }

    @Test
    fun upgradeFromVersionOneKeepsRowsAndAddsTheNewColumns() {
        val driver = openSnapshot(1)
        seedVersionOneRows(driver)

        FitAiDatabase.Schema.migrate(driver, 1, FitAiDatabase.Schema.version)
        val db = FitAiDatabase(driver)

        val user = db.usersQueries.selectById("u1").executeAsOne()
        assertEquals("Ada", user.name)
        assertEquals("vegan", user.diet_restrictions)

        val plan = db.nutritionQueries.selectActiveDiet("u1").executeAsOne()
        assertEquals(2100L, plan.daily_calories)
        assertNull(plan.meals_json)

        val meals = db.nutritionQueries.selectMealsForUser("u1").executeAsList()
        assertEquals(listOf("m2", "m1"), meals.map { it.id })
        val photo = meals.first { it.id == "m1" }
        assertEquals("Mercimek çorbası", photo.meal_name)
        assertEquals(22.5, photo.protein_g)
        assertEquals("analyzed_photo", photo.photo_url)
        assertNull(photo.slot)
        assertNull(photo.food_id)
        assertNull(photo.grams)

        // Workout history is untouched.
        assertEquals(1, db.workoutQueries.selectSessionsForUser("u1").executeAsList().size)
        assertEquals(1, db.progressQueries.selectMeasurementsForUser("u1").executeAsList().size)

        // New columns and tables are usable right away.
        db.nutritionQueries.insertMealLog(
            "m3", "u1", "Beyaz peynir", 79, 4.5, 0.8, 6.5, null, "2026-11-15T08:00:00Z",
            "breakfast", "cheese_white", "slice", 1.0, 30.0,
        )
        db.nutritionQueries.updateDietPlanMeals("[]", plan.id)
        db.foodQueries.insertCustomFood(
            "custom_0123456789abcdef", "u1", "Annemin keki", 380.0, 6.0, 50.0, 17.0, "slice", 60.0,
            "2026-11-15T08:00:00Z", "2026-11-15T08:00:00Z",
        )
        val logged = db.nutritionQueries.selectMealLogById("m3").executeAsOne()
        assertEquals("breakfast", logged.slot)
        assertEquals(30.0, logged.grams)
        assertEquals(listOf("cheese_white"), db.nutritionQueries.selectRecentFoodIds("u1", 10).executeAsList().map { it.food_id })
        assertEquals("[]", db.nutritionQueries.selectDietPlanById(plan.id).executeAsOne().meals_json)
        assertEquals(1, db.foodQueries.selectCustomFoodsForUser("u1").executeAsList().size)
    }

    @Test
    fun migratedSchemaMatchesAFreshInstall() {
        val migrated = openSnapshot(1)
        FitAiDatabase.Schema.migrate(migrated, 1, FitAiDatabase.Schema.version)
        val fresh = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also(drivers::add)
        FitAiDatabase.Schema.create(fresh)

        assertEquals(describe(fresh), describe(migrated))
    }

    @Test
    fun latestSnapshotMatchesTheCurrentSchema() {
        // Guards against forgetting generateCommonMainFitAiDatabaseSchema after a schema change.
        val snapshot = openSnapshot(FitAiDatabase.Schema.version.toInt())
        val fresh = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also(drivers::add)
        FitAiDatabase.Schema.create(fresh)
        assertEquals(describe(fresh), describe(snapshot))
    }

    private fun openSnapshot(version: Int): SqlDriver {
        val source = File("src/commonMain/sqldelight/databases/$version.db")
        assertTrue(source.exists(), "missing schema snapshot ${source.path}")
        val copy = File.createTempFile("fitai-v$version-", ".db").also(files::add)
        source.copyTo(copy, overwrite = true)
        return JdbcSqliteDriver("jdbc:sqlite:${copy.absolutePath}").also(drivers::add)
    }

    private fun seedVersionOneRows(driver: SqlDriver) {
        listOf(
            "INSERT INTO users VALUES ('u1','Ada','ada@example.com',31,168.0,64.5,'build_muscle','moderate'," +
                "'dumbbells','vegan','2026-11-01T10:00:00Z',NULL,'2026-11-01T10:00:00Z')",
            "INSERT INTO diet_plans VALUES ('d1','u1',2100,130,240,70,'2026-11-10T08:00:00Z',1)",
            "INSERT INTO meal_logs VALUES ('m1','u1','Mercimek çorbası',420,22.5,50.0,12.0,'analyzed_photo'," +
                "'2026-11-14T11:00:00Z')",
            "INSERT INTO meal_logs VALUES ('m2','u1','Ayran',72,3.4,5.0,3.6,NULL,'2026-11-14T12:00:00Z')",
            "INSERT INTO workout_plans VALUES ('p1','u1','Plan','desc',3,'2026-11-10T08:00:00Z',1)",
            "INSERT INTO workout_sessions VALUES ('s1','p1','2026-11-14T06:00:00Z','2026-11-14T06:50:00Z',50,NULL)",
            "INSERT INTO body_measurements VALUES ('w1','u1',64.5,NULL,NULL,70.0,NULL,NULL,NULL,'2026-11-14T06:00:00Z')",
        ).forEach { driver.execute(null, it, 0) }
    }

    /** Tables with their columns (name, type, not null, default, pk) and indexes with their columns. */
    private fun describe(driver: SqlDriver): Map<String, List<String>> {
        val objects = query(driver, "SELECT type, name FROM sqlite_master WHERE name NOT LIKE 'sqlite_%' ORDER BY name") {
            listOf(it.getString(0)!!, it.getString(1)!!)
        }
        return objects.associate { (type, name) ->
            val details = when (type) {
                "table" -> query(driver, "PRAGMA table_info('$name')") { c ->
                    listOf(c.getString(1), c.getString(2), c.getLong(3), c.getString(4), c.getLong(5)).joinToString("|")
                }
                "index" -> query(driver, "PRAGMA index_xinfo('$name')") { c -> "${c.getString(2)}|${c.getLong(3)}|${c.getLong(5)}" }
                else -> emptyList()
            }
            "$type:$name" to details
        }
    }

    private fun <T> query(driver: SqlDriver, sql: String, map: (app.cash.sqldelight.db.SqlCursor) -> T): List<T> =
        driver.executeQuery(null, sql, { cursor ->
            val rows = mutableListOf<T>()
            while (cursor.next().value) rows += map(cursor)
            QueryResult.Value(rows)
        }, 0).value
}
