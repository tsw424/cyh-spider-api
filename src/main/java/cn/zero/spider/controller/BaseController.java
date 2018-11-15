package cn.zero.spider.controller;

import cn.zero.spider.util.Ajax;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author 蔡元豪
 * @date 2018/6/23 21:54
 */
public class BaseController {

    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public Ajax exceptionHandler(Exception e) {
        return new Ajax(500, null, e.getMessage());
    }

}
