# Back-transliteration of Persian to Latin names

## COMP90049 Knowledge Technologies

University of Melbourne

Project 1 - Sem 1, 2017

Project specification can be read [here](2017S1-90049P1-spec.pdf).

Sem 1, 2017

## Editing

To enable various processing methods, uncomment the following lines:
(only one at a time - comment them after use and before moving onto the next method)
(Option 4 is enabled by default; thus those lines should be commented out before selecting another method)

- GED (Levenshtein): 96-113
- Soundex processing: 117-134
- GED (mod-r, mod-i): 96-113, 238-247, 303-318
- GED (mod i/r) + Soundex: 161-182 + ones for GED (mod-r, mod-i) above
- Soundex + GED (mod i/r): 117-134, 139-157, 238-247, 303-318

PS: If GED of any sort is applied as the last method e.g. in SX + GED or GED by itself:
Change variable name on line 196, 203 & 206 from sxScoreNamesMap to scoreNamesMap, and vice versa if GED is not the last method applied.

To process only a subset (evenly-distributed) sample from train.txt:
Change line 19 variable (proc_entire_file) from 'true' to 'false'
Line 20 variable (proc_limit) should be set to the number of records that should be processed

PS: This is the only time a valid output file will be generated [otherwise, the output becomes too large and is thus omitted]

## Compilation

Compile `BackTransliteration.java`. 

Include 'commons-codec-1.10.jar' library from `/lib`

## Running

Three file paths need to be provided as arguments to the file:

```java
java BackTransliteration <train.txt filepath> <names.txt filepath> <output filepath>
```

## Output

File as specified above; contains method type and predictions [only when proc_entire_file is set to false]
Performance metrics are printed in console in all cases.

---

Source files: `/src/BackTransliteration.java`

Data files must be in `/data`

- train.txt
- test.txt
- names.txt

Library files must be in `/lib`

- commons-codec-1.10.jar [for Soundex]

---