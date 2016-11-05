/*
 * URandom.java.java
 *
 * Created on 01-03-2010 09:03:00 AM
 *
 * Copyright 2010 Jonathan Colt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jivesoftware.os.upena.deployable.profiler.visualize.paint;

/**
 *
 * @author Administrator
 */
public abstract class URandom {

    /**
     *
     */
    static public class Seed {

        /**
         *
         */
        public long randSeed;

        /**
         *
         * @param _seed
         */
        public Seed(long _seed) {
            randSeed = _seed;
        }
    }

    private final static long randMult = 0x5DEECE66DL;
    private final static long randAdd = 0xBL;
    private final static long randMask = (1L << 48) - 1;
    // public so you can set your own randSeed for repeatability
    /**
     *
     */
    public static Seed defaultSeed = new Seed((System.currentTimeMillis() ^ randMult) & randMask);

    /**
     *
     * @param _longSeed
     * @return
     */
    public static int rand(Seed _longSeed) {
        long x = (_longSeed.randSeed * randMult + randAdd) & randMask;
        _longSeed.randSeed = x;
        return (int) (x >>> (16));
    }

    /**
     *
     * @param max
     * @return
     */
    public static int rand(int max) { // 0 <= return < max
        return rand(defaultSeed, max);
    }

    /**
     *
     * @param _longSeed
     * @param max
     * @return
     */
    public static int rand(Seed _longSeed, int max) { // 0 <= return < max
        long x = (_longSeed.randSeed * randMult + randAdd) & randMask;
        _longSeed.randSeed = x;
        int rand = (int) (x >>> (16));
        rand %= max;
        if (rand < 0) {
            return -rand;
        }
        return rand;
    }

}
