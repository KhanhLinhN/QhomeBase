package com.QhomeBase.assetmaintenanceservice.service;

import com.QhomeBase.assetmaintenanceservice.dto.service.CreateServiceComboRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceComboDto;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceComboItemRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.UpdateServiceComboItemsRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.UpdateServiceComboRequest;
import com.QhomeBase.assetmaintenanceservice.model.service.ServiceCombo;
import com.QhomeBase.assetmaintenanceservice.model.service.ServiceComboItem;
import com.QhomeBase.assetmaintenanceservice.model.service.ServiceOption;
import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServiceBookingType;
import com.QhomeBase.assetmaintenanceservice.repository.ServiceComboRepository;
import com.QhomeBase.assetmaintenanceservice.repository.ServiceOptionRepository;
import com.QhomeBase.assetmaintenanceservice.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceComboService {

    private final ServiceRepository serviceRepository;
    private final ServiceComboRepository serviceComboRepository;
    private final ServiceOptionRepository serviceOptionRepository;
    private final ServiceConfigService serviceConfigService;

    @Transactional(readOnly = true)
    public List<ServiceComboDto> getCombos(UUID serviceId, Boolean isActive) {
        com.QhomeBase.assetmaintenanceservice.model.service.Service service = findServiceOrThrow(serviceId);
        return serviceComboRepository.findAllByServiceId(service.getId()).stream()
                .filter(combo -> {
                    if (isActive == null) {
                        return true;
                    }
                    boolean comboActive = Boolean.TRUE.equals(combo.getIsActive());
                    return Boolean.TRUE.equals(isActive) ? comboActive : !comboActive;
                })
                .map(serviceConfigService::toComboDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServiceComboDto getCombo(UUID comboId) {
        ServiceCombo combo = serviceComboRepository.findById(comboId)
                .orElseThrow(() -> new IllegalArgumentException("Service combo not found: " + comboId));
        return serviceConfigService.toComboDto(combo);
    }

    @Transactional
    public ServiceComboDto createCombo(UUID serviceId, CreateServiceComboRequest request) {
        com.QhomeBase.assetmaintenanceservice.model.service.Service service = findServiceOrThrow(serviceId);
        validateServiceSupportsCombo(service);
        validateComboCode(serviceId, request.getCode());

        ServiceCombo combo = new ServiceCombo();
        combo.setService(service);
        combo.setCode(request.getCode().trim());
        combo.setName(request.getName().trim());
        combo.setDescription(request.getDescription());
        combo.setServicesIncluded(request.getServicesIncluded());
        combo.setDurationMinutes(request.getDurationMinutes());
        combo.setPrice(request.getPrice());
        combo.setIsActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE);
        combo.setSortOrder(getLastestSortOrderCombo(serviceId)+1);

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            List<ServiceComboItem> items = buildComboItems(combo, request.getItems());
            combo.setItems(items);
        }

        ServiceCombo saved = serviceComboRepository.save(combo);
        return serviceConfigService.toComboDto(saved);
    }
    public int getLastestSortOrderCombo(UUID serviceId) {
        List<ServiceCombo> serviceCombos = serviceComboRepository.findAllByServiceId(serviceId);
        int maxSortOrder = serviceCombos.stream()
                .sorted(Comparator.comparing(ServiceCombo::getSortOrder).reversed())
                .findFirst()
                .map(ServiceCombo::getSortOrder)
                .orElse(0);
        return maxSortOrder;
    }

    @Transactional
    public ServiceComboDto updateCombo(UUID comboId, UpdateServiceComboRequest request) {
        ServiceCombo combo = serviceComboRepository.findById(comboId)
                .orElseThrow(() -> new IllegalArgumentException("Service combo not found: " + comboId));

        if (StringUtils.hasText(request.getName())) {
            combo.setName(request.getName().trim());
        }
        combo.setDescription(request.getDescription());
        combo.setServicesIncluded(request.getServicesIncluded());
        combo.setDurationMinutes(request.getDurationMinutes());
        combo.setPrice(request.getPrice());
        if (request.getIsActive() != null) {
            combo.setIsActive(request.getIsActive());
        }
        combo.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : combo.getSortOrder());

        if (request.getItems() != null) {
            combo.getItems().clear();
            combo.getItems().addAll(buildComboItems(combo, request.getItems()));
        }

        ServiceCombo saved = serviceComboRepository.save(combo);
        return serviceConfigService.toComboDto(saved);
    }

    @Transactional
    public ServiceComboDto updateComboItems(UUID comboId, UpdateServiceComboItemsRequest request) {
        ServiceCombo combo = serviceComboRepository.findById(comboId)
                .orElseThrow(() -> new IllegalArgumentException("Service combo not found: " + comboId));

        combo.getItems().clear();
        combo.getItems().addAll(buildComboItems(combo, request.getItems()));

        ServiceCombo saved = serviceComboRepository.save(combo);
        return serviceConfigService.toComboDto(saved);
    }

    @Transactional
    public ServiceComboDto setComboStatus(UUID comboId, boolean active) {
        ServiceCombo combo = serviceComboRepository.findById(comboId)
                .orElseThrow(() -> new IllegalArgumentException("Service combo not found: " + comboId));
        combo.setIsActive(active);
        ServiceCombo saved = serviceComboRepository.save(combo);
        return serviceConfigService.toComboDto(saved);
    }

    @Transactional
    public void deleteCombo(UUID comboId) {
        ServiceCombo combo = serviceComboRepository.findById(comboId)
                .orElseThrow(() -> new IllegalArgumentException("Service combo not found: " + comboId));
        serviceComboRepository.delete(combo);
    }

    private void validateComboCode(UUID serviceId, String code) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("Combo code is required");
        }
        if (serviceComboRepository.existsByServiceIdAndCodeIgnoreCase(serviceId, code.trim())) {
            throw new IllegalArgumentException("Service combo code already exists for this service: " + code);
        }
    }

    private List<ServiceComboItem> buildComboItems(ServiceCombo combo,
                                                   List<ServiceComboItemRequest> requests) {
        return requests.stream()
                .map(request -> buildComboItem(combo, request))
                .collect(Collectors.toList());
    }

    private ServiceComboItem buildComboItem(ServiceCombo combo, ServiceComboItemRequest request) {
        if (request.getIncludedServiceId() == null && request.getOptionId() == null) {
            throw new IllegalArgumentException("Combo item must reference either a service or an option");
        }
        ServiceComboItem item = new ServiceComboItem();
        item.setCombo(combo);
        if (request.getIncludedServiceId() != null) {
            com.QhomeBase.assetmaintenanceservice.model.service.Service includedService = serviceRepository.findById(request.getIncludedServiceId())
                    .orElseThrow(() -> new IllegalArgumentException("Included service not found: " + request.getIncludedServiceId()));
            item.setIncludedService(includedService);
        }
        if (request.getOptionId() != null) {
            ServiceOption option = serviceOptionRepository.findById(request.getOptionId())
                    .orElseThrow(() -> new IllegalArgumentException("Service option not found: " + request.getOptionId()));
            item.setOption(option);
        }
        item.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);
        item.setNote(request.getNote());
        item.setSortOrder(getLastestOrderOfCombo(combo));
        return item;
    }
    public int getLastestOrderOfCombo(ServiceCombo combo) {
        List<ServiceComboItem> serviceComboItemList = combo.getItems();
        int maxSortOrder = serviceComboItemList.stream().sorted(Comparator.comparing(ServiceComboItem::getSortOrder)).findFirst()
                .map(ServiceComboItem::getSortOrder).orElse(0);
        return maxSortOrder;

    }


    private com.QhomeBase.assetmaintenanceservice.model.service.Service findServiceOrThrow(UUID serviceId) {
        return serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));
    }

    private void validateServiceSupportsCombo(com.QhomeBase.assetmaintenanceservice.model.service.Service service) {
        if (service.getBookingType() != ServiceBookingType.COMBO_BASED) {
            throw new IllegalArgumentException("Service booking type must be COMBO_BASED to use combos");
        }
    }
}

