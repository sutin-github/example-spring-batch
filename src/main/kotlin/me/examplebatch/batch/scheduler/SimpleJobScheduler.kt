package me.examplebatch.batch.scheduler

import me.examplebatch.batch.job.SimpleJobConfig
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException
import org.springframework.batch.core.launch.JobOperator
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.text.SimpleDateFormat
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Simple ジョブを定期実行で起動するスケジューラ。
 * - 設定 'schedule.active' が true のときのみ有効になります。
 * - 実行ごとにタイムスタンプを JobParameters に設定し、一意な実行にします。
 */
@Component
@ConditionalOnProperty(prefix = "schedule", name = ["active"], havingValue = "true")
class SimpleJobScheduler(
    val jobOperator: JobOperator,
    val simpleJobConfig: SimpleJobConfig
) {
    private val logger: Log = LogFactory.getLog(SimpleJobScheduler::class.java)
    private val running = AtomicBoolean(false)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

    @Scheduled(initialDelay = 10000, fixedDelay = 30000)
    fun runJob() {
        if (!running.compareAndSet(false, true)) {
            logger.warn("SimpleJobScheduler skipped execution because the previous job is still running.")
            return
        }

        try {
            val jobParameters = JobParametersBuilder()
                .addString("time", dateFormat.format(System.currentTimeMillis()))
                .toJobParameters()

            jobOperator.start(simpleJobConfig.simpleJob(), jobParameters)
        } catch (e: JobExecutionAlreadyRunningException) {
            logger.warn("Simple job is already running. Skip scheduling.")
        } catch (e: JobInstanceAlreadyCompleteException) {
            logger.warn("Simple job instance already completed. Skip scheduling.")
        } catch (e: Exception) {
            logger.error("Failed to start simple job.", e)
        } finally {
            running.set(false)
        }
    }
}