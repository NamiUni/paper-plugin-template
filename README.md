# Paper Plugin Template
本テンプレートは、Paperサーバー向けのリッチなプラグイン開発をサポートするために作成されました。PaperAPI、Adventure、Configurateなどのモダンなライブラリを活用しており、開発環境をシンプルかつ効率的に整えることができます。  

**「use this template」** ボタンを押すだけで、すぐにPaperプラグインの開発を開始できます。

## 特徴

- **統合開発環境（IDE）での起動・デバッグ**  
  IDE上でPaperサーバーを起動し、プラグインの動作をリアルタイムで確認・デバッグ可能です。
- **依存性注入（DI）コンテナの活用**  
  DIコンテナにより各クラスの依存関係が明確になり、テストや保守が容易になります。
- **設定ファイルの自動マッピング**  
  エンドユーザー向けの設定がJavaオブジェクトへ自動マッピングされ、管理が簡単です。
- **多言語対応**  
  プレイヤーの言語設定に応じた自動翻訳機能を提供し、ユーザーフレンドリーな環境を実現しています。

## Gradleプラグイン

### [indra licenser spotless](https://github.com/KyoriPowered/indra/wiki/indra-licenser-spotless)

- **概要**: 全クラスのソースコードに一括でライセンスヘッダーを挿入できるGradleプラグインです。  
- **メリット**: IntelliJ IDEAなどのIDEで個別に設定する手間を省き、開発環境が変わっても一貫したライセンス管理が可能です。

### [gremlin](https://github.com/jpenilla/gremlin)

- **概要**: Paperプラグイン実行時に必要な依存関係を自動解決するGradleプラグインです。  
- **利用理由**: [PluginLoader](https://docs.papermc.io/paper/dev/getting-started/paper-plugins#loaders) の仕組みを利用してプラグインを効率的に読み込むために使用します。

### [Resource Factory](https://github.com/jpenilla/resource-factory)

- **概要**: `paper-plugin.yml` の設定をビルドスクリプト内で定義することで、コンパイル時にプラグインjarへ自動同梱できるプラグインです。  
- **メリット**: 設定ミスの削減と管理の簡略化が期待できます。  
- **補足**: 同様の機能を提供する [plugin-yml](https://github.com/eldoriarpg/plugin-yml?tab=readme-ov-file) も利用可能です。gremlinの機能も含まれており、シンプルな設定で利用できるためおすすめです。

### [run-task](https://github.com/jpenilla/run-task)

- **概要**: IDE上で **Paper/Velocity/Waterfall** サーバーを実行できるGradleプラグインです。  
- **メリット**:
  - サーバー起動だけでプラグインのテストが可能
  - 依存プラグインを自動インストール
  - ホットスワップ対応により、サーバー起動中でもコード変更を反映可能
- **効果**: プラグインのコンパイルやテスト用プラグインディレクトリへの配置などの手間を大幅に削減し、ストレスフリーな開発体験を実現します。

### [check-style](https://docs.gradle.org/current/userguide/checkstyle_plugin.html) (未導入)

- **概要**: コード品質を一定水準に保つためのGradleプラグインです。  
- **機能**: 設計上の問題検出、コードレイアウトやフォーマットがコーディングスタイルに準拠しているかのチェックが可能。  
- **現状**: 設定が複雑なため、まだ導入していませんが、今後の検討対象です。

## Javaライブラリ

### [Guice](https://github.com/google/guice)

- **概要**: オブジェクト生成やファクトリーメソッドの必要性を軽減するDI（依存性注入）コンテナです。  
- **採用理由**: Spring BootなどのDIコンテナは機能が多すぎるため、Minecraftプラグイン向けに軽量なGuiceを採用。  
- **メリット**:
  - コンストラクタがシンプルに保たれる
  - インスタンス生成の手間が軽減
  - 依存関係が明確になり、変更や再利用、テストが容易に

### [Configurate](https://github.com/SpongePowered/Configurate?tab=readme-ov-file)

- **概要**: Sponge開発のコンフィグレーションAPIで、BukkitAPIのFileConfigurationよりも多機能です。  
- **メリット**:
  - Javaオブジェクトと設定ファイルの相互変換
  - カスタムType Serializerによる文字列変換
- **留意点**: 現状、YAMLファイルのコメント出力に対応していない点が課題ですが、改善に向けたプルリクエストも提出されています。

### [Paper Plugins API](https://docs.papermc.io/paper/dev/getting-started/paper-plugins)

- **概要**: サーバー起動前の資源（設定やデータベース）の初期化、Bukkit/Spigotではできなかったレジストリの編集などを可能にするAPIです。  
- **メリット**:
  - サーバー起動前に資源の初期化が可能なため、エラーを早期に検出できます。  
    - ※ただし、Bukkitの一部API（例: `Server` や `ItemStack`）は `JavaPlugin#onEnable` 以降での利用が前提なので注意が必要です。
  - Mojangの[Brigadier](https://github.com/Mojang/brigadier)を基にしたコマンド登録が可能
  - レジストリ編集（現状はエンチャントのみ対応）
  - JavaPlugin具象クラスがコンストラクタを持てるため、柔軟な初期化が可能
  - 実行時に必要なランタイムの自動ダウンロード機能を提供

### [MiniMessage](https://docs.advntr.dev/minimessage/index.html)

- **概要**: adventureプロジェクトの一部で、Minecraftのチャットコンポーネントに対応するメッセージフォーマットAPIです。  
- **メリット**:
  - カラー、リンク、ホバーなどのリッチな表現が可能
  - 独自タグの定義が可能
  - プレースホルダー機能による動的な文字列置換が容易

### [MiniPlaceholders](https://github.com/MiniPlaceholders/MiniPlaceholders)

- **概要**: MiniMessageのプレースホルダー機能を拡張するライブラリです。  
- **特徴**:
  - 他プラグインが提供するタグの利用が可能
  - 他プラグインに提供するタグの登録も可能
  - [PlaceholderAPI拡張](https://modrinth.com/plugin/miniplaceholders-placeholderapi-expansion)もあります。

### [moonshine](https://github.com/KyoriPowered/moonshine) (廃止予定)

- **概要**: プラグインの多言語対応を実現するためのローカライゼーションライブラリです。  
- **現状**: 主要開発者がMinecraft関連開発から距離を置いたため、長期間メンテナンスされていませんが、動作には問題ありません。  
- **将来的な対応**: adventureにMiniMessageTranslatorが実装されるなどのアップデートに伴い、不要になる可能性があるため、将来的に入れ替えを検討予定です。  
- **メリット**:
  - プレイヤーごとに異なるメッセージを送信可能（言語以外の条件分岐にも対応）
  - メッセージ生成時にプレースホルダーで動的な文字列置換を実施可能

---

## 補足

- 各Gradleプラグイン・ライブラリの選定理由やメリットについて、プロジェクトの開発効率・保守性向上を目的として記述しています。
- 今後も定期的に追加や変更を加えていく予定です。

このREADMEが、Paperプラグイン開発の参考として役立つことを願っています。ご不明な点や改善案などがあればぜひフィードバックをお寄せください :)
