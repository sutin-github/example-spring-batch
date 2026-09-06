package me.examplebatch.batch.job

import me.examplebatch.batch.tasklet.SimpleTasklet
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

/**
 * シンプルなジョブ設定（複数ステップの例）。
 * - simpleStep1: インライン tasklet
 * - simpleStep2: 再利用可能な SimpleTasklet を実行
 */
@Configuration
class SimpleJobConfig(
    val jobRepository: JobRepository,
    val transactionManager: PlatformTransactionManager,
    val simpleTasklet: SimpleTasklet
) {
    private val logger: Log = LogFactory.getLog(SimpleJobConfig::class.java)

    // Job/Step 名（JobRepository に登録するための識別子）
    private val simpleJobName = "SIMPLE_JOB"
    private val simpleJobStep1Name = "SIMPLE_JOB_STEP_1"
    private val simpleJobStep2Name = "SIMPLE_JOB_STEP_2"

    /**
     * ステップを連結してジョブを構築します: step1 -> step2
     */
    @Bean
    fun simpleJob(): Job {
        return JobBuilder(simpleJobName, jobRepository)
            .start(simpleStep1())
            .next(simpleStep2())
            .build()
    }

    /**
     * インライン tasklet のステップ: メッセージをログに出力して即終了します。
     * 短い一回限りの処理に適しています。
     */
    @Bean
    fun simpleStep1(): Step{
        return StepBuilder(simpleJobStep1Name, jobRepository)
            .tasklet({ _, _ ->
                logger.info("SimpleStep1 >>>>>")
                RepeatStatus.FINISHED
            }, transactionManager)
            .build()
    }

    /**
     * Spring 管理の Tasklet Bean（SimpleTasklet）を再利用します。
     * 実際の処理は注入された tasklet 実装に委譲されます。
     */
    @Bean
    fun simpleStep2(): Step{
        return StepBuilder(simpleJobStep2Name, jobRepository)
            .tasklet(simpleTasklet, transactionManager)
            .build()
    }
}