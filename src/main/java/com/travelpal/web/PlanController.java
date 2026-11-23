package com.travelpal.web;

import com.travelpal.entity.Plan;
import com.travelpal.entity.PlanItem;
import com.travelpal.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Plan pages. The logged-in user always comes from {@link Principal} (the login session),
 * never from the form, so nobody can act as another user. Permission checks live in PlanService.
 */
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
        if (!model.containsAttribute("itemForm")) {
            PlanItemForm itemForm = new PlanItemForm();
            itemForm.setOrderIndex(plan.getPlanItems().size() + 1);
            model.addAttribute("itemForm", itemForm);
        }
        return showDetail(plan, principal, model);
    }

    @GetMapping("/{id}/edit")
    public String editPlanForm(@PathVariable UUID id, Principal principal, Model model) {
        model.addAttribute("planForm", planService.getEditForm(id, principal.getName()));
        model.addAttribute("planId", id);
        return "plans/form";
    }

    @PostMapping("/{id}/edit")
    public String updatePlan(@PathVariable UUID id,
                             @Valid @ModelAttribute("planForm") PlanForm form,
                             BindingResult bindingResult,
                             Principal principal,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        model.addAttribute("planId", id);
        if (bindingResult.hasErrors()) {
            return "plans/form";
        }
        try {
            planService.updatePlan(id, form, principal.getName());
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("days", "tooShort", ex.getMessage());
            return "plans/form";
        }
        redirectAttributes.addFlashAttribute("message", "Plan updated.");
        return "redirect:/plans/" + id;
    }

    @PostMapping("/{id}/items")
    public String addItem(@PathVariable UUID id,
                          @Valid @ModelAttribute("itemForm") PlanItemForm form,
                          BindingResult bindingResult,
                          Principal principal,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                planService.addItem(id, form, principal.getName());
                redirectAttributes.addFlashAttribute("message", "Item added to plan.");
                return "redirect:/plans/" + id;
            } catch (IllegalArgumentException ex) {
                bindingResult.rejectValue("dayNumber", "invalidDay", ex.getMessage());
            }
        }
        return showDetail(planService.getPlanDetail(id, principal.getName()), principal, model);
    }

    @PostMapping("/{id}/items/{itemId}/delete")
    public String removeItem(@PathVariable UUID id, @PathVariable UUID itemId,
                             Principal principal, RedirectAttributes redirectAttributes) {
        planService.removeItem(id, itemId, principal.getName());
        redirectAttributes.addFlashAttribute("message", "Item removed.");
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

    private String showDetail(Plan plan, Principal principal, Model model) {
        model.addAttribute("plan", plan);
        model.addAttribute("canModify", planService.canModify(plan, principal.getName()));
        model.addAttribute("itemsByDay", itemsByDay(plan));
        return "plans/detail";
    }

    /** Day number -> that day's stops (in order), with an empty list for days that have no stops yet. */
    private Map<Integer, List<PlanItem>> itemsByDay(Plan plan) {
        Map<Integer, List<PlanItem>> days = new TreeMap<>();
        for (int day = 1; day <= plan.getDays(); day++) {
            days.put(day, new ArrayList<>());
        }
        plan.getPlanItems().forEach(item -> days.computeIfAbsent(item.getDayNumber(), d -> new ArrayList<>()).add(item));
        return days;
    }
}
