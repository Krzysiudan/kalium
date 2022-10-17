@file:Suppress("MatchingDeclarationName")

package com.wire.kalium.persistence.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.wire.kalium.persistence.UserDatabase
import com.wire.kalium.persistence.dao.UserIDEntity
import kotlinx.coroutines.CoroutineDispatcher
import java.io.File
import java.util.Properties

private const val DATABASE_NAME = "main.db"

internal actual class PlatformDatabaseData(
    val storePath: File?
)

fun UserDatabaseProvider(
    userId: UserIDEntity,
    storePath: File,
    dispatcher: CoroutineDispatcher
): UserDatabaseProvider {
    val databasePath = storePath.resolve(DATABASE_NAME)
    val databaseExists = databasePath.exists()

    // Make sure all intermediate directories exist
    storePath.mkdirs()

    val driver: SqlDriver = sqlDriver("jdbc:sqlite:${databasePath.absolutePath}")

    if (!databaseExists) {
        UserDatabase.Schema.create(driver)
    }
    return UserDatabaseProvider(userId, driver, dispatcher, PlatformDatabaseData(storePath))
}

private fun sqlDriver(driverUri: String): SqlDriver = JdbcSqliteDriver(
    driverUri,
    Properties(1).apply { put("foreign_keys", "true") }
)

fun inMemoryDatabase(userId: UserIDEntity, dispatcher: CoroutineDispatcher): UserDatabaseProvider {
    val driver = sqlDriver(JdbcSqliteDriver.IN_MEMORY)
    UserDatabase.Schema.create(driver)
    return UserDatabaseProvider(userId, driver, dispatcher, PlatformDatabaseData(File("inMemory")))
}

internal actual fun nuke(
    userId: UserIDEntity,
    database: UserDatabase,
    platformDatabaseData: PlatformDatabaseData
): Boolean = platformDatabaseData.storePath?.resolve(DATABASE_NAME)?.delete() ?: false
