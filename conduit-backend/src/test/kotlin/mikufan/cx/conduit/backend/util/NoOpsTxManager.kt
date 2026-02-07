package mikufan.cx.conduit.backend.util

import io.mockk.mockk
import mikufan.cx.conduit.backend.db.TransactionManager
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

/**
 * A transaction manager that does not actually perform any transactions.
 * Useful for testing purposes.
 *
 * However, make sure the test code does not call any functions from [JdbcTransaction] directly.
 */
object NoOpsTxManager : TransactionManager {
  override fun <T> tx(block: JdbcTransaction.() -> T): T = mockk<JdbcTransaction>().block()
}