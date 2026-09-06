package me.examplebatch.batch.item

import com.blazebit.persistence.CriteriaBuilderFactory
import me.examplebatch.batch.domain.Book
import jakarta.persistence.EntityManager
import org.springframework.batch.infrastructure.item.ItemReader
import org.springframework.beans.factory.annotation.Autowired

/**
 * Blaze-Persistence を使って Book をページング取得する ItemReader。
 * JpaPagingItemReader と同じく、offset / maxResults を用いて一度に一定件数だけ読み込む。
 * 全件をメモリに抱え込まず、必要なページのみ順次処理するので大量データでも安定します。
 *
 * 注意: @StepScope のプロキシ生成のため、クラスは open にしておく必要があります。
 */
open class CustomItemReader(
    private val pageSize: Int = 10
) : ItemReader<Book> {
    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var criteriaBuilderFactory: CriteriaBuilderFactory

    private var currentPage: MutableList<Book> = mutableListOf()
    private var currentIndex: Int = 0
    private var offset: Int = 0
    private var initialized: Boolean = false

    private fun loadNextPage(): MutableList<Book> {
        val criteriaBuilder = criteriaBuilderFactory.create(entityManager, Book::class.java)
        val query = criteriaBuilder.getQuery()
        query.firstResult = offset
        query.maxResults = pageSize

        val result = query.resultList
        currentPage = result.toMutableList()
        currentIndex = 0
        offset += currentPage.size
        return currentPage
    }

    override fun read(): Book? {
        if (!initialized) {
            currentPage = loadNextPage()
            initialized = true
            if (currentPage.isEmpty()) {
                return null
            }
        }

        if (currentIndex >= currentPage.size) {
            currentPage = loadNextPage()
            if (currentPage.isEmpty()) {
                return null
            }
            currentIndex = 0
        }
        return currentPage[currentIndex++]
    }
}