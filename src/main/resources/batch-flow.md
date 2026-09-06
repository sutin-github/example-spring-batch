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
処理概要: 独自の ItemReader を使用し、DB の Book を読み込み、author を加工して保存するジョブ。

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
participant "BookRepository" as Repo
participant "ItemProcessor" as Processor
participant "ItemWriter" as Writer

rnote over CustomJob
  役割: カスタムリーダーを使って Step を定義
endrnote
rnote over Reader
  役割: BookRepository から読み込んだデータを 1 件ずつ返す
endrnote
rnote over Processor
  役割: author に接頭辞を付けて加工する
endrnote
rnote over Writer
  役割: 変換済み Book を保存する
endrnote

Scheduler -> JobOperator: start(customReaderJob, JobParameters)
JobOperator -> CustomJob: customReaderJob()
CustomJob -> CustomStep: start()
CustomStep -> Reader: read()
Reader -> Repo: findAll()
Repo --> Reader: Book リスト
Reader --> CustomStep: Book
CustomStep -> Processor: process(book)
Processor -> Processor: author = "Author. " + author
Processor --> CustomStep: 変換済み Book
CustomStep -> Writer: writer(chunk)
Writer -> Repo: saveAll(list)
Repo --> Writer: 保存完了
Writer --> CustomStep: 書き込み完了
CustomStep --> CustomJob: Step 完了
CustomJob --> JobOperator: Job 完了
@enduml
```

補足:
- SIMPLE_JOB は Tasklet ベースのシンプルな実行フローです。
- JSON_FILE_JOB は ClassPath の JSON を読み取る構成です。
- JPA_PAGING_IR_JOB はページング読み取りでデータベースから Book を取得します。
- CUSTOM_READER_JOB は独自の ItemReader と処理済みデータを DB に保存します。
