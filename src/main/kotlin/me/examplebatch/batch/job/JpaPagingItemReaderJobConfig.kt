package me.examplebatch.batch.job

import jakarta.persistence.EntityManagerFactory
import me.examplebatch.batch.domain.Book
import me.examplebatch.batch.utils.DateUtil
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.item.ItemProcessor
import org.springframework.batch.infrastructure.item.ItemWriter
import org.springframework.batch.infrastructure.item.database.JpaPagingItemReader
import org.springframework.batch.infrastructure.item.database.builder.JpaPagingItemReaderBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * JPA のページング読み取りを示すジョブ設定。
 * - JpaPagingItemReader を使用して効率的にページング取得を行います。
 * - ジョブパラメータ `createDate` を使ってそれより古いレコードをフィルタします。
 */
@Configuration
class JpaPagingItemReaderJobConfig(
        val jobRepository: JobRepository,
        val transactionManager: PlatformTransactionManager,
        val entityManagerFactory: EntityManagerFactory
) {
    private val jpaPagingIrJobName = "JPA_PAGING_IR_JOB"
    private val jpaPagingIrJobStepName = "${jpaPagingIrJobName}_STEP"
    private val chunkSize = 10

    /**
         * JPA ページングステップを実行するジョブを構築します。
     */
    @Bean
    fun jpaPagingItemReaderJob(): Job {
        return JobBuilder(jpaPagingIrJobName, jobRepository)
                .start(jpaPagingItemReaderStep())
                .build()
    }

    /**
         * チャンク処理を使ったステップ設定。
         * リーダーは @StepScope（ジョブパラメータを受け取れる）です。
         * プロセッサは現在はパススルーのプレースホルダです。
         */
    @Bean
    fun jpaPagingItemReaderStep(): Step {
        return StepBuilder(jpaPagingIrJobStepName, jobRepository)
                .chunk<Book, Book>(chunkSize)
                .reader(jpaPagingItemReader(null))
                .processor(jpaPagingProcessor())
                .writer(jpaPagingWriter())
                .transactionManager(transactionManager)
                .build()
    }

    /**
         * @StepScope の JpaPagingItemReader。
         * ジョブパラメータ createDate（形式: yyyy-MM-dd）を受け取り、クエリの :now パラメータとして使用します。
     */
    @Bean
    @StepScope
    fun jpaPagingItemReader(@Value("#{jobParameters[createDate]}") createDate: String?): JpaPagingItemReader<Book> {
        val params = hashMapOf<String, Any>()
        val effectiveCreateDate = createDate?.takeIf { it.isNotBlank() }
            ?: SimpleDateFormat("yyyy-MM-dd").format(Date())

        // createDate が未指定のときは現在日付をデフォルト値として使い、NullPointerException を避けます。
        params["now"] = Date(DateUtil.getTimestamp(effectiveCreateDate, "yyyy-MM-dd").time)

        return JpaPagingItemReaderBuilder<Book>()
                .queryString("SELECT b FROM Book b WHERE b.createdTime < :now")
                .pageSize(chunkSize)
                .entityManagerFactory(entityManagerFactory)
                .parameterValues(params)
                .name("bookJpaPagingItemReader")
                .build()
    }

    /**
         * サンプルプロセッサ: 現在は何もせずそのまま返却します。
         * 必要に応じて Book の変換や検証を行ってください。
     */
    @Bean
    fun jpaPagingProcessor(): ItemProcessor<Book, Book> {
        return ItemProcessor {
            // 変換処理を実装するプレースホルダ
            it
        }
    }

    /**
         * シンプルなライター: 各 Book を標準出力に出力します。
         * 必要に応じてリポジトリ保存等に置き換えてください。
     */
    @Bean
    fun jpaPagingWriter(): ItemWriter<Book> {
        return ItemWriter {
            it.forEach { book ->
                println(book)
            }
        }
    }
}