#!/usr/bin/env python3
"""Expand Hindi suffix gender shorthand into full-word markup.

Authoring shorthand:  करता{|ी}  रहा{|ही}  गया{|ई}  हुआ{|ी}
Expanded (what the app reads):  {करता|करती}  {रहा|रही}  {गया|गई}  {हुआ|हुई}

Usage: python3 tools/expand_gender.py content/src/hi/*.txt
"""
import re, sys

def fem(w, suf):
    if w.endswith('हुआ'):
        return w[:-3] + 'हुई'
    if suf == 'ी' and w.endswith('ा'):
        return w[:-1] + 'ी'
    if suf == 'ही' and w.endswith('हा'):
        return w[:-2] + 'ही'
    if suf == 'ई' and w.endswith('या'):
        return w[:-2] + 'ई'
    raise SystemExit(f'unhandled shorthand: {w}{{|{suf}}}')

PAT = re.compile(r'([^\s{}|"«»“”]+)\{\|([^{}|]+)\}')

for path in sys.argv[1:]:
    s = open(path, encoding='utf-8').read()
    n = len(PAT.findall(s))
    s = PAT.sub(lambda m: '{%s|%s}' % (m.group(1), fem(m.group(1), m.group(2))), s)
    open(path, 'w', encoding='utf-8').write(s)
    print(f'{path}: expanded {n}')
