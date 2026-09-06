package me.examplebatch.batch.job

import me.examplebatch.batch.type.JsonItem
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.item.json.JacksonJsonObjectReader
import org.springframework.batch.infrastructure.item.json.JsonItemReader
import org.springframework.batch.infrastructure.item.json.builder.JsonItemReaderBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.transaction.PlatformTransactionManager

/**
 * ClassPath 上の JSON を読み取るジョブ設定。
 * Spring Batch の JsonItemReader を使って JsonItem を生成し、各レコードを出力します。
 */
@Configuration
class JsonFileJobConfig(
    val jobRepository: JobRepository,
    val transactionManager: PlatformTransactionManager
) {
    private val jsonFileJobName = "JSON_FILE_JOB"
    private val jsonFileJobStepName = "${jsonFileJobName}STEP"
    private val chunkSize = 5

    /**
         * 単一ステップで JSON アイテムを処理するジョブです。
     */
    @Bean
    fun jsonFileJob(): Job{
        return JobBuilder(jsonFileJobName, jobRepository)
            .start(jsonFileStep())
            .build()
    }

    /**
         * JsonItem をチャンク単位で読み取り、標準出力へ書き出すステップ。
         * - chunk サイズはトランザクション境界とコミット頻度を制御します。
     */
    @Bean
    fun jsonFileStep(): Step{
        return StepBuilder(jsonFileJobStepName, jobRepository)
            .chunk<JsonItem, JsonItem>(chunkSize)
            .reader(jsonItemReader())
            .writer { jsonItems ->
                // チャンク単位のアイテムリストを受け取り、各要素を出力する
                jsonItems.forEach { println(it.toString()) }
            }
            .transactionManager(transactionManager)
            .build()
    }

    /**
         * JacksonJsonObjectReader を使って JSON を JsonItem にマッピングする JsonItemReader。
         * @StepScope を付けているため、将来的にステップ・ジョブスコープのパラメータへアクセスできます。
         */
    @Bean
    @StepScope
    fun jsonItemReader(): JsonItemReader<JsonItem> {
        return JsonItemReaderBuilder<JsonItem>()
            .jsonObjectReader(JacksonJsonObjectReader(JsonItem::class.java))
            .resource(ClassPathResource("/sample/json/test.json"))
            .name("jsonItemReader")
            .build()
    }
}