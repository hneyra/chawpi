#!/usr/bin/env bash
# rebuilds legacy-routes.txt: the original app's REST routes ("VERB /path"), read statically from its
# controllers. no boot, no database. class @RequestMapping prefix + method @Get/Post/Put/Patch/Delete/RequestMapping.
# the original has no functional (RouterFunction) routes: the script fails if that ever changes, and on any
# annotation it cannot read (multi-line, constants, method-level @RequestMapping without a verb).
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
src="${SAPGIS_SRC:-/Users/jorge/IdeaProjects/sapgis/backend/src/main/kotlin}"
[ -d "$src" ] || { echo "no sources at $src (set SAPGIS_SRC)" >&2; exit 1; }
if grep -rqE 'RouterFunction|coRouter[[:space:]]*\{|router[[:space:]]*\{' "$src"; then
  echo "functional routes found in $src: extend this script" >&2
  exit 1
fi
out="$here/legacy-routes.txt"
find "$src" -name '*.kt' -print0 | sort -z | xargs -0 awk '
  function fail(msg) { printf "%s:%d: %s\n", FILENAME, FNR, msg > "/dev/stderr"; bad = 1; exit 1 }
  # the path strings of one annotation line: value/path arrays, or the positional first argument
  function paths(line, arr,   args, n, s) {
    n = 0
    if (line !~ /\(/) { arr[++n] = ""; return n }
    if (line !~ /\)/) fail("multi-line annotation")
    args = substr(line, index(line, "(") + 1)
    if (args ~ /(value|path)[[:space:]]*=/) { sub(/.*(value|path)[[:space:]]*=[[:space:]]*/, "", args) }
    else if (args ~ /^[[:space:]]*[a-z]+[[:space:]]*=/) { arr[++n] = ""; return n }
    if (args ~ /^[[:space:]]*\[/) { sub(/\].*/, "", args) } else { sub(/[,)].*/, "", args) }
    if (args !~ /"/ && args !~ /^[[:space:]]*$/) fail("non-literal path: " args)
    while (match(args, /"[^"]*"/)) { s = substr(args, RSTART + 1, RLENGTH - 2); arr[++n] = s; args = substr(args, RSTART + RLENGTH) }
    if (n == 0) arr[++n] = ""
    return n
  }
  FNR == 1 { inClass = 0; prefix = "" }
  /^[[:space:]]*@(Rest)?Controller([^A-Za-z]|$)/ { inClass = 1; prefix = ""; header = 1; next }
  header && /^[[:space:]]*(@|$)/ && !/@RequestMapping/ { next }
  header && /@RequestMapping/ { np = paths($0, p); if (np != 1) fail("several class prefixes"); prefix = p[1]; next }
  header && /class[[:space:]]/ { header = 0; next }
  inClass && /@(Get|Post|Put|Patch|Delete)Mapping/ {
    match($0, /@(Get|Post|Put|Patch|Delete)Mapping/); verb = toupper(substr($0, RSTART + 1, RLENGTH - 8))
    np = paths($0, p); for (i = 1; i <= np; i++) print verb " " prefix p[i]
    next
  }
  inClass && /@RequestMapping/ {
    if (!match($0, /RequestMethod\.[A-Z]+/)) fail("method-level @RequestMapping without a verb")
    verb = substr($0, RSTART + 14, RLENGTH - 14)
    np = paths($0, p); for (i = 1; i <= np; i++) print verb " " prefix p[i]
  }
' | sed -E 's#(.)/$#\1#' | LC_ALL=C sort -u > "$out.tmp"
mv "$out.tmp" "$out"
echo "$(wc -l < "$out" | tr -d ' ') routes -> $out"
