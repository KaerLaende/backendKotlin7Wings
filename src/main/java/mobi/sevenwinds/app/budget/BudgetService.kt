package mobi.sevenwinds.app.budget

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import org.joda.time.DateTime.now


object BudgetService {
    suspend fun addRecord(body: BudgetRecord): BudgetRecord = withContext(Dispatchers.IO) {
        transaction {
            val entity = BudgetEntity.new {
                this.year = body.year
                this.month = body.month
                this.amount = body.amount
                this.type = body.type
                this.author = body.authorId?.let { AuthorEntity[it] }
            }
            return@transaction entity.toResponse()
        }
    }
    suspend fun getYearStats(param: BudgetYearParam): BudgetYearStatsResponse = withContext(Dispatchers.IO) {
        transaction {
            val query = BudgetTable
                .leftJoin(AuthorTable)
                .select {
                    (BudgetTable.year eq param.year) and
                            (param.authorFilter?.let { AuthorTable.fio.lowerCase() like "%${it.toLowerCase()}%" } ?: Op.TRUE)
                }
                .orderBy(BudgetTable.month, SortOrder.ASC)
                .orderBy(BudgetTable.amount, SortOrder.DESC)
                .limit(param.limit, (param.offset * param.limit))
            val total = BudgetTable.select { BudgetTable.year eq param.year }
                .count()
            val data = BudgetEntity.wrapRows(query).map { it.toResponse() }
            val sumByType = data.groupBy { it.type.name }.mapValues { it.value.sumOf { v -> v.amount } }
            return@transaction BudgetYearStatsResponse(
                total = total,
                totalByType = sumByType,
                items = data
            )
        }
    }
    suspend fun addAuthor(fio: String): AuthorRecord = withContext(Dispatchers.IO) {
        transaction {
            val entity = AuthorEntity.new {
                this.fio = fio
                this.createdAt = now()
            }
            return@transaction entity.toResponse()
        }
    }
    suspend fun getAuthors(filter: AuthorFilterParam): List<AuthorRecord> = withContext(Dispatchers.IO) {
        transaction {
            val query = if (filter != null) {
                AuthorTable.select { AuthorTable.fio.lowerCase() like "%${filter.filter?.toLowerCase()}%" }
            } else {
                AuthorTable.selectAll()
            }
            return@transaction AuthorEntity.wrapRows(query).map { it.toResponse() }
        }
    }
}