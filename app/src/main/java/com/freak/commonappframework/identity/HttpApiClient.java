//
//  Created by  fred on 2017/1/12.
//  Copyright © 2016年 Alibaba. All rights reserved.
//

package com.freak.commonappframework.identity;

import com.alibaba.cloudapi.sdk.enums.HttpMethod;
import com.alibaba.cloudapi.sdk.enums.Scheme;
import com.alibaba.cloudapi.sdk.model.ApiCallback;
import com.alibaba.cloudapi.sdk.model.ApiRequest;
import com.alibaba.cloudapi.sdk.model.HttpClientBuilderParams;


/**
 * @author freak
 * @date 2019/2/19
 */
public class HttpApiClient extends com.alibaba.cloudapi.sdk.client.HttpApiClient {
    public final static String HOST = "dm-51.data.aliyun.com";
    static HttpApiClient instance = new HttpApiClient();

    public static HttpApiClient getInstance() {
        return instance;
    }

    @Override
    public void init(HttpClientBuilderParams httpClientBuilderParams) {
        httpClientBuilderParams.setScheme(Scheme.HTTP);
        httpClientBuilderParams.setHost(HOST);
        super.init(httpClientBuilderParams);
    }


    public void identification(byte[] body, ApiCallback callback) {
        String path = "/rest/160601/ocr/ocr_idcard.json";
        ApiRequest request = new ApiRequest(HttpMethod.POST_BODY, path, body);
        sendAsyncRequest(request, callback);
    }
}