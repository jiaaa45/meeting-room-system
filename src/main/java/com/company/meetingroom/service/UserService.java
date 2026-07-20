package com.company.meetingroom.service;

import com.company.meetingroom.dto.UserRequestDto;
import com.company.meetingroom.dto.UserResponseDto;
import com.company.meetingroom.entity.User;
import com.company.meetingroom.exception.DuplicateEmailException;
import com.company.meetingroom.exception.ResourceNotFoundException;
import com.company.meetingroom.mapper.UserMapper;
import com.company.meetingroom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserResponseDto create(UserRequestDto requestDto) {
        userRepository.findByEmail(requestDto.getEmail())
                .ifPresent(existing -> {
                    throw new DuplicateEmailException("email 已被使用:" + requestDto.getEmail());
                });

        User user = userMapper.toEntity(requestDto);
        User saved = userRepository.save(user);
        return userMapper.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到 id 為 " + id + " 的使用者"));
        return userMapper.toResponseDto(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponseDto> getAll() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponseDto)
                .toList();
    }
}
