package com.freak.commonappframework.model.merchant.base;


import com.freak.commonappframework.base.BaseView;
import com.freak.httphelper.BasePresenter;

/**
 * @author freak
 * @date 2019/2/19
 */

public interface MerchantContract {
    interface View extends BaseView {

    }

    interface Prtesenter extends BasePresenter<View> {

    }
}
