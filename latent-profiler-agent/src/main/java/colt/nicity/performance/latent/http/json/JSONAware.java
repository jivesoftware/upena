/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package colt.nicity.performance.latent.http.json;

/**
 * Beans that support customized output of JSON text shall implement this interface.
 *
 * @author FangYidong<fangyidong@yahoo.com.cn>
 */
public interface JSONAware {

    /**
     * @return JSON text
     */
    String toJSONString();
}