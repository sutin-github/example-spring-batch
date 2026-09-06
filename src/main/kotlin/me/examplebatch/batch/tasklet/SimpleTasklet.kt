package me.examplebatch.batch.tasklet

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * シンプルな Tasklet の例。
 * - @StepScope により @Value でジョブパラメータへアクセスできます。
 * - execute() ではログ出力を行います。
 */
@Component
@StepScope // ジョブパラメータを使用するため
class SimpleTasklet : Tasklet {
    val logger: Log = LogFactory.getLog(SimpleTasklet::class.java)

    @Value("#{jobParameters[createDate]}")
    private val createDate: String? = null   // 例) プログラム引数に 'createDate=2020-10-10' のように指定します
    @Value("#{jobParameters[time]}")
    private val time: String? = null         // スケジューラで設定されます

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus? {
        logger.info("SimpleTasklet >>>>")
        logger.info("createDate : $createDate")
        logger.info("time: $time")

        return RepeatStatus.FINISHED
    }
}