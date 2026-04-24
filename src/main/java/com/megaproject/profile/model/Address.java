package com.megaproject.profile.model;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Address {
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}
