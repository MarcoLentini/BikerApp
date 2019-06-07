package com.example.bikerapp.Helper;

import com.google.firebase.firestore.GeoPoint;

public class Haversine {

    private static Double helpRad(Double v) { return v * Math.PI / 180; }

    public static Double getHaversineDistance(GeoPoint a, GeoPoint b) {
        final int R = 6371;
        Double lat1 = a.getLatitude();
        Double lat2 = b.getLatitude();

        Double lon1 = a.getLongitude();
        Double lon2 = b.getLongitude();

        Double latDist = helpRad(lat1 - lat2);
        Double lonDist = helpRad(lon1 - lon2);

        Double tmp = Math.sin(latDist / 2) * Math.sin(latDist / 2) +
                Math.cos(helpRad(lat1)) * Math.cos(helpRad(lat2)) *
                        Math.sin(lonDist / 2) * Math.sin(lonDist / 2);
        Double c = 2 * Math.atan2(Math.sqrt(tmp), Math.sqrt(1-tmp));

        return R * c;
    }
}
