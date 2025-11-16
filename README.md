# Bike-Planetiler

**This is a specialized version of [Planetiler](https://github.com/onthegomap/planetiler) for visualizing bicycle infrastructure.**

All core functionalities are derived from the original Planetiler project. This repository includes additional profiles and examples specifically optimized for extracting and displaying bicycle-related data from OpenStreetMap.

## About Planetiler

Planetiler is a tool for generating [Vector Tiles](https://github.com/mapbox/vector-tile-spec/tree/master/2.1) from geographic data sources like [OpenStreetMap](https://www.openstreetmap.org/). It is fast and memory-efficient, allowing large areas to be processed on a single machine.

The generated vector tiles are output as [MBTiles](https://github.com/mapbox/mbtiles-spec/blob/master/1.3/spec.md) (SQLite) files and can be served using tools like [TileServer GL](https://github.com/maptiler/tileserver-gl) or [Martin](https://github.com/maplibre/martin).

**Upstream Project:** [github.com/onthegomap/planetiler](https://github.com/onthegomap/planetiler)

## Requirements

- Java 21+ (see [CONTRIBUTING.md](CONTRIBUTING.md))
- At least 1GB of free SSD space plus 5-10x the size of the `.osm.pbf` file
- At least 0.5x as much free RAM as the size of the `.osm.pbf` file

## Setup as Git Submodule

This repository is used as a Git submodule in other projects:

```bash
# Initialize submodule for the first time
git submodule update --init --recursive

# Update submodule to the latest version
git submodule update --remote --merge
```

## Usage

### 1. Build the Project

First, compile the project using Maven:

```bash
mvn clean package --file planetiler-examples/standalone.pom.xml
```

This creates an executable JAR file at:
```
planetiler-examples/target/planetiler-examples-HEAD-with-deps.jar
```

### 2. Obtain OSM Data

Download a `.osm.pbf` file for your desired area:
- **Geofabrik Downloads:** https://download.geofabrik.de/
  - e.g., Germany: https://download.geofabrik.de/europe/germany-latest.osm.pbf
  - e.g., Europe: https://download.geofabrik.de/europe-latest.osm.pbf
- **Smaller Extracts:** https://extract.bbbike.org/

Save the `.osm.pbf` file in a suitable directory (e.g., `osm_data/`).

### 3. Generate MBTiles

The following profiles are available:

#### Bike Routes (BikeRouteOverlay)
Extracts all marked bike routes from OSM:

```bash
java -Xmx25g -cp planetiler-examples/target/planetiler-examples-HEAD-with-deps.jar \
  com.onthegomap.planetiler.examples.BikeRouteOverlay \
  --osm_path=osm_data/germany-latest.osm.pbf \
  --mbtiles=output/bikeroutes.mbtiles
```

#### Bicycle Infrastructure (BicycleSecondaryInfra)
Extracts parking, charging stations, repair stations, and speed limits:

```bash
java -Xmx25g -cp planetiler-examples/target/planetiler-examples-HEAD-with-deps.jar \
  com.onthegomap.planetiler.cycling.BicycleSecondaryInfra \
  --osm_path=osm_data/germany-latest.osm.pbf \
  --mbtiles=output/bike-secondary-infra.mbtiles
```

#### Cycle Highways (BikeCyclehighways)
Extracts cycle highways and bicycle streets:

```bash
java -Xmx25g -cp planetiler-examples/target/planetiler-examples-HEAD-with-deps.jar \
  com.onthegomap.planetiler.cycling.BikeCyclehighways \
  --osm_path=osm_data/germany-latest.osm.pbf \
  --mbtiles=output/cyclehighways.mbtiles
```

#### Surfaces (BikeSurfaceInfra)
Extracts paths by surface type (asphalt vs. gravel):

```bash
java -Xmx25g -cp planetiler-examples/target/planetiler-examples-HEAD-with-deps.jar \
  com.onthegomap.planetiler.cycling.BikeSurfaceInfra \
  --osm_path=osm_data/germany-latest.osm.pbf \
  --mbtiles=output/bike-surface.mbtiles
```

### Parameters

- `--osm_path`: Path to the `.osm.pbf` input file
- `--mbtiles`: Path to the `.mbtiles` output file (optional, default value is set)
- `-Xmx25g`: RAM limit for Java (adjust based on available RAM and OSM file size)


### Schnell & einfach Tiles anschauen (Quickstart)

Nach dem Erzeugen der MBTiles kannst du sie direkt lokal im Browser anschauen:

**Variante 1: tileserver-gl-light (empfohlen, schnell & einfach)**

```bash
npm install -g tileserver-gl-light
tileserver-gl-light output/bikeroutes.mbtiles
```

**Variante 2: Docker (keine Installation nötig)**

```bash
docker run --rm -it -v "$(pwd)/output":/data -p 8080:8080 maptiler/tileserver-gl -p 8080
```

**Im Browser öffnen:**

http://localhost:8080

Du kannst jede erzeugte MBTiles-Datei (z.B. `output/bike-surface.mbtiles`) einfach anstelle von `bikeroutes.mbtiles` angeben.

## Profile Details

### BikeRouteOverlay
- **Layer:** `bikeroutes`
- **Source Tags:** `route=bicycle`, `network` (international/national/regional/local)
- **Output:** Lines for bike routes with network classification
- **Zoom Levels:** 5-14

### BicycleSecondaryInfra
- **Layer:** `parking`, `charging`, `repairstation`, `compressed_air`, `maxspeed`
- **Source Tags:**
  - `amenity=bicycle_parking`
  - `amenity=charging_station` + `bicycle=yes`
  - `amenity=bicycle_repair_station`
  - `amenity=compressed_air`
  - `maxspeed >= 80` (excluding motorways)
- **Output:** Points and polygons for infrastructure
- **Zoom Levels:** 5-14

### BikeCyclehighways
- **Layer:** `cyclehighways`
- **Source Tags:** `cycle_highway=yes/proposed`, `cyclestreet=yes`, `bicycle_road=yes`
- **Output:** Lines for cycle highways and bicycle streets
- **Zoom Levels:** 5-14

### BikeSurfaceInfra
- **Layer:** `surface`
- **Source Tags:**
  - `surface=asphalt/paved/concrete` → `surface_category=asphalt`
  - `surface=gravel/fine_gravel/pebblestone/unpaved/earth/dirt/ground/grass` → `surface_category=gravel`
- **Filter:** Excludes `bicycle=no`, `access=no`, `motorway`
- **Output:** Lines categorized by surface type
- **Zoom Levels:** 8-14

## License

This project is based on [Planetiler](https://github.com/onthegomap/planetiler) and uses the same license.

The data comes from [OpenStreetMap](https://www.openstreetmap.org/copyright) and is subject to the ODbL license.

## Credits

- [Planetiler](https://github.com/onthegomap/planetiler) - The underlying tool
- [OpenStreetMap Contributors](https://www.openstreetmap.org/copyright) - The data source
- [PMTiles](https://github.com/protomaps/PMTiles) optimized tile storage format
- [Apache Parquet](https://github.com/apache/parquet-mr) to support reading geoparquet files in Java (with dependencies minimized by [parquet-floor](https://github.com/strategicblue/parquet-floor))

See [NOTICE.md](NOTICE.md) for a full list and license details.

## Author

Planetiler was created by [Michael Barry](https://github.com/msbarry) for future use generating custom basemaps or overlays for [On The Go Map](https://onthegomap.com).

## License and Attribution

Planetiler source code is licensed under the [Apache 2.0 License](LICENSE), so it can be used and modified in commercial or other open source projects according to the license guidelines.

Maps built using Planetiler do not require any special attribution, but the data or schema used might. Any maps generated from OpenStreetMap data must [visibly credit OpenStreetMap contributors](https://www.openstreetmap.org/copyright). Any map generated with the profile based on OpenMapTiles or a derivative must [visibly credit OpenMapTiles](https://github.com/openmaptiles/openmaptiles/blob/master/LICENSE.md#design-license-cc-by-40) as well.

