# Perené ECF — modelo de ejemplo para Chawpi

Modelo de metadata para el catastro fiscal (ECF) del distrito de Perené (ubigeo 120302, Junín,
EPSG:32718), a partir de `120302_MD_Perene_ECF.gdb`. Aquí hay `model.json` (13 Custom Objects, 17
relaciones, 15 enums), `apply.py` (lo crea en Core vía REST) y sus tests (`test_apply.py`,
`test_model.py`). No hay carga de datos: Core no tiene import masivo, así que este ejemplo crea
solo la metadata; cargar los registros del GDB es trabajo de otra herramienta (ver
[Cargar los datos](#cargar-los-datos-fuera-de-este-ejemplo)).

## Requisitos

- Core corriendo: el servidor de `gis-sample` (`CHAWPI_DB_PORT=<puerto> ./gradlew :gis-sample-server:bootRun`
  desde la raíz de `chawpi`, ver [../README.md](../README.md)), escuchando en `:8090`. Usuario dev
  `admin@chawpi.local` / `admin` (seed de desarrollo, `chawpi.seed.dev`, activo por defecto en el ejemplo).
- Python 3.11+, sin dependencias (`apply.py` usa solo stdlib: `urllib`, `json`, `argparse`).
- GDAL (`ogr2ogr`, `ogrinfo`) solo si vas a preparar datos para cargar; no lo necesita `apply.py`.

## Uso

```bash
cd examples/gis-sample/perene

# valida model.json contra las reglas de Core sin llamar a nada
python3 apply.py --validate-only

# imprime los payloads POST que se enviarían, sin llamar a Core
python3 apply.py --dry-run

# crea objetos y relaciones en Core
python3 apply.py

# una segunda corrida es idempotente: cada objeto/relación existente se salta
python3 apply.py
# done: 0 created, 30 skipped

# borra relaciones y objetos, en orden inverso
python3 apply.py --drop
# done: 30 deleted, 0 skipped
```

Flags (`apply.py --help`):

| Flag | Default | Notas |
|---|---|---|
| `--model` | `model.json` | ruta al modelo |
| `--core` | `http://localhost:8090` (o `$CHAWPI_CORE`) | URL base de Core |
| `--email` | `admin@chawpi.local` (o `$CHAWPI_EMAIL`) | usuario de login |
| `--password` | `admin` (o `$CHAWPI_PASSWORD`) | password de login |
| `--dry-run` | — | imprime los payloads, no llama a Core |
| `--drop` | — | borra en vez de crear |
| `--validate-only` | — | solo valida `model.json`, no llama a Core |

Códigos de salida: `0` todo bien, `1` error de Core (login sin token, un fallo de conexión, o
cualquier respuesta HTTP no exitosa), `2` `model.json` no pasa la validación. Los POST de
objetos/relaciones toleran solo el 409 ("ya existe" → se salta); el PUT que marca la relación
`required` no tolera nada, ni siquiera 409.

Tests: `python3 -m unittest -v` (`test_apply.py` levanta un servidor HTTP fake en un hilo y graba las
peticiones; `test_model.py` valida `model.json` contra las reglas de Core: regex de nombres,
longitudes, keywords SQL, opciones de ENUM, targets existentes, orden topológico).

## Qué contiene el GDB

| Capa | Geometría | Filas | Destino |
|---|---|---|---|
| `CF_SECTOR_18` | MultiPolygon | 12 | `sector` |
| `CF_UNIDADES_URBANAS_18` | MultiPolygon | 18 | `unidad_urbana` |
| `CF_MANZANA_CAT_18` | MultiPolygon | 323 | `manzana_catastral` |
| `CF_MANZANA_URB_18` | MultiPolygon | 341 | `manzana_urbana` |
| `CF_LOTES_POL_18` | MultiPolygon | 3 977 | `lote` |
| `CF_LOTES_PUN_18` | Point | 5 223 | omitida — derivada de `lote` (~1.3 puntos por lote), sin dato propio |
| `CF_PREDIO_18` | Point | 3 977 | `predio` |
| `CF_EJE_VIAL_18` | MultiLineString | 228 | `via` |
| `CF_SEG_VIAL_18` | MultiLineString | 644 | `segmento_via` |
| `CF_ARANCEL_18` | MultiLineString | 1 374 | `frente_arancelario` |
| `CAL_VL_ARANCEL_18` | MultiLineString | 1 374 | omitida como objeto — plegada en `valoracion` (`MATERIAL_<año>`, `SERVICIOS_<año>`) |
| `CF_PARQUES_18` | MultiPolygon | 16 | `parque` |
| `CF_EQUIPAMIENTO_18` | Point | 58 | `equipamiento` |
| `TB_VALORACION_HIST` | tabla | 12 366 | `valoracion` |
| `PADRON_PREDIOS` | tabla | 3 749 | `padron` |
| `fras_*` | tablas raster | 0 | omitidas — vacías |

## Modelo

13 objetos (11 espaciales + `valoracion` y `padron` sin geometría), creados en este orden (orden
topológico: cada target existe antes que su source):

| Objeto | Etiqueta | Geometría | Origen (capa) | Relaciones → |
|---|---|---|---|---|
| `sector` | Sector | MULTIPOLYGON | `CF_SECTOR_18` | — |
| `unidad_urbana` | Unidad urbana | MULTIPOLYGON | `CF_UNIDADES_URBANAS_18` | — |
| `via` | Vía | MULTILINESTRING | `CF_EJE_VIAL_18` | — |
| `manzana_catastral` | Manzana catastral | MULTIPOLYGON | `CF_MANZANA_CAT_18` | sector |
| `manzana_urbana` | Manzana urbana | MULTIPOLYGON | `CF_MANZANA_URB_18` | sector, unidad_urbana |
| `segmento_via` | Segmento vial | MULTILINESTRING | `CF_SEG_VIAL_18` | via, sector |
| `frente_arancelario` | Frente arancelario | MULTILINESTRING | `CF_ARANCEL_18` | segmento_via, manzana_catastral, unidad_urbana |
| `lote` | Lote | MULTIPOLYGON | `CF_LOTES_POL_18` | manzana_catastral, unidad_urbana, frente_arancelario |
| `predio` | Predio | POINT | `CF_PREDIO_18` | lote, manzana_catastral, frente_arancelario |
| `valoracion` | Valoración arancelaria | sin geometría | `TB_VALORACION_HIST` + `CAL_VL_ARANCEL_18` | frente_arancelario (**required**) |
| `parque` | Parque | MULTIPOLYGON | `CF_PARQUES_18` | sector |
| `equipamiento` | Equipamiento | POINT | `CF_EQUIPAMIENTO_18` | — |
| `padron` | Padrón | sin geometría | `PADRON_PREDIOS` | predio |

Lista de aristas (17, orden de `model.json`, generada de sus `relationships`):

```
manzana_catastral ──sector──► sector
manzana_urbana ──sector──► sector
manzana_urbana ──unidad_urbana──► unidad_urbana
segmento_via ──via──► via
segmento_via ──sector──► sector
frente_arancelario ──segmento_via──► segmento_via
frente_arancelario ──manzana_catastral──► manzana_catastral
frente_arancelario ──unidad_urbana──► unidad_urbana
lote ──manzana_catastral──► manzana_catastral
lote ──unidad_urbana──► unidad_urbana
lote ──frente_arancelario──► frente_arancelario
predio ──lote──► lote
predio ──manzana_catastral──► manzana_catastral
predio ──frente_arancelario──► frente_arancelario
valoracion ──frente_arancelario──► frente_arancelario (required)
parque ──sector──► sector
padron ──predio──► predio
```

`equipamiento` no tiene relaciones.

### Decisiones

- **`id_gdb` en todos los objetos**: cada objeto lleva un `id_gdb` INTEGER `unique` (el `OBJECTID` o
  `ID_*` de origen) para que quien cargue los datos pueda resolver relaciones mapeando
  `id_gdb → uuid` de Core sin depender de las claves de negocio.
- **ENUM = etiqueta del dominio, no el código**: los campos ENUM guardan el texto del dominio (p.
  ej. `AVENIDA`, no `01`). Dos correcciones de texto sobre el dominio original: `RECREATIVO O
  DEPORTIVO` sin la barra (`RECREATIVO/DEPORTIVO` no pasa la regex de opciones ENUM de Core) y
  `PROGRAM MUNICIP'AL DE VIVIENDA` → `PROGRAMA MUNICIPAL DE VIVIENDA`.
- **SRID 32718**: la geometría se guarda en UTM 18S (igual que el GDB); Core reproyecta a 4326 en
  la API de lectura, así que quien consuma la API no necesita reproyectar nada.
- **Columnas descartadas**: 100 % vacías, `Shape_*`, `COORD_X`/`COORD_Y`, `ZONA_UTM`, `UBIGEO`,
  `SEC_EJEC`, `ESTADO_INS`, `ABR_TUU`, `ABR_TVIA`, `DES_VIA`, `id_lote_sirv`.
- **Claves naturales conservadas como TEXT** (`cod_sect`, `cod_mzn`, `cod_uu`, `cod_via`, etc.)
  además de las relaciones: sirven para depurar y para el mapeo del cargador, aunque la relación
  use el `uuid`.
- **`padron` ↔ `predio` solo cruza por dirección**: `COD_PRE` está vacío en `PADRON_PREDIOS`, así
  que el vínculo usa `LLAVE = predio.direccion` (verificado: 3 716/3 749 filas del padrón cruzan).
- Hay **1 predio con coordenada fuera del distrito** (outlier al sur); el modelo no lo filtra, lo
  documenta para quien cargue los datos.

## Cargar los datos (fuera de este ejemplo)

Core no tiene import masivo: la única forma de crear registros es `POST
/api/objects/{objeto}/records` con `{"attributes": {...}, "geometries": {"geom": <GeoJSON>}}`, una
fila a la vez. El GeoJSON va en **EPSG:4326**; Core lo reproyecta a 32718 al guardar. Importante:
Core **no** promueve `Polygon` a `MultiPolygon` — hay que promoverlo al exportar.

**Orden de carga** (targets antes que sources, mismo orden que `apply.py`): `sector`,
`unidad_urbana`, `via`, `manzana_catastral`, `manzana_urbana`, `segmento_via`,
`frente_arancelario`, `lote`, `predio`, `valoracion`, `parque`, `equipamiento`, `padron`.

**Resolver relaciones**: al cargar cada objeto, guardar un mapa clave → `uuid` devuelto por Core
(`id_gdb → uuid`, o clave natural → `uuid` cuando el target se busca por su clave de negocio). El FK
se envía como el `uuid` del target en `attributes.<fieldName>`. Mapeo explícito por relación (17):

| Relación (source.fieldName → target) | Columna(s) en la capa origen | Se busca en el target por |
|---|---|---|
| `manzana_catastral.sector` → `sector` | `CF_MANZANA_CAT_18.COD_SECT` | `sector.codigo` |
| `manzana_urbana.sector` → `sector` | `CF_MANZANA_URB_18.COD_SECT` | `sector.codigo` |
| `manzana_urbana.unidad_urbana` → `unidad_urbana` | `CF_MANZANA_URB_18.COD_UU` | `unidad_urbana.codigo` |
| `segmento_via.via` → `via` | `CF_SEG_VIAL_18.ID_VIA` | `via.id_gdb` |
| `segmento_via.sector` → `sector` | `CF_SEG_VIAL_18.COD_SECT` | `sector.codigo` |
| `frente_arancelario.segmento_via` → `segmento_via` | `CF_ARANCEL_18.ID_SVIA` (7 % vacío → dejar nulo) | `segmento_via.id_gdb` |
| `frente_arancelario.manzana_catastral` → `manzana_catastral` | `CF_ARANCEL_18.COD_SECT` + `COD_MZN` | `manzana_catastral.cod_sect` + `cod_mzn` |
| `frente_arancelario.unidad_urbana` → `unidad_urbana` | `CF_ARANCEL_18.COD_UU` | `unidad_urbana.codigo` |
| `lote.manzana_catastral` → `manzana_catastral` | `CF_LOTES_POL_18.COD_SECT` + `COD_MZN` | `manzana_catastral.cod_sect` + `cod_mzn` |
| `lote.unidad_urbana` → `unidad_urbana` | `CF_LOTES_POL_18.COD_UU` | `unidad_urbana.codigo` |
| `lote.frente_arancelario` → `frente_arancelario` | `CF_LOTES_POL_18.ID_ARANC` | `frente_arancelario.id_gdb` |
| `predio.lote` → `lote` | `CF_PREDIO_18.ID_LOTE_P` | `lote.id_gdb` |
| `predio.manzana_catastral` → `manzana_catastral` | `CF_PREDIO_18.ID_MZN_C` | `manzana_catastral.id_gdb` |
| `predio.frente_arancelario` → `frente_arancelario` | `CF_PREDIO_18.ID_ARANC` | `frente_arancelario.id_gdb` |
| `valoracion.frente_arancelario` → `frente_arancelario` (obligatoria) | `TB_VALORACION_HIST.ID_ARANC` | `frente_arancelario.id_gdb` |
| `parque.sector` → `sector` | `CF_PARQUES_18.COD_SECT` | `sector.codigo` |
| `padron.predio` → `predio` (opcional) | `PADRON_PREDIOS.LLAVE` | `predio.direccion` (3 716/3 749 cruzan) |

**Dominios (código → etiqueta)**: `model.json` trae la lista de etiquetas válidas en `enums`, pero
no el mapeo código→etiqueta del GDB (los dominios de la gdb usan código numérico). Exportar la
tabla de dominio con `ogrinfo -fielddomain` (o desde el catálogo de dominios del GDB) y mapear
código→etiqueta en el cargador, contra la lista de `enums.<nombre>` de `model.json`.

Ejemplo `ogr2ogr` por tipo de capa:

```bash
# capa espacial: reproyectar a 4326 y promover a Multi*
ogr2ogr -f GeoJSON -t_srs EPSG:4326 -nlt PROMOTE_TO_MULTI \
  lotes.geojson 120302_MD_Perene_ECF.gdb CF_LOTES_POL_18

# tabla sin geometría
ogr2ogr -f CSV padron.csv 120302_MD_Perene_ECF.gdb PADRON_PREDIOS
```

**`valoracion`**: una fila por `(ID_ARANC, año)`, tomando `VAL_ACT` y `FUENTE` de
`TB_VALORACION_HIST` y enriqueciendo con `CAL_VL_ARANCEL_18` (columnas `MATERIAL_<año>`,
`SERVICIOS_<año>`). Es un unpivot; en pseudo-SQL (`ogrinfo -dialect sqlite -sql`):

```sql
-- por cada año 2018..2026 presente en CAL_VL_ARANCEL_18. ANO_EJEC es TEXT: el literal va entre comillas.
-- h.OBJECTID AS id_gdb funciona en el dialecto sqlite de ogrinfo; si el driver no expone OBJECTID
-- como columna seleccionable, usar h.rowid AS id_gdb (mismo valor en OpenFileGDB).
SELECT h.OBJECTID AS id_gdb, h.ID_ARANC, h.ANO_EJEC AS anio, h.VAL_ACT AS valor_arancel, h.FUENTE,
       c.MATERIAL_2018 AS material_via,   -- sustituir 2018 por el año de la fila
       c.SERVICIOS_2018 AS servicios
FROM TB_VALORACION_HIST h
JOIN CAL_VL_ARANCEL_18 c ON c.ID_ARANC = h.ID_ARANC
WHERE h.ANO_EJEC = '2018';
-- repetir por año y UNION ALL, o generar la consulta por año en el cargador
```

Verificado (solo lectura) contra `~/Downloads/120302_MD_Perene_ECF.gdb`:
`ogrinfo -ro <gdb> -dialect sqlite -sql "<SQL arriba> LIMIT 3"` — la primera fila trae `id_gdb = 6`,
que coincide con el FID de `TB_VALORACION_HIST` (`ogrinfo -ro <gdb> TB_VALORACION_HIST -fid 6` →
mismo `ID_ARANC` y `ANO_EJEC`).

**`padron`**: enlazar a `predio` por `LLAVE == predio.direccion`; si no cruza, dejar el campo
`predio` nulo en el `attributes` del POST (la relación es opcional).

**Limpieza** antes de enviar:

- `TIP_DOC`: trim (`"DNI "` → `"DNI"`).
- `LOTE_MEDITERRANEO`: `0`/`1` → boolean.
- `ANO_*`: string/float → entero.
- `Area_Terreno` (y `Area_terreno` en predio): string → decimal.
- **Vacío o solo-espacios → `null` (o el atributo omitido) en todo campo no-TEXT** (ENUM, INTEGER,
  DECIMAL, BOOLEAN): Core responde 400 a `""` ahí (`FieldValueCodec` en Core no acepta el string
  vacío para esos tipos). Casos conocidos en este GDB: `padron.grupo_uso` (`Grupo_uso_desc` vacío en
  3590/3749 filas de `PADRON_PREDIOS`), `padron.tipo_via` (`TIP_VIA` vacío en 62 filas),
  `padron.tipo_documento` (`TIP_DOC` solo-espacios en 30 filas → `""` después del trim → enviar
  `null`). Además: `MATERIAL_<año>` y `SERVICIOS_<año>` en `CAL_VL_ARANCEL_18` guardan **códigos** de
  dominio (`1`, `A`), no la etiqueta — mapear código→etiqueta con los dominios `MATERIAL_VIA` /
  `SERVICIO` del GDB antes de enviarlos (contra `enums.material_via` / `enums.servicios` de
  `model.json`).

## Verificación en GIS-XP

Después de `python3 apply.py`:

- `GET /api/objects` en Core lista los 13 objetos.
- `GET /api/gis/layers` lista 11 capas (las espaciales), todas con `srid` 32718.
- En GIS-XP (`../gisxp`, BFF en `:8095` + `yarn dev` en `:5174`), el home muestra 13 objetos (11
  espaciales) y el mapa ofrece 11 capas.
