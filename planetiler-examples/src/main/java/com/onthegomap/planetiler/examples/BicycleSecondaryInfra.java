package com.onthegomap.planetiler.cycling;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.FeatureMerge;
import com.onthegomap.planetiler.Planetiler;
import com.onthegomap.planetiler.Profile;
import com.onthegomap.planetiler.VectorTile;
import com.onthegomap.planetiler.config.Arguments;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.onthegomap.planetiler.util.ZoomFunction;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class BicycleSecondaryInfra implements Profile {

  /*
   * Assign every bike parking a monotonically increasing ID so that we can limit output at low zoom levels to only the
   * highest ID parking nodes. Be sure to use thread-safe data structures any time a profile holds state since multiple
   * threads invoke processFeature concurrently.
   */
  private final AtomicInteger parkingNumber = new AtomicInteger(0);
  private final AtomicInteger chargingNumber = new AtomicInteger(0);

  @Override
  public void processFeature(SourceFeature sourceFeature, FeatureCollector features) {
    //////////////////////////////////////////////////////////
    //////////// Bicycle Parking   ///////////////////////////
    //////////////////////////////////////////////////////////

    if (sourceFeature.isPoint() && sourceFeature.hasTag("amenity", "bicycle_parking")) {
      int parkingNumberCount = parkingNumber.getAndIncrement();
      if (sourceFeature.hasTag("cargobike", "yes", "designated")) {
        parkingNumberCount = parkingNumber.get() + 40;
      }
      if (sourceFeature.hasTag("covered", "yes")) {
        parkingNumberCount = parkingNumber.get() + 60;
      }
      long capacity = sourceFeature.getLong("capacity");
      if (capacity > 0) {
        try {
          parkingNumberCount = Math.toIntExact(parkingNumber.get() * ((capacity / 100) + 1));
        } finally {
          parkingNumberCount = parkingNumberCount;
        }
      }
      features.point("parking")
        .setAttr("osm_type", sourceFeature.getTag("node"))
        .setAttr("osm_id", sourceFeature.id())
        .setAttr("cargobike", sourceFeature.getTag("cargo_bike"))
        .setAttr("type", sourceFeature.getTag("bicycle_parking"))
        .setAttr("type:position", sourceFeature.getTag("bicycle_parking:position"))
        .setAttr("capacity", sourceFeature.getTag("capacity"))
        .setAttr("capacity:cargobike", sourceFeature.getTag("capacity:cargo_bike"))
        .setAttr("short_name", sourceFeature.getTag("short_name"))
        .setAttr("name", sourceFeature.getTag("name"))
        .setAttr("costs", sourceFeature.getTag("fee"))
        .setAttr("covered", sourceFeature.getTag("covered"))
        .setAttr("opening_hours", sourceFeature.getTag("opening_hours"))
        .setAttr("access", sourceFeature.getTag("access"))
        // to limit parking displayed at lower zoom levels:
        // 1) set a sort key that defines a priority ordering of parkings. For mountains, you might use "elevation"
        // but for parkings we just set it to the order in which we see them.
        .setSortKey(parkingNumberCount)
        // 2) at lower zoom levels, divide each 256x256 px tile into 32x32 px squares and in each square only include
        // the parkings with the lowest sort key within that square
        .setPointLabelGridSizeAndLimit(
          10, // only limit at z12 and below
          16, // break the tile up into 32x32 px squares
          10 // any only keep the 4 nodes with lowest sort-key in each 32px square
          )
          .setMinZoom(5)
        // and also whenever you set a label grid size limit, make sure you increase the buffer size so no
        // label grid squares will be the consistent between adjacent tiles
        .setBufferPixelOverrides(ZoomFunction.maxZoom(12, 32));
    }
    if(sourceFeature.canBePolygon() && sourceFeature.hasTag("amenity", "bicycle_parking")) {
      features.polygon("parking-lines")
          .setAttr("osm_type", sourceFeature.getTag("way"))
          .setAttr("osm_id", sourceFeature.id())
          .setAttr("name", sourceFeature.getTag("name"))
          .setAttr("short_name", sourceFeature.getTag("short_name"))
          .setAttr("cargobike", sourceFeature.getTag("cargo_bike"))
          .setAttr("type", sourceFeature.getTag("bicycle_parking"))
          .setAttr("type:position", sourceFeature.getTag("bicycle_parking:position"))
          .setAttr("capacity", sourceFeature.getTag("capacity"))
          .setAttr("capacity:cargobike", sourceFeature.getTag("capacity:cargo_bike"))
          .setAttr("costs", sourceFeature.getTag("fee"))
          .setAttr("covered", sourceFeature.getTag("covered"))
          .setAttr("access", sourceFeature.getTag("access"))
          .setAttr("opening_hours", sourceFeature.getTag("opening_hours"))
          .setMinZoom(10)
          // don't filter out short line segments even at low zooms because the next step needs them
          // to merge lines with the same tags where the endpoints are touching
          .setMinPixelSize(0);
      // Add centered point in polygon for using as label in map client
      features.centroid("parking-labels")
        .setAttr("osm_type", sourceFeature.getTag("way"))
        .setAttr("osm_id", sourceFeature.id())
        .setAttr("name", sourceFeature.getTag("name"))
        .setAttr("cargobike", sourceFeature.getTag("cargo_bike"))
        .setAttr("capacity", sourceFeature.getTag("capacity"))
        .setAttr("short_name", sourceFeature.getTag("short_name"))
        .setAttr("covered", sourceFeature.getTag("covered"))
        .setAttr("access", sourceFeature.getTag("access"))
        .setAttr("opening_hours", sourceFeature.getTag("opening_hours"))
        .setMinZoom(5);
    }

    //////////////////////////////////////////////////////////
    //////////// Bicycle Charging ///////////////////////////
    //////////////////////////////////////////////////////////
    if (sourceFeature.isPoint() && sourceFeature.hasTag("amenity", "charging_station") && sourceFeature.hasTag("bicycle", "yes")) {
      int chargingNumberCount = chargingNumber.getAndIncrement();

      long capacity = sourceFeature.getLong("socket:schuko") + sourceFeature.getLong("socket:typee");
      if (capacity > 0) {
        try {
          chargingNumberCount = Math.toIntExact(chargingNumber.get() * (capacity+ 1));
        } finally {
          chargingNumberCount = chargingNumberCount;
        }
      }
      features.point("charging")
        .setAttr("osm_type", sourceFeature.getTag("node"))
        .setAttr("osm_id", sourceFeature.id())
        .setAttr("name", sourceFeature.getTag("name"))
        .setAttr("operator", sourceFeature.getTag("operator"))
        .setAttr("costs", sourceFeature.getTag("fee"))
        .setAttr("brand", sourceFeature.getTag("brand"))
        .setAttr("opening_hours", sourceFeature.getTag("opening_hours"))
        .setAttr("socket:schuko", sourceFeature.getTag("socket:schuko"))
        .setAttr("socket:bosch_3pin", sourceFeature.getTag("socket:bosch_3pin"))
        .setAttr("socket:bosch_5pin", sourceFeature.getTag("socket:bosch_5pin"))
        .setAttr("socket:shimano_steps_5pin", sourceFeature.getTag("socket:shimano_steps_5pin"))
        .setAttr("socket:ropd", sourceFeature.getTag("socket:ropd"))
        .setAttr("socket:typee", sourceFeature.getTag("socket:typee"))
        // to limit parking displayed at lower zoom levels:
        // 1) set a sort key that defines a priority ordering of parkings. For mountains, you might use "elevation"
        // but for parkings we just set it to the order in which we see them.
        .setSortKey(chargingNumberCount)
        // 2) at lower zoom levels, divide each 256x256 px tile into 32x32 px squares and in each square only include
        // the parkings with the lowest sort key within that square
        .setPointLabelGridSizeAndLimit(
          10, // only limit at z12 and below
          16, // break the tile up into 32x32 px squares
          10 // any only keep the 4 nodes with lowest sort-key in each 32px square
        )
        .setMinZoom(5)
        // and also whenever you set a label grid size limit, make sure you increase the buffer size so no
        // label grid squares will be the consistent between adjacent tiles
        .setBufferPixelOverrides(ZoomFunction.maxZoom(12, 32));
    }


    //////////////////////////////////////////////////////////
    //////////// Bicycle Repair Station //////////////////////
    //////////////////////////////////////////////////////////
    if (sourceFeature.isPoint() && sourceFeature.hasTag("amenity", "bicycle_repair_station")) {

      features.point("repairstation")
        .setAttr("osm_type", sourceFeature.getTag("node"))
        .setAttr("osm_id", sourceFeature.id())
        .setAttr("name", sourceFeature.getTag("name"))
        .setAttr("short_name", sourceFeature.getTag("short_name"))
        .setAttr("bicycle_pump", sourceFeature.getTag("service:bicycle:pump"))
        .setAttr("tools", sourceFeature.getTag("service:bicycle:tools"))
        .setAttr("compressed_air", sourceFeature.getTag("compressed_air"))
        .setAttr("opening_hours", sourceFeature.getTag("opening_hours"))
        .setMinZoom(5)
        // 2) at lower zoom levels, divide each 256x256 px tile into 32x32 px squares and in each square only include
        // the parkings with the lowest sort key within that square
        .setPointLabelGridSizeAndLimit(
          10, // only limit at z12 and below
          16, // break the tile up into 32x32 px squares
          10 // any only keep the 4 nodes with lowest sort-key in each 32px square
        )
        // and also whenever you set a label grid size limit, make sure you increase the buffer size so no
        // label grid squares will be the consistent between adjacent tiles
        .setBufferPixelOverrides(ZoomFunction.maxZoom(12, 32));
    }

    //////////////////////////////////////////////////////////
    ////////////     Compressed Air    //////////////////////
    //////////////////////////////////////////////////////////
    if (sourceFeature.isPoint() && sourceFeature.hasTag("amenity", "compressed_air")) {

      features.point("compressed_air")
        .setAttr("osm_type", sourceFeature.getTag("node"))
        .setAttr("osm_id", sourceFeature.id())
        .setAttr("name", sourceFeature.getTag("name"))
        .setAttr("access", sourceFeature.getTag("access"))
        .setAttr("costs", sourceFeature.getTag("fee"))
        .setAttr("valves", sourceFeature.getTag("valves"))
        .setAttr("opening_hours", sourceFeature.getTag("opening_hours"))
        .setMinZoom(5)

        // 2) at lower zoom levels, divide each 256x256 px tile into 32x32 px squares and in each square only include
        // the parkings with the lowest sort key within that square
        .setPointLabelGridSizeAndLimit(
          10, // only limit at z12 and below
          16, // break the tile up into 32x32 px squares
          10 // any only keep the 4 nodes with lowest sort-key in each 32px square
        )
        // and also whenever you set a label grid size limit, make sure you increase the buffer size so no
        // label grid squares will be the consistent between adjacent tiles
        .setBufferPixelOverrides(ZoomFunction.maxZoom(12, 32));
    }

    //////////////////////////////////////////////////////////
    ////////////   Highway maxspeeds    //////////////////////
    //////////////////////////////////////////////////////////
    String highestMaxspeed = getHighestMaxspeed(sourceFeature);

    if (sourceFeature.canBeLine()
    && sourceFeature.getTag("highway") != null
    && (sourceFeature.getLong("maxspeed") >= 80 || sourceFeature.getLong("maxspeed:forward") >= 80 || sourceFeature.getLong("maxspeed:forward") >= 80 )
    && !(sourceFeature.hasTag("highway", "motorway_link") ||sourceFeature.hasTag("highway", "motorway") || sourceFeature.hasTag("motorroad", "yes")))  {
      features.line("maxspeed")
        .setAttr("maxspeed", highestMaxspeed)
        .setMinZoom(13);
    }
  }

  private String getHighestMaxspeed(SourceFeature sourceFeature) {
      // Retrieve the maxspeed values
      Optional<Integer> maxspeed = parseMaxspeed((String) sourceFeature.getTag("maxspeed"));
      Optional<Integer> maxspeedForward = parseMaxspeed((String) sourceFeature.getTag("maxspeed:forward"));
      Optional<Integer> maxspeedBackward = parseMaxspeed((String) sourceFeature.getTag("maxspeed:backward"));

      // Determine the highest value
      int highest = maxspeed.orElse(0);
      if (maxspeedForward.isPresent() && maxspeedForward.get() > highest) {
          highest = maxspeedForward.get();
      }
      if (maxspeedBackward.isPresent() && maxspeedBackward.get() > highest) {
          highest = maxspeedBackward.get();
      }

      return highest > 0 ? String.valueOf(highest) : null;
  }

  private Optional<Integer> parseMaxspeed(String maxspeed) {
      try {
          return Optional.ofNullable(maxspeed).map(Integer::parseInt);
      } catch (NumberFormatException e) {
          return Optional.empty();
      }
  }

  /*
   * Step 3)
   *
   * Before writing tiles to the output, first merge linestrings where the endpoints are touching that share the same
   * tags to improve line and text rendering in clients.
   */

  // @Override
  // public List<VectorTile.Feature> postProcessLayerFeatures(String layer, int zoom,
  //   List<VectorTile.Feature> items) {
  //   // FeatureMerge has several utilities for merging geometries in a layer that share the same tags.
  //   // `mergeLineStrings` combines lines with the same tags where the endpoints touch.
  //   // Tiles are 256x256 pixels and all FeatureMerge operations work in tile pixel coordinates.
  //   return FeatureMerge.mergeLineStrings(items,
  //     2, // in px: after merging, remove lines that are still less than px long
  //     1, // simplify output linestrings using a tolerance in px
  //     6 // remove any detail more than 4px outside the tile boundary
  //   );
  // }

  /*
   * Hooks to override metadata values in the output mbtiles file. Only name is required, the rest are optional. Bounds,
   * center, minzoom, maxzoom are set automatically based on input data and planetiler config.
   *
   * See: https://github.com/mapbox/mbtiles-spec/blob/master/1.3/spec.md#metadata)
   */

  @Override
  public String name() {
    return "Bicycle Secondary Infra";
  }

  @Override
  public String description() {
    return "Showing different kinds of parking, pumps and charging facilities for bicycles";
  }

  @Override
  public boolean isOverlay() {
    return true;
  }

  /*
   * Any time you use OpenStreetMap data, you must ensure clients display the following copyright. Most clients will
   * display this automatically if you populate it in the attribution metadata in the mbtiles file:
   */
  @Override
  public String attribution() {
    return """
      <a href="https://www.openstreetmap.org/copyright" target="_blank">&copy; OpenStreetMap contributors</a>
      """.trim();
  }

  /*
   * Main entrypoint for the example program
   */
  public static void main(String[] args) throws Exception {
    run(Arguments.fromArgsOrConfigFile(args));
  }

  static void run(Arguments args) throws Exception {
    String area = args.getString("area", "geofabrik area to download", "germany");
    Planetiler.create(args)
      .setProfile(new BicycleSecondaryInfra())
      // override this default with osm_path="path/to/data.osm.pbf"
      .addOsmSource("osm", Path.of("data", "sources", area + ".osm.pbf"), "geofabrik:" + area)
      // override this default with mbtiles="path/to/output.mbtiles"
      .overwriteOutput(Path.of("data", "bike-secondary-infra.mbtiles"))
      .run();
  }
}
