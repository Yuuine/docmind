package yuuine.docmind.core.user.service;

import yuuine.docmind.core.user.dto.UserLoginRequest;
import yuuine.docmind.core.user.dto.UserRegisterRequest;
import yuuine.docmind.core.user.dto.UserResponse;
import yuuine.docmind.core.user.dto.UserUpdateRequest;

public interface UserService {
    UserResponse register(UserRegisterRequest request);

    UserResponse login(UserLoginRequest request);

    UserResponse getProfile(Long userId);

    UserResponse updateProfile(Long userId, UserUpdateRequest request);

    void deleteUser(Long userId);
}
