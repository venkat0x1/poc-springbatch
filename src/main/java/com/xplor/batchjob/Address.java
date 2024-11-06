package com.xplor.batchjob;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    private String id;
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String smartKey;
    private boolean smartKeyReviewed;

}
