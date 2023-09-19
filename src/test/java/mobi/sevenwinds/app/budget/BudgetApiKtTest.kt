package mobi.sevenwinds.app.budget

import io.restassured.RestAssured
import kotlinx.coroutines.runBlocking
import mobi.sevenwinds.app.budget.BudgetService.addAuthor
import mobi.sevenwinds.common.ServerTest
import mobi.sevenwinds.common.jsonBody
import mobi.sevenwinds.common.toResponse
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BudgetApiKtTest : ServerTest() {
    private lateinit var author1: AuthorRecord
    private lateinit var author2: AuthorRecord
    private lateinit var author3: AuthorRecord


    @BeforeEach
    internal fun setUp() {
        runBlocking {
            author1 = addAuthor("Иван Иванович")
            author2 = addAuthor("Маруся")
            author3 = addAuthor("Федя")
            addAuthor("Стёпа")
            addAuthor("Иван Семёнович")
        }
    }

    @AfterEach
    internal fun afterDell() {
        transaction {
            BudgetTable.deleteAll()
            AuthorTable.deleteAll()
        }
    }

    @Test
    fun testBudgetPagination() {
        addRecord(BudgetRecord(2020, 5, 10, BudgetType.Приход, author1))
        addRecord(BudgetRecord(2020, 5, 5, BudgetType.Приход, author2))
        addRecord(BudgetRecord(2020, 5, 20, BudgetType.Приход, author3))
        addRecord(BudgetRecord(2020, 5, 30, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 40, BudgetType.Приход))
        addRecord(BudgetRecord(2030, 1, 1, BudgetType.Расход))

        RestAssured.given()
            .queryParam("limit", 3)
            .queryParam(
                "offset",
                1
            )//изначально делал смещение на кол-во пропущеных страниц(offset*limit), но потом вернул смещение записей что бы не запутать.
            .get("/budget/year/2020/stats")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println("${response.total} / ${response.items} / ${response.totalByType}")

                Assert.assertEquals(5, response.total) // теперь я здесь смогу проверить что учитывается только 2020 год
                Assert.assertEquals(3, response.items.size) // а здесь что с учетом 1 пропущеной странице будет 2 записи
                Assert.assertEquals(
                    60,
                    response.totalByType[BudgetType.Приход.name]
                )// здесь сравню сумму прихода 3х по убыванию после самого большего которого пропустили
            }
    }

    @Test
    fun testStatsSortOrder() {
        addRecord(BudgetRecord(2020, 5, 100, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 1, 5, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 50, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 1, 30, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 400, BudgetType.Приход))

        // expected sort order - month ascending, amount descending

        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println(response.items)

                Assert.assertEquals(30, response.items[0].amount)
                Assert.assertEquals(5, response.items[1].amount)
                Assert.assertEquals(400, response.items[2].amount)
                Assert.assertEquals(100, response.items[3].amount)
                Assert.assertEquals(50, response.items[4].amount)
            }
    }

    @Test
    fun testInvalidMonthValues() {
        RestAssured.given()
            .jsonBody(BudgetRecord(2020, -5, 5, BudgetType.Приход))
            .post("/budget/add")
            .then().statusCode(400)

        RestAssured.given()
            .jsonBody(BudgetRecord(2020, 15, 5, BudgetType.Приход))
            .post("/budget/add")
            .then().statusCode(400)
    }

    private fun addRecord(record: BudgetRecord) {
        RestAssured.given()
            .jsonBody(record)
            .post("/budget/add")
            .toResponse<BudgetRecord>().let { response ->
                Assert.assertEquals(record, response)
            }
    }

    @Test
    fun testGetAuthors() {
        val filter = "Иван"
        val response = RestAssured.given()
            .queryParam("filterName", filter)
            .get("/author/list")
            .toResponse<List<AuthorRecord>>().let { response ->
                println(response)
                Assert.assertEquals(2, response.size)
            }
    }

    @Test
    fun testBudgetWhitAuthor() {
        addRecord(BudgetRecord(2020, 1, 9, BudgetType.Приход, author1))
        addRecord(BudgetRecord(2020, 2, 2, BudgetType.Приход, author2))
        addRecord(BudgetRecord(2020, 5, 5, BudgetType.Приход))

        RestAssured.given()
            .queryParam("limit", 3)
            .queryParam("offset", 0)
            .queryParam("authorFilter", "Иван")
            .get("/budget/year/2020/stats")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println("${response.total} / ${response.items} / ${response.totalByType}")
                Assert.assertEquals(1, response.total)
                Assert.assertEquals(1, response.items.size)
                Assert.assertEquals(9, response.totalByType[BudgetType.Приход.name])
            }
    }
}
