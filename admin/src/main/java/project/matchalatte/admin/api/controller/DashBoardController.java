package project.matchalatte.admin.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import project.matchalatte.admin.domain.DashBoardService;

@Controller
public class DashBoardController {

    private final DashBoardService dashBoardService;

    public DashBoardController(DashBoardService dashBoardService) {
        this.dashBoardService = dashBoardService;
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {

        var dailySignUps = dashBoardService.getDailySignUps();
        var priceRanges = dashBoardService.getPriceRanges();
        var topSellers = dashBoardService.getTopSellers();

        model.addAttribute("dailySignUps", dailySignUps);
        model.addAttribute("priceRanges", priceRanges);
        model.addAttribute("topSellers", topSellers);

        return "admin/dashboard";
    }

}
