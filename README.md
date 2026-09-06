## Kotlin + Spring Batch サンプル
Spring Batch 6.x / Spring Boot 4.x をベースにした Kotlin サンプルプロジェクトです。
Chunk 処理や Tasklet、JSON 読み取り、JPA ページング、Blaze-Persistence を組み合わせた実装例をまとめています。

---

### 対応環境
- Spring Boot 4.1.1
- Spring Batch 6.x
- Kotlin 2.2.0
- Java 21 (Amazon Corretto 21)
- PostgreSQL 16
- Blaze-Persistence 1.6.20

### 目的
- Spring Batch の基本構成を理解しやすくする
- chunk ベースのバッチ処理を Kotlin で書く例を示す
- JPA Paging と Blaze-Persistence を比較できるようにする
- スケジューラ連携と手動起動の両方を確認できるようにする

### プロジェクト構成
- `src/main/kotlin/me/examplebatch/batch/job` : Job / Step 定義
- `src/main/kotlin/me/examplebatch/batch/tasklet` : Tasklet 実装
- `src/main/kotlin/me/examplebatch/batch/item` : 読み取りロジック
- `src/main/kotlin/me/examplebatch/batch/scheduler` : スケジューラ
- `src/main/resources/application.yml` : 実行設定
- `src/main/resources/sample/json/test.json` : JSON 読み取りサンプル

### Blaze-Persistence のカスタム Reader
`CustomItemReader` は Blaze-Persistence の `CriteriaBuilderFactory` を使って、
テーブル全件をメモリへ展開せずに `offset` と `maxResults` でページ単位に読み取ります。
そのため `JpaPagingItemReader` と同様の考え方で大量データを安全に処理できます。

実装上のポイント:
- `@PostConstruct` に依存した早期初期化ではなく、`read()` の初回アクセス時に必要なページを読み込むようにしています。
- これにより、Spring Batch の初期化タイミングとリーダー起動タイミングのズレを防ぎ、`EntityManager` が未準備の状態で参照されるケースを避けています。
- 1ページごとに `currentPage` を切り替えながら処理するので、大量データでもメモリ使用量を抑えられます。

利点:
- メモリ使用量を抑えられる
- `findAll()` より DB 転送量を削減できる
- 1ページ単位の処理で堅牢なバッチ実行が可能

---

### 前提条件
- Java 21 がインストール済みであること
- PostgreSQL が起動していること
- Docker を使う場合は Docker Desktop / Docker Engine が起動していること

### ローカル PostgreSQL の起動
```bash
# PostgreSQL をバックグラウンド起動
docker compose up -d

# 接続確認
psql -h localhost -U postgres -d book
```

### アプリ起動
```bash
# Gradle で起動
./gradlew bootRun
```

必要に応じて、起動時にジョブ名を指定します。

### ジョブ実行方法
1. スケジューラで実行する場合
```yaml
# application.yml
schedule:
 active: true
```

2. 起動引数でジョブを指定して実行する場合
```yaml
# application.yml
spring:
 batch:
   job:
     names: ${job.name:NONE}

schedule:
 active: false
```

```bash
# 例: JPA Paging Reader ジョブを実行
java -jar --job.name=JPA_PAGING_IR_JOB createDate=2020-10-14 ./batch.jar
```

### ジョブ一覧
- `SIMPLE_JOB`: Tasklet を順に実行する最小構成のジョブ
- `JSON_FILE_JOB`: JSON ファイルを読み込んで出力するジョブ
- `JPA_PAGING_IR_JOB`: JPA Paging ItemReader を使ってページ単位にデータを取得するジョブ
- `CUSTOM_READER_JOB`: Blaze-Persistence を使ったカスタム Reader でページング取得を行うジョブ

### ジョブごとの詳細な処理内容

#### 1. `SIMPLE_JOB`
- 定義箇所: `src/main/kotlin/me/examplebatch/batch/job/SimpleJobConfig.kt`
- 処理概要:
  - Step1 でインラインの Tasklet を実行する
  - Step2 で `SimpleTasklet` を再利用して処理する
  - 連続した 2 ステップを `JobBuilder` で組み立てる
- ステップ構成:
  - `SIMPLE_JOB_STEP_1`:
    - ログを出力して終了するだけの簡易 Tasklet
    - `RepeatStatus.FINISHED` を返して処理を完了する
  - `SIMPLE_JOB_STEP_2`:
    - `SimpleTasklet` を呼び出す
    - `createDate` と `time` のジョブパラメータをログに出す
    - その後 `FINISHED` を返す
- 目的:
  - Spring Batch の最小構成を理解するためのジョブ
  - `Tasklet` の定義方法と `StepBuilder` の使い方を学ぶ
- 実行時の注意:
  - `schedule.active: true` にするとスケジューラが起動する
  - `time` は `SimpleJobScheduler` により実行時タイムスタンプが設定される

#### 2. `JSON_FILE_JOB`
- 定義箇所: `src/main/kotlin/me/examplebatch/batch/job/JsonFileJobConfig.kt`
- 処理概要:
  - `sample/json/test.json` を `JsonItemReader` で読み込む
  - JSON から `JsonItem` オブジェクトへマッピングする
  - 1 チャンクごとに item を出力する
- ステップ構成:
  - `JSON_FILE_JOB_STEP`
  - `chunk<JsonItem, JsonItem>(5, transactionManager)` を使用
  - `JsonItemReaderBuilder` と `JacksonJsonObjectReader` を組み合わせる
