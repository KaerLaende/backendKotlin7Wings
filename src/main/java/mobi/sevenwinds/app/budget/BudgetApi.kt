package mobi.sevenwinds.app.budget

import com.fasterxml.jackson.annotation.JsonIgnore
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import com.papsign.ktor.openapigen.annotations.type.number.integer.max.Max
import com.papsign.ktor.openapigen.annotations.type.number.integer.min.Min
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import org.joda.time.LocalDateTime

fun NormalOpenAPIRoute.budget() {
    route("/budget") {
        route("/add").post<Unit, BudgetRecord, BudgetRecord>(info("Добавить запись")) { _, body ->
            respond(BudgetService.addRecord(body))
        }

        route("/year/{year}/stats") {
            get<BudgetYearParam, BudgetYearStatsResponse>(info("Получить статистику за год")) { param ->
                respond(BudgetService.getYearStats(param))
            }
        }
    }
}

open class Budget(
    @Min(1900)open val year: Int,
    @Min(1) @Max(12)open val month: Int,
    @Min(1) open val amount: Int,
    open val type: BudgetType,
    @JsonIgnore open val authorId: Int? = null
)

data class BudgetRecord(
    override val year: Int,
    override val month: Int,
    override val amount: Int,
    override val type: BudgetType,
    @QueryParam("ID автора") override val authorId: Int? = null
) : Budget(year, month, amount, type)

data class BudgetRecordWithAuthor(
    @Min(1900) override val year: Int,
    @Min(1) @Max(12) override val month: Int,
    @Min(1) override val amount: Int,
    override val type: BudgetType,
    val author: String?,
    val createdAt: LocalDateTime
) : Budget(year, month, amount, type)
data class BudgetYearParam(
    @PathParam("Год") val year: Int,
    @QueryParam("Лимит пагинации") val limit: Int,
    @QueryParam("Смещение пагинации") val offset: Int,
    @QueryParam("Фильтр по ФИО автора") val authorFilter: String? = null
)

class BudgetYearStatsResponse(
    val total: Int,
    val totalByType: Map<String, Int>,
    val items: List<Budget>
)


enum class BudgetType {
    Приход, Расход, Комиссия
}