package com.travelpal.web;

import com.travelpal.entity.Plan;
import com.travelpal.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequestMapping("/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping
    public String listPlans(@RequestParam(required = false) String city,
                            Principal principal,
                            Model model) {
        model.addAttribute("publicPlans", planService.getPublicPlans(city));
        model.addAttribute("myPlans", planService.getMyPlans(principal.getName()));
        model.addAttribute("cityFilter", city != null ? city : "");
        return "plans/list";
    }

    @GetMapping("/new")
    public String newPlanForm(Model model) {
        model.addAttribute("planForm", new PlanForm());
        return "plans/form";
    }

    @PostMapping
    public String createPlan(@Valid @ModelAttribute("planForm") PlanForm form,
                             BindingResult bindingResult,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "plans/form";
        }
        Plan created = planService.createPlan(form, principal.getName());
        redirectAttributes.addFlashAttribute("message", "Plan created.");
        return "redirect:/plans/" + created.getId();
    }

    @GetMapping("/{id}")
    public String planDetail(@PathVariable UUID id, Principal principal, Model model) {
        Plan plan = planService.getPlanDetail(id, principal.getName());
        model.addAttribute("plan", plan);
        if (!model.containsAttribute("itemForm")) {
            PlanItemForm itemForm = new PlanItemForm();
            itemForm.setOrderIndex(plan.getPlanItems().size() + 1);
            model.addAttribute("itemForm", itemForm);
        }
        return "plans/detail";
    }

    @PostMapping("/{id}/items")
    public String addItem(@PathVariable UUID id,
                          @Valid @ModelAttribute("itemForm") PlanItemForm form,
                          BindingResult bindingResult,
                          Principal principal,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("plan", planService.getPlanDetail(id, principal.getName()));
            return "plans/detail";
        }
        try {
            planService.addItem(id, form, principal.getName());
            redirectAttributes.addFlashAttribute("message", "Item added to plan.");
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("dayNumber", "invalidDay", ex.getMessage());
            model.addAttribute("plan", planService.getPlanDetail(id, principal.getName()));
            return "plans/detail";
        }
        return "redirect:/plans/" + id;
    }

    @PostMapping("/{id}/copy")
    public String copyPlan(@PathVariable UUID id, Principal principal, RedirectAttributes redirectAttributes) {
        Plan copied = planService.copyPublicPlan(id, principal.getName());
        redirectAttributes.addFlashAttribute("message", "Plan copied to your plans.");
        return "redirect:/plans/" + copied.getId();
    }

    @PostMapping("/{id}/delete")
    public String deletePlan(@PathVariable UUID id, Principal principal, RedirectAttributes redirectAttributes) {
        planService.deletePlan(id, principal.getName());
        redirectAttributes.addFlashAttribute("message", "Plan deleted.");
        return "redirect:/plans";
    }
}