- 実際の流れ:
  1. リソースを `ClassPathResource` で開く
  2. JSON を `JsonItem` に変換する Reader を生成する
  3. 5 件単位で chunk を処理する
  4. 各 item を `println` で出力する
- 目的:
  - Spring Batch の JSON 読み取りを試すサンプル
  - `ItemReader` をファイルベースで組み立てる方法の例

#### 3. `JPA_PAGING_IR_JOB`
- 定義箇所: `src/main/kotlin/me/examplebatch/batch/job/JpaPagingItemReaderJobConfig.kt`
- 処理概要:
  - `JpaPagingItemReader` を使って DB から大量データをページング取得する
  - `createDate` パラメータを使って古いデータだけを対象にする
  - `Book` エンティティを読み取り、加工または出力する
- ステップ構成:
  - `JPA_PAGING_IR_JOB_STEP`
  - `chunk<Book, Book>(10, transactionManager)`
  - リーダー: `jpaPagingItemReader(null)`
  - プロセッサ: `jpaPagingProcessor()`
  - ライター: `jpaPagingWriter()`
- Query の例:
  - `SELECT b FROM Book b WHERE b.createdTime < :now`
  - `createDate` を `DateUtil` で変換し、比較条件として使う
  - `createDate` が未指定の場合は当日の日付をデフォルト値として利用し、`!!` による null 参照を避ける
- 実際の流れ:
  1. `createDate` を job parameter から取得する
  2. `DateUtil` で `yyyy-MM-dd` を `Date` に変換する
  3. `JpaPagingItemReader` が 1ページごとに `Book` を取得する
  4. chunk 単位で処理される
  5. ここでは出力のみを行うシンプルな writer を定義している
- 目的:
  - JPA の標準的なページング読み取りを理解する
  - `findAll()` のような全件取得の危険性を避ける設計の例を示す

#### 4. `CUSTOM_READER_JOB`
- 定義箇所: `src/main/kotlin/me/examplebatch/batch/job/CustomReaderJobConfig.kt`
- 処理概要:
  - Blaze-Persistence を使って `Book` をページング取得する
  - `CustomItemReader` を独自実装し、標準の `JpaPagingItemReader` と同様の設計に寄せる
  - 取得したデータを加工して保存する
- ステップ構成:
  - `CUSTOM_READER_JOB_STEP`
  - `chunk<Book, Book>(10, transactionManager)`
  - リーダー: `CustomItemReader(pageSize = CHUNK_SIZE)`
  - プロセッサ: `author` を `Author. ` 付きの形式に変換する
  - ライター: `JdbcBatchItemWriter` による chunk 単位のバッチ更新
- 実際の流れ:
  1. `CustomItemReader` が `CriteriaBuilderFactory.create(...)` を使ってクエリを作成する
  2. `firstResult` と `maxResults` によりページ単位で取得する
  3. `read()` が次の item を返すたびに、現在ページ内の index を進める
  4. ページが終わると次のページをロードして続きを処理する
  5. プロセッサが `author` を加工する
  6. `JdbcBatchItemWriter` が `INSERT INTO tbl_book ...` で一括書き込みを行う
- 目的:
  - `JpaPagingItemReader` に近いページング設計を自前実装で再現する
  - Blaze-Persistence による柔軟なクエリ制御を学ぶ
  - 大量データの読み込みを効率化する例を提供する
  - `saveAll()` のような単純な JPA 保存よりも、バッチ向けの SQL 書き込みを採用して本番運用に近い設計を示す
- 目的:
  - `JpaPagingItemReader` に近いページング設計を自前実装で再現する
  - Blaze-Persistence による柔軟なクエリ制御を学ぶ
  - 大量データの読み込みを効率化する例を提供する

### `SimpleTasklet` と `SimpleJobScheduler` の役割
- `SimpleTasklet`: Job parameter を受け取り、ログ出力だけでなく将来的な処理の入口として使う
- `SimpleJobScheduler`: `@Scheduled` により定期実行を実現し、同時実行を防ぐために `AtomicBoolean` を使って再入を抑止する
  - `JobOperator.startNextInstance(...)` を使って Job を起動し、重複実行を抑える
  - 既に実行中のジョブがある場合は警告ログを出してスキップする
  - `JobExecutionAlreadyRunningException` などの例外を安全に処理する

### 注意事項
- PostgreSQL は `localhost:5432` で起動している必要があります
- `spring.batch.job.names` にジョブ名を指定して実行してください
- `createDate` などのジョブパラメータは、各ジョブの要件に応じて渡してください
- `createDate` が未指定の場合、`JpaPagingItemReaderJobConfig` は当日の日付をデフォルト値として使います
- ログ出力先は `logs/batch.log` を使うように設定しており、環境依存の `/User/logs` への書き込みを避けています
- Spring Batch のメタデータテーブルは起動時に自動的に扱われる構成になっています
- `CUSTOM_READER_JOB` は Blaze-Persistence の準備が必要で、JPA と Hibernate のバージョンに合わせた設定が前提です

### 実行確認のポイント
- `./gradlew compileKotlin` でソースの整合性を確認できます
- `docker compose up -d` で PostgreSQL を立ち上げた状態でアプリ起動を確認します
- データベースが未起動のときは接続エラーで起動失敗するため、まず DB を準備してください
- Job を起動する際は、ジョブ名と required parameter を確認してから実行してください

---

この README は、プロジェクトのセットアップ、各ジョブの役割、処理の流れ、DB 接続の前提条件まで確認できるように整理しています。