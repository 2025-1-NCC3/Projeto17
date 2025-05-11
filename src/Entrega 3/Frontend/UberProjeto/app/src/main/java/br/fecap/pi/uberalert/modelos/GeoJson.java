package br.fecap.pi.uberalert.modelos;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GeoJson {
    @SerializedName("type")
    private String type;

    @SerializedName("features")
    public List<Feature> features;

    public static class Feature {
        @SerializedName("geometry")
        private Geometry geometry;

        @SerializedName("properties")
        private Properties properties;

        // Getters
        public Geometry getGeometry() {
            return geometry;
        }

        public Properties getProperties() {
            return properties;
        }
    }

    public static class Geometry {
        @SerializedName("type")
        private String type;

        @SerializedName("coordinates")
        private Object coordinates;

        // Getters
        public String getType() {
            return type;
        }

        public Object getCoordinates() {
            return coordinates;
        }
    }

    public static class Properties {
        @SerializedName("name")
        private String name;

        // Getters
        public String getName() {
            return name;
        }
    }
}
