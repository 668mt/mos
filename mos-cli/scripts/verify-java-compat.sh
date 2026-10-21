#!/usr/bin/env bash
#
# 与 Java SDK 的交叉验证。
#
# 验证两件事：
#   1. mos-cli 生成的签名，能被真实的 Java SDK（MosEncrypt.decrypt）正确解析
#   2. Java SDK 生成的签名，能被 mos-cli 正确解析（src/test/crypto.test.ts 已覆盖）
#
# 需要本机有 JDK，且 mos-base / mos-sdk 已编译。
#
# 用法：bash scripts/verify-java-compat.sh

set -euo pipefail

# Git Bash 会把 /a.txt 这类参数自动转换成 Windows 路径（MSYS 路径转换），
# 导致 Java 收到的 pathname 被篡改，这里关闭该行为。
export MSYS_NO_PATHCONV=1
export MSYS2_ARG_CONV_EXCL='*'

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO="$(cd "$ROOT/.." && pwd)"
M2="${M2_REPO:-D:/Maven/repository}"

# Git Bash 下 pwd 返回 /d/work/... 这类 MSYS 路径，Java 无法识别，统一转成 Windows 路径
win_path() {
  local p="$1"
  case "$p" in
    /[A-Za-z]/*) printf '%s:%s' "$(echo "${p:1:1}" | tr '[:lower:]' '[:upper:]')" "${p:2}" ;;
    *) printf '%s' "$p" ;;
  esac
}

ROOT="$(win_path "$ROOT")"
REPO="$(win_path "$REPO")"

MOS_BASE="$REPO/mos-base/target/classes"
MOS_SDK="$REPO/mos-sdk/target/classes"

if [[ ! -d "$MOS_BASE" || ! -d "$MOS_SDK" ]]; then
  echo "错误：找不到 mos-base / mos-sdk 的编译产物"
  echo "请先执行：cd $REPO && (cd mos-base && mvn -q -DskipTests compile) && (cd mos-sdk && mvn -q -DskipTests compile)"
  exit 1
fi

if ! command -v javac >/dev/null 2>&1; then
  echo "错误：未找到 javac，请安装 JDK"
  exit 1
fi

# 组装 classpath（依赖 jar 缺失时给出提示）
find_jar() {
  local pattern="$1"
  local found
  found="$(find "$M2" -name "$pattern" ! -name '*sources*' ! -name '*javadoc*' 2>/dev/null | sort -V | tail -1)"
  if [[ -z "$found" ]]; then
    echo "错误：找不到依赖 $pattern（可设置 M2_REPO 指向本地 Maven 仓库）" >&2
    exit 1
  fi
  echo "$found"
}

CP="$MOS_BASE;$MOS_SDK"
CP="$CP;$(find_jar 'slf4j-api-*.jar')"
CP="$CP;$(find_jar 'fastjson-1*.jar')"
CP="$CP;$(find_jar 'commons-lang3-*.jar')"

WORK="$(mktemp -d)"
WORK="$(win_path "$(cd "$WORK" && pwd -W 2>/dev/null || pwd)")"
trap 'rm -rf "$WORK"' EXIT

# 编译验证器
cat > "$WORK/VerifyCli.java" <<'EOF'
import mt.spring.mos.sdk.utils.MosEncrypt;
import mt.spring.mos.sdk.type.PathnamesEncryptContent;

public class VerifyCli {
    public static void main(String[] args) throws Exception {
        String key = args[0];
        String sign = args[1];
        String path = args[2];
        long openId = Long.parseLong(args[3]);
        long expire = Long.parseLong(args[4]);

        MosEncrypt.MosEncryptContent c = MosEncrypt.decrypt(key, sign);
        PathnamesEncryptContent content = (PathnamesEncryptContent) c.getContent();
        boolean ok = c.getOpenId() == openId
                && c.getExpireSeconds() == expire
                && content.getPathnames().contains(path);

        System.out.println((ok ? "PASS" : "FAIL")
                + " bucket=" + c.getBucketName()
                + " openId=" + c.getOpenId()
                + " expire=" + c.getExpireSeconds()
                + " pathnames=" + content.getPathnames());
        if (!ok) System.exit(1);
    }
}
EOF

javac -encoding UTF-8 -cp "$CP" -d "$WORK" "$WORK/VerifyCli.java" 2>&1 | grep -v '^Note' || true
[[ -f "$WORK/VerifyCli.class" ]] || { echo "错误：VerifyCli 编译失败" >&2; exit 1; }

# 构建 mos-cli
echo "==> 构建 mos-cli"
npm --prefix "$ROOT" run build >/dev/null

# 生成签名
cat > "$WORK/gen.mjs" <<EOF
import { encrypt, PathnamesEncryptContent } from 'file:///${ROOT}/dist/core/sign.js';
const cases = [
  ['secret-abc123', '/a.txt', 0, -1],
  ['secret-abc123', '/dir/sub/file name.png', 10, 600],
  ['secret-abc123', '/中文/文件.png', 12345, 7200],
  ['xK9\$mQ2pL7wZ4tR', '/deep/path/with-dash_and_underscore.bin', 999999, 3600],
  ['short', '/x', 0, 60],
  ['0123456789abcdef0123456789abcdef', '/long-key.bin', 42, 1800],
  ['p@ss|w0rd', '/weird|key.txt', 7, 300],
];
for (const [key, path, oid, exp] of cases) {
  // 用 JSON 传递，避免分隔符与内容冲突
  console.log(JSON.stringify({
    key, path, oid, exp,
    sign: encrypt(key, new PathnamesEncryptContent(path), 'default', oid, exp),
  }));
}
EOF

node "$WORK/gen.mjs" > "$WORK/sigs.jsonl"

echo "==> 用真实 Java SDK 校验 mos-cli 生成的签名"
FAILED=0
COUNT=0
while IFS= read -r line; do
  COUNT=$((COUNT + 1))
  key=$(node -e "console.log(JSON.parse(process.argv[1]).key)" "$line")
  sign=$(node -e "console.log(JSON.parse(process.argv[1]).sign)" "$line")
  path=$(node -e "console.log(JSON.parse(process.argv[1]).path)" "$line")
  oid=$(node -e "console.log(JSON.parse(process.argv[1]).oid)" "$line")
  exp=$(node -e "console.log(JSON.parse(process.argv[1]).exp)" "$line")

  if output=$(java -Dfile.encoding=UTF-8 -cp "$WORK;$CP" VerifyCli "$key" "$sign" "$path" "$oid" "$exp" 2>&1 | grep -v SLF4J); then
    echo "  ✔ [$COUNT] $path"
  else
    echo "  ✖ [$COUNT] $path -> $output"
    FAILED=$((FAILED + 1))
  fi
done < "$WORK/sigs.jsonl"

echo
if [[ "$FAILED" -eq 0 ]]; then
  echo "全部通过：$COUNT/$COUNT 个签名均可被 Java SDK 正确解析"
else
  echo "失败 $FAILED/$COUNT"
  exit 1
fi
