package yuuine.docmind.core.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import yuuine.docmind.common.exception.BusinessException;
import yuuine.docmind.common.exception.ErrorCode;
import yuuine.docmind.core.user.dto.UserLoginRequest;
import yuuine.docmind.core.user.dto.UserRegisterRequest;
import yuuine.docmind.core.user.dto.UserResponse;
import yuuine.docmind.core.user.dto.UserUpdateRequest;
import yuuine.docmind.core.user.model.User;
import yuuine.docmind.core.user.repository.UserRepository;
import yuuine.docmind.core.user.service.UserService;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public UserResponse register(UserRegisterRequest request) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, request.getUsername());
        if (userRepository.selectCount(queryWrapper) > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        String email = isBlank(request.getEmail()) ? null : request.getEmail();
        if (email != null) {
            queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getEmail, email);
            if (userRepository.selectCount(queryWrapper) > 0) {
                throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
            }
        }

        String phone = isBlank(request.getPhone()) ? null : request.getPhone();
        if (phone != null) {
            queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone, phone);
            if (userRepository.selectCount(queryWrapper) > 0) {
                throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
            }
        }

        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .email(email)
                .phone(phone)
                .build();

        userRepository.insert(user);
        return toUserResponse(user);
    }

    @Override
    public UserResponse login(UserLoginRequest request) {
        String identifier = request.getUsername().trim();
        User user = findByIdentifier(identifier);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        return toUserResponse(user);
    }

    private User findByIdentifier(String identifier) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, identifier);
        User user = userRepository.selectOne(queryWrapper);
        if (user != null) return user;

        queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getEmail, identifier);
        user = userRepository.selectOne(queryWrapper);
        if (user != null) return user;

        queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, identifier);
        return userRepository.selectOne(queryWrapper);
    }

    @Override
    public UserResponse getProfile(Long userId) {
        User user = userRepository.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return toUserResponse(user);
    }

    @Override
    public UserResponse updateProfile(Long userId, UserUpdateRequest request) {
        User user = userRepository.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, request.getEmail())
                    .ne(User::getId, userId);
            if (userRepository.selectCount(wrapper) > 0) {
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        userRepository.updateById(user);
        return toUserResponse(user);
    }

    @Override
    public void deleteUser(Long userId) {
        if (userRepository.selectById(userId) == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        userRepository.deleteById(userId);
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
