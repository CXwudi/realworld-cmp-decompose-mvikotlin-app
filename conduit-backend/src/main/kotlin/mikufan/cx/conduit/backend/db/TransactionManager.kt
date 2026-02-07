package mikufan.cx.conduit.backend.db

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

interface TransactionManager {
  fun <T> tx(block: JdbcTransaction.() -> T): T
}

class TransactionManagerImpl(
  val db: Database
) : TransactionManager {
  override fun <T> tx(block: JdbcTransaction.() -> T): T = transaction(db) {
    block()
  }
}