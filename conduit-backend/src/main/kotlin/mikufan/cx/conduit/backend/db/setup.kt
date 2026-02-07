package mikufan.cx.conduit.backend.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import mikufan.cx.conduit.backend.config.DbConfig
import org.jetbrains.exposed.v1.jdbc.Database

fun creatDataSource(dbConfig: DbConfig): HikariDataSource {
  val config = HikariConfig().apply {
    jdbcUrl = dbConfig.url
    username = dbConfig.user
    password = dbConfig.password
    driverClassName = dbConfig.driver
  }
  return HikariDataSource(config)
}

fun createExposedDb(dataSource: HikariDataSource) : Database = Database.connect(dataSource)

fun createConduitTransactionManager(db: Database) : TransactionManager = TransactionManagerImpl(db)
