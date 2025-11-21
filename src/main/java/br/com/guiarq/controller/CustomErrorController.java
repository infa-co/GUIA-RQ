package br.com.guiarq.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError() {
        // aqui você pode tratar o tipo de erro se quiser (404, 500, etc)
        return "forward:/error/404.html";
    }
}
