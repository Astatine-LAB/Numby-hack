package cqb13.NumbyHack.utils;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class RoundRenderUtils {
    private RoundRenderUtils() {
    }

    // good thickness values for circles are very small around like 0.08 - 1
    public static void renderCircle(Render3DEvent event, double radius, double thickness,
            double cx, double cy, double cz, Color color) {
        final double maxSegmentLength = 0.2;

        int segments = (int) Math.ceil(2 * Math.PI * radius / maxSegmentLength);
        segments = Math.max(16, segments);

        Vec3[] outerPts = new Vec3[segments];
        Vec3[] innerPts = new Vec3[segments];

        double outerRadius = radius + thickness / 2.0;
        double innerRadius = radius - thickness / 2.0;
        if (innerRadius < 0) {
            innerRadius = 0;
        }

        // points around circle
        for (int s = 0; s < segments; s++) {
            double angle = 2 * Math.PI * s / segments;
            double sin = Math.sin(angle);
            double cos = Math.cos(angle);

            outerPts[s] = new Vec3(
                    cx + sin * outerRadius,
                    cy,
                    cz + cos * outerRadius);

            innerPts[s] = new Vec3(
                    cx + sin * innerRadius,
                    cy,
                    cz + cos * innerRadius);
        }

        // connect with triangles
        for (int s = 0; s < segments; s++) {
            int next = (s + 1) % segments;

            event.renderer.triangles.ensureQuadCapacity();

            int outer1 = event.renderer.triangles
                    .vec3(outerPts[s].x, outerPts[s].y, outerPts[s].z)
                    .color(color)
                    .next();

            int outer2 = event.renderer.triangles
                    .vec3(outerPts[next].x, outerPts[next].y, outerPts[next].z)
                    .color(color)
                    .next();

            int inner1 = event.renderer.triangles
                    .vec3(innerPts[s].x, innerPts[s].y, innerPts[s].z)
                    .color(color)
                    .next();

            int inner2 = event.renderer.triangles
                    .vec3(innerPts[next].x, innerPts[next].y, innerPts[next].z)
                    .color(color)
                    .next();

            event.renderer.triangles.triangle(outer1, outer2, inner1);
            event.renderer.triangles.triangle(inner1, outer2, inner2);
        }
    }

    public static void renderCircle(Render3DEvent event, double radius, double thickness,
            BlockPos origin, Color color) {
        renderCircle(event, radius, thickness, origin.getX(), origin.getY(), origin.getZ(), color);
    }

    public static void renderSphere(Render3DEvent event, double radius, int gradation,
            double cx, double cy, double cz, Color color) {

        // the number of slices in each direction
        int horizontalSteps = Math.max(8, gradation);
        int verticalSteps = Math.max(16, gradation * 2);

        for (int lat = 0; lat < horizontalSteps; lat++) {
            double theta1 = Math.PI * lat / horizontalSteps;
            double theta2 = Math.PI * (lat + 1) / horizontalSteps;

            for (int lon = 0; lon < verticalSteps; lon++) {
                double phi1 = 2.0 * Math.PI * lon / verticalSteps;
                double phi2 = 2.0 * Math.PI * (lon + 1) / verticalSteps;

                Vec3 p1 = spherePoint(cx, cy, cz, radius, theta1, phi1);
                Vec3 p2 = spherePoint(cx, cy, cz, radius, theta1, phi2);
                Vec3 p3 = spherePoint(cx, cy, cz, radius, theta2, phi2);
                Vec3 p4 = spherePoint(cx, cy, cz, radius, theta2, phi1);

                event.renderer.triangles.ensureQuadCapacity();

                int i1 = event.renderer.triangles.vec3(p1.x, p1.y, p1.z).color(color).next();
                int i2 = event.renderer.triangles.vec3(p2.x, p2.y, p2.z).color(color).next();
                int i3 = event.renderer.triangles.vec3(p3.x, p3.y, p3.z).color(color).next();
                int i4 = event.renderer.triangles.vec3(p4.x, p4.y, p4.z).color(color).next();

                event.renderer.triangles.triangle(i1, i2, i3);
                event.renderer.triangles.triangle(i1, i3, i4);
            }
        }
    }

    public static void renderSphere(Render3DEvent event, double radius, int gradation,
            BlockPos origin, Color color) {
        renderSphere(event, radius, gradation, origin.getX(), origin.getY(), origin.getZ(), color);
    }

    private static Vec3 spherePoint(double cx, double cy, double cz, double r,
            double theta, double phi) {
        double sinTheta = Math.sin(theta);

        double x = cx + r * sinTheta * Math.cos(phi);
        double y = cy + r * Math.cos(theta);
        double z = cz + r * sinTheta * Math.sin(phi);

        return new Vec3(x, y, z);
    }
}
