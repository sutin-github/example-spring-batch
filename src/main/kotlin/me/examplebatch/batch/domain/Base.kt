package me.examplebatch.batch.domain

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import java.sql.Timestamp

/**
 * エンティティ共通のタイムスタンプフィールドを提供するマップドスーパークラス。
 * - createdTime: 永続化前に一度設定されます
 * - modifyTime: 更新前に設定されます
 */
@MappedSuperclass
abstract class Base {
    @Column(updatable = false)
    lateinit var createdTime: Timestamp
    lateinit var modifyTime: Timestamp

    @PrePersist
    fun onCreate(){
        this.createdTime = Timestamp(System.currentTimeMillis())
    }

    @PreUpdate
    fun onUpdate(){
        this.modifyTime = Timestamp(System.currentTimeMillis())
    }
}