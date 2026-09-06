package me.examplebatch.batch.type

import java.io.Serializable

/**
 * sample/json/test.json の JSON オブジェクトにマッピングされるデータクラス。
 * 入力の欠損に対応するためフィールドは nullable にしています。
 */
data class JsonItem(
    var number: Int? = null,
    var data: String? = null
): Serializable
