# バッチ処理フロー（JOB ごとのシーケンス図）

このファイルでは、各ジョブを独立した PlantUML シーケンス図としてまとめています。

## 1. SIMPLE_JOB
処理概要: シンプルな Tasklet ベースのジョブ。2つのステップを順に実行し、ログ出力と処理終了を確認する。

```plantuml
@startuml
skinparam shadowing false
skinparam NoteBackgroundColor #F5F5F5
skinparam NoteBorderColor #666666
skinparam ParticipantBackgroundColor #EAF4FF
skinparam ParticipantBorderColor #4A90E2

actor Scheduler
participant JobOperator
participant "SimpleJobConfig" as SimpleJobConfig
participant "simpleStep1" as Step1
participant "simpleStep2" as Step2
participant "SimpleTasklet" as Tasklet

rnote over SimpleJobConfig
  役割: ジョブ定義とステップ構成の管理
endrnote
rnote over Step1
  役割: 先頭ステップ。ログ出力後に終了
endrnote
rnote over Step2
  役割: 実際の処理を Tasklet に委譲
endrnote
rnote over Tasklet
  役割: jobParameters を読み取り処理を実行
endrnote

Scheduler -> JobOperator: start(simpleJob, JobParameters)
JobOperator -> SimpleJobConfig: simpleJob()
SimpleJobConfig -> Step1: start()
Step1 -> Step1: logger.info("SimpleStep1 >>>>>")
Step1 --> SimpleJobConfig: RepeatStatus.FINISHED
SimpleJobConfig -> Step2: next(simpleStep2())
Step2 -> Tasklet: execute()
Tasklet --> Step2: RepeatStatus.FINISHED
Step2 --> SimpleJobConfig: 完了
SimpleJobConfig --> JobOperator: Job 終了
@enduml
```

## 2. JSON_FILE_JOB
処理概要: ClassPath 上の JSON ファイルを読み込み、JsonItem を生成してチャンク単位で標準出力へ出力するジョブ。

```plantuml
@startuml
skinparam shadowing false
skinparam NoteBackgroundColor #F5F5F5
skinparam NoteBorderColor #666666
skinparam ParticipantBackgroundColor #EAF4FF
skinparam ParticipantBorderColor #4A90E2

actor Scheduler
participant JobOperator
participant "JsonFileJobConfig" as JsonJob
participant "jsonFileStep" as JsonStep
participant "JsonItemReader" as Reader
participant "sample/json/test.json" as JsonFile
participant "Writer" as Writer

rnote over JsonJob
  役割: JSON ファイルを読んで Step を定義
endrnote
rnote over JsonStep
  役割: chunk 単位で JSON を読み込み、書き出し処理を行う
endrnote
rnote over Reader
  役割: JSON を JsonItem に変換して 1 件ずつ返す
endrnote
rnote over Writer
  役割: 読み込んだ Item を標準出力へ出力
endrnote

Scheduler -> JobOperator: start(jsonFileJob, JobParameters)
JobOperator -> JsonJob: jsonFileJob()
JsonJob -> JsonStep: start()
JsonStep -> Reader: read()
Reader -> JsonFile: JSON を読み込む
JsonFile --> Reader: JsonItem
Reader --> JsonStep: 1件返却
JsonStep -> Writer: writer(chunk)
Writer -> Writer: println(item)
Writer --> JsonStep: 書き込み完了
JsonStep --> JsonJob: Step 完了
JsonJob --> JobOperator: Job 完了
@enduml
```

## 3. JPA_PAGING_IR_JOB
処理概要: JPA の JpaPagingItemReader を使って Book をページング取得し、条件付きで古いデータを読み出すジョブ。

