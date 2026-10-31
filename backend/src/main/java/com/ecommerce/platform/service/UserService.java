package com.ecommerce.platform.service;

import com.ecommerce.platform.common.PageResponse;
import com.ecommerce.platform.dto.response.UserResponse;

public interface UserService {

    PageResponse<UserResponse> listForAdmin(int page, int size);
}
