package com.fitai.app.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.fitai.app.db.FitAiDatabase

class IosDatabaseDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver =
        NativeSqliteDriver(FitAiDatabase.Schema, "fitai.db")
}
