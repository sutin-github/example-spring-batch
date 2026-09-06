package me.examplebatch.batch.job

import me.examplebatch.batch.domain.Book
import me.examplebatch.batch.item.CustomItemReader
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.item.ItemProcessor
import org.springframework.batch.infrastructure.item.ItemWriter
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

/**
 * Blaze-Persistence を使うカスタム ItemReader ベースのジョブ設定。
 * - リーダーは Bean（@StepScope）として提供し、CustomItemReader が Blaze で Book をページング取得します。
 * - JpaPagingItemReader と同じく、chunk 単位で読み取りと加工を行います。
 * - プロセッサは author フィールドを加工する例です。
 * - ライターは BookRepository を使って永続化します。
 */
@Configuration
class CustomReaderJobConfig(
        val jobRepository: JobRepository,
        val transactionManager: PlatformTransactionManager,
        val dataSource: DataSource
) {
    private val customReaderJobName = "CUSTOM_READER_JOB"
    private val customReaderJobStepName = "${customReaderJobName}_STEP"
    private val chunkSize = 10

    @Bean
    fun customReaderJob(): Job{
        return JobBuilder(customReaderJobName, jobRepository)
                .start(customReaderStep())
                .build()
    }

    @Bean
    fun customReaderStep(): Step {
        return StepBuilder(customReaderJobStepName, jobRepository)
                .chunk<Book, Book>(chunkSize)
                .reader(reader())
                .processor(processor())
                .writer(writer())
                .transactionManager(transactionManager)
                .build()
    }

    /**
     * JpaPagingItemReader と同等の設計で、pageSize を固定値として持つ Reader を生成します。
     * - pageSize は chunk サイズに合わせて 10 として扱います
     * - @StepScope により、ステップごとに新しい Reader を生成できます
     */
    @Bean
    @StepScope
    fun reader(): CustomItemReader{
       return CustomItemReader(pageSize = chunkSize)
    }

    /**
         * 例: author フィールドに "Author. " を付加するプロセッサ。
     */
    @Bean
    fun processor(): ItemProcessor<Book, Book>{
        return ItemProcessor {
           val author = it.author ?: "unknown"
           it.author = if (author.startsWith("Author. ")) author else "Author. $author"
           it
       }
    }

    /**
     * JdbcBatchItemWriter に置き換えることで、chunk 単位で SQL バッチ書き込みを行う。
     * - JPA の persistence context を経由しないため、書き込みオーバーヘッドが小さくなる
     * - PostgreSQL では insert のバッチ処理が安定しやすく、本番向けの大量データ処理に適する
     * - 重複実行時の二重 insert を抑制するため、同一キー候補に対して WHERE NOT EXISTS を付与する
     */
    @Bean
    fun writer(): ItemWriter<Book> {
       return JdbcBatchItemWriterBuilder<Book>()
           .dataSource(dataSource)
           .sql(
               """
               INSERT INTO tbl_book (name, author, bookstore_id)
               SELECT :name, :author, :bookstoreId
               WHERE NOT EXISTS (
                   SELECT 1
                   FROM tbl_book existing
                   WHERE (existing.name = :name OR (existing.name IS NULL AND :name IS NULL))
                     AND (existing.author = :author OR (existing.author IS NULL AND :author IS NULL))
                     AND (existing.bookstore_id = :bookstoreId OR (existing.bookstore_id IS NULL AND :bookstoreId IS NULL))
               )
               """.trimIndent()
           )
           .beanMapped()
           .build()
    }
}