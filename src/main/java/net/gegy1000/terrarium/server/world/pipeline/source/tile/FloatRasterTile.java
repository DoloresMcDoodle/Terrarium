package net.gegy1000.terrarium.server.world.pipeline.source.tile;

import net.gegy1000.terrarium.server.world.pipeline.DataView;

import java.util.Arrays;

public class FloatRasterTile implements TiledDataAccess, NumberRasterTile<Float> {
    private final float[] data;
    private final int width;
    private final int height;

    public FloatRasterTile(float[] data, int width, int height) {
        if (data.length != width * height) {
            throw new IllegalArgumentException("Given width and height do not match array length!");
        }
        this.data = data;
        this.width = width;
        this.height = height;
    }

    public FloatRasterTile(int width, int height) {
        this(new float[width * height], width, height);
    }

    public FloatRasterTile(DataView view) {
        this.data = new float[view.getWidth() * view.getHeight()];
        this.width = view.getWidth();
        this.height = view.getHeight();
    }

    @Override
    @Deprecated
    public void set(int x, int z, Float value) {
        this.data[x + z * this.width] = value;
    }

    public void setFloat(int x, int z, float value) {
        this.data[x + z * this.width] = value;
    }

    @Override
    @Deprecated
    public Float get(int x, int z) {
        return this.getFloat(x, z);
    }

    public float getFloat(int x, int z) {
        return this.data[x + z * this.width];
    }

    @Override
    public Float[] getData() {
        float[] data = this.getFloatData();
        Float[] result = new Float[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = data[i];
        }
        return result;
    }

    public float[] getFloatData() {
        return this.data;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public void setDouble(int x, int y, double value) {
        this.setFloat(x, y, (float) value);
    }

    @Override
    public double getDouble(int x, int y) {
        return this.getFloat(x, y);
    }

    @Override
    public FloatRasterTile copy() {
        return new FloatRasterTile(Arrays.copyOf(this.data, this.data.length), this.width, this.height);
    }
}
