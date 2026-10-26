package com.hotel.hotel.mapper;

import com.hotel.hotel.dto.HotelDto;
import com.hotel.hotel.entity.Hotel;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface HotelMapper {
    HotelDto toDto(Hotel hotel);
    Hotel toEntity(HotelDto dto);
}