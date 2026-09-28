package com.huige233.transcend.client;

/** Keeps a fixed logical layout inside the window without changing the user's GUI scale. */
record UiViewport(int width, int height, double scale) {
    static UiViewport fit(int width, int height, int minimumWidth, int minimumHeight) {
        double scale = Math.min(1.0, Math.min((double) Math.max(1, width) / minimumWidth,
                (double) Math.max(1, height) / minimumHeight));
        return new UiViewport((int) Math.ceil(width / scale), (int) Math.ceil(height / scale), scale);
    }

    double logical(double coordinate) { return coordinate / scale; }
    int pixel(int coordinate) { return (int) Math.floor(logical(coordinate)); }
}
