# SneakGrow

プレイヤーが**しゃがんだ瞬間**に、周囲の作物や苗木を骨粉と同じ仕組みで成長させ、家畜の成長・繁殖クールダウンも早める [Paper](https://papermc.io/) 用プラグインです。

## 機能

- **作物・植物の成長**: `growable` に指定したブロックへ、骨粉と同じ処理(`Block#applyBoneMeal`)を確率で適用します。
  - 地面ブロックには使わないので、草や海草、サンゴが勝手に生えることはありません。
- **家畜の成長**: 周囲の子供の家畜の成長までの時間を短縮します。
- **繁殖クールダウン短縮**: 繁殖後の親の、次の繁殖までの時間を短縮します。
- 成長時にパーティクルを表示します(切り替え可)。

## 動作環境

- Paper 1.21.x(`api-version: 1.21`)
- Java 21 以上

## インストール

1. [Releases](../../releases) から JAR をダウンロードするか、自分でビルドします。
2. JAR をサーバーの `plugins/` に入れて起動します。

## ビルド

```sh
gradle build
```

`build/libs/SneakGrow-<version>.jar` が生成されます。

## コマンドと権限

| コマンド | 説明 | 権限 |
|---|---|---|
| `/sneakgrow reload` | 設定を再読み込み | `sneakgrow.admin` (既定: OP) |

| 権限 | 説明 | 既定 |
|---|---|---|
| `sneakgrow.use` | しゃがみによる成長効果を使える | 全員 |
| `sneakgrow.admin` | 設定の再読み込み | OP |

## 設定 (`plugins/SneakGrow/config.yml`)

| キー | 初期値 | 説明 |
|---|---|---|
| `radius` | `4` | 効果範囲の半径(ブロック、最大16) |
| `vertical-range` | `1` | 垂直方向の探索範囲(上下それぞれ、最大8) |
| `chance` | `0.35` | 各ブロックが1回のしゃがみで成長する確率 |
| `cooldown-ticks` | `5` | 連続発動を防ぐクールダウン(tick) |
| `particles` | `true` | 成長時のパーティクル表示 |
| `enabled-worlds` | `[]` | 有効にするワールド(空なら全ワールド) |
| `entities.enabled` | `true` | 家畜への効果を有効にする |
| `entities.chance` | `0.5` | 各個体が成長する確率 |
| `entities.age-boost-ticks` | `2400` | 子供の成長までの時間を1回で短縮する量(tick) |
| `entities.breed-cooldown-boost-ticks` | `600` | 親の繁殖クールダウンを1回で短縮する量(tick、0で無効) |
| `entities.types` | 牛・ムーシュルーム・羊・豚・鶏・ヤギ | 対象のエンティティ(`EntityType` 名) |
| `growable` | 作物・苗木・竹・ツタ | 骨粉を適用するブロック(`Material` 名) |

### `growable` の注意

ホワイトリスト方式です。草ブロックや水中の砂のような**地面ブロックを入れると**、骨粉の仕様で周囲に草・海草・サンゴなどが生えます。キノコ類は巨大キノコになります。

## ライセンス

[MIT License](LICENSE)

## リリース手順(メンテナ向け)

`v1.0.0` のようなタグを push すると、GitHub Actions が JAR をビルドして Release を作成します。

```sh
git tag v1.0.0
git push origin v1.0.0
```
