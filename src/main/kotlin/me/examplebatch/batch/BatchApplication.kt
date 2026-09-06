package me.examplebatch.batch

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * バッチプロジェクトのメイン Spring Boot アプリケーション。
 * - @SpringBootApplication: コンポーネントスキャンと自動設定を有効化
 * - @EnableBatchProcessing: Spring Batch の機能（JobLauncher, JobRepository 等）を有効化
 * - @EnableScheduling: スケジューリング機能を有効化（SimpleJobScheduler で使用）
 */
@SpringBootApplication
@EnableBatchProcessing
@EnableScheduling
class BatchApplication

/**
 * アプリケーションのエントリポイント。Spring Boot にコンテキストの起動を委譲します。
 * プログラム引数にジョブパラメータ（例: createDate=2020-10-10）を渡せます。
 */
fun main(args: Array<String>) {
	runApplication<BatchApplication>(*args)
}
