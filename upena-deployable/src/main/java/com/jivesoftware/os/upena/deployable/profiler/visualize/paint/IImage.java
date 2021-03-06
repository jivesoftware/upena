/*
 * IImage.java.java
 *
 * Created on 01-03-2010 01:31:38 PM
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
public interface IImage {

    /**
     *
     * @param _who
     * @return
     */
    public Object data(long _who);

    /**
     *
     * @return
     */
    public int getWidth();

    /**
     *
     * @return
     */
    public int getHeight();

    /**
     *
     * @param _who
     * @return
     */
    public ICanvas canvas(long _who);
}
