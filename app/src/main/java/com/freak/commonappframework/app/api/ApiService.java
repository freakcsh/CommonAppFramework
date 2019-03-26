package com.freak.commonappframework.app.api;

import com.freak.commonappframework.model.homepage.base.bean.LoginBean;
import com.freak.commonappframework.net.HttpResult;

import io.reactivex.Observable;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 *
 * @author freak
 * @date 2019/2/19
 */
public interface ApiService {
    /**
     * 用户登陆
     *
     * @return
     */
    @POST("/login")
    Observable<HttpResult> login(@Query("userName") String userName,
                                            @Query("pwd") String pwd
    );
}
