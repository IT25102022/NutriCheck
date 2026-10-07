package com.nutricheck.nutricheck.controller;

import com.nutricheck.nutricheck.model.BmiCategory;
import com.nutricheck.nutricheck.service.BmiService;
import com.nutricheck.nutricheck.service.DietPlanService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    private final BmiService bmiService;
    private final DietPlanService dietPlanService;

    public HomeController(BmiService bmiService, DietPlanService dietPlanService) {
        this.bmiService = bmiService;
        this.dietPlanService = dietPlanService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("dietPlansCount", dietPlanService.countTotalDietPlans());
        return "index";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/bmi-calculator")
    public String bmiCalculator() {
        return "bmi-calculator";
    }

    @PostMapping("/bmi-calculator")
    public String calculatePublicBmi(@RequestParam("heightCm") Double heightCm,
                                     @RequestParam("weightKg") Double weightKg,
                                     @RequestParam(value = "age", required = false) Integer age,
                                     Model model) {
        try {
            double bmi = bmiService.calculateBmi(weightKg, heightCm, age);
            BmiCategory category = bmiService.classifyBmi(bmi, age);

            model.addAttribute("heightCm", heightCm);
            model.addAttribute("weightKg", weightKg);
            model.addAttribute("age", age);
            model.addAttribute("bmiValue", bmi);
            model.addAttribute("bmiCategory", category);
            model.addAttribute("recommendedPlans", dietPlanService.getDietPlansByBmiCategory(category));
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "bmi-calculator";
    }
}
