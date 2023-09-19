package mobi.sevenwinds.app.budget

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object BudgetService {
    suspend fun addRecord(body: BudgetRecord): BudgetRecord = withContext(Dispatchers.IO) {
        transaction {
            val entity = BudgetEntity.new {
                this.year = body.year
                this.month = body.month
                this.amount = body.amount
                this.type = body.type
                this.author = body.author?.let { AuthorEntity[it.id] }
            }
            return@transaction entity.toResponse()
        }
    }

    suspend fun getYearStats(param: BudgetYearParam): BudgetYearStatsResponse = withContext(Dispatchers.IO) {
        transaction {
            val filteredQuery = BudgetTable
                .leftJoin(AuthorTable)
                .select {
                    (BudgetTable.year eq param.year) and
                            (param.authorFilter?.let { AuthorTable.fio.lowerCase() like "%${it.toLowerCase()}%" }
                                ?: Op.TRUE)
                }
            val total = filteredQuery.count()
            val query = filteredQuery
                .orderBy(BudgetTable.month, SortOrder.ASC)
                .orderBy(BudgetTable.amount, SortOrder.DESC)
                .limit(param.limit, param.offset)
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
                this.createdAt = System.currentTimeMillis()
            }
            return@transaction entity.toResponse()
        }
    }

    suspend fun getAuthors(filter: AuthorFilterParam): List<AuthorRecord> = withContext(Dispatchers.IO) {
        transaction {
            val query = if (filter.filterName != null) {
                AuthorTable.select { AuthorTable.fio.lowerCase() like "%${filter.filterName.toLowerCase()}%" }
            } else {
                AuthorTable.selectAll()
            }
            return@transaction AuthorEntity.wrapRows(query).map { it.toResponse() }
        }
    }
}