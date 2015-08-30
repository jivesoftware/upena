/*
 * Flex.java.java
 *
 * Created on 01-03-2010 01:31:37 PM
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
public class Flex implements Cloneable {

    /**
     *
     */
    public float x;
    /**
     *
     */
    public float y;

    /**
     *
     * @param _x
     * @param _y
     */
    public Flex(float _x, float _y) {
        this.x = _x;
        this.y = _y;
    }



    @Override
    public String toString() {
        return " [" + x + "," + y + "]";
    }

    // Cloneable
    @Override
    public Object clone() throws CloneNotSupportedException {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

}
