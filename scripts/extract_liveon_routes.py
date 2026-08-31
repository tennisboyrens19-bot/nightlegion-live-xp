#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / 'src/main/java/com/liveon/ClanMessagesPlugin.java'
OUT = ROOT / 'LIVEON_REST_CONTRACT.md'
text = SRC.read_text(encoding='utf-8')
lines = text.splitlines()

# Build simple method ranges by brace counting.
starts=[]
for i,line in enumerate(lines):
    m=re.match(r'\s*(?:private|public|protected)\s+(?:static\s+)?[^=;]+?\s+(\w+)\s*\([^;]*\)\s*$', line)
    if m:
        starts.append((i,m.group(1)))
ranges=[]
for idx,(start,name) in enumerate(starts):
    end=starts[idx+1][0] if idx+1<len(starts) else len(lines)
    ranges.append((start,end,name))

interesting=[]
for start,end,name in ranges:
    body='\n'.join(lines[start:end])
    if 'config.serverUrl()' not in body and 'discordNotificationRequest' not in body and 'wiseoldman' not in body.lower():
        continue
    paths=[]
    for kind,val in re.findall(r'\.addPath(Segments|Segment)\("([^"]+)"\)',body):
        paths.append(val)
    queries=re.findall(r'\.addQueryParameter\("([^"]+)",\s*([^\n]+?)\)',body)
    verbs=[]
    for v in ('get','post','put','delete','patch'):
        if re.search(rf'\.{v}\s*\(',body): verbs.append(v.upper())
    if '.delete()' in body: verbs.append('DELETE')
    headers=re.findall(r'\.header\("([^"]+)",\s*([^\n]+?)\)',body)
    types=re.findall(r'gson\.fromJson\([^,]+,\s*([^\n]+?\.class)',body)
    jsonputs=re.findall(r'(?:payload|body|requestPayload|data|extra)\.put\("([^"]+)"',body)
    interesting.append((name,start+1,paths,queries,sorted(set(verbs)),headers,types,sorted(set(jsonputs))))

out=['# Live On REST contract (static extraction)','', 'Generated from the exact vendored upstream `ClanMessagesPlugin.java`.', '']
for name,line,paths,queries,verbs,headers,types,jsonputs in interesting:
    out += [f'## `{name}` (line {line})', '', f'- HTTP verbs: `{", ".join(verbs) or "unknown"}`', f'- Path segments: `{ "/".join(paths) if paths else "(none detected)" }`']
    if queries:
        out.append('- Query parameters: ' + ', '.join(f'`{k}`' for k,_ in queries))
    if headers:
        out.append('- Headers: ' + ', '.join(f'`{k}`' for k,_ in headers))
    if types:
        out.append('- Gson response types: ' + ', '.join(f'`{t}`' for t in types))
    if jsonputs:
        out.append('- JSON fields written: ' + ', '.join(f'`{k}`' for k in jsonputs))
    out.append('')
OUT.write_text('\n'.join(out),encoding='utf-8')
