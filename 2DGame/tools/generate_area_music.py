#!/usr/bin/env python3
"""
Generator musik latar per area (sintesis dari nol: tanpa sample, tanpa lagu orang lain).

Menghasilkan dua file di src/SFX/ :
    Frozen Silence.wav   -> musik Ice Land  (pad dingin, lonceng kaca, angin salju)
    Open Sea Drift.wav   -> musik Open Sea  (ombak, harpa petik, pad hangat, seruling pelan)

Semua lapisan dirender MELINGKAR (sinyal yang lewat ujung buffer disambung ke awal) dan reverb-nya konvolusi
melingkar, jadi ujung file nyambung mulus ke awal waktu dimainkan Clip.LOOP_CONTINUOUSLY (lihat SFX/MusicManager).

Mau ubah suasana? Edit konstanta di bagian "KOMPOSISI" (akor, pola nada, level tiap lapisan) lalu jalankan lagi:
    python tools/generate_area_music.py          (butuh numpy)
Format: WAV 16-bit mono 16 kHz, sama dengan sample rate lagu yang sudah ada di src/SFX.
"""
import os
import wave

import numpy as np

SR = 16000
BPM = 72
BEAT = 60.0 / BPM
BARS = 12                                   # 12 bar x 4 ketukan @ 72 BPM = 40 detik persis
N = int(round(BARS * 4 * BEAT * SR))
T = np.arange(N) / SR
LOOP_SEC = N / SR
CHORD_SEC = 3 * 4 * BEAT                    # tiap akor 3 bar = 10 detik, 4 akor per loop

RMS_TARGET_DB = -18.0                       # setara lagu lain di src/SFX (-15 s/d -18 dBFS)
PEAK_LIMIT = 0.708                          # -3 dBFS

HERE = os.path.dirname(os.path.abspath(__file__))
OUT_DIR = os.path.normpath(os.path.join(HERE, "..", "src", "SFX"))


# ============================================================ alat sintesis

def midi(n):
    return 440.0 * 2.0 ** ((n - 69) / 12.0)


def put(buf, sig, t0):
    """Tambah sig ke buf mulai detik t0, melingkar: ekor yang lewat ujung buffer nyambung ke awal."""
    i0 = int(round(t0 * SR))
    idx = (i0 + np.arange(len(sig))) % N
    buf[idx] += sig


