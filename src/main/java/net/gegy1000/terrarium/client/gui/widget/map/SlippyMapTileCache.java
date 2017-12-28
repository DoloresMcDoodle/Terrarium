package net.gegy1000.terrarium.client.gui.widget.map;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.gegy1000.terrarium.Terrarium;
import net.gegy1000.terrarium.server.map.source.TerrariumData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SideOnly(Side.CLIENT)
public class SlippyMapTileCache {
    private static final File CACHE_ROOT = new File(TerrariumData.CACHE_ROOT, "carto");
    private static final int CACHE_SIZE = 256;

    private final ExecutorService loadingService = Executors.newFixedThreadPool(2, new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("terrarium-map-load-%d")
            .build());

    private final LoadingCache<SlippyMapTilePos, SlippyMapTile> tileCache = CacheBuilder.newBuilder()
            .maximumSize(CACHE_SIZE)
            .build(new CacheLoader<SlippyMapTilePos, SlippyMapTile>() {
                @Override
                public SlippyMapTile load(SlippyMapTilePos key) {
                    SlippyMapTile tile = new SlippyMapTile(key);
                    SlippyMapTileCache.this.loadingService.submit(() -> tile.supplyImage(SlippyMapTileCache.this.downloadImage(key)));
                    return tile;
                }
            });

    public SlippyMapTile getTile(SlippyMapTilePos pos) {
        try {
            return this.tileCache.get(pos);
        } catch (Exception e) {
            SlippyMapTile tile = new SlippyMapTile(pos);
            tile.supplyImage(this.createErrorImage());
            return tile;
        }
    }

    public void shutdown() {
        this.loadingService.shutdownNow();
    }

    private BufferedImage downloadImage(SlippyMapTilePos pos) {
        try (InputStream input = this.getStream(pos)) {
            return ImageIO.read(input);
        } catch (IOException e) {
            Terrarium.LOGGER.error("Failed to load map tile {}", e.getClass().getName());
        }
        return this.createErrorImage();
    }

    private InputStream getStream(SlippyMapTilePos pos) throws IOException {
        File cacheFile = new File(CACHE_ROOT, pos.getCacheName());
        if (cacheFile.exists()) {
            return new BufferedInputStream(new FileInputStream(cacheFile));
        }
        String query = String.format(TerrariumData.info.getRasterMapQuery(), pos.getZoom(), pos.getX(), pos.getY());
        URL url = new URL(TerrariumData.info.getRasterMapEndpoint() + "/" + query);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(1000);
        connection.setRequestProperty("User-Agent", Terrarium.MODID);
        connection.setRequestProperty("Accept-Encoding", "gzip");
        try (InputStream input = connection.getInputStream()) {
            byte[] data = IOUtils.toByteArray(input);
            this.cacheData(cacheFile, data);
            return new ByteArrayInputStream(data);
        }
    }

    private void cacheData(File cacheFile, byte[] data) {
        if (!CACHE_ROOT.exists()) {
            CACHE_ROOT.mkdirs();
        }
        try (OutputStream output = new FileOutputStream(cacheFile)) {
            output.write(data);
        } catch (IOException e) {
            Terrarium.LOGGER.error("Failed to cache map raster tile", e);
        }
    }

    private BufferedImage createErrorImage() {
        BufferedImage result = new BufferedImage(SlippyMap.TILE_SIZE, SlippyMap.TILE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        FontMetrics metrics = graphics.getFontMetrics();

        String message = "Failed to download tile";

        int x = (SlippyMap.TILE_SIZE - metrics.stringWidth(message)) / 2;
        int y = (SlippyMap.TILE_SIZE - metrics.getHeight()) / 2;
        graphics.drawString(message, x, y);

        graphics.dispose();

        return result;
    }
}
