# RePlugin - Resource Pack Distributor

GitHub Releasesとローカルフォルダからリソースパックを自動配布するPaperプラグインです。

## 機能

- 🌐 **GitHub Releases直接配布** - GitHub から直接ダウンロード可能な URL をクライアントに送信
- 📦 **ローカルサーバー統合** - プラグイン内蔵の HTTP サーバーでローカルパックを配信
- 🔐 **SHA1ハッシュ自動計算** - サーバー起動時に全パックのハッシュを計算
- 📡 **複数パック配布** - 接続時に複数パックを1秒間隔で配布
- ⚙️ **複数GitHub Releases対応** - owner/repo/tag/filename で複数リリースから配布可能

## セットアップ

### 1. プラグインの構築

```bash
mvn clean package
```

生成された `target/RePlugin-1.0.0.jar` を `plugins/` に配置します。

### 2. 設定

サーバー起動後、以下が自動生成されます：

```
plugins/RePlugin/
├── config.yml      # GitHub設定
├── packs.yml       # 個別パック設定
└── packs/          # パックフォルダ
    ├── tuki.zip
    ├── bgm-pack.zip
    └── ...
```

#### **config.yml** - GitHub Releases設定とサーバー設定

複数の GitHub Releases から自動ダウンロード、ローカルサーバーで配信：

```yaml
github:
  enabled: true
  sources:
    - owner: "swmr71"                    # GitHubユーザー名
      repo: "MinecraftKaguya"            # リポジトリ名
      tag: "Mirrorv1"                    # リリースタグ
      filename: "tuki.zip"               # ファイル名
    
    - owner: "your-user"
      repo: "your-repo"
      tag: "v1.0.0"
      filename: "bgm-pack.zip"

# ローカルリソースパック配信サーバー
server:
  host: "localhost"                      # バインドホスト
  port: 8765                             # バインドポート
```

**GitHub Releases URL:**  
`https://github.com/{owner}/{repo}/releases/download/{tag}/{filename}`

**ローカルパック URL:**  
`http://{server.host}:{server.port}/{filename}`

#### **packs.yml** - パック個別設定

```yaml
packs:
  my-texture-pack.zip:
    enabled: true
    url: "https://clusters-prj.com/resourcepacks/my-texture-pack.zip"
  
  bgm-pack.zip:
    enabled: true
    url: "https://clusters-prj.com/resourcepacks/bgm-pack.zip"

global:
  reject-action: "warn"  # プレイヤーが拒否した場合の処理
```

### 3. リソースパックの配置

#### **GitHub Releasesから配布**

`config.yml` でオーナー・リポ・タグを設定すれば、サーバー起動時に自動ダウンロード。
`.zip` ファイルのみが対象です。

#### **ローカルフォルダから配布**

`plugins/RePlugin/packs/` に `.zip` ファイルを置くだけです。

## 動作フロー

```
サーバー起動時
  ├─ config.yml を読み込む
  ├─ ローカルHTTPサーバーを起動 (デフォルト: localhost:8765)
  ├─ GitHub API で複数リリースを取得・ダウンロード
  │   └─ GitHub Releases の直接 URL を使用
  ├─ ローカルフォルダをスキャン
  │   └─ プラグイン内 HTTP サーバーで配信するよう URL を設定
  └─ 全パックのSHA1を計算

プレイヤー接続時
  ├─ キューに追加
  └─ 1秒ごとに1プレイヤーに対して全パックを配布
      （複数プレイヤーが同時接続しても負荷分散）
```

## コマンド

```bash
/replugin reload    # 設定をリロード（開発中）
/replugin list      # ロード済みパックの一覧表示（開発中）
/replugin test <プレイヤー>  # 指定プレイヤーにパック配布をテスト（開発中）
```

## トラブルシューティング

### GitHubからダウンロードできない

- `config.yml` の `owner` と `repo` が正しいか確認
- GitHub API の public access でアクセスできるか確認
- ネットワーク接続を確認

### パックが配布されない

- `packs.yml` で `enabled: true` になっているか確認
- `url` が正しいか確認（サーバーからアクセス可能な URL を指定）
- コンソールにエラーが出ていないか確認

### SHA1ハッシュが計算されない

- `.zip` ファイルが正しい形式か確認
- ファイルのパーミッションを確認

## ライセンス

MIT License

## 作成者

clusters-prj
