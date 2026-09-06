# Manual 16px artwork

The casing, amethyst, five curved-fragment states and ivory stone were painted in Piskel on a 16 × 16 canvas using the pen, bucket, circle and rectangle tools through computer use operated by an AI agent, not a human artist. AI concept images are references only and are not runtime assets.

- `iron.piskel` and `ivory.piskel`: editable three-layer documents, five frames representing 0–4 pearls. The iron document was reopened in Piskel to verify the format and all five fragment states.
- `dimensional_core.png` and `dimensional_core.piskel`: the approved **Fractured Eye (B)** design, manually painted at 16 × 16 with ten colors. Three ivory Nether Star fragments clasp a green Eye of Ender split by a dark dimensional fissure. `dimensional_core-clipboard.json` preserves the lossless Piskel selection export; the PNG is the editable master used by the resource assembly script.
- `*.png`: native-resolution layers that can be opened in any pixel art editor.
- `manual-16.json`: lossless indexed copy of Piskel's selection clipboard output. `.` is transparent; other symbols index `colors` in base 36. This preserves the exact manually painted pixels. Piskel's file downloads did not reach the integrated browser, so its **Select → Copy** command was used for lossless extraction.

The script `tools/craft_visuals.py` reads the layer PNGs, composites them, creates nearest-neighbour rotations of the authored pointer and maps the same pixels onto cuboid relief. The 32 rotational frames are mechanical derivatives, not 32 separately painted drawings. It also packages the layers into the editable `.piskel` documents. It never derives silhouettes or shading from a drawing algorithm. The indexed clipboard archive is only a fallback for a missing layer PNG.

The portable body is 9 × 9 × 2 Minecraft model units, measured against the installed p1kl compass. Both stations use that body above their lodestone top. The four curved fragments are reused unchanged around each lateral amethyst; north fills first, followed by east, south and west, for 16 individually represented pearls. The top instrument has no pearl indicators.

To edit, open a `.piskel` document or the individual layer PNGs at native resolution. Keep the fragments on their separate layer. Export revised layers losslessly to the corresponding PNGs in this folder before running `python tools/craft_visuals.py`. The script reads these PNGs and refreshes the packaged documents; editing only a runtime PNG under `src/main/resources` will be overwritten. Keep at most 64 colors across the source layers for the model palette. Do not resize concept art into a final sprite.

Preview sheets in `build/visual-review` enlarge the pixels for inspection; the actual resource PNGs remain 16 × 16. Visual verification in Minecraft is separate from file-format and gameplay tests.
