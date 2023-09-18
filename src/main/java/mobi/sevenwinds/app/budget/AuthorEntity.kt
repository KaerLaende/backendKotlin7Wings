package mobi.sevenwinds.app.budget
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import com.papsign.ktor.openapigen.annotations.type.number.integer.min.Min
import com.papsign.ktor.openapigen.annotations.type.string.length.Length
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import org.joda.time.DateTime

fun NormalOpenAPIRoute.author() {
    route("/author") {
        route("/add").post<Unit, AuthorRecord, AuthorRecord>(info("Добавить автора")) { _, body ->
            respond(BudgetService.addAuthor(body.fio))
        }
        route("/list").get<AuthorFilterParam, List<AuthorRecord>>(info("Получить список авторов")) { param ->
            respond(BudgetService.getAuthors(param))
        }
    }
}

class AuthorEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AuthorEntity>(AuthorTable)

    var fio by AuthorTable.fio
    var createdAt by AuthorTable.createdAt

    fun toResponse(): AuthorRecord {
        return AuthorRecord(id.value, fio, createdAt)
    }
}

data class AuthorRecord(
    val id: Int,
    @Length(min=2, max=40) var fio: String,
    val createdAt: DateTime
)
data class AuthorFilterParam(
    @QueryParam("Фильтрация по имени автора")val filter: String?)
