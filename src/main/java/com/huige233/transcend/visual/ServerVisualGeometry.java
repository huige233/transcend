package com.huige233.transcend.visual;

import com.huige233.transcend.network.S2CParticleBatchPack;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class ServerVisualGeometry {
    private ServerVisualGeometry() {}

    public static List<S2CParticleBatchPack.ParticleEntry> circle(
            double cx, double cy, double cz, double radius, int points,
            double rotationAngle, Vector3f axis) {
        List<S2CParticleBatchPack.ParticleEntry> entries = new ArrayList<>();
        double nx = axis.x(), ny = axis.y(), nz = axis.z();
        double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length < 1.0E-6) {
            ny = 1.0;
            length = 1.0;
        }
        nx /= length;
        ny /= length;
        nz /= length;
        Vector3f u = perpendicular(nx, ny, nz);
        Vector3f v = cross(nx, ny, nz, u.x(), u.y(), u.z());
        for (int i = 0; i < points; i++) {
            double angle = rotationAngle + 2.0 * Math.PI * i / points;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            entries.add(new S2CParticleBatchPack.ParticleEntry(
                    cx + radius * (cos * u.x() + sin * v.x()),
                    cy + radius * (cos * u.y() + sin * v.y()),
                    cz + radius * (cos * u.z() + sin * v.z())));
        }
        return entries;
    }

    public static List<S2CParticleBatchPack.ParticleEntry> line(
            double x1, double y1, double z1, double x2, double y2, double z2, int points) {
        List<S2CParticleBatchPack.ParticleEntry> entries = new ArrayList<>();
        for (int i = 0; i <= points; i++) {
            double t = (double) i / points;
            entries.add(new S2CParticleBatchPack.ParticleEntry(
                    x1 + t * (x2 - x1), y1 + t * (y2 - y1), z1 + t * (z2 - z1)));
        }
        return entries;
    }

    private static Vector3f perpendicular(double nx, double ny, double nz) {
        if (Math.abs(ny) < 0.9) {
            double length = Math.sqrt(nz * nz + nx * nx);
            return new Vector3f((float) (nz / length), 0, (float) (-nx / length));
        }
        double length = Math.sqrt(nz * nz + ny * ny);
        return new Vector3f(0, (float) (-nz / length), (float) (ny / length));
    }

    private static Vector3f cross(double ax, double ay, double az, double bx, double by, double bz) {
        return new Vector3f((float) (ay * bz - az * by), (float) (az * bx - ax * bz),
                (float) (ax * by - ay * bx));
    }
}
