package me.examplebatch.batch.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Book エンティティ用の Spring Data JPA リポジトリ。
 * JpaRepository により CRUD やページングが利用可能です。
 */
@Repository
interface BookRepository: JpaRepository<Book, Long>{
}