def smooth_env(n, atk, rel):
    """Naik-turun halus (raised cosine) sepanjang n sampel."""
    a = min(max(1, int(atk * SR)), n // 2)
    r = min(max(1, int(rel * SR)), n - a)
    e = np.ones(n)
    e[:a] = 0.5 - 0.5 * np.cos(np.pi * np.arange(a) / a)
    e[n - r:] = np.minimum(e[n - r:], 0.5 + 0.5 * np.cos(np.pi * np.arange(r) / r))
    return e


def band_noise(n, lo, hi, rng):
    """Noise putih yang disaring ke pita lo-hi Hz lewat FFT (tepi halus), RMS = 1. Panjang N -> otomatis melingkar."""
    spec = np.fft.rfft(rng.standard_normal(n))
    f = np.fft.rfftfreq(n, 1.0 / SR)
    lo_e, hi_e = lo * 0.6, hi * 1.35
    up = np.clip((f - lo_e) / (lo - lo_e), 0.0, 1.0)
    dn = np.clip((hi_e - f) / (hi_e - hi), 0.0, 1.0)
    w = (0.5 - 0.5 * np.cos(np.pi * up)) * (0.5 - 0.5 * np.cos(np.pi * dn))
    y = np.fft.irfft(spec * w, n)
    return y / (np.std(y) + 1e-12)


def cyc(cycles, phase=0.0):
    """LFO 0..1 dengan jumlah siklus BULAT per loop (supaya mulus di sambungan)."""
    return 0.5 + 0.5 * np.sin(2.0 * np.pi * cycles * T / LOOP_SEC + phase)


def reverb(sig, rt60, damp_hz, seed):
    """Reverb konvolusi melingkar: respons impuls = noise meluruh, makin ke ekor makin tumpul."""
    rng = np.random.default_rng(seed)
    L = int(rt60 * 1.2 * SR)
    t = np.arange(L) / SR
    ir = rng.standard_normal(L) * np.exp(-6.91 * t / rt60)
    f = np.fft.rfftfreq(L, 1.0 / SR)
    ir = np.fft.irfft(np.fft.rfft(ir) / (1.0 + (f / damp_hz) ** 2), L)
    ir /= np.sqrt(np.sum(ir ** 2))
    full = np.zeros(len(sig))
    full[:L] = ir
    return np.fft.irfft(np.fft.rfft(sig) * np.fft.rfft(full), len(sig))


def rms_db(x):
    return 20.0 * np.log10(np.sqrt(np.mean(x ** 2)) + 1e-12)


def level(x, db):
    """Skala sinyal supaya RMS-nya = db dBFS."""
    return x * 10.0 ** ((db - rms_db(x)) / 20.0)


# ============================================================ instrumen

def pad_note(f, dur, atk, rel, rng):
    """Satu nada pad: tiga sinus sedikit disetem (efek chorus) + vibrato sangat pelan."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    sig = np.zeros(n)
    for det in (-0.13, 0.0, 0.11):
        vib = 1.0 + 0.0015 * np.sin(2 * np.pi * 0.23 * t + rng.uniform(0, 6.28))
        ph = 2 * np.pi * np.cumsum((f + det) * vib) / SR + rng.uniform(0, 6.28)
        sig += np.sin(ph) + 0.35 * np.sin(2 * ph) + 0.12 * np.sin(3 * ph)
    return sig * smooth_env(n, atk, rel) / 3.0


def bell(f, dur):
    """Lonceng kaca: FM dengan rasio tidak harmonik (3,5) yang indeksnya meluruh, ekor panjang."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    idx = 2.2 * np.exp(-t / 0.9)
    car = np.sin(2 * np.pi * f * t + idx * np.sin(2 * np.pi * f * 3.5 * t))
    car += 0.25 * np.sin(2 * np.pi * f * 2.0 * t) * np.exp(-t / 1.2)
    env = np.exp(-t / 1.5) * (1.0 - np.exp(-t / 0.004))
    env *= np.minimum(1.0, (n - np.arange(n)) / (0.25 * SR))
    return car * env


def pluck(f, dur):
    """Petikan harpa: harmonik 1..8, harmonik tinggi meluruh lebih cepat."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    sig = np.zeros(n)
    for k in range(1, 9):
        if f * k > 0.45 * SR:
            break
        sig += (1.0 / k ** 1.3) * np.sin(2 * np.pi * f * k * t * (1 + 0.0004 * k * k)) * np.exp(-t * (1.2 + 0.9 * k))
    return sig * (1.0 - np.exp(-t / 0.003))


def flute(f, dur, rng):
    """Seruling lembut: sinus + sedikit harmonik, vibrato yang baru muncul setelah nada berjalan, hembusan napas."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    vib = 1.0 + 0.004 * np.minimum(1.0, t / 0.5) * np.sin(2 * np.pi * 5.2 * t)
    ph = 2 * np.pi * np.cumsum(f * vib) / SR
    sig = np.sin(ph) + 0.18 * np.sin(2 * ph) + 0.06 * np.sin(3 * ph)
    sig += 0.05 * band_noise(n, 1500, 5000, rng)
    return sig * smooth_env(n, 0.12, 0.55)


# ============================================================ KOMPOSISI

# --- Ice Land: A minor, dingin dan lapang. Akor: Am(add9) - Fmaj7 - C(add9) - Gsus2 (balik ke Am).
ICE_CHORDS = [
    [45, 52, 57, 60, 64, 71],
    [41, 48, 53, 57, 60, 64],
    [48, 55, 60, 64, 67, 74],
    [43, 50, 55, 57, 62, 71],
]
ICE_BELL_BEATS = [0, 2, 3.5, 5, 7, 8.5, 10]      # posisi lonceng dalam 12 ketukan satu akor
ICE_BELL_PICK = [0, 2, 1, 3, 2, 4, 3]            # nada akor (oktaf atas) yang dipakai tiap lonceng

# --- Open Sea: D mayor, lapang dan hangat. Akor: D - A - Bm - G.
SEA_CHORDS = [
    [50, 57, 62, 66, 69],
    [45, 52, 57, 61, 64],
    [47, 54, 59, 62, 66],
    [43, 50, 55, 59, 62],
]
SEA_ARP = [0, 1, 2, 3, 2, 1, 2, 3]               # pola petikan harpa per bar (8 nada 1/8)
SEA_MELODY = [                                    # (ketukan mulai dari awal loop, nada MIDI, panjang dalam ketukan)
    (2, 78, 3), (5, 76, 2), (7, 74, 4),
    (14, 81, 3), (17, 78, 2), (19, 76, 4),
    (26, 78, 3), (29, 83, 2), (31, 81, 4),
    (38, 83, 3), (41, 81, 2), (43, 78, 4),
]


def make_ice():
    rng = np.random.default_rng(2611)
    pad = np.zeros(N)
    for k, chord in enumerate(ICE_CHORDS):
        for m in chord:
            put(pad, pad_note(midi(m), CHORD_SEC + 3.0, 3.0, 3.0, rng), k * CHORD_SEC - 1.5)

    bells = np.zeros(N)
    for k, chord in enumerate(ICE_CHORDS):
        tones = [m + 12 for m in chord[2:]]       # empat nada atas akor, naik satu oktaf
        for b, p in zip(ICE_BELL_BEATS, ICE_BELL_PICK):
            vel = rng.uniform(0.55, 1.0)
            t0 = k * CHORD_SEC + b * BEAT + rng.uniform(-0.02, 0.02)
            put(bells, vel * bell(midi(tones[p % len(tones)]), 5.0), t0)

    gust = 0.25 + 0.75 * (0.7 * cyc(3, 1.0) + 0.3 * cyc(7, 4.0)) ** 1.5
    wind = band_noise(N, 250, 1100, rng) * gust + 0.22 * band_noise(N, 2500, 6500, rng) * gust ** 2

    pad, bells, wind = level(pad, -23), level(bells, -27), level(wind, -28)
    dry = pad + bells
    mix = dry + 0.9 * reverb(dry, 4.0, 3200, seed=7) + wind
    return mix


def make_sea():
    rng = np.random.default_rng(2612)
    pad = np.zeros(N)
    for k, chord in enumerate(SEA_CHORDS):
        for m in chord:
            put(pad, pad_note(midi(m), CHORD_SEC + 3.0, 3.0, 3.0, rng), k * CHORD_SEC - 1.5)

    harp = np.zeros(N)
    eighth = BEAT / 2.0
    for bar in range(BARS):
        chord = SEA_CHORDS[bar // 3]
        tones = chord[1:] if len(chord) >= 5 else chord      # lewati nada bass
        for i, p in enumerate(SEA_ARP):
            vel = (1.0 if i % 4 == 0 else 0.62) * rng.uniform(0.85, 1.0)
            t0 = bar * 4 * BEAT + i * eighth + rng.uniform(-0.008, 0.008)
            put(harp, vel * pluck(midi(tones[p % len(tones)] + 12), 2.6), t0)

    lead = np.zeros(N)
    for beat, m, ln in SEA_MELODY:
        put(lead, flute(midi(m), ln * BEAT + 0.6, rng), beat * BEAT)

    low = band_noise(N, 90, 700, rng)
    hiss = band_noise(N, 1800, 7000, rng)
    swell = 0.55 * cyc(4, 0.6) + 0.45 * cyc(5, 2.1)          # 10 dtk + 8 dtk, keduanya bulat per loop
    waves = low * (0.15 + 0.85 * swell ** 1.6) + 0.30 * hiss * swell ** 3.2

    pad, harp, lead, waves = level(pad, -26), level(harp, -24), level(lead, -29), level(waves, -22)
    dry = pad + harp + lead
    mix = dry + 0.55 * reverb(dry, 2.4, 4200, seed=11) + waves
    return mix


# ============================================================ keluaran

def finalize(x):
    x = x - np.mean(x)
    scale = 10.0 ** ((RMS_TARGET_DB - rms_db(x)) / 20.0)
    peak = np.max(np.abs(x)) * scale
    if peak > PEAK_LIMIT:
        scale *= PEAK_LIMIT / peak
    return x * scale


def write_wav(path, x):
    pcm = (np.clip(x, -1.0, 1.0) * 32767.0).astype("<i2")
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())


def report(name, x):
    d = np.diff(x)
    seam = abs(x[0] - x[-1]) / (np.std(d) + 1e-12)
    print("%-22s %.1f dtk  RMS %.1f dBFS  puncak %.1f dBFS  sambungan loop = %.2fx selisih sampel biasa"
          % (name, len(x) / SR, rms_db(x), 20 * np.log10(np.max(np.abs(x))), seam))


if __name__ == "__main__":
    os.makedirs(OUT_DIR, exist_ok=True)
    for fname, maker in (("Frozen Silence.wav", make_ice), ("Open Sea Drift.wav", make_sea)):
        audio = finalize(maker())
        write_wav(os.path.join(OUT_DIR, fname), audio)
        report(fname, audio)
