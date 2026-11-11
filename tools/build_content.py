#!/usr/bin/env python3
"""
Builds app/src/main/assets/i18n/<lang>.json from content/src/<lang>/*.txt and validates
every language against English (the master).

Source format (UTF-8):
    # comment
    key = single line value
    key:
    - list item
    - list item
    key <<
    multi-line text block
    >>

Validation:
  * every English key exists in every language (and no unknown extras)
  * list lengths match English
  * {placeholders} such as {name} {n} {a} {b} match English
  * gender markup {masc|fem} is well-formed
Run:  python3 tools/build_content.py            (build + validate)
      python3 tools/build_content.py --check    (validate only)
"""
import json, os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "content", "src")
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "i18n")
LANGS = ["en", "hi", "ru", "ko", "zh", "es", "fr", "it", "ja"]

PLACEHOLDER = re.compile(r"\{([a-z_]+)\}")
GENDER = re.compile(r"\{([^{}|]*)\|([^{}|]*)\}")


def parse_file(path):
    data = {}
    cur_list_key = None
    block_key = None
    block_lines = []
    with open(path, encoding="utf-8") as f:
        for lineno, raw in enumerate(f, 1):
            line = raw.rstrip("\n").rstrip("\r")
            if block_key is not None:
                if line.strip() == ">>":
                    data[block_key] = "\n".join(block_lines).strip("\n")
                    block_key, block_lines = None, []
                else:
                    block_lines.append(line.rstrip())
                continue
            s = line.strip()
            if not s or s.startswith("#"):
                cur_list_key = None if not s else cur_list_key
                continue
            if s.startswith("- ") and cur_list_key is not None:
                data[cur_list_key].append(s[2:].strip())
                continue
            if s.endswith("<<") and "=" not in s:
                block_key = s[:-2].strip()
                cur_list_key = None
                continue
            if s.endswith(":") and "=" not in s and " " not in s[:-1]:
                cur_list_key = s[:-1].strip()
                data[cur_list_key] = []
                continue
            if "=" in s:
                k, v = s.split("=", 1)
                k = k.strip()
                if " " in k:
                    raise SystemExit(f"{path}:{lineno}: bad key '{k}'")
                data[k] = v.strip()
                cur_list_key = None
                continue
            raise SystemExit(f"{path}:{lineno}: cannot parse line: {line!r}")
    if block_key is not None:
        raise SystemExit(f"{path}: unterminated block {block_key}")
    return data


def load_lang(lang):
    d = {}
    folder = os.path.join(SRC, lang)
    if not os.path.isdir(folder):
        return None
    for name in sorted(os.listdir(folder)):
        if name.endswith(".txt"):
            part = parse_file(os.path.join(folder, name))
            dup = set(part) & set(d)
            if dup:
                raise SystemExit(f"{lang}/{name}: duplicate keys {sorted(dup)[:5]}")
            d.update(part)
    return d


def strings_of(v):
    return v if isinstance(v, list) else [v]


def validate(lang, d, en):
    errs = []
    missing = [k for k in en if k not in d]
    extra = [k for k in d if k not in en]
    if missing:
        errs.append(f"missing {len(missing)} keys, e.g. {missing[:8]}")
    if extra:
        errs.append(f"unknown {len(extra)} keys, e.g. {extra[:8]}")
    for k, ev in en.items():
        if k not in d:
            continue
        v = d[k]
        if isinstance(ev, list) != isinstance(v, list):
            errs.append(f"{k}: type mismatch")
            continue
        if isinstance(ev, list) and len(ev) != len(v):
            errs.append(f"{k}: list length {len(v)} != {len(ev)}")
        eph = sorted(set(PLACEHOLDER.findall(" ".join(strings_of(ev)))))
        vph = sorted(set(PLACEHOLDER.findall(" ".join(strings_of(v)))))
        if eph != vph:
            errs.append(f"{k}: placeholders {vph} != {eph}")
        for s in strings_of(v):
            if not s.strip():
                errs.append(f"{k}: empty string")
            stripped = GENDER.sub("", PLACEHOLDER.sub("", s))
            if "{" in stripped or "}" in stripped:
                errs.append(f"{k}: malformed braces in {s!r}")
    return errs


def main():
    check_only = "--check" in sys.argv
    en = load_lang("en")
    if en is None:
        raise SystemExit("English source missing")
    os.makedirs(OUT, exist_ok=True)
    ok = True
    for lang in LANGS:
        d = en if lang == "en" else load_lang(lang)
        if d is None:
            print(f"[{lang}] (no source yet)")
            ok = False
            continue
        errs = [] if lang == "en" else validate(lang, d, en)
        n_str = sum(len(strings_of(v)) for v in d.values())
        if errs:
            ok = False
            print(f"[{lang}] {len(errs)} problem(s):")
            for e in errs[:40]:
                print("   ", e)
        else:
            print(f"[{lang}] OK  keys={len(d)} strings={n_str}")
        if not check_only:
            # Fall back to English for anything missing so the app never shows raw keys.
            merged = dict(en)
            merged.update({k: v for k, v in d.items() if k in en})
            with open(os.path.join(OUT, f"{lang}.json"), "w", encoding="utf-8") as f:
                json.dump(merged, f, ensure_ascii=False, separators=(",", ":"))
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
