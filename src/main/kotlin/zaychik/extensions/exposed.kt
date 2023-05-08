package zaychik.extensions

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

suspend fun <T> Database.suspendedTransaction(
    context: CoroutineDispatcher? = Dispatchers.IO,
    transactionIsolation: Int? = null,
    statement: suspend Transaction.() -> T
): T = newSuspendedTransaction(
    context = context,
    db = this,
    transactionIsolation = transactionIsolation,
    statement = statement
)