```plantuml
@startuml
skinparam shadowing false
skinparam NoteBackgroundColor #F5F5F5
skinparam NoteBorderColor #666666
skinparam ParticipantBackgroundColor #EAF4FF
skinparam ParticipantBorderColor #4A90E2

actor Scheduler
participant JobOperator
participant "JpaPagingItemReaderJobConfig" as JpaJob
participant "jpaPagingItemReaderStep" as JpaStep
participant "JpaPagingItemReader" as Reader
participant "EntityManagerFactory" as EMF
participant "ItemProcessor" as Processor
participant "ItemWriter" as Writer

rnote over JpaJob
  役割: JPA ページング取得用の Step を定義
endrnote
rnote over JpaStep
  役割: chunk 単位で処理を分割し、ページング取得を実行
endrnote
rnote over Reader
  役割: createDate 以前の Book をページ単位で取得
endrnote
rnote over Processor
  役割: 取得した Book を処理前後で変換・検証
endrnote
rnote over Writer
  役割: 変換済み Book を標準出力へ出力
endrnote

Scheduler -> JobOperator: start(jpaPagingItemReaderJob, JobParameters)
JobOperator -> JpaJob: jpaPagingItemReaderJob()
JpaJob -> JpaStep: start()
JpaStep -> Reader: read()
Reader -> EMF: SELECT b FROM Book b WHERE b.createdTime < :now
EMF --> Reader: Book ページ
Reader --> JpaStep: Book
JpaStep -> Processor: process(book)
Processor --> JpaStep: book
JpaStep -> Writer: writer(chunk)
Writer -> Writer: println(book)
Writer --> JpaStep: 書き込み完了
JpaStep --> JpaJob: Step 完了
JpaJob --> JobOperator: Job 完了
@enduml
```

## 4. CUSTOM_READER_JOB
処理概要: Blaze-Persistence を使って Book をページング取得し、author を加工したうえで、重複抑止付きの SQL でレコードを保存するジョブ。

```plantuml
@startuml
skinparam shadowing false
skinparam NoteBackgroundColor #F5F5F5
skinparam NoteBorderColor #666666
skinparam ParticipantBackgroundColor #EAF4FF
skinparam ParticipantBorderColor #4A90E2

actor Scheduler
participant JobOperator
participant "CustomReaderJobConfig" as CustomJob
participant "customReaderStep" as CustomStep
participant "CustomItemReader" as Reader
participant "CriteriaBuilderFactory" as Criteria
participant "EntityManager" as EM
participant "ItemProcessor" as Processor
participant "JdbcBatchItemWriter" as Writer
participant "tbl_book" as Table

rnote over CustomJob
  役割: カスタム ItemReader と Step を定義
endrnote
rnote over Reader
  役割: offset / maxResults で 1 ページずつ取得
endrnote
rnote over Criteria
  役割: Blaze-Persistence の CriteriaBuilder を生成
endrnote
rnote over Processor
  役割: author がすでに "Author. " 付きなら重複付与しない
endrnote
rnote over Writer
  役割: WHERE NOT EXISTS で重複 insert を抑止
endrnote

Scheduler -> JobOperator: start(customReaderJob, JobParameters)
JobOperator -> CustomJob: customReaderJob()
CustomJob -> CustomStep: start()
CustomStep -> Reader: read()
Reader -> Criteria: create(entityManager, Book)
Criteria -> EM: query.firstResult = offset
EM --> Reader: Book page
Reader --> CustomStep: Book
CustomStep -> Processor: process(book)
Processor -> Processor: author = if (startsWith("Author. ")) ... else "Author. $author"
Processor --> CustomStep: 変換済み Book
CustomStep -> Writer: writer(chunk)
Writer -> Table: INSERT ... SELECT ... WHERE NOT EXISTS
Table --> Writer: 追加/スキップ結果
Writer --> CustomStep: 書き込み完了
CustomStep --> CustomJob: Step 完了
CustomJob --> JobOperator: Job 完了
@enduml
```

補足:
- SIMPLE_JOB は Tasklet ベースのシンプルな実行フローです。
- JSON_FILE_JOB は ClassPath の JSON を読み取る構成です。
- JPA_PAGING_IR_JOB はページング読み取りでデータベースから Book を取得します。
- CUSTOM_READER_JOB は Blaze-Persistence の CriteriaBuilder を使ってページング取得し、再実行時の重複を避ける実装です。

## 依存関係と運用上の注意
- Spring Boot 4.1.1 では Hibernate 7 系が使われるため、Blaze の Hibernate 7.1 向けモジュールを使用する。
- PostgreSQL JDBC は 42.7.13 まで更新しており、脆弱性報告の対象となる 42.7.4 ではないことを確認している。
- 依存の組み合わせは `hibernate-core 7.4.5.Final` と `blaze-persistence-integration-hibernate-7.1:1.6.20` が解決される構成を前提にしている。
- `CUSTOM_READER_JOB` は再実行時の二重登録を避けるため、author の prefix 重複抑止と `WHERE NOT EXISTS` を併用している。
