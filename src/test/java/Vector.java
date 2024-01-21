import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class Vector implements Cloneable {
    private static final long serialVersionUID = -2657651106777219169L;
    private static final Random random = new Random();
    private static final double epsilon = 1.0E-6;
    protected double x;
    protected double y;
    protected double z;

    public Vector() {
        this.x = 0.0;
        this.y = 0.0;
        this.z = 0.0;
    }

    public Vector(int x, int y, int z) {
        this.x = (double)x;
        this.y = (double)y;
        this.z = (double)z;
    }

    public Vector(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector(float x, float y, float z) {
        this.x = (double)x;
        this.y = (double)y;
        this.z = (double)z;
    }

    @NotNull
    public Vector add(@NotNull Vector vec) {
        this.x += vec.x;
        this.y += vec.y;
        this.z += vec.z;
        return this;
    }

    @NotNull
    public Vector subtract(@NotNull Vector vec) {
        this.x -= vec.x;
        this.y -= vec.y;
        this.z -= vec.z;
        return this;
    }

    @NotNull
    public Vector multiply(@NotNull Vector vec) {
        this.x *= vec.x;
        this.y *= vec.y;
        this.z *= vec.z;
        return this;
    }

    @NotNull
    public Vector divide(@NotNull Vector vec) {
        this.x /= vec.x;
        this.y /= vec.y;
        this.z /= vec.z;
        return this;
    }

    @NotNull
    public Vector copy(@NotNull Vector vec) {
        this.x = vec.x;
        this.y = vec.y;
        this.z = vec.z;
        return this;
    }

    public double length() {
        return Math.sqrt(square(this.x) + square(this.y) + square(this.z));
    }

    public double lengthSquared() {
        return square(this.x) + square(this.y) + square(this.z);
    }

    public double distance(@NotNull Vector o) {
        return Math.sqrt(square(this.x - o.x) + square(this.y - o.y) + square(this.z - o.z));
    }

    public double distanceSquared(@NotNull Vector o) {
        return square(this.x - o.x) + square(this.y - o.y) + square(this.z - o.z);
    }

    public float angle(@NotNull Vector other) {
        double dot = constrainToRange(this.dot(other) / (this.length() * other.length()), -1.0, 1.0);
        return (float)Math.acos(dot);
    }

    @NotNull
    public Vector midpoint(@NotNull Vector other) {
        this.x = (this.x + other.x) / 2.0;
        this.y = (this.y + other.y) / 2.0;
        this.z = (this.z + other.z) / 2.0;
        return this;
    }

    @NotNull
    public Vector getMidpoint(@NotNull Vector other) {
        double x = (this.x + other.x) / 2.0;
        double y = (this.y + other.y) / 2.0;
        double z = (this.z + other.z) / 2.0;
        return new Vector(x, y, z);
    }

    @NotNull
    public Vector multiply(int m) {
        this.x *= (double)m;
        this.y *= (double)m;
        this.z *= (double)m;
        return this;
    }

    @NotNull
    public Vector multiply(double m) {
        this.x *= m;
        this.y *= m;
        this.z *= m;
        return this;
    }

    @NotNull
    public Vector multiply(float m) {
        this.x *= (double)m;
        this.y *= (double)m;
        this.z *= (double)m;
        return this;
    }

    public double dot(@NotNull Vector other) {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    @NotNull
    public Vector crossProduct(@NotNull Vector o) {
        double newX = this.y * o.z - o.y * this.z;
        double newY = this.z * o.x - o.z * this.x;
        double newZ = this.x * o.y - o.x * this.y;
        this.x = newX;
        this.y = newY;
        this.z = newZ;
        return this;
    }

    @NotNull
    public Vector getCrossProduct(@NotNull Vector o) {
        double x = this.y * o.z - o.y * this.z;
        double y = this.z * o.x - o.z * this.x;
        double z = this.x * o.y - o.x * this.y;
        return new Vector(x, y, z);
    }

    @NotNull
    public Vector normalize() {
        double length = this.length();
        this.x /= length;
        this.y /= length;
        this.z /= length;
        return this;
    }

    @NotNull
    public Vector zero() {
        this.x = 0.0;
        this.y = 0.0;
        this.z = 0.0;
        return this;
    }

    @NotNull
    Vector normalizeZeros() {
        if (this.x == -0.0) {
            this.x = 0.0;
        }

        if (this.y == -0.0) {
            this.y = 0.0;
        }

        if (this.z == -0.0) {
            this.z = 0.0;
        }

        return this;
    }

    public boolean isInAABB(@NotNull Vector min, @NotNull Vector max) {
        return this.x >= min.x && this.x <= max.x && this.y >= min.y && this.y <= max.y && this.z >= min.z && this.z <= max.z;
    }

    public boolean isInSphere(@NotNull Vector origin, double radius) {
        return square(origin.x - this.x) + square(origin.y - this.y) + square(origin.z - this.z) <= square(radius);
    }

    public boolean isNormalized() {
        return Math.abs(this.lengthSquared() - 1.0) < getEpsilon();
    }

    @NotNull
    public Vector rotateAroundX(double angle) {
        double angleCos = Math.cos(angle);
        double angleSin = Math.sin(angle);
        double y = angleCos * this.getY() - angleSin * this.getZ();
        double z = angleSin * this.getY() + angleCos * this.getZ();
        return this.setY(y).setZ(z);
    }

    @NotNull
    public Vector rotateAroundY(double angle) {
        double angleCos = Math.cos(angle);
        double angleSin = Math.sin(angle);
        double x = angleCos * this.getX() + angleSin * this.getZ();
        double z = -angleSin * this.getX() + angleCos * this.getZ();
        return this.setX(x).setZ(z);
    }

    @NotNull
    public Vector rotateAroundZ(double angle) {
        double angleCos = Math.cos(angle);
        double angleSin = Math.sin(angle);
        double x = angleCos * this.getX() - angleSin * this.getY();
        double y = angleSin * this.getX() + angleCos * this.getY();
        return this.setX(x).setY(y);
    }

    @NotNull
    public Vector rotateAroundAxis(@NotNull Vector axis, double angle) throws IllegalArgumentException {
        return this.rotateAroundNonUnitAxis(axis.isNormalized() ? axis : axis.clone().normalize(), angle);
    }

    @NotNull
    public Vector rotateAroundNonUnitAxis(@NotNull Vector axis, double angle) throws IllegalArgumentException {
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        double x2 = axis.getX();
        double y2 = axis.getY();
        double z2 = axis.getZ();
        double cosTheta = Math.cos(angle);
        double sinTheta = Math.sin(angle);
        double dotProduct = this.dot(axis);
        double xPrime = x2 * dotProduct * (1.0 - cosTheta) + x * cosTheta + (-z2 * y + y2 * z) * sinTheta;
        double yPrime = y2 * dotProduct * (1.0 - cosTheta) + y * cosTheta + (z2 * x - x2 * z) * sinTheta;
        double zPrime = z2 * dotProduct * (1.0 - cosTheta) + z * cosTheta + (-y2 * x + x2 * y) * sinTheta;
        return this.setX(xPrime).setY(yPrime).setZ(zPrime);
    }

    public double getX() {
        return this.x;
    }

    public int getBlockX() {
        return floor(this.x);
    }

    public double getY() {
        return this.y;
    }

    public int getBlockY() {
        return floor(this.y);
    }

    public double getZ() {
        return this.z;
    }

    public int getBlockZ() {
        return floor(this.z);
    }

    @NotNull
    public Vector setX(int x) {
        this.x = (double)x;
        return this;
    }

    @NotNull
    public Vector setX(double x) {
        this.x = x;
        return this;
    }

    @NotNull
    public Vector setX(float x) {
        this.x = (double)x;
        return this;
    }

    @NotNull
    public Vector setY(int y) {
        this.y = (double)y;
        return this;
    }

    @NotNull
    public Vector setY(double y) {
        this.y = y;
        return this;
    }

    @NotNull
    public Vector setY(float y) {
        this.y = (double)y;
        return this;
    }

    @NotNull
    public Vector setZ(int z) {
        this.z = (double)z;
        return this;
    }

    @NotNull
    public Vector setZ(double z) {
        this.z = z;
        return this;
    }

    @NotNull
    public Vector setZ(float z) {
        this.z = (double)z;
        return this;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Vector)) {
            return false;
        } else {
            Vector other = (Vector)obj;
            return Math.abs(this.x - other.x) < 1.0E-6 && Math.abs(this.y - other.y) < 1.0E-6 && Math.abs(this.z - other.z) < 1.0E-6 && this.getClass().equals(obj.getClass());
        }
    }

    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + (int)(Double.doubleToLongBits(this.x) ^ Double.doubleToLongBits(this.x) >>> 32);
        hash = 79 * hash + (int)(Double.doubleToLongBits(this.y) ^ Double.doubleToLongBits(this.y) >>> 32);
        hash = 79 * hash + (int)(Double.doubleToLongBits(this.z) ^ Double.doubleToLongBits(this.z) >>> 32);
        return hash;
    }

    @NotNull
    public Vector clone() {
        try {
            return (Vector)super.clone();
        } catch (CloneNotSupportedException var2) {
            throw new IllegalStateException(var2);
        }
    }

    public String toString() {
        return this.x + "," + this.y + "," + this.z;
    }

    public void checkFinite() throws IllegalArgumentException {
        checkFinite(this.x, "x not finite");
        checkFinite(this.y, "y not finite");
        checkFinite(this.z, "z not finite");
    }

    public static void checkFinite(double d, @NotNull String message) {
        if (!isFinite(d)) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void checkFinite(float d, @NotNull String message) {
        if (!isFinite(d)) {
            throw new IllegalArgumentException(message);
        }
    }

    public static double getEpsilon() {
        return 1.0E-6;
    }

    @NotNull
    public static Vector getMinimum(@NotNull Vector v1, @NotNull Vector v2) {
        return new Vector(Math.min(v1.x, v2.x), Math.min(v1.y, v2.y), Math.min(v1.z, v2.z));
    }

    @NotNull
    public static Vector getMaximum(@NotNull Vector v1, @NotNull Vector v2) {
        return new Vector(Math.max(v1.x, v2.x), Math.max(v1.y, v2.y), Math.max(v1.z, v2.z));
    }

    @NotNull
    public static Vector getRandom() {
        return new Vector(random.nextDouble(), random.nextDouble(), random.nextDouble());
    }

    public static double square(double num) {
        return num * num;
    }

    public static int floor(double num) {
        int floor = (int)num;
        return (double)floor == num ? floor : floor - (int)(Double.doubleToRawLongBits(num) >>> 63);
    }

    public static boolean isFinite(double d) {
        return Math.abs(d) <= Double.MAX_VALUE;
    }

    public static boolean isFinite(float f) {
        return Math.abs(f) <= Float.MAX_VALUE;
    }

    public static double constrainToRange(double value, double min, double max) {
        if (min > max) {
            throw new IllegalStateException("min (" + min + ") must be less than or equal to max (" + max + ")");
        } else {
            return Math.min(Math.max(value, min), max);
        }
    }
}
