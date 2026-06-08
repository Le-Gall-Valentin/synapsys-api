package com.synapsys.api.identity.domain.port.out;

import com.synapsys.api.identity.domain.model.User;
import com.synapsys.api.shared.model.PageResult;
import com.synapsys.api.shared.model.SortRequest;

public interface UserAdminPort {
    boolean isEmpty();
    PageResult<User> findAll(int page, int size, SortRequest sort);
}