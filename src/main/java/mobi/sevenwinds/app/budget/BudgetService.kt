package mobi.sevenwinds.app.budget

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mobi.sevenwinds.app.budget.AuthorTable.createdAt
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.LocalDateTime.now


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
            val data = toBudgetRecordResponse(query)
            val sumByType = data.groupBy { it.type.name }.mapValues { it.value.sumOf { v -> v.amount } }
            return@transaction BudgetYearStatsResponse(
                total = total,
                totalByType = sumByType,
                items = data
            )
        }
    }

    private fun toBudgetRecordResponse(query: Query): List<Budget> {
        return query.mapNotNull { row ->
            val authorId = row[BudgetTable.author]?.value
            if (authorId != null) {
                BudgetRecordWithAuthor(
                    year = row[BudgetTable.year],
                    month = row[BudgetTable.month],
                    amount = row[BudgetTable.amount],
                    type = row[BudgetTable.type],
                    author = row[AuthorTable.fio],
                    createdAt = row[AuthorTable.createdAt]
                )
            } else {
                BudgetRecord(
                    year = row[BudgetTable.year],
                    month = row[BudgetTable.month],
                    amount = row[BudgetTable.amount],
                    type = row[BudgetTable.type],
                    authorId = null
                )
            }
        }
    }

    suspend fun addAuthor(fio: String): AuthorRecord = withContext(Dispatchers.IO) {
        transaction {
            val entity = AuthorEntity.new {
                this.fio = fio
                this.createdAt = now().toDateTime()
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