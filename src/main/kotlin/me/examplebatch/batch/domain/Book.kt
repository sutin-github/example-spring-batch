package me.examplebatch.batch.domain

import jakarta.persistence.*
import java.io.Serializable

/**
 * tbl_book テーブルの Book エンティティ。
 * 共通のタイムスタンプフィールドは Base から継承します。
 */
@Entity
@Table(name = "tbl_book")
class Book(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var bookId: Long? = null,
    var name: String? = null,
    var author: String? = null,
    var bookstoreId: Long? = null
): Base(), Serializable {
}