#!/usr/bin/env python3
"""
Generator sprite "Werewolf" (manusia serigala daratan es).

Art-nya DIAMBIL dari gambar referensi  tools/werewolf_gen/werewolf_reference.png  (pose 3/4 menghadap kanan).
Gambar itu di-cutout, dipecah jadi bagian badan (kepala, torso, lengan atas/bawah, tangan kanan, paha/betis kiri-kanan,
cawat, bulu duri), lalu digerakkan seperti animasi skeletal 2D: kaki pakai 2-bone IK (telapak nempel tanah, lutut
nekuk sendiri), badan merunduk, kepala "mengunci" mangsa, cakar menyabet dengan efek goresan. Hasilnya di-downscale
ke resolusi game, palet warna dikunci (anti-flicker), alpha dikeras + outline 1px, jadi pixel-art kayak sprite lain.
Pandangan belakang (arah ATAS) disintesis dari tekstur bulu referensi.

Output: SATU sprite sheet  src/Aset/werewolf_sheet.png   (25 kolom x 4 baris, sel 136 x 100 -> 3400 x 400)
    baris  : right | left | back_right | back_left
    kolom  : idle x4 | walk x8 | attack x6 (frame ke-4 = cakaran, STRIKE_FRAME=3) | lunge x3 | dead x4

Jalankan dari folder 2DGame:
    pip install pillow numpy scipy opencv-python
    python tools/generate_werewolf.py                       # tulis src/Aset/werewolf_sheet.png
    python tools/generate_werewolf.py --preview out.png     # contact sheet 2x (tidak menulis sheet game)
    python tools/generate_werewolf.py --gif outdir          # GIF per gerakan buat dicek mata

Ganti pose/tempo animasi: edit tools/werewolf_gen/anim.py (walk_pose, attack_pose, ...). Ganti art: timpa
tools/werewolf_gen/werewolf_reference.png (kalau proporsi badannya beda, koordinat poligon di seg.py perlu disesuaikan).
Ukuran sel (CELL_W/H) & GROUND harus sama dengan Entitas/WerewolfSprite.java.
"""
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE / 'werewolf_gen'))

import sheet  # noqa: E402

ASET = HERE.parent / 'src' / 'Aset'


def main():
    argv = sys.argv[1:]
    s, rows = sheet.build_sheet()
    sheet.check_margins(rows)
    if '--preview' in argv:
        i = argv.index('--preview')
        out = Path(argv[i + 1]) if i + 1 < len(argv) else Path('werewolf_preview.png')
        sheet.contact_sheet(rows, out)
        return
    if '--gif' in argv:
        i = argv.index('--gif')
        sheet.gifs(rows, argv[i + 1] if i + 1 < len(argv) else 'werewolf_gifs')
        return
    ASET.mkdir(parents=True, exist_ok=True)
    path = ASET / 'werewolf_sheet.png'
    sheet.save_png(s, path)
    print('ditulis', path, s.size)


if __name__ == '__main__':
    main()
