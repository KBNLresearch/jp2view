/**
 * Copyright (c) 2013, Koninklijke Bibliotheek - Nationale bibliotheek van Nederland
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this
 *     list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the Koninklijke Bibliotheek nor the names of its contributors
 *     may be used to endorse or promote products derived from this software without
 *     specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

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

    private native int[] getJp2Specs(String filename);
    private native int[] getTile(String filename, int tileIndex, int reduction, int[][] pixels);


    private class TileToBufferJob implements Runnable {
        private BufferedImage img;
        private JPEG2000Image image;
        private int reduction;
        private int tileIndex;
        private int imageX;
        private int imageY;

        private int subX1 = -1;
        private int subY1 = -1;
        private int subX = -1;
        private int subY = -1;
        private int realX = 0;
        private int realY = 0;

        public TileToBufferJob(JPEG2000Image image, BufferedImage img, int reduction, int tileIndex, int imageX, int imageY,
                               int subX, int subY, int subX1, int subY1, int realX, int realY) {
            this(image, img, reduction, tileIndex, imageX, imageY);
            this.subX = subX;
            this.subY = subY;
            this.subX1 = subX1;
            this.subY1 = subY1;
            this.realX = realX;
            this.realY = realY;
        }

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
            int startX = subX > 0 ? subX : 0;
            int startY = subY > 0 ? subY : 0;
            int endX = subX1 > 0 ? (subX1 > tileSpecs[1] ? tileSpecs[1] : subX1) : tileSpecs[1];
            int endY = subY1 > 0 ? (subY1 > tileSpecs[2] ? tileSpecs[2] : subY1) : tileSpecs[2];
            int outLeft = imageX - realX < 0 ? 0 : imageX - realX;
            int outTop = imageY - realY < 0 ? 0 : imageY - realY;

            for(int y = startY; y < endY; ++y) {
                for(int x = startX; x < endX; ++x) {
                    int[] rgb = new int[3];
                    int i = y * tileSpecs[1] + x;
                    if(image.getNumCompositions() >= 3) {
                        rgb[0] = tileRBG[0][i];
                        rgb[1] = tileRBG[1][i];
                        rgb[2] = tileRBG[2][i];
                    } else {
                        /** we do not use the alpha channel at the kbnl; maybe grayscale though **/
                        rgb[0] = tileRBG[0][i];
                        rgb[1] = tileRBG[0][i];
                        rgb[2] = tileRBG[0][i];
                    }
                    try {
                        img.getRaster().setPixel(outLeft + x - startX, outTop + y - startY, rgb);
                    } catch(ArrayIndexOutOfBoundsException e) {
                        System.err.print("x");
                    }
                }
            }
        }
    }


    public BufferedImage getFullImage(JPEG2000Image image, int reduction) {
        if(reduction < 0) { reduction = 0; }
        if(reduction > image.getMaxReduction()) { reduction = image.getMaxReduction(); }
        return getRegion(image, reduction, 0, 0, image.getWidth(reduction), image.getHeight(reduction));
    }

    public BufferedImage getRegion(JPEG2000Image image, int reduction, int x, int y, int w, int h) {
        if(reduction < 0) { reduction = 0; }
        else if(reduction > image.getMaxReduction()) { reduction = image.getMaxReduction(); }
        if(x > image.getWidth(reduction)) { x = image.getWidth(reduction); }
        if(y > image.getHeight(reduction)) { y = image.getHeight(reduction); }
        if(x + w > image.getWidth(reduction)) { w = image.getWidth(reduction) - x; }
        if(y + h > image.getHeight(reduction)) { h = image.getHeight(reduction) - y; }

        if(w <= 0|| h <= 0) {
            return new BufferedImage(1,1, BufferedImage.TYPE_INT_RGB);
        }

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        List<Thread> openThreads = new ArrayList<Thread>();
        for(int tileX : image.filterTilesX(x, w, reduction)) {
            int imageX = tileX * image.getTileW(reduction);
            for(int tileY : image.filterTilesY(y, h, reduction)) {
                int imageY = tileY * image.getTileH(reduction);
                int tileIndex = (image.getTilesX() * tileY) + tileX;
                int subX = (imageX > x ? 0 : x - imageX);
                int subY = (imageY > y ? 0 : y - imageY);
                int subX1 = (imageX + image.getTileW(reduction) > x + w ?
                        (x + w) - imageX :
                        image.getTileW(reduction));
                int subY1 = (imageY + image.getTileH(reduction) > y + h ?
                        (y + h) - imageY :
                        image.getTileH(reduction));

                TileToBufferJob job = new TileToBufferJob(image, img, reduction, tileIndex, imageX, imageY,
                        subX, subY, subX1, subY1, x, y);
                Thread t = new Thread(job);
                t.start();
                openThreads.add(t);
                if(openThreads.size() >= MAX_THREADS_PER_JOB) {
                    try {
                        openThreads.remove(0).join();
                    } catch(InterruptedException e) { }
                }
            }
        }
        for(Thread t : openThreads) {
            try {
                t.join();
            } catch (InterruptedException e) { }
        }

        return img;
    }

    public static void main(String args[]) {
        String filename = args[1];
        long start = new Date().getTime();
        JP2Reader reader = new JP2Reader();

        JPEG2000Image image = new JPEG2000Image(filename, reader.getJp2Specs(filename));
        System.out.println("Read header ms: " + ((new Date().getTime()) - start));
        if(image.headerLoaded()) {
            System.out.println(image);

            start = new Date().getTime();
            BufferedImage outImg = reader.getFullImage(image, Integer.parseInt(args[2]));
            System.out.println("Decompile full image ms: " + ((new Date().getTime()) - start));
            try {
                ImageIO.write(outImg, "jpg", new File("test.jpg"));
            } catch(IOException e) {
                e.printStackTrace();
            }

            start = new Date().getTime();
            BufferedImage outImg1 = reader.getRegion(image, 1, 250, 135, 180, 400);
            System.out.println("Get region ms: " + ((new Date().getTime()) - start));
            try {
                ImageIO.write(outImg1, "jpg", new File("test_region.jpg"));
            } catch(IOException e) {
                e.printStackTrace();
            }


        } else {
            System.err.println("failed to load file: " + filename);
        }
    }
}
