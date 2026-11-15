package com.fitai.app.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.fitai.app.db.FitAiDatabase

class AndroidDatabaseDriverFactory(
    private val context: Context,
) : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver =
        AndroidSqliteDriver(FitAiDatabase.Schema, context, "fitai.db")
}
