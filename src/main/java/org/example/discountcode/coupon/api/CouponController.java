package org.example.discountcode.coupon.api;

import jakarta.validation.Valid;
import org.example.discountcode.coupon.application.CouponService;
import org.example.discountcode.coupon.api.request.CreateCouponRequest;
import org.example.discountcode.coupon.api.response.CouponResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CouponResponse createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        return couponService.createCoupon(request);
    }

    @GetMapping("/{code}")
    CouponResponse getCoupon(@PathVariable String code) {
        return couponService.getCoupon(code);
    }
}
