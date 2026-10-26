package com.hotel.hotel.service;

import com.hotel.hotel.dto.HotelDto;
import com.hotel.hotel.entity.Hotel;
import com.hotel.hotel.exception.NotFoundException;
import com.hotel.hotel.mapper.HotelMapper;
import com.hotel.hotel.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HotelService {
    private final HotelRepository hotelRepository;
    private final HotelMapper hotelMapper;

    @Transactional
    public HotelDto create(HotelDto dto) {
        Hotel saved = hotelRepository.save(Hotel.builder()
                .name(dto.name()).address(dto.address()).build());
        return hotelMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<HotelDto> findAll() {
        return hotelRepository.findAll().stream().map(hotelMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public HotelDto findById(Long id) {
        return hotelRepository.findById(id)
                .map(hotelMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Hotel not found: " + id));
    }

    @Transactional
    public void delete(Long id) {
        if (!hotelRepository.existsById(id)) {
            throw new NotFoundException("Hotel not found: " + id);
        }
        hotelRepository.deleteById(id);
    }
}
