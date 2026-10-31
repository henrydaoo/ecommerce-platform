package com.ecommerce.platform.service.impl;

import com.ecommerce.platform.common.PageResponse;
import com.ecommerce.platform.common.Role;
import com.ecommerce.platform.dto.response.UserResponse;
import com.ecommerce.platform.entity.User;
import com.ecommerce.platform.repository.UserRepository;
import com.ecommerce.platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listForAdmin(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(userRepository.findAllByRole(Role.CUSTOMER, pageable).map(this::toResponse));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole().name());
    }
}
