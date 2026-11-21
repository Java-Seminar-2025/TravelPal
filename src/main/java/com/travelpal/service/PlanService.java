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

/**
 * Travel plans. Ownership rules:
 * - a new plan always belongs to the logged-in user who created it
 * - everyone logged in can view PUBLIC plans; PRIVATE plans only by their owner or an admin
 * - only the owner or an admin can edit a plan, add/remove its items or delete it
 */
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
        Plan plan = findPlan(planId);
        User currentUser = getUserByUsername(username);

        if (plan.getVisibility() != Plan.Visibility.PUBLIC && !isOwnerOrAdmin(plan, currentUser)) {
            throw new AccessDeniedException("You do not have permission to view this plan");
        }

        plan.getPlanItems().sort(Comparator
                .comparing(PlanItem::getDayNumber)
                .thenComparing(PlanItem::getOrderIndex));
        return plan;
    }

    /** True if this user may edit/delete the plan (owner or admin). Used to show or hide buttons. */
    @Transactional(readOnly = true)
    public boolean canModify(Plan plan, String username) {
        return isOwnerOrAdmin(plan, getUserByUsername(username));
    }

    /** Fills a form with the plan's current values, for the edit page. */
    @Transactional(readOnly = true)
    public PlanForm getEditForm(UUID planId, String username) {
        Plan plan = getModifiablePlan(planId, username);
        PlanForm form = new PlanForm();
        form.setTitle(plan.getTitle());
        form.setCity(plan.getCity());
        form.setDays(plan.getDays());
        form.setVisibility(plan.getVisibility());
        form.setAiGenerated(plan.getAiGenerated());
        return form;
    }

    @Transactional
    public Plan createPlan(PlanForm form, String username) {
        Plan plan = new Plan();
        plan.setOwner(getUserByUsername(username));
        plan.setAiGenerated(form.isAiGenerated());
        applyForm(plan, form);
        return planRepository.save(plan);
    }

    /** Changes title, city, days and visibility. The owner never changes, even when an admin edits. */
    @Transactional
    public Plan updatePlan(UUID planId, PlanForm form, String username) {
        Plan plan = getModifiablePlan(planId, username);
        int lastUsedDay = plan.getPlanItems().stream().mapToInt(PlanItem::getDayNumber).max().orElse(0);
        if (form.getDays() < lastUsedDay) {
            throw new IllegalArgumentException("This plan has items on day " + lastUsedDay + ", so it needs at least " + lastUsedDay + " days");
        }
        applyForm(plan, form);
        return planRepository.save(plan);
    }

    @Transactional
    public PlanItem addItem(UUID planId, PlanItemForm form, String username) {
        Plan plan = getModifiablePlan(planId, username);

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
    public void removeItem(UUID planId, UUID itemId, String username) {
        Plan plan = getModifiablePlan(planId, username);
        boolean removed = plan.getPlanItems().removeIf(item -> item.getId().equals(itemId));
        if (!removed) {
            throw new NoSuchElementException("Item " + itemId + " is not part of plan " + planId);
        }
    }

    @Transactional
    public Plan copyPublicPlan(UUID sourcePlanId, String username) {
        Plan source = findPlan(sourcePlanId);

        if (source.getVisibility() != Plan.Visibility.PUBLIC) {
            throw new AccessDeniedException("Only public plans can be copied");
        }

        Plan copy = new Plan();
        copy.setOwner(getUserByUsername(username));
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
        planRepository.delete(getModifiablePlan(planId, username));
    }

    // --- helpers ---

    private void applyForm(Plan plan, PlanForm form) {
        plan.setTitle(form.getTitle().trim());
        plan.setCity(form.getCity().trim());
        plan.setDays(form.getDays());
        plan.setVisibility(form.getVisibility());
    }

    /** Loads the plan and checks the logged-in user owns it or is an admin; otherwise 403. */
    private Plan getModifiablePlan(UUID planId, String username) {
        Plan plan = findPlan(planId);
        if (!isOwnerOrAdmin(plan, getUserByUsername(username))) {
            throw new AccessDeniedException("You do not have permission to modify this plan");
        }
        return plan;
    }

    /** Ownership is checked by user id, never by name or by anything the browser sends. */
    private boolean isOwnerOrAdmin(Plan plan, User user) {
        return plan.getOwner().getId().equals(user.getId()) || user.getRole() == User.Role.ADMIN;
    }

    private Plan findPlan(UUID planId) {
        return planRepository.findWithDetailsById(planId)
                .orElseThrow(() -> new NoSuchElementException("Plan not found: " + planId));
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));
    }
}
