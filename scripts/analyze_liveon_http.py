#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'src/main/java/com/liveon/ClanMessagesPlugin.java'
OUT = ROOT / 'LIVEON_HTTP_CALLS.md'

text = SOURCE.read_text(encoding='utf-8')
lines = text.splitlines()
methods = []
current = '<class>'
brace_depth = 0
method_depth = None
for i, line in enumerate(lines, 1):
    m = re.search(r'^\s*(?:private|public|protected)\s+(?:static\s+)?(?:[\w<>?,.\[\]]+\s+)+(?P<name>\w+)\s*\(', line)
    if m:
        current = m.group('name')
        method_depth = brace_depth
    brace_depth += line.count('{') - line.count('}')
    if any(token in line for token in ('HttpUrl', 'new Request.Builder()', 'Request.Builder()', '.url(', 'addPathSegment(', 'addQueryParameter(', 'MultipartBody.Builder')):
        start = max(1, i - 18)
        end = min(len(lines), i + 28)
        snippet = '\n'.join(f'{n:04d}: {lines[n-1]}' for n in range(start, end + 1))
        methods.append((current, i, snippet))

seen = set()
out = ['# Live On HTTP call inventory', '', 'Generated from exact vendored upstream source.', '']
for name, line, snippet in methods:
    key = (name, snippet)
    if key in seen:
        continue
    seen.add(key)
    out += [f'## `{name}` around line {line}', '', '```java', snippet, '```', '']
OUT.write_text('\n'.join(out), encoding='utf-8')
