package nl.kb.jp2;

import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class JP2Reader {
    private static final String LIBRARY_NAME = "libkbjp2.so";
    private static final int MAX_THREADS_PER_JOB = 8;


    static {
        InputStream is = JP2Reader.class.getResourceAsStream("/" + LIBRARY_NAME);
        try {
            File temp = File.createTempFile(LIBRARY_NAME, "");
            FileOutputStream fos = new FileOutputStream(temp);
            fos.write(IOUtils.toByteArray(is));
            fos.close();
            is.close();
            System.load(temp.getAbsolutePath());
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static native int[] getJp2Specs(String filename);
    private static native int[] getTile(String filename, int tileIndex, int reduction, int[][] pixels);


    private static class TileToBufferJob extends Thread {
        private BufferedImage img;
        private JPEG2000Image image;
        private int reduction;
        private int tileIndex;
        private int imageX;
        private int imageY;

        public TileToBufferJob(JPEG2000Image image, BufferedImage img, int reduction, int tileIndex, int imageX, int imageY) {
            this.image = image;
            this.img = img;
            this.reduction = reduction;
            this.tileIndex = tileIndex;
            this.imageX = imageX;
            this.imageY = imageY;
        }

        public void run() {
            int[][] tileRBG = new int[image.getNumCompositions()][];
            int[] tileSpecs = getTile(image.getFilename(), tileIndex, reduction, tileRBG);
            int i = 0;
            for(int y = 0; y < tileSpecs[2]; ++y) {
                for(int x = 0; x < tileSpecs[1]; ++x) {
                    int[] rgb = new int[3];
                    rgb[0] = tileRBG[0][i];
                    rgb[1] = tileRBG[1][i];
                    rgb[2] = tileRBG[2][i++];
                    img.getRaster().setPixel(imageX + x, imageY + y, rgb);
                }
            }
        }
    }


    public static BufferedImage getFullImage(JPEG2000Image image, int reduction) {
        if(reduction < 0) { reduction = 0; }
        if(reduction > image.getMaxReduction()) { reduction = image.getMaxReduction(); }

        BufferedImage img = new BufferedImage(image.getWidth(reduction), image.getHeight(reduction), BufferedImage.TYPE_INT_RGB);
        List<TileToBufferJob> openThreads = new ArrayList<TileToBufferJob>();
        for(int tileX = 0; tileX < image.getTilesX(); ++tileX) {
            int imageX = tileX * image.getTileW(reduction);
            for(int tileY = 0; tileY < image.getTilesY(); ++tileY) {
                int imageY = tileY * image.getTileH(reduction);
                int tileIndex = (image.getTilesX() * tileY) + tileX;
                TileToBufferJob job = new TileToBufferJob(image, img, reduction, tileIndex, imageX, imageY);
                job.start();
                openThreads.add(job);
                if(openThreads.size() >= MAX_THREADS_PER_JOB) {
                    try {
                        openThreads.remove(0).join();
                    } catch(InterruptedException e) { }
                }
            }
        }
        for(TileToBufferJob job : openThreads) {
            try {
                job.join();
            } catch (InterruptedException e) {

            }
        }

        return img;
    }

    public static void main(String args[]) {
        String filename = args[1];
        long start = new Date().getTime();
        JPEG2000Image image = new JPEG2000Image(filename, getJp2Specs(filename));
        if(image.headerLoaded()) {
            System.out.println(image);
            BufferedImage outImg = getFullImage(image, Integer.parseInt(args[2]));
            try {
                ImageIO.write(outImg, "jpg", new File("test.jpg"));
            } catch(IOException e) {
                e.printStackTrace();
            }

        } else {
            System.err.println("failed to load file: " + filename);
        }
        System.out.println("Timed ms: " + ((new Date().getTime()) - start));
    }
}
