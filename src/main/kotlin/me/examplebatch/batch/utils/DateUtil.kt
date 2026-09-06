package me.examplebatch.batch.utils

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import java.sql.Timestamp
import java.text.SimpleDateFormat

/**
 * 日付文字列を Timestamp に変換するユーティリティ。
 * 変換ロジックを集中管理し、失敗時はログを残します。
 */
open class DateUtil {
    companion object {
        private val logger: Log = LogFactory.getLog(DateUtil::class.java)

        /**
         * Get Timestamp from string
         *
         * @param dateStr String
         * @param format String (ex.yyyy-MM-dd HH:mm:ss)
         * @return timestamp Timestamp
         */
        fun getTimestamp(dateStr: String, format: String): Timestamp {
            try{
                val simpleDateFormat = SimpleDateFormat(format)
                val date = simpleDateFormat.parse(dateStr)
                return Timestamp(date.time)
            } catch(e: Exception){
                logger.error(e.localizedMessage)
                throw e
            }
        }
    }
}