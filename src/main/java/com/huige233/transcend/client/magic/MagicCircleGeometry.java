package com.huige233.transcend.client.magic;

import com.huige233.transcend.network.S2CParticleBatchPack;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class MagicCircleGeometry {

    public static List<S2CParticleBatchPack.ParticleEntry> buildCircle(double cx, double cy, double cz,
                                                                       double radius, int points,
                                                                       double rotationAngle,
                                                                       Vector3f axis) {
        List<S2CParticleBatchPack.ParticleEntry> entries = new ArrayList<>();
        double nx = axis.x(), ny = axis.y(), nz = axis.z();
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-6) { ny = 1; len = 1; }
        nx /= len; ny /= len; nz /= len;

        Vector3f u = perpendicular(nx, ny, nz);
        Vector3f v = cross(nx, ny, nz, u.x(), u.y(), u.z());

        for (int i = 0; i < points; i++) {
            double angle = rotationAngle + 2.0 * Math.PI * i / points;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double px = cx + radius * (cos * u.x() + sin * v.x());
            double py = cy + radius * (cos * u.y() + sin * v.y());
            double pz = cz + radius * (cos * u.z() + sin * v.z());
            entries.add(new S2CParticleBatchPack.ParticleEntry(px, py, pz));
        }
        return entries;
    }

    public static List<S2CParticleBatchPack.ParticleEntry> buildPolygon(double cx, double cy, double cz,
                                                                         double radius, int sides, int pointsPerEdge,
                                                                         double rotationAngle,
                                                                         Vector3f axis) {
        List<S2CParticleBatchPack.ParticleEntry> entries = new ArrayList<>();
        double nx = axis.x(), ny = axis.y(), nz = axis.z();
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-6) { ny = 1; len = 1; }
        nx /= len; ny /= len; nz /= len;

        Vector3f u = perpendicular(nx, ny, nz);
        Vector3f v = cross(nx, ny, nz, u.x(), u.y(), u.z());

        double[] vx = new double[sides];
        double[] vy = new double[sides];
        double[] vz = new double[sides];
        for (int i = 0; i < sides; i++) {
            double angle = rotationAngle + 2.0 * Math.PI * i / sides;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            vx[i] = cx + radius * (cos * u.x() + sin * v.x());
            vy[i] = cy + radius * (cos * u.y() + sin * v.y());
            vz[i] = cz + radius * (cos * u.z() + sin * v.z());
        }

        for (int i = 0; i < sides; i++) {
            int next = (i + 1) % sides;
            for (int j = 0; j < pointsPerEdge; j++) {
                double t = (double) j / pointsPerEdge;
                double px = vx[i] + t * (vx[next] - vx[i]);
                double py = vy[i] + t * (vy[next] - vy[i]);
                double pz = vz[i] + t * (vz[next] - vz[i]);
                entries.add(new S2CParticleBatchPack.ParticleEntry(px, py, pz));
            }
        }
        return entries;
    }

    public static List<S2CParticleBatchPack.ParticleEntry> buildLine(double x1, double y1, double z1,
                                                                      double x2, double y2, double z2,
                                                                      int points) {
        List<S2CParticleBatchPack.ParticleEntry> entries = new ArrayList<>();
        for (int i = 0; i <= points; i++) {
            double t = (double) i / points;
            entries.add(new S2CParticleBatchPack.ParticleEntry(
                    x1 + t * (x2 - x1),
                    y1 + t * (y2 - y1),
                    z1 + t * (z2 - z1)
            ));
        }
        return entries;
    }

    public static List<S2CParticleBatchPack.ParticleEntry> buildDottedCircle(double cx, double cy, double cz,
                                                                              double radius, int segments,
                                                                              int pointsPerSegment, double gapRatio,
                                                                              double rotationAngle,
                                                                              Vector3f axis) {
        List<S2CParticleBatchPack.ParticleEntry> entries = new ArrayList<>();
        double nx = axis.x(), ny = axis.y(), nz = axis.z();
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-6) { ny = 1; len = 1; }
        nx /= len; ny /= len; nz /= len;

        Vector3f u = perpendicular(nx, ny, nz);
        Vector3f v = cross(nx, ny, nz, u.x(), u.y(), u.z());

        double segAngle = 2.0 * Math.PI / segments;
        double drawAngle = segAngle * (1.0 - gapRatio);

        for (int s = 0; s < segments; s++) {
            double baseAngle = rotationAngle + s * segAngle;
            for (int p = 0; p < pointsPerSegment; p++) {
                double angle = baseAngle + drawAngle * p / pointsPerSegment;
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                double px = cx + radius * (cos * u.x() + sin * v.x());
                double py = cy + radius * (cos * u.y() + sin * v.y());
                double pz = cz + radius * (cos * u.z() + sin * v.z());
                entries.add(new S2CParticleBatchPack.ParticleEntry(px, py, pz));
            }
        }
        return entries;
    }

    public static List<S2CParticleBatchPack.ParticleEntry> buildRadialLines(double cx, double cy, double cz,
                                                                             double innerRadius, double outerRadius,
                                                                             int lineCount, int pointsPerLine,
                                                                             double rotationAngle,
                                                                             Vector3f axis) {
        List<S2CParticleBatchPack.ParticleEntry> entries = new ArrayList<>();
        double nx = axis.x(), ny = axis.y(), nz = axis.z();
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-6) { ny = 1; len = 1; }
        nx /= len; ny /= len; nz /= len;

        Vector3f u = perpendicular(nx, ny, nz);
        Vector3f v = cross(nx, ny, nz, u.x(), u.y(), u.z());

        for (int i = 0; i < lineCount; i++) {
            double angle = rotationAngle + 2.0 * Math.PI * i / lineCount;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            for (int j = 0; j <= pointsPerLine; j++) {
                double t = (double) j / pointsPerLine;
                double r = innerRadius + t * (outerRadius - innerRadius);
                double px = cx + r * (cos * u.x() + sin * v.x());
                double py = cy + r * (cos * u.y() + sin * v.y());
                double pz = cz + r * (cos * u.z() + sin * v.z());
                entries.add(new S2CParticleBatchPack.ParticleEntry(px, py, pz));
            }
        }
        return entries;
    }

    public static List<S2CParticleBatchPack.ParticleEntry> buildStar(double cx, double cy, double cz,
                                                                      double radius, int vertices, int skip,
                                                                      int pointsPerEdge,
                                                                      double rotationAngle,
                                                                      Vector3f axis) {
        List<S2CParticleBatchPack.ParticleEntry> entries = new ArrayList<>();
        double nx = axis.x(), ny = axis.y(), nz = axis.z();
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-6) { ny = 1; len = 1; }
        nx /= len; ny /= len; nz /= len;

        Vector3f u = perpendicular(nx, ny, nz);
        Vector3f v = cross(nx, ny, nz, u.x(), u.y(), u.z());

        double[] vx = new double[vertices];
        double[] vy = new double[vertices];
        double[] vz = new double[vertices];
        for (int i = 0; i < vertices; i++) {
            double angle = rotationAngle + 2.0 * Math.PI * i / vertices;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            vx[i] = cx + radius * (cos * u.x() + sin * v.x());
            vy[i] = cy + radius * (cos * u.y() + sin * v.y());
            vz[i] = cz + radius * (cos * u.z() + sin * v.z());
        }

        for (int i = 0; i < vertices; i++) {
            int next = (i + skip) % vertices;
            for (int j = 0; j < pointsPerEdge; j++) {
                double t = (double) j / pointsPerEdge;
                double px = vx[i] + t * (vx[next] - vx[i]);
                double py = vy[i] + t * (vy[next] - vy[i]);
                double pz = vz[i] + t * (vz[next] - vz[i]);
                entries.add(new S2CParticleBatchPack.ParticleEntry(px, py, pz));
            }
        }
        return entries;
    }

    public static List<S2CParticleBatchPack.ParticleEntry> buildSpiral(double cx, double cy, double cz,
                                                                        double innerRadius, double outerRadius,
                                                                        double turns, int totalPoints,
                                                                        double rotationAngle,
                                                                        Vector3f axis) {
        List<S2CParticleBatchPack.ParticleEntry> entries = new ArrayList<>();
        double nx = axis.x(), ny = axis.y(), nz = axis.z();
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-6) { ny = 1; len = 1; }
        nx /= len; ny /= len; nz /= len;

        Vector3f u = perpendicular(nx, ny, nz);
        Vector3f v = cross(nx, ny, nz, u.x(), u.y(), u.z());

        for (int i = 0; i < totalPoints; i++) {
            double t = (double) i / totalPoints;
            double angle = rotationAngle + t * turns * 2.0 * Math.PI;
            double r = outerRadius - t * (outerRadius - innerRadius);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double px = cx + r * (cos * u.x() + sin * v.x());
            double py = cy + r * (cos * u.y() + sin * v.y());
            double pz = cz + r * (cos * u.z() + sin * v.z());
            entries.add(new S2CParticleBatchPack.ParticleEntry(px, py, pz));
        }
        return entries;
    }

    public static List<S2CParticleBatchPack.ParticleEntry> buildCross(double cx, double cy, double cz,
                                                                       double armLength, double armWidth,
                                                                       int pointsPerArm,
                                                                       double rotationAngle,
                                                                       Vector3f axis) {
        List<S2CParticleBatchPack.ParticleEntry> entries = new ArrayList<>();
        double nx = axis.x(), ny = axis.y(), nz = axis.z();
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-6) { ny = 1; len = 1; }
        nx /= len; ny /= len; nz /= len;

        Vector3f u = perpendicular(nx, ny, nz);
        Vector3f v = cross(nx, ny, nz, u.x(), u.y(), u.z());

        for (int arm = 0; arm < 4; arm++) {
            double armAngle = rotationAngle + arm * Math.PI / 2.0;
            double dirX = Math.cos(armAngle);
            double dirZ = Math.sin(armAngle);
            double perpX = -dirZ;
            double perpZ = dirX;

            for (int i = 0; i <= pointsPerArm; i++) {
                double t = (double) i / pointsPerArm;
                double dist = t * armLength;

                double baseX = dirX * dist;
                double baseZ = dirZ * dist;

                for (int side = -1; side <= 1; side += 2) {
                    double ox = baseX + perpX * armWidth * side;
                    double oz = baseZ + perpZ * armWidth * side;
                    double px = cx + ox * u.x() + oz * v.x();
                    double py = cy + ox * u.y() + oz * v.y();
                    double pz = cz + ox * u.z() + oz * v.z();
                    entries.add(new S2CParticleBatchPack.ParticleEntry(px, py, pz));
                }
            }

            double tipX = dirX * armLength;
            double tipZ = dirZ * armLength;
            for (int i = 0; i <= 4; i++) {
                double t = (double) i / 4 * 2.0 - 1.0;
                double ox = tipX + perpX * armWidth * t;
                double oz = tipZ + perpZ * armWidth * t;
                double px = cx + ox * u.x() + oz * v.x();
                double py = cy + ox * u.y() + oz * v.y();
                double pz = cz + ox * u.z() + oz * v.z();
                entries.add(new S2CParticleBatchPack.ParticleEntry(px, py, pz));
            }
        }
        return entries;
    }

    public static List<S2CParticleBatchPack.ParticleEntry> buildConcentricRings(double cx, double cy, double cz,
                                                                                 int ringCount, double baseRadius,
                                                                                 double radiusShrink, double ySpacing,
                                                                                 int pointsPerRing,
                                                                                 double rotationAngle,
                                                                                 Vector3f axis) {
        List<S2CParticleBatchPack.ParticleEntry> entries = new ArrayList<>();
        for (int ring = 0; ring < ringCount; ring++) {
            double r = baseRadius - ring * radiusShrink;
            if (r < 0.2) r = 0.2;
            double y = cy + ring * ySpacing;
            double rot = rotationAngle + (ring % 2 == 0 ? 1 : -1) * ring * Math.PI / 6.0;
            entries.addAll(buildCircle(cx, y, cz, r, pointsPerRing, rot, axis));
        }
        return entries;
    }

    public static List<S2CParticleBatchPack.ParticleEntry> buildSquareMandala(double cx, double cy, double cz,
                                                                               int layers, double outerRadius,
                                                                               int pointsPerEdge,
                                                                               double rotationAngle,
                                                                               Vector3f axis) {
        List<S2CParticleBatchPack.ParticleEntry> entries = new ArrayList<>();
        for (int i = 0; i < layers; i++) {
            double r = outerRadius * (layers - i) / layers;
            double rot = rotationAngle + (i % 2 == 0 ? 0 : Math.PI / 4.0);
            entries.addAll(buildPolygon(cx, cy, cz, r, 4, pointsPerEdge, rot, axis));
        }
        return entries;
    }

    public static List<S2CParticleBatchPack.ParticleEntry> buildHexagonalSnowflake(double cx, double cy, double cz,
                                                                                     double radius, int branchPoints,
                                                                                     double branchLength,
                                                                                     double rotationAngle,
                                                                                     Vector3f axis) {
        List<S2CParticleBatchPack.ParticleEntry> entries = new ArrayList<>();
        double nx = axis.x(), ny = axis.y(), nz = axis.z();
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-6) { ny = 1; len = 1; }
        nx /= len; ny /= len; nz /= len;

        Vector3f u = perpendicular(nx, ny, nz);
        Vector3f v = cross(nx, ny, nz, u.x(), u.y(), u.z());

        entries.addAll(buildPolygon(cx, cy, cz, radius, 6, 10, rotationAngle, axis));

        for (int i = 0; i < 6; i++) {
            double armAngle = rotationAngle + 2.0 * Math.PI * i / 6;
            double cos = Math.cos(armAngle);
            double sin = Math.sin(armAngle);

            double tipX = cx + radius * (cos * u.x() + sin * v.x());
            double tipY = cy + radius * (cos * u.y() + sin * v.y());
            double tipZ = cz + radius * (cos * u.z() + sin * v.z());

            double outerX = cx + (radius + branchLength) * (cos * u.x() + sin * v.x());
            double outerY = cy + (radius + branchLength) * (cos * u.y() + sin * v.y());
            double outerZ = cz + (radius + branchLength) * (cos * u.z() + sin * v.z());

            entries.addAll(buildLine(tipX, tipY, tipZ, outerX, outerY, outerZ, branchPoints));

            double midFrac = 0.5;
            double midX = tipX + midFrac * (outerX - tipX);
            double midY = tipY + midFrac * (outerY - tipY);
            double midZ = tipZ + midFrac * (outerZ - tipZ);

            for (int side = -1; side <= 1; side += 2) {
                double subAngle = armAngle + side * Math.PI / 6.0;
                double subCos = Math.cos(subAngle);
                double subSin = Math.sin(subAngle);
                double subLen = branchLength * 0.4;
                double subX = midX + subLen * (subCos * u.x() + subSin * v.x());
                double subY = midY + subLen * (subCos * u.y() + subSin * v.y());
                double subZ = midZ + subLen * (subCos * u.z() + subSin * v.z());
                entries.addAll(buildLine(midX, midY, midZ, subX, subY, subZ, branchPoints / 2));
            }
        }
        return entries;
    }

    private static Vector3f perpendicular(double nx, double ny, double nz) {
        if (Math.abs(ny) < 0.9) {
            double ux = nz, uy = 0, uz = -nx;
            double uLen = Math.sqrt(ux * ux + uz * uz);
            return new Vector3f((float)(ux / uLen), 0, (float)(uz / uLen));
        } else {
            double ux = 0, uy = -nz, uz = ny;
            double uLen = Math.sqrt(uy * uy + uz * uz);
            return new Vector3f(0, (float)(uy / uLen), (float)(uz / uLen));
        }
    }

    private static Vector3f cross(double ax, double ay, double az, double bx, double by, double bz) {
        return new Vector3f(
                (float)(ay * bz - az * by),
                (float)(az * bx - ax * bz),
                (float)(ax * by - ay * bx)
        );
    }
}
