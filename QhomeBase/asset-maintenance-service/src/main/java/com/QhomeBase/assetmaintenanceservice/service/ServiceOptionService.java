package com.QhomeBase.assetmaintenanceservice.service;

import com.QhomeBase.assetmaintenanceservice.dto.service.CreateServiceOptionGroupRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.CreateServiceOptionRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceOptionDto;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceOptionGroupDto;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceOptionGroupItemRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.UpdateServiceOptionGroupItemsRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.UpdateServiceOptionGroupRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.UpdateServiceOptionRequest;
import com.QhomeBase.assetmaintenanceservice.model.service.ServiceOption;
import com.QhomeBase.assetmaintenanceservice.model.service.ServiceOptionGroup;
import com.QhomeBase.assetmaintenanceservice.model.service.ServiceOptionGroupItem;
import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServiceBookingType;
import com.QhomeBase.assetmaintenanceservice.repository.ServiceOptionGroupRepository;
import com.QhomeBase.assetmaintenanceservice.repository.ServiceOptionRepository;
import com.QhomeBase.assetmaintenanceservice.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ServiceOptionService {

    private final ServiceRepository serviceRepository;
    private final ServiceOptionRepository serviceOptionRepository;
    private final ServiceOptionGroupRepository serviceOptionGroupRepository;
    private final ServiceConfigService serviceConfigService;

    @Transactional(readOnly = true)
    public List<ServiceOptionDto> getOptions(UUID serviceId, Boolean isActive) {
        com.QhomeBase.assetmaintenanceservice.model.service.Service service = findServiceOrThrow(serviceId);
        return serviceOptionRepository.findAllByServiceId(service.getId()).stream()
                .filter(option -> filterByActive(option, isActive))
                .sorted(Comparator.comparing(ServiceOption::getSortOrder, Comparator.nullsFirst(Integer::compareTo))
                        .thenComparing(ServiceOption::getName, String.CASE_INSENSITIVE_ORDER))
                .map(serviceConfigService::toOptionDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceOptionDto> getAllOptions(Boolean isActive) {
        return serviceOptionRepository.findAll().stream()
                .filter(option -> filterByActive(option, isActive))
                .map(serviceConfigService::toOptionDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServiceOptionDto getOption(UUID optionId) {
        ServiceOption option = serviceOptionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("Service option not found: " + optionId));
        return serviceConfigService.toOptionDto(option);
    }

    @Transactional
    public ServiceOptionDto createOption(UUID serviceId, CreateServiceOptionRequest request) {
        com.QhomeBase.assetmaintenanceservice.model.service.Service service = findServiceOrThrow(serviceId);
        validateServiceSupportsOptions(service);
        validateOptionCodeUnique(serviceId, null, request.getCode());

        ServiceOption option = new ServiceOption();
        option.setService(service);
        option.setCode(request.getCode().trim());
        option.setName(request.getName().trim());
        option.setDescription(trimToNull(request.getDescription()));
        option.setPrice(request.getPrice());
        option.setUnit(trimToNull(request.getUnit()));
        option.setIsRequired(request.getIsRequired() != null ? request.getIsRequired() : Boolean.FALSE);
        option.setIsActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE);
        if (Boolean.TRUE.equals(option.getIsRequired()) && Boolean.FALSE.equals(option.getIsActive())) {
            throw new IllegalArgumentException("A required option cannot be created as inactive");
        }
        option.setSortOrder(resolveOptionSortOrder(serviceId, request.getSortOrder()));

        ServiceOption saved = serviceOptionRepository.save(option);
        return serviceConfigService.toOptionDto(saved);
    }

    @Transactional
    public ServiceOptionDto updateOption(UUID optionId, UpdateServiceOptionRequest request) {
        ServiceOption option = serviceOptionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("Service option not found: " + optionId));

        option.setName(request.getName().trim());
        option.setDescription(trimToNull(request.getDescription()));
        option.setPrice(request.getPrice());
        option.setUnit(trimToNull(request.getUnit()));
        if (request.getIsRequired() != null) {
            option.setIsRequired(request.getIsRequired());
        }
        if (request.getIsActive() != null) {
            if (Boolean.TRUE.equals(option.getIsRequired()) && Boolean.FALSE.equals(request.getIsActive())) {
                throw new IllegalArgumentException("Cannot deactivate an option that is marked as required");
            }
            option.setIsActive(request.getIsActive());
        }
        if (request.getSortOrder() != null) {
            option.setSortOrder(request.getSortOrder());
        }

        ServiceOption saved = serviceOptionRepository.save(option);
        return serviceConfigService.toOptionDto(saved);
    }

    @Transactional
    public ServiceOptionDto setOptionStatus(UUID optionId, boolean active) {
        ServiceOption option = serviceOptionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("Service option not found: " + optionId));
        if (Boolean.TRUE.equals(option.getIsRequired()) && !active) {
            throw new IllegalArgumentException("Cannot deactivate an option that is marked as required");
        }
        option.setIsActive(active);
        ServiceOption saved = serviceOptionRepository.save(option);
        return serviceConfigService.toOptionDto(saved);
    }

    @Transactional
    public void deleteOption(UUID optionId) {
        ServiceOption option = serviceOptionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("Service option not found: " + optionId));
        if (Boolean.TRUE.equals(option.getIsRequired())) {
            throw new IllegalArgumentException("Cannot delete an option that is marked as required");
        }
        serviceOptionRepository.delete(option);
    }

    @Transactional(readOnly = true)
    public List<ServiceOptionGroupDto> getOptionGroups(UUID serviceId) {
        com.QhomeBase.assetmaintenanceservice.model.service.Service service = findServiceOrThrow(serviceId);
        return serviceOptionGroupRepository.findAllByServiceId(service.getId()).stream()
                .sorted(Comparator.comparing(ServiceOptionGroup::getSortOrder, Comparator.nullsFirst(Integer::compareTo))
                        .thenComparing(ServiceOptionGroup::getName, String.CASE_INSENSITIVE_ORDER))
                .map(serviceConfigService::toOptionGroupDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceOptionGroupDto> getAllOptionGroups() {
        return serviceOptionGroupRepository.findAll().stream()
                .map(serviceConfigService::toOptionGroupDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServiceOptionGroupDto getOptionGroup(UUID groupId) {
        ServiceOptionGroup group = serviceOptionGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Service option group not found: " + groupId));
        return serviceConfigService.toOptionGroupDto(group);
    }

    @Transactional
    public ServiceOptionGroupDto createOptionGroup(UUID serviceId, CreateServiceOptionGroupRequest request) {
        com.QhomeBase.assetmaintenanceservice.model.service.Service service = findServiceOrThrow(serviceId);
        validateServiceSupportsOptions(service);
        validateGroupCodeUnique(serviceId, null, request.getCode());

        ServiceOptionGroup group = new ServiceOptionGroup();
        group.setService(service);
        group.setCode(request.getCode().trim());
        group.setName(request.getName().trim());
        group.setDescription(trimToNull(request.getDescription()));
        group.setMinSelect(request.getMinSelect() != null ? request.getMinSelect() : 0);
        group.setMaxSelect(request.getMaxSelect());
        group.setIsRequired(request.getIsRequired() != null ? request.getIsRequired() : Boolean.FALSE);
        group.setSortOrder(resolveGroupSortOrder(serviceId, request.getSortOrder()));

        ServiceOptionGroup saved = serviceOptionGroupRepository.save(group);
        return serviceConfigService.toOptionGroupDto(saved);
    }

    @Transactional
    public ServiceOptionGroupDto updateOptionGroup(UUID groupId, UpdateServiceOptionGroupRequest request) {
        ServiceOptionGroup group = serviceOptionGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Service option group not found: " + groupId));

        group.setName(request.getName().trim());
        group.setDescription(trimToNull(request.getDescription()));
        group.setMinSelect(request.getMinSelect() != null ? request.getMinSelect() : group.getMinSelect());
        group.setMaxSelect(request.getMaxSelect());
        if (group.getMinSelect() != null && group.getMaxSelect() != null && group.getMaxSelect() < group.getMinSelect()) {
            throw new IllegalArgumentException("maxSelect must be greater than or equal to minSelect");
        }
        if (request.getIsRequired() != null) {
            group.setIsRequired(request.getIsRequired());
        }
        if (request.getSortOrder() != null) {
            group.setSortOrder(request.getSortOrder());
        }

        ServiceOptionGroup saved = serviceOptionGroupRepository.save(group);
        return serviceConfigService.toOptionGroupDto(saved);
    }

    @Transactional
    public ServiceOptionGroupDto updateOptionGroupItems(UUID groupId, UpdateServiceOptionGroupItemsRequest request) {
        ServiceOptionGroup group = serviceOptionGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Service option group not found: " + groupId));

        List<ServiceOptionGroupItem> items = buildGroupItems(group, request.getItems());
        group.getItems().clear();
        group.getItems().addAll(items);

        ServiceOptionGroup saved = serviceOptionGroupRepository.save(group);
        return serviceConfigService.toOptionGroupDto(saved);
    }

    @Transactional
    public void deleteOptionGroup(UUID groupId) {
        ServiceOptionGroup group = serviceOptionGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Service option group not found: " + groupId));
        if (Boolean.TRUE.equals(group.getIsRequired())) {
            throw new IllegalArgumentException("Cannot delete a required option group");
        }
        serviceOptionGroupRepository.delete(group);
    }

    private com.QhomeBase.assetmaintenanceservice.model.service.Service findServiceOrThrow(UUID serviceId) {
        return serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));
    }

    private boolean filterByActive(ServiceOption option, Boolean isActive) {
        if (isActive == null) {
            return true;
        }
        boolean optionActive = Boolean.TRUE.equals(option.getIsActive());
        return Boolean.TRUE.equals(isActive) ? optionActive : !optionActive;
    }

    private void validateServiceSupportsOptions(com.QhomeBase.assetmaintenanceservice.model.service.Service service) {
        ServiceBookingType bookingType = service.getBookingType();
        if (bookingType == ServiceBookingType.COMBO_BASED) {
            throw new IllegalArgumentException("Service booking type COMBO_BASED cannot configure standalone options");
        }
    }

    private void validateOptionCodeUnique(UUID serviceId, UUID optionId, String code) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("Option code is required");
        }
        String trimmed = code.trim();
        boolean exists = optionId == null
                ? serviceOptionRepository.existsByServiceIdAndCodeIgnoreCase(serviceId, trimmed)
                : serviceOptionRepository.existsByServiceIdAndCodeIgnoreCaseAndIdNot(serviceId, trimmed, optionId);
        if (exists) {
            throw new IllegalArgumentException("Option code already exists for this service: " + trimmed);
        }
    }

    private void validateGroupCodeUnique(UUID serviceId, UUID groupId, String code) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("Option group code is required");
        }
        String trimmed = code.trim();
        boolean exists = groupId == null
                ? serviceOptionGroupRepository.existsByServiceIdAndCodeIgnoreCase(serviceId, trimmed)
                : serviceOptionGroupRepository.existsByServiceIdAndCodeIgnoreCaseAndIdNot(serviceId, trimmed, groupId);
        if (exists) {
            throw new IllegalArgumentException("Option group code already exists for this service: " + trimmed);
        }
    }

    private int resolveOptionSortOrder(UUID serviceId, Integer requested) {
        if (requested != null) {
            return requested;
        }
        return serviceOptionRepository.findAllByServiceId(serviceId).stream()
                .map(ServiceOption::getSortOrder)
                .filter(sort -> sort != null)
                .max(Integer::compareTo)
                .map(max -> max + 1)
                .orElse(1);
    }

    private int resolveGroupSortOrder(UUID serviceId, Integer requested) {
        if (requested != null) {
            return requested;
        }
        return serviceOptionGroupRepository.findAllByServiceId(serviceId).stream()
                .map(ServiceOptionGroup::getSortOrder)
                .filter(sort -> sort != null)
                .max(Integer::compareTo)
                .map(max -> max + 1)
                .orElse(1);
    }

    private List<ServiceOptionGroupItem> buildGroupItems(ServiceOptionGroup group,
                                                         List<ServiceOptionGroupItemRequest> requests) {
        Set<UUID> uniqueOptionIds = new HashSet<>();
        return IntStream.range(0, requests.size())
                .mapToObj(index -> {
                    ServiceOptionGroupItemRequest itemRequest = requests.get(index);
                    UUID optionId = itemRequest.getOptionId();
                    if (!uniqueOptionIds.add(optionId)) {
                        throw new IllegalArgumentException("Duplicate option detected in group items: " + optionId);
                    }
                    ServiceOption option = serviceOptionRepository.findById(optionId)
                            .orElseThrow(() -> new IllegalArgumentException("Service option not found: " + optionId));
                    if (!option.getService().getId().equals(group.getService().getId())) {
                        throw new IllegalArgumentException("Option " + optionId + " does not belong to service " + group.getService().getId());
                    }
                    ServiceOptionGroupItem item = new ServiceOptionGroupItem();
                    item.setGroup(group);
                    item.setOption(option);
                    item.setSortOrder(itemRequest.getSortOrder() != null ? itemRequest.getSortOrder() : index + 1);
                    return item;
                })
                .collect(Collectors.toList());
    }
    private List<ServiceOptionGroupItem> buildGroupItem(ServiceOptionGroup group,List<ServiceOptionGroupItemRequest> options) {
        Set<UUID> uniqueOptionIds = new HashSet<>();
        List<ServiceOptionGroupItem> items = group.getItems();
        for (ServiceOptionGroupItem request : items) {
            if (!uniqueOptionIds.add(request.getOption().getId())) {
                throw new IllegalArgumentException("Duplicate option detected in group items: " + request.getOption().getId());
            }
            uniqueOptionIds.add(request.getOption().getId());
        }
        List<UUID> optionIds = options.stream()
                .map(ServiceOptionGroupItemRequest::getOptionId)
                .toList();
        Map<UUID, ServiceOption> optionMap = new HashMap<>();
        for (UUID optionId : optionIds) {
            ServiceOption serviceOption = serviceOptionRepository.findById(optionId).orElseThrow(() -> new IllegalArgumentException("Service option not found: " + optionId));
            optionMap.put(optionId, serviceOption);
        }
        UUID parentServiceId = group.getService().getId();
        List<ServiceOptionGroupItem> result = new ArrayList<>();
        int size = 0;
        for (ServiceOptionGroupItemRequest request : options) {
            UUID optionId = request.getOptionId();
            ServiceOption option = optionMap.get(optionId);
            if (!option.getService().getId().equals(parentServiceId)) {
                throw new IllegalArgumentException("Option " + optionId + " does not belong to service " + parentServiceId);
            }
            ServiceOptionGroupItem item = new ServiceOptionGroupItem();
            item.setGroup(group);
            item.setOption(option);
            item.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : size + 1);

            result.add(item);
            size++;
        }
        return result;
    }


    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}


