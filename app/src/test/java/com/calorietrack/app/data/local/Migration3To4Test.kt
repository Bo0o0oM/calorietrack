package com.calorietrack.app.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

/**
 * Validates the upgrade migration path from Version 3 (Milestone 2D)
 * to Version 4 (Milestone 2I) and data preservation.
 */
class Migration3To4Test {

  @Test
  fun migration3To4_executesAlterStatementForIsActiveColumn() {
    val executedSqls = mutableListOf<String>()

    val dbHandler = InvocationHandler { _, method, args ->
      when (method.name) {
        "execSQL" -> {
          executedSqls.add(args[0] as String)
          null
        }
        else -> null
      }
    }

    val fakeDb = Proxy.newProxyInstance(
      SupportSQLiteDatabase::class.java.classLoader,
      arrayOf(SupportSQLiteDatabase::class.java),
      dbHandler
    ) as SupportSQLiteDatabase

    val migration = CalorieTrackDatabase.Migration3To4()
    migration.migrate(fakeDb)

    assertEquals(1, executedSqls.size)
    assertTrue(
      "Migration must add is_active column with default 1",
      executedSqls[0].contains("ALTER TABLE foods ADD COLUMN is_active INTEGER NOT NULL DEFAULT 1")
    )
  }
}
