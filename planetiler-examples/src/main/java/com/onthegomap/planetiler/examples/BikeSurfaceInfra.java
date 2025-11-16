package com.onthegomap.planetiler.cycling;

import com.onthegomap.planetiler.Planetiler;
import com.onthegomap.planetiler.Profile;
import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.config.Arguments;
import com.onthegomap.planetiler.reader.SourceFeature;
import java.nio.file.Path;
import java.util.Set;

public class BikeSurfaceInfra implements Profile {

  // Define surface types for asphalt category
  private static final Set<String> ASPHALT_SURFACES = Set.of(
    "asphalt",
    "paved",
    "concrete"
  );

  // Define surface types for gravel category (grade2+)
  private static final Set<String> GRAVEL_SURFACES = Set.of(
    "gravel",
    "fine_gravel",
    "pebblestone",
    "unpaved",
    "earth",
    "dirt",
    "ground",
    "grass"
  );

  // Highway types that appear at lower zoom levels (major roads)
  private static final Set<String> MAJOR_HIGHWAYS = Set.of(
    "trunk", "trunk_link",
    "primary", "primary_link",
    "secondary", "secondary_link"
  );

  // Medium priority highways
  private static final Set<String> MEDIUM_HIGHWAYS = Set.of(
    "tertiary", "tertiary_link",
    "unclassified",
    "cycleway"
  );

  // Low priority highways
  private static final Set<String> RESIDENTIAL_HIGHWAYS = Set.of(
    "residential",
    "living_street"
  );

  /**
   * Calculate minimum zoom level based on highway type, surface category, and other attributes
   */
  private int calculateMinZoom(String highway, String surfaceCategory, String bicycle,
                                String tracktype, String name) {
    // Special case: tracks with tracktype
    if ("track".equals(highway)) {
      // tracktype=grade1 is asphalt-like, but still show from zoom 9
      if ("grade1".equals(tracktype)) {
        return 9;
      }
      // grade2-grade5 (or unknown grade) tracks: show from zoom 9
      return 9;
    }

    // For asphalt surfaces
    if ("asphalt".equals(surfaceCategory)) {
      // Major highways (trunk, primary, secondary)
      if (MAJOR_HIGHWAYS.contains(highway)) {
        return 9;
      }
      // Medium priority: tertiary, unclassified, cycleway
      if (MEDIUM_HIGHWAYS.contains(highway)) {
        return 9;
      }
      // Residential streets
      if (RESIDENTIAL_HIGHWAYS.contains(highway)) {
        return 12;
      }
      // Service streets
      if ("service".equals(highway)) {
        return 14;
      }
      // Other paved ways (path, footway with bicycle access)
      if ("designated".equals(bicycle)) {
        return 12;
      }
      return 12;
    }

    // For gravel surfaces
    if ("gravel".equals(surfaceCategory)) {
      // Dedicated cycling infrastructure
      if ("cycleway".equals(highway) || "designated".equals(bicycle)) {
        return 9;
      }
      // Named paths/tracks (likely significant routes)
      if (name != null && !name.isEmpty()) {
        if ("path".equals(highway)) {
          return 10;
        }
      }
      // Regular paths and tracks
      if ("path".equals(highway)) {
        return 12;
      }
      // Service and footway with bicycle access
      if ("service".equals(highway) || "footway".equals(highway)) {
        return 14;
      }
      // Default for other gravel
      return 12;
    }

    return 12; // Default fallback
  }

  @Override
  public void processFeature(SourceFeature sourceFeature, FeatureCollector features) {
    // Only process ways (linestrings)
    if (!sourceFeature.canBeLine()) {
      return;
    }

    // Get highway tag
    String highway = (String) sourceFeature.getTag("highway");
    if (highway == null) {
      return;
    }

    // Exclude motorways, motorway links, and footways
    if ("motorway".equals(highway) || "motorway_link".equals(highway) || "footway".equals(highway)) {
      return;
    }

    // Exclude if bicycle access is explicitly denied
    String bicycle = (String) sourceFeature.getTag("bicycle");
    String access = (String) sourceFeature.getTag("access");

    if ("no".equals(bicycle) || "no".equals(access)) {
      return;
    }

    // Get surface and tracktype tags
    String surface = (String) sourceFeature.getTag("surface");
    String tracktype = (String) sourceFeature.getTag("tracktype");
    String name = (String) sourceFeature.getTag("name");

    // Determine surface category
    String surfaceCategory = null;

    // Special handling for tracks with tracktype but no surface
    if ("track".equals(highway) && surface == null && tracktype != null) {
      // grade1 is asphalt-like
      if ("grade1".equals(tracktype)) {
        surfaceCategory = "asphalt";
        surface = "compacted"; // Implicit surface for grade1
      }
      // grade2-5 without explicit surface tag
      else if (tracktype.matches("grade[2-5]")) {
        surfaceCategory = "gravel";
        surface = "compacted"; // Implicit surface for gravel tracks
      }
    }

    // Check explicit surface tag
    if (surface != null && surfaceCategory == null) {
      if (ASPHALT_SURFACES.contains(surface)) {
        surfaceCategory = "asphalt";
      } else if (GRAVEL_SURFACES.contains(surface)) {
        surfaceCategory = "gravel";
      }
    }

    // Only process if we have a surface category
    if (surfaceCategory != null) {
      int minZoom = calculateMinZoom(highway, surfaceCategory, bicycle, tracktype, name);

      // Calculate minimum pixel size based on zoom level
      // At lower zoom levels, filter out very short segments to reduce tile size
      int minPixelSize = 0;
      if (minZoom < 11) {
        minPixelSize = 4; // Filter short segments at low zoom
      } else if (minZoom < 12) {
        minPixelSize = 2; // Some filtering at medium zoom
      }
      // At zoom 12+, show all segments (minPixelSize = 0)

      // Create separate layers for asphalt and gravel
      String layerName = "asphalt".equals(surfaceCategory) ? "surface_asphalt" : "surface_gravel";

      features.line(layerName)
        .setAttr("osm_type", "way")
        .setAttr("osm_id", sourceFeature.id())
        .setAttr("surface", surface)
        .setAttr("surface_category", surfaceCategory)
        .setAttr("highway", highway)
        .setAttr("name", name)
        .setAttr("tracktype", tracktype)
        .setMinZoom(minZoom)
        .setMinPixelSize(minPixelSize);
    }
  }

  @Override
  public String name() {
    return "Bicycle Surface Infrastructure";
  }

  @Override
  public String description() {
    return "Showing bicycle-accessible ways categorized by surface type (asphalt/paved/concrete vs gravel/unpaved)";
  }

  @Override
  public boolean isOverlay() {
    return true;
  }

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
      .setProfile(new BikeSurfaceInfra())
      // override this default with osm_path="path/to/data.osm.pbf"
      .addOsmSource("osm", Path.of("data", "sources", area + ".osm.pbf"), "geofabrik:" + area)
      // override this default with mbtiles="path/to/output.mbtiles"
      .overwriteOutput(Path.of("data", "bike-surface.mbtiles"))
      .run();
  }
}
