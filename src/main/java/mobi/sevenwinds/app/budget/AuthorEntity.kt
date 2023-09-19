package mobi.sevenwinds.app.budget

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import com.papsign.ktor.openapigen.annotations.type.string.length.Length
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset


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
        return AuthorRecord(id.value, fio,  LocalDateTime.ofInstant(Instant.ofEpochMilli(createdAt), ZoneOffset.UTC))
    }
}

object AuthorTable : IntIdTable() {
    val fio = varchar("fio", 50)
    val createdAt = long("created_at")

    init {
        createdAt.index()
    }
}

data class AuthorRecord(
    val id: Int,
    @Length(min = 2, max = 50) var fio: String,
    val createdAt: LocalDateTime
)

data class AuthorFilterParam(
    @QueryParam("Фильтрация по имени автора") val filterName: String?
)