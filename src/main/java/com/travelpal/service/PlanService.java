package com.travelpal.service;

import com.travelpal.entity.Plan;
import com.travelpal.entity.PlanItem;
import com.travelpal.entity.User;
import com.travelpal.repository.PlanRepository;
import com.travelpal.repository.UserRepository;
import com.travelpal.web.PlanForm;
import com.travelpal.web.PlanItemForm;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Plan> getPublicPlans(String city) {
        if (city != null && !city.isBlank()) {
            return planRepository.findByCityIgnoreCase(city.trim()).stream()
                    .filter(p -> p.getVisibility() == Plan.Visibility.PUBLIC)
                    .toList();
        }
        return planRepository.findByVisibilityOrderByCreatedAtDesc(Plan.Visibility.PUBLIC);
    }

    @Transactional(readOnly = true)
    public List<Plan> getMyPlans(String username) {
        User owner = getUserByUsername(username);
        return planRepository.findByOwnerIdOrderByCreatedAtDesc(owner.getId());
    }

    @Transactional(readOnly = true)
    public Plan getPlanDetail(UUID planId, String username) {
        Plan plan = planRepository.findWithDetailsById(planId)
                .orElseThrow(() -> new NoSuchElementException("Plan not found: " + planId));
        User currentUser = getUserByUsername(username);

        boolean isOwner = plan.getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;
        boolean isPublic = plan.getVisibility() == Plan.Visibility.PUBLIC;

        if (!isPublic && !isOwner && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to view this plan");
        }

        plan.getPlanItems().sort(Comparator
                .comparing(PlanItem::getDayNumber)
                .thenComparing(PlanItem::getOrderIndex));

        return plan;
    }

    @Transactional
    public Plan createPlan(PlanForm form, String username) {
        User owner = getUserByUsername(username);

        Plan plan = new Plan();
        plan.setOwner(owner);
        plan.setTitle(form.getTitle().trim());
        plan.setCity(form.getCity().trim());
        plan.setDays(form.getDays());
        plan.setVisibility(form.getVisibility());
        plan.setAiGenerated(form.isAiGenerated());

        return planRepository.save(plan);
    }

    @Transactional
    public PlanItem addItem(UUID planId, PlanItemForm form, String username) {
        Plan plan = getOwnedOrAdminPlan(planId, username);

        if (form.getDayNumber() > plan.getDays()) {
            throw new IllegalArgumentException("Day number cannot exceed plan duration (" + plan.getDays() + " days)");
        }

        PlanItem item = new PlanItem();
        item.setPlan(plan);
        item.setTripadvisorLocationId(form.getTripadvisorLocationId().trim());
        item.setDayNumber(form.getDayNumber());
        item.setOrderIndex(form.getOrderIndex());
        item.setNote(form.getNote() != null && !form.getNote().isBlank() ? form.getNote().trim() : null);

        plan.getPlanItems().add(item);
        planRepository.save(plan);
        return item;
    }

    @Transactional
    public Plan copyPublicPlan(UUID sourcePlanId, String username) {
        Plan source = planRepository.findWithDetailsById(sourcePlanId)
                .orElseThrow(() -> new NoSuchElementException("Plan not found: " + sourcePlanId));

        if (source.getVisibility() != Plan.Visibility.PUBLIC) {
            throw new AccessDeniedException("Only public plans can be copied");
        }

        User newOwner = getUserByUsername(username);

        Plan copy = new Plan();
        copy.setOwner(newOwner);
        copy.setCopiedFrom(source);
        copy.setTitle(source.getTitle() + " (Copy)");
        copy.setCity(source.getCity());
        copy.setDays(source.getDays());
        copy.setVisibility(Plan.Visibility.PRIVATE);
        copy.setAiGenerated(source.getAiGenerated());

        for (PlanItem srcItem : source.getPlanItems()) {
            PlanItem itemCopy = new PlanItem();
            itemCopy.setPlan(copy);
            itemCopy.setTripadvisorLocationId(srcItem.getTripadvisorLocationId());
            itemCopy.setDayNumber(srcItem.getDayNumber());
            itemCopy.setOrderIndex(srcItem.getOrderIndex());
            itemCopy.setNote(srcItem.getNote());
            copy.getPlanItems().add(itemCopy);
        }

        return planRepository.save(copy);
    }

    @Transactional
    public void deletePlan(UUID planId, String username) {
        Plan plan = getOwnedOrAdminPlan(planId, username);
        planRepository.delete(plan);
    }

    private Plan getOwnedOrAdminPlan(UUID planId, String username) {
        Plan plan = planRepository.findWithDetailsById(planId)
                .orElseThrow(() -> new NoSuchElementException("Plan not found: " + planId));
        User currentUser = getUserByUsername(username);

        boolean isOwner = plan.getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to modify this plan");
        }
        return plan;
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));
    }
}