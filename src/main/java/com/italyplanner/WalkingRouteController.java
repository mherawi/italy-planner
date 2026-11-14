package com.italyplanner;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WalkingRouteController {
    private final WalkingRoutes routes;

    public WalkingRouteController(WalkingRoutes routes) {
        this.routes = routes;
    }

    @GetMapping("/api/routes/walking")
    public WalkingRoutes.Estimate walking(@RequestParam String from, @RequestParam String to) {
        return routes.estimate(from, to);
    }
}
