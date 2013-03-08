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

import java.util.ArrayList;
import java.util.List;

public class JPEG2000Image {

    public static final int HEADER_FAIL = 0;
    public static final int HEADER_SUCCESS = 1;
    private int status;
    private int width;
    private int height;
    private int tilesX;
    private int tilesY;
    private int tileW;
    private int tileH;
    private int maxReduction;
    private int numCompositions;
    private String filename;

    public JPEG2000Image(String filename, final int[] arySpex) {
        this.filename = filename;
        status = arySpex[0];
        width = arySpex[1];
        height = arySpex[2];
        tilesX = arySpex[3];
        tilesY = arySpex[4];
        tileW = arySpex[5];
        tileH = arySpex[6];
        maxReduction = arySpex[7];
        numCompositions = arySpex[8];
    }

    public boolean headerLoaded() {
        return status == HEADER_SUCCESS;
    }

    public int getTilesX() {
        return tilesX;
    }

    public int getTilesY() {
        return tilesY;
    }

    public int getNumCompositions() {
        return numCompositions;
    }

    public int getMaxReduction() {
        return maxReduction - 1;
    }

    private int reduce(int num, int reduction) {
        for(int i = 0; i < reduction; ++i) {
            num = (int) Math.ceil(((double) num) / 2.0d);
        }
        return num;
    }

    public int getWidth(int reduction) {
        return reduce(width, reduction);
    }

    public int getHeight(int reduction) {
        return reduce(height, reduction);
    }

    public int getTileW(int reduction) {
        return reduce(tileW, reduction);
    }

    public int getTileH(int reduction) {
        return reduce(tileH, reduction);
    }

    public String getFilename() {
        return filename;
    }

    private List<Integer> filterTilesDim(int start, int finish, int tiles, int tsiz, int reduction) {
        int siz = reduce(tsiz, reduction);
        List<Integer> indices = new ArrayList<Integer>();
        for(int i = 0; i < tiles; ++i) {
            int cur1 = i * siz;
            int cur2 = (i+1) * siz;
            if( (start >= cur1 && start <= cur2) ||
                (finish >= cur1 && finish <= cur2) ||
                (start <= cur1 && finish >= cur2)) {
                indices.add(i);
            }
        }
        return indices;
    }

    public List<Integer> filterTilesX(int x, int w, int reduction) {
        return filterTilesDim(x, x + w, tilesX, tileW, reduction);
    }

    public List<Integer> filterTilesY(int y, int h, int reduction) {
        return filterTilesDim(y, y + h, tilesY, tileH, reduction);
    }


    @Override
    public String toString() {
        return "JPEG2000Image{" +
                "status=" + (status == HEADER_FAIL ? "HEADER_FAIL" : "HEADER_SUCCESS") +
                ", width=" + width +
                ", height=" + height +
                ", tilesX=" + tilesX +
                ", tilesY=" + tilesY +
                ", tileW=" + tileW +
                ", tileH=" + tileH +
                ", maxReduction=" + maxReduction +
                ", numCompositions=" + numCompositions +
                '}';
    }
}
