package com.hotel.hotel.mapper;

import com.hotel.hotel.dto.RoomDto;
import com.hotel.hotel.entity.Room;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoomMapper {
    RoomDto toDto(Room room);
}