package com.megaproject.profile.dto.request;
import lombok.Data;
@Data
public class AddressRequest {
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}
