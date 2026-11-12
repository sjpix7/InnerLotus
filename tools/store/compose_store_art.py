"""Compose the Play Store screenshots and feature graphic in the glass-lotus style.

Usage: python3 tools/store/compose_store_art.py <raw_screens_dir>
  <raw_screens_dir> holds 1080x2400 emulator captures named 01_home.png, 02_areas.png,
  03_chakra_pick.png, 04_deck.png, 05_eft_rating.png, 07_sound.png, 09_journal.png, 12_calm.png.
Writes docs/store/screenshots/0N-*.png (1080x1920) and docs/store/feature-graphic-1024x500.png.
Needs Pillow (pip install pillow).
"""
import os, sys
from PIL import Image, ImageDraw, ImageFont, ImageFilter
R=os.path.abspath(os.path.join(os.path.dirname(__file__), "../..")) + "/"
RAW=sys.argv[1] if len(sys.argv) > 1 else sys.exit(__doc__)
OUT=R+"docs/store/"
os.makedirs(OUT+"screenshots", exist_ok=True)
INK=(48,44,67); SOFT=(87,79,106); ACC=(119,96,169)
def font(name, size, w):
    f=ImageFont.truetype(R+"app/src/main/res/font/"+name, size); f.set_variation_by_axes([w]); return f
serif=lambda s,w=500: font("cormorant_garamond.ttf",s,w)
italic=lambda s,w=500: font("cormorant_garamond_italic.ttf",s,w)
jost=lambda s,w=400: font("jost.ttf",s,w)
dawn=Image.open(R+"app/src/main/res/drawable-nodpi/bg_dawn.webp").convert("RGB")

def rich_width(d, parts):
    return sum(d.textlength(t, font=f) for t,f in parts)
def draw_rich(d, y, parts, W, fill):
    x=(W-rich_width(d,parts))/2
    for t,f in parts:
        d.text((x,y), t, font=f, fill=fill); x+=d.textlength(t,font=f)
def parse(title, size):
    # *word* -> italic
    out=[]; ital=False
    for i,seg in enumerate(title.split("*")):
        if seg: out.append((seg, italic(size) if i%2 else serif(size)))
    return out
def tracked(d, xy_center_y, text, f, W, fill, track):
    w=sum(d.textlength(c,font=f) for c in text)+track*(len(text)-1); x=(W-w)/2
    for c in text: d.text((x,xy_center_y),c,font=f,fill=fill); x+=d.textlength(c,font=f)+track

def frame(src, kicker, title_lines, out):
    W,H=1080,1920
    # background: the dawn photo, cover-scaled, softly blurred and veiled
    bg=dawn.resize((1440,1920)).crop((180,0,1260,1920)).filter(ImageFilter.GaussianBlur(18))
    bg=Image.blend(bg, Image.new("RGB",(W,H),(250,244,250)), 0.35)
    d=ImageDraw.Draw(bg)
    tracked(d, 120, kicker.upper(), jost(30,500), W, SOFT, 6)
    y=172
    for line in title_lines:
        draw_rich(d, y, parse(line,82), W, INK); y+=92
    # phone screenshot: drop status bar, keep app; rounded corners + soft shadow
    shot=Image.open(src).convert("RGB")
    shot=shot.crop((0,0,1080,2330))
    sw=760; sh=int(shot.height*sw/shot.width); shot=shot.resize((sw,sh), Image.LANCZOS)
    x=(W-sw)//2; top=y+56
    r=54
    mask=Image.new("L",(sw,sh),0); ImageDraw.Draw(mask).rounded_rectangle((0,0,sw-1,sh-1),r,fill=255)
    sh_layer=Image.new("RGBA",(W,H),(0,0,0,0)); ImageDraw.Draw(sh_layer).rounded_rectangle((x,top+14,x+sw,top+sh+14),r,fill=(70,50,90,70))
    sh_layer=sh_layer.filter(ImageFilter.GaussianBlur(26))
    bg=Image.alpha_composite(bg.convert("RGBA"), sh_layer)
    bg.paste(shot,(x,top),mask)
    # hairline light edge
    ImageDraw.Draw(bg).rounded_rectangle((x,top,x+sw-1,top+sh-1),r,outline=(255,255,255,200),width=3)
    bg.convert("RGB").save(OUT+"screenshots/"+out, optimize=True)

shots=[
 ("01_home","A calm, private companion",["Come back to *calm*."],"01-home.png"),
 ("02_areas","33 life areas · 1008 each",["Affirmations for","*every* part of life."],"02-affirmations.png"),
 ("03_chakra_pick","Guided by the seven chakras",["Begin where you","*need* it most."],"03-chakras.png"),
 ("04_deck","Swipe · listen · repeat",["Words to grow *into*."],"04-cards.png"),
 ("05_eft_rating","Guided EFT tapping",["Tapping, gently,","step by *step*."],"05-eft.png"),
 ("07_sound","Frequencies · nature · mixes",["Sounds that help","you *rest*."],"06-sound.png"),
 ("09_journal","Private · offline · no account",["A journal only","*you* can read."],"07-journal.png"),
 ("12_calm","Breathing & grounding",["Calm, in *one* tap."],"08-calm-now.png"),
]
for s,k,t,o in shots: frame(f"{RAW}/{s}.png",k,t,o)

# ---- feature graphic 1024x500 ----
W,H=1024,500
c=dawn.crop((100,640,1000,1334)); f=500/c.height; c=c.resize((int(c.width*f),500),Image.LANCZOS)
px=W-c.width
fg=Image.new("RGB",(W,H))
edge=c.crop((0,0,6,500)).filter(ImageFilter.GaussianBlur(2)).resize((px+200,500))
fg.paste(edge,(0,0))
full=Image.new("L",c.size,255); dm=ImageDraw.Draw(full)
for i in range(180): dm.line([(i,0),(i,499)],fill=int(255*(i/180)**1.5))
fg.paste(c,(px,0),full)
# soft light wash behind the text
wash=Image.new("L",(W,H),0); ImageDraw.Draw(wash).ellipse((-220,40,560,470),fill=150); wash=wash.filter(ImageFilter.GaussianBlur(70))
fg=Image.composite(Image.new("RGB",(W,H),(253,248,252)),fg,wash)
d=ImageDraw.Draw(fg)
logo=Image.open(R+"docs/design/prototype/assets/logo.png").convert("RGBA").resize((84,84),Image.LANCZOS)
lm=Image.new("L",(84,84),0); ImageDraw.Draw(lm).rounded_rectangle((0,0,83,83),20,fill=255)
la=Image.composite(logo.split()[3],Image.new("L",(84,84),0),lm)
fg.paste(logo,(64,118),la)
d.text((62,212),"Inner Lotus",font=serif(76,500),fill=INK)
d.text((64,300),"Come as you ",font=serif(36,500),fill=SOFT)
d.text((64+d.textlength("Come as you ",font=serif(36,500)),300),"are.",font=italic(36,500),fill=ACC)
x=65
for ch in "AFFIRMATIONS · EFT · SOUND · JOURNAL":
    d.text((x,368),ch,font=jost(15,500),fill=SOFT); x+=d.textlength(ch,font=jost(15,500))+2.4
fg.save(OUT+"feature-graphic-1024x500.png",optimize=True)
print("ok")
