package com.example.springhotel.controller;

import com.example.springhotel.dto.HotelSearchDTO;
import com.example.springhotel.entity.Hotel;
import com.example.springhotel.service.HotelSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class HotelSearchController {

    private final HotelSearchService hotelSearchService;

    @PostMapping("/search")
    public ResponseEntity<List<Hotel>> searchHotels(@RequestBody HotelSearchDTO searchDTO) {
        List<Hotel> hotels = hotelSearchService.searchHotels(searchDTO);
        return ResponseEntity.ok(hotels);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Hotel>> getAllHotels() {
        HotelSearchDTO emptySearch = new HotelSearchDTO();
        List<Hotel> hotels = hotelSearchService.searchHotels(emptySearch);
        return ResponseEntity.ok(hotels);
    